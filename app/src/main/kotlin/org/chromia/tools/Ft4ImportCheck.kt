package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Scan a Rell source string for forbidden FT4 production imports.
 * Flags lib.ft4.admin, admin.crosschain, ras_open, ras_transfer_open
 * (and the rest of DappScaffold.forbiddenModules).
 * Does not run chr, generate keys, or send signed txs.
 */
object Ft4ImportCheck {
    const val IMPORTS_URL = "https://docs.chromia.com/build/ft4/setup/imports"
    const val CONFIG_VALUES_URL = "https://docs.chromia.com/build/ft4/configuration-values"
    const val RELEASES_URL = "https://docs.chromia.com/build/ft4/releases/ft4"
    const val RELEASES_404_URL = "https://docs.chromia.com/build/ft4/releases"
    const val CROSSCHAIN_IMPORT = "lib.ft4.crosschain"
    const val CROSSCHAIN_LIST_LABEL = "cross-chain"
    val officialModules = listOf(
        "accounts",
        "admin  # list; NEVER import in production",
        "admin.crosschain  # list; NEVER import in production",
        "assets",
        "auth",
        "cross-chain  # list label; official import is lib.ft4.crosschain",
        "prioritization",
        "test  # tests only; exposes no external functions",
        "utils  # pagination; exposes no external functions"
    )
    val officialImports = listOf(
        "import lib.ft4.<module_name>  # public import (entities + mounted ops/queries)",
        "import lib.ft4.core.<module_name>  # core import (no externals)",
        "import lib.ft4.assets",
        "import lib.ft4.core.assets",
        "import lib.ft4.crosschain;  # hyphenated list name is not the import"
    )
    val forbidden = DappScaffold.forbiddenModules

    data class Hit(val module: String, val line: Int, val excerpt: String)

    data class Result(
        val ok: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
        val hits: List<Hit>,
        val exemptedLibFiles: Int = 0
    ) {
        fun toJson() = buildJsonObject {
            put("ok", ok)
            put("errors", buildJsonArray { errors.forEach { add(JsonPrimitive(it)) } })
            put("warnings", buildJsonArray { warnings.forEach { add(JsonPrimitive(it)) } })
            put(
                "hits",
                buildJsonArray {
                    hits.forEach { hit ->
                        add(
                            buildJsonObject {
                                put("module", hit.module)
                                put("line", hit.line)
                                put("excerpt", hit.excerpt)
                            }
                        )
                    }
                }
            )
            put(
                "forbidden",
                buildJsonArray { forbidden.forEach { add(JsonPrimitive(it)) } }
            )
            put("ft4Version", DappScaffold.FT4_VERSION)
            put("ft4Api", DappScaffold.FT4_API)
            put("imports_docs", IMPORTS_URL)
            put("config_values_docs", CONFIG_VALUES_URL)
            put("releases_docs", RELEASES_URL)
            put("releases_404", RELEASES_404_URL)
            put("crosschain_import", CROSSCHAIN_IMPORT)
            put(
                "official_modules",
                buildJsonArray { officialModules.forEach { add(JsonPrimitive(it)) } }
            )
            put(
                "official_imports",
                buildJsonArray { officialImports.forEach { add(JsonPrimitive(it)) } }
            )
            put(
                "notes",
                if (exemptedLibFiles > 0) {
                    RellLibs.exemptedFt4Note(exemptedLibFiles) + "\n" + notes()
                } else {
                    notes()
                }
            )
        }
    }

    fun notes(): String = """
        FT4 ${DappScaffold.FT4_VERSION} API ${DappScaffold.FT4_API} import check.
        Official imports (200): $IMPORTS_URL
        Official configuration-values (200): $CONFIG_VALUES_URL
        Official releases (200): $RELEASES_URL  Official $RELEASES_404_URL is 404.
        Official modules: accounts, assets, auth, cross-chain (import $CROSSCHAIN_IMPORT), prioritization, test, utils.
        Official admin / admin.crosschain: NEVER import in production. Official printed sample admin pubkey is skipped.
        Official public vs core: `import lib.ft4.<module>` mounts user ops/queries; `import lib.ft4.core.<module>` does not.
        Official list label `$CROSSCHAIN_LIST_LABEL` is not an import path — official import is `$CROSSCHAIN_IMPORT`.
        NEVER import ${forbidden.joinToString(", ")} in production dApps.
        require_mandatory_flags only on the main auth descriptor.
        Comments (`//`, `/* */`) are ignored. This tool does not run chr
        and does not send signed transactions.
    """.trimIndent()

