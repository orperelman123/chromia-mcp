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

    /**
     * How a dApp actually turns on permissionless registration: it imports the
     * strategy module. The [BANNED_STRATEGY_RULES] token scan cannot see this -
     * `operation ras_open(` is declared inside FT4 itself, and FT4's own tree is
     * exempt from this scan, so the token never matches real app code and the
     * rule was dead against the only path anyone uses (audit 2026-09-02).
     * Matched with the same dotted-prefix boundary rule as [BANNED_IMPORTS], so
     * `...strategies.open_gated` would not trip it.
     */
    private val BANNED_STRATEGY_IMPORTS = listOf(
        "lib.ft4.accounts.strategies.open",
        "lib.ft4.accounts.strategies.transfer.open"
    )

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
    internal fun mutatingFunctionNames(maskedFiles: Map<String, String>): Set<String> =
        functionNamesMatchingSeed(maskedFiles, MUTATION_REGEX)

    /**
     * Names of user functions whose body matches [seed], closed over calls
     * (a function calling a matching function matches). Same-named functions
     * across files used to clobber a name-keyed map (last file won), hiding a
     * matching definition behind a later benign one (audit round 4 minor).
     * Conservative for security: a name matches if ANY definition matches -
     * concatenating all bodies per name gives exactly that ("any body
     * matches" / "any body calls").
     */
    internal fun functionNamesMatchingSeed(maskedFiles: Map<String, String>, seed: Regex): Set<String> {
        val bodies = mutableMapOf<String, StringBuilder>()
        maskedFiles.forEach { (_, masked) ->
            functionBodies(masked).forEach { (name, body) ->
                bodies.getOrPut(name) { StringBuilder() }.append(body).append('\n')
            }
        }
        val functions = bodies.mapValues { (_, sb) -> sb.toString() }
        return closeOverCalls(functions, functions.filterValues { seed.containsMatchIn(it) }.keys.toSet())
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
        val valueMutatingFunctions = functionNamesMatchingSeed(fullyMasked, VALUE_MUTATION_REGEX)
        val ft4AuthCallers = functionNamesMatchingSeed(fullyMasked, AUTHENTICATE_CALL_REGEX)
        // Validation done in a helper is still validation: the auth and mutation
        // closures already walk helpers, so an op-body-only require() scan
        // false-flagged every validate_x() helper pattern (gate fatigue).
        val requireFunctions = functionNamesMatchingSeed(fullyMasked, REQUIRE_REGEX)
        val emptyFlagsOnly = allAuthHandlersHaveEmptyFlags(files)

        var exemptedLibFiles = 0
        var thirdPartyLibFiles = 0
        var exemptedTestModules = 0
        val modifiedLibNotes = mutableListOf<String>()
        files.forEach { (path, content) ->
            // A submitted lib/ft4 (or lib/iccf) tree is the library's own code:
            // FT4 v1.1.0r itself contains `operation ras_open(` and
            // `import lib.ft4.admin;`, so per-file rules reported CRITICALs
            // pointing INTO the library (audit F2 follow-up). Library files
            // still feed the auth/mutation call graphs above; the user's app
            // files (where a forbidden import matters) stay scanned.
            // The exemption is content-gated: only a file bit-identical (modulo
            // line endings) to the vendored copy is trusted - a differing
            // lib file could be planted code and is scanned like app code.
            if (RellLibs.isVendoredLibraryPath(path)) {
                if (RellLibs.matchesVendoredFt4(path, content)) {
                    exemptedLibFiles++
                    return@forEach
                }
                modifiedLibNotes += RellLibs.modifiedFt4Note(path)
            } else if (RellLibs.isThirdPartyLibPath(path)) {
                // lib/** with no vendored copy to hash-compare (lib/ft3,
                // lib/icmf, ...): third-party library code the user does not
                // own - findings inside it are noise (real-world round 2 D5).
                // It still feeds the call graphs above.
                thirdPartyLibFiles++
                return@forEach
            }
            // Comment-masked: string contents kept (hex key material lives in x"..."
            // literals) but comments cannot hide or fake findings.
            val commentMasked = maskRellSource(content, maskStrings = false)
            val masked = fullyMasked.getValue(path)
            // Banned-module/strategy rules run on fully masked text: a banned name
            // inside a string literal (e.g. a require() message) is not an import.
            // @test modules are exempt: exercising admin modules and registration
            // strategies is exactly what test code does (crc2-lib false reds,
            // real-world round 2 D4) - matching the test-surface downgrade
            // precedent, but these rules are exempt+noted rather than downgraded.
            if (RunRellTests.isTestModuleSource(content)) {
                exemptedTestModules++
            } else {
                findings += bannedModuleFindings(path, masked, allowAdminModules)
            }
            findings += hardcodedSecretFindings(path, commentMasked)
            findings += massMutationFindings(path, masked)
            val authMarkers = authMarkersFor(masked)
            val ops = scanOperations(path, masked)
            operationsScanned += ops.size
            ops.forEach { op ->
                findings += operationFindings(
                    path, op, authFunctions, mutatingFunctions, authMarkers,
                    valueMutatingFunctions, ft4AuthCallers, emptyFlagsOnly, requireFunctions
                )
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
            append("Scanned ${files.size - exemptedLibFiles - thirdPartyLibFiles} file(s), $operationsScanned operation(s). ")
            if (exemptedLibFiles > 0) {
                append(RellLibs.exemptedFt4Note(exemptedLibFiles) + " ")
            }
            if (thirdPartyLibFiles > 0) {
                append(RellLibs.thirdPartyLibNote(thirdPartyLibFiles) + " ")
            }
            if (exemptedTestModules > 0) {
                append(
                    "$exemptedTestModules @test module file(s) exempt from the banned-module/open-strategy " +
                        "scan - test code legitimately exercises admin modules and registration strategies. "
                )
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
                "Heuristic static checks only (authentication AND authorization binding, signer-gate " +
                    "integrity, auth-handler flags, mass mutations, require() validation, banned FT4 " +
                    "admin modules, hardcoded secrets) - a clean report does not replace a security " +
                    "audit, and economic invariants (conservation, quorum) are not checked."
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
            BANNED_STRATEGY_IMPORTS.forEach { banned ->
                if (Ft4ImportCheck.containsModule(trimmed, banned)) {
                    findings.add(
                        Finding(
                            severity, "open-registration-strategy", path, idx + 1, trimmed + allowedTag,
                            "Remove the $banned import - it lets anyone register an account or move " +
                                "assets without approval. Use a gated registration strategy instead."
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

    /** `name = x"..."` / `name: type = "..."` - the identifier the hex literal is assigned to. */
    private val HEX_ASSIGN_NAME_REGEX = Regex("""([A-Za-z_]\w*)\s*(?::\s*[A-Za-z_][\w<>.?\[\]]*)?\s*=\s*$""")
    // Secret-looking name parts always win: a "priv_key_id" is a key, not an id.
    private val SECRET_NAME_PARTS = setOf("priv", "private", "secret", "seed", "mnemonic", "sk", "key")
    // Public 32-byte identifiers (BRIDs, tx/block hashes, asset ids): hardcoding
    // one is a config smell, not leaked key material. Reporting it HIGH trained
    // agents to wave the whole rule through (gate-fatigue, audit 2026-09-02).
    private val CHAIN_ID_NAME_PARTS = setOf("brid", "rid", "hash", "tx", "txid", "block", "blockchain", "chain", "asset", "id")

    private fun hardcodedSecretFindings(path: String, content: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        content.lineSequence().forEachIndexed { idx, line ->
            if (line.trim().startsWith("//")) return@forEachIndexed
            HEX_SECRET_REGEX.findAll(line).forEach { match ->
                val name = HEX_ASSIGN_NAME_REGEX.find(line.substring(0, match.range.first))?.groupValues?.get(1)
                val parts = name?.lowercase()?.split('_')?.toSet() ?: emptySet()
                val isChainId = parts.none { it in SECRET_NAME_PARTS } && parts.any { it in CHAIN_ID_NAME_PARTS }
                findings.add(
                    if (isChainId) {
                        Finding(
                            "MEDIUM", "hardcoded-chain-identifier", path, idx + 1, line.trim().take(120),
                            "'$name' looks like a public chain/asset identifier (BRID, hash), not key material - " +
                                "still prefer configuration/module args over source constants so environments can differ."
                        )
                    } else {
                        Finding(
                            "HIGH", "hardcoded-key-material", path, idx + 1, line.trim().take(120),
                            "A 64+ char hex literal looks like key material that should come from configuration/module args, not source."
                        )
                    }
                )
            }
        }
        return findings
    }

    // ---- authorization-not-bound-to-caller (confused deputy) ----
    // "Authenticated + a require() somewhere" used to be certified secure; the
    // adversary round drained a vault through exactly that shape: authenticate,
    // then mutate rows SELECTED by a caller-supplied account parameter that is
    // never related to the authenticated identity. Anyone drains anyone.
    /** Parameter names that denote an account/identity being acted UPON. */
    private val ACCOUNT_PARAM_NAME_REGEX = Regex(
        """(?:^|_)(account|owner|from|sender|user|holder|wallet|member|spender|payer)(?:$|_)""",
        RegexOption.IGNORE_CASE
    )

    /** `val x = something(` - candidate bindings of the authenticated identity. */
    private val VAL_CALL_REGEX = Regex("""\bval\s+([A-Za-z_]\w*)\s*=\s*([A-Za-z_][\w.]*)\s*\(""")

    /** Top-level `name: type` / bare `name` pairs of an operation's parameter list. */
    internal fun parseParams(params: String): List<Pair<String, String>> {
        if (params.isBlank()) return emptyList()
        val out = mutableListOf<Pair<String, String>>()
        var depth = 0
        val cur = StringBuilder()
        fun flush() {
            val raw = cur.toString().substringBefore('=').trim()
            cur.clear()
            if (raw.isEmpty()) return
            val colon = raw.indexOf(':')
            if (colon >= 0) out.add(raw.substring(0, colon).trim() to raw.substring(colon + 1).trim())
            else out.add(raw to raw) // `operation f(pubkey)` - the name IS the type
        }
        for (c in params) {
            when (c) {
                '(', '<', '[', '{' -> { depth++; cur.append(c) }
                ')', '>', ']', '}' -> { depth--; cur.append(c) }
                ',' -> if (depth == 0) flush() else cur.append(c)
                else -> cur.append(c)
            }
        }
        flush()
        return out
    }

    /** A set-clause consisting purely of `+=` credits cannot drain the selected row. */
    private fun isCreditOnly(setPart: String): Boolean {
        val residue = setPart
            .replace("+=", "").replace("==", "").replace("!=", "").replace(">=", "").replace("<=", "")
        return !residue.contains('=')
    }

    /**
     * "deletes" / "updates" when the (masked) body contains a delete, or an
     * update whose set-clause is not credit-only, whose where-clause references
     * [paramRef]; null otherwise. Credit-only updates (`.balance += x`) of a
     * caller-named row are the receiving half of every idiomatic transfer and
     * must not read as a drain.
     */
    /**
     * `val src = wallet @ { .owner == from };` - a local bound to rows SELECTED
     * with the parameter. Mutating that local is mutating rows the parameter
     * chose, so for this rule the local is the parameter.
     */
    private val AT_SELECT_INTO_LOCAL_REGEX =
        Regex("""\bval\s+([A-Za-z_]\w*)\s*=\s*[^;]*?@[^;{]*\{([^}]*)\}""")

    /**
     * Names that stand in for [paramRef] because they were selected using it.
     * Without this the rule only saw mutations keyed DIRECTLY by the parameter,
     * and the natural phrasing - select into a local, then update the local -
     * walked straight past it. That is exactly how the real drain exploit is
     * written, so the rule looked correct against its own fixtures while missing
     * the thing it was built for (exploit corpus, 2026-09-02).
     */
    private fun aliasesSelectedBy(body: String, paramRef: Regex): List<Regex> =
        AT_SELECT_INTO_LOCAL_REGEX.findAll(body)
            .filter { paramRef.containsMatchIn(it.groupValues[2]) }
            .map { Regex("""\b${Regex.escape(it.groupValues[1])}\b""") }
            .toList()

    private fun harmfulMutationKindKeyedBy(body: String, paramRef: Regex): String? {
        val refs = listOf(paramRef) + aliasesSelectedBy(body, paramRef)
        MUTATION_REGEX.findAll(body).forEach { m ->
            val keyword = m.groupValues[1]
            if (keyword == "create") return@forEach
            val end = body.indexOf(';', m.range.first).let { if (it < 0) body.length else it }
            val stmt = body.substring(m.range.first, end)
            val braceStart = stmt.indexOf('{')
            if (braceStart < 0) {
                // `update src ( .balance -= amount );` - no where-clause at all,
                // the target IS a local. Only an alias can make this harmful.
                val target = stmt.substringAfter(keyword).substringBefore('(').trim()
                if (target.isEmpty() || refs.none { it.matches(target) }) return@forEach
                if (keyword == "delete") return "deletes"
                if (!isCreditOnly(stmt.substringAfter('('))) return "updates"
                return@forEach
            }
            val braceEnd = matchDelimiter(stmt, braceStart, '{', '}') ?: return@forEach
            if (refs.none { it.containsMatchIn(stmt.substring(braceStart + 1, braceEnd)) }) return@forEach
            if (keyword == "delete") return "deletes"
            if (!isCreditOnly(stmt.substring(braceEnd + 1))) return "updates"
        }
        return null
    }

    /**
     * HIGH when an authenticated operation debits/deletes rows selected by an
     * account-ish operation parameter that is never bound to the caller: no
     * statement relates it to an authenticated-identity variable, no
     * `is_signer(<param>)`, and the operation is not an admin op keyed to
     * `chain_context.args.*`. Runs only when the operation authenticates -
     * the unauthenticated case is already `unauthenticated-mutation`.
     */
    private fun confusedDeputyFindings(
        path: String,
        op: OperationBlock,
        authFunctions: Set<String>
    ): List<Finding> {
        val body = op.body
        // Admin ops keyed to blockchain config are the sanctioned break-glass
        // pattern (require(account.id == chain_context.args.admin, ...)).
        if (body.contains("chain_context.args")) return emptyList()
        val accountParams = parseParams(op.params)
            .filter { (name, type) ->
                ACCOUNT_PARAM_NAME_REGEX.containsMatchIn(name) || type.lowercase().contains("account")
            }
            .map { it.first }
        if (accountParams.isEmpty()) return emptyList()
        val authVars = VAL_CALL_REGEX.findAll(body)
            .filter { m ->
                val callee = m.groupValues[2].substringAfterLast('.')
                callee == "authenticate" || callee in authFunctions
            }
            .map { it.groupValues[1] }
            .toList()
        val statements = body.split(';')
        val findings = mutableListOf<Finding>()
        accountParams.forEach { param ->
            val paramRef = Regex("""\b${Regex.escape(param)}\b""")
            // is_signer(<param>) proves the caller controls that key - bound.
            if (Regex("""is_signer\s*\(\s*${Regex.escape(param)}\s*\)""").containsMatchIn(body)) return@forEach
            // Any single statement relating the param to the authenticated
            // identity counts: require(param == account.id, ...), or a mutation
            // co-keyed by both (.grantee == param, .granter == account.id).
            val bound = authVars.any { v ->
                val authRef = Regex("""\b${Regex.escape(v)}\b""")
                statements.any { paramRef.containsMatchIn(it) && authRef.containsMatchIn(it) }
            }
            if (bound) return@forEach
            val kind = harmfulMutationKindKeyedBy(body, paramRef) ?: return@forEach
            findings.add(
                Finding(
                    "HIGH", "authorization-not-bound-to-caller", path, op.line,
                    "operation ${op.name} authenticates but $kind rows selected by caller-supplied " +
                        "parameter '$param' never bound to the authenticated account",
                    "Key the mutation off the authenticated identity (.owner == account.id), or bind the " +
                        "parameter first: require($param == account.id, ...) or op_context.is_signer($param). " +
                        "Authentication says who is calling - it does not say the caller may touch the rows " +
                        "'$param' selects."
                )
            )
        }
        return findings
    }

    // ---- signer-check-on-untrusted-argument (phantom gate) ----
    /** `is_signer(x)` with a single bare identifier argument. */
    private val IS_SIGNER_ARG_REGEX = Regex("""\bis_signer\s*\(\s*([A-Za-z_]\w*)\s*\)""")
    private val IS_SIGNER_CALL_REGEX = Regex("""\bis_signer\s*\([^)]*\)""")

    /**
     * HIGH when a mutating operation's only use of a parameter is inside
     * `is_signer(<param>)`: the caller supplies the very key being checked and
     * signs with it, so the "gate" always passes - a phantom admin check.
     * `is_signer(p)` DOES prove the caller controls key p, so when p is also
     * used (keying the write, passed onward) it is the idiomatic self-binding
     * pattern and stays clean; the raw "any is_signer(param)" version of this
     * rule would flag every self-registration op and train agents to ignore
     * the gate.
     */
    private fun phantomSignerGateFindings(path: String, op: OperationBlock, mutates: Boolean): List<Finding> {
        if (!mutates) return emptyList()
        val paramNames = parseParams(op.params).mapTo(mutableSetOf()) { it.first }
        if (paramNames.isEmpty()) return emptyList()
        val checkedParams = IS_SIGNER_ARG_REGEX.findAll(op.body)
            .map { it.groupValues[1] }.filter { it in paramNames }.toSet()
        if (checkedParams.isEmpty()) return emptyList()
        val residual = IS_SIGNER_CALL_REGEX.replace(op.body, "is_signer(_)")
        return checkedParams.mapNotNull { param ->
            if (Regex("""\b${Regex.escape(param)}\b""").containsMatchIn(residual)) return@mapNotNull null
            Finding(
                "HIGH", "signer-check-on-untrusted-argument", path, op.line,
                "operation ${op.name} gates a mutation on is_signer('$param') where '$param' is a " +
                    "caller-supplied parameter used nowhere else - the caller passes their own key and " +
                    "signs with it, so the check always passes",
                "Check the signer against a trusted constant instead: " +
                    "op_context.is_signer(chain_context.args.admin_pubkey) (module args), or authenticate " +
                    "with ft4 auth and authorize against stored state. A caller-chosen key is not a gate."
            )
        }
    }

    // ---- value-op-without-transfer-flag ----
    // Ground truth: FT4 has_flags = flags.contains_all(required_flags), and
    // contains_all([]) is ALWAYS true (raw-ft4-src v1.1.0r
    // rell/src/lib/ft4/core/accounts/module.rell:502-504). A handler with
    // flags = [] admits EVERY auth descriptor on the account - including a
    // limited session/login key the user believed could not spend.
    /**
     * Value-moving state change: a debit (`-=`) on any field, or any write
     * (`+=` / `-=` / `=`) to a balance-named field. Credits/assignments to
     * non-value fields (counters, timestamps) deliberately do not count - a
     * session key bumping a view counter is not the exploit, and flagging it
     * would train agents to ignore the gate.
     */
    private val VALUE_MUTATION_REGEX = Regex(
        """\.\w*(?:balance|amount|credit|fund|supply|share|debt|stake|reward|coin|token)\w*\s*(?:\+=|-=|=(?!=))|\.\w+\s*-=""",
        RegexOption.IGNORE_CASE
    )
    private val AUTHENTICATE_CALL_REGEX = Regex("""\bauthenticate\s*\(""")
    private val ADD_AUTH_HANDLER_CALL_REGEX = Regex("""\badd_auth_handler\s*\(""")
    private val FLAGS_LIST_REGEX = Regex("""flags\s*=\s*\[([^\]]*)]""")
    private val ANY_LIST_REGEX = Regex("""\[([^\]]*)]""")

    /**
     * True when the app (non-lib, non-test) files register at least one FT4
     * auth handler and EVERY one of them has an empty flags list. With mixed
     * handlers we cannot statically tell which scope governs which operation,
     * so the rule stays quiet rather than guessing; a flags-by-variable
     * handler counts as non-empty for the same reason.
     */
    internal fun allAuthHandlersHaveEmptyFlags(files: Map<String, String>): Boolean {
        var empty = 0
        var nonEmptyOrUnknown = 0
        files.forEach { (path, content) ->
            if (RellLibs.isVendoredLibraryPath(path) || RellLibs.isThirdPartyLibPath(path)) return@forEach
            if (RunRellTests.isTestModuleSource(content)) return@forEach
            // Comment-masked, strings KEPT: fully masked text blanks "T" and
            // makes flags = ["T"] indistinguishable from flags = [].
            val masked = maskRellSource(content, maskStrings = false)
            ADD_AUTH_HANDLER_CALL_REGEX.findAll(masked).forEach { m ->
                val parenStart = masked.indexOf('(', m.range.first)
                val parenEnd = matchDelimiter(masked, parenStart, '(', ')') ?: return@forEach
                val args = masked.substring(parenStart + 1, parenEnd)
                val list = FLAGS_LIST_REGEX.find(args) ?: ANY_LIST_REGEX.find(args)
                if (list != null && list.groupValues[1].isBlank()) empty++ else nonEmptyOrUnknown++
            }
        }
        return empty > 0 && nonEmptyOrUnknown == 0
    }

    // ---- mass-mutation ----
    /** `update x @* {}` / `delete x @* {}`: an empty where-clause hits EVERY row. */
    private val MASS_MUTATION_REGEX = Regex("""\b(update|delete)\s+[A-Za-z_][\w.]*\s*@\*\s*\{\s*\}""")

    private fun massMutationFindings(path: String, masked: String): List<Finding> =
        MASS_MUTATION_REGEX.findAll(masked).map { m ->
            val keyword = m.groupValues[1]
            Finding(
                "HIGH", "mass-mutation", path,
                masked.substring(0, m.range.first).count { it == '\n' } + 1,
                m.value.replace(Regex("""\s+"""), " "),
                "$keyword with an empty where-clause ${if (keyword == "delete") "deletes" else "rewrites"} " +
                    "EVERY row of the entity. Filter the rows (delete x @* { .owner == account.id }); " +
                    "if a full wipe really is intended, it belongs behind an explicit admin gate."
            )
        }.toList()

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
        authMarkers: List<String> = AUTH_MARKERS,
        valueMutatingFunctions: Set<String> = emptySet(),
        ft4AuthCallers: Set<String> = emptySet(),
        emptyFlagsOnly: Boolean = false,
        requireFunctions: Set<String> = emptySet()
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        val calls = calledNames(op.body)
        val hasAuth = containsAuthMarker(op.body, authMarkers) ||
            authFunctions.any { it in calls }
        val hasRequire = REQUIRE_REGEX.containsMatchIn(op.body) ||
            requireFunctions.any { it in calls }
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
        if (hasAuth) {
            findings += confusedDeputyFindings(path, op, authFunctions)
        }
        findings += phantomSignerGateFindings(path, op, mutates)
        if (emptyFlagsOnly) {
            val ft4Authed = "authenticate" in calls || calls.any { it in ft4AuthCallers }
            val movesValue = VALUE_MUTATION_REGEX.containsMatchIn(op.body) ||
                calls.any { it in valueMutatingFunctions }
            if (ft4Authed && movesValue) {
                findings.add(
                    Finding(
                        "MEDIUM", "value-op-without-transfer-flag", path, op.line,
                        "operation ${op.name} moves value but every registered auth handler has flags = [] - " +
                            "FT4 checks flags with contains_all(), and contains_all([]) is always true, so ANY " +
                            "descriptor on the account (including limited session keys) can call it",
                        "Require the Transfer flag on the handler governing value-moving operations: " +
                            "auth.add_auth_handler(flags = [\"T\"]). Keep flags = [] only for operations that " +
                            "move no value."
                    )
                )
            }
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
