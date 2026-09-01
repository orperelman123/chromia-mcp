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
    val leftoverOfficialBlockchainKeys = setOf("webStatic")
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

    fun validate(yaml: String): Result {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
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
            errors += "compile.rellVersion is required (production pin $RELL_VERSION; the newest Rell the installed CLI ${DappScaffold.CLI_SERIES} accepts)"
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
            errors += "merkle_hash_version must be $MERKLE_HASH_VERSION under blockchains.<name>.config.features (do not ship version 1)"
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

        // moduleArgs live under each chain; admin / ras_open must not ship in production.
        if (blockchains != null) {
            blockchains.entries.forEach { (name, node) ->
                val chain = node as? YamlNode.Mapping ?: return@forEach
                val moduleArgs = chain.mapping("moduleArgs") ?: return@forEach
                moduleArgs.entries.keys.forEach { key ->
                    forbiddenHit(key)?.let { hit ->
                        errors += "blockchains.$name.moduleArgs.$key: forbidden FT4 production module $hit"
                    }
                }
            }
        }

        val driver = mapping.mapping("database")?.scalar("driver")
        if (!driver.isNullOrBlank() && driver != "org.postgresql.Driver") {
            warnings += "database.driver must be org.postgresql.Driver (found $driver)"
        }

        if (blockchains != null) {
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
            }
        }

        return Result(errors.isEmpty(), errors, warnings)
    }

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
 * Indent-based YAML subset used by chromia.yml (maps, lists, scalars, flow lists).
 * Not a full YAML 1.1 engine: no aliases, no !include resolution, no multiline blocks.
 */
internal object SimpleYaml {
    fun parse(text: String): YamlNode {
        val lines = preprocess(text)
        if (lines.isEmpty()) return YamlNode.Mapping()
        val (node, next) = parseBlock(lines, 0, 0)
        if (next < lines.size) {
            throw IllegalArgumentException("unexpected content at line ${lines[next].number}")
        }
        return node
    }

    private data class Line(val number: Int, val indent: Int, val content: String)

    private fun preprocess(text: String): List<Line> {
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

    private fun parseBlock(lines: List<Line>, start: Int, minIndent: Int): Pair<YamlNode, Int> {
        if (start >= lines.size) return YamlNode.Mapping() to start
        val first = lines[start]
        if (first.indent < minIndent) return YamlNode.Mapping() to start
        return if (first.content.startsWith("- ") || first.content == "-") {
            parseSequence(lines, start, first.indent)
        } else {
            parseMapping(lines, start, first.indent)
        }
    }

    private fun parseMapping(lines: List<Line>, start: Int, indent: Int): Pair<YamlNode, Int> {
        val entries = LinkedHashMap<String, YamlNode>()
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
            val rest = colon.second
            if (rest.isEmpty()) {
                val next = i + 1
                if (next < lines.size && lines[next].indent > indent) {
                    val (child, after) = parseBlock(lines, next, indent + 1)
                    entries[key] = child
                    i = after
                } else if (
                    next < lines.size && lines[next].indent == indent &&
                    (lines[next].content.startsWith("- ") || lines[next].content == "-")
                ) {
                    // Legal YAML: sequence items may sit at the same indent as their parent key.
                    val (child, after) = parseSequence(lines, next, indent)
                    entries[key] = child
                    i = after
                } else {
                    entries[key] = YamlNode.Mapping()
                    i++
                }
            } else {
                entries[key] = parseScalarOrFlow(rest)
                i++
            }
        }
        return YamlNode.Mapping(entries) to i
    }

    private fun parseSequence(lines: List<Line>, start: Int, indent: Int): Pair<YamlNode, Int> {
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
            val rest = if (line.content == "-") "" else line.content.removePrefix("- ").trim()
            if (rest.isEmpty()) {
                val next = i + 1
                if (next < lines.size && lines[next].indent > indent) {
                    val (child, after) = parseBlock(lines, next, indent + 1)
                    items += child
                    i = after
                } else {
                    items += YamlNode.Scalar("")
                    i++
                }
            } else if (rest.contains(':') && !rest.startsWith("[") && !rest.startsWith("{") && !isQuoted(rest)) {
                // `{` must route to parseScalarOrFlow below: without the guard,
                // `- { require_mandatory_flags: true }` split at the colon into a
                // mangled Mapping("{ require_mandatory_flags" -> "true }"), so
                // collectKeys rules never saw keys inside flow mappings in block
                // sequences (audit F3 follow-up).
                val colon = splitKeyValue(rest)
                if (colon != null && colon.second.isEmpty()) {
                    val next = i + 1
                    if (next < lines.size && lines[next].indent > indent) {
                        val (nested, afterNested) = parseBlock(lines, next, indent + 1)
                        items += YamlNode.Mapping(linkedMapOf(unquote(colon.first) to nested))
                        i = afterNested
                    } else {
                        items += YamlNode.Mapping(linkedMapOf(unquote(colon.first) to YamlNode.Mapping()))
                        i++
                    }
                } else if (colon != null) {
                    items += YamlNode.Mapping(linkedMapOf(unquote(colon.first) to parseScalarOrFlow(colon.second)))
                    i++
                } else {
                    items += parseScalarOrFlow(rest)
                    i++
                }
            } else {
                items += parseScalarOrFlow(rest)
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
