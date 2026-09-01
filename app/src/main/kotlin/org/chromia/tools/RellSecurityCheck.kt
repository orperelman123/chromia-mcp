package org.chromia.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Static security pass for Rell sources, run after a successful [RellCheck]
 * compile. Heuristic, line-anchored findings for the security rules the agent
 * briefs mandate: authenticated operations, validated inputs, no banned FT4
 * admin modules, no hardcoded secrets. A clean report is necessary but not
 * sufficient - it does not replace a human audit, and the notes say so.
 */
/**
 * Masks Rell comments (// and block comments) and optionally string literals
 * ("..." / '...', with escapes) with spaces, preserving newlines so line numbers
 * survive. Textual scanners must run on masked source: raw text lets a single
 * "}" or "update" inside a string or comment defeat brace matching and every
 * regex rule (verified adversarially).
 */
internal fun maskRellSource(source: String, maskStrings: Boolean): String {
    val out = StringBuilder(source.length)
    var i = 0
    var state = 0 // 0 code, 1 line comment, 2 block comment, 3 double-quote, 4 single-quote
    while (i < source.length) {
        val c = source[i]
        val next = if (i + 1 < source.length) source[i + 1] else '\u0000'
        when (state) {
            0 -> when {
                c == '/' && next == '/' -> { state = 1; out.append("  "); i += 2; continue }
                c == '/' && next == '*' -> { state = 2; out.append("  "); i += 2; continue }
                c == '"' -> { state = 3; out.append(if (maskStrings) ' ' else c) }
                c == '\'' -> { state = 4; out.append(if (maskStrings) ' ' else c) }
                else -> out.append(c)
            }
            1 -> if (c == '\n') { state = 0; out.append('\n') } else out.append(' ')
            2 -> when {
                c == '*' && next == '/' -> { state = 0; out.append("  "); i += 2; continue }
                c == '\n' -> out.append('\n')
                else -> out.append(' ')
            }
            3, 4 -> {
                val quote = if (state == 3) '"' else '\''
                when {
                    c == '\\' && i + 1 < source.length -> {
                        if (maskStrings) out.append("  ") else out.append(c).append(next)
                        i += 2; continue
                    }
                    c == quote -> { state = 0; out.append(if (maskStrings) ' ' else c) }
                    c == '\n' -> out.append('\n')
                    else -> out.append(if (maskStrings) ' ' else c)
                }
            }
        }
        i++
    }
    return out.toString()
}

object RellSecurityCheck {

    data class Finding(
        val severity: String, // CRITICAL | HIGH | MEDIUM
        val rule: String,
        val file: String,
        val line: Int,
        val text: String,
        val fix: String
    )

    data class Result(
        val ok: Boolean,
        val findings: List<Finding>,
        val operationsScanned: Int,
        val notes: String
    )

    /**
     * Modules that must never ship in production dApps
     * (see AGENTS.md / CLAUDE.md FT4 pins).
     */
    private val BANNED_IMPORTS = listOf(
        "lib.ft4.admin",
        "admin.crosschain"
    )
    private val BANNED_STRATEGY_RULES = listOf("ras_open", "ras_transfer_open")

    private val AUTH_MARKERS = listOf(
        "auth.authenticate",
        "ft4.auth",
        "op_context.is_signer",
        "is_signer(",
        "require_signer"
    )
    // "auth_handler" as a bare substring matched identifiers like auth_handlers_cfg;
    // require the call/definition paren (add_auth_handler(...) still matches).
    private val AUTH_HANDLER_REGEX = Regex("""auth_handler\s*\(""")
    // `import a: lib.ft4.auth;` - `a.authenticate()` must count as an auth marker.
    private val FT4_AUTH_ALIAS_REGEX = Regex("""\bimport\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*:\s*lib\.ft4\.auth\s*;""")

    // Next char must be whitespace or '(' - the Rell grammar allows parenthesized
    // targets with no space (`delete(u);`, `update(u)(...)`), which `\s` missed;
    // `created` / `update_helper(` still cannot match thanks to the \b.
    private val MUTATION_REGEX = Regex("""\b(create|update|delete)\b[\s(]""")
    // Not line-anchored: `@mount('x') operation f(...)`, `namespace a { operation g() {...} }`
    // and `} operation h(` were invisible to a ^-anchored scan (audit 2026-09-01).
    // Line numbers are computed from the keyword's own match offset.
    private val OPERATION_REGEX = Regex("""\boperation\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(""")
    private val FUNCTION_REGEX = Regex("""\bfunction\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(""")
    private val REQUIRE_REGEX = Regex("""\brequire(_not_empty)?\s*\(""")
    private val HEX_SECRET_REGEX = Regex("""x?["']([0-9a-fA-F]{64,})["']""")

