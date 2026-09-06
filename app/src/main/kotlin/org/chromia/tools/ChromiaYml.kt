package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Production-pin validator for chromia.yml.
 * Official schema: https://docs.chromia.com/build/configuration/project-config
 * Pins: Rell 0.16.1 for compile.rellVersion (the newest version the CLI 0.33.x
 * bundle accepts; the Rell source tag is 0.16.7 but the bundled compiler's
 * SUPPORTED_VERSIONS list stops at 0.16.1 — docs may still say 0.16.4 / show 0.14.9),
 * merkle_hash_version 2, no FT4 admin / ras_open libs.
 */
object ChromiaYmlValidator {
    const val RELL_VERSION = DappScaffold.RELL_VERSION
    const val MERKLE_HASH_VERSION = DappScaffold.MERKLE_HASH_VERSION

    val forbiddenLibModules = DappScaffold.forbiddenModules
    val officialBlockchainKeys = setOf("webStatic")
    val projectConfigBlockchainKeys = setOf("module", "moduleArgs", "config", "test")

    data class Result(
        val ok: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    ) {
        fun toJson() = buildJsonObject {
            put("ok", ok)
            put("errors", buildJsonArray { errors.forEach { add(JsonPrimitive(it)) } })
            put("warnings", buildJsonArray { warnings.forEach { add(JsonPrimitive(it)) } })
            put(
                "pins",
                buildJsonObject {
                    put("rell", RELL_VERSION)
                    put("merkle_hash_version", MERKLE_HASH_VERSION)
                    put("ft4", DappScaffold.FT4_VERSION)
                    put("ft4Api", DappScaffold.FT4_API)
                    put("cli", DappScaffold.CLI_SERIES)
                }
            )
        }
    }

