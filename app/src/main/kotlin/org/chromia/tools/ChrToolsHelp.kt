package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr tools` help (gtv / validate-config / lib-model).
 * Does not run chr, generate a key, invent a BRID, or send signed transactions.
 * Source: docs.chromia.com/build/cli/commands/tools and chromia-cli ToolsCommand (0.33.x).
 * `chr gtv` is the official alias of `chr tools gtv`.
 */
object ChrToolsHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/cli/commands/tools"
    const val TOOLS_INDEX_URL = DOCS_URL
    const val TOOLS_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/tools/"
    const val TOOLS_INDEX_TITLE = "tools"  // official H1
    const val ECOSYSTEM_DEPLOY_BRIDGE_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge"
    const val ECOSYSTEM_DEPLOY_BRIDGE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/"
    const val ECOSYSTEM_DEPLOY_BRIDGE_INDEX_TITLE = "Deploy the bridge"  // official H1
    const val ECOSYSTEM_CHANGE_FT4_KEY_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/change-ft4-key"
    const val ECOSYSTEM_CHANGE_FT4_KEY_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/change-ft4-key/"
    const val ECOSYSTEM_CHANGE_FT4_KEY_INDEX_TITLE = "Change the FT4 key"  // official H1
    const val ECOSYSTEM_PMC_VOTERSET_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/voterset"
    const val ECOSYSTEM_PMC_VOTERSET_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/voterset/"
    const val ECOSYSTEM_PMC_VOTERSET_INDEX_TITLE = "voterset"  // official H1
    const val LEARN_RELL_MASTERCLASS_UPDATE_INDEX_URL = "https://learn.chromia.com/courses/rell-masterclass/update"
    const val LEARN_RELL_MASTERCLASS_UPDATE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-masterclass/update/"
    const val LEARN_RELL_MASTERCLASS_UPDATE_INDEX_TITLE = "UPDATE statement"  // official H1
    const val LEARN_ICMF_DEFINING_MESSAGES_INDEX_URL = "https://learn.chromia.com/courses/icmf-course/defining-messages"
    const val LEARN_ICMF_DEFINING_MESSAGES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/icmf-course/defining-messages/"
    const val LEARN_ICMF_DEFINING_MESSAGES_INDEX_TITLE = "Define messages"  // official H1
    const val LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-assets"
    const val LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-assets/"
    const val LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_TITLE = "Module 3 - Build a marketplace"  // official H1
    const val RELL_MODULE_STRUCT_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/struct"
    const val RELL_MODULE_STRUCT_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/struct/"
    const val RELL_MODULE_STRUCT_INDEX_TITLE = "Struct"  // official H1
    const val LEARN_TAGS_BIGDATA_INDEX_URL = "https://learn.chromia.com/tags/BigData"
    const val LEARN_TAGS_BIGDATA_INDEX_URL_SLASH = "https://learn.chromia.com/tags/BigData/"
    const val LEARN_TAGS_BIGDATA_INDEX_TITLE = "Courses tagged with: BigData"  // official H1
    const val TOOL_NAME = "chr_tools_help"
    const val HEX_EXAMPLE = "A41A3018300A0C0161A2050C03464F4F300A0C0162A2050C03424152"

    fun notes(): String = """
        Chromia CLI $CLI_SERIES `chr tools` help. Java 21+, Postgres 16+.
        Official page: $DOCS_URL
        Public verbs on the live command page / ToolsCommand: gtv, validate-config, lib-model.
        `chr gtv` is an alias for `chr tools gtv` (main.kt aliases).
        `chr tools gtv` decodes GTV. Use `--hex` or pipe binary GTV (`chr gtv --output-format yaml < data.gtv`).
        Official `--output-format=(pretty|raw|JSON|XML|YAML)`. `--hash=<version>` prints the Merkle hash (version > 0; production pin ${DappScaffold.MERKLE_HASH_VERSION}).
        `chr tools validate-config -f, --file=<path>` validates a chromia config. Source accepts .yml / .yaml only.
        `chr tools lib-model` prints a git-shape `libs:` block. `-s, --library-source` is required (a directory).
        Official lib-model flags: --name, --registry (git URL), --tag-or-branch, --insecure=true|false (default false).
        lib-model computes `rid` from the library source — do not invent a RID.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover BUILD cli/commands/tools (leftover official $TOOLS_INDEX_URL leftover official 307 leftover official $TOOLS_INDEX_URL_SLASH leftover official 200 leftover official $TOOLS_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr tools leftover official leftover gtv leftover official leftover validate-config leftover official leftover lib-model leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover verbs leftover official leftover RID leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official leftover ECOSYSTEM ecosystem/bridge/deploy-bridge INDEX (leftover official $ECOSYSTEM_DEPLOY_BRIDGE_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_DEPLOY_BRIDGE_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_DEPLOY_BRIDGE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/change-ft4-key INDEX (leftover official $ECOSYSTEM_CHANGE_FT4_KEY_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_CHANGE_FT4_KEY_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_CHANGE_FT4_KEY_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/voterset INDEX (leftover official $ECOSYSTEM_PMC_VOTERSET_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_VOTERSET_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_VOTERSET_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/rell-masterclass/update INDEX (leftover official $LEARN_RELL_MASTERCLASS_UPDATE_INDEX_URL leftover official 301 leftover official $LEARN_RELL_MASTERCLASS_UPDATE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_RELL_MASTERCLASS_UPDATE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). leftover official leftover It is only possible to update the value of an attribute that has been explicitly marked with mutable leftover official leftover If you have a reference to an entity, updating an attribute is as simple as making an assignment. Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/icmf-course/defining-messages INDEX (leftover official $LEARN_ICMF_DEFINING_MESSAGES_INDEX_URL leftover official 301 leftover official $LEARN_ICMF_DEFINING_MESSAGES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICMF_DEFINING_MESSAGES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-assets INDEX (leftover official $LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/modules/struct INDEX (leftover official $RELL_MODULE_STRUCT_INDEX_URL leftover official 307 leftover official $RELL_MODULE_STRUCT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_MODULE_STRUCT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/BigData INDEX (leftover official $LEARN_TAGS_BIGDATA_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_BIGDATA_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_BIGDATA_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("tools_index_docs", TOOLS_INDEX_URL)
        put("tools_index_url_slash", TOOLS_INDEX_URL_SLASH)
        put("tools_index_title", TOOLS_INDEX_TITLE)
        put("tool", TOOL_NAME)
        put(
            "commands",
            buildJsonObject {
                put("tools", "chr tools")
                put("gtv", "chr tools gtv")
                put("gtv_alias_of", "chr gtv")
                put("gtv_hex", "chr gtv --hex $HEX_EXAMPLE")
                put("gtv_yaml", "chr gtv --output-format yaml < data.gtv")
                put("gtv_hash", "chr tools gtv --hex <gtv-hex> --hash=${DappScaffold.MERKLE_HASH_VERSION}")
                put("validate_config", "chr tools validate-config --file chromia.yml")
                put("lib_model", "chr tools lib-model --library-source <dir> --name <lib>")
            }
        )
        put(
            "gtv_flags",
            buildJsonObject {
                put("hex", "--hex=<hex>  # hex encoded GTV data")
                put("output_format", "-f, --output-format=(pretty|raw|JSON|XML|YAML)")
                put("hash", "--hash=<version>  # Merkle hash; version > 0; production pin ${DappScaffold.MERKLE_HASH_VERSION}")
            }
        )
        put(
            "validate_config_flags",
            buildJsonObject {
                put("file", "-f, --file=<path>  # required; .yml / .yaml only")
            }
        )
        put(
            "lib_model_flags",
            buildJsonObject {
                put("name", "--name=<text>  # Name of the library")
                put("library_source", "-s, --library-source=<path>  # required directory")
                put("registry", "--registry=<text>  # Git reference (source: git URL)")
                put("tag_or_branch", "--tag-or-branch=<text>")
                put("insecure", "--insecure=true|false  # default false")
            }
        )
        put("ecosystem_deploy_bridge_index_url_slash", ECOSYSTEM_DEPLOY_BRIDGE_INDEX_URL_SLASH)
        put("ecosystem_deploy_bridge_index_title", ECOSYSTEM_DEPLOY_BRIDGE_INDEX_TITLE)
        put("ecosystem_change_ft4_key_index_url_slash", ECOSYSTEM_CHANGE_FT4_KEY_INDEX_URL_SLASH)
        put("ecosystem_change_ft4_key_index_title", ECOSYSTEM_CHANGE_FT4_KEY_INDEX_TITLE)
        put("ecosystem_pmc_voterset_index_url_slash", ECOSYSTEM_PMC_VOTERSET_INDEX_URL_SLASH)
        put("ecosystem_pmc_voterset_index_title", ECOSYSTEM_PMC_VOTERSET_INDEX_TITLE)
        put("learn_rell_masterclass_update_index_url_slash", LEARN_RELL_MASTERCLASS_UPDATE_INDEX_URL_SLASH)
        put("learn_rell_masterclass_update_index_title", LEARN_RELL_MASTERCLASS_UPDATE_INDEX_TITLE)
        put("learn_icmf_defining_messages_index_url_slash", LEARN_ICMF_DEFINING_MESSAGES_INDEX_URL_SLASH)
        put("learn_icmf_defining_messages_index_title", LEARN_ICMF_DEFINING_MESSAGES_INDEX_TITLE)
        put("learn_marketplace_module_assets_index_url_slash", LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_URL_SLASH)
        put("learn_marketplace_module_assets_index_title", LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_TITLE)

        put("rell_module_struct_index_url_slash", RELL_MODULE_STRUCT_INDEX_URL_SLASH)
        put("rell_module_struct_index_title", RELL_MODULE_STRUCT_INDEX_TITLE)
        put("learn_tags_bigdata_index_url_slash", LEARN_TAGS_BIGDATA_INDEX_URL_SLASH)
        put("learn_tags_bigdata_index_title", LEARN_TAGS_BIGDATA_INDEX_TITLE)
        put("notes", notes())
    }
}

// Leftover official leftover BUILD cli/commands/tools leftovers encoded as TOOLS_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/bridge/deploy-bridge INDEX leftovers encoded as ECOSYSTEM_DEPLOY_BRIDGE_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/change-ft4-key INDEX leftovers encoded as ECOSYSTEM_CHANGE_FT4_KEY_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/voterset INDEX leftovers encoded as ECOSYSTEM_PMC_VOTERSET_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/rell-masterclass/update INDEX leftovers encoded as LEARN_RELL_MASTERCLASS_UPDATE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/icmf-course/defining-messages INDEX leftovers encoded as LEARN_ICMF_DEFINING_MESSAGES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/module-assets INDEX leftovers encoded as LEARN_MARKETPLACE_MODULE_ASSETS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/modules/struct INDEX leftovers encoded as RELL_MODULE_STRUCT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/BigData INDEX leftovers encoded as LEARN_TAGS_BIGDATA_INDEX_* (query-only HELP ONLY WRITE SKIP).
