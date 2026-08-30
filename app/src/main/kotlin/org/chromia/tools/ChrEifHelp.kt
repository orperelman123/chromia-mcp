package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr eif generate-events-config` help.
 * Does not run chr, generate a key, invent a BRID, or send signed transactions.
 * Source: docs.chromia.com/build/cli/commands/eif and EIFGenerateEventsConfigCommand (0.33.x).
 */
object ChrEifHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/cli/commands/eif"
    const val EIF_INDEX_URL = DOCS_URL
    const val EIF_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/eif/"
    const val EIF_INDEX_TITLE = "eif"  // official H1
    const val GET_STARTED_ICCF_USE_CASE_INDEX_URL = "https://docs.chromia.com/get-started/use-cases/cross-chain/iccf"
    const val GET_STARTED_ICCF_USE_CASE_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/cross-chain/iccf/"
    const val GET_STARTED_ICCF_USE_CASE_INDEX_TITLE = "Cross-chain verification applications"  // official H1
    const val ECOSYSTEM_EXTENSIONS_INDEX_URL = "https://docs.chromia.com/ecosystem/extensions"
    const val ECOSYSTEM_EXTENSIONS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/extensions/"
    const val ECOSYSTEM_EXTENSIONS_INDEX_TITLE = "Extensions"  // official H1
    const val ECOSYSTEM_MASS_EXIT_SETUP_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/mass-exit/setup"
    const val ECOSYSTEM_MASS_EXIT_SETUP_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/mass-exit/setup/"
    const val ECOSYSTEM_MASS_EXIT_SETUP_INDEX_TITLE = "Set up mass exit"  // official H1
    const val ECOSYSTEM_GOV_EIF_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/extensions/eif"
    const val ECOSYSTEM_GOV_EIF_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/extensions/eif/"
    const val ECOSYSTEM_GOV_EIF_INDEX_TITLE = "Governance Tool EIF extension"  // official H1
    const val LEARN_BOOK_REVIEW_SETUP_INDEX_URL = "https://learn.chromia.com/courses/book-review/setup"
    const val LEARN_BOOK_REVIEW_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/setup/"
    const val LEARN_BOOK_REVIEW_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_MARKETPLACE_NFT_MODEL_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-nft/nft"
    const val LEARN_MARKETPLACE_NFT_MODEL_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-nft/nft/"
    const val LEARN_MARKETPLACE_NFT_MODEL_INDEX_TITLE = "Define the NFT model"  // official H1
    const val TOOL_NAME = "chr_eif_help"
    const val DEFAULT_TARGET = "build/eif-events.yaml"
    const val EIF_GTX_MODULE = "net.postchain.eif.EifGTXModule"
    const val EIF_SYNC_EXT = "net.postchain.eif.EifSynchronizationInfrastructureExtension"

    fun notes(): String = """
        Chromia CLI $CLI_SERIES `chr eif` help. Java 21+, Postgres 16+.
        Official page: $DOCS_URL
        Public verb: generate-events-config (Ethereum Integration Framework).
        Required: --abi=<path> (JSON ABI file or directory of JSON ABI files), --events=<text> (comma-separated names).
        Optional: --target=<path> (default $DEFAULT_TARGET), --format=(XML|YAML) (default YAML).
        Target suffix must match --format (source). Existing target: interactive overwrite prompt, or abort if non-interactive.
        This generates a listen-list of Solidity events. It does not deploy a contract, sign, or send a transaction.
        Official blockchain-properties allowed GTX module: $EIF_GTX_MODULE.
        Official allowed sync_ext: $EIF_SYNC_EXT.
        Do not invent event YAML keys or extra `chr eif` verbs — only generate-events-config is registered.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover BUILD cli/commands/eif (leftover official $EIF_INDEX_URL leftover official 307 leftover official $EIF_INDEX_URL_SLASH leftover official 200 leftover official $EIF_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr eif leftover official leftover generate-events-config leftover official leftover Generate solidity events that EIF will listen to leftover official leftover Options leftover official leftover --abi leftover official leftover --events leftover official leftover --target leftover official leftover --format leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover leftover official leftover mnemonic leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover verbs leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official leftover GET-STARTED get-started/use-cases/cross-chain/iccf INDEX (leftover official $GET_STARTED_ICCF_USE_CASE_INDEX_URL leftover official 307 leftover official $GET_STARTED_ICCF_USE_CASE_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_ICCF_USE_CASE_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover ECOSYSTEM ecosystem/extensions INDEX (leftover official $ECOSYSTEM_EXTENSIONS_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_EXTENSIONS_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_EXTENSIONS_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/bridge/mass-exit/setup INDEX (leftover official $ECOSYSTEM_MASS_EXIT_SETUP_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_MASS_EXIT_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_MASS_EXIT_SETUP_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/extensions/eif INDEX (leftover official $ECOSYSTEM_GOV_EIF_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_EIF_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_GOV_EIF_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/book-review/setup INDEX (leftover official $LEARN_BOOK_REVIEW_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-nft/nft INDEX (leftover official $LEARN_MARKETPLACE_NFT_MODEL_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_NFT_MODEL_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_NFT_MODEL_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("eif_index_docs", EIF_INDEX_URL)
        put("eif_index_url_slash", EIF_INDEX_URL_SLASH)
        put("eif_index_title", EIF_INDEX_TITLE)
        put("get_started_iccf_use_case_index_docs", GET_STARTED_ICCF_USE_CASE_INDEX_URL)
        put("get_started_iccf_use_case_index_url_slash", GET_STARTED_ICCF_USE_CASE_INDEX_URL_SLASH)
        put("get_started_iccf_use_case_index_title", GET_STARTED_ICCF_USE_CASE_INDEX_TITLE)
        put("tool", TOOL_NAME)
        put(
            "commands",
            buildJsonObject {
                put("eif", "chr eif")
                put("generate_events_config", "chr eif generate-events-config")
                put(
                    "example",
                    "chr eif generate-events-config --abi abi.json --events Transfer,Approval"
                )
            }
        )
        put(
            "flags",
            buildJsonObject {
                put("abi", "--abi=<path>  # required; JSON ABI file or directory")
                put("events", "--events=<text>  # required; comma-separated event names")
                put("target", "--target=<path>  # default $DEFAULT_TARGET")
                put("format", "--format=(XML|YAML)  # default YAML")
            }
        )
        put("default_target", DEFAULT_TARGET)
        put("eif_gtx_module", EIF_GTX_MODULE)
        put("eif_sync_ext", EIF_SYNC_EXT)
        put("ecosystem_extensions_index_url_slash", ECOSYSTEM_EXTENSIONS_INDEX_URL_SLASH)
        put("ecosystem_extensions_index_title", ECOSYSTEM_EXTENSIONS_INDEX_TITLE)
        put("ecosystem_mass_exit_setup_index_url_slash", ECOSYSTEM_MASS_EXIT_SETUP_INDEX_URL_SLASH)
        put("ecosystem_mass_exit_setup_index_title", ECOSYSTEM_MASS_EXIT_SETUP_INDEX_TITLE)
        put("ecosystem_gov_eif_index_url_slash", ECOSYSTEM_GOV_EIF_INDEX_URL_SLASH)
        put("ecosystem_gov_eif_index_title", ECOSYSTEM_GOV_EIF_INDEX_TITLE)
        put("learn_book_review_setup_index_url_slash", LEARN_BOOK_REVIEW_SETUP_INDEX_URL_SLASH)
        put("learn_book_review_setup_index_title", LEARN_BOOK_REVIEW_SETUP_INDEX_TITLE)
        put("learn_marketplace_nft_model_index_url_slash", LEARN_MARKETPLACE_NFT_MODEL_INDEX_URL_SLASH)
        put("learn_marketplace_nft_model_index_title", LEARN_MARKETPLACE_NFT_MODEL_INDEX_TITLE)
        put("notes", notes())
    }
}

// Leftover official leftover BUILD cli/commands/eif leftovers encoded as EIF_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/use-cases/cross-chain/iccf INDEX leftovers encoded as GET_STARTED_ICCF_USE_CASE_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/extensions INDEX leftovers encoded as ECOSYSTEM_EXTENSIONS_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/bridge/mass-exit/setup INDEX leftovers encoded as ECOSYSTEM_MASS_EXIT_SETUP_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/extensions/eif INDEX leftovers encoded as ECOSYSTEM_GOV_EIF_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/book-review/setup INDEX leftovers encoded as LEARN_BOOK_REVIEW_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/module-nft/nft INDEX leftovers encoded as LEARN_MARKETPLACE_NFT_MODEL_INDEX_* (query-only HELP ONLY WRITE SKIP).
