package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Production FT4 v1.1.0r API 1 module_args + libs block.
 * Source: gitlab.com/chromaway/ft4-lib tag v1.1.0r (compiler binds Rell structs).
 * Never emits lib.ft4.admin / admin.crosschain / ras_open / ras_transfer_open.
 * require_mandatory_flags is main-descriptor only; DEFAULT_LOGIN_CONFIG_NAME is "default".
 * includeIccf emits official IccfGTXModule plus protocol-page library-chain com.chromia.iccf 1.90.1;
 * FT4 setup git pin 1.87.0 is documented separately (do not invent versions).
 */
object Ft4ModuleArgs {
    const val DEFAULT_LOGIN_CONFIG_NAME = "default"
    const val REQUIRE_MANDATORY_FLAGS_SCOPE = "main descriptor only"
    const val ICCF_GTX_MODULE = "net.postchain.d1.iccf.IccfGTXModule"
    const val ICCF_PROTOCOL_URL = "https://docs.chromia.com/get-started/about/protocols/iccf"
    const val ICCF_PROPERTIES_URL = "https://docs.chromia.com/build/configuration/blockchain-properties"
    const val ICCF_FT4_SETUP_URL = "https://docs.chromia.com/build/ft4/setup/ft4-setup"
    const val IMPORTS_URL = "https://docs.chromia.com/build/ft4/setup/imports"
    const val CONFIG_VALUES_URL = "https://docs.chromia.com/build/ft4/configuration-values"
    const val RELEASES_URL = "https://docs.chromia.com/build/ft4/releases/ft4"
    const val RELEASES_404_URL = "https://docs.chromia.com/build/ft4/releases"
    const val DOCS_LATEST_FT4 = "1.1.0r"
    const val DOCS_LATEST_FT4_DATE = "2025-02-25"
    const val ICCF_GIT_TAG = "1.87.0"
    const val ICCF_LIBRARY_CHAIN_ID = "com.chromia.iccf"
    const val ICCF_LIBRARY_CHAIN_VERSION = "1.90.1"
    const val ICCF_GIT_REGISTRY = "https://gitlab.com/chromaway/core/directory-chain"
    const val ICCF_GIT_PATH = "src/lib/iccf"
    const val ICCF_GIT_RID = "x\"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D\""
    const val ICCF_IMPORT = "lib.iccf"

    fun libraryChainYaml(): String = """
        libs:
          $ICCF_LIBRARY_CHAIN_ID:
            version: $ICCF_LIBRARY_CHAIN_VERSION
    """.trimIndent() + "\n"

    fun gitIccfYaml(): String = """
        libs:
          iccf:
            registry: $ICCF_GIT_REGISTRY
            path: $ICCF_GIT_PATH
            tagOrBranch: $ICCF_GIT_TAG
            rid: $ICCF_GIT_RID
            insecure: false
    """.trimIndent() + "\n"

    fun libsYaml(includeIccf: Boolean): String {
        val ft4 = """
            libs:
              ft4:
                registry: ${DappScaffold.FT4_REGISTRY}
                path: ${DappScaffold.FT4_PATH}
                tagOrBranch: ${DappScaffold.FT4_VERSION}
                rid: ${DappScaffold.FT4_RID}
                insecure: false
        """.trimIndent()
        if (!includeIccf) return ft4 + "\n"
        // Official ICCF protocol page (source of truth for ICCF) uses library-chain 1.90.1.
        // Official FT4 setup still documents git 1.87.0 — see gitIccfYaml().
        return ft4 + "\n" + """
              $ICCF_LIBRARY_CHAIN_ID:
                version: $ICCF_LIBRARY_CHAIN_VERSION
        """.trimIndent().prependIndent("  ") + "\n"
    }

    fun gtxConfigYaml(): String = """
        config:
          gtx:
            modules:
              - "$ICCF_GTX_MODULE"
    """.trimIndent() + "\n"

    fun moduleArgsYaml(): String = """
        moduleArgs:
          lib.ft4:
            query_max_page_size: 100
          lib.ft4.core.accounts:
            rate_limit:
              active: true
              max_points: 10
              recovery_time: 5000
              points_at_account_creation: 1
            auth_descriptor:
              max_rules: 8
              max_number_per_account: 10
            auth_flags:
              mandatory: ["A", "T"]
    """.trimIndent() + "\n"

