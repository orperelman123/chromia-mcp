package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x read-only `chr multi-signature view` help.
 * Source: docs.chromia.com/build/cli/commands/multi-signature + MultiSignatureViewCommand.
 * Does not run chr, generate a key, invent a BRID, or send signed transactions.
 *
 * Skipped (sign / send): multi-signature create, sign, send; chr tx; chr keygen.
 */
object ChrMultiSignatureHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/cli/commands/multi-signature"
    const val MULTI_SIGNATURE_INDEX_URL = DOCS_URL
    const val MULTI_SIGNATURE_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/multi-signature/"
    const val MULTI_SIGNATURE_INDEX_TITLE = "multi-signature"  // official H1
    const val ECOSYSTEM_START_NODE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/start-a-node"
    const val ECOSYSTEM_START_NODE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/start-a-node/"
    const val ECOSYSTEM_START_NODE_INDEX_TITLE = "Start a node"  // official H1
    const val ECOSYSTEM_PROVIDER_AUTH_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/blockchain-based-provider-authentication"
    const val ECOSYSTEM_PROVIDER_AUTH_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/blockchain-based-provider-authentication/"
    const val ECOSYSTEM_PROVIDER_AUTH_INDEX_TITLE = "Blockchain authentication of Directory Chain operations"  // official H1
    const val ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/cluster-anchoring"
    const val ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/cluster-anchoring/"
    const val ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_TITLE = "cluster-anchoring"  // official H1
    const val LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_URL = "https://learn.chromia.com/courses/book-review/what-next"
    const val LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/what-next/"
    const val LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_TITLE = "What’s next?"  // official H1
    const val LEARN_ICMF_INTRODUCTION_INDEX_URL = "https://learn.chromia.com/courses/icmf-course/introduction"
    const val LEARN_ICMF_INTRODUCTION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/icmf-course/introduction/"
    const val LEARN_ICMF_INTRODUCTION_INDEX_TITLE = "Build an event-driven multi-blockchain dapp"  // official H1
    const val LEARN_MARKETPLACE_INTRO_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/introduction"
    const val LEARN_MARKETPLACE_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/introduction/"
    const val LEARN_MARKETPLACE_INTRO_INDEX_TITLE = "Build a decentralized marketplace using FT4"  // official H1
    const val LEARN_TAGS_AI_INDEX_URL = "https://learn.chromia.com/tags/AI"
    const val LEARN_TAGS_AI_INDEX_URL_SLASH = "https://learn.chromia.com/tags/AI/"
    const val LEARN_TAGS_AI_INDEX_TITLE = "Courses tagged with: AI"  // official H1
    const val TOOL_NAME = "chr_multi_signature_help"

    fun viewExample(): String = "chr multi-signature view --file <transaction-file>"

    fun notes(): String = """
        Chromia CLI $CLI_SERIES read-only `chr multi-signature view` help. Java 21+, Postgres 16+.
        Official page: $DOCS_URL
        `chr multi-signature view` views an existing transaction file. Official flag: -f, --file=<path> (required).
        Source MultiSignatureViewCommand prints JSON (transactionRID, blockchainRID, operations, signers, signatures).
        No --secret / --key-id on the live view section. This tool does not sign or send.
        Skipped (sign / send): `chr multi-signature create`, `sign`, `send`.
        Skipped: `chr tx` (signs and runs transactions). keygen is forbidden and is not documented.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official BUILD cli/commands/multi-signature ($MULTI_SIGNATURE_INDEX_URL 307 $MULTI_SIGNATURE_INDEX_URL_SLASH 200 $MULTI_SIGNATURE_INDEX_TITLE): intro Usage chr multi-signature view WRITE SKIP create sign send Query-only skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no keygen do not invent flags do not document chr tx signed send keygen samples.
        Official ECOSYSTEM ecosystem/providers/nodes/start-a-node INDEX ($ECOSYSTEM_START_NODE_INDEX_URL 307 $ECOSYSTEM_START_NODE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_START_NODE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/blockchain-based-provider-authentication INDEX ($ECOSYSTEM_PROVIDER_AUTH_INDEX_URL 307 $ECOSYSTEM_PROVIDER_AUTH_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PROVIDER_AUTH_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/cluster-anchoring INDEX ($ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_URL 307 $ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/what-next INDEX ($LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_URL 301 $LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_TITLE HELP ONLY WRITE SKIP). Congratulations! You have successfully completed the course Now that you have a foundational understanding of Chromia, here are some suggested next steps Explore more advanced features and concepts of Chromia. Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/icmf-course/introduction INDEX ($LEARN_ICMF_INTRODUCTION_INDEX_URL 301 $LEARN_ICMF_INTRODUCTION_INDEX_URL_SLASH 200 H1 $LEARN_ICMF_INTRODUCTION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/marketplace-course/introduction INDEX ($LEARN_MARKETPLACE_INTRO_INDEX_URL 301 $LEARN_MARKETPLACE_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_MARKETPLACE_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/AI INDEX ($LEARN_TAGS_AI_INDEX_URL 301 $LEARN_TAGS_AI_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_AI_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("multi_signature_index_docs", MULTI_SIGNATURE_INDEX_URL)
        put("multi_signature_index_url_slash", MULTI_SIGNATURE_INDEX_URL_SLASH)
        put("multi_signature_index_title", MULTI_SIGNATURE_INDEX_TITLE)
        put("tool", TOOL_NAME)
        put(
            "commands",
            buildJsonObject {
                put("view", viewExample())
            }
        )
        put(
            "flags",
            buildJsonObject {
                put(
                    "view",
                    buildJsonObject {
                        put("file", "-f, --file=<path>  # required path to an existing transaction file")
                    }
                )
            }
        )
        put(
            "skipped",
            buildJsonObject {
                put("create", "chr multi-signature create — signs a new transaction; skipped")
                put("sign", "chr multi-signature sign — signs an existing transaction file; skipped")
                put("send", "chr multi-signature send — sends a fully signed transaction; skipped")
                put("tx", "chr tx — signs and runs transactions; skipped")
                put("keygen", "keygen — generates a key pair; forbidden; not documented")
            }
        )
        put("ecosystem_start_node_index_url_slash", ECOSYSTEM_START_NODE_INDEX_URL_SLASH)
        put("ecosystem_start_node_index_title", ECOSYSTEM_START_NODE_INDEX_TITLE)
        put("ecosystem_provider_auth_index_url_slash", ECOSYSTEM_PROVIDER_AUTH_INDEX_URL_SLASH)
        put("ecosystem_provider_auth_index_title", ECOSYSTEM_PROVIDER_AUTH_INDEX_TITLE)
        put("ecosystem_pmc_cluster_anchoring_index_url_slash", ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_URL_SLASH)
        put("ecosystem_pmc_cluster_anchoring_index_title", ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_TITLE)
        put("learn_book_review_what_next_index_url_slash", LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_URL_SLASH)
        put("learn_book_review_what_next_index_title", LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_TITLE)
        put("learn_icmf_introduction_index_url_slash", LEARN_ICMF_INTRODUCTION_INDEX_URL_SLASH)
        put("learn_icmf_introduction_index_title", LEARN_ICMF_INTRODUCTION_INDEX_TITLE)
        put("learn_marketplace_intro_index_url_slash", LEARN_MARKETPLACE_INTRO_INDEX_URL_SLASH)
        put("learn_marketplace_intro_index_title", LEARN_MARKETPLACE_INTRO_INDEX_TITLE)

        put("learn_tags_ai_index_url_slash", LEARN_TAGS_AI_INDEX_URL_SLASH)
        put("learn_tags_ai_index_title", LEARN_TAGS_AI_INDEX_TITLE)
        put("notes", notes())
    }
}

// Official BUILD cli/commands/multi-signature encoded as MULTI_SIGNATURE_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/nodes/start-a-node INDEX encoded as ECOSYSTEM_START_NODE_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/blockchain-based-provider-authentication INDEX encoded as ECOSYSTEM_PROVIDER_AUTH_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/cluster-anchoring INDEX encoded as ECOSYSTEM_PMC_CLUSTER_ANCHORING_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/book-review/what-next INDEX encoded as LEARN_BOOK_REVIEW_WHAT_NEXT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/icmf-course/introduction INDEX encoded as LEARN_ICMF_INTRODUCTION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/marketplace-course/introduction INDEX encoded as LEARN_MARKETPLACE_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/AI INDEX encoded as LEARN_TAGS_AI_INDEX_* (query-only HELP ONLY WRITE SKIP).
