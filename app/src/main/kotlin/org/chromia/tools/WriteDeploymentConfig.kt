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
        // No `chains:` key on purpose: a FIRST `chr deployment create` must not
        // see one. A placeholder entry with no value is a null map value that
        // chr rejects with "Incorrect type, expected string" (live 2026-09-02,
        // chr 0.29.10). CLI 0.30.0+ writes chains.<name>: x"<dapp rid>" back
        // after deploying; on 0.29.x add it by hand from chr's stdout.
        return lines.joinToString("\n") + "\n"
    }

    /**
     * AUDIT F7 (2026-09-06): this used to synthesise a WHOLE chromia.yml from the
     * `hello` scaffold whenever it was called, and deployment_preflight's own fix
     * line sent agents here. Adopting the result silently deleted, from an ft4
     * project:
     *   - blockchains.<name>.moduleArgs entirely (lib.ft4.query_max_page_size,
     *     lib.ft4.core.accounts.rate_limit, auth_descriptor, and
     *     auth_flags.mandatory ["A","T"]),
     *   - the whole test.moduleArgs block (the FT4 test admin wiring),
     *   - the libs.iccf entry the shipped tests need,
     * and validate_chromia_yml answered {"ok":true,"errors":[],"warnings":[]} on
     * the gutted file. A chain deployed with FT4 auth flags and rate limits unset,
     * green on every gate in the server.
     *
     * It now MERGES: every key the caller's yml declares is preserved verbatim -
     * comments included, because this is a text merge and not a re-serialisation -
     * and only `deployments.<network>` is written. An existing block for the same
     * network is replaced, but its real `container` and any `chains` map survive:
     * those are the two things the caller has that this tool cannot invent.
     */
    fun mergeDeployments(existingYaml: String, spec: NetworkSpec, chain: String): String {
        val block = deploymentsYaml(spec, chain).trimEnd(NEWLINE)
        val lines = existingYaml.replace(CRLF, NEWLINE.toString()).split(NEWLINE)
        val deploymentsAt = lines.indexOfFirst {
            it.trimEnd() == "deployments:" && !it.startsWith(" ") && !it.startsWith(TAB)
        }
        if (deploymentsAt < 0) {
            return existingYaml.trimEnd(NEWLINE, ' ', TAB) + NEWLINE + NEWLINE + block + NEWLINE
        }
        // The whole `deployments:` section: everything until the next non-blank
        // line at indent 0.
        var end = lines.size
        for (i in deploymentsAt + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            if (!line.startsWith(" ") && !line.startsWith(TAB)) {
                end = i
                break
            }
        }
        val section = lines.subList(deploymentsAt + 1, end)
        val netHeader = Regex("^(\\s+)" + Regex.escape(spec.name) + "\\s*:\\s*$")
        val netAt = section.indexOfFirst { netHeader.matches(it) }
        val rebuilt = if (netAt >= 0) {
            val indent = netHeader.find(section[netAt])!!.groupValues[1]
            var netEnd = section.size
            for (i in netAt + 1 until section.size) {
                val line = section[i]
                if (line.isBlank()) continue
                val lead = line.takeWhile { it == ' ' || it == TAB }
                if (lead.length <= indent.length) {
                    netEnd = i
                    break
                }
            }
            // Carry over what this tool cannot invent: a real lease id, and the
            // chains map `chr deployment create` writes back after a deploy.
            val existingNet = section.subList(netAt, netEnd)
            val kept = mutableListOf<String>()
            val container = existingNet.firstOrNull { it.trim().startsWith("container:") }
                ?.substringAfter("container:")?.trim()
            if (container != null && container.isNotEmpty() && !container.startsWith("<") &&
                container.lowercase() !in PLACEHOLDER_CONTAINER_VALUES
            ) {
                kept += "    container: " + container
            }
            val chainsAt = existingNet.indexOfFirst { it.trim() == "chains:" }
            if (chainsAt >= 0) kept += existingNet.subList(chainsAt, existingNet.size).filter { it.isNotBlank() }
            buildList {
                addAll(lines.subList(0, deploymentsAt + 1))
                addAll(section.subList(0, netAt))
                addAll(networkBlockLines(spec, kept))
                addAll(section.subList(netEnd, section.size))
                addAll(lines.subList(end, lines.size))
            }
        } else {
            buildList {
                addAll(lines.subList(0, deploymentsAt + 1))
                addAll(networkBlockLines(spec, emptyList()))
                addAll(section)
                addAll(lines.subList(end, lines.size))
            }
        }
        return rebuilt.joinToString(NEWLINE.toString()).trimEnd(NEWLINE) + NEWLINE
    }

    private const val NEWLINE = '\n'
    private const val TAB = '\t'
    private const val CRLF = "\r\n"
    private val PLACEHOLDER_CONTAINER_VALUES =
        setOf("<containeriid>", "todo", "tbd", "placeholder", "changeme", "container")

    /** The `<network>:` sub-block on its own, plus whatever survived from the old one. */
    private fun networkBlockLines(spec: NetworkSpec, kept: List<String>): List<String> {
        val out = mutableListOf("  " + spec.name + ":", "    url:")
        spec.urls.forEach { out += "      - " + it }
        out += "    brid: " + spec.bridYaml()
        if (kept.none { it.trim().startsWith("container:") }) out += "    container: <containerIID>"
        out += kept
        return out
    }

    /**
     * The full chromia.yml, ONLY when the caller supplied one to merge into.
     * There is deliberately no "make one up" branch any more: inventing a project
     * file from the `hello` scaffold is exactly how the FT4 configuration was lost.
     */
    fun chromiaYml(spec: NetworkSpec, chain: String, existingYaml: String): String =
        mergeDeployments(existingYaml, spec, chain)

    fun notes(spec: NetworkSpec, chain: String): String = """
        chromia.yml deployments.${spec.name} block for Chromia CLI ${DappScaffold.CLI_SERIES}.
        Directory Chain BRID is the official ${spec.name} value from $PROJECT_CONFIG_URL.
        Do not invent a Directory or dapp BRID. Re-verify on Explorer (system cluster → directory_chain).
        Reserved names mainnet / testnet auto-fill Directory brid + url since CLI 0.29.8.
        Do not invent a single required URL. Official testnet system nodes are node0–node3.testnet.chromia.com:7740.
        Official mainnet hosts (project-config only): https://system.chromaway.com (no port) and https://mainnet-dapp1.sunube.net:7740.
        url may be a string or a list (official schema).
        This block writes those official project-config hosts only. Extra hosts on the mainnet connect-client page are an explorer snapshot, not required — do not invent hosts.
        chains is omitted on purpose: a FIRST `chr deployment create` needs no chains entry, and a
        placeholder chains.$chain with no value is a null that chr rejects with "Incorrect type, expected string".
        Since CLI 0.30.0, `chr deployment create` writes deployments.${spec.name}.chains.$chain: x"<dapp rid>" back into chromia.yml.
        On CLI 0.29.x there is no write-back — after the first create, add chains.$chain: x"<dapp rid>" by hand (the RID is printed on stdout).
        `chr deployment update` requires chains and does not rewrite chromia.yml.
        First create: `chr deployment create --settings chromia.yml --network ${spec.name} --blockchain $chain`
        container is the Vault / PMC lease id — set it after leasing; this tool does not invent one.
        merkle_hash_version must stay ${DappScaffold.MERKLE_HASH_VERSION}. compile.rellVersion ${DappScaffold.RELL_VERSION}.
        NEVER import ${DappScaffold.forbiddenModules.joinToString(", ")}.
        This tool does not send signed transactions and does not run chr.
    """.trimIndent()

    /**
     * @param existingYaml the project's own chromia.yml. When given, `chromia_yml`
     * is that file with only `deployments.<network>` merged in - every other key
     * preserved. When it is NOT given, there is no `chromia_yml` field at all:
     * this tool has no business inventing a project file, and the one it used to
     * invent deleted the caller's FT4 configuration (audit F7).
     */
    fun toJson(network: String?, name: String?, existingYaml: String? = null): kotlinx.serialization.json.JsonObject {
        val spec = resolveNetwork(network)
            ?: throw IllegalArgumentException(unknownNetworkMessage(network))
        // A present-but-invalid name used to silently become 'hello', writing a
        // wrong-keyed deployments block (audit round 4 minor).
        val chain = DappScaffold.requireValidName(name)
        val yaml = deploymentsYaml(spec, chain)
        val full = existingYaml?.takeIf { it.isNotBlank() }?.let { chromiaYml(spec, chain, it) }
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
            // Only ever the CALLER's file with the block merged in - never a
            // synthesised one (audit F7).
            if (full != null) {
                put("chromia_yml", full)
                put("merged_into", "the chromia.yml you passed as `yaml`")
            } else {
                put(
                    "merge_note",
                    "No `yaml` was passed, so no full chromia.yml is returned: paste the `yaml` " +
                        "block above into your own file, or call this tool again with " +
                        "yaml=<your chromia.yml> and take `chromia_yml`. This tool never " +
                        "regenerates a project file - the version that did dropped FT4 moduleArgs " +
                        "(auth_flags.mandatory included), the test.moduleArgs block and libs.iccf, " +
                        "and every gate in this server called the result ok:true (audit F7)."
                )
            }
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
