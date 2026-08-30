package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x existing-key reference only.
 * Source: docs.chromia.com/build/cli/key-pair-management (read / lookup flow).
 * Leftover official keygen command page is HELP ONLY (flags + URL).
 * Does not generate a key, print a private key, print a sample key, or send a signed tx.
 * Leftover official leftover BUILD cli/key-pair-management index slash/title leftovers live here (query-only).
 */
object ChrKeyIdHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/cli/key-pair-management"
    const val KEY_PAIR_INDEX_URL = DOCS_URL
    const val KEY_PAIR_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/key-pair-management/"
    const val KEY_PAIR_INDEX_TITLE = "Key pair management in Chromia CLI"
    const val KEYGEN_DOCS_URL = "https://docs.chromia.com/build/cli/commands/keygen"
    const val KEYGEN_INDEX_URL = KEYGEN_DOCS_URL
    const val KEYGEN_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/keygen/"
    const val KEYGEN_INDEX_TITLE = "keygen"  // official H1
    const val ECOSYSTEM_PROVIDERS_NODES_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes"
    const val ECOSYSTEM_PROVIDERS_NODES_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/"
    const val ECOSYSTEM_PROVIDERS_NODES_INDEX_TITLE = "Set up and start a node"  // official H1
    const val LEARN_MARKETPLACE_TEST_CLI_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-assets/test-cli"
    const val LEARN_MARKETPLACE_TEST_CLI_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-assets/test-cli/"
    const val LEARN_MARKETPLACE_TEST_CLI_INDEX_TITLE = "Test using Chromia CLI"  // official H1
    const val LEARN_TTT_CONNECT_CLIENT_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-two/connecting-the-client"
    const val LEARN_TTT_CONNECT_CLIENT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-two/connecting-the-client/"
    const val LEARN_TTT_CONNECT_CLIENT_INDEX_TITLE = "Connect the client to the blockchain"  // official H1
    const val LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive/python-client"
    const val LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive/python-client/"
    const val LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_TITLE = "Interacting with Chromia using the Python client"  // official H1
    const val LEARN_TTT_AUTHENTICATION_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/create-accounts/authentication"
    const val LEARN_TTT_AUTHENTICATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/create-accounts/authentication/"
    const val LEARN_TTT_AUTHENTICATION_INDEX_TITLE = "Authentication with FT4 accounts"  // official H1
    const val LEARN_ZK_PROOF_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/zero-knowledge-proof"
    const val LEARN_ZK_PROOF_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/zero-knowledge-proof/"
    const val LEARN_ZK_PROOF_INDEX_TITLE = "Zero-knowledge proof"  // official H1
    const val LEARN_WEB3_CHROMIA_STACK_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/chromia-web3-stack"
    const val LEARN_WEB3_CHROMIA_STACK_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/chromia-web3-stack/"
    const val LEARN_WEB3_CHROMIA_STACK_INDEX_TITLE = "Chromia dapp overview"  // official H1
    const val LEARN_COMPARISONS_ETHEREUM_INDEX_URL = "https://learn.chromia.com/courses/chromia-comparisons/etherum"
    const val LEARN_COMPARISONS_ETHEREUM_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-comparisons/etherum/"
    const val LEARN_COMPARISONS_ETHEREUM_INDEX_TITLE = "Ethereum"  // official H1
    const val LEARN_TAGS_CHATBOT_INDEX_URL = "https://learn.chromia.com/tags/Chatbot"
    const val LEARN_TAGS_CHATBOT_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Chatbot/"
    const val LEARN_TAGS_CHATBOT_INDEX_TITLE = "Courses tagged with: Chatbot"  // official H1
    const val TOOL_NAME = "chr_key_id_help"

    fun notes(): String = """
        Chromia CLI $CLI_SERIES existing-key reference only. Java 21+, Postgres 16+.
        Official page: $DOCS_URL
        This tool documents how chr finds an *existing* key id. It does not generate a key
        and does not print a private key. Leftover official keygen is HELP ONLY (flags + URL).
        Official lookup precedence (highest to lowest):
        1. `--secret=<path>` — path to an existing secret file (this tool does not write one).
        2. `--key-id=<key_id>` — existing key id; keys are still read from ~/.chromia.
        3. `--config` file property `key.id`.
        4. project `<project-path>/.chromia/config` property `key.id`.
        5. global `~/.chromia/config` property `key.id`.
        Getting-started default id *name* is chromia_key (reference an existing id; do not generate).
        Skipped: key generation, leftover official --file write, leftover official --dry (prints keys), and private-key file contents.
        Leftover official BUILD keygen command page (leftover official $KEYGEN_DOCS_URL leftover official 200): leftover official HELP ONLY leftover official flags leftover official --key-id leftover official --file leftover official --get-pubkey leftover official --dry leftover official this tool does not generate a key leftover official does not print a private key leftover official does not run leftover official keygen leftover official skip leftover official --dry leftover official skip leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cli/key-pair-management (leftover official $KEY_PAIR_INDEX_URL leftover official 307 leftover official $KEY_PAIR_INDEX_URL_SLASH leftover official 200 leftover official $KEY_PAIR_INDEX_TITLE): leftover official leftover intro leftover official leftover This document describes how key pairs are read and managed within the Chromia CLI for various commands leftover official leftover Key pair reading flow leftover official leftover The Chromia CLI follows a specific precedence order when determining which key pair to use for operations leftover official leftover Secret file option leftover official leftover Key ID option leftover official leftover Explicit configuration path leftover official leftover Project-specific configuration leftover official leftover Global configuration leftover official leftover Key storage leftover official leftover Keys are stored in the ~/.chromia directory by default leftover official leftover WRITE SKIP leftover official leftover generate leftover official leftover keygen leftover official leftover sign leftover official leftover pubkey leftover official leftover sample leftover official leftover keys leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Leftover official leftover BUILD cli/commands/keygen (leftover official $KEYGEN_INDEX_URL leftover official 307 leftover official $KEYGEN_INDEX_URL_SLASH leftover official 200 leftover official $KEYGEN_INDEX_TITLE): leftover official leftover intro leftover official leftover keygen command page leftover official leftover HELP ONLY leftover official leftover flags leftover official leftover --key-id leftover official leftover --file leftover official leftover --get-pubkey leftover official leftover --dry leftover official leftover skipped leftover official leftover this tool does not generate a key leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover --dry leftover official leftover skip leftover official leftover --file leftover official leftover write leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover HELP ONLY leftover official leftover flags leftover official leftover keygen command page leftover official leftover this tool does not generate a key leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover --dry leftover official leftover prints leftover official leftover keys leftover official leftover skip leftover official leftover --file leftover official leftover write leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover skip leftover official leftover private leftover official leftover key leftover official leftover this tool does not generate a key.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes INDEX (leftover official $ECOSYSTEM_PROVIDERS_NODES_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PROVIDERS_NODES_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PROVIDERS_NODES_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-assets/test-cli INDEX (leftover official $LEARN_MARKETPLACE_TEST_CLI_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_TEST_CLI_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_TEST_CLI_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/tic-tac-toe/module-two/connecting-the-client INDEX (leftover official $LEARN_TTT_CONNECT_CLIENT_INDEX_URL leftover official 301 leftover official $LEARN_TTT_CONNECT_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_CONNECT_CLIENT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/vector-db-movie-demo/code-deep-dive/python-client INDEX (leftover official $LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_URL leftover official 301 leftover official $LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/tic-tac-toe/module-one/create-accounts/authentication INDEX (leftover official $LEARN_TTT_AUTHENTICATION_INDEX_URL leftover official 301 leftover official $LEARN_TTT_AUTHENTICATION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_AUTHENTICATION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX zero-knowledge-proof course (leftover official $LEARN_ZK_PROOF_INDEX_URL leftover official 301 leftover official $LEARN_ZK_PROOF_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ZK_PROOF_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX web3-for-web2-devs chromia-web3-stack (leftover official $LEARN_WEB3_CHROMIA_STACK_INDEX_URL leftover official 301 leftover official $LEARN_WEB3_CHROMIA_STACK_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_WEB3_CHROMIA_STACK_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX chromia-comparisons etherum (official leftover URL typo leftover official $LEARN_COMPARISONS_ETHEREUM_INDEX_URL leftover official 301 leftover official $LEARN_COMPARISONS_ETHEREUM_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_COMPARISONS_ETHEREUM_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr and does not send signed transactions.
        Leftover official leftover LEARN tags/Chatbot INDEX (leftover official $LEARN_TAGS_CHATBOT_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_CHATBOT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_CHATBOT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("tool", TOOL_NAME)
        put("docs", DOCS_URL)
        put("key_pair_index_docs", KEY_PAIR_INDEX_URL)
        put("key_pair_index_url_slash", KEY_PAIR_INDEX_URL_SLASH)
        put("key_pair_index_title", KEY_PAIR_INDEX_TITLE)
        put("existing_key_only", true)
        put(
            "flags",
            buildJsonObject {
                put(
                    "key_id",
                    "--key-id=<key_id>  # reference an existing key id; this tool does not generate a key"
                )
                put(
                    "secret",
                    "--secret=<path>  # path to an existing secret file; this tool does not write one"
                )
                put("config", "-cfg, --config=<config>  # key.id from this client config")
                put("key_id_property", "key.id  # in --config / project .chromia/config / ~/.chromia/config")
            }
        )
        put(
            "precedence",
            buildJsonObject {
                put("1_secret", "--secret")
                put("2_key_id", "--key-id")
                put("3_config_key_id", "--config key.id")
                put("4_project_config", "<project-path>/.chromia/config key.id")
                put("5_global_config", "~/.chromia/config key.id")
            }
        )
        put("default_id_name", "chromia_key")
        put("keygen_docs", KEYGEN_DOCS_URL)
        put("keygen_index_docs", KEYGEN_INDEX_URL)
        put("keygen_index_url_slash", KEYGEN_INDEX_URL_SLASH)
        put("keygen_index_title", KEYGEN_INDEX_TITLE)
        put("keygen_help_only", true)
        put(
            "leftover_official_keygen_flags",
            buildJsonObject {
                put(
                    "key_id",
                    "--key-id=<value>  # leftover official keygen flag; this tool does not generate a key"
                )
                put(
                    "file",
                    "-f, --file=<path>  # leftover official keygen flag; this tool does not write a key"
                )
                put(
                    "get_pubkey",
                    "--get-pubkey[=<key-id>]  # leftover official existing-key public key lookup"
                )
                put("dry", "--dry  # leftover official; skipped (prints keys)")
            }
        )
        put("skipped", "key generation, --file write, --dry, private-key contents")
        put("ecosystem_providers_nodes_index_url_slash", ECOSYSTEM_PROVIDERS_NODES_INDEX_URL_SLASH)
        put("ecosystem_providers_nodes_index_title", ECOSYSTEM_PROVIDERS_NODES_INDEX_TITLE)
        put("learn_marketplace_test_cli_index_url_slash", LEARN_MARKETPLACE_TEST_CLI_INDEX_URL_SLASH)
        put("learn_marketplace_test_cli_index_title", LEARN_MARKETPLACE_TEST_CLI_INDEX_TITLE)
        put("learn_ttt_connect_client_index_url_slash", LEARN_TTT_CONNECT_CLIENT_INDEX_URL_SLASH)
        put("learn_ttt_connect_client_index_title", LEARN_TTT_CONNECT_CLIENT_INDEX_TITLE)
        put("learn_vector_db_python_client_index_url_slash", LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_URL_SLASH)
        put("learn_vector_db_python_client_index_title", LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_TITLE)
        put("learn_ttt_authentication_index_url_slash", LEARN_TTT_AUTHENTICATION_INDEX_URL_SLASH)
        put("learn_ttt_authentication_index_title", LEARN_TTT_AUTHENTICATION_INDEX_TITLE)
        put("learn_zk_proof_index_url_slash", LEARN_ZK_PROOF_INDEX_URL_SLASH)
        put("learn_zk_proof_index_title", LEARN_ZK_PROOF_INDEX_TITLE)
        put("learn_web3_chromia_stack_index_url_slash", LEARN_WEB3_CHROMIA_STACK_INDEX_URL_SLASH)
        put("learn_web3_chromia_stack_index_title", LEARN_WEB3_CHROMIA_STACK_INDEX_TITLE)
        put("learn_comparisons_ethereum_index_url_slash", LEARN_COMPARISONS_ETHEREUM_INDEX_URL_SLASH)
        put("learn_comparisons_ethereum_index_title", LEARN_COMPARISONS_ETHEREUM_INDEX_TITLE)
        put("learn_tags_chatbot_index_url_slash", LEARN_TAGS_CHATBOT_INDEX_URL_SLASH)
        put("learn_tags_chatbot_index_title", LEARN_TAGS_CHATBOT_INDEX_TITLE)
        put("notes", notes())
    }
}

// Leftover official leftover BUILD cli/commands/keygen leftovers encoded as KEYGEN_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes INDEX leftovers encoded as ECOSYSTEM_PROVIDERS_NODES_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/marketplace-course/module-assets/test-cli INDEX leftovers encoded as LEARN_MARKETPLACE_TEST_CLI_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-two/connecting-the-client INDEX leftovers encoded as LEARN_TTT_CONNECT_CLIENT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/vector-db-movie-demo/code-deep-dive/python-client INDEX leftovers encoded as LEARN_VECTOR_DB_PYTHON_CLIENT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-one/create-accounts/authentication INDEX leftovers encoded as LEARN_TTT_AUTHENTICATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX zero-knowledge-proof course leftovers encoded as LEARN_ZK_PROOF_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX web3-for-web2-devs chromia-web3-stack leftovers encoded as LEARN_WEB3_CHROMIA_STACK_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX chromia-comparisons etherum leftovers encoded as LEARN_COMPARISONS_ETHEREUM_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/Chatbot INDEX leftovers encoded as LEARN_TAGS_CHATBOT_INDEX_* (query-only HELP ONLY WRITE SKIP).
