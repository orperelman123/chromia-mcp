package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.NetworkResult

/**
 * Logic behind the `deployment_preflight` tool: catch every deployment problem
 * BEFORE a human burns a lease step or signs anything.
 *
 * Checks, each a structured [Finding]:
 * 1. chromia.yml / deployments.<target> validity (reuses [ChromiaYmlValidator]).
 * 2. Target reachability - a read-only height probe on the block's Directory
 *    Chain BRID against the block's own URL(s), classified with
 *    [VerifyDeployment.failureHint].
 * 3. Network sanity - a testnet/mainnet-named target whose brid/url point at
 *    the OTHER network is a HIGH blocker (wrong-network deploys are
 *    unrecoverable); custom targets are fine but noted.
 * 4. Source gate when `rell` is supplied - must compile ([RellCheck]); for
 *    MAINNET targets CRITICAL/HIGH security findings ([RellSecurityCheck]) are
 *    blockers, for other targets they are warnings.
 * 5. Pins - via the validator; strict (default true for mainnet targets) turns
 *    missing production pins into blockers.
 *
 * ready=true only with zero blocking findings, and NEVER for something not
 * checked: a skipped source gate is said in notes, and a mainnet target with
 * no sources stays blocked until the gate runs.
 *
 * Policy: read-only. No keys, no signing, no network writes - the only network
 * I/O is the caller-supplied height probe.
 */
object DeploymentPreflight {

    const val SEVERITY_BLOCKER = "BLOCKER"
    const val SEVERITY_HIGH = "HIGH"
    const val SEVERITY_WARNING = "WARNING"
    const val SEVERITY_INFO = "INFO"

    /** Reachability never probes more URLs than this (bounded network cost). */
    internal const val MAX_PROBED_URLS = 3

    private const val POLICY_NOTE =
        "Read-only preflight: no keys, no signing, no network writes - the only network I/O is a height probe."

    data class Finding(
        val severity: String,
        val check: String,
        val message: String,
        val fix: String
    ) {
        val blocking: Boolean
            get() = severity == SEVERITY_BLOCKER || severity == SEVERITY_HIGH

        fun toJson() = buildJsonObject {
            put("severity", severity)
            put("check", check)
            put("message", message)
            put("fix", fix)
        }
    }

    data class Result(
        val ready: Boolean,
        val target: String,
        val network: String,
        val findings: List<Finding>,
        val nextAction: String,
        val notes: List<String>
    ) {
        val blockers: List<String>
            get() = findings.filter { it.blocking }.map { "[${it.check}] ${it.message}" }

        fun toJson() = buildJsonObject {
            put("ready", ready)
            put("target", target)
            put("network", network)
            put("findings", buildJsonArray { findings.forEach { add(it.toJson()) } })
            put("blockers", buildJsonArray { blockers.forEach { add(JsonPrimitive(it)) } })
            put("nextAction", nextAction)
            put("notes", notes.joinToString(" "))
        }
    }

    private fun hostsOf(urls: Iterable<String>): Set<String> =
        urls.mapNotNull {
            runCatching { java.net.URI(it.trim().trimEnd('/')) }.getOrNull()?.host?.lowercase()
        }.toSet()

    /** Known public node hosts per network: project-config + explorer snapshot + client defaults. */
    internal val MAINNET_HOSTS: Set<String> = hostsOf(
        WriteDeploymentConfig.MAINNET_URLS +
            WriteDeploymentConfig.MAINNET_EXPLORER_SNAPSHOT_URLS +
            ChromiaConfig().predefinedNetworks.getValue("mainnet")
    )
    internal val TESTNET_HOSTS: Set<String> = hostsOf(
        WriteDeploymentConfig.TESTNET_URLS + ChromiaConfig().predefinedNetworks.getValue("testnet")
    )

    private val PLACEHOLDER_CONTAINERS = setOf(
        "todo", "tbd", "placeholder", "changeme", "container", "containeriid",
        "container-id", "container_id", "your-container-id", "my-container"
    )

    private fun validHttpUrl(url: String): Boolean {
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        return uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank()
    }