    // One generic pass extracts every `name(` call site per body; closure and
    // per-operation checks then use set lookups. Compiling a regex per
    // (function, candidate) pair per fixed-point pass was O(N^3) regex compiles
    // and effectively hung the tool on a long call chain (audit F3). Matching a
    // qualified call `ns.fn(` still yields `fn`, like the old \bfn\s*\( regex.
    private val CALL_SITE_REGEX = Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*\(""")

    /** Names of the functions a (masked) body calls, e.g. {"fn"} for `ns.fn (x)`. */
    internal fun calledNames(maskedBody: String): Set<String> =
        CALL_SITE_REGEX.findAll(maskedBody).mapTo(mutableSetOf()) { it.groupValues[1] }

    /** Auth markers for one (masked) file: the globals plus its FT4 auth aliases. */
    internal fun authMarkersFor(masked: String): List<String> =
        AUTH_MARKERS + FT4_AUTH_ALIAS_REGEX.findAll(masked).map { "${it.groupValues[1]}.authenticate" }

    private fun containsAuthMarker(text: String, markers: List<String>): Boolean =
        markers.any { text.contains(it) } || AUTH_HANDLER_REGEX.containsMatchIn(text)

    /** name -> masked body for every function definition in the (masked) source. */
    internal fun functionBodies(maskedContent: String): Map<String, String> {
        val functions = mutableMapOf<String, String>()
        FUNCTION_REGEX.findAll(maskedContent).forEach { match ->
            val name = match.groupValues[1]
            val parenStart = maskedContent.indexOf('(', match.range.first)
            val parenEnd = matchDelimiter(maskedContent, parenStart, '(', ')') ?: return@forEach
            val braceStart = maskedContent.indexOf('{', parenEnd)
            val eqIdx = maskedContent.indexOf('=', parenEnd)
            val body = when {
                braceStart >= 0 && (eqIdx < 0 || braceStart < eqIdx) -> {
                    val braceEnd = matchDelimiter(maskedContent, braceStart, '{', '}') ?: return@forEach
                    maskedContent.substring(braceStart + 1, braceEnd)
                }
                eqIdx >= 0 -> {
                    val end = maskedContent.indexOf(';', eqIdx).let { if (it < 0) maskedContent.length else it }
                    maskedContent.substring(eqIdx + 1, end)
                }
                else -> return@forEach
            }
            functions[name] = body
        }
        return functions
    }

    /** Fixed point: a function is in the set if seeded or if it calls one that is. */
    private fun closeOverCalls(functions: Map<String, String>, seed: Set<String>): Set<String> {
        val callSites = functions.mapValues { (_, body) -> calledNames(body) }
        val result = seed.toMutableSet()
        var changed = true
        while (changed) {
            changed = false
            callSites.forEach { (name, calls) ->
                if (name !in result && calls.any { it in result }) {
                    result.add(name)
                    changed = true
                }
            }
        }
        return result
    }

    /**
     * Names of user functions whose (masked) body contains an auth marker,
     * expanded to a fixed point: a function that calls an auth-establishing
     * function is itself auth-establishing. Computed over ALL files of the
     * submission - an auth helper defined in a sibling file must be recognized
     * (audit 2026-09-01). Closes the indirect-auth false-positive class
     * (require_user()-style helpers).
     */
    internal fun authFunctionNames(maskedFiles: Map<String, String>): Set<String> {
        // Same-named functions across files used to clobber a single name-keyed
        // map, and any auth definition made the NAME auth - so a non-auth helper
        // sharing a name with an auth helper suppressed findings (audit round 4
        // minor). Conservative for security: a name counts as auth-establishing
        // only if EVERY definition of it establishes auth.
        data class Def(val name: String, val calls: Set<String>, val hasMarker: Boolean)
        val defs = mutableListOf<Def>()
        maskedFiles.forEach { (_, masked) ->
            val markers = authMarkersFor(masked)
            functionBodies(masked).forEach { (name, body) ->
                defs.add(Def(name, calledNames(body), containsAuthMarker(body, markers)))
            }
        }
        val byName = defs.groupBy { it.name }
        val authDefs = defs.filterTo(mutableSetOf()) { it.hasMarker }
        val authNames = mutableSetOf<String>()
        fun refresh(name: String) {
            if (byName.getValue(name).all { it in authDefs }) authNames.add(name)
        }
        byName.keys.forEach { refresh(it) }
        var changed = true
        while (changed) {
            changed = false
            defs.forEach { def ->
                if (def !in authDefs && def.calls.any { it in authNames }) {
                    authDefs.add(def)
                    refresh(def.name)
                    changed = true
                }
            }
        }
        return authNames
    }

    /**
     * Names of user functions that mutate state, directly or transitively - an
     * operation that mutates only via a helper (`operation transfer() { do_transfer(); }`)
     * must still get an unauthenticated-mutation finding (audit 2026-09-01).
     */
    internal fun mutatingFunctionNames(maskedFiles: Map<String, String>): Set<String> {
        // Same-named functions across files used to clobber the map (last file
        // won), hiding a mutating definition behind a later benign one (audit
        // round 4 minor). Conservative for security: a name is mutating if ANY
        // definition mutates - concatenating all bodies per name gives exactly
        // that ("any body matches" / "any body calls").
        val bodies = mutableMapOf<String, StringBuilder>()
        maskedFiles.forEach { (_, masked) ->
            functionBodies(masked).forEach { (name, body) ->
                bodies.getOrPut(name) { StringBuilder() }.append(body).append('\n')
            }
        }
        val functions = bodies.mapValues { (_, sb) -> sb.toString() }
        val seed = functions.filterValues { MUTATION_REGEX.containsMatchIn(it) }.keys.toSet()
        return closeOverCalls(functions, seed)
    }

    fun analyze(files: Map<String, String>): Result {
        val findings = mutableListOf<Finding>()
        var operationsScanned = 0

        // Fully masked: brace/paren matching and the mutation/auth regexes must
        // never see braces, "update", or auth markers inside strings or comments.
        val fullyMasked = files.mapValues { (_, content) -> maskRellSource(content, maskStrings = true) }
        // Auth and mutation call graphs span the whole submission: an auth helper
        // or a mutating helper defined in a sibling file must be recognized.
        val authFunctions = authFunctionNames(fullyMasked)
        val mutatingFunctions = mutatingFunctionNames(fullyMasked)

        files.forEach { (path, content) ->
            // Comment-masked: string contents kept (hex key material lives in x"..."
            // literals) but comments cannot hide or fake findings.
            val commentMasked = maskRellSource(content, maskStrings = false)
            val masked = fullyMasked.getValue(path)
            // Banned-module/strategy rules run on fully masked text: a banned name
            // inside a string literal (e.g. a require() message) is not an import.
            findings += bannedModuleFindings(path, masked)
            findings += hardcodedSecretFindings(path, commentMasked)
            val authMarkers = authMarkersFor(masked)
            val ops = scanOperations(path, masked)
            operationsScanned += ops.size
            ops.forEach { op ->
                findings += operationFindings(path, op, authFunctions, mutatingFunctions, authMarkers)
            }
        }

        val blocking = findings.any { it.severity == "CRITICAL" || it.severity == "HIGH" }
        val notes = buildString {
            append("Scanned ${files.size} file(s), $operationsScanned operation(s). ")
            append(
                if (findings.isEmpty()) "No findings from the static rules. "
                else "${findings.size} finding(s); fix CRITICAL/HIGH before deploying. "
            )
            append(
                "Heuristic static checks only (auth, require() validation, banned FT4 admin modules, " +
                    "hardcoded secrets) - a clean report does not replace a security audit."
            )
        }
        return Result(!blocking, findings.sortedWith(compareBy({ severityRank(it.severity) }, { it.file }, { it.line })), operationsScanned, notes)
    }

    private fun severityRank(s: String) = when (s) {
        "CRITICAL" -> 0
        "HIGH" -> 1
        else -> 2
    }

    private fun bannedModuleFindings(path: String, content: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        content.lineSequence().forEachIndexed { idx, line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("//")) return@forEachIndexed
            BANNED_IMPORTS.forEach { banned ->
                // Dotted-prefix match with boundaries: lib.ft4.admin.crosschain is
                // banned, lib.ft4.admin_utils is not (audit 2026-09-01).
                if (Ft4ImportCheck.containsModule(trimmed, banned)) {
                    findings.add(
                        Finding(
                            "CRITICAL", "banned-module", path, idx + 1, trimmed,
                            "Remove $banned - admin modules must never ship in a production dApp."
                        )
                    )
                }
            }
            BANNED_STRATEGY_RULES.forEach { rule ->
                if (Regex("""\b$rule\b""").containsMatchIn(trimmed)) {
                    findings.add(
                        Finding(
                            "CRITICAL", "open-registration-strategy", path, idx + 1, trimmed,
                            "Remove $rule - open registration/transfer strategies allow anyone to register or move assets."
                        )
                    )
                }
            }
        }
        return findings
    }

    private fun hardcodedSecretFindings(path: String, content: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        content.lineSequence().forEachIndexed { idx, line ->
            if (line.trim().startsWith("//")) return@forEachIndexed
            HEX_SECRET_REGEX.findAll(line).forEach { _ ->
                findings.add(
                    Finding(
                        "HIGH", "hardcoded-key-material", path, idx + 1, line.trim().take(120),
                        "A 64+ char hex literal looks like key material or a BRID that should come from configuration/module args, not source."
                    )
                )
            }
        }
        return findings
    }

    internal data class OperationBlock(val name: String, val line: Int, val params: String, val body: String)

    internal fun scanOperations(path: String, content: String): List<OperationBlock> {
        val blocks = mutableListOf<OperationBlock>()
        OPERATION_REGEX.findAll(content).forEach { match ->
            val name = match.groupValues[1]
            val startLine = content.substring(0, match.range.first).count { it == '\n' } + 1
            val parenStart = content.indexOf('(', match.range.first)
            val parenEnd = matchDelimiter(content, parenStart, '(', ')') ?: return@forEach
            val params = content.substring(parenStart + 1, parenEnd)
            val braceStart = content.indexOf('{', parenEnd)
            if (braceStart < 0) return@forEach
            // Unterminated body (e.g. `operation x() {` at EOF): fall back to end-of-input,
            // clamped so the substring can never invert (fuzzer-found crash).
            val braceEnd = (matchDelimiter(content, braceStart, '{', '}') ?: content.length)
                .coerceIn(braceStart + 1, content.length)
            val body = content.substring(braceStart + 1, braceEnd)
            blocks.add(OperationBlock(name, startLine, params, body))
        }
        return blocks
    }

    private fun matchDelimiter(content: String, start: Int, open: Char, close: Char): Int? {
        if (start < 0 || start >= content.length || content[start] != open) return null
        var depth = 0
        for (i in start until content.length) {
            when (content[i]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                    if (depth < 0) return null
                }
            }
        }
        return null
    }

    private fun operationFindings(
        path: String,
        op: OperationBlock,
        authFunctions: Set<String> = emptySet(),
        mutatingFunctions: Set<String> = emptySet(),
        authMarkers: List<String> = AUTH_MARKERS
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        val calls = calledNames(op.body)
        val hasAuth = containsAuthMarker(op.body, authMarkers) ||
            authFunctions.any { it in calls }
        val hasRequire = REQUIRE_REGEX.containsMatchIn(op.body)
        val mutates = MUTATION_REGEX.containsMatchIn(op.body) ||
            mutatingFunctions.any { it in calls }

        if (mutates && !hasAuth) {
            findings.add(
                Finding(
                    "HIGH", "unauthenticated-mutation", path, op.line,
                    "operation ${op.name} mutates state without an auth check",
                    "Authenticate the caller (ft4 auth.authenticate() or an explicit op_context.is_signer / require(...) signer check) before create/update/delete."
                )
            )
        }
        if (op.params.isNotBlank() && !hasRequire && !hasAuth) {
            findings.add(
                Finding(
                    "MEDIUM", "unvalidated-inputs", path, op.line,
                    "operation ${op.name} takes parameters but has no require(...) validation",
                    "Validate inputs with require(...) - length/range/format checks - before using them."
                )
            )
        }
        return findings
    }

    fun Result.toJson(): JsonObject = buildJsonObject {
        put("ok", ok)
        put("operationsScanned", operationsScanned)
        put(
            "findings",
            buildJsonArray {
                findings.forEach { f ->
                    add(
                        buildJsonObject {
                            put("severity", f.severity)
                            put("rule", f.rule)
                            put("file", f.file)
                            put("line", f.line)
                            put("text", f.text)
                            put("fix", f.fix)
                        }
                    )
                }
            }
        )
        put("notes", notes)
    }
}
