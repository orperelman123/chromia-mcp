package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x public `chr library` help (install / list / view / versions).
 * `chr install` is the documented alias of `chr library install`.
 * Documents official ICCF library-chain com.chromia.iccf 1.90.1 and FT4-setup git 1.87.0.
 * Does not run chr, generate keys, invent a library-chain BRID, or send signed transactions.
 * Source: docs.chromia.com/build/cli/commands/library and docs.chromia.com/build/cli/library.
 * Official BUILD cli/library index slash/title values live here (query-only).
 */
object ChrLibraryHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/cli/commands/library"
    const val COMMANDS_LIBRARY_INDEX_URL = DOCS_URL
    const val COMMANDS_LIBRARY_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/library/"
    const val COMMANDS_LIBRARY_INDEX_TITLE = "library"  // official H1
    const val GUIDE_URL = "https://docs.chromia.com/build/cli/library"
    const val LIBRARY_INDEX_URL = GUIDE_URL
    const val LIBRARY_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/library/"
    const val LIBRARY_INDEX_TITLE = "Library"
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val FT4_LIBRARY_ID = "com.chromia.ft4"
    const val ICCF_LIBRARY_ID = Ft4ModuleArgs.ICCF_LIBRARY_CHAIN_ID
    const val ICCF_LIBRARY_CHAIN_VERSION = Ft4ModuleArgs.ICCF_LIBRARY_CHAIN_VERSION
    const val ICCF_GIT_TAG = Ft4ModuleArgs.ICCF_GIT_TAG
    const val ICCF_PROTOCOL_URL = Ft4ModuleArgs.ICCF_PROTOCOL_URL
    const val ICCF_FT4_SETUP_URL = Ft4ModuleArgs.ICCF_FT4_SETUP_URL
    const val ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/container-management"
    const val ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/container-management/"
    const val ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_TITLE = "Container management"  // official H1
    const val ECOSYSTEM_ADD_REPLICA_NODE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/add-replica-node"
    const val ECOSYSTEM_ADD_REPLICA_NODE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/add-replica-node/"
    const val ECOSYSTEM_ADD_REPLICA_NODE_INDEX_TITLE = "Add a replica node to the network"  // official H1
    const val ECOSYSTEM_PMC_COMMANDS_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands"
    const val ECOSYSTEM_PMC_COMMANDS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/"
    const val ECOSYSTEM_PMC_COMMANDS_INDEX_TITLE = "PMC command reference"  // official H1
    const val ECOSYSTEM_PMC_TRANSACTION_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/transaction"
    const val ECOSYSTEM_PMC_TRANSACTION_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/transaction/"
    const val ECOSYSTEM_PMC_TRANSACTION_INDEX_TITLE = "transaction"  // official H1
    const val LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_URL = "https://learn.chromia.com/courses/book-review/rell-structure"
    const val LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/rell-structure/"
    const val LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_TITLE = "Rell project structure"  // official H1
    const val LEARN_ICMF_ORDER_CHAIN_INDEX_URL = "https://learn.chromia.com/courses/icmf-course/order-chain"
    const val LEARN_ICMF_ORDER_CHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/icmf-course/order-chain/"
    const val LEARN_ICMF_ORDER_CHAIN_INDEX_TITLE = "Order chain (send message)"  // official H1
    const val LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts/install-configure-ft4"
    const val LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts/install-configure-ft4/"
    const val LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_TITLE = "FT4 accounts configuration"  // official H1
    const val RELL_STATEMENTS_CONDITIONAL_INDEX_URL = "https://docs.chromia.com/rell/language-features/statements/conditional-statements"
    const val RELL_STATEMENTS_CONDITIONAL_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/statements/conditional-statements/"
    const val RELL_STATEMENTS_CONDITIONAL_INDEX_TITLE = "Conditional statements"  // official H1
    const val LEARN_TAGS_TAG_INDEX_URL = "https://learn.chromia.com/tags/tag"
    const val LEARN_TAGS_TAG_INDEX_URL_SLASH = "https://learn.chromia.com/tags/tag/"
    const val LEARN_TAGS_TAG_INDEX_TITLE = "Courses tagged with: tag"  // official H1

    fun libraryChainYaml(): String = """
        libs:
          $FT4_LIBRARY_ID:
            version: "<semver>"
            registry: mainnet
    """.trimIndent() + "\n"

    fun gitYaml(): String = """
        libs:
          ft4:
            registry: ${DappScaffold.FT4_REGISTRY}
            path: ${DappScaffold.FT4_PATH}
            tagOrBranch: ${DappScaffold.FT4_VERSION}
            rid: ${DappScaffold.FT4_RID}
            insecure: false
    """.trimIndent() + "\n"

    fun iccfLibraryChainYaml(): String = Ft4ModuleArgs.libraryChainYaml()

    fun iccfGitYaml(): String = Ft4ModuleArgs.gitIccfYaml()

    fun notes(): String = """
        Chromia CLI $CLI_SERIES public `chr library` help. Java 21+, Postgres 16+.
        Official command page: $DOCS_URL
        Official library guide: $GUIDE_URL
        Schema: $PROJECT_CONFIG_URL
        `chr install` is an alias for `chr library install` (installs libs from chromia.yml).
        Public verbs on the live command page: install, list, view, versions.
        Two official `libs:` shapes: library-chain (recommended; `version` + `registry`) and external Git.
        Library-chain `registry` is mainnet (default), testnet, localhost, or a custom URL.
        Rell import uses the simple name (`import ft4;`), not the full id (`$FT4_LIBRARY_ID`). Official ICCF protocol page imports `lib.iccf;` (not `import iccf;`).
        `--url` / `--brid` override the default mainnet library-chain. Do not invent a library-chain BRID.
        `-f, --force` skips RID verification — not for production.
        Production FT4 git pin is ${DappScaffold.FT4_VERSION} API ${DappScaffold.FT4_API} from ${DappScaffold.FT4_REGISTRY}.
        Confirm a library-chain semver with `chr library versions $FT4_LIBRARY_ID` — do not invent one.
        Official ICCF protocol page documents library-chain $ICCF_LIBRARY_ID version $ICCF_LIBRARY_CHAIN_VERSION ($ICCF_PROTOCOL_URL).
        Official FT4 setup still documents ICCF git tagOrBranch $ICCF_GIT_TAG from directory-chain ($ICCF_FT4_SETUP_URL).
        Use one ICCF lib shape, not both. Do not invent a newer git tag or library-chain semver.
        NEVER import ${DappScaffold.forbiddenModules.joinToString(", ")}.
        Official BUILD cli/library ($LIBRARY_INDEX_URL 307 $LIBRARY_INDEX_URL_SLASH 200 $LIBRARY_INDEX_TITLE): intro The Library commands provide a CLI interface for discovering and installing libraries from the Chromia library registry These commands allow developers to browse available Rell libraries and integrate them into their projects info The Library Chain is a complete dApp deployed on the Chromia blockchain that hosts the library registry Chromia Explorer h1 Library commands library list List all available libraries in the registry chr library list --limit --offset --sort-by library view View detailed information about a specific library chr library view library versions List all versions of a specific library chr library versions library install Install libraries from the registry to your local project chromia.yml libs library-chain version registry External Git path tagOrBranch rid insecure --force skips RID verification import simple name Common options --url --brid skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex.
        Official BUILD cli/commands/library ($COMMANDS_LIBRARY_INDEX_URL 307 $COMMANDS_LIBRARY_INDEX_URL_SLASH 200 $COMMANDS_LIBRARY_INDEX_TITLE): intro Usage chr library list view versions install Query-only WRITE SKIP skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no keygen do not invent flags do not document chr tx signed send keygen samples.
        Official ECOSYSTEM ecosystem/providers/container-management INDEX ($ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_URL 307 $ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/nodes/add-replica-node INDEX ($ECOSYSTEM_ADD_REPLICA_NODE_INDEX_URL 307 $ECOSYSTEM_ADD_REPLICA_NODE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_ADD_REPLICA_NODE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc/commands INDEX ($ECOSYSTEM_PMC_COMMANDS_INDEX_URL 307 $ECOSYSTEM_PMC_COMMANDS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_COMMANDS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/transaction INDEX ($ECOSYSTEM_PMC_TRANSACTION_INDEX_URL 307 $ECOSYSTEM_PMC_TRANSACTION_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_TRANSACTION_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/rell-structure INDEX ($LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_URL 301 $LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/icmf-course/order-chain INDEX ($LEARN_ICMF_ORDER_CHAIN_INDEX_URL 301 $LEARN_ICMF_ORDER_CHAIN_INDEX_URL_SLASH 200 H1 $LEARN_ICMF_ORDER_CHAIN_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/create-accounts/install-configure-ft4 INDEX ($LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_URL 301 $LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official RELL rell/language-features/statements/conditional-statements INDEX ($RELL_STATEMENTS_CONDITIONAL_INDEX_URL 307 $RELL_STATEMENTS_CONDITIONAL_INDEX_URL_SLASH 200 H1 $RELL_STATEMENTS_CONDITIONAL_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/tag INDEX ($LEARN_TAGS_TAG_INDEX_URL 301 $LEARN_TAGS_TAG_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_TAG_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("commands_library_index_docs", COMMANDS_LIBRARY_INDEX_URL)
        put("commands_library_index_url_slash", COMMANDS_LIBRARY_INDEX_URL_SLASH)
        put("commands_library_index_title", COMMANDS_LIBRARY_INDEX_TITLE)
        put("guide", GUIDE_URL)
        put("library_index_docs", LIBRARY_INDEX_URL)
        put("library_index_url_slash", LIBRARY_INDEX_URL_SLASH)
        put("library_index_title", LIBRARY_INDEX_TITLE)
        put(
            "commands",
            buildJsonObject {
                put("install", "chr library install")
                put("install_alias_of", "chr install")
                put("install_id", "chr library install $FT4_LIBRARY_ID")
                put("install_version", "chr library install $FT4_LIBRARY_ID@<semver>")
                put("list", "chr library list")
                put("view", "chr library view $FT4_LIBRARY_ID")
                put("versions", "chr library versions $FT4_LIBRARY_ID")
                put("view_iccf", "chr library view $ICCF_LIBRARY_ID")
                put("versions_iccf", "chr library versions $ICCF_LIBRARY_ID")
                put("install_iccf", "chr library install $ICCF_LIBRARY_ID@$ICCF_LIBRARY_CHAIN_VERSION")
            }
        )
        put(
            "flags",
            buildJsonObject {
                put("settings", "-s, --settings=<settings>")
                put("config", "-cfg, --config=<config>")
                put("url", "--url=<text>  # testnet | localhost | custom library-chain URL")
                put("brid", "-b, --brid=<value>  # library-chain RID; do not invent")
                put("library", "-lib, --library=<text>")
                put("force", "-f, --force  # skips RID check; not for production")
                put("limit", "-l, --limit=<int>")
                put("offset", "-o, --offset=<int>")
                put("sort_by", "--sort-by=(asc|desc)")
            }
        )
        put("library_chain_yaml", libraryChainYaml())
        put("git_yaml", gitYaml())
        put("iccf_library_chain_yaml", iccfLibraryChainYaml())
        put("iccf_git_yaml", iccfGitYaml())
        put("iccfLibraryChainId", ICCF_LIBRARY_ID)
        put("iccfLibraryChainVersion", ICCF_LIBRARY_CHAIN_VERSION)
        put("iccfGitTag", ICCF_GIT_TAG)
        put(
            "forbidden",
            buildJsonArray { DappScaffold.forbiddenModules.forEach { add(JsonPrimitive(it)) } }
        )
        put("ecosystem_container_management_index_url_slash", ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_URL_SLASH)
        put("ecosystem_container_management_index_title", ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_TITLE)
        put("ecosystem_add_replica_node_index_url_slash", ECOSYSTEM_ADD_REPLICA_NODE_INDEX_URL_SLASH)
        put("ecosystem_add_replica_node_index_title", ECOSYSTEM_ADD_REPLICA_NODE_INDEX_TITLE)
        put("ecosystem_pmc_commands_index_url_slash", ECOSYSTEM_PMC_COMMANDS_INDEX_URL_SLASH)
        put("ecosystem_pmc_commands_index_title", ECOSYSTEM_PMC_COMMANDS_INDEX_TITLE)
        put("ecosystem_pmc_transaction_index_url_slash", ECOSYSTEM_PMC_TRANSACTION_INDEX_URL_SLASH)
        put("ecosystem_pmc_transaction_index_title", ECOSYSTEM_PMC_TRANSACTION_INDEX_TITLE)
        put("learn_book_review_rell_structure_index_url_slash", LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_URL_SLASH)
        put("learn_book_review_rell_structure_index_title", LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_TITLE)
        put("learn_icmf_order_chain_index_url_slash", LEARN_ICMF_ORDER_CHAIN_INDEX_URL_SLASH)
        put("learn_icmf_order_chain_index_title", LEARN_ICMF_ORDER_CHAIN_INDEX_TITLE)

        put("learn_news_ft4_accounts_config_index_url_slash", LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_URL_SLASH)
        put("learn_news_ft4_accounts_config_index_title", LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_TITLE)

        put("rell_statements_conditional_index_url_slash", RELL_STATEMENTS_CONDITIONAL_INDEX_URL_SLASH)
        put("rell_statements_conditional_index_title", RELL_STATEMENTS_CONDITIONAL_INDEX_TITLE)
        put("learn_tags_tag_index_url_slash", LEARN_TAGS_TAG_INDEX_URL_SLASH)
        put("learn_tags_tag_index_title", LEARN_TAGS_TAG_INDEX_TITLE)
        put("notes", notes())
    }
}
// Official BUILD cli/commands/library encoded as COMMANDS_LIBRARY_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/container-management INDEX encoded as ECOSYSTEM_CONTAINER_MANAGEMENT_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/nodes/add-replica-node INDEX encoded as ECOSYSTEM_ADD_REPLICA_NODE_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc/commands INDEX encoded as ECOSYSTEM_PMC_COMMANDS_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/transaction INDEX encoded as ECOSYSTEM_PMC_TRANSACTION_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/book-review/rell-structure INDEX encoded as LEARN_BOOK_REVIEW_RELL_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/icmf-course/order-chain INDEX encoded as LEARN_ICMF_ORDER_CHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/create-accounts/install-configure-ft4 INDEX encoded as LEARN_NEWS_FT4_ACCOUNTS_CONFIG_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/statements/conditional-statements INDEX encoded as RELL_STATEMENTS_CONDITIONAL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/tag INDEX encoded as LEARN_TAGS_TAG_INDEX_* (query-only HELP ONLY WRITE SKIP).
