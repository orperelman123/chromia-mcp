package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official chromia.yml `deployments.<net>` block for Chromia CLI 0.33.x.
 * Directory BRIDs from docs.chromia.com/build/configuration/project-config
 * (also briefs/cli-deploy.md). Does not invent a dapp RID.
 * Since 0.30.0, `chr deployment create` writes deployments.<net>.chains back.
 * Never signs or sends a transaction.
 */
object WriteDeploymentConfig {
    const val MAINNET_DIRECTORY_BRID =
        "7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4"
    const val TESTNET_DIRECTORY_BRID =
        "6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"
    const val MAINNET_URL = "https://system.chromaway.com"
    const val TESTNET_URL = "https://node0.testnet.chromia.com:7740"
    const val DIRECTORY_BRID_HEX_LENGTH = 64
    const val PROJECT_CONFIG_URL = "https://docs.chromia.com/build/configuration/project-config"

    val MAINNET_URLS = listOf(
        "https://system.chromaway.com",
        "https://mainnet-dapp1.sunube.net:7740"
    )

    /** Explorer connect-client snapshot — not project-config, not required. */
    val MAINNET_EXPLORER_SNAPSHOT_URLS = listOf(
        "https://chromia.validatrium.club",
        "https://chromia-mainnet-systemnode-1.stakin-nodes.com",
        "https://chroma.node.monster:7741",
        "https://dapps0.chromaway.com",
        "https://chromia-mainnet.w3coins.io:7740"
    )

    val TESTNET_URLS = listOf(
        "https://node0.testnet.chromia.com:7740",
        "https://node1.testnet.chromia.com:7740",
        "https://node2.testnet.chromia.com:7740",
        "https://node3.testnet.chromia.com:7740"
    )

    data class NetworkSpec(
        val name: String,
        val directoryBrid: String,
        val url: String,
        val urls: List<String>
    ) {
        fun bridYaml(): String = """x"$directoryBrid""""
    }

    fun resolveNetwork(raw: String?): NetworkSpec? {
        return when (raw?.trim()?.lowercase()) {
            "mainnet" -> NetworkSpec("mainnet", MAINNET_DIRECTORY_BRID, MAINNET_URL, MAINNET_URLS)
            "testnet" -> NetworkSpec("testnet", TESTNET_DIRECTORY_BRID, TESTNET_URL, TESTNET_URLS)
            else -> null
        }
    }

    fun unknownNetworkMessage(raw: String?): String {
        val shown = raw?.trim().orEmpty().ifEmpty { "(missing)" }
        return "Unknown network: $shown. Use testnet or mainnet."
    }

    fun urlListYaml(spec: NetworkSpec, indent: String = "    "): String {
        val lines = mutableListOf("${indent}url:")
        spec.urls.forEach { lines += "$indent  - $it" }
        return lines.joinToString("\n")
    }

    fun deploymentsYaml(spec: NetworkSpec, chain: String): String {
        val lines = mutableListOf(
            "deployments:",
            "  ${spec.name}:",
            "    url:"
        )
        spec.urls.forEach { lines += "      - $it" }
        lines += "    brid: ${spec.bridYaml()}"
        lines += "    container: <containerIID>"
        lines += "    chains:"
        lines += "      $chain: # placeholder — omit dapp RID on first create; chr deployment create writes x\"<dapp rid>\""
        return lines.joinToString("\n") + "\n"
    }

    fun chromiaYml(spec: NetworkSpec, chain: String): String {
        val scaffold = DappScaffold.files(chain).getValue("chromia.yml").trimEnd()
        return scaffold + "\n\n" + deploymentsYaml(spec, chain)
    }

    fun notes(spec: NetworkSpec, chain: String): String = """
        chromia.yml deployments.${spec.name} block for Chromia CLI ${DappScaffold.CLI_SERIES}.
        Directory Chain BRID is the official ${spec.name} value from $PROJECT_CONFIG_URL.
        Do not invent a Directory or dapp BRID. Re-verify on Explorer (system cluster → directory_chain).
        Reserved names mainnet / testnet auto-fill Directory brid + url since CLI 0.29.8.
        Do not invent a single required URL. Official testnet system nodes are node0–node3.testnet.chromia.com:7740.
        Official mainnet hosts (project-config only): https://system.chromaway.com (no port) and https://mainnet-dapp1.sunube.net:7740.
        url may be a string or a list (official schema).
        This block writes those official project-config hosts only. Extra hosts on the mainnet connect-client page are an explorer snapshot, not required — do not invent hosts.
        Since CLI 0.30.0, `chr deployment create` writes deployments.${spec.name}.chains.$chain: x"<dapp rid>" back into chromia.yml.
        Live docs that say you must paste chains by hand are stale — source + CHANGELOG 0.30.0 win.
        `chr deployment update` requires chains and does not rewrite chromia.yml.
        First create: `chr deployment create --settings chromia.yml --network ${spec.name} --blockchain $chain`
        container is the Vault / PMC lease id — set it after leasing; this tool does not invent one.
        merkle_hash_version must stay ${DappScaffold.MERKLE_HASH_VERSION}. compile.rellVersion ${DappScaffold.RELL_VERSION}.
        NEVER import ${DappScaffold.forbiddenModules.joinToString(", ")}.
        This tool does not send signed transactions and does not run chr.
    """.trimIndent()

    fun toJson(network: String?, name: String?): kotlinx.serialization.json.JsonObject {
        val spec = resolveNetwork(network)
            ?: throw IllegalArgumentException(unknownNetworkMessage(network))
        // A present-but-invalid name used to silently become 'hello', writing a
        // wrong-keyed deployments block (audit round 4 minor).
        val chain = DappScaffold.requireValidName(name)
        val yaml = deploymentsYaml(spec, chain)
        val full = chromiaYml(spec, chain)
        return buildJsonObject {
            put("network", spec.name)
            put("name", chain)
            put("cli", DappScaffold.CLI_SERIES)
            put("url", spec.url)
            put("brid", spec.bridYaml())
            put("directoryBrid", spec.directoryBrid)
            put(
                "urls",
                buildJsonArray { spec.urls.forEach { add(JsonPrimitive(it)) } }
            )
            put(
                "explorer_snapshot_urls",
                buildJsonArray {
                    if (spec.name == "mainnet") {
                        MAINNET_EXPLORER_SNAPSHOT_URLS.forEach { add(JsonPrimitive(it)) }
                    }
                }
            )
            put(
                "explorer_snapshot_note",
                if (spec.name == "mainnet") {
                    "Explorer connect-client snapshot only — not project-config and not required. Do not invent hosts."
                } else {
                    ""
                }
            )
            put("yaml", yaml)
            put("chromia_yml", full)
            put("notes", notes(spec, chain))
            put(
                "pins",
                buildJsonObject {
                    put("rell", DappScaffold.RELL_VERSION)
                    put("merkle_hash_version", DappScaffold.MERKLE_HASH_VERSION)
                    put("ft4", DappScaffold.FT4_VERSION)
                    put("cli", DappScaffold.CLI_SERIES)
                }
            )
        }
    }
}