    /**
     * @param strict when true, missing production pins (compile.rellVersion,
     * merkle_hash_version) are ERRORS; by default they are warnings - official
     * Chromia configs (crc2-lib, directory1-example, vector-db-extension) omit
     * them and `chr build` accepts that (real-world round 2 D3). Genuinely
     * build-breaking findings (a rellVersion newer than the CLI's compiler, a
     * present-but-wrong merkle value, malformed values) stay errors regardless.
     */
    fun validate(yaml: String, strict: Boolean = false): Result {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        // Missing-pin findings: errors only in strict mode.
        val pinFindings = if (strict) errors else warnings
        val trimmed = yaml.trim()
        if (trimmed.isEmpty()) {
            return Result(false, listOf("chromia.yml is empty"), emptyList())
        }

        val root = try {
            SimpleYaml.parse(trimmed)
        } catch (e: IllegalArgumentException) {
            return Result(false, listOf("YAML parse error: ${e.message}"), emptyList())
        }

        val mapping = root as? YamlNode.Mapping
        if (mapping == null) {
            return Result(false, listOf("chromia.yml root must be a mapping"), emptyList())
        }

        val blockchains = mapping.mapping("blockchains")
        if (blockchains == null || blockchains.entries.isEmpty()) {
            errors += "blockchains is required and must list at least one chain"
        } else {
            blockchains.entries.forEach { (name, node) ->
                if (name.contains('-')) {
                    errors += "blockchains.$name: chain names cannot contain hyphens (CLI 0.20.14+ / directory-chain)"
                }
                val chain = node as? YamlNode.Mapping
                if (chain == null) {
                    errors += "blockchains.$name must be a mapping with module"
                    return@forEach
                }
                val module = chain.scalar("module")
                if (module.isNullOrBlank()) {
                    errors += "blockchains.$name.module is required (module name, not a file path)"
                } else if (
                    module.contains('/') ||
                    module.contains('\\') ||
                    module.contains(' ') ||
                    module.endsWith(".rell")
                ) {
                    errors += "blockchains.$name.module must be a module name (e.g. main), never a file path"
                }
                val webStatic = chain.entries["webStatic"]
                if (webStatic is YamlNode.Scalar) {
                    warnings += "blockchains.$name.webStatic deploy-frontend-dapp prints a directory path (out)"
                }
            }
        }

        val compile = mapping.mapping("compile")
        val rellVersion = compile?.scalar("rellVersion")
        if (rellVersion.isNullOrBlank()) {
            // chr builds fine without the pin (official configs omit it) - a
            // missing production pin is a warning unless strict (round 2 D3).
            pinFindings += "compile.rellVersion is missing (production pin $RELL_VERSION; the newest Rell the installed CLI ${DappScaffold.CLI_SERIES} accepts)"
        } else if (!RELL_VERSION_FORMAT.matches(rellVersion)) {
            errors += "compile.rellVersion must be a semver string N.N.N (found $rellVersion); production pin $RELL_VERSION"
        } else {
            val cmp = compareRellVersions(rellVersion, RELL_VERSION)
            when {
                cmp == null ->
                    warnings += "compile.rellVersion $rellVersion has components that could not be parsed as numbers; cannot compare against CLI-bundled Rell $RELL_VERSION"
                cmp > 0 ->
                    errors += "compile.rellVersion $rellVersion is newer than Rell $RELL_VERSION bundled with the installed Chromia CLI ${DappScaffold.CLI_SERIES}; " +
                        "the bundled compiler's SUPPORTED_VERSIONS list stops at $RELL_VERSION, so `chr build` will reject this project with an \"Unknown version\" error — pin $RELL_VERSION"
                cmp < 0 ->
                    warnings += "compile.rellVersion is $rellVersion; production pin is $RELL_VERSION (the Rell bundled with CLI ${DappScaffold.CLI_SERIES})"
            }
        }

        val merkleHits = mutableListOf<Pair<String, String>>()
        collectKeys(mapping, "") { path, key, node ->
            if (key.equals("require_mandatory_flags", ignoreCase = true)) {
                errors += "$path: require_mandatory_flags is not a chromia.yml / moduleArgs key; it belongs only on the main auth descriptor"
            }
            if (key.equals("max_auth_descriptor_rules", ignoreCase = true)) {
                warnings += "$path: /build/ft4/configuration-values sibling key; source binds auth_descriptor.max_rules (default 8)"
            }
            if (key == "merkle_hash_version") {
                val value = when (node) {
                    is YamlNode.Scalar -> node.raw
                    else -> node.toString()
                }
                merkleHits += path to value
            }
        }
        if (merkleHits.isEmpty()) {
            // Missing pin: warning unless strict (chr builds without it; round 2
            // D3). A present-but-WRONG value below stays an error.
            pinFindings += "merkle_hash_version is missing - production pin is $MERKLE_HASH_VERSION under blockchains.<name>.config.features (do not ship version 1)"
        } else {
            merkleHits.forEach { (path, value) ->
                val n = value.toIntOrNull()
                if (n != MERKLE_HASH_VERSION) {
                    errors += "$path: merkle_hash_version must be $MERKLE_HASH_VERSION (found $value)"
                }
            }
        }

        val libs = mapping.mapping("libs")
        if (libs != null) {
            libs.entries.forEach { (libName, node) ->
                forbiddenHit(libName)?.let { hit ->
                    errors += "libs.$libName: forbidden FT4 production module $hit"
                }
                val libMap = node as? YamlNode.Mapping
                val path = libMap?.scalar("path").orEmpty()
                forbiddenHit(path)?.let { hit ->
                    errors += "libs.$libName.path: forbidden FT4 production module $hit"
                }
                val insecure = libMap?.scalar("insecure")
                if (insecure.equals("true", ignoreCase = true)) {
                    warnings += "libs.$libName.insecure: true skips rid check; not recommended for production"
                }
            }
        }

        // moduleArgs KEYS name the module being CONFIGURED, not imported: setting
        // e.g. lib.ft4.core.admin's admin_pubkey is standard documented practice,
        // and applying the code-import blacklist to these keys false-flagged
        // official configs (real-world round 2 D3). Admin modules are dropped
        // from the KEY check; open-strategy names stay flagged, and importing an
        // admin module in code or pulling it via libs is still an error.
        if (blockchains != null) {
            blockchains.entries.forEach { (name, node) ->
                val chain = node as? YamlNode.Mapping ?: return@forEach
                val moduleArgs = chain.mapping("moduleArgs") ?: return@forEach
                moduleArgs.entries.keys.forEach { key ->
                    forbiddenHit(key)?.takeIf { it !in ADMIN_MODULES }?.let { hit ->
                        errors += "blockchains.$name.moduleArgs.$key: forbidden FT4 production module $hit"
                    }
                }
            }
        }

        // `!include other.yml` is official chr CLI syntax (our own
        // chromia_yml_definitions_help teaches it), but SimpleYaml does not
        // resolve includes: the included file parses as an opaque scalar, so
        // every check that would have run on its content - the libs
        // forbidden-module gate, the moduleArgs key checks, the merkle pin -
        // silently does not run (reality audit D4: `libs: !include libs.yml`
        // reached ok:true even when libs.yml pulled lib.ft4.admin). Say so
        // loudly: always at least a warning naming the file; in strict mode
        // the security-relevant positions (libs, moduleArgs) are ERRORS,
        // because a gate that cannot see that part of the config must not
        // report it clean.
        collectIncludes(mapping, "").forEach { (path, target) ->
            val sensitive = isSecuritySensitiveIncludePath(path)
            val message = "$path: `!include $target` is not resolved by this validator - the " +
                "content of $target was NOT validated" +
                (if (sensitive) {
                    " and the forbidden-module / moduleArgs key checks did not run on it"
                } else {
                    ""
                }) +
                "; inline the file or validate its content separately."
            if (sensitive && strict) errors += message else warnings += message
        }

        val driver = mapping.mapping("database")?.scalar("driver")
        if (!driver.isNullOrBlank() && driver != "org.postgresql.Driver") {
            warnings += "database.driver must be org.postgresql.Driver (found $driver)"
        }

        // Per-chain missing-merkle warnings only when the key exists SOMEWHERE:
        // with no key anywhere the global finding above already says so, and
        // reporting both double-counted one omission (real-world round 2 D3).
        if (blockchains != null && merkleHits.isNotEmpty()) {
            blockchains.entries.forEach { (name, node) ->
                val chain = node as? YamlNode.Mapping ?: return@forEach
                val merkle = chain.mapping("config")?.mapping("features")?.scalar("merkle_hash_version")
                if (merkle.isNullOrBlank()) {
                    warnings += "blockchains.$name.config.features.merkle_hash_version is missing (must be $MERKLE_HASH_VERSION)"
                }
            }
        }

        val deployments = mapping.mapping("deployments")
        if (deployments != null) {
            val chainNames = blockchains?.entries?.keys.orEmpty()
            deployments.entries.forEach { (net, node) ->
                val dep = node as? YamlNode.Mapping
                if (dep == null) {
                    errors += "deployments.$net must be a mapping with url / brid / container / chains"
                    return@forEach
                }
                val reserved = net in RESERVED_DEPLOYMENT_NAMES
                val brid = dep.scalar("brid")
                val hasUrl = mappingHasUrl(dep)
                val container = dep.scalar("container")
                if (!reserved && (brid.isNullOrBlank() || !hasUrl)) {
                    errors += "deployments.$net: custom names require brid and url (only reserved names mainnet / testnet auto-fill Directory brid + url)"
                }
                if (!brid.isNullOrBlank()) {
                    val hex = normalizeDirectoryBrid(brid)
                    if (hex == null || hex.length != DIRECTORY_BRID_HEX_LENGTH) {
                        val found = hex?.length ?: 0
                        errors += "deployments.$net.brid must be a $DIRECTORY_BRID_HEX_LENGTH-hex Directory Chain RID (x\"..\"); found length $found"
                    } else if (reserved) {
                        val official = officialDirectoryBrid(net)
                        if (official != null && hex != official) {
                            errors += "deployments.$net.brid must be the official $net Directory Chain RID $official (found $hex)"
                        }
                    }
                }
                if (container.isNullOrBlank()) {
                    warnings += "deployments.$net.container is missing (Vault / PMC lease id required for chr deployment create)"
                }
                val chains = dep.mapping("chains")
                if (chains != null && chainNames.isNotEmpty()) {
                    chains.entries.keys.forEach { chainName ->
                        if (chainName !in chainNames) {
                            warnings += "deployments.$net.chains.$chainName does not match a blockchains name"
                        }
                    }
                }
                // `chains.<name>: null` is the pre-0.30 placeholder agents still
                // write; chr rejects it, and this validator passed it with only the
                // container warning (DX audit 2026-09-04, Q7). Every chains value
                // must be the dapp RID `chr deployment create` writes.
                chains?.entries?.forEach { (chainName, valueNode) ->
                    val raw = (valueNode as? YamlNode.Scalar)?.raw?.trim()
                    val placeholder = raw == null || raw.isEmpty() || raw.equals("null", ignoreCase = true) || raw == "~"
                    val hex = raw?.let { normalizeDirectoryBrid(it) }
                    if (placeholder) {
                        errors += "deployments.$net.chains.$chainName is empty/null - chr rejects a placeholder there." +
                            " Remove the chains key until `chr deployment create` writes the dapp RID into it (CLI 0.30.0+)," +
                            " or set the real value: x\"<64-hex dapp blockchain RID>\"."
                    } else if (hex == null || hex.length != DIRECTORY_BRID_HEX_LENGTH) {
                        errors += "deployments.$net.chains.$chainName must be the dapp's $DIRECTORY_BRID_HEX_LENGTH-hex blockchain RID" +
                            " (x\"...\"), as written by `chr deployment create`; found \"${raw.take(40)}\"."
                    }
                }
            }
        }

        return Result(errors.isEmpty(), errors, warnings)
    }

