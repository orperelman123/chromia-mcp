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
        "require_signer",
        // ICCF cross-chain proof validation IS the auth mechanism for
        // proof-carrying operations: lib.iccf's require_valid_proof aborts the
        // op on an invalid proof. Production filechain/iccf-example ops using
        // this documented pattern were false-flagged HIGH (real-world round 1).
        "require_valid_proof",
        // Reading the actual transaction signers to derive the mutation
        // (create x(signer) for op_context.get_signers()) ties the write to
        // whoever REALLY signed - same trust level as is_signer( above.
        "op_context.get_signers("
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

    /**
     * (name, masked body) for EVERY function definition in the (masked) source.
     * A name-keyed map here let same-named functions in different namespaces of
     * ONE file clobber each other (last definition won), so a benign body could
     * hide a mutating or non-auth sibling from the per-name conservative merge
     * that only saw cross-file duplicates (audit 2026-09-01). Callers must
     * merge per name over all definitions, never assume the name is unique.
     */
    internal fun functionBodies(maskedContent: String): List<Pair<String, String>> {
        val functions = mutableListOf<Pair<String, String>>()
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
            functions.add(name to body)
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

    /** `import name;` / `import alias: name;` - captures the dotted module path. */
    private val IMPORT_REGEX = Regex("""\bimport\s+(?:[A-Za-z_][A-Za-z0-9_]*\s*:\s*)?([A-Za-z_][A-Za-z0-9_.]*)""")

    /** True when any parent directory of the (source-root-normalized) path is test/ or tests/. */
    private fun isTestDirPath(normalizedPath: String): Boolean =
        normalizedPath.split('/').dropLast(1).any { it == "test" || it == "tests" }

    /**
     * Original-path keys of files whose findings sit on the TEST surface: the
     * file is a `@test module`, lives under a test/ or tests/ directory, or
     * belongs to a module reachable from such test modules via imports but NOT
     * from any app root (a non-test module nothing imports). Code only tests can
     * reach never ships in the dApp, so a HIGH there is advisory, not blocking -
     * a test fixture's helper mutation used to fail the gate exactly like
     * production code (probe finding 2026-09-01). Reuses the module-name and
     * @test detection the compile tools already use.
     */
    internal fun testSurfaceFiles(files: Map<String, String>): Set<String> {
        data class FileInfo(val path: String, val module: String, val isTestFile: Boolean)
        val infos = files.map { (path, content) ->
            val normalized = RellCheck.normalizeSourceRoot(path)
            FileInfo(
                path,
                RunRellTests.moduleNameForPath(normalized, content),
                RunRellTests.isTestModuleSource(content) || isTestDirPath(normalized)
            )
        }
        val allModules = infos.mapTo(mutableSetOf()) { it.module }
        val testModules = infos.filter { it.isTestFile }.mapTo(mutableSetOf()) { it.module }
        // module -> submitted modules it imports
        val imports = mutableMapOf<String, MutableSet<String>>()
        infos.forEach { info ->
            val masked = maskRellSource(files.getValue(info.path), maskStrings = true)
            IMPORT_REGEX.findAll(masked).forEach { m ->
                val target = m.groupValues[1].trimEnd('.')
                if (target in allModules && target != info.module) {
                    imports.getOrPut(info.module) { mutableSetOf() }.add(target)
                }
            }
        }
        val importedBy = mutableMapOf<String, MutableSet<String>>()
        imports.forEach { (from, tos) -> tos.forEach { importedBy.getOrPut(it) { mutableSetOf() }.add(from) } }
        fun reachableFrom(seeds: Collection<String>): Set<String> {
            val seen = mutableSetOf<String>()
            val queue = ArrayDeque(seeds)
            while (queue.isNotEmpty()) {
                val module = queue.removeFirst()
                if (!seen.add(module)) continue
                imports[module]?.forEach { queue.addLast(it) }
            }
            return seen
        }
        // App roots: non-test modules nothing imports. A module reachable from an
        // app root stays app surface even when tests also import it; a module
        // reachable ONLY from test modules is test surface. A module reachable
        // from neither (e.g. an app-module import cycle no test touches) is not
        // in testReachable and conservatively stays app surface.
        val appReachable = reachableFrom(allModules.filter { it !in testModules && importedBy[it].isNullOrEmpty() })
        val testOnlyModules = reachableFrom(testModules) - appReachable
        return infos.filter { it.isTestFile || it.module in testOnlyModules }.mapTo(mutableSetOf()) { it.path }
    }

    /**
     * @param allowAdminModules downgrade the banned-module/open-strategy rules
     * from CRITICAL to MEDIUM, each tagged "(allowed by allowAdminModules)" -
     * the escape hatch for deliberately building admin/ops tooling. Everything
     * else (rules, texts, the default) is unchanged.
     */
    fun analyze(files: Map<String, String>, allowAdminModules: Boolean = false): Result {
        val findings = mutableListOf<Finding>()
        var operationsScanned = 0

        // Fully masked: brace/paren matching and the mutation/auth regexes must
        // never see braces, "update", or auth markers inside strings or comments.
        val fullyMasked = files.mapValues { (_, content) -> maskRellSource(content, maskStrings = true) }
        // Auth and mutation call graphs span the whole submission: an auth helper
        // or a mutating helper defined in a sibling file must be recognized.
        val authFunctions = authFunctionNames(fullyMasked)
        val mutatingFunctions = mutatingFunctionNames(fullyMasked)

        var exemptedLibFiles = 0
        val modifiedLibNotes = mutableListOf<String>()
        files.forEach { (path, content) ->
            // A submitted lib/ft4 tree is FT4's own code: v1.1.0r itself contains
            // `operation ras_open(` and `import lib.ft4.admin;`, so per-file rules
            // reported CRITICALs pointing INTO the library (audit F2 follow-up).
            // Library files still feed the auth/mutation call graphs above; the
            // user's app files (where a forbidden import matters) stay scanned.
            // The exemption is content-gated: only a file bit-identical (modulo
            // line endings) to the vendored FT4 copy is trusted - a differing
            // lib/ft4 file could be planted code and is scanned like app code.
            if (RellLibs.isSubmittedFt4Path(path)) {
                if (RellLibs.matchesVendoredFt4(path, content)) {
                    exemptedLibFiles++
                    return@forEach
                }
                modifiedLibNotes += RellLibs.modifiedFt4Note(path)
            }
            // Comment-masked: string contents kept (hex key material lives in x"..."
            // literals) but comments cannot hide or fake findings.
            val commentMasked = maskRellSource(content, maskStrings = false)
            val masked = fullyMasked.getValue(path)
            // Banned-module/strategy rules run on fully masked text: a banned name
            // inside a string literal (e.g. a require() message) is not an import.
            findings += bannedModuleFindings(path, masked, allowAdminModules)
            findings += hardcodedSecretFindings(path, commentMasked)
            val authMarkers = authMarkersFor(masked)
            val ops = scanOperations(path, masked)
            operationsScanned += ops.size
            ops.forEach { op ->
                findings += operationFindings(path, op, authFunctions, mutatingFunctions, authMarkers)
            }
        }

        // HIGH findings on the test surface downgrade to MEDIUM with a rule
        // suffix; CRITICALs (banned modules, open strategies) never downgrade
        // this way - shipping-forbidden code is forbidden wherever it sits.
        val testSurface = testSurfaceFiles(files)
        val adjusted = findings.map { finding ->
            if (finding.severity == "HIGH" && finding.file in testSurface) {
                finding.copy(severity = "MEDIUM", rule = finding.rule + "-test-surface")
            } else {
                finding
            }
        }
        val downgraded = adjusted.count { it.rule.endsWith("-test-surface") }

        val blocking = adjusted.any { it.severity == "CRITICAL" || it.severity == "HIGH" }
        val notes = buildString {
            append("Scanned ${files.size - exemptedLibFiles} file(s), $operationsScanned operation(s). ")
            if (exemptedLibFiles > 0) {
                append(RellLibs.exemptedFt4Note(exemptedLibFiles) + " ")
            }
            modifiedLibNotes.forEach { append("$it ") }
            append(
                if (adjusted.isEmpty()) "No findings from the static rules. "
                else "${adjusted.size} finding(s); fix CRITICAL/HIGH before deploying. "
            )
            if (downgraded > 0) {
                append(
                    "$downgraded HIGH finding(s) in test-only code reported as MEDIUM " +
                        "with a -test-surface rule suffix. "
                )
            }
            if (allowAdminModules) {
                append("allowAdminModules=true: banned-module findings reported as MEDIUM, not CRITICAL. ")
            }
            append(
                "Heuristic static checks only (auth, require() validation, banned FT4 admin modules, " +
                    "hardcoded secrets) - a clean report does not replace a security audit."
            )
        }
        return Result(!blocking, adjusted.sortedWith(compareBy({ severityRank(it.severity) }, { it.file }, { it.line })), operationsScanned, notes)
    }

    private fun severityRank(s: String) = when (s) {
        "CRITICAL" -> 0
        "HIGH" -> 1
        else -> 2
    }

    private fun bannedModuleFindings(path: String, content: String, allowAdminModules: Boolean = false): List<Finding> {
        val severity = if (allowAdminModules) "MEDIUM" else "CRITICAL"
        val allowedTag = if (allowAdminModules) " (allowed by allowAdminModules)" else ""
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
                            severity, "banned-module", path, idx + 1, trimmed + allowedTag,
                            "Remove $banned - admin modules must never ship in a production dApp."
                        )
                    )
                }
            }
            BANNED_STRATEGY_RULES.forEach { rule ->
                if (Regex("""\b$rule\b""").containsMatchIn(trimmed)) {
                    findings.add(
                        Finding(
                            severity, "open-registration-strategy", path, idx + 1, trimmed + allowedTag,
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