    /**
     * @param allowAdminModules downgrade forbidden-module findings from errors
     * to warnings (each tagged "(allowed by allowAdminModules)") - the escape
     * hatch for deliberately building admin/ops tooling. Default behavior and
     * the forbidden list itself are unchanged.
     */
    fun scan(rell: String, allowAdminModules: Boolean = false): Result {
        // Mask string literals as well as comments: imports never live inside
        // strings, and a banned name in a doc string ("never use lib.ft4.admin")
        // was flagged as a forbidden import - contradicting rell_security_check,
        // which already masks strings (audit 2026-09-01).
        val live = maskRellSource(rell, maskStrings = true)
        val hits = mutableListOf<Hit>()
        val warnings = mutableListOf<String>()
        live.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed
            forbidden.forEach { module ->
                if (containsModule(line, module)) {
                    hits += Hit(module, index + 1, line.take(160))
                }
            }
            if (line.contains("lib.ft4.cross-chain")) {
                warnings += "line ${index + 1}: list label 'cross-chain' is not an import; official import is $CROSSCHAIN_IMPORT"
            }
        }
        val unique = hits.distinctBy { it.module to it.line }
        val findings = unique.map { hit ->
            "line ${hit.line}: forbidden FT4 production module ${hit.module}"
        }
        return if (allowAdminModules) {
            Result(
                ok = true,
                errors = emptyList(),
                warnings = findings.map { "$it (allowed by allowAdminModules)" } + warnings,
                hits = unique
            )
        } else {
            Result(
                ok = findings.isEmpty(),
                errors = findings,
                warnings = warnings,
                hits = unique
            )
        }
    }

    fun scanFiles(rellFiles: Map<String, String>, allowAdminModules: Boolean = false): Result {
        if (rellFiles.isEmpty()) {
            return Result(
                ok = false,
                errors = listOf("missing .rell file contents"),
                warnings = emptyList(),
                hits = emptyList()
            )
        }
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val hits = mutableListOf<Hit>()
        var exempted = 0
        rellFiles.forEach { (path, content) ->
            // FT4's own library files legitimately contain `operation ras_open(`
            // and `import lib.ft4.admin;` - scanning a submitted lib/ft4 tree
            // reported forbidden-module errors pointing INTO the library (audit
            // F2 follow-up). Skip vendored-library files; app files stay scanned.
            // Content-gated: only a file bit-identical (modulo line endings) to
            // the vendored FT4 copy is trusted - a differing lib/ft4 file could
            // be planted code and is scanned like app code, with a warning why.
            if (RellLibs.isSubmittedFt4Path(path)) {
                if (RellLibs.matchesVendoredFt4(path, content)) {
                    exempted++
                    return@forEach
                }
                warnings += RellLibs.modifiedFt4Note(path)
            }
            val label = path.trim().ifEmpty { "rell" }
            val one = scan(content, allowAdminModules)
            one.errors.forEach { err -> errors += "$label: $err" }
            one.warnings.forEach { warn -> warnings += "$label: $warn" }
            hits += one.hits
        }
        return Result(
            ok = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            hits = hits,
            exemptedLibFiles = exempted
        )
    }

    internal fun stripComments(source: String): String {
        val out = StringBuilder()
        var i = 0
        var inBlock = false
        var inLine = false
        var inString = false
        var stringDelim = '\u0000'
        while (i < source.length) {
            val c = source[i]
            val next = source.getOrNull(i + 1)
            if (inLine) {
                if (c == '\n') {
                    inLine = false
                    out.append(c)
                }
                i++
                continue
            }
            if (inBlock) {
                if (c == '*' && next == '/') {
                    inBlock = false
                    i += 2
                } else {
                    if (c == '\n') out.append(c)
                    i++
                }
                continue
            }
            if (inString) {
                out.append(c)
                if (c == '\\' && next != null) {
                    out.append(next)
                    i += 2
                    continue
                }
                if (c == stringDelim) inString = false
                i++
                continue
            }
            if (c == '"' || c == '\'') {
                inString = true
                stringDelim = c
                out.append(c)
                i++
                continue
            }
            if (c == '/' && next == '/') {
                inLine = true
                i += 2
                continue
            }
            if (c == '/' && next == '*') {
                inBlock = true
                i += 2
                continue
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    internal fun containsModule(line: String, module: String): Boolean {
        // A forbidden module matches as a dotted-path prefix: a following '.' is allowed
        // (lib.ft4.admin matches lib.ft4.admin.crosschain) but a preceding '.' or
        // identifier char is not (admin.crosschain must not match lib.ft4.crosschain).
        var from = 0
        while (true) {
            val idx = line.indexOf(module, from, ignoreCase = true)
            if (idx < 0) return false
            val before = line.getOrNull(idx - 1)
            val after = line.getOrNull(idx + module.length)
            val ident = { ch: Char? -> ch != null && (ch.isLetterOrDigit() || ch == '_') }
            val beforeOk = before == null || (!ident(before) && before != '.')
            val afterOk = !ident(after)
            if (beforeOk && afterOk) return true
            from = idx + 1
        }
    }
}