    /**
     * @param probe read-only height read against (network-or-url, 64-hex brid);
     * null skips reachability entirely (noted, never claimed checked).
     */
    suspend fun run(
        yaml: String,
        target: String,
        rellFiles: Map<String, String>?,
        strictOverride: Boolean?,
        probe: (suspend (network: String, bridHex: String) -> NetworkResult<Long>)?
    ): Result {
        val findings = mutableListOf<Finding>()
        val notes = mutableListOf<String>(POLICY_NOTE)
        val trimmedTarget = target.trim()

        // ---- parse ----------------------------------------------------------
        val root = runCatching { SimpleYaml.parse(yaml.trim()) }.getOrElse { e ->
            findings += Finding(
                SEVERITY_BLOCKER, "yaml",
                "chromia.yml does not parse: ${e.message}",
                "Fix the YAML syntax, then re-run deployment_preflight."
            )
            null
        } as? YamlNode.Mapping
        if (root == null) {
            if (findings.isEmpty()) {
                findings += Finding(
                    SEVERITY_BLOCKER, "yaml",
                    "chromia.yml root must be a mapping",
                    "Fix the YAML structure, then re-run deployment_preflight."
                )
            }
            notes += "Reachability and source gate did not run - the yaml must parse first."
            return buildResult(
                trimmedTarget, network = "unknown", findings = findings, notes = notes,
                chain = null, updateNotCreate = false
            )
        }

        // ---- target block ----------------------------------------------------
        val deployments = root.mapping("deployments")
        val targetPresent = deployments?.entries?.containsKey(trimmedTarget) == true
        val dep = deployments?.mapping(trimmedTarget)
        if (dep == null) {
            if (targetPresent) {
                findings += Finding(
                    SEVERITY_BLOCKER, "target",
                    "deployments.$trimmedTarget must be a mapping with url / brid / container / chains",
                    "Rewrite the block - write_deployment_config emits the correct shape."
                )
            } else {
                val available = deployments?.entries?.keys?.joinToString(", ").orEmpty().ifEmpty { "(none)" }
                findings += Finding(
                    SEVERITY_BLOCKER, "target",
                    "deployments.$trimmedTarget not found in chromia.yml (available: $available)",
                    "Add the block with write_deployment_config, or pass an existing deployment target name."
                )
            }
        }
        val reserved = trimmedTarget in ChromiaYmlValidator.RESERVED_DEPLOYMENT_NAMES

        // ---- brid ------------------------------------------------------------
        var bridHex: String? = null
        val rawBrid = dep?.scalar("brid")
        if (dep != null) {
            if (rawBrid.isNullOrBlank()) {
                if (reserved) {
                    bridHex = ChromiaYmlValidator.officialDirectoryBrid(trimmedTarget)
                    findings += Finding(
                        SEVERITY_INFO, "brid",
                        "deployments.$trimmedTarget.brid omitted - the reserved name auto-fills the " +
                            "official $trimmedTarget Directory Chain RID (CLI 0.29.8+)",
                        "Nothing required; write_deployment_config pins it explicitly if you prefer."
                    )
                } else {
                    findings += Finding(
                        SEVERITY_BLOCKER, "brid",
                        "deployments.$trimmedTarget.brid is missing - custom deployment names require " +
                            "an explicit Directory Chain RID",
                        "Add brid: x\"<64-hex Directory Chain RID>\" (write_deployment_config emits the " +
                            "official testnet/mainnet values)."
                    )
                }
            } else {
                val hex = ChromiaYmlValidator.normalizeDirectoryBrid(rawBrid)
                if (hex == null || hex.length != ChromiaYmlValidator.DIRECTORY_BRID_HEX_LENGTH) {
                    findings += Finding(
                        SEVERITY_BLOCKER, "brid",
                        "deployments.$trimmedTarget.brid is not a " +
                            "${ChromiaYmlValidator.DIRECTORY_BRID_HEX_LENGTH}-hex Directory Chain RID " +
                            "(found \"${rawBrid.take(80)}\")",
                        "Use the official Directory Chain RID in x\"...\" form - write_deployment_config emits it."
                    )
                } else {
                    bridHex = hex
                }
            }
        }

        // ---- network classification + sanity (check 3) -----------------------
        val network = when {
            reserved -> trimmedTarget
            bridHex == WriteDeploymentConfig.MAINNET_DIRECTORY_BRID -> "mainnet"
            bridHex == WriteDeploymentConfig.TESTNET_DIRECTORY_BRID -> "testnet"
            else -> "custom"
        }
        if (reserved && bridHex != null) {
            val official = ChromiaYmlValidator.officialDirectoryBrid(trimmedTarget)
            if (official != null && bridHex != official) {
                val actual = when (bridHex) {
                    WriteDeploymentConfig.MAINNET_DIRECTORY_BRID -> " (it is the MAINNET Directory Chain RID)"
                    WriteDeploymentConfig.TESTNET_DIRECTORY_BRID -> " (it is the TESTNET Directory Chain RID)"
                    else -> ""
                }
                findings += Finding(
                    SEVERITY_HIGH, "network",
                    "deployments.$trimmedTarget.brid is NOT the official $trimmedTarget Directory Chain " +
                        "RID$actual - deploying to the wrong network is unrecoverable",
                    "Set brid to x\"$official\" (write_deployment_config emits it) before anyone signs anything."
                )
            }
        }
        if (!reserved && dep != null) {
            when {
                network != "custom" -> notes +=
                    "Custom target name \"$trimmedTarget\" carries the official $network Directory Chain RID - treated as a $network deployment."
                bridHex != null -> findings += Finding(
                    SEVERITY_INFO, "network",
                    "deployments.$trimmedTarget.brid is not a known public (testnet/mainnet) Directory " +
                        "Chain RID - a private/custom network is fine, but double-check it is intended",
                    "Nothing required if this is your own network; otherwise use write_deployment_config."
                )
            }
        }

        // ---- url -------------------------------------------------------------
        val urls = mutableListOf<String>()
        if (dep != null) {
            when (val urlNode = dep.entries["url"]) {
                is YamlNode.Scalar -> if (urlNode.raw.isNotBlank()) urls += urlNode.raw.trim()
                is YamlNode.Sequence -> urlNode.items.forEach {
                    if (it is YamlNode.Scalar && it.raw.isNotBlank()) urls += it.raw.trim()
                }
                else -> Unit
            }
            if (urls.isEmpty()) {
                if (reserved) {
                    WriteDeploymentConfig.resolveNetwork(trimmedTarget)?.let { spec ->
                        urls += spec.urls
                        findings += Finding(
                            SEVERITY_INFO, "url",
                            "deployments.$trimmedTarget.url omitted - the reserved name auto-fills the " +
                                "official $trimmedTarget node URLs",
                            "Nothing required; write_deployment_config pins them explicitly if you prefer."
                        )
                    }
                } else {
                    findings += Finding(
                        SEVERITY_BLOCKER, "url",
                        "deployments.$trimmedTarget.url is missing - custom deployment names require an " +
                            "explicit node URL (string or list)",
                        "Add the node URL(s) this deployment should use, e.g. url: https://<node-host>:7740."
                    )
                }
            } else {
                urls.forEach { u ->
                    if (!validHttpUrl(u)) {
                        findings += Finding(
                            SEVERITY_BLOCKER, "url",
                            "deployments.$trimmedTarget.url \"$u\" is not a valid http(s) URL",
                            "Use a full URL like https://node0.testnet.chromia.com:7740."
                        )
                    }
                }
                if (network == "mainnet" || network == "testnet") {
                    val own = if (network == "mainnet") MAINNET_HOSTS else TESTNET_HOSTS
                    val other = if (network == "mainnet") TESTNET_HOSTS else MAINNET_HOSTS
                    val otherName = if (network == "mainnet") "testnet" else "mainnet"
                    urls.filter { validHttpUrl(it) }.forEach { u ->
                        val host = runCatching { java.net.URI(u) }.getOrNull()?.host?.lowercase()
                        when {
                            host == null -> Unit
                            host in other && host !in own -> findings += Finding(
                                SEVERITY_HIGH, "network",
                                "deployments.$trimmedTarget.url $u is a known $otherName node but the " +
                                    "target network is $network - deploying to the wrong network is unrecoverable",
                                "Replace it with an official $network URL (write_deployment_config emits them)."
                            )
                            host !in own -> findings += Finding(
                                SEVERITY_WARNING, "url",
                                "deployments.$trimmedTarget.url $u is not a known official $network node",
                                "Fine if it is your own node serving $network; otherwise use the official " +
                                    "URLs from write_deployment_config."
                            )
                        }
                    }
                }
            }
        }

        // ---- container -------------------------------------------------------
        if (dep != null) {
            val container = dep.scalar("container")?.trim().orEmpty()
            val containerFix =
                "Lease a container (testnet: ${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS}; mainnet: " +
                    "${VaultLeaseHelp.MAINNET_VAULT_CONTAINERS} after a >=10 CHR deposit) and put the " +
                    "real Container ID here - a human Vault web step this tool cannot do."
            when {
                container.isEmpty() -> findings += Finding(
                    SEVERITY_BLOCKER, "container",
                    "deployments.$trimmedTarget.container is missing - `chr deployment create` needs " +
                        "the Vault / PMC lease id",
                    containerFix
                )
                container.contains('<') || container.contains('>') ||
                    container.lowercase() in PLACEHOLDER_CONTAINERS -> findings += Finding(
                    SEVERITY_BLOCKER, "container",
                    "deployments.$trimmedTarget.container \"${container.take(60)}\" looks like a " +
                        "placeholder, not a real lease id",
                    containerFix
                )
            }
        }

        // ---- chains ----------------------------------------------------------
        val blockchains = root.mapping("blockchains")
        val chainNames = blockchains?.entries?.keys.orEmpty()
        val chains = dep?.mapping("chains")
        var chain: String? = null
        var updateNotCreate = false
        if (dep != null) {
            if (chains == null || chains.entries.isEmpty()) {
                chain = chainNames.firstOrNull()
                if (chain == null) {
                    findings += Finding(
                        SEVERITY_BLOCKER, "chains",
                        "no blockchains are declared and deployments.$trimmedTarget.chains is empty - " +
                            "there is nothing to deploy",
                        "Declare blockchains.<name> with a module (scaffold_dapp emits a working chromia.yml)."
                    )
                } else {
                    findings += Finding(
                        SEVERITY_WARNING, "chains",
                        "deployments.$trimmedTarget.chains is empty - fine for a FIRST `chr deployment " +
                            "create` (CLI 0.30.0+ writes chains.<name>: x\"<dapp rid>\" back), but " +
                            "`chr deployment update` requires it",
                        "Nothing required for a first create; for an update run the first create before updating."
                    )
                }
            } else {
                chains.entries.keys.forEach { name ->
                    if (chainNames.isNotEmpty() && name !in chainNames) {
                        findings += Finding(
                            SEVERITY_BLOCKER, "chains",
                            "deployments.$trimmedTarget.chains.$name does not match any blockchains name " +
                                "(${chainNames.joinToString(", ")}) - `chr deployment ... --blockchain " +
                                "$name` will fail",
                            "Use one of the declared blockchains names, or add blockchains.$name."
                        )
                    }
                }
                chain = chains.entries.keys.first()
                val value = (chains.entries[chain] as? YamlNode.Scalar)?.raw?.trim().orEmpty()
                updateNotCreate = value.isNotEmpty()
            }
        }

        // ---- yml validity + pins (checks 1 and 5) ----------------------------
        val strict = strictOverride ?: (network == "mainnet")
        val validated = ChromiaYmlValidator.validate(yaml, strict)
        // The target block was checked above with richer findings; other
        // deployments blocks and everything global pass through.
        val targetPrefix = "deployments.$trimmedTarget"
        fun aboutTarget(msg: String) =
            msg.startsWith("$targetPrefix.") || msg.startsWith("$targetPrefix:") || msg.startsWith("$targetPrefix ")
        validated.errors.filterNot(::aboutTarget).forEach { msg ->
            findings += Finding(
                SEVERITY_BLOCKER, "chromia_yml", msg,
                "Fix chromia.yml until validate_chromia_yml (strict=$strict) reports ok:true - it documents the pins."
            )
        }
        validated.warnings.filterNot(::aboutTarget).forEach { msg ->
            findings += Finding(
                SEVERITY_WARNING, "chromia_yml", msg,
                "Recommended before deploying; validate_chromia_yml documents the production pins."
            )
        }
        if (strict) {
            notes += "Strict pins active (default for mainnet targets): missing compile.rellVersion / " +
                "merkle_hash_version are blockers."
        }

        // ---- source gate (check 4) -------------------------------------------
        if (!rellFiles.isNullOrEmpty()) {
            var compiledModules: List<String>? = null
            val compile = runCatching { RellCheck.check(rellFiles, null) }.getOrElse { e ->
                findings += Finding(
                    SEVERITY_BLOCKER, "source",
                    "compile check failed: ${e.message}",
                    "Fix the `rell` input (map of path.rell -> source) or run rell_check directly for detail."
                )
                null
            }
            if (compile != null) {
                if (compile.notes.isNotBlank()) notes += compile.notes
                compile.errors.forEach { d ->
                    val where = listOfNotNull(d.file, d.line?.toString()).joinToString(":")
                    findings += Finding(
                        SEVERITY_BLOCKER, "source",
                        (if (where.isEmpty()) "" else "$where: ") + d.text,
                        "Fix the compile error (translate_error explains cryptic ones), then re-run."
                    )
                }
                if (compile.ok) {
                    compiledModules = compile.modules
                    val security = RellSecurityCheck.analyze(rellFiles, false)
                    security.findings.forEach { f ->
                        val serious = f.severity == "CRITICAL" || f.severity == "HIGH"
                        val severity = if (serious && network == "mainnet") SEVERITY_BLOCKER else SEVERITY_WARNING
                        findings += Finding(
                            severity, "security",
                            "${f.file}:${f.line}: [${f.severity}] ${f.rule} - ${f.text}",
                            f.fix
                        )
                    }
                    if (network != "mainnet" &&
                        security.findings.any { it.severity == "CRITICAL" || it.severity == "HIGH" }
                    ) {
                        notes += "CRITICAL/HIGH security findings are warnings for a $network target - " +
                            "they would BLOCK a mainnet preflight; fix them before going further."
                    }
                }
            }
            // Every chain the target deploys must map to a module that actually compiled.
            val mods = compiledModules
            if (!mods.isNullOrEmpty() && chains != null && blockchains != null) {
                chains.entries.keys.forEach { name ->
                    val module = (blockchains.entries[name] as? YamlNode.Mapping)?.scalar("module")?.trim()
                    if (!module.isNullOrEmpty() && name in chainNames && module !in mods) {
                        findings += Finding(
                            SEVERITY_BLOCKER, "chains",
                            "deployments.$trimmedTarget.chains.$name: module \"$module\" is not among " +
                                "the compiled modules (${mods.joinToString(", ")})",
                            "Submit that module's sources in `rell`, or fix blockchains.$name.module."
                        )
                    }
                }
            }
        } else if (network == "mainnet") {
            findings += Finding(
                SEVERITY_BLOCKER, "source_gate",
                "no Rell sources supplied - the compile + security gate did not run, and a MAINNET " +
                    "target is never declared ready on unchecked code",
                "Pass `rell` ({\"path.rell\": \"source\"}) so the exact code about to be deployed is " +
                    "compiled and security-scanned."
            )
        } else {
            notes += "Source gate SKIPPED - no `rell` supplied, so nothing here vouches for the code " +
                "itself; pass `rell` (or run check_dapp_project) before deploying."
        }

        // ---- reachability (check 2) ------------------------------------------
        if (probe != null && dep != null && bridHex != null) {
            val candidates = urls.filter { validHttpUrl(it) }.take(MAX_PROBED_URLS)
                .ifEmpty { if (reserved) listOf(trimmedTarget) else emptyList() }
            if (candidates.isEmpty()) {
                notes += "Reachability probe skipped - no usable URL to probe."
            } else {
                val errors = mutableListOf<String>()
                var reached = false
                for (candidate in candidates) {
                    when (val r = probe(candidate, bridHex)) {
                        is NetworkResult.Success -> {
                            findings += Finding(
                                SEVERITY_INFO, "reachability",
                                "$candidate answers for the Directory Chain (height ${r.data})",
                                "Nothing required."
                            )
                            reached = true
                        }
                        is NetworkResult.Error ->
                            errors += "$candidate: ${VerifyDeployment.failureHint(r.message, candidate)} " +
                                "(node error: ${r.message.take(200)})"
                    }
                    if (reached) break
                }
                if (!reached) {
                    findings += Finding(
                        SEVERITY_BLOCKER, "reachability",
                        "no deployment URL answered a read-only height probe for the Directory Chain - " +
                            errors.joinToString("; "),
                        "Fix the URL/network per the hint; if they are correct the node may be down - " +
                            "retry shortly or point url at a node that serves this network."
                    )
                }
            }
        } else if (probe != null && dep != null) {
            notes += "Reachability probe skipped - no valid brid to probe with."
        } else if (probe == null) {
            notes += "Reachability probe skipped - no probe available."
        }

        return buildResult(trimmedTarget, network, findings, notes, chain, updateNotCreate)
    }

    private fun buildResult(
        target: String,
        network: String,
        findings: List<Finding>,
        notes: List<String>,
        chain: String?,
        updateNotCreate: Boolean
    ): Result {
        val blocking = findings.filter { it.blocking }
        val ready = blocking.isEmpty()
        val verb = if (updateNotCreate) "update" else "create"
        val command =
            "chr deployment $verb --settings chromia.yml --network $target --blockchain ${chain ?: "<name>"}"
        val nextAction = if (ready) {
            "Run `$command` - headless, signed by the container key via the POSTCHAIN_CLIENT_PUBKEY/" +
                "POSTCHAIN_CLIENT_PRIVKEY environment variables; this tool never runs or signs it."
        } else {
            "Fix ${blocking.size} blocker(s) before deploying - start with: " +
                "[${blocking.first().check}] ${blocking.first().message}"
        }
        return Result(ready, target, network, findings, nextAction, notes)
    }
}
