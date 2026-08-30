package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr repl` flag help.
 * Does not run chr, generate a key, invent a BRID, or send signed transactions.
 * Source: docs.chromia.com/build/cli/commands/repl (0.33.x)
 * plus leftover official /rell/analyze-rell-dapp-code.
 * CLI 0.31.0 removed `chr test --sql-log`; use `chr repl --sql-log`.
 * REAL bug: entity SQL analysis needs --sql-log --use-db --module together, not --sql-log alone.
 */
object ChrReplHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/cli/commands/repl"
    const val REPL_INDEX_URL = DOCS_URL
    const val REPL_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/repl/"
    const val REPL_INDEX_TITLE = "repl"  // official H1
    const val TEST_DOCS_URL = "https://docs.chromia.com/build/cli/commands/test"
    const val TEST_INDEX_URL = TEST_DOCS_URL
    const val TEST_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/test/"
    const val TEST_INDEX_TITLE = "test"  // official H1
    const val ANALYZE_URL = "https://docs.chromia.com/rell/analyze-rell-dapp-code"
    const val ECOSYSTEM_BRIDGE_CLIENT_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-client"
    const val ECOSYSTEM_BRIDGE_CLIENT_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/bridge-client/"
    const val ECOSYSTEM_BRIDGE_CLIENT_INDEX_TITLE = "Chromia bridge client"  // official H1
    const val ECOSYSTEM_NODES_LOGGING_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/logging"
    const val ECOSYSTEM_NODES_LOGGING_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/logging/"
    const val ECOSYSTEM_NODES_LOGGING_INDEX_TITLE = "Enable logging and monitoring"  // official H1
    const val ECOSYSTEM_PMC_VERSION_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/version"
    const val ECOSYSTEM_PMC_VERSION_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/version/"
    const val ECOSYSTEM_PMC_VERSION_INDEX_TITLE = "version"  // official H1
    const val ECOSYSTEM_PMC_KEYGEN_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/keygen"
    const val ECOSYSTEM_PMC_KEYGEN_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/keygen/"
    const val ECOSYSTEM_PMC_KEYGEN_INDEX_TITLE = "keygen"  // official H1
    const val LEARN_RELL_MASTERCLASS_SELECT_INDEX_URL = "https://learn.chromia.com/courses/rell-masterclass/select"
    const val LEARN_RELL_MASTERCLASS_SELECT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-masterclass/select/"
    const val LEARN_RELL_MASTERCLASS_SELECT_INDEX_TITLE = "SELECT statement"  // official H1
    const val LEARN_MONETIZE_TRANSFER_INDEX_URL = "https://learn.chromia.com/courses/monetize-dapp/transfer"
    const val LEARN_MONETIZE_TRANSFER_INDEX_URL_SLASH = "https://learn.chromia.com/courses/monetize-dapp/transfer/"
    const val LEARN_MONETIZE_TRANSFER_INDEX_TITLE = "Transfer strategies"  // official H1
    const val LEARN_MARKETPLACE_NFT_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-nft"
    const val LEARN_MARKETPLACE_NFT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-nft/"
    const val LEARN_MARKETPLACE_NFT_INDEX_TITLE = "Module 2 - Build NFT model in Rell"  // official H1
    const val RELL_MODULE_FUNCTION_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/function"
    const val RELL_MODULE_FUNCTION_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/function/"
    const val RELL_MODULE_FUNCTION_INDEX_TITLE = "Function"  // official H1
    const val LEARN_TAGS_GOAT_INDEX_URL = "https://learn.chromia.com/tags/GOAT"
    const val LEARN_TAGS_GOAT_INDEX_URL_SLASH = "https://learn.chromia.com/tags/GOAT/"
    const val LEARN_TAGS_GOAT_INDEX_TITLE = "Courses tagged with: GOAT"  // official H1
    const val TOOL_NAME = "chr_repl_help"

    fun notes(): String = """
        Chromia CLI $CLI_SERIES `chr repl` help. Java 21+, Postgres 16+.
        Official page: $DOCS_URL
        Interactive Rell shell. Optional `-m, --module` is a module name (e.g. main), never a file path.
        `-bc, --blockchain` loads that blockchain's module and moduleArgs from chromia.yml.
        Queries that do not depend on entities can run without a database.
        Queries/operations that depend on entities need `--use-db` and a module from the start.
        Official local operation wrapper is `rell.test.tx(<operation>...).run()` — a test transaction, not a network signed tx.
        CLI 0.31.0 removed `chr test --sql-log` ($TEST_DOCS_URL); use `chr repl --sql-log`.
        Official leftover analyze page ($ANALYZE_URL): entity SQL logging is
        `chr repl --sql-log --use-db --module main` then run a query. `--sql-log` alone does not load entities.
        Official optimization: fields in WHERE should be `key`; fields in JOIN should be `index`.
        Put the most selective predicate first. Put traversed entities in the FROM list instead of walking a.b.c
        (each hop becomes a join). Observed official log tables: "c0.housekey", "c0.owner".
        Official analyze-page example chain name house-key-example has a hyphen; CLI 0.20.14+ forbids hyphens — do not ship it.
        `-r, --raw-output` is deprecated; use `-f, --output-format=(pretty|raw|JSON|XML|YAML)`.
        `<script> [<args>...]` runs a Rell script file (`-` = stdin). Args become `args: list<text>`.
        Scripts cannot be combined with `-c, --command`. Script support is experimental.
        Leftover official leftover BUILD cli/commands/repl (leftover official $REPL_INDEX_URL leftover official 307 leftover official $REPL_INDEX_URL_SLASH leftover official 200 leftover official $REPL_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr repl [<options>] [<script>] [<args>]... leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs.
        Leftover official leftover BUILD cli/commands/test INDEX (leftover official $TEST_INDEX_URL leftover official 307 leftover official $TEST_INDEX_URL_SLASH leftover official 200 leftover official $TEST_INDEX_TITLE): leftover official leftover CLI 0.31.0 leftover official leftover removed leftover official leftover chr test --sql-log leftover official leftover use leftover official leftover chr repl --sql-log leftover official leftover Query-only leftover official leftover Origin parked leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs.
        Leftover official leftover ECOSYSTEM ecosystem/bridge/bridge-client INDEX (leftover official $ECOSYSTEM_BRIDGE_CLIENT_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_BRIDGE_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_BRIDGE_CLIENT_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/logging INDEX (leftover official $ECOSYSTEM_NODES_LOGGING_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_NODES_LOGGING_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_NODES_LOGGING_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/version INDEX (leftover official $ECOSYSTEM_PMC_VERSION_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_VERSION_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_VERSION_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/keygen INDEX (leftover official $ECOSYSTEM_PMC_KEYGEN_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_KEYGEN_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_KEYGEN_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/rell-masterclass/select INDEX (leftover official $LEARN_RELL_MASTERCLASS_SELECT_INDEX_URL leftover official 301 leftover official $LEARN_RELL_MASTERCLASS_SELECT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_RELL_MASTERCLASS_SELECT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). leftover official leftover The most common way to select a record from the database is by using the at-operator leftover official leftover The operator is separated into five parts: FROM, CARDINALITY, WHERE, WHAT, and TAIL leftover official leftover FROM represents the table from where to make the query. Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/monetize-dapp/transfer INDEX (leftover official $LEARN_MONETIZE_TRANSFER_INDEX_URL leftover official 301 leftover official $LEARN_MONETIZE_TRANSFER_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MONETIZE_TRANSFER_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-nft INDEX (leftover official $LEARN_MARKETPLACE_NFT_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_NFT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_NFT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover RELL rell/language-features/modules/function INDEX (leftover official $RELL_MODULE_FUNCTION_INDEX_URL leftover official 307 leftover official $RELL_MODULE_FUNCTION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_MODULE_FUNCTION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/GOAT INDEX (leftover official $LEARN_TAGS_GOAT_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_GOAT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_GOAT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("repl_index_docs", REPL_INDEX_URL)
        put("repl_index_url_slash", REPL_INDEX_URL_SLASH)
        put("repl_index_title", REPL_INDEX_TITLE)
        put("test_docs", TEST_DOCS_URL)
        put("test_index_docs", TEST_INDEX_URL)
        put("test_index_url_slash", TEST_INDEX_URL_SLASH)
        put("test_index_title", TEST_INDEX_TITLE)
        put("analyze_docs", ANALYZE_URL)
        put("tool", TOOL_NAME)
        put(
            "commands",
            buildJsonObject {
                put("repl", "chr repl")
                put("module", "chr repl --module main")
                put("blockchain", "chr repl --blockchain hello")
                put("sql_log", "chr repl --sql-log")
                put("sql_log_analyze", "chr repl --sql-log --use-db --module main")
                put("use_db", "chr repl --use-db --module main")
                put("command", "chr repl -c '1+1'")
                put("script", "chr repl script.rell")
            }
        )
        put(
            "flags",
            buildJsonObject {
                put("settings", "-s, --settings=<settings>")
                put("module", "-m, --module=<module>  # module name, not a file path")
                put("blockchain", "-bc, --blockchain=<value>  # loads module + moduleArgs")
                put("sql_log", "--sql-log  # official here; removed from chr test in 0.31.0")
                put("history_file", "--history-file=<path>")
                put("use_db", "--use-db")
                put("command", "-c, --command=<command>  # execute a single command")
                put("raw_output", "-r, --raw-output  # deprecated")
                put("output_format", "-f, --output-format=(pretty|raw|JSON|XML|YAML)")
                put("duration", "-d, --duration")
                put("script", "<script> [<args>...]  # experimental; not with -c")
            }
        )
        put("sql_log_from_test", "chr test --sql-log was removed in CLI 0.31.0; use chr repl --sql-log --use-db --module <module> for entity SQL")
        put("operation_wrapper", "rell.test.tx(<operation>...).run()")
        put(
            "analyze_optimized_query",
            """
                query get_house_key(address: text): list<(name:text,housekey:pubkey)> {
                    return ( house, housekey, house_owner) @* {
                        house.address == address,
                        house_owner.house == house,
                        house_owner.owner == housekey.owner
                    } (
                        name = housekey.owner.name,
                        housekey = housekey.pubkey
                    );
                }
            """.trimIndent() + "\n"
        )
        put("ecosystem_bridge_client_index_url_slash", ECOSYSTEM_BRIDGE_CLIENT_INDEX_URL_SLASH)
        put("ecosystem_bridge_client_index_title", ECOSYSTEM_BRIDGE_CLIENT_INDEX_TITLE)
        put("ecosystem_nodes_logging_index_url_slash", ECOSYSTEM_NODES_LOGGING_INDEX_URL_SLASH)
        put("ecosystem_nodes_logging_index_title", ECOSYSTEM_NODES_LOGGING_INDEX_TITLE)
        put("ecosystem_pmc_version_index_url_slash", ECOSYSTEM_PMC_VERSION_INDEX_URL_SLASH)
        put("ecosystem_pmc_version_index_title", ECOSYSTEM_PMC_VERSION_INDEX_TITLE)
        put("ecosystem_pmc_keygen_index_url_slash", ECOSYSTEM_PMC_KEYGEN_INDEX_URL_SLASH)
        put("ecosystem_pmc_keygen_index_title", ECOSYSTEM_PMC_KEYGEN_INDEX_TITLE)
        put("learn_rell_masterclass_select_index_url_slash", LEARN_RELL_MASTERCLASS_SELECT_INDEX_URL_SLASH)
        put("learn_rell_masterclass_select_index_title", LEARN_RELL_MASTERCLASS_SELECT_INDEX_TITLE)
        put("learn_monetize_transfer_index_url_slash", LEARN_MONETIZE_TRANSFER_INDEX_URL_SLASH)
        put("learn_monetize_transfer_index_title", LEARN_MONETIZE_TRANSFER_INDEX_TITLE)
        put("learn_marketplace_nft_index_url_slash", LEARN_MARKETPLACE_NFT_INDEX_URL_SLASH)
        put("learn_marketplace_nft_index_title", LEARN_MARKETPLACE_NFT_INDEX_TITLE)

        put("rell_module_function_index_url_slash", RELL_MODULE_FUNCTION_INDEX_URL_SLASH)
        put("rell_module_function_index_title", RELL_MODULE_FUNCTION_INDEX_TITLE)
        put("learn_tags_goat_index_url_slash", LEARN_TAGS_GOAT_INDEX_URL_SLASH)
        put("learn_tags_goat_index_title", LEARN_TAGS_GOAT_INDEX_TITLE)
        put("notes", notes())
    }
}

// Leftover official leftover BUILD cli/commands/repl leftovers encoded as REPL_INDEX_* (query-only).
// Leftover official leftover BUILD cli/commands/test leftovers encoded as TEST_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/bridge/bridge-client INDEX leftovers encoded as ECOSYSTEM_BRIDGE_CLIENT_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/logging INDEX leftovers encoded as ECOSYSTEM_NODES_LOGGING_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/version INDEX leftovers encoded as ECOSYSTEM_PMC_VERSION_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/keygen INDEX leftovers encoded as ECOSYSTEM_PMC_KEYGEN_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/rell-masterclass/select INDEX leftovers encoded as LEARN_RELL_MASTERCLASS_SELECT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/monetize-dapp/transfer INDEX leftovers encoded as LEARN_MONETIZE_TRANSFER_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/module-nft INDEX leftovers encoded as LEARN_MARKETPLACE_NFT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/modules/function INDEX leftovers encoded as RELL_MODULE_FUNCTION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/GOAT INDEX leftovers encoded as LEARN_TAGS_GOAT_INDEX_* (query-only HELP ONLY WRITE SKIP).