    /** Admin modules: banned as code imports / libs, but VALID as moduleArgs keys (configuring admin_pubkey). */
    internal val ADMIN_MODULES = setOf("lib.ft4.admin", "lib.ft4.core.admin", "admin.crosschain")

    internal const val DIRECTORY_BRID_HEX_LENGTH = WriteDeploymentConfig.DIRECTORY_BRID_HEX_LENGTH
    internal val RESERVED_DEPLOYMENT_NAMES = setOf("mainnet", "testnet")
    internal val RELL_VERSION_FORMAT = Regex("""^\d+\.\d+\.\d+$""")

    /**
     * Compares two dotted numeric version strings component by component, so
     * "0.16.10" > "0.16.9" (plain string comparison would get this wrong) and
     * "1.0" == "1.0.0" (missing components count as 0). Returns a negative /
     * zero / positive Int like compareTo, or null when either version has a
     * non-numeric or empty component (malformed input never throws).
     */
    internal fun compareRellVersions(a: String, b: String): Int? {
        val left = parseVersionComponents(a) ?: return null
        val right = parseVersionComponents(b) ?: return null
        val size = maxOf(left.size, right.size)
        for (i in 0 until size) {
            val l = left.getOrElse(i) { 0 }
            val r = right.getOrElse(i) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    private fun parseVersionComponents(version: String): List<Int>? {
        val trimmed = version.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split('.')
        val out = ArrayList<Int>(parts.size)
        for (part in parts) {
            val n = part.toIntOrNull() ?: return null
            if (n < 0) return null
            out += n
        }
        return out
    }

    internal fun officialDirectoryBrid(net: String): String? = when (net) {
        "mainnet" -> WriteDeploymentConfig.MAINNET_DIRECTORY_BRID
        "testnet" -> WriteDeploymentConfig.TESTNET_DIRECTORY_BRID
        else -> null
    }

    internal fun mappingHasUrl(dep: YamlNode.Mapping): Boolean {
        val node = dep.entries["url"] ?: return false
        return when (node) {
            is YamlNode.Scalar -> node.raw.isNotBlank()
            is YamlNode.Sequence -> node.items.any { child ->
                child is YamlNode.Scalar && child.raw.isNotBlank()
            }
            else -> false
        }
    }

    internal fun normalizeDirectoryBrid(raw: String): String? {
        var s = raw.trim()
        if (s.length >= 3 && s.startsWith("x\"") && s.endsWith("\"")) {
            s = s.substring(2, s.length - 1)
        } else if (s.length >= 3 && s.startsWith("x'") && s.endsWith("'")) {
            s = s.substring(2, s.length - 1)
        } else if (s.length >= 2 && s.first() == '"' && s.last() == '"') {
            s = s.substring(1, s.length - 1)
        }
        s = s.trim()
        if (s.isEmpty()) return ""
        if (!s.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        return s.uppercase()
    }

    private fun forbiddenHit(text: String): String? {
        val lower = text.lowercase()
        val segments = lower.split('/', '.').filter { it.isNotEmpty() }
        return forbiddenLibModules.firstOrNull { hit ->
            val hitLower = hit.lowercase()
            if ('.' in hitLower) {
                containsSegmentRun(segments, hitLower.split('.'))
            } else {
                segments.any { segment -> containsWord(segment, hitLower) }
            }
        }
    }

    /** True when [run] appears as a contiguous run of whole segments inside [segments]. */
    private fun containsSegmentRun(segments: List<String>, run: List<String>): Boolean {
        if (run.isEmpty() || run.size > segments.size) return false
        return (0..segments.size - run.size).any { start ->
            run.indices.all { j -> segments[start + j] == run[j] }
        }
    }

    /** True when [word] appears in [segment] with no identifier char (letter/digit/_) on either side. */
    private fun containsWord(segment: String, word: String): Boolean {
        var from = 0
        while (true) {
            val idx = segment.indexOf(word, from)
            if (idx < 0) return false
            val before = segment.getOrNull(idx - 1)
            val after = segment.getOrNull(idx + word.length)
            val ident = { ch: Char? -> ch != null && (ch.isLetterOrDigit() || ch == '_') }
            if (!ident(before) && !ident(after)) return true
            from = idx + 1
        }
    }

    /**
     * Every unresolved `!include` in the tree as (path, included file). The
     * scalar shape is what SimpleYaml produces for a tagged value; a missing
     * file name is reported as <unnamed> rather than dropped.
     */
    internal fun collectIncludes(node: YamlNode, path: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        fun walk(n: YamlNode, p: String) {
            when (n) {
                is YamlNode.Scalar -> {
                    val raw = n.raw.trim()
                    if (raw == "!include" || raw.startsWith("!include ")) {
                        val target = raw.removePrefix("!include").trim().ifEmpty { "<unnamed>" }
                        out += p to target
                    }
                }
                is YamlNode.Mapping -> n.entries.forEach { (key, child) ->
                    walk(child, if (p.isEmpty()) key else "$p.$key")
                }
                is YamlNode.Sequence -> n.items.forEachIndexed { i, child -> walk(child, "$p[$i]") }
            }
        }
        walk(node, path)
        return out
    }

    /**
     * Positions whose content feeds the forbidden-module / key checks: the
     * root `libs` block and any `moduleArgs` block. An include there hides
     * exactly the config the security gates exist for.
     */
    internal fun isSecuritySensitiveIncludePath(path: String): Boolean {
        val segments = path.split('.').map { it.substringBefore('[') }
        return segments.firstOrNull() == "libs" || "moduleArgs" in segments
    }

    private fun collectKeys(
        node: YamlNode,
        path: String,
        visit: (path: String, key: String, value: YamlNode) -> Unit
    ) {
        when (node) {
            is YamlNode.Mapping -> node.entries.forEach { (key, child) ->
                val childPath = if (path.isEmpty()) key else "$path.$key"
                visit(childPath, key, child)
                collectKeys(child, childPath, visit)
            }
            is YamlNode.Sequence -> node.items.forEachIndexed { i, child ->
                collectKeys(child, "$path[$i]", visit)
            }
            is YamlNode.Scalar -> Unit
        }
    }
}

internal sealed class YamlNode {
    data class Scalar(val raw: String) : YamlNode()
    data class Mapping(val entries: LinkedHashMap<String, YamlNode> = LinkedHashMap()) : YamlNode() {
        fun mapping(key: String): Mapping? = entries[key] as? Mapping
        fun scalar(key: String): String? = (entries[key] as? Scalar)?.raw
    }
    data class Sequence(val items: List<YamlNode>) : YamlNode()
}

/**
 * Indent-based YAML subset used by chromia.yml (maps, lists, scalars, flow lists,
 * anchors/aliases/merge keys, multi-key mappings in sequence items).
 * Not a full YAML 1.1 engine: no !include resolution, no multiline blocks.
 */
internal object SimpleYaml {
    fun parse(text: String): YamlNode {
        val lines = preprocess(text)
        if (lines.isEmpty()) return YamlNode.Mapping()
        // Anchors are per-parse state (this is a shared object; no globals).
        val (node, next) = parseBlock(lines, 0, 0, mutableMapOf())
        if (next < lines.size) {
            throw IllegalArgumentException("unexpected content at line ${lines[next].number}")
        }
        return node
    }

    private data class Line(val number: Int, val indent: Int, val content: String)

    private fun preprocess(text: String): List<Line> {
        // A tab in the indentation is the mistake, not the "bad indent" it
        // produces one line later once tabs are widened to two spaces (DX audit
        // 2026-09-04, Q2): name the line with the tab. YAML forbids tabs for
        // indentation, so chr's parser rejects the file too.
        text.lines().forEachIndexed { index, raw ->
            if (raw.takeWhile { it == ' ' || it == '\t' }.contains('\t') && stripComment(raw).isNotBlank()) {
                throw IllegalArgumentException(
                    "tab character in the indentation of line ${index + 1} - YAML indentation must be spaces only" +
                        " (chr rejects tabs as well); replace the tab(s) with spaces"
                )
            }
        }
        return text.replace("\t", "  ").lines().mapIndexedNotNull { index, raw ->
            val noComment = stripComment(raw)
            if (noComment.isBlank()) null
            else {
                val indent = noComment.takeWhile { it == ' ' }.length
                val content = noComment.trim()
                if (indent == 0 && (content == "---" || content == "...")) null
                else Line(index + 1, indent, content)
            }
        }
    }

    private fun stripComment(line: String): String {
        var inSingle = false
        var inDouble = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            val prev = if (i > 0) line[i - 1] else null
            val opensQuote = prev == null || prev == ' ' || prev == '[' || prev == ','
            when {
                // `\"` inside a double-quoted scalar must not close the quote:
                // a ` #` after it was treated as a comment and truncated the
                // value mid-string (audit F4). `''` doubling in single quotes is
                // handled by the plain close/reopen toggle.
                inDouble && c == '\\' && i + 1 < line.length -> {
                    i += 2
                    continue
                }
                inSingle -> if (c == '\'') inSingle = false
                inDouble -> if (c == '"') inDouble = false
                c == '\'' && opensQuote -> inSingle = true
                c == '"' && opensQuote -> inDouble = true
                c == '#' && (prev == null || prev == ' ') -> return line.substring(0, i)
            }
            i++
        }
        return line
    }

    private fun parseBlock(
        lines: List<Line>,
        start: Int,
        minIndent: Int,
        anchors: MutableMap<String, YamlNode>
    ): Pair<YamlNode, Int> {
        if (start >= lines.size) return YamlNode.Mapping() to start
        val first = lines[start]
        if (first.indent < minIndent) return YamlNode.Mapping() to start
        return if (first.content.startsWith("- ") || first.content == "-") {
            parseSequence(lines, start, first.indent, anchors)
        } else {
            parseMapping(lines, start, first.indent, anchors)
        }
    }

    /** `&name rest` -> (name, "rest"); no anchor -> (null, value). */
    private val ANCHOR_REGEX = Regex("""^&([^\s\[\]{},*&]+)\s*(.*)$""")

    private fun stripAnchor(value: String): Pair<String?, String> {
        val m = ANCHOR_REGEX.find(value.trim()) ?: return null to value.trim()
        return m.groupValues[1] to m.groupValues[2].trim()
    }

    private fun resolveAlias(value: String, lineNumber: Int, anchors: Map<String, YamlNode>): YamlNode {
        val name = value.removePrefix("*").trim()
        return anchors[name]
            ?: throw IllegalArgumentException("unknown alias *$name at line $lineNumber (anchors must be defined before use)")
    }

    /**
     * YAML merge key `<<: *anchor` (or `<<: [*a, *b]`): merge the referenced
     * mapping's entries in; keys explicitly present in the mapping win, and for
     * multiple merges the earlier merge wins (YAML merge-key semantics).
     * Production chromia.yml files build per-network chain configs this way
     * (filehub's `config: <<: *gtx` - real-world round 1).
     */
    private fun mergeInto(merged: LinkedHashMap<String, YamlNode>, node: YamlNode, lineNumber: Int) {
        val mapping = node as? YamlNode.Mapping
            ?: throw IllegalArgumentException("merge key << references a non-mapping at line $lineNumber")
        mapping.entries.forEach { (k, v) -> merged.putIfAbsent(k, v) }
    }

    private fun parseMapping(
        lines: List<Line>,
        start: Int,
        indent: Int,
        anchors: MutableMap<String, YamlNode>
    ): Pair<YamlNode, Int> {
        val entries = LinkedHashMap<String, YamlNode>()
        val merged = LinkedHashMap<String, YamlNode>()
        var i = start
        while (i < lines.size) {
            val line = lines[i]
            if (line.indent < indent) break
            if (line.indent > indent) {
                throw IllegalArgumentException("bad indent at line ${line.number}")
            }
            if (line.content.startsWith("-")) {
                throw IllegalArgumentException("sequence item in mapping at line ${line.number}")
            }
            val colon = splitKeyValue(line.content)
                ?: throw IllegalArgumentException("expected key: value at line ${line.number}")
            val key = unquote(colon.first)
            // `moduleArgs: &name` anchors the nested block that follows;
            // `moduleArgs: *name` reuses one (dapp-aggregator - real-world round 1).
            val (anchorName, rest) = stripAnchor(colon.second)
            if (rest.isEmpty()) {
                val next = i + 1
                val child: YamlNode
                if (next < lines.size && lines[next].indent > indent) {
                    val (node, after) = parseBlock(lines, next, indent + 1, anchors)
                    child = node
                    i = after
                } else if (
                    next < lines.size && lines[next].indent == indent &&
                    (lines[next].content.startsWith("- ") || lines[next].content == "-")
                ) {
                    // Legal YAML: sequence items may sit at the same indent as their parent key.
                    val (node, after) = parseSequence(lines, next, indent, anchors)
                    child = node
                    i = after
                } else {
                    child = YamlNode.Mapping()
                    i++
                }
                anchorName?.let { anchors[it] = child }
                entries[key] = child
            } else if (rest.startsWith("*")) {
                val node = resolveAlias(rest, line.number, anchors)
                anchorName?.let { anchors[it] = node }
                if (key == "<<") mergeInto(merged, node, line.number) else entries[key] = node
                i++
            } else if (key == "<<" && rest.startsWith("[")) {
                // `<<: [*a, *b]` - flow list of aliases; earlier aliases win.
                val inner = rest.removePrefix("[").removeSuffix("]")
                inner.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
                    mergeInto(merged, resolveAlias(part, line.number, anchors), line.number)
                }
                i++
            } else {
                val node = parseScalarOrFlow(rest)
                anchorName?.let { anchors[it] = node }
                entries[key] = node
                i++
            }
        }
        // Explicit keys override merged ones regardless of position (YAML spec).
        merged.forEach { (k, v) -> entries.putIfAbsent(k, v) }
        return YamlNode.Mapping(entries) to i
    }

    private fun parseSequence(
        lines: List<Line>,
        start: Int,
        indent: Int,
        anchors: MutableMap<String, YamlNode>
    ): Pair<YamlNode, Int> {
        val items = mutableListOf<YamlNode>()
        var i = start
        while (i < lines.size) {
            val line = lines[i]
            if (line.indent < indent) break
            if (line.indent > indent) {
                throw IllegalArgumentException("bad indent at line ${line.number}")
            }
            if (!(line.content.startsWith("- ") || line.content == "-")) {
                // Sequence ended; a sibling key at the same indent continues the parent mapping.
                break
            }
            val rawRest = if (line.content == "-") "" else line.content.removePrefix("- ").trim()
            // `- &gtx` anchors the block item that follows (filehub filechain -
            // real-world round 1); `- *alias` reuses an anchored node.
            val (anchorName, rest) = stripAnchor(rawRest)
            if (rest.isEmpty()) {
                val next = i + 1
                val child: YamlNode
                if (next < lines.size && lines[next].indent > indent) {
                    val (node, after) = parseBlock(lines, next, indent + 1, anchors)
                    child = node
                    i = after
                } else {
                    child = YamlNode.Scalar("")
                    i++
                }
                anchorName?.let { anchors[it] = child }
                items += child
            } else if (rest.startsWith("*")) {
                items += resolveAlias(rest, line.number, anchors)
                i++
            } else if (
                rest.contains(':') && !rest.startsWith("[") && !rest.startsWith("{") &&
                !isQuoted(rest) && splitKeyValue(rest) != null
            ) {
                // `{` must route to parseScalarOrFlow below: without the guard,
                // `- { require_mandatory_flags: true }` split at the colon into a
                // mangled Mapping("{ require_mandatory_flags" -> "true }"), so
                // collectKeys rules never saw keys inside flow mappings in block
                // sequences (audit F3 follow-up).
                //
                // A block mapping starting on the dash line may continue with
                // sibling keys on the following lines (`- topic: x` then
                // `bc-rid: y` two columns in) - every production chromia.yml
                // with FT4 transfer rules or ICMF receivers uses this shape,
                // and the old single-key special case mis-reported it as "bad
                // indent" (real-world round 1). Re-parse the item as a mapping
                // whose first line is the content after "- ".
                val itemIndent = line.indent + 2
                val sub = ArrayList(lines)
                sub[i] = Line(line.number, itemIndent, rest)
                val (child, after) = parseMapping(sub, i, itemIndent, anchors)
                anchorName?.let { anchors[it] = child }
                items += child
                i = after
            } else {
                val node = parseScalarOrFlow(rest)
                anchorName?.let { anchors[it] = node }
                items += node
                i++
            }
        }
        return YamlNode.Sequence(items) to i
    }

    private fun parseScalarOrFlow(raw: String): YamlNode {
        val value = raw.trim()
        if (value.startsWith("[") && value.endsWith("]")) {
            val inner = value.substring(1, value.length - 1).trim()
            if (inner.isEmpty()) return YamlNode.Sequence(emptyList())
            val items = splitFlow(inner).map { parseScalarOrFlow(it) }
            return YamlNode.Sequence(items)
        }
        // Flow mapping `{ features: { merkle_hash_version: 2 } }` used to fall
        // through to a Scalar, so the keys inside vanished: the merkle rule then
        // hard-errored on a CLI-valid config, and flow-style moduleArgs dodged the
        // forbidden-module scan (audit F3). Parse it into the same Mapping shape
        // the block parser produces.
        if (value.startsWith("{") && value.endsWith("}")) {
            val inner = value.substring(1, value.length - 1).trim()
            if (inner.isEmpty()) return YamlNode.Mapping()
            val entries = LinkedHashMap<String, YamlNode>()
            for (part in splitFlow(inner)) {
                val colon = splitFlowKeyValue(part)
                    // A brace blob we cannot split (`{not yaml}`) stays an opaque
                    // scalar, like before - never a wrong hard error.
                    ?: return YamlNode.Scalar(unquote(value))
                entries[unquote(colon.first)] = parseScalarOrFlow(colon.second)
            }
            return YamlNode.Mapping(entries)
        }
        return YamlNode.Scalar(unquote(value))
    }

    /**
     * Splits one flow-mapping entry at its key colon. [splitKeyValue] requires
     * a space after ':' (so plain scalars like https://host:7740 survive), which
     * also covers block-style flow content; JSON-style `"key":value` without the
     * space is additionally accepted when the key is quoted.
     */
    private fun splitFlowKeyValue(part: String): Pair<String, String>? {
        splitKeyValue(part)?.let { return it }
        val trimmed = part.trim()
        val quote = trimmed.firstOrNull()
        if (quote == '"' || quote == '\'') {
            // Honor `\"` escapes when scanning for the closing double quote
            // (audit F4) - `"a\"b": v` must not end the key at the escaped quote.
            var end = -1
            var j = 1
            while (j < trimmed.length) {
                val c = trimmed[j]
                if (quote == '"' && c == '\\' && j + 1 < trimmed.length) {
                    j += 2
                    continue
                }
                if (c == quote) {
                    end = j
                    break
                }
                j++
            }
            if (end > 0 && trimmed.getOrNull(end + 1) == ':') {
                return trimmed.substring(0, end + 1) to trimmed.substring(end + 2).trim()
            }
        }
        return null
    }

    private fun splitFlow(inner: String): List<String> {
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        var inSingle = false
        var inDouble = false
        // Nested flow nodes ([a, b] / {k: v}) must not be split at their inner
        // commas - only depth-0 commas separate entries (audit F3).
        var depth = 0
        var i = 0
        while (i < inner.length) {
            val c = inner[i]
            when {
                // YAML double-quoted scalars escape with backslash: `\"` must not
                // toggle the quote state, or everything after it leaks outside the
                // string and swallows subsequent entries (audit F4). Single-quoted
                // scalars escape by doubling `''`, which the plain toggle already
                // handles (close + immediate reopen).
                inDouble && c == '\\' && i + 1 < inner.length -> {
                    buf.append(c).append(inner[i + 1])
                    i += 2
                    continue
                }
                c == '\'' && !inDouble -> { inSingle = !inSingle; buf.append(c) }
                c == '"' && !inSingle -> { inDouble = !inDouble; buf.append(c) }
                inSingle || inDouble -> buf.append(c)
                c == '[' || c == '{' -> { depth++; buf.append(c) }
                c == ']' || c == '}' -> { depth--; buf.append(c) }
                c == ',' && depth == 0 -> {
                    out += buf.toString().trim()
                    buf.clear()
                }
                else -> buf.append(c)
            }
            i++
        }
        if (buf.isNotBlank()) out += buf.toString().trim()
        return out
    }

    private fun splitKeyValue(content: String): Pair<String, String>? {
        var inSingle = false
        var inDouble = false
        var i = 0
        while (i < content.length) {
            val c = content[i]
            val next = content.getOrNull(i + 1)
            when {
                // `\"` inside a double-quoted scalar must not toggle the quote
                // state (audit F4); `''` doubling is handled by the plain toggle.
                inDouble && c == '\\' && next != null -> {
                    i += 2
                    continue
                }
                c == '\'' && !inDouble -> inSingle = !inSingle
                c == '"' && !inSingle -> inDouble = !inDouble
                // YAML: ':' separates key and value only when followed by whitespace or end of line,
                // so plain scalars like https://host:7740 stay scalars.
                c == ':' && !inSingle && !inDouble && (next == null || next == ' ') -> {
                    val key = content.substring(0, i).trim()
                    val value = content.substring(i + 1).trim()
                    if (key.isEmpty()) return null
                    return key to value
                }
            }
            i++
        }
        return null
    }

    private fun isQuoted(value: String): Boolean {
        val v = value.trim()
        return (v.length >= 2 && v.first() == '"' && v.last() == '"') ||
            (v.length >= 2 && v.first() == '\'' && v.last() == '\'')
    }

    private fun unquote(value: String): String {
        val v = value.trim()
        return if (isQuoted(v)) v.substring(1, v.length - 1) else v
    }
}

/**
 * The module_args a chromia.yml declares, in the shape `run_rell_tests` takes.
 *
 * AUDIT F4 (2026-09-06): the flagship `ft4` template's own shipped tests failed
 * every case on the first honest `run_rell_tests{files}` from the scaffold
 * output - 22,131 ms to a red - with
 *   "System function 'rell.test.tx.run': Block execution failed: ...
 *    Unable to create GTX module: net.postchain.rell.module.RellPostchainModuleFactory"
 * because `moduleArgs` and `test.moduleArgs` were never merged: the tool takes
 * module args as a PARAMETER and never read the yml the scaffold had just
 * handed back. Assembling the merge by hand cost another 35,824 ms, and the
 * instructions for doing it were 14 KB inside a 22 KB notes blob.
 *
 * `chr test` merges the two blocks; so does this. Test-scoped args win on a key
 * collision, which is what `chr test` does and what the FT4 admin wiring needs.
 */
object ChromiaYmlModuleArgs {