    fun notes(name: String, includeIccf: Boolean = false): String = """
        FT4 ${DappScaffold.FT4_VERSION} API ${DappScaffold.FT4_API} module_args / libs for `$name`.
        Compiler binds these keys to Rell struct module_args (source tag ${DappScaffold.FT4_VERSION}).
        auth_flags.mandatory default is [A, T]. require_mandatory_flags runs only on the main auth descriptor
        (create_account_with_auth / update_main_auth_descriptor), not on login/disposable descriptors.
        DEFAULT_LOGIN_CONFIG_NAME is "$DEFAULT_LOGIN_CONFIG_NAME" (source constant; narrative docs omit the string).
        Official leftover configuration-values (200): $CONFIG_VALUES_URL
        Official leftover imports (200): $IMPORTS_URL
        Official leftover releases (200): $RELEASES_URL  Official leftover $RELEASES_404_URL is 404.
        Official leftover changelog latest listed $DOCS_LATEST_FT4 ($DOCS_LATEST_FT4_DATE). Official docs pin remains ${DappScaffold.FT4_VERSION} / API ${DappScaffold.FT4_API}.
        Official leftover table defaults (source wins): rate_limit.active true, max_points 10, recovery_time 5000, points_at_account_creation 1; auth_descriptor.max_number_per_account 10.
        Official leftover example uses max_points: 20 — that is an example, not the table default (10).
        Use auth_descriptor.max_rules (not the stale leftover sibling key max_auth_descriptor_rules).
        Official leftover configuration-values / imports print a sample admin pubkey — NEVER emit it. NEVER emit lib.ft4.core.admin.
        NEVER import or configure ${DappScaffold.forbiddenModules.joinToString(", ")}.
        Prefer ras_transfer_fee / ras_transfer_subscription if you add a registration strategy.
        Paste moduleArgs under blockchains.$name and merge libs at the project root. Then `chr install`.
        ${if (includeIccf) {
            "includeIccf=true emits official IccfGTXModule wiring under blockchains.$name.config.gtx.modules: $ICCF_GTX_MODULE ($ICCF_PROTOCOL_URL, $ICCF_PROPERTIES_URL). libs/yaml use official ICCF protocol-page library-chain $ICCF_LIBRARY_CHAIN_ID version $ICCF_LIBRARY_CHAIN_VERSION (import $ICCF_IMPORT;). Official FT4 setup still documents git tagOrBranch $ICCF_GIT_TAG ($ICCF_FT4_SETUP_URL) — see iccf_git_yaml. Use one ICCF lib shape, not both. Do not invent a newer git tag, library-chain semver, or module name."
        } else {
            "includeIccf=false omits ICCF library-chain / git lib and IccfGTXModule wiring. Set includeIccf=true for official cross-chain proof setup ($ICCF_PROTOCOL_URL)."
        }}
        Confirm keys with fetch_docs before inventing fields. This tool does not send signed transactions.
    """.trimIndent()

    fun toJson(name: String?, includeIccf: Boolean): kotlinx.serialization.json.JsonObject {
        val chain = DappScaffold.normalizeName(name)
        val libs = libsYaml(includeIccf)
        val moduleArgs = moduleArgsYaml()
        val gtx = if (includeIccf) gtxConfigYaml() else ""
        val combined = buildString {
            append("blockchains:\n")
            append("  $chain:\n")
            append("    module: main\n")
            append("    config:\n")
            append("      features:\n")
            append("        merkle_hash_version: ${DappScaffold.MERKLE_HASH_VERSION}\n")
            if (includeIccf) {
                append("      gtx:\n")
                append("        modules:\n")
                append("          - \"$ICCF_GTX_MODULE\"\n")
            }
            moduleArgs.lineSequence().forEach { line ->
                if (line.isBlank()) append('\n') else append("    ").append(line).append('\n')
            }
            append('\n')
            append("compile:\n")
            append("  rellVersion: ${DappScaffold.RELL_VERSION}\n")
            append('\n')
            append(libs)
        }
        return buildJsonObject {
            put("name", chain)
            put("ft4Version", DappScaffold.FT4_VERSION)
            put("ft4Api", DappScaffold.FT4_API)
            put("DEFAULT_LOGIN_CONFIG_NAME", DEFAULT_LOGIN_CONFIG_NAME)
            put("require_mandatory_flags", REQUIRE_MANDATORY_FLAGS_SCOPE)
            put("imports_docs", IMPORTS_URL)
            put("config_values_docs", CONFIG_VALUES_URL)
            put("releases_docs", RELEASES_URL)
            put("releases_404", RELEASES_404_URL)
            put("docs_latest_ft4", DOCS_LATEST_FT4)
            put("docs_latest_ft4_date", DOCS_LATEST_FT4_DATE)
            put("includeIccf", includeIccf)
            put("iccfGtxModule", if (includeIccf) ICCF_GTX_MODULE else "")
            put("iccfLibraryChainId", if (includeIccf) ICCF_LIBRARY_CHAIN_ID else "")
            put("iccfLibraryChainVersion", if (includeIccf) ICCF_LIBRARY_CHAIN_VERSION else "")
            put("iccfGitTag", if (includeIccf) ICCF_GIT_TAG else "")
            put("library_chain_yaml", if (includeIccf) libraryChainYaml() else "")
            put("iccf_git_yaml", if (includeIccf) gitIccfYaml() else "")
            put(
                "pins",
                buildJsonObject {
                    put("ft4", DappScaffold.FT4_VERSION)
                    put("ft4Api", DappScaffold.FT4_API)
                    put("rell", DappScaffold.RELL_VERSION)
                    put("DEFAULT_LOGIN_CONFIG_NAME", DEFAULT_LOGIN_CONFIG_NAME)
                    put("require_mandatory_flags", REQUIRE_MANDATORY_FLAGS_SCOPE)
                    if (includeIccf) {
                        put("iccfGtxModule", ICCF_GTX_MODULE)
                        put("iccfLibraryChainId", ICCF_LIBRARY_CHAIN_ID)
                        put("iccfLibraryChainVersion", ICCF_LIBRARY_CHAIN_VERSION)
                        put("iccfGitTag", ICCF_GIT_TAG)
                    }
                }
            )
            put("libs", libs)
            put("moduleArgs", moduleArgs)
            put("gtx", gtx)
            put("yaml", combined)
            put(
                "forbidden",
                buildJsonArray {
                    DappScaffold.forbiddenModules.forEach { add(JsonPrimitive(it)) }
                }
            )
            put("notes", notes(chain, includeIccf))
        }
    }
}