    /**
     * `blockchains.<any>.moduleArgs` merged with `test.moduleArgs`, keyed by
     * Rell module name. Empty when the yml declares none, or does not parse -
     * this is a convenience, never a gate.
     */
    fun merged(yaml: String): Map<String, Map<String, kotlinx.serialization.json.JsonElement>> {
        val root = runCatching { SimpleYaml.parse(yaml.trim()) }.getOrNull() as? YamlNode.Mapping ?: return emptyMap()
        val out = LinkedHashMap<String, MutableMap<String, kotlinx.serialization.json.JsonElement>>()
        fun absorb(node: YamlNode?) {
            val mapping = node as? YamlNode.Mapping ?: return
            mapping.entries.forEach { (module, args) ->
                val argsMapping = args as? YamlNode.Mapping ?: return@forEach
                val target = out.getOrPut(module) { LinkedHashMap() }
                argsMapping.entries.forEach { (key, value) -> target[key] = toJson(value) }
            }
        }
        root.mapping("blockchains")?.entries?.values?.forEach { chain ->
            absorb((chain as? YamlNode.Mapping)?.mapping("moduleArgs"))
        }
        // Test-scoped last: `chr test` lets test.moduleArgs win, and the FT4
        // admin wiring (lib.ft4.core.admin, lib.ft4.test.core.auth) lives only there.
        absorb(root.mapping("test")?.mapping("moduleArgs"))
        return out.mapValues { (_, v) -> v.toMap() }
    }

    /**
     * Scalars keep the type the yml wrote: integers and booleans as such, and
     * everything else - including the `x"..."` byte_array literal - as the
     * string the tool's own GTV conversion already accepts.
     */
    private fun toJson(node: YamlNode): kotlinx.serialization.json.JsonElement = when (node) {
        is YamlNode.Scalar -> scalarToJson(node.raw)
        is YamlNode.Sequence -> buildJsonArray { node.items.forEach { add(toJson(it)) } }
        is YamlNode.Mapping -> buildJsonObject { node.entries.forEach { (k, v) -> put(k, toJson(v)) } }
    }

    private fun scalarToJson(raw: String): kotlinx.serialization.json.JsonElement {
        val t = raw.trim()
        t.toLongOrNull()?.let { return JsonPrimitive(it) }
        if (t.equals("true", ignoreCase = true)) return JsonPrimitive(true)
        if (t.equals("false", ignoreCase = true)) return JsonPrimitive(false)
        return JsonPrimitive(t)
    }
}
