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
        "require_valid_proof"
        // op_context.get_signers( is deliberately NOT a token marker any more:
        // merely READING the signer list proved nothing (audit probe N7 wrote
        // `val signers = op_context.get_signers();` and never looked at the
        // value, and the gate called that auth). [getSignersUsedAsGate] below
        // recognizes it only when the VALUE actually gates or derives the
        // mutation.
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

    // ---- marker integrity: a marker must be bound to what it claims to be ----
    // A local `namespace auth { function authenticate() {} }` satisfied the
    // "auth.authenticate" token scan (audit probe N6): the marker matched a
    // no-op the ATTACKER defined. Markers derived from names the submission
    // can redefine are only trusted when the redefinition is provably absent.

    /** `import lib.ft4.auth;` / `import a: lib.ft4.auth;` - the real FT4 auth module. */
    private val FT4_AUTH_IMPORT_REGEX = Regex("""\bimport\s+(?:[A-Za-z_]\w*\s*:\s*)?lib\.ft4\.auth\b""")

    /** Any import line that mentions the real lib.ft4 tree. */
    private val FT4_ANY_IMPORT_REGEX = Regex("""\bimport\b[^;]*\blib\.ft4\b""")

    /** A local `namespace auth`/`namespace ft4` that can shadow the FT4 markers. */
    private val AUTH_NS_SPOOF_REGEX = Regex("""\bnamespace\s+(?:auth|ft4)\b""")

    /** `import auth: something;` - aliasing a module AS `auth`/`ft4`. */
    private val AUTH_ALIAS_SPOOF_REGEX = Regex("""\bimport\s+(?:auth|ft4)\s*:\s*([A-Za-z_][\w.]*)""")

    /**
     * True when any app (non-library) file defines a local `auth`/`ft4`
     * namespace or aliases a non-FT4 module as `auth`/`ft4` - i.e. the tokens
     * `auth.authenticate` / `ft4.auth` may resolve to attacker-defined code
     * somewhere in this submission. While a spoof is present, those two
     * markers are only trusted in files that genuinely import the FT4 module
     * (a real import and a same-named local namespace cannot coexist - the
     * compiler rejects the name clash - so the import proves the resolution).
     * With no spoof anywhere, behavior is unchanged.
     */
    internal fun authMarkerSpoofPresent(fullyMasked: Map<String, String>): Boolean =
        fullyMasked.any { (path, masked) ->
            if (RellLibs.isVendoredLibraryPath(path) || RellLibs.isThirdPartyLibPath(path)) return@any false
            AUTH_NS_SPOOF_REGEX.containsMatchIn(masked) ||
                AUTH_ALIAS_SPOOF_REGEX.findAll(masked).any { !it.groupValues[1].startsWith("lib.ft4") }
        }

    /**
     * True when the submission defines `require_valid_proof` itself and NO
     * definition contains a require(...) - a local no-op spoofing the ICCF
     * proof check (probe N12 shipped exactly `function require_valid_proof(x) {}`).
     * The genuine lib.iccf definition (vendored or hand-rolled wrappers that
     * really abort) contains a require and stays trusted; a submission that
     * only IMPORTS lib.iccf defines nothing locally and stays trusted.
     */
    internal fun validProofSpoofed(fullyMasked: Map<String, String>): Boolean {
        val defs = fullyMasked.values.flatMap { functionBodies(it) }
            .filter { (name, _) -> name == "require_valid_proof" }
        if (defs.isEmpty()) return false
        return defs.none { (_, body) -> REQUIRE_REGEX.containsMatchIn(body) }
    }

    internal data class MarkerDistrust(val authSpoof: Boolean, val proofSpoof: Boolean) {
        companion object { val NONE = MarkerDistrust(authSpoof = false, proofSpoof = false) }
    }

    internal fun markerDistrust(fullyMasked: Map<String, String>): MarkerDistrust =
        MarkerDistrust(authMarkerSpoofPresent(fullyMasked), validProofSpoofed(fullyMasked))

    /** Auth markers for one (masked) file: the trusted globals plus its FT4 auth aliases. */
    internal fun authMarkersFor(masked: String, distrust: MarkerDistrust = MarkerDistrust.NONE): List<String> {
        var base = AUTH_MARKERS
        if (distrust.authSpoof) {
            if (!FT4_AUTH_IMPORT_REGEX.containsMatchIn(masked)) base = base - "auth.authenticate"
            if (!FT4_ANY_IMPORT_REGEX.containsMatchIn(masked)) base = base - "ft4.auth"
        }
        if (distrust.proofSpoof) base = base - "require_valid_proof"
        return base + FT4_AUTH_ALIAS_REGEX.findAll(masked).map { "${it.groupValues[1]}.authenticate" }
    }

    // ---- get_signers must gate, not merely be read (a4) ----
    private val GET_SIGNERS_FOR_REGEX =
        Regex("""\b(?:for\s*\(\s*[A-Za-z_]\w*|[A-Za-z_][\w.]*)\s+in\s+op_context\s*\.\s*get_signers\s*\(""")
    private val GET_SIGNERS_CALL_REGEX = Regex("""op_context\s*\.\s*get_signers\s*\(\s*\)""")
    private val GET_SIGNERS_BIND_REGEX = Regex("""\bval\s+([A-Za-z_]\w*)\s*=\s*op_context\s*\.\s*get_signers\s*\(""")

    /**
     * True when op_context.get_signers() is used as an actual gate or as the
     * source the mutation derives from: iterated (`for (s in get_signers())`),
     * membership-tested (`x in get_signers()`), indexed, or bound to a local
     * that is then USED for something other than a presence check. Merely
     * binding the list - or checking only `.size()` / `.empty()`, which every
     * signed transaction passes - proves nothing about WHO signed and does
     * not count (audit probe N7).
     */
    internal fun getSignersUsedAsGate(text: String): Boolean {
        if (!text.contains("get_signers")) return false
        if (GET_SIGNERS_FOR_REGEX.containsMatchIn(text)) return true
        GET_SIGNERS_CALL_REGEX.findAll(text).forEach { m ->
            val rest = text.substring(m.range.last + 1).trimStart()
            if (rest.startsWith("[")) return true
            if (rest.startsWith(".")) {
                val method = rest.drop(1).trimStart().takeWhile { it.isLetterOrDigit() || it == '_' }
                if (method != "size" && method != "empty") return true
            }
        }
        GET_SIGNERS_BIND_REGEX.findAll(text).forEach { m ->
            val name = m.groupValues[1]
            Regex("""\b${Regex.escape(name)}\b""").findAll(text).forEach { r ->
                if (r.range.first >= m.range.first && r.range.first <= m.range.last) return@forEach
                val after = text.substring(r.range.last + 1).trimStart()
                if (!after.startsWith(".size") && !after.startsWith(".empty")) return true
            }
        }
        return false
    }

    private fun containsAuthMarker(text: String, markers: List<String>): Boolean =
        markers.any { text.contains(it) } || AUTH_HANDLER_REGEX.containsMatchIn(text) ||
            getSignersUsedAsGate(text)

    /**
     * (name, masked body) for EVERY function definition in the (masked) source.
     * A name-keyed map here let same-named functions in different namespaces of
     * ONE file clobber each other (last definition won), so a benign body could
     * hide a mutating or non-auth sibling from the per-name conservative merge
     * that only saw cross-file duplicates (audit 2026-09-01). Callers must
     * merge per name over all definitions, never assume the name is unique.
     */
    internal fun functionBodies(maskedContent: String): List<Pair<String, String>> =
        functionDefinitions(maskedContent).map { it.name to it.body }

    /**
     * One `function name(params) { body }` / `= expr;` definition, masked.
     * [expressionBody] is true for the `= expr` form, where the body IS the
     * return value (no `return` keyword to find).
     */
    internal data class FunctionDef(
        val name: String,
        val params: String,
        val body: String,
        val expressionBody: Boolean = false
    )

    /** Every function definition in the (masked) source with its raw parameter list - see [functionBodies]. */
    internal fun functionDefinitions(maskedContent: String): List<FunctionDef> {
        val functions = mutableListOf<FunctionDef>()
        FUNCTION_REGEX.findAll(maskedContent).forEach { match ->
            val name = match.groupValues[1]
            val parenStart = maskedContent.indexOf('(', match.range.first)
            val parenEnd = matchDelimiter(maskedContent, parenStart, '(', ')') ?: return@forEach
            val params = maskedContent.substring(parenStart + 1, parenEnd)
            val braceStart = maskedContent.indexOf('{', parenEnd)
            val eqIdx = maskedContent.indexOf('=', parenEnd)
            val blockBody = braceStart >= 0 && (eqIdx < 0 || braceStart < eqIdx)
            val body = when {
                blockBody -> {
                    val braceEnd = matchDelimiter(maskedContent, braceStart, '{', '}') ?: return@forEach
                    maskedContent.substring(braceStart + 1, braceEnd)
                }
                eqIdx >= 0 -> {
                    val end = maskedContent.indexOf(';', eqIdx).let { if (it < 0) maskedContent.length else it }
                    maskedContent.substring(eqIdx + 1, end)
                }
                else -> return@forEach
            }
            functions.add(FunctionDef(name, params, body, expressionBody = !blockBody))
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
        val distrust = markerDistrust(maskedFiles)
        val defs = mutableListOf<Def>()
        maskedFiles.forEach { (_, masked) ->
            val markers = authMarkersFor(masked, distrust)
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
        var queriesScanned = 0

        // Fully masked: brace/paren matching and the mutation/auth regexes must
        // never see braces, "update", or auth markers inside strings or comments.
        val fullyMasked = files.mapValues { (_, content) -> maskRellSource(content, maskStrings = true) }
        // Marker integrity first: a submission-local redefinition of the names
        // the markers key on (namespace auth spoof, no-op require_valid_proof)
        // strips those markers from the untrusted files.
        val distrust = markerDistrust(fullyMasked)
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
        // Every function name the submission defines: a parameter handed to a
        // callee OUTSIDE this set (library/builtin code we cannot see) gets the
        // benefit of the doubt in the per-parameter validation rules.
        val knownFunctions = fullyMasked.values.flatMapTo(mutableSetOf()) { m ->
            functionBodies(m).map { it.first }
        }
        val quorumTermPresent = submissionHasQuorumTerm(fullyMasked)
        val allEntityNames = entityNames(fullyMasked)
        val entityHelperReturns = helperEntityReturns(fullyMasked, allEntityNames)
        val priceReadFunctions = functionNamesMatchingSeed(
            fullyMasked.mapValues { (_, m) -> CHAIN_ARGS_REF_REGEX.replace(m, " ") },
            PRICE_STATE_READ_REGEX
        )
        val emptyFlagsOnly = allAuthHandlersHaveEmptyFlags(files)
        val inlinable = inlinableFunctions(fullyMasked)
        // Row fields that hold a price-derived quote (redemption.cash_due): the
        // operation that pays such a quote out reads no price itself.
        val priceDerivedFields = priceDerivedStoredFields(fullyMasked, allEntityNames, entityHelperReturns, priceReadFunctions)
        // Row fields carrying value that was already debited when the row was
        // created (a vesting grant's total): a payout bounded by one of these
        // is paid out of escrow, not minted.
        val escrowedCaps = escrowedCapFields(fullyMasked, allEntityNames, entityHelperReturns)
        // Fields that are one side of a two-sided record of one obligation
        // (loan.principal / pool.total_debt): raising them makes somebody OWE
        // more, so an unbacked-credit rule must not read them as a payout.
        val mirroredCounters = mirroredCounterFields(fullyMasked, allEntityNames, entityHelperReturns)
        // A clock parked in a row by one operation is the same public number
        // when another reads it back, so those fields are clock sources too.
        val storedClockFields = clockDerivedStoredFields(fullyMasked, allEntityNames, entityHelperReturns)
        // Identity-typed attributes: the fields that name WHO a row belongs to.
        // The randomness rule keys the beneficiary on this type, never on a name.
        val identityFieldNames = entityFields(fullyMasked)
            .filter { it.type.trimEnd('?') == "byte_array" }
            .mapTo(mutableSetOf()) { it.name }

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
            findings += massMutationFindings(path, masked, commentMasked)
            findings += icmfReceiverFindings(
                path, masked, mutatingFunctions, requireFunctions, knownFunctions, allEntityNames
            )
            if (!RunRellTests.isTestModuleSource(content)) {
                val queries = scanQueries(masked)
                queriesScanned += queries.size
                queries.forEach { q -> findings += querySecretExposureFindings(path, q) }
            }
            val authMarkers = authMarkersFor(masked, distrust)
            val ops = scanOperations(path, masked)
            operationsScanned += ops.size
            ops.forEach { op ->
                findings += operationFindings(
                    path, op, authFunctions, mutatingFunctions, authMarkers,
                    valueMutatingFunctions, ft4AuthCallers, emptyFlagsOnly, requireFunctions,
                    inlinable, allEntityNames, RunRellTests.isTestModuleSource(content)
                )
                findings += amountLowerBoundFindings(
                    path, op, requireFunctions, knownFunctions, valueMutatingFunctions,
                    mutatingFunctions, allEntityNames
                )
                findings += perParamValidationFindings(
                    path, op, requireFunctions, knownFunctions, mutatingFunctions, allEntityNames
                )
                findings += iccfProvenanceFindings(path, op, mutatingFunctions)
                findings += majorityWithoutQuorumFindings(path, op, valueMutatingFunctions, quorumTermPresent)
                findings += unboundedTimeWindowFindings(path, op, requireFunctions)
                findings += unbackedConversionFindings(
                    path, op, allEntityNames, entityHelperReturns, priceReadFunctions, priceDerivedFields,
                    inlinable, escrowedCaps, mirroredCounters
                )
                findings += blockClockRandomnessFindings(
                    path, op, allEntityNames, entityHelperReturns, inlinable, identityFieldNames,
                    storedClockFields
                )
            }
        }

        findings += valueSinkFindings(files, fullyMasked, allEntityNames, entityHelperReturns)
        findings += droppedQuorumGateFindings(files, fullyMasked, inlinable, allEntityNames, valueMutatingFunctions)

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
            append(
                "Scanned ${files.size - exemptedLibFiles - thirdPartyLibFiles} file(s), " +
                    "$operationsScanned operation(s), $queriesScanned query(ies). "
            )
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
                "Heuristic static checks only (authentication AND authorization binding, auth on every " +
                    "path, signer-gate integrity, auth-marker binding to real FT4, auth-handler flags, " +
                    "mass mutations, require() validation incl. amount sign bounds and per-parameter " +
                    "coverage, ICMF sender binding, ICCF proof provenance, query secret exposure, banned " +
                    "FT4 admin modules, hardcoded secrets, the block clock used to SELECT who receives " +
                    "value) - a clean report does not replace a security audit. These rules structurally " +
                    "cannot see whether an outcome meant to be UNPREDICTABLE actually is (no chain value " +
                    "is secret: a hash, a counter or a seed mixed from on-chain state is public before " +
                    "the transaction is signed, so only the block-clock-as-selector shape is caught), nor " +
                    "anything about TRANSACTION ORDERING / MEV (front-running, sandwiching, a price or a " +
                    "listing repriced under a pending transaction). Economic invariants (quorum, quorum " +
                    "gates dropped on a sibling payout path, " +
                    "voting windows, reserve backing of price- and time-derived credits, locked value " +
                    "sinks) get ADVISORY MEDIUM findings only: they are design judgments no static rule " +
                    "can prove, they never block, and their absence does not certify sound economics."
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
    /**
     * Parameter names that denote an account/identity being acted UPON.
     * ADDITIVE trigger only - it can widen the account-param set for unusual
     * types, never narrow it. The security boundary is [isAccountTypedParam]:
     * the attacker chooses parameter names, so when this list WAS the gate the
     * identical drain was HIGH as `from` and silent as `victim`/`target`/
     * `beneficiary`/... (verified against the built jar, adversary round 2).
     */
    private val ACCOUNT_PARAM_NAME_REGEX = Regex(
        """(?:^|_)(account|owner|from|sender|user|holder|wallet|member|spender|payer)(?:$|_)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Account-ish by TYPE and use, not by name: byte_array / pubkey / *account*
     * types are how identities travel in Rell. Whether such a parameter is
     * DANGEROUS is decided downstream by use ([harmfulMutationKindKeyedBy]:
     * it must key a debit/delete and never be bound to the caller) - so a
     * hash/blob byte_array that keys nothing harmful stays clean, whatever
     * it is called.
     */
    private fun isAccountTypedParam(name: String, type: String): Boolean {
        val t = type.trim().trimEnd('?').trim().lowercase()
        return t == "byte_array" || t == "pubkey" || t.contains("account") ||
            ACCOUNT_PARAM_NAME_REGEX.containsMatchIn(name)
    }

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

    // ---- helper inlining: keyed per-operation rules must see through calls ----
    // The submission-wide auth/mutation closures answer "does this op reach a
    // mutation / an auth marker" but not "keyed by WHICH identifier". The
    // confused-deputy and phantom-signer rules key on operation parameters, so
    // an attacker moved the keyed mutation (`.balance -= amount` keyed by the
    // caller-supplied `from`) or the phantom gate (`is_signer(admin)`) one call
    // deep and both rules went silent while the exploit still ran (adversary
    // round 3, verified against the built jar). [inlineHelpers] rewrites every
    // reachable app-owned helper body into the operation's own namespace -
    // formals become the caller's argument text, so "caller-supplied" survives
    // the call, and helper locals get fresh names so they can never capture a
    // caller identifier - recursively, cross-file, through namespaced calls.

    /** Bare name -> every definition of an app-owned function (all files, all namespaces). */
    internal fun inlinableFunctions(fullyMasked: Map<String, String>): Map<String, List<FunctionDef>> {
        val out = mutableMapOf<String, MutableList<FunctionDef>>()
        fullyMasked.forEach { (path, masked) ->
            // Library code (vendored FT4, third-party lib/) is not inlined: its
            // functions stay opaque callees, exactly as they are for the
            // per-parameter validation rules (benefit of the doubt).
            if (RellLibs.isVendoredLibraryPath(path) || RellLibs.isThirdPartyLibPath(path)) return@forEach
            functionDefinitions(masked).forEach { def -> out.getOrPut(def.name) { mutableListOf() }.add(def) }
        }
        return out
    }

    private const val INLINE_MAX_DEPTH = 6
    private const val INLINE_MAX_CHARS = 200_000
    private val NAMED_ARG_REGEX = Regex("""^([A-Za-z_]\w*)\s*=(?!=)(.*)$""", RegexOption.DOT_MATCHES_ALL)
    private val LOCAL_DECL_REGEX = Regex("""\b(?:val|var)\s+([A-Za-z_]\w*)""")
    /** An identifier that is not a member access (`.owner` and the `id` of `account.id` are left alone). */
    private val FREE_IDENT_REGEX = Regex("""(?<![.\w])[A-Za-z_]\w*""")

    /** Top-level comma split of a call's argument text (`(`, `[`, `{` nest; `<` is a comparison here). */
    private fun splitArgs(text: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        for (c in text) {
            when (c) {
                '(', '[', '{' -> { depth++; cur.append(c) }
                ')', ']', '}' -> { depth--; cur.append(c) }
                ',' -> if (depth == 0) { out.add(cur.toString()); cur.clear() } else cur.append(c)
                else -> cur.append(c)
            }
        }
        if (cur.isNotBlank()) out.add(cur.toString())
        return out.map { it.trim() }
    }

    /**
     * The helper body rewritten in the caller's namespace: each formal becomes
     * the actual argument text bound to it (positional or `name = expr`), and
     * every unbound formal or helper-declared local is renamed to a fresh
     * identifier. Substitution is a single pass over free identifiers, so a
     * replacement is never itself rewritten.
     */
    private fun bindHelperBody(def: FunctionDef, argText: String, serial: Int): String {
        val formals = parseParams(def.params).map { it.first }
        val binding = mutableMapOf<String, String>()
        var positional = 0
        splitArgs(argText).forEach { actual ->
            val named = NAMED_ARG_REGEX.find(actual)
            if (named != null && named.groupValues[1] in formals) {
                binding[named.groupValues[1]] = named.groupValues[2].trim()
            } else if (positional < formals.size) {
                binding[formals[positional]] = actual
                positional++
            }
        }
        formals.forEach { binding.putIfAbsent(it, "${it}__h$serial") }
        LOCAL_DECL_REGEX.findAll(def.body).forEach { m ->
            binding.putIfAbsent(m.groupValues[1], "${m.groupValues[1]}__h$serial")
        }
        return FREE_IDENT_REGEX.replace(def.body) { m -> binding[m.value] ?: m.value }
    }

    /**
     * [body] with every call to an app-owned helper expanded in place: the
     * call keeps its name but its argument list is replaced by the bound
     * helper body (`check(admin)` -> `check( ; require(is_signer(admin)); )`),
     * so an identifier whose only use was as an argument to an inlined helper
     * no longer reads as "used" at the call site. Recursive to
     * [INLINE_MAX_DEPTH] (chains of helpers), cycle-guarded, every same-named
     * definition expanded (conservative, as for the closures), capped at
     * [INLINE_MAX_CHARS]. A name preceded by update/delete is a mutation
     * target, never a callee ([isMutationTarget] precedent).
     */
    internal fun inlineHelpers(
        body: String,
        functions: Map<String, List<FunctionDef>>,
        entities: Set<String>
    ): String {
        if (functions.isEmpty()) return body
        var serial = 0
        var budget = INLINE_MAX_CHARS
        fun expand(text: String, depth: Int, stack: Set<String>): String {
            if (depth >= INLINE_MAX_DEPTH || budget <= 0) return text
            val sb = StringBuilder()
            var last = 0
            CALL_SITE_REGEX.findAll(text).forEach { m ->
                if (m.range.first < last) return@forEach
                val callee = m.groupValues[1]
                val defs = functions[callee] ?: return@forEach
                if (callee in stack || callee in CONTROL_KEYWORDS || callee in entities || isMutationTarget(text, m)) {
                    return@forEach
                }
                val parenStart = text.indexOf('(', m.range.first)
                val parenEnd = matchDelimiter(text, parenStart, '(', ')') ?: return@forEach
                val args = text.substring(parenStart + 1, parenEnd)
                sb.append(text, last, parenStart + 1)
                defs.forEach { def ->
                    serial++
                    val bound = bindHelperBody(def, args, serial)
                    budget -= bound.length
                    sb.append(" ;").append(expand(bound, depth + 1, stack + callee)).append("; ")
                }
                sb.append(')')
                last = parenEnd + 1
            }
            sb.append(text, last, text.length)
            return sb.toString()
        }
        return expand(body, 0, emptySet())
    }

    private val RETURN_STMT_REGEX = Regex("""\breturn\b([^;]*)$""")

    /**
     * [body] as a FLAT statement list: every reachable app-owned helper's
     * statements are hoisted in front of the statement that calls it, and the
     * call keeps its name but its argument list becomes the helper's return
     * expression(s) - `val r = reward_for(m);` reads as
     * `val now__h1 = op_context.last_block_time; ...; val r = reward_for( m.staked * (now__h1 - since__h1) * RATE );`.
     * Same binding and renaming as [inlineHelpers] (formals become the caller's
     * argument text, helper locals get fresh names), same depth/budget/cycle
     * guards, every same-named definition expanded. Unlike [inlineHelpers] the
     * result splits on ';' into one statement per fragment, so the data-flow
     * rules (time/price taint, credit-vs-debit pairing) see a helper's writes
     * and bindings exactly as if they were written in the operation - moving
     * the reward math or the payout into a helper changes nothing.
     */
    internal fun flattenHelpers(
        body: String,
        functions: Map<String, List<FunctionDef>>,
        entities: Set<String>
    ): String {
        if (functions.isEmpty()) return body
        var serial = 0
        var budget = INLINE_MAX_CHARS
        fun expand(text: String, depth: Int, stack: Set<String>): String {
            if (depth >= INLINE_MAX_DEPTH || budget <= 0) return text
            val out = StringBuilder()
            text.split(';').forEach { fragment ->
                val hoisted = StringBuilder()
                val sb = StringBuilder()
                var last = 0
                CALL_SITE_REGEX.findAll(fragment).forEach { m ->
                    if (m.range.first < last) return@forEach
                    val callee = m.groupValues[1]
                    val defs = functions[callee] ?: return@forEach
                    if (callee in stack || callee in CONTROL_KEYWORDS || callee in entities || isMutationTarget(fragment, m)) {
                        return@forEach
                    }
                    val parenStart = fragment.indexOf('(', m.range.first)
                    val parenEnd = matchDelimiter(fragment, parenStart, '(', ')') ?: return@forEach
                    val args = fragment.substring(parenStart + 1, parenEnd)
                    val returns = mutableListOf<String>()
                    defs.forEach { def ->
                        serial++
                        val bound = bindHelperBody(def, args, serial)
                        budget -= bound.length
                        val flat = expand(bound, depth + 1, stack + callee).trimEnd().trimEnd(';').split(';')
                        if (def.expressionBody) {
                            // Hoisted statements of nested calls come first; the
                            // expression itself is the last fragment.
                            flat.dropLast(1).forEach { hoisted.append(it).append(';') }
                            returns.add(flat.last())
                        } else {
                            flat.forEach { stmt ->
                                val r = RETURN_STMT_REGEX.find(stmt)
                                if (r != null) {
                                    returns.add(r.groupValues[1])
                                    hoisted.append(stmt, 0, r.range.first).append(';')
                                } else {
                                    hoisted.append(stmt).append(';')
                                }
                            }
                        }
                    }
                    sb.append(fragment, last, parenStart + 1)
                    sb.append(returns.filter { it.isNotBlank() }.joinToString(" , "))
                    sb.append(')')
                    last = parenEnd + 1
                }
                sb.append(fragment, last, fragment.length)
                out.append(hoisted).append(sb).append(';')
            }
            return out.toString()
        }
        return expand(body, 0, emptySet())
    }

    /**
     * HIGH when an authenticated operation debits/deletes rows selected by an
     * account-ish operation parameter that is never bound to the caller: no
     * statement relates it to an authenticated-identity variable, no
     * `is_signer(<param>)`, and the operation is not an admin op keyed to
     * `chain_context.args.*`. Runs only when the operation authenticates -
     * the unauthenticated case is already `unauthenticated-mutation`.
     * [body] is the operation body with reachable helpers inlined
     * ([inlineHelpers]) - the keyed mutation and the binding are found
     * wherever the call chain puts them.
     */
    private fun confusedDeputyFindings(
        path: String,
        op: OperationBlock,
        body: String,
        authFunctions: Set<String>
    ): List<Finding> {
        // Admin ops keyed to blockchain config are the sanctioned break-glass
        // pattern (require(account.id == chain_context.args.admin, ...)).
        if (body.contains("chain_context.args")) return emptyList()
        val accountParams = parseParams(op.params)
            .filter { (name, type) -> isAccountTypedParam(name, type) }
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
            // The parameter plus every local assigned from it: `val victim = from;`
            // then keying on `victim` (in the body or handed to a helper) is
            // the same caller-controlled selection under another name.
            val names = taintedNames(body, param)
            val alternatives = names.joinToString("|") { Regex.escape(it) }
            // Free identifiers only: `.from` is an attribute, not the parameter.
            val paramRef = Regex("""(?<![.\w])(?:$alternatives)\b""")
            // is_signer(<param>) proves the caller controls that key - bound.
            if (Regex("""is_signer\s*\(\s*(?:$alternatives)\s*\)""").containsMatchIn(body)) return@forEach
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
     * the gate. [body] is the operation body with reachable helpers inlined:
     * `check(admin)` over `function check(k) { require(is_signer(k)); }` is
     * the same phantom gate one call deep, and the inlined call site no longer
     * counts the argument as a use.
     */
    private fun phantomSignerGateFindings(
        path: String,
        op: OperationBlock,
        body: String,
        mutates: Boolean
    ): List<Finding> {
        if (!mutates) return emptyList()
        val paramNames = parseParams(op.params).mapTo(mutableSetOf()) { it.first }
        if (paramNames.isEmpty()) return emptyList()
        // `val k = admin; require(is_signer(k));` is the same phantom gate
        // through a local: the check is matched against the parameter's taint
        // set, and the binding statements themselves do not count as a use.
        val taints = paramNames.associateWith { taintedNames(body, it) }
        val signerArgs = IS_SIGNER_ARG_REGEX.findAll(body).map { it.groupValues[1] }.toSet()
        val checkedParams = paramNames.filter { p -> taints.getValue(p).any { it in signerArgs } }
        if (checkedParams.isEmpty()) return emptyList()
        val withoutGates = IS_SIGNER_CALL_REGEX.replace(body, "is_signer(_)")
        return checkedParams.mapNotNull { param ->
            val names = taints.getValue(param)
            val residual = VAL_BINDING_REGEX.replace(withoutGates) { m ->
                if (m.groupValues[1] in names) "" else m.value
            }
            val used = Regex("""(?<![.\w])(?:${names.joinToString("|") { Regex.escape(it) }})\b""")
            if (used.containsMatchIn(residual)) return@mapNotNull null
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
    /** `update x @* {` / `delete x @* {`: a set-mutation; the where-clause decides how many rows. */
    private val BULK_MUTATION_REGEX = Regex("""\b(update|delete)\s+[A-Za-z_][\w.]*\s*@\*\s*\{""")
    /** `x""` / `""` / `''` - the empty literal no real key equals. Read from comment-masked text. */
    private val EMPTY_LITERAL = Regex("""^x?(?:""|'')$""")
    private val EMPTY_LITERAL_BLANKED = Regex("""^x?$""")
    /** A member/attribute reference that is not `op_context.*` / `chain_context.*`. */
    private val ATTRIBUTE_REF_REGEX = Regex("""(?<!\bop_context|\bchain_context)\.[A-Za-z_]""")

    /** One `update|delete <entity> @* { where }` site in a masked text. */
    private data class BulkMutation(val keyword: String, val start: Int, val whereStart: Int, val whereEnd: Int)

    private fun bulkMutations(masked: String): List<BulkMutation> =
        BULK_MUTATION_REGEX.findAll(masked).mapNotNull { m ->
            val braceStart = m.range.last
            val braceEnd = matchDelimiter(masked, braceStart, '{', '}') ?: return@mapNotNull null
            BulkMutation(m.groupValues[1], m.range.first, braceStart + 1, braceEnd)
        }.toList()

    /** Top-level split on a keyword (`and` / `or`) - parentheses and brackets nest. */
    private fun splitTopLevelKeyword(text: String, keyword: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var i = 0
        val kw = Regex("""\b$keyword\b""")
        while (i < text.length) {
            val c = text[i]
            if (c == '(' || c == '[') depth++
            if (c == ')' || c == ']') depth--
            val m = if (depth == 0 && c == keyword[0]) kw.find(text, i)?.takeIf { it.range.first == i } else null
            if (m != null) {
                out.add(cur.toString())
                cur.clear()
                i = m.range.last + 1
                continue
            }
            cur.append(c)
            i++
        }
        out.add(cur.toString())
        return out
    }

    /**
     * True when the where-clause cannot exclude a real row. Two provable
     * shapes: no attribute is referenced at all (`1 == 1`, `true`, `flag`,
     * `op_context.last_block_time > 0` - the condition is the same for every
     * row, so it selects all rows or none), or `<attr> != <empty literal>`
     * (`.account_id != x""` - no real key is the empty value). A disjunction
     * with a tautological branch is a tautology; a conjunction needs every
     * conjunct. [whereText] is comment-masked (strings kept) so the literal
     * is readable.
     */
    internal fun isTautologicalWhere(whereText: String, literalsBlanked: Boolean = false): Boolean {
        if (whereText.isBlank()) return true
        // With string contents AND quotes blanked (the fully masked text the
        // per-operation rules run on) an empty literal and a non-empty one
        // are the same run of spaces; that mode only ever SUPPRESSES the
        // advisory rule, so it errs toward "tautology".
        val empty = if (literalsBlanked) EMPTY_LITERAL_BLANKED else EMPTY_LITERAL
        fun atomTautological(atom: String): Boolean {
            val t = atom.trim()
            if (t.isEmpty()) return false
            if (!ATTRIBUTE_REF_REGEX.containsMatchIn(t) && !t.contains('$')) return true
            val sides = t.split("!=")
            if (sides.size != 2) return false
            val l = sides[0].trim()
            val r = sides[1].trim()
            return (empty.matches(r) && ATTRIBUTE_REF_REGEX.containsMatchIn(l)) ||
                (empty.matches(l) && ATTRIBUTE_REF_REGEX.containsMatchIn(r))
        }
        return splitTopLevelKeyword(whereText, "or").any { disjunct ->
            splitTopLevelKeyword(disjunct, "and").all { atomTautological(it) }
        }
    }

    /**
     * HIGH `mass-mutation` for a set-mutation whose where-clause is empty or
     * tautological ([isTautologicalWhere]). [masked] (strings blanked) gives
     * the structure; [commentMasked] (strings kept) is read at the same
     * offsets for the literal - both maskings are length-preserving.
     */
    private fun massMutationFindings(path: String, masked: String, commentMasked: String): List<Finding> =
        bulkMutations(masked).mapNotNull { b ->
            // Quotes come from the comment-masked text, everything else from
            // the fully masked one: `and`/`or` inside a literal must not split
            // the clause, but `""` vs `"x"` must stay distinguishable.
            val whereRaw = buildString {
                for (i in b.whereStart until b.whereEnd) {
                    val q = commentMasked[i]
                    append(if (q == '"' || q == '\'') q else masked[i])
                }
            }
            if (!isTautologicalWhere(whereRaw)) return@mapNotNull null
            val keyword = b.keyword
            val site = masked.substring(b.start, b.whereEnd + 1).replace(Regex("""\s+"""), " ")
            val verb = if (keyword == "delete") "deletes" else "rewrites"
            val shape = if (whereRaw.isBlank()) "an empty where-clause" else
                "a where-clause that excludes no real row (${whereRaw.trim().replace(Regex("""\s+"""), " ")})"
            Finding(
                "HIGH", "mass-mutation", path,
                masked.substring(0, b.start).count { it == '\n' } + 1,
                site,
                "$keyword with $shape $verb EVERY row of the entity. Filter the rows " +
                    "(delete x @* { .owner == account.id }); if a full wipe really is intended, it belongs " +
                    "behind an explicit admin gate."
            )
        }

    /**
     * ADVISORY MEDIUM `bulk-mutation-not-caller-bound`: a set-mutation whose
     * where-clause references nothing the caller or the chain supplies - no
     * operation parameter (or local derived from one), no authenticated
     * identity, no `op_context`/`chain_context` term, no function call. Such a
     * filter (`.status == "pending"`, `.tier > 0`) selects the same rows for
     * every caller, so any account that can reach the operation rewrites or
     * deletes rows it does not own. A tautology dressed up as a filter
     * (`.account_id.size() >= 0`, `not (.id == x"")`) lands here too, so the
     * provable HIGH rule cannot be sidestepped in silence. Whether that is a
     * wipe or a legitimate
     * batch job is not decidable from the filter text, so this is advisory
     * only and never blocks; the tautological shapes that ARE provable are
     * the HIGH `mass-mutation` rule. Skipped for admin ops keyed to
     * `chain_context.args` and for @test modules (fixtures wipe tables).
     * [body] is the helper-inlined operation body.
     */
    private fun bulkMutationNotCallerBoundFindings(
        path: String,
        op: OperationBlock,
        body: String,
        authFunctions: Set<String>,
        knownFunctions: Set<String>
    ): List<Finding> {
        // A call to code the submission does not define (`now()`, a library
        // predicate) may bind the filter where we cannot see - benefit of the
        // doubt, as for paramDelegated. A method on an attribute
        // (`.account_id.size() >= 0`), a control keyword (`not (...)`) or an
        // app helper (already inlined) is not that.
        fun callsUnknown(where: String): Boolean = CALL_SITE_REGEX.findAll(where).any { m ->
            val callee = m.groupValues[1]
            val precededByDot = m.range.first > 0 && where[m.range.first - 1] == '.'
            !precededByDot && callee !in CONTROL_KEYWORDS && callee !in knownFunctions
        }
        if (body.contains("chain_context.args")) return emptyList()
        val sites = bulkMutations(body)
        if (sites.isEmpty()) return emptyList()
        val seeds = parseParams(op.params).mapTo(mutableSetOf()) { it.first }
        VAL_CALL_REGEX.findAll(body).forEach { m ->
            val callee = m.groupValues[2].substringAfterLast('.')
            if (callee == "authenticate" || callee in authFunctions) seeds.add(m.groupValues[1])
        }
        VAL_BINDING_REGEX.findAll(body).forEach { m ->
            if (m.groupValues[2].contains("op_context") || m.groupValues[2].contains("chain_context")) {
                seeds.add(m.groupValues[1])
            }
        }
        val trusted = seeds.flatMapTo(mutableSetOf()) { taintedNames(body, it) }
        return sites.mapNotNull { b ->
            val where = body.substring(b.whereStart, b.whereEnd)
            // Empty/tautological filters are the HIGH rule's; a call, a chain
            // term or a trusted name in the filter gets the benefit of the doubt.
            if (where.isBlank() || isTautologicalWhere(where, literalsBlanked = true)) return@mapNotNull null
            if (where.contains("op_context") || where.contains("chain_context") || callsUnknown(where)) {
                return@mapNotNull null
            }
            if (trusted.any { Regex("""(?<![.\w])${Regex.escape(it)}\b""").containsMatchIn(where) }) {
                return@mapNotNull null
            }
            val verb = if (b.keyword == "delete") "deletes" else "rewrites"
            Finding(
                "MEDIUM", "bulk-mutation-not-caller-bound", path, op.line,
                "operation ${op.name} $verb every row matching { ${where.trim().replace(Regex("""\s+"""), " ")} } - " +
                    "the filter references no parameter, no authenticated identity and no chain state, so it " +
                    "selects the same rows for every caller",
                "If this is a per-caller change, key the rows off the authenticated identity " +
                    "(.owner == account.id). If it is a batch job, gate it explicitly (an admin from " +
                    "chain_context.args, or a stored role). Advisory only: the filter text cannot show " +
                    "whether the selection is intended."
            )
        }
    }

    // ---- economic-invariant advisories (MEDIUM, never blocking) ----
    // The adversary round proved the drainable dApps were drainable by DESIGN:
    // quorumless governance and unbacked oracle conversion both shipped with
    // every operation authenticated and validated. None of these shapes is
    // statically provable - whether a DAO needs a quorum is a design decision,
    // and conservation cannot be decided from syntax - so every rule in this
    // section reports MEDIUM and can never make ok=false. A gate that blocks
    // on a heuristic trains agents to route around it (observed); an advisory
    // names the invariant the developer must consciously own.

    /** `yes_votes > no_votes` - two vote-tally terms compared to each other. */
    private val MAJORITY_COMPARISON_REGEX = Regex(
        """[\w.]*(?:vote|tally|ballot)\w*\s*>=?\s*[\w.]*(?:vote|tally|ballot)\w*""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Evidence the governance thought about participation or weighting: a
     * quorum/threshold identifier, or membership/supply/stake/weight terms.
     * Any of these anywhere in the submission's own (non-lib) code silences
     * the rule - a stake-weighted DAO's execute operation is textually
     * IDENTICAL to an unweighted one (the weighting lives in cast_vote), so
     * only a submission-wide scan can tell them apart. The bias is deliberate:
     * prefer missing a quorumless DAO in a codebase that uses these words
     * elsewhere over advising a designer who demonstrably weighed votes.
     */
    private val QUORUM_TERM_REGEX = Regex(
        """quorum|threshold|min_votes|min_yes|total_members|member_count|total_votes|total_supply|voting_power|weight|stake""",
        RegexOption.IGNORE_CASE
    )

    /** True when any non-library file of the submission contains a quorum/weight term. */
    internal fun submissionHasQuorumTerm(fullyMasked: Map<String, String>): Boolean =
        fullyMasked.any { (path, masked) ->
            !RellLibs.isVendoredLibraryPath(path) && !RellLibs.isThirdPartyLibPath(path) &&
                QUORUM_TERM_REGEX.containsMatchIn(masked)
        }

    /**
     * MEDIUM when an operation moves value gated by a bare vote-majority
     * comparison and no quorum, threshold, or weight term exists anywhere in
     * the submission. The adversary DAO drain is exactly this: one account
     * with zero contribution proposes paying itself, votes 1-0 on its own
     * proposal, and executes. Advisory, never blocking: a two-party escrow
     * where 1-0 is a legitimate outcome exists, and only the designer knows
     * which one they built.
     */
    private fun majorityWithoutQuorumFindings(
        path: String,
        op: OperationBlock,
        valueMutatingFunctions: Set<String>,
        quorumTermPresent: Boolean
    ): List<Finding> {
        if (quorumTermPresent) return emptyList()
        if (!MAJORITY_COMPARISON_REGEX.containsMatchIn(op.body)) return emptyList()
        val calls = calledNames(op.body)
        val movesValue = VALUE_MUTATION_REGEX.containsMatchIn(op.body) || calls.any { it in valueMutatingFunctions }
        if (!movesValue) return emptyList()
        return listOf(
            Finding(
                "MEDIUM", "majority-without-quorum", path, op.line,
                "operation ${op.name} moves value gated only by a bare vote majority (yes > no) - " +
                    "no quorum, participation threshold, or vote-weight term anywhere in the check, so " +
                    "a single account voting 1-0 on its own proposal satisfies it",
                "Add a participation floor and/or weight votes: require(yes_votes + no_votes >= quorum) " +
                    "with quorum derived from membership or supply, or accumulate voting_power per voter " +
                    "instead of 1. Advisory: whether this governance needs a quorum is a design decision " +
                    "static analysis cannot prove - if a bare majority is intended (e.g. 2-party escrow), " +
                    "document it and ignore this finding."
            )
        )
    }

    /** Parameter names that set the length of some time window. */
    private val TIME_WINDOW_PARAM_REGEX = Regex("""period|window|duration""", RegexOption.IGNORE_CASE)

    /** The statement actually uses the parameter as a time offset/deadline. */
    private val TIME_ANCHOR_REGEX = Regex(
        """last_block_time|block_time|deadline|expires|expiry|ends_at|closes_at""",
        RegexOption.IGNORE_CASE
    )

    /**
     * MEDIUM when a caller-supplied period/window/duration parameter feeds a
     * deadline and its only lower bound is `> 0`. The adversary DAO accepted
     * voting_period_ms = 1: `require(voting_period_ms > 0)` reads like
     * validation but permits a voting window that is over before anyone else
     * can vote, turning governance into a race the proposer always wins.
     * Advisory, never blocking: the right minimum is a design number the gate
     * cannot know, and some windows (short auctions, heartbeats) are
     * legitimately tiny.
     */
    private fun unboundedTimeWindowFindings(
        path: String,
        op: OperationBlock,
        requireFunctions: Set<String>
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        val statements = op.body.split(';')
        parseParams(op.params).forEach { (name, type) ->
            if (!TIME_WINDOW_PARAM_REGEX.containsMatchIn(name)) return@forEach
            if (!type.lowercase().contains("integer")) return@forEach
            val paramRef = Regex("""\b${Regex.escape(name)}\b""")
            val timed = statements.any { paramRef.containsMatchIn(it) && TIME_ANCHOR_REGEX.containsMatchIn(it) }
            if (!timed) return@forEach
            // Lower bounds on the parameter: `p > X` / `p >= X` / `X < p` / `X <= p`.
            val lowerBounds =
                Regex("""\b${Regex.escape(name)}\b\s*>=?\s*([\w.]+)""").findAll(op.body).map { it.groupValues[1] } +
                    Regex("""([\w.]+)\s*<=?\s*\b${Regex.escape(name)}\b""").findAll(op.body).map { it.groupValues[1] }
            if (lowerBounds.any { it != "0" }) return@forEach
            // Validation delegated to a require()-bearing helper the param is
            // passed to may bound it where this scan cannot see - stay quiet.
            val delegated = requireFunctions.any { fn ->
                Regex("""\b${Regex.escape(fn)}\s*\([^)]*\b${Regex.escape(name)}\b""").containsMatchIn(op.body)
            }
            if (delegated) return@forEach
            findings.add(
                Finding(
                    "MEDIUM", "unbounded-voting-period", path, op.line,
                    "operation ${op.name} sets a time window from caller-supplied '$name' with no minimum " +
                        "(only compared against 0, or not at all) - $name = 1 closes the window in the same " +
                        "block it opens, e.g. a voting period nobody but the proposer can act in",
                    "Enforce a real minimum: require($name >= min_period) with the floor from module args " +
                        "or a named constant. Advisory: the right minimum is a design decision - if a " +
                        "near-zero window is intended here, document why and ignore this finding."
                )
            )
        }
        return findings
    }

    // ---- unbacked-conversion-credit (oracle mint) ----
    // The adversary oracle vault (dapp_d_oracle, certified ok:true) turns
    // 100 USD into 200,000,000: buy tokens at a transient low price, sell
    // back at the restored fair price. sell_tokens credits USD that no
    // reserve ever held - the credited entity is never debited, so value is
    // created from nothing at whatever rate the price feed says.

    private val ENTITY_DEF_REGEX = Regex("""\bentity\s+([A-Za-z_]\w*)""")

    /** All entity names declared anywhere in the (masked) submission. */
    internal fun entityNames(maskedFiles: Map<String, String>): Set<String> =
        maskedFiles.values.flatMapTo(mutableSetOf()) { m ->
            ENTITY_DEF_REGEX.findAll(m).map { it.groupValues[1] }
        }

    /** `function get_or_create_x(...): some_entity` - helper name to entity it returns. */
    private val FUNCTION_RETURN_REGEX = Regex("""\bfunction\s+([A-Za-z_]\w*)\s*\([^()]*\)\s*:\s*([A-Za-z_]\w*)""")

    internal fun helperEntityReturns(maskedFiles: Map<String, String>, entities: Set<String>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        maskedFiles.values.forEach { m ->
            FUNCTION_RETURN_REGEX.findAll(m).forEach { f ->
                if (f.groupValues[2] in entities) out[f.groupValues[1]] = f.groupValues[2]
            }
        }
        return out
    }

    /**
     * One textual write to an entity field. [entity] is null when the update
     * target could not be resolved (dotted path, unknown local) - callers must
     * treat null conservatively for their direction of the argument.
     */
    internal data class ValueWrite(val entity: String?, val field: String, val kind: String)

    private val ALIAS_AT_REGEX = Regex("""\bval\s+([A-Za-z_]\w*)\s*=\s*([A-Za-z_]\w*)\s*@""")
    private val ALIAS_CALL_REGEX = Regex("""\bval\s+([A-Za-z_]\w*)\s*=\s*([A-Za-z_][\w.]*)\s*\(""")
    private val FIELD_WRITE_REGEX = Regex("""\.\s*([A-Za-z_]\w*)\s*(\+=|-=|=(?!=))""")
    private val CREATE_STMT_REGEX = Regex("""\bcreate\s+([A-Za-z_]\w*)\s*\(""")
    private val UPDATE_KEYWORD_REGEX = Regex("""\bupdate\b""")
    private val CREATE_ARG_REGEX = Regex("""([A-Za-z_]\w*)\s*=(?!=)\s*([^,]+)""")

    /**
     * Every `update`/`create` field write in a (masked) body, with the target
     * entity resolved through the three shapes real code uses: direct
     * (`update wallet @ {...} (...)`), select-into-local
     * (`val w = wallet @ {...}; update w (...)`), and helper-returned
     * (`val t = get_or_create_treasury(); update t (...)`).
     */
    internal fun valueWrites(
        body: String,
        entities: Set<String>,
        helperReturns: Map<String, String>
    ): List<ValueWrite> {
        val alias = mutableMapOf<String, String>()
        ALIAS_AT_REGEX.findAll(body).forEach { m ->
            if (m.groupValues[2] in entities) alias[m.groupValues[1]] = m.groupValues[2]
        }
        ALIAS_CALL_REGEX.findAll(body).forEach { m ->
            helperReturns[m.groupValues[2].substringAfterLast('.')]?.let { alias[m.groupValues[1]] = it }
        }
        val writes = mutableListOf<ValueWrite>()
        UPDATE_KEYWORD_REGEX.findAll(body).forEach { m ->
            val end = body.indexOf(';', m.range.first).let { if (it < 0) body.length else it }
            val stmt = body.substring(m.range.last + 1, end)
            val head = stmt.takeWhile { it !in "@({" }.trim()
            val rest = stmt.dropWhile { it !in "@({" }
            val entity: String? = when {
                rest.startsWith("@") -> head.takeIf { it in entities }
                head.isNotEmpty() -> alias[head]
                rest.startsWith("(") -> rest.substring(1).substringBefore(')').trim()
                    .let { alias[it] ?: it.takeIf { t -> t in entities } }
                else -> null
            }
            FIELD_WRITE_REGEX.findAll(stmt).forEach { f ->
                writes.add(ValueWrite(entity, f.groupValues[1], f.groupValues[2]))
            }
        }
        CREATE_STMT_REGEX.findAll(body).forEach { m ->
            val entityName = m.groupValues[1]
            if (entityName !in entities) return@forEach
            val parenStart = body.indexOf('(', m.range.first)
            val parenEnd = matchDelimiter(body, parenStart, '(', ')') ?: return@forEach
            CREATE_ARG_REGEX.findAll(body.substring(parenStart + 1, parenEnd)).forEach { a ->
                if (a.groupValues[2].trim() != "0") {
                    writes.add(ValueWrite(entityName, a.groupValues[1], "create"))
                }
            }
        }
        return writes
    }

    /**
     * A read of a price/rate field: `price` may appear anywhere in the name,
     * `rate` must be a whole underscore-separated token (fee_rate yes,
     * generated/migrated no). Assignments (`.price = x`) are writes, not reads.
     */
    private val PRICE_STATE_READ_REGEX = Regex(
        """\.\s*(?:\w*price\w*|(?:[a-z0-9]+_)*rates?(?:_[a-z0-9]+)*)(?!\w)(?!\s*=(?!=))""",
        RegexOption.IGNORE_CASE
    )
    private val CHAIN_ARGS_REF_REGEX = Regex("""\bchain_context\s*\.\s*args\s*\.\s*\w+""")

    /** `a * b` / `a / b` - real arithmetic, not `@*` or comment residue. */
    private val ARITHMETIC_REGEX = Regex("""[\w)\]]\s*[*/]\s*[\w(]""")

    /** Field names that hold value (mirrors VALUE_MUTATION_REGEX's name list). */
    private val VALUE_FIELD_NAME_REGEX = Regex(
        """balance|amount|credit|fund|supply|share|debt|stake|reward|coin|token""",
        RegexOption.IGNORE_CASE
    )

    // ---- statement-level data flow over a flattened body ----
    // The conversion/emission rules need to know, per write, WHAT amount moves
    // and where it came from: a credit is backed when the same quantity (or a
    // quantity derived from the same price/time term) is debited in the same
    // operation, whatever the rows are called. [flattenHelpers] puts helper
    // statements in the operation's own statement list first, so none of this
    // depends on where the code was written.

    internal enum class Flow { CREDIT, DEBIT, SET }

    /**
     * One field write with the amount expression it moves. [flow] normalises
     * the spelling: `+= x`, `= old + x`, `-= -x` and `create e(f = x)` are
     * CREDITs of x; `-= x`, `= old - x`, `+= -x` are DEBITs of x; a plain
     * `= x` with no top-level +/- is a SET. [create] marks `create` arguments.
     */
    internal data class AmountWrite(
        val entity: String?,
        val field: String,
        val flow: Flow,
        val amount: String,
        val create: Boolean = false
    )

    private val WS_REGEX = Regex("""\s+""")
    private val LOCAL_BINDING_REGEX = Regex(
        """\b(?:val|var)\s+([A-Za-z_]\w*)\s*(?::[^=;]*)?=(?!=)(.*)$""", RegexOption.DOT_MATCHES_ALL
    )
    private val BARE_ASSIGN_REGEX = Regex(
        """(?:^|[{};)])\s*([A-Za-z_]\w*)\s*(\+=|-=|=(?!=))(.*)$""", RegexOption.DOT_MATCHES_ALL
    )
    private val DOTTED_ASSIGN_REGEX = Regex(
        """(?:^|[{};)])\s*([A-Za-z_]\w*(?:\s*\.\s*[A-Za-z_]\w*)+)\s*(\+=|-=|=(?!=))(.*)$""", RegexOption.DOT_MATCHES_ALL
    )
    private val SET_ITEM_REGEX = Regex("""^\s*\.\s*([A-Za-z_]\w*)\s*(\+=|-=|=(?!=))(.*)$""", RegexOption.DOT_MATCHES_ALL)
    private val CREATE_ITEM_REGEX = Regex("""^\s*([A-Za-z_]\w*)\s*=(?!=)(.*)$""", RegexOption.DOT_MATCHES_ALL)
    private val FOR_ALIAS_REGEX = Regex("""\bfor\s*\(\s*([A-Za-z_]\w*)\s+in\s+([A-Za-z_]\w*)\s*@""")
    private val REQUIRE_ALIAS_REGEX = Regex("""\bval\s+([A-Za-z_]\w*)\s*=\s*require\s*\(\s*([A-Za-z_]\w*)\s*@""")
    private val REF_TOKEN_REGEX = Regex("""[A-Za-z_]\w*(?:\s*\.\s*[A-Za-z_]\w*)*""")

    /** Identifiers and dotted paths referenced in [expr], with every dotted prefix (`a.b.c` -> a, a.b, a.b.c). */
    internal fun refsOf(expr: String): Set<String> {
        val out = mutableSetOf<String>()
        REF_TOKEN_REGEX.findAll(expr).forEach { m ->
            val parts = m.value.split('.').map { it.trim() }
            for (i in 1..parts.size) out.add(parts.subList(0, i).joinToString("."))
        }
        return out
    }

    private fun statementsOf(body: String): List<String> = body.split(';')

    /** `update <target> [@ {...}] ( .f op e, ... )` -> the raw target text and its (field, op, rhs) items. */
    private fun updateSetList(stmt: String): Pair<String, List<Triple<String, String, String>>>? {
        val m = UPDATE_KEYWORD_REGEX.find(stmt) ?: return null
        val rest = stmt.substring(m.range.last + 1)
        var brace = 0
        var parenStart = -1
        for ((i, c) in rest.withIndex()) {
            when (c) {
                '{' -> brace++
                '}' -> brace--
                '(' -> if (brace == 0) { parenStart = i; break }
            }
        }
        if (parenStart < 0) return null
        val parenEnd = matchDelimiter(rest, parenStart, '(', ')') ?: return null
        val items = splitArgs(rest.substring(parenStart + 1, parenEnd)).mapNotNull { item ->
            SET_ITEM_REGEX.find(item)?.let { Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }
        }
        return rest.substring(0, parenStart).trim() to items
    }

    /** The update target's local name (`m` in `update m (...)`, `wallet` in `update wallet @ {...} (...)`). */
    private fun updateTargetName(head: String): String = head.takeWhile { !it.isWhitespace() && it !in "@({" }

    /** Local -> entity, through `val x = e @`, `val x = require(e @`, `val x = helper_returning_e(`, `for (x in e @`. */
    private fun aliasMap(body: String, entities: Set<String>, helperReturns: Map<String, String>): Map<String, String> {
        val alias = mutableMapOf<String, String>()
        ALIAS_AT_REGEX.findAll(body).forEach { m ->
            if (m.groupValues[2] in entities) alias[m.groupValues[1]] = m.groupValues[2]
        }
        REQUIRE_ALIAS_REGEX.findAll(body).forEach { m ->
            if (m.groupValues[2] in entities) alias[m.groupValues[1]] = m.groupValues[2]
        }
        ALIAS_CALL_REGEX.findAll(body).forEach { m ->
            helperReturns[m.groupValues[2].substringAfterLast('.')]?.let { alias[m.groupValues[1]] = it }
        }
        FOR_ALIAS_REGEX.findAll(body).forEach { m ->
            if (m.groupValues[2] in entities) alias[m.groupValues[1]] = m.groupValues[2]
        }
        return alias
    }

    /** Index of the first top-level binary [op] in [expr] (outside brackets, not unary, not `->`/`+=`), or -1. */
    private fun topLevelOperator(expr: String, op: Char): Int {
        var depth = 0
        var prev = ' '
        for ((i, c) in expr.withIndex()) {
            when (c) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                op -> if (depth == 0) {
                    val next = expr.getOrNull(i + 1) ?: ' '
                    val binary = prev.isLetterOrDigit() || prev == '_' || prev == ')' || prev == ']'
                    if (binary && next != '=' && next != '>') return i
                }
            }
            if (!c.isWhitespace()) prev = c
        }
        return -1
    }

    private fun flowOf(op: String, rhsRaw: String): Pair<Flow, String> {
        val rhs = rhsRaw.trim()
        return when (op) {
            "+=" -> if (rhs.startsWith("-")) Flow.DEBIT to rhs.drop(1).trim() else Flow.CREDIT to rhs
            "-=" -> if (rhs.startsWith("-")) Flow.CREDIT to rhs.drop(1).trim() else Flow.DEBIT to rhs
            else -> {
                val plus = topLevelOperator(rhs, '+')
                val minus = topLevelOperator(rhs, '-')
                when {
                    plus >= 0 && (minus < 0 || plus < minus) -> Flow.CREDIT to rhs.substring(plus + 1).trim()
                    minus >= 0 -> Flow.DEBIT to rhs.substring(minus + 1).trim()
                    else -> Flow.SET to rhs
                }
            }
        }
    }

    /**
     * Every field write in a (flattened, masked) body with its amount: update
     * set-lists (target resolved as in [valueWrites], plus `require(e @`, and
     * `for (x in e @` loop variables), object/dotted assignments
     * (`pool.undistributed -= earned`), and non-zero `create` arguments.
     */
    internal fun amountWrites(body: String, entities: Set<String>, helperReturns: Map<String, String>): List<AmountWrite> {
        val alias = aliasMap(body, entities, helperReturns)
        val out = mutableListOf<AmountWrite>()
        statementsOf(body).forEach { stmt ->
            updateSetList(stmt)?.let { (head, items) ->
                val name = updateTargetName(head)
                val entity = when {
                    name.isEmpty() -> null
                    head.substring(name.length).trimStart().startsWith("@") -> name.takeIf { it in entities }
                    else -> alias[name] ?: name.takeIf { it in entities }
                }
                items.forEach { (field, op, rhs) ->
                    val (flow, amount) = flowOf(op, rhs)
                    out.add(AmountWrite(entity, field, flow, amount))
                }
            }
            DOTTED_ASSIGN_REGEX.find(stmt)?.let { m ->
                val path = m.groupValues[1].replace(WS_REGEX, "")
                val base = path.substringBeforeLast('.')
                val (flow, amount) = flowOf(m.groupValues[2], m.groupValues[3])
                out.add(AmountWrite(alias[base] ?: base.takeIf { it in entities }, path.substringAfterLast('.'), flow, amount))
            }
        }
        CREATE_STMT_REGEX.findAll(body).forEach { m ->
            val entityName = m.groupValues[1]
            if (entityName !in entities) return@forEach
            val parenStart = body.indexOf('(', m.range.first)
            val parenEnd = matchDelimiter(body, parenStart, '(', ')') ?: return@forEach
            splitArgs(body.substring(parenStart + 1, parenEnd)).forEach { arg ->
                CREATE_ITEM_REGEX.find(arg)?.let { a ->
                    val rhs = a.groupValues[2].trim()
                    if (rhs != "0") out.add(AmountWrite(entityName, a.groupValues[1], Flow.CREDIT, rhs, create = true))
                }
            }
        }
        return out
    }

    /**
     * Every binding in a (flattened) body: `val x = e`, `x op= e`, `a.b op= e`
     * (keyed `a.b`) and `update t ( .f op= e )` (keyed `t.f`). A name assigned
     * more than once keeps every right-hand side.
     */
    internal fun bindingsOf(body: String): Map<String, List<String>> {
        val out = mutableMapOf<String, MutableList<String>>()
        fun add(name: String, rhs: String) = out.getOrPut(name) { mutableListOf() }.add(rhs)
        statementsOf(body).forEach { stmt ->
            LOCAL_BINDING_REGEX.find(stmt)?.let { add(it.groupValues[1], it.groupValues[2]); return@forEach }
            DOTTED_ASSIGN_REGEX.find(stmt)?.let { add(it.groupValues[1].replace(WS_REGEX, ""), it.groupValues[3]); return@forEach }
            BARE_ASSIGN_REGEX.find(stmt)?.let { add(it.groupValues[1], it.groupValues[3]); return@forEach }
            updateSetList(stmt)?.let { (head, items) ->
                val name = updateTargetName(head)
                if (name.isNotEmpty()) items.forEach { (field, _, rhs) -> add("$name.$field", rhs) }
            }
        }
        return out
    }

    /** [expr]'s references closed over [bindings]: everything the expression's value was computed from. */
    internal fun refClosure(expr: String, bindings: Map<String, List<String>>): Set<String> {
        val seen = mutableSetOf<String>()
        val work = ArrayDeque(refsOf(expr))
        while (work.isNotEmpty()) {
            val n = work.removeFirst()
            if (!seen.add(n)) continue
            bindings[n]?.forEach { rhs -> refsOf(rhs).forEach { if (it !in seen) work.add(it) } }
        }
        return seen
    }

    /** [seeds] plus every binding whose value matches [source] or references a derived name (to a fixpoint). */
    internal fun derivedNames(bindings: Map<String, List<String>>, seeds: Set<String>, source: Regex?): Set<String> {
        val derived = seeds.toMutableSet()
        var changed = true
        while (changed) {
            changed = false
            bindings.forEach { (name, values) ->
                if (name in derived) return@forEach
                val hit = values.any { v -> (source != null && source.containsMatchIn(v)) || refsOf(v).any { it in derived } }
                if (hit) { derived.add(name); changed = true }
            }
        }
        return derived
    }

    /** The derived names whose value went through `*` or `/` (directly or via another scaled name). */
    private fun scaledNames(bindings: Map<String, List<String>>, derived: Set<String>): Set<String> {
        val scaled = mutableSetOf<String>()
        var changed = true
        while (changed) {
            changed = false
            bindings.forEach { (name, values) ->
                if (name in scaled || name !in derived) return@forEach
                val hit = values.any { v -> ARITHMETIC_REGEX.containsMatchIn(v) || refsOf(v).any { it in scaled } }
                if (hit) { scaled.add(name); changed = true }
            }
        }
        return scaled
    }

    /** A price read: a price/rate state field, or a call to a helper that reads one. */
    private fun priceSourceRegex(priceReadFunctions: Set<String>): Regex {
        val calls = if (priceReadFunctions.isEmpty()) "" else
            "|\\b(?:" + priceReadFunctions.joinToString("|") { Regex.escape(it) } + ")\\s*\\("
        return Regex("(?i:${PRICE_STATE_READ_REGEX.pattern})$calls")
    }

    /** A read (not a write, not a call) of any of [fields]. */
    private fun fieldReadRegex(fields: Set<String>): Regex? =
        if (fields.isEmpty()) null else
            Regex("""\.\s*(?:${fields.joinToString("|") { Regex.escape(it) }})\b(?!\s*(?:\(|=(?!=)|\+=|-=))""")

    /**
     * Field names that hold a PRICE-DERIVED QUOTE: written by `create`/`=`
     * somewhere in the app from a scaled price computation, and never debited
     * anywhere (a balance that is debited elsewhere is an account, not a
     * quote). `redemption.cash_due = tokens_in * current_price() / SCALE` is
     * the shape: the conversion happens when the row is written, so the
     * operation that later pays the quote out reads no price at all.
     */
    /**
     * Field names that are one side of a TWO-SIDED RECORD OF ONE OBLIGATION -
     * a liability counter, not somebody's spendable value.
     *
     * A field F qualifies when some other field G moves with it in LOCKSTEP
     * across the WHOLE module: every non-zero, non-create write of F has a
     * write of G in the same body, in the same direction, with the identical
     * amount expression, and vice versa - and the pair moves in BOTH
     * directions somewhere (an up-only pair is an accumulator, not an
     * obligation).
     *
     * `loan.principal += interest` next to `pool.total_debt += interest` in
     * the accrual helper, and `loan.principal -= paid` next to
     * `pool.total_debt -= paid` in repay: raising the pair makes the borrower
     * owe MORE and discharging it costs them cash, so nothing spendable is
     * created. Adversary round 6 fired unbacked-conversion-credit on three
     * operations of one lending pool for exactly this, prescribing "debit a
     * reward pool before crediting the staker" - which would make the pool pay
     * for its own interest income.
     *
     * Keyed on USE, never on names. A spendable balance cannot qualify: a
     * transfer moves it on its own (`from.balance -= x; to.balance += x` is
     * opposite directions, not lockstep), which breaks the set equality for
     * every candidate partner. Nor can a mint plus a statistics counter - the
     * counter is never debited alongside the balance, and the balance is spent
     * without the counter.
     */
    internal fun mirroredCounterFields(
        fullyMasked: Map<String, String>,
        entities: Set<String>,
        helperReturns: Map<String, String>
    ): Set<Pair<String?, String>> {
        // Keyed on (owning entity, field), never the bare name: two distinct
        // fields that happen to share a name collapsed into one key, and the
        // genuine lockstep pair became invisible to the rule that exists to see
        // it. Found live on the lending template, whose loan.scaled_debt /
        // pool.scaled_debt pair drew a false positive until one side was
        // renamed - a workaround, not a fix.
        val occurrences = mutableMapOf<Pair<String?, String>, MutableSet<Triple<Int, Flow, String>>>()
        // A receiver is not always resolvable, and keying a null owner as its own
        // partition would SPLIT one field's moves across two keys - the credit in
        // one, the debit in the other - so the pair check finds neither. When a
        // field name is declared on exactly one entity, attribute it there.
        val soleOwner = entityFields(fullyMasked)
            .groupBy { it.name }
            .filterValues { it.map { f -> f.entity }.distinct().size == 1 }
            .mapValues { (_, v) -> v.first().entity }
        var bodyIndex = 0
        fullyMasked.forEach { (path, masked) ->
            if (RellLibs.isVendoredLibraryPath(path) || RellLibs.isThirdPartyLibPath(path)) return@forEach
            val bodies = scanOperations(path, masked).map { it.body } + functionBodies(masked).map { it.second }
            bodies.forEach { body ->
                val idx = bodyIndex++
                amountWrites(body, entities, helperReturns).forEach { w ->
                    if (w.create || w.flow == Flow.SET) return@forEach
                    val amount = w.amount.replace(WS_REGEX, "")
                    if (amount.isEmpty() || amount == "0") return@forEach
                    val owner = w.entity ?: soleOwner[w.field]
                    occurrences.getOrPut(owner to w.field) { mutableSetOf() }.add(Triple(idx, w.flow, amount))
                }
            }
        }
        val out = mutableSetOf<Pair<String?, String>>()
        occurrences.forEach { (key, moves) ->
            if (moves.none { it.second == Flow.CREDIT } || moves.none { it.second == Flow.DEBIT }) return@forEach
            if (occurrences.any { (other, otherMoves) -> other != key && otherMoves == moves }) out.add(key)
        }
        return out
    }

    internal fun priceDerivedStoredFields(
        fullyMasked: Map<String, String>,
        entities: Set<String>,
        helperReturns: Map<String, String>,
        priceReadFunctions: Set<String>
    ): Set<String> {
        val source = priceSourceRegex(priceReadFunctions)
        val derived = mutableSetOf<String>()
        val debited = mutableSetOf<String>()
        fullyMasked.forEach { (path, masked) ->
            if (RellLibs.isVendoredLibraryPath(path) || RellLibs.isThirdPartyLibPath(path)) return@forEach
            val bodies = scanOperations(path, masked).map { it.body } + functionBodies(masked).map { it.second }
            bodies.forEach { raw ->
                val body = CHAIN_ARGS_REF_REGEX.replace(raw, " ")
                val writes = amountWrites(body, entities, helperReturns)
                writes.filter { it.flow == Flow.DEBIT }.forEach { debited.add(it.field) }
                if (!source.containsMatchIn(body)) return@forEach
                val bindings = bindingsOf(body)
                val tainted = derivedNames(bindings, emptySet(), source)
                val scaled = scaledNames(bindings, tainted)
                writes.filter { it.create || it.flow == Flow.SET }.forEach { w ->
                    val direct = source.containsMatchIn(w.amount) && ARITHMETIC_REGEX.containsMatchIn(w.amount)
                    if (direct || refClosure(w.amount, bindings).any { it in scaled }) derived.add(w.field)
                }
            }
        }
        return derived - debited
    }

    /** `delete l` / `delete listing @ {...}` - the local or entity whose row is destroyed. */
    private val DELETE_TARGET_REGEX = Regex("""\bdelete\s+([A-Za-z_]\w*)""")

    private val TIME_SOURCE_REGEX = Regex("""\bop_context\s*\.\s*(?:last_block_time|block_height)\b|\bblock\s*@""")
    private val TIME_SOURCE_NAMES = setOf("op_context.last_block_time", "op_context.block_height", "block")

    /**
     * `(entity, field)` pairs that hold an amount ALREADY DEBITED elsewhere:
     * some operation debits a value field by an amount and, in the same
     * operation, creates a row of that entity carrying the same amount
     * expression in that field. `update p ( .unclaimed -= amount ); create
     * vesting_grant(total = amount)` - the grant's `total` is escrowed value,
     * not a promise, so a payout bounded by it mints nothing.
     */
    internal fun escrowedCapFields(
        fullyMasked: Map<String, String>,
        entities: Set<String>,
        helperReturns: Map<String, String>
    ): Set<Pair<String, String>> {
        val out = mutableSetOf<Pair<String, String>>()
        fullyMasked.forEach { (path, masked) ->
            if (RellLibs.isVendoredLibraryPath(path) || RellLibs.isThirdPartyLibPath(path)) return@forEach
            val bodies = scanOperations(path, masked).map { it.body } + functionBodies(masked).map { it.second }
            bodies.forEach { body ->
                val writes = amountWrites(body, entities, helperReturns)
                val debited = writes.filter { it.flow == Flow.DEBIT }
                    .map { it.amount.replace(WS_REGEX, "") }
                    .filter { it.isNotEmpty() && it != "0" }
                    .toSet()
                if (debited.isEmpty()) return@forEach
                writes.filter { it.create && it.entity != null }.forEach { w ->
                    if (w.amount.replace(WS_REGEX, "") in debited) out.add(w.entity!! to w.field)
                }
            }
        }
        return out
    }

    /**
     * Field names that appear as a SUBTRAHEND anywhere in [expr]'s closure -
     * the quantities that make the value smaller. `claimable = vested -
     * g.released` puts `released` here: crediting `released` reduces what the
     * next call can pay, which is what makes a `+=` a debit.
     */
    private fun subtrahendFields(expr: String, bindings: Map<String, List<String>>): Set<String> {
        val out = mutableSetOf<String>()
        fun scan(text: String) {
            var rest = text
            while (true) {
                val i = topLevelOperator(rest, '-')
                if (i < 0) break
                refsOf(rest.substring(i + 1)).forEach { out.add(it.substringAfterLast('.')) }
                rest = rest.substring(i + 1)
            }
        }
        scan(expr)
        refClosure(expr, bindings).forEach { name -> bindings[name]?.forEach { scan(it) } }
        return out
    }

    /**
     * MEDIUM, one finding per operation, when a value field is credited with
     * an amount that comes from nowhere - three shapes of the same mint:
     *
     *  1. PRICE: the operation reads a mutable price/rate (entity field,
     *     directly or via a helper), does arithmetic, and credits a value
     *     field of an entity it never debits. The adversary oracle vault
     *     turned 100 USD into 200,000,000 this way. Config rates
     *     (chain_context.args.fee_bps) do not count - the exploit needs a
     *     MUTABLE price.
     *  2. STORED QUOTE: the credit is paid from a row field that was itself
     *     computed at a price when the row was written (a redemption's
     *     cash_due) - the conversion and the payout are split across two
     *     operations, so the paying operation reads no price.
     *  3. TIME: the credit is derived from elapsed block time (or height)
     *     scaled by a rate or a stake - `staked * (now - since) * RATE` - and
     *     no debit in the operation moves a quantity derived from the same
     *     elapsed term. Rewards are minted from nothing: with an empty or
     *     absent pool a staker's balance grows without bound (adversary
     *     round 4 drained from an empty pool).
     *
     * A credit is BACKED - and stays quiet - when the same operation debits
     * the same row type (shape 1), debits exactly the same amount expression
     * (an escrow record: `.tokens -= tokens_in` then
     * `create redemption(tokens_locked = tokens_in)`), or debits a quantity
     * computed from the same price/time term (`pool.undistributed -= earned`
     * then `acc_reward_per_share += earned * SCALE / total`). Helpers are
     * flattened into the operation first, so where the math or the payout
     * lives does not matter. Advisory, never blocking: conservation is not
     * decidable from syntax (an intentional emission backed off-op has the
     * same shape) - only the designer knows whether a reserve exists.
     */
    private fun unbackedConversionFindings(
        path: String,
        op: OperationBlock,
        entities: Set<String>,
        helperReturns: Map<String, String>,
        priceReadFunctions: Set<String>,
        priceDerivedFields: Set<String>,
        helpers: Map<String, List<FunctionDef>>,
        escrowedCaps: Set<Pair<String, String>>,
        mirroredCounters: Set<Pair<String?, String>>
    ): List<Finding> {
        val flat = CHAIN_ARGS_REF_REGEX.replace(flattenHelpers(op.body, helpers, entities), " ")
        val bindings = bindingsOf(flat)
        val writes = amountWrites(flat, entities, helperReturns)
        val debits = writes.filter { it.flow == Flow.DEBIT }
        val debitedEntities = debits.mapNotNull { it.entity }.toSet()
        val debitAmounts = debits.map { it.amount.replace(WS_REGEX, "") }.toSet()
        val debitClosures = debits.map { refClosure(it.amount, bindings) }
        // A liability counter is not a payout: raising `pool.total_debt` (which
        // moves in lockstep with `loan.principal`) makes the borrower owe more,
        // and there is nothing to fund or cap. See [mirroredCounterFields].
        val valueCredits = writes.filter { w ->
            w.flow != Flow.DEBIT && VALUE_FIELD_NAME_REGEX.containsMatchIn(w.field) &&
                (w.entity to w.field) !in mirroredCounters && w.amount.replace(WS_REGEX, "") != "0"
        }
        fun backedByEntity(c: AmountWrite) = c.entity != null && c.entity in debitedEntities
        fun backedByAmount(c: AmountWrite) = c.amount.replace(WS_REGEX, "") in debitAmounts
        fun backedByFlow(c: AmountWrite, derived: Set<String>): Boolean {
            val cc = refClosure(c.amount, bindings)
            return debitClosures.any { d -> d.any { it in cc && it in derived } }
        }
        fun where(c: AmountWrite) = if (c.entity != null) "${c.entity}.${c.field}" else c.field
        // A released-so-far counter IS the paired debit. The vesting shape:
        // `update g ( .released += claimable )` next to `update m ( .balance +=
        // claimable )`, where `claimable = vested - g.released` and the row's
        // cap was itself debited from a funded pool when the grant was created.
        // Crediting the counter strictly reduces what the next call can pay, so
        // the payout is bounded by escrowed value and mints nothing - the rule
        // used to demand a `-=` and called this a mint (adversary round 5).
        fun backedByEscrowedRelease(c: AmountWrite): Boolean {
            val amount = c.amount.replace(WS_REGEX, "")
            if (amount.isEmpty() || amount == "0") return false
            val subtrahends = subtrahendFields(c.amount, bindings)
            val closure = refClosure(c.amount, bindings)
            return writes.any { w ->
                w.flow == Flow.CREDIT && !w.create && w.entity != null && w.entity != c.entity &&
                    w.amount.replace(WS_REGEX, "") == amount &&
                    w.field in subtrahends &&
                    escrowedCaps.any { (entity, cap) ->
                        entity == w.entity && cap != w.field && closure.any { it.endsWith(".$cap") }
                    }
            }
        }
        // AN ESCROW RELEASED BY DELETION IS STILL A DEBIT. The marketplace
        // template's own discipline - a row that already holds debited value is
        // destroyed in the operation that pays it out - has no compound
        // assignment anywhere: `delete l; update owner ( .balance += earned )`,
        // where `earned` is computed from `l.escrowed`. That row's cap was
        // debited from the tenant when it was created (escrowedCaps), and
        // deleting it is what makes the payout happen exactly once, so this
        // mints nothing. One step past backedByEscrowedRelease, where the debit
        // was a released-so-far counter (adversary round 5); round 6 hit the
        // no-counter form on a lease built on template=marketplace.
        val deletedEscrowRows: List<Pair<String, String>> = run {
            val alias = aliasMap(flat, entities, helperReturns)
            DELETE_TARGET_REGEX.findAll(flat).flatMap { m ->
                val local = m.groupValues[1]
                val entity = alias[local] ?: local.takeIf { it in entities }
                if (entity == null) emptySequence()
                else escrowedCaps.asSequence().filter { it.first == entity }.map { local to it.second }
            }.toList()
        }
        fun backedByEscrowedDelete(c: AmountWrite): Boolean {
            if (deletedEscrowRows.isEmpty()) return false
            val closure = refClosure(c.amount, bindings)
            return deletedEscrowRows.any { (local, cap) -> "$local.$cap" in closure }
        }
        val fixTail = "Advisory: conservation cannot be proven statically - if this credit is an " +
            "intentional emission backed elsewhere, document it and ignore this finding."

        // 1. price read in this operation
        val calls = calledNames(flat)
        val readsPrice = PRICE_STATE_READ_REGEX.containsMatchIn(flat) || calls.any { it in priceReadFunctions }
        val priceSource = priceSourceRegex(priceReadFunctions)
        val priceDerived = derivedNames(bindings, emptySet(), priceSource)
        if (readsPrice && ARITHMETIC_REGEX.containsMatchIn(flat)) {
            val credit = valueCredits.firstOrNull { c ->
                c.flow == Flow.CREDIT && c.entity != null &&
                    !backedByEntity(c) && !backedByAmount(c) && !backedByFlow(c, priceDerived)
            }
            if (credit != null) {
                return listOf(
                    Finding(
                        "MEDIUM", "unbacked-conversion-credit", path, op.line,
                        "operation ${op.name} credits ${where(credit)} at a price/rate read from " +
                            "mutable state, but never debits any ${credit.entity} row or the converted " +
                            "amount - the credited value is backed by nothing, so a transient oracle price " +
                            "mints unbacked balance (100 -> 200,000,000 in the adversary corpus)",
                        "Make the conversion conserve value: pay the credit out of a reserve/vault row of the " +
                            "same asset (require(reserve.balance >= out) then update reserve ( .balance -= out )), " +
                            "and bound price updates (max move per update, staleness check). $fixTail"
                    )
                )
            }
        }

        // 2. paid from a stored price-derived quote
        val storedSource = fieldReadRegex(priceDerivedFields)
        if (storedSource != null) {
            val storedDerived = derivedNames(bindings, emptySet(), storedSource)
            val credit = valueCredits.firstOrNull { c ->
                c.flow == Flow.CREDIT && c.entity != null &&
                    (storedSource.containsMatchIn(c.amount) || refClosure(c.amount, bindings).any { it in storedDerived }) &&
                    !backedByEntity(c) && !backedByAmount(c) && !backedByFlow(c, storedDerived + priceDerived)
            }
            if (credit != null) {
                val quote = refClosure(credit.amount, bindings)
                    .firstOrNull { ref -> priceDerivedFields.any { f -> ref.endsWith(".$f") } }
                    ?: "a stored quote"
                return listOf(
                    Finding(
                        "MEDIUM", "unbacked-conversion-credit", path, op.line,
                        "operation ${op.name} credits ${where(credit)} from $quote, an amount that was " +
                            "computed at a mutable price when that row was written, and never debits any " +
                            "reserve by that amount - the quote is paid from nothing, so the conversion " +
                            "mints unbacked balance one operation later than where the price was read",
                        "Pay the quote out of a reserve row of the credited asset in the same operation " +
                            "(require(reserve.balance >= due) then update reserve ( .balance -= due )), " +
                            "and settle at the current bounded price rather than a stale stored one. $fixTail"
                    )
                )
            }
        }

        // 3. elapsed time x rate
        val timeDerived = derivedNames(bindings, TIME_SOURCE_NAMES, TIME_SOURCE_REGEX)
        if (timeDerived.size > TIME_SOURCE_NAMES.size || TIME_SOURCE_REGEX.containsMatchIn(flat)) {
            val scaled = scaledNames(bindings, timeDerived)
            val credit = valueCredits.firstOrNull { c ->
                val cc = refClosure(c.amount, bindings)
                cc.any { it in timeDerived } &&
                    (ARITHMETIC_REGEX.containsMatchIn(c.amount) || cc.any { it in scaled }) &&
                    !backedByFlow(c, timeDerived) && !backedByEscrowedRelease(c) && !backedByEscrowedDelete(c)
            }
            if (credit != null) {
                return listOf(
                    Finding(
                        "MEDIUM", "unbacked-conversion-credit", path, op.line,
                        "operation ${op.name} credits ${where(credit)} with an amount derived from elapsed " +
                            "block time scaled by a rate/stake, and no debit in the operation moves a quantity " +
                            "from the same elapsed term - the reward is minted from nothing: with an empty " +
                            "or absent pool the balance still grows without bound (adversary round 4 " +
                            "drained a staking dapp from an empty pool this way)",
                        "FIRST decide which side of the ledger this field is on. If it is an OBLIGATION " +
                            "counter - what somebody OWES (a loan's principal, a pool's total_debt) - raising " +
                            "it makes the holder poorer and nothing needs funding: do NOT add a pool debit, " +
                            "which would make the pool pay for its own interest income; instead make the " +
                            "counter move in lockstep with the matching per-row obligation and prove it with " +
                            "a conservation test. If it is SPENDABLE value, fund the emission: keep a reward " +
                            "pool row, cap the emission at what it holds (val paid = min(pool.undistributed, " +
                            "elapsed * RATE)) and debit it in the same operation (update pool " +
                            "( .undistributed -= paid )) before crediting the staker. $fixTail"
                    )
                )
            }
        }
        return emptyList()
    }

    // ---- block-clock-randomness (the chain's clock used as a lottery) ----
    // Adversary round 5 dapp4: a raffle picks its winner with
    // `op_context.last_block_time % ticket_count`. last_block_time is the
    // PREVIOUS block's timestamp - already committed and public when the
    // attacker signs the draw - so the winner is computable in advance, anyone
    // may trigger the draw, and a losing entrant simply waits for a block whose
    // clock names a ticket they hold. Block height has the identical property,
    // and so does any hash of either: there is no per-block secret an operation
    // can reach. Every other guard was present and the gate was silent.
    //
    // The rule keys on TYPE AND USE, never on names. The source is the language
    // construct (`op_context.last_block_time`, `op_context.block_height`,
    // `block @ {...}`) closed over every local it flows into, through helpers,
    // conversions and hashes. The use that matters is SELECTION: the clock
    // reduced by `%`, compared for equality against a row, or indexed with -
    // and then deciding who receives value. A legitimate schedule uses the
    // clock as a BOUND (`require(now >= deadline)`, a staleness check, a
    // cooldown): an inequality that can only abort, never choose. Bounds are
    // untouched by this rule.

    /**
     * Row fields written from a block-clock value anywhere in the app. Parking
     * `op_context.last_block_time` in a row and drawing from the stored copy in
     * a later operation is the same public number one block later, so reads of
     * these fields count as clock reads.
     */
    internal fun clockDerivedStoredFields(
        fullyMasked: Map<String, String>,
        entities: Set<String>,
        helperReturns: Map<String, String>
    ): Set<String> {
        val out = mutableSetOf<String>()
        fullyMasked.forEach { (path, masked) ->
            if (RellLibs.isVendoredLibraryPath(path) || RellLibs.isThirdPartyLibPath(path)) return@forEach
            val bodies = scanOperations(path, masked).map { it.body } + functionBodies(masked).map { it.second }
            bodies.forEach { body ->
                if (!TIME_SOURCE_REGEX.containsMatchIn(body)) return@forEach
                val bindings = bindingsOf(body)
                val clock = derivedNames(bindings, TIME_SOURCE_NAMES, TIME_SOURCE_REGEX)
                amountWrites(body, entities, helperReturns).forEach { w ->
                    val fromClock = TIME_SOURCE_REGEX.containsMatchIn(w.amount) ||
                        refsOf(w.amount).any { it in clock }
                    if (fromClock) out.add(w.field)
                }
            }
        }
        return out
    }

    /** `x[i]` - a subscript, not a `@ {}` block or a type parameter. */
    private val SUBSCRIPT_REGEX = Regex("""[A-Za-z_)\]]\s*\[""")

    /** The operand tokens either side of an `==`/`!=`, in source order. */
    private val EQUALITY_OPERANDS_REGEX = Regex(
        """([A-Za-z_][\w.]*(?:\s*\.\s*\w+\s*\(\s*\))?|\d+)\s*(?:==|!=)\s*([A-Za-z_][\w.]*(?:\s*\.\s*\w+\s*\(\s*\))?|\d+)"""
    )

    /**
     * True when [expr] uses a block-clock value to SELECT rather than to bound:
     * reduced modulo something, compared for equality against a non-literal, or
     * used as a subscript. An inequality against the clock (a deadline, a
     * staleness bound, a cooldown) is deliberately not a selector.
     */
    private fun clockSelectorUse(expr: String, clockNames: Set<String>, clockSource: Regex): Boolean {
        fun isClock(token: String): Boolean {
            val t = token.replace(WS_REGEX, "")
            return t in clockNames || clockSource.containsMatchIn(t)
        }
        val refsClock = refsOf(expr).any { it in clockNames } || clockSource.containsMatchIn(expr)
        if (!refsClock) return false
        if (expr.contains('%')) return true
        if (SUBSCRIPT_REGEX.containsMatchIn(expr)) return true
        return EQUALITY_OPERANDS_REGEX.findAll(expr).any { m ->
            val (l, r) = m.groupValues[1] to m.groupValues[2]
            // `x == 0` is an initialisation test, not a row selection.
            val literal = l.toLongOrNull() != null || r.toLongOrNull() != null
            !literal && (isClock(l) || isClock(r))
        }
    }

    /**
     * HIGH, one finding per operation, when a block-clock value selects who
     * receives value: the update target of a value credit (or an identity
     * field the operation writes) traces back to a clock value used as a
     * selector. Not an advisory - the loser who waits for a favourable block
     * takes the pot, which is a drain with a running proof of concept.
     */
    private fun blockClockRandomnessFindings(
        path: String,
        op: OperationBlock,
        entities: Set<String>,
        helperReturns: Map<String, String>,
        helpers: Map<String, List<FunctionDef>>,
        identityFields: Set<String>,
        storedClockFields: Set<String>
    ): List<Finding> {
        val flat = flattenHelpers(op.body, helpers, entities)
        // A clock parked in a row by one operation and read back by another is
        // the same public value one block later, so a read of such a field is a
        // clock source too (evasion E8: `update r ( .seed = last_block_time )`
        // in `seal`, `r.seed % ticket_count` in `draw`).
        val storedRead = fieldReadRegex(storedClockFields)
        val clockSource = if (storedRead == null) TIME_SOURCE_REGEX else
            Regex("(?:${TIME_SOURCE_REGEX.pattern})|(?:${storedRead.pattern})")
        if (!clockSource.containsMatchIn(flat)) return emptyList()
        val bindings = bindingsOf(flat)
        val clockNames = derivedNames(bindings, TIME_SOURCE_NAMES, clockSource)
        // Seeds: locals whose VALUE came out of a selector use of the clock.
        // Everything computed from a seed inherits it, so routing the draw
        // through an intermediate val, a helper or a hash changes nothing.
        val seeds = bindings.filterValues { rhs -> rhs.any { clockSelectorUse(it, clockNames, clockSource) } }.keys
        if (seeds.isEmpty() && !statementsOf(flat).any { clockSelectorUse(it, clockNames, clockSource) }) return emptyList()
        val selectors = derivedNames(bindings, seeds, null)

        fun selected(text: String): Boolean =
            refClosure(text, bindings).any { it in selectors } || clockSelectorUse(text, clockNames, clockSource)

        var beneficiary: String? = null
        var how: String? = null
        // A payout can also be gated on the draw instead of flowing from it:
        // `require(t.holder == account.id)` where `t` is the clock-selected row
        // lets the caller collect only when the clock named them. The row the
        // credit updates is the caller's own, so the target trace above sees
        // nothing - the guard is what the clock decides (evasion E10).
        var identityGuard = false
        var valueCredited = false
        statementsOf(flat).forEach { stmt ->
            if (beneficiary != null) return@forEach
            updateSetList(stmt)?.let { (head, items) ->
                val target = updateTargetName(head)
                // The row that receives the value: a local carrying the drawn
                // row, or the where-clause that picked it.
                // `if (now % 2 == 0) { update alice (...)` puts the draw in the
                // same `;`-fragment as the payout, so the statement counts too.
                val targetSelected = selected(target) || selected(head) || clockSelectorUse(stmt, clockNames, clockSource)
                items.forEach { (field, opText, rhs) ->
                    if (beneficiary != null) return@forEach
                    val (flow, _) = flowOf(opText, rhs)
                    if (flow == Flow.CREDIT && VALUE_FIELD_NAME_REGEX.containsMatchIn(field)) valueCredited = true
                    if (flow == Flow.CREDIT && VALUE_FIELD_NAME_REGEX.containsMatchIn(field) && targetSelected) {
                        beneficiary = if (target.isNotEmpty()) "$target.$field" else field
                        how = "the row credited with $field"
                    } else if (field in identityFields && selected(rhs)) {
                        beneficiary = field
                        how = "the identity stored in $field"
                    }
                }
            }
            EQUALITY_OPERANDS_REGEX.findAll(stmt).forEach { eq ->
                val sides = listOf(eq.groupValues[1], eq.groupValues[2])
                val fromDraw = sides.any { refClosure(it, bindings).any { r -> r in selectors } }
                val onIdentity = sides.any { it.substringAfterLast('.') in identityFields }
                if (fromDraw && onIdentity) identityGuard = true
            }
        }
        if (beneficiary == null && identityGuard && valueCredited) {
            beneficiary = "who may collect"
            how = "the identity the payout is gated on"
        }
        if (beneficiary == null) {
            // `create winner_record(holder = <drawn>)` - recording the draw now
            // and paying it out later is the same lottery.
            CREATE_STMT_REGEX.findAll(flat).forEach { m ->
                if (beneficiary != null) return@forEach
                if (m.groupValues[1] !in entities) return@forEach
                val parenStart = flat.indexOf('(', m.range.first)
                val parenEnd = matchDelimiter(flat, parenStart, '(', ')') ?: return@forEach
                splitArgs(flat.substring(parenStart + 1, parenEnd)).forEach { arg ->
                    if (beneficiary != null) return@forEach
                    CREATE_ITEM_REGEX.find(arg)?.let { a ->
                        if (a.groupValues[1] in identityFields && selected(a.groupValues[2])) {
                            beneficiary = "${m.groupValues[1]}.${a.groupValues[1]}"
                            how = "the identity stored in ${a.groupValues[1]}"
                        }
                    }
                }
            }
        }
        val target = beneficiary ?: return emptyList()
        return listOf(
            Finding(
                "HIGH", "block-clock-randomness", path, op.line,
                "operation ${op.name} decides $target from the block clock " +
                    "(op_context.last_block_time / block_height, or a value derived from one) used as a " +
                    "SELECTOR - ${how!!} is picked by it. That value is the previous block's, already " +
                    "committed and public when the transaction is signed, so the outcome is computable " +
                    "in advance: anyone who may trigger this operation waits for a block whose clock " +
                    "names them and takes the value on demand (adversary round 5 drained a raffle pot " +
                    "this way). Hashing the clock, using height instead of time, or routing it through a " +
                    "helper does not change this - there is no per-block secret an operation can read",
                "Do not derive an outcome from the block clock. Use commit-reveal (every entrant commits " +
                    "hash(secret) before the close, reveals after it, and the seed is the XOR of the " +
                    "revealed secrets, with a forfeit for not revealing), or an external randomness " +
                    "beacon delivered by a signed oracle operation. If the value is meant to be first-" +
                    "come-first-served rather than random, select the recipient explicitly and say so. " +
                    "The block clock is legitimate as a BOUND (require(op_context.last_block_time >= " +
                    "deadline)) - only never as the thing that picks the winner."
            )
        )
    }

    // ---- vote-gated-payout-drops-quorum (copied payout path) ----
    // Adversary round 4 (dapp_a_grants V2b): a stake-weighted, quorum-gated DAO
    // gained a second payout operation copied from execute_proposal minus the
    // quorum line. majority-without-quorum is silenced submission-wide once any
    // quorum/stake/weight term exists - deliberately, because a stake-weighted
    // execute is textually identical to an unweighted one - so the copy drew
    // zero findings and a single account voting 1-0 drained the treasury.

    private val COMPARISON_OP_REGEX = Regex("""(?<![=!<>\-])[<>]=?(?!=)""")
    private val ACCUMULATED_FIELD_REGEX = Regex("""\.\s*([A-Za-z_]\w*)\s*\+=""")
    private val FIELD_READ_REGEX = Regex("""\.\s*([A-Za-z_]\w*)\b(?!\s*(?:\(|=(?!=)|\+=|-=))""")
    private val WORD_REGEX = Regex("""[A-Za-z_]\w*""")

    /**
     * MEDIUM when a value-moving operation is gated by comparing two tallies
     * (two distinct fields the app accumulates with `+=`, compared in one
     * condition - `p.yes_weight > p.no_weight`), its own body (helpers
     * flattened in) references NO quorum/threshold/weight term other than
     * those tallies, and ANOTHER operation of the submission gated by the
     * same tally pair does reference one. That sibling is the proof the
     * designer meant a quorum here; the flagged path lost it. Keyed on use
     * (accumulated fields in a comparison), not on the tallies' names; the
     * quorum evidence is per operation, not per submission. A consistently
     * quorumless stake-weighted DAO has no such sibling and stays quiet -
     * that remains the deliberate majority-without-quorum bias. Advisory,
     * never blocking: a path that legitimately needs no quorum (refunding a
     * rejected proposal's deposit) has the same shape.
     */
    internal fun droppedQuorumGateFindings(
        files: Map<String, String>,
        fullyMasked: Map<String, String>,
        helpers: Map<String, List<FunctionDef>>,
        entities: Set<String>,
        valueMutatingFunctions: Set<String>
    ): List<Finding> {
        val eligible = fullyMasked.filter { (path, _) ->
            !RellLibs.isVendoredLibraryPath(path) && !RellLibs.isThirdPartyLibPath(path) &&
                !RunRellTests.isTestModuleSource(files.getValue(path))
        }
        val accumulated = eligible.values.flatMapTo(mutableSetOf()) { m ->
            ACCUMULATED_FIELD_REGEX.findAll(m).map { it.groupValues[1] }
        }
        if (accumulated.size < 2) return emptyList()
        data class Gate(val path: String, val op: OperationBlock, val pairs: Set<Set<String>>, val hasTerm: Boolean, val movesValue: Boolean)
        val gates = eligible.flatMap { (path, masked) ->
            scanOperations(path, masked).mapNotNull { op ->
                val flat = flattenHelpers(op.body, helpers, entities)
                val pairs = statementsOf(flat)
                    .filter { COMPARISON_OP_REGEX.containsMatchIn(it) }
                    .map { s -> FIELD_READ_REGEX.findAll(s).map { it.groupValues[1] }.filter { it in accumulated }.toSet() }
                    .filter { it.size >= 2 }
                    .toSet()
                if (pairs.isEmpty()) return@mapNotNull null
                val tallies = pairs.flatten().toSet()
                val hasTerm = WORD_REGEX.findAll(flat).any { w ->
                    w.value !in tallies && QUORUM_TERM_REGEX.containsMatchIn(w.value)
                }
                val movesValue = VALUE_MUTATION_REGEX.containsMatchIn(flat) ||
                    calledNames(flat).any { it in valueMutatingFunctions }
                Gate(path, op, pairs, hasTerm, movesValue)
            }
        }
        return gates.filter { it.movesValue && !it.hasTerm }.mapNotNull { g ->
            val sibling = gates.firstOrNull { s -> s !== g && s.hasTerm && s.pairs.any { it in g.pairs } }
                ?: return@mapNotNull null
            val pair = g.pairs.first { it in sibling.pairs }.sorted().joinToString(" vs ")
            Finding(
                "MEDIUM", "vote-gated-payout-drops-quorum", g.path, g.op.line,
                "operation ${g.op.name} moves value gated by the vote tally comparison ($pair) but " +
                    "references no quorum, threshold, or weight term - while operation ${sibling.op.name} " +
                    "(${sibling.path}:${sibling.op.line}), gated by the same tallies, does. This path " +
                    "dropped the quorum gate its sibling has, so a single account voting 1-0 on its own " +
                    "proposal satisfies it (adversary round 4: treasury 1000 -> 0 through the copied op)",
                "Apply the same participation floor here (e.g. require(yes + no >= quorum_weight)), " +
                    "or route both paths through one shared helper that checks quorum and majority " +
                    "together so a copy cannot lose the line. Advisory: if this path is meant to need " +
                    "no quorum (e.g. refunding a rejected proposal), document it and ignore this finding."
            )
        }
    }

    // ---- value-sink-without-withdrawal (locked funds) ----
    // Both adversary fee sinks shipped this way (dapp_a_points `treasury`,
    // dapp_b_market `fee_pot`, both certified ok:true): every transfer skims
    // a fee into a balance that is only ever incremented - no operation can
    // pay it out, so the value is permanently locked at deploy time.

    /** Entity/field names that suggest the row HOLDS collected value. */
    private val SINK_NAME_REGEX = Regex("""fee|pot|treasury|vault|reserve|escrow|pool""", RegexOption.IGNORE_CASE)

    /** One declared attribute of an entity, with its declared type. */
    internal data class EntityField(
        val entity: String,
        val name: String,
        val type: String,
        val mutable: Boolean,
        val file: String,
        val line: Int
    )

    private val ATTR_PREFIX_REGEX = Regex("""\A\s*(?:(?:key|index|mutable)\b\s*)+""")
    private val ATTR_LEAD_KEYWORDS = Regex("""\b(?:key|index|mutable)\b""")

    /**
     * Every attribute declared by every entity in the (masked) submission.
     * Attribute lists are split on both `;` and `,` so the compound forms
     * (`key member, seq: integer;`, `key owner: byte_array;`) yield one entry
     * per name; a name with no `: type` is an entity reference whose type is
     * its own name (`key member` -> type `member`), which is how Rell reads it.
     */
    internal fun entityFields(fullyMasked: Map<String, String>): List<EntityField> {
        val out = mutableListOf<EntityField>()
        fullyMasked.forEach { (path, masked) ->
            ENTITY_DEF_REGEX.findAll(masked).forEach { m ->
                val entity = m.groupValues[1]
                val braceStart = masked.indexOf('{', m.range.last)
                if (braceStart < 0) return@forEach
                val braceEnd = matchDelimiter(masked, braceStart, '{', '}') ?: return@forEach
                var cursor = braceStart + 1
                val block = masked.substring(braceStart + 1, braceEnd)
                block.split(';', ',').forEach { rawFragment ->
                    val start = cursor
                    cursor += rawFragment.length + 1
                    val fragment = rawFragment.substringBefore('=')
                    val mutable = ATTR_LEAD_KEYWORDS.find(fragment)?.value == "mutable" ||
                        Regex("""\bmutable\b""").containsMatchIn(fragment)
                    val stripped = ATTR_PREFIX_REGEX.replace(fragment, "").trim()
                    if (stripped.isEmpty()) return@forEach
                    val name = stripped.substringBefore(':').trim()
                    if (!Regex("""\A[A-Za-z_]\w*\z""").matches(name)) return@forEach
                    val type = if (stripped.contains(':')) stripped.substringAfter(':').trim() else name
                    val line = masked.substring(0, start).count { it == '\n' } + 1 +
                        rawFragment.takeWhile { it.isWhitespace() }.count { it == '\n' }
                    out.add(EntityField(entity, name, type, mutable, path, line))
                }
            }
        }
        return out
    }

    /** Local -> entity for function/operation parameters declared with an entity type. */
    private fun paramTypeAliases(masked: String, entities: Set<String>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        Regex("""\b(?:function|operation|query)\s+[A-Za-z_]\w*\s*\(([^()]*)\)""").findAll(masked).forEach { m ->
            parseParams(m.groupValues[1]).forEach { (name, type) ->
                val t = type.trim().trimEnd('?')
                if (t in entities) out[name] = t
            }
        }
        return out
    }

    /** The text from the previous statement/block delimiter to the next one. */
    private fun enclosingStatement(text: String, index: Int): String {
        var start = index
        while (start > 0 && text[start - 1] !in ";{}") start--
        var end = index
        while (end < text.length && text[end] !in ";{}") end++
        return text.substring(start, end)
    }

    /**
     * True when [field] of [entity] is read somewhere in the app as an
     * ACCOUNTING TERM - a factor in a `*`/`/` computation - and nowhere as a
     * spendable balance (no `>= amount` sufficiency check on it).
     *
     * This is the per-share accumulator and the denominator it divides by:
     * `acc_reward_per_share` and `total_staked` are a scaled INDEX and a sum,
     * monotonic by construction, and the value they index is paid out of a
     * different field. The rule's own advice - "add an admin operation that
     * debits it" - would corrupt every staker's payout, so firing on this
     * shape is the gate crying wolf on the pattern its own staking template
     * teaches (adversary round 5). A locked fee pot has the opposite profile:
     * it is credited and then never read at all.
     *
     * A read counts as this field's only when the receiver resolves to
     * [entity] or the field name is declared on exactly one entity in the
     * submission - so `wallet.balance / 2` elsewhere cannot exempt
     * `fee_pot.balance`.
     */
    private fun isAccountingIndex(
        bodies: List<Pair<String, Map<String, String>>>,
        entity: String,
        field: String,
        uniqueFieldName: Boolean
    ): Boolean {
        val esc = Regex.escape(field)
        val readRegex = Regex("""([A-Za-z_]\w*)\s*\.\s*$esc\b(?!\s*(?:\(|=(?!=)|\+=|-=))""")
        val sufficiency = Regex(
            """(?:\.\s*$esc\b(?:\s*\.\s*\w+\s*\(\s*\))?\s*(?:>=|<=|>|<)\s*(?!0\b)[A-Za-z_(]""" +
                """|(?<![<>=!])(?:>=|<=|>|<)\s*[A-Za-z_]\w*\s*\.\s*$esc\b)"""
        )
        var arithmeticRead = false
        bodies.forEach { (masked, alias) ->
            readRegex.findAll(masked).forEach { m ->
                val recv = m.groupValues[1]
                if (!uniqueFieldName && alias[recv] != entity && recv != entity) return@forEach
                val stmt = enclosingStatement(masked, m.range.first)
                if (sufficiency.containsMatchIn(stmt)) return false
                if (ARITHMETIC_REGEX.containsMatchIn(stmt)) arithmeticRead = true
            }
        }
        return arithmeticRead
    }

    /**
     * MEDIUM, once per sink field, when a value-holding field on a
     * fee/pot/treasury-named entity is credited (`+=`, or created non-zero)
     * somewhere in the app and NO app code ever decrements or reassigns it.
     * The name gate keeps monotonic statistics counters out; an unresolvable
     * write (dotted target, unknown local) to a same-named field is treated
     * as a possible withdrawal and silences the rule - conservative in the
     * quiet direction, because an advisory that cries wolf trains agents to
     * ignore the gate. Advisory, never blocking: a lock-forever sink can be
     * intended (burn-style), and only the designer knows.
     */
    internal fun valueSinkFindings(
        files: Map<String, String>,
        fullyMasked: Map<String, String>,
        entities: Set<String>,
        helperReturns: Map<String, String>
    ): List<Finding> {
        val eligible = files.keys.filter { path ->
            !RellLibs.isVendoredLibraryPath(path) && !RellLibs.isThirdPartyLibPath(path) &&
                !RunRellTests.isTestModuleSource(files.getValue(path))
        }
        val declaredFields = entityFields(fullyMasked.filterKeys { it in eligible })
        val fieldOwners = declaredFields.groupBy({ it.name }, { it.entity })
            .mapValues { (_, v) -> v.toSet() }
        // One candidate per (entity, field), anchored on the FIELD's line: two
        // sink fields on one entity used to report twice on the entity's line
        // and read as a duplicate emission (adversary round 5).
        val candidates = declaredFields.filter { f ->
            f.mutable && VALUE_FIELD_NAME_REGEX.containsMatchIn(f.name) &&
                (SINK_NAME_REGEX.containsMatchIn(f.entity) || SINK_NAME_REGEX.containsMatchIn(f.name))
        }.distinctBy { it.entity to it.name }
        if (candidates.isEmpty()) return emptyList()
        // Only operation and function bodies count as reads: a query that
        // formats the field for display is not a payout computation.
        val bodies = eligible.flatMap { path ->
            val masked = fullyMasked.getValue(path)
            val fileAlias = paramTypeAliases(masked, entities)
            (scanOperations(path, masked).map { it.body } + functionBodies(masked).map { it.second })
                .map { body -> body to (fileAlias + aliasMap(body, entities, helperReturns)) }
        }
        val writes = eligible.flatMap { valueWrites(fullyMasked.getValue(it), entities, helperReturns) }
        return candidates.mapNotNull { c ->
            val fieldWrites = writes.filter { it.field == c.name && it.entity == c.entity }
            val credited = fieldWrites.any { it.kind == "+=" || it.kind == "create" }
            val drained = fieldWrites.any { it.kind == "-=" || it.kind == "=" }
            val unresolvedOut = writes.any {
                it.entity == null && it.field == c.name && (it.kind == "-=" || it.kind == "=")
            }
            if (!credited || drained || unresolvedOut) return@mapNotNull null
            // An accumulator/denominator is not a pot: it is read as a factor
            // in a payout computation, so debiting it corrupts the payout.
            if (isAccountingIndex(bodies, c.entity, c.name, fieldOwners[c.name]?.size == 1)) {
                return@mapNotNull null
            }
            Finding(
                "MEDIUM", "value-sink-without-withdrawal", c.file, c.line,
                "${c.entity}.${c.name} is only ever incremented - fees/value accumulate here and no " +
                    "operation in the app can ever pay them out, so everything credited is permanently " +
                    "locked at deploy time",
                "Add a withdrawal path behind an explicit gate (e.g. an admin operation keyed to " +
                    "chain_context.args that debits ${c.entity}.${c.name}), or route fees to an owned " +
                    "account. Advisory: if locking value forever is intended (burn sink), document it " +
                    "and ignore this finding."
            )
        }
    }

    // ---- shared helpers for the parameter-flow rules ----

    /** Call-shaped tokens that are control flow or builtins, never validators. */
    private val CONTROL_KEYWORDS = setOf(
        "if", "while", "for", "when", "not", "require", "require_not_empty", "exists", "empty"
    )

    /** The (masked) argument text of one [CALL_SITE_REGEX] match. */
    private fun argsOf(body: String, m: MatchResult): String {
        val parenStart = body.indexOf('(', m.range.first)
        val parenEnd = matchDelimiter(body, parenStart, '(', ')') ?: return ""
        return body.substring(parenStart + 1, parenEnd)
    }

    private val VAL_BINDING_REGEX = Regex("""\bval\s+([A-Za-z_]\w*)\s*=([^;]*)""")

    /** The parameter plus every local transitively assigned from it. */
    internal fun taintedNames(body: String, param: String): Set<String> {
        val names = mutableSetOf(param)
        val bindings = VAL_BINDING_REGEX.findAll(body).map { it.groupValues[1] to it.groupValues[2] }.toList()
        var changed = true
        while (changed) {
            changed = false
            bindings.forEach { (n, expr) ->
                if (n !in names && names.any { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(expr) }) {
                    names.add(n)
                    changed = true
                }
            }
        }
        return names
    }

    /**
     * True when [param] is handed to a call that may validate it where this
     * scan cannot see: a submitted helper that require()s, or a callee the
     * submission does not define at all (library/builtin - benefit of the
     * doubt, matching the delegated-validation precedent). Entity names are
     * excluded: `create log_row(p)` stores p, it does not validate it.
     */
    /**
     * `update v ( .balance -= amount );` is textually a call `v(...)`, and since
     * a local is not a known function the delegation check read it as "handed to
     * a helper that validates it" and stayed silent - so every rule using
     * paramDelegated was blind to the select-into-a-local shape, which is the
     * natural way to write a withdraw. The mutation keyword decides: a name
     * immediately preceded by `update`/`delete` is a TARGET, never a callee.
     */
    private fun isMutationTarget(body: String, m: MatchResult): Boolean {
        val before = body.substring(0, m.range.first).trimEnd()
        return before.endsWith("update") || before.endsWith("delete")
    }

    private fun paramDelegated(
        body: String,
        names: Set<String>,
        requireFunctions: Set<String>,
        knownFunctions: Set<String>,
        entities: Set<String>
    ): Boolean = CALL_SITE_REGEX.findAll(body).any { m ->
        val callee = m.groupValues[1]
        callee !in CONTROL_KEYWORDS && callee !in entities && !isMutationTarget(body, m) &&
            (callee in requireFunctions || callee !in knownFunctions) &&
            names.any { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(argsOf(body, m)) }
    }

    // ---- amount-without-lower-bound (negative-amount inversion, probe N4) ----
    // `.balance -= amount` with no sign check: amount = -5000 turns a withdraw
    // into a mint (and `.balance += amount` into a drain of the credited row).
    // The rule keys on USE - a numeric parameter feeding a compound value
    // write with no lower bound anywhere - never on the parameter's name;
    // names are attacker-chosen (the a2-renamed lesson).

    private val NUMERIC_TYPE_REGEX = Regex("""^(?:integer|big_integer|decimal)\??$""")

    /** RHS of a compound write starting at [from]: to the first top-level `,` `;` or closer. */
    private fun rhsAfter(body: String, from: Int): String {
        val sb = StringBuilder()
        var depth = 0
        var k = from
        while (k < body.length) {
            val c = body[k]
            when {
                c in "([{" -> depth++
                c in ")]}" -> { if (depth == 0) break; depth-- }
                (c == ',' || c == ';') && depth == 0 -> break
            }
            sb.append(c)
            k++
        }
        return sb.toString()
    }

    /** `n > x` / `n >= x` / `n == x` / `x < n` / `x <= n` - anything bounding n from below (or pinning it). */
    private fun hasLowerBound(body: String, names: Set<String>): Boolean =
        names.any { n ->
            val esc = Regex.escape(n)
            Regex("""\b$esc\b\s*(?:>=?|==)""").containsMatchIn(body) ||
                Regex("""<=?\s*\b$esc\b""").containsMatchIn(body)
        }

    private fun amountLowerBoundFindings(
        path: String,
        op: OperationBlock,
        requireFunctions: Set<String>,
        knownFunctions: Set<String>,
        valueMutatingFunctions: Set<String>,
        mutatingFunctions: Set<String>,
        entities: Set<String>
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        parseParams(op.params).forEach { (name, type) ->
            if (!NUMERIC_TYPE_REGEX.matches(type.trim().lowercase())) return@forEach
            val tainted = taintedNames(op.body, name)
                        if (hasLowerBound(op.body, tainted)) return@forEach
            if (paramDelegated(op.body, tainted, requireFunctions, knownFunctions, entities)) return@forEach
            fun taintedIn(text: String) =
                tainted.any { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(text) }
            // Direct value write fed by the parameter. Debit shapes: `-=` on any
            // field, or a subtraction of the tainted value inside a `+=`/`=`
            // write to a value-named field (`.balance = .balance - amount` and
            // `.balance += -amount` are the same inversion spelled differently -
            // rewriting the operator must not evade the rule). Credit shape:
            // `+=` of the tainted value to a value-named field.
            fun subtractedTaintIn(rhs: String) =
                tainted.any { Regex("""-\s*\b${Regex.escape(it)}\b""").containsMatchIn(rhs) }
            val directHit = FIELD_WRITE_REGEX.findAll(op.body).any { w ->
                val kind = w.groupValues[2]
                val valueField = VALUE_FIELD_NAME_REGEX.containsMatchIn(w.groupValues[1])
                val rhs = rhsAfter(op.body, w.range.last + 1)
                when (kind) {
                    "-=" -> taintedIn(rhs)
                    "+=" -> taintedIn(rhs) && valueField
                    "=" -> subtractedTaintIn(rhs) && valueField
                    else -> false
                }
            }
            // ...or handed to a submitted helper that mutates value and never
            // require()s anything - the wrap-it-in-a-helper evasion.
            val helperHit = !directHit && CALL_SITE_REGEX.findAll(op.body).any { m ->
                val callee = m.groupValues[1]
                (callee in valueMutatingFunctions || callee in mutatingFunctions) &&
                    callee !in requireFunctions && taintedIn(argsOf(op.body, m))
            }
            if (!directHit && !helperHit) return@forEach
            findings.add(
                Finding(
                    "HIGH", "amount-without-lower-bound", path, op.line,
                    "operation ${op.name} feeds caller-supplied numeric '$name' into a += / -= balance " +
                        "write with no lower bound anywhere in the operation - a negative value inverts " +
                        "the write, turning a debit into a mint (or a credit into a drain)",
                    "Bound the amount before using it: require($name > 0, \"amount must be positive\") " +
                        "(or an explicit domain minimum). An upper-bound check like " +
                        "require(balance >= $name) does not stop negative values."
                )
            )
        }
        return findings
    }

    // ---- unvalidated-stored-parameter (per-parameter validation, probe S5) ----
    // One require() on x used to count as validating y too. When an operation
    // demonstrably validates SOME parameter, every other text parameter it
    // stores must carry its own check - the masking effect is exactly how the
    // exploit sample slipped through. Scoped to text parameters (length/format
    // checks always exist for them); identity types (byte_array/pubkey) have
    // no meaningful range check and stay out to keep the rule quiet.

    private fun storedAsValue(
        body: String,
        ref: Regex,
        mutatingFunctions: Set<String>,
        requireFunctions: Set<String>
    ): Boolean {
        Regex("""\bcreate\s+[A-Za-z_][\w.]*\s*\(""").findAll(body).forEach { m ->
            val ps = body.indexOf('(', m.range.first)
            val pe = matchDelimiter(body, ps, '(', ')') ?: return@forEach
            if (ref.containsMatchIn(body.substring(ps + 1, pe))) return true
        }
        UPDATE_KEYWORD_REGEX.findAll(body).forEach { m ->
            val end = body.indexOf(';', m.range.first).let { if (it < 0) body.length else it }
            val stmt = body.substring(m.range.first, end)
            val braceStart = stmt.indexOf('{')
            val setPart = if (braceStart >= 0) {
                val be = matchDelimiter(stmt, braceStart, '{', '}') ?: return@forEach
                stmt.substring(be + 1)
            } else {
                stmt.substringAfter('(', "")
            }
            if (ref.containsMatchIn(setPart)) return true
        }
        return CALL_SITE_REGEX.findAll(body).any { m ->
            val callee = m.groupValues[1]
            callee in mutatingFunctions && callee !in requireFunctions &&
                ref.containsMatchIn(argsOf(body, m))
        }
    }

    private fun perParamValidationFindings(
        path: String,
        op: OperationBlock,
        requireFunctions: Set<String>,
        knownFunctions: Set<String>,
        mutatingFunctions: Set<String>,
        entities: Set<String>
    ): List<Finding> {
        val params = parseParams(op.params)
        if (params.size < 2) return emptyList()
        val statements = op.body.split(';')
        fun validated(p: String): Boolean {
            val ref = Regex("""\b${Regex.escape(p)}\b""")
            if (statements.any { REQUIRE_REGEX.containsMatchIn(it) && ref.containsMatchIn(it) }) return true
            return paramDelegated(op.body, setOf(p), requireFunctions, knownFunctions, entities)
        }
        // The masking effect needs a mask: fire only when some parameter IS
        // validated (an op with no require at all is unvalidated-inputs').
        if (params.none { (n, _) -> validated(n) }) return emptyList()
        val findings = mutableListOf<Finding>()
        params.forEach { (name, type) ->
            if (type.trim().trimEnd('?').trim().lowercase() != "text") return@forEach
            if (validated(name)) return@forEach
            val ref = Regex("""\b${Regex.escape(name)}\b""")
            if (!storedAsValue(op.body, ref, mutatingFunctions, requireFunctions)) return@forEach
            findings.add(
                Finding(
                    "MEDIUM", "unvalidated-stored-parameter", path, op.line,
                    "operation ${op.name} validates other input(s) but stores text parameter '$name' " +
                        "with no check of its own - a require() on one parameter does not validate " +
                        "the others",
                    "Validate '$name' before storing it: require($name.size() > 0 and " +
                        "$name.size() <= MAX, \"bad $name\") - length/format checks per parameter, " +
                        "not per operation."
                )
            )
        }
        return findings
    }

    // ---- iccf-proof-without-provenance (probe N12) ----
    // require_valid_proof proves SOME transaction is anchored - it says
    // nothing about WHICH chain it came from. Without binding a source
    // blockchain_rid, any chain's transaction satisfies the proof: proof is
    // not provenance. Advisory MEDIUM: the documented ICCF pattern binds the
    // source chain from module args (see clean-iccf-proof-provenance), but
    // some proof-carrying flows legitimately accept multiple sources.

    private val VALID_PROOF_CALL_REGEX = Regex("""\brequire_valid_proof\s*\(""")
    private val CHAIN_BINDING_REGEX = Regex("""chain_context\s*\.\s*args|blockchain_rid""")

    private fun iccfProvenanceFindings(
        path: String,
        op: OperationBlock,
        mutatingFunctions: Set<String>
    ): List<Finding> {
        if (!VALID_PROOF_CALL_REGEX.containsMatchIn(op.body)) return emptyList()
        val mutates = MUTATION_REGEX.containsMatchIn(op.body) ||
            calledNames(op.body).any { it in mutatingFunctions }
        if (!mutates) return emptyList()
        if (CHAIN_BINDING_REGEX.containsMatchIn(op.body)) return emptyList()
        return listOf(
            Finding(
                "MEDIUM", "iccf-proof-without-provenance", path, op.line,
                "operation ${op.name} mutates state gated by require_valid_proof but never binds the " +
                    "source chain - a valid proof only shows SOME anchored transaction exists, so any " +
                    "chain's transaction (or the wrong chain's) satisfies it: proof is not provenance",
                "Bind provenance alongside the proof: require(source == chain_context.args.source_brid, " +
                    "\"wrong source chain\") with the trusted blockchain_rid from module args, then " +
                    "validate what the proven transaction actually did. Advisory: if multiple source " +
                    "chains are intended, check membership in a trusted set instead."
            )
        )
    }

    // ---- icmf-sender-not-validated (probe S4) ----
    // An @extend(receive_icmf_message) body runs for ANY chain publishing on
    // the topic; mutating state without validating `sender` lets any chain
    // push fake data. Extend bodies are not operations, so the operation scan
    // never saw them at all.

    private val ICMF_EXTEND_REGEX =
        Regex("""@extend\s*\(\s*[\w.]*?receive_icmf_message\s*\)\s*function\s+[A-Za-z_]\w*\s*\(""")
    private val VALIDATION_CONTEXT_REGEX =
        Regex("""==|!=|\bin\b|\brequire|\bexists\s*\(|@|\bif\s*\(|\bwhen\b""")

    private fun senderValidated(
        body: String,
        sender: String,
        requireFunctions: Set<String>,
        knownFunctions: Set<String>,
        entities: Set<String>
    ): Boolean {
        val tainted = taintedNames(body, sender)
        fun taintedIn(text: String) =
            tainted.any { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(text) }
        if (body.split(';').any { st -> taintedIn(st) && VALIDATION_CONTEXT_REGEX.containsMatchIn(st) }) return true
        return paramDelegated(body, tainted, requireFunctions, knownFunctions, entities)
    }

    internal fun icmfReceiverFindings(
        path: String,
        masked: String,
        mutatingFunctions: Set<String>,
        requireFunctions: Set<String>,
        knownFunctions: Set<String>,
        entities: Set<String>
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        ICMF_EXTEND_REGEX.findAll(masked).forEach { m ->
            val parenEnd = matchDelimiter(masked, m.range.last, '(', ')') ?: return@forEach
            val params = parseParams(masked.substring(m.range.last + 1, parenEnd))
            val braceStart = masked.indexOf('{', parenEnd)
            if (braceStart < 0) return@forEach
            val braceEnd = matchDelimiter(masked, braceStart, '{', '}') ?: return@forEach
            val body = masked.substring(braceStart + 1, braceEnd)
            val mutates = MUTATION_REGEX.containsMatchIn(body) ||
                calledNames(body).any { it in mutatingFunctions }
            if (!mutates) return@forEach
            // The sender is the FIRST parameter by position - its name is the
            // author's choice and must not matter.
            val sender = params.firstOrNull()?.first ?: return@forEach
            if (senderValidated(body, sender, requireFunctions, knownFunctions, entities)) return@forEach
            findings.add(
                Finding(
                    "HIGH", "icmf-sender-not-validated", path,
                    masked.substring(0, m.range.first).count { it == '\n' } + 1,
                    "an @extend(receive_icmf_message) handler mutates state without ever checking the " +
                        "message sender - ANY chain publishing on the topic can trigger this write with " +
                        "attacker-chosen content",
                    "Validate the sender before mutating: require(sender == " +
                        "chain_context.args.trusted_chain, \"untrusted sender\") (or look it up in a " +
                        "registry of trusted chain RIDs). The topic name is not a trust boundary - " +
                        "the sender chain is."
                )
            )
        }
        return findings
    }

    // ---- query-returns-secret-data (probe N8) ----
    // Queries are publicly callable on every node with NO caller identity:
    // whatever a query can return, anyone can read. A query touching fields
    // named like secrets is therefore publishing them. Advisory MEDIUM, keyed
    // on the AUTHOR's own naming (not attacker-controlled input): the threat
    // here is a well-meaning generator storing secrets on chain, not an
    // adversarial author evading the scan.

    internal data class QueryBlock(val name: String, val line: Int, val body: String)

    private val QUERY_REGEX = Regex("""\bquery\s+([A-Za-z_]\w*)\s*\(""")

    internal fun scanQueries(masked: String): List<QueryBlock> {
        val out = mutableListOf<QueryBlock>()
        QUERY_REGEX.findAll(masked).forEach { m ->
            val parenStart = masked.indexOf('(', m.range.first)
            val parenEnd = matchDelimiter(masked, parenStart, '(', ')') ?: return@forEach
            val line = masked.substring(0, m.range.first).count { it == '\n' } + 1
            var j = parenEnd + 1
            while (j < masked.length && masked[j].isWhitespace()) j++
            if (j < masked.length && masked[j] == ':') {
                // explicit return type: skip to the `=` or `{` that starts the body
                while (j < masked.length && masked[j] != '=' && masked[j] != '{') j++
            }
            val body = when {
                j < masked.length && masked[j] == '{' -> {
                    val close = matchDelimiter(masked, j, '{', '}') ?: return@forEach
                    masked.substring(j + 1, close)
                }
                j < masked.length && masked[j] == '=' -> {
                    val semi = masked.indexOf(';', j).let { if (it < 0) masked.length else it }
                    masked.substring(j + 1, semi)
                }
                else -> return@forEach
            }
            out.add(QueryBlock(m.groupValues[1], line, body))
        }
        return out
    }

    private val SECRET_ID_PARTS = setOf(
        "secret", "secrets", "private", "priv", "password", "passwords", "passwd", "pwd",
        "credential", "credentials", "mnemonic", "ssn"
    )
    private val IDENTIFIER_REGEX = Regex("""[A-Za-z_]\w*""")

    private fun querySecretExposureFindings(path: String, q: QueryBlock): List<Finding> {
        val secretId = IDENTIFIER_REGEX.findAll(q.body)
            .map { it.value }
            .firstOrNull { id -> id.lowercase().split('_').any { it in SECRET_ID_PARTS } }
            ?: return emptyList()
        return listOf(
            Finding(
                "MEDIUM", "query-returns-secret-data", path, q.line,
                "query ${q.name} reads '$secretId' - queries are publicly callable on every node with " +
                    "no caller identity, so anything a query returns is readable by anyone",
                "Queries cannot be access-controlled: do not expose secret material through them. " +
                    "Remember all Chromia state is visible to node operators regardless - secrets " +
                    "should not live on chain at all. Advisory: if '$secretId' is not actually " +
                    "secret, rename it or ignore this finding."
            )
        )
    }

    // ---- conditional-auth-bypass (auth not on every completing path, probe N9) ----
    // `if (as_admin) { require(is_signer(...)) } update ...` - the caller opts
    // OUT of the only auth check and the mutation runs anyway. Rell operations
    // are transactional, so a require-style auth check guards every mutation
    // in its segment regardless of statement order (an abort reverts); what it
    // cannot guard is a path that never executes it. This scanner walks the
    // if/else structure: a mutation is guarded when auth is established
    // unconditionally in its segment, in its own branch, by a non-negated
    // auth-bearing branch condition, or by a full-coverage if/else chain where
    // every branch authenticates. `for`/`while` bodies are conditional (zero
    // iterations); `if (not is_signer(...)) return;` guards everything AFTER
    // it (return commits, so it cannot guard mutations before it).

    private val CONTROL_HEAD_REGEX = Regex("""\b(if|for|while)\s*\(""")
    private val COND_NEGATION_REGEX = Regex("""\bnot\b|==\s*false|!(?!=)""")
    private val RETURN_REGEX = Regex("""\breturn\b""")
    private val WHEN_HEAD_REGEX = Regex("""\bwhen\b""")
    private val WHEN_ELSE_ARM_REGEX = Regex("""\belse\s*->""")

    internal data class PathScan(val establishesAuth: Boolean, val unguardedMutation: Boolean)

    private data class BranchText(val text: String, val end: Int)

    private fun extractBranch(body: String, from: Int): BranchText? {
        var j = from
        while (j < body.length && body[j].isWhitespace()) j++
        if (j >= body.length) return null
        return if (body[j] == '{') {
            val close = matchDelimiter(body, j, '{', '}') ?: return null
            BranchText(body.substring(j + 1, close), close + 1)
        } else {
            val semi = body.indexOf(';', j)
            if (semi < 0) BranchText(body.substring(j), body.length)
            else BranchText(body.substring(j, semi + 1), semi + 1)
        }
    }

    /**
     * `when` blocks resist textual branch parsing, so they are handled in the
     * QUIET direction: a when whose block contains an auth marker is kept
     * verbatim (its marker reads as straight-line auth) when it has an
     * `else ->` arm or contains a mutation of its own; a when WITHOUT an else
     * that only carries auth cannot cover all paths, so its content is
     * blanked and the marker cannot masquerade as unconditional.
     */
    private fun neutralizeNonExhaustiveWhens(
        body: String,
        markers: List<String>,
        authFns: Set<String>
    ): String {
        var result = body
        var searchFrom = 0
        while (true) {
            val m = WHEN_HEAD_REGEX.find(result, searchFrom) ?: break
            var j = m.range.last + 1
            while (j < result.length && result[j].isWhitespace()) j++
            if (j < result.length && result[j] == '(') {
                val pe = matchDelimiter(result, j, '(', ')') ?: return result
                j = pe + 1
                while (j < result.length && result[j].isWhitespace()) j++
            }
            if (j >= result.length || result[j] != '{') {
                searchFrom = m.range.last + 1
                continue
            }
            val close = matchDelimiter(result, j, '{', '}') ?: return result
            val block = result.substring(j + 1, close)
            val hasAuth = containsAuthMarker(block, markers) || calledNames(block).any { it in authFns }
            val exhaustive = WHEN_ELSE_ARM_REGEX.containsMatchIn(block)
            val hasMutation = MUTATION_REGEX.containsMatchIn(block)
            if (hasAuth && !exhaustive && !hasMutation) {
                val blanked = block.map { if (it == '\n') '\n' else ' ' }.joinToString("")
                result = result.substring(0, j + 1) + blanked + result.substring(close)
            }
            searchFrom = close + 1
        }
        return result
    }

    internal fun scanAuthPaths(
        rawBody: String,
        markers: List<String>,
        authFns: Set<String>,
        mutFns: Set<String>
    ): PathScan {
        val body = neutralizeNonExhaustiveWhens(rawBody, markers, authFns)
        fun authIn(text: String) =
            containsAuthMarker(text, markers) || calledNames(text).any { it in authFns }
        fun mutatesIn(text: String) =
            MUTATION_REGEX.containsMatchIn(text) || calledNames(text).any { it in mutFns }

        val straight = StringBuilder()
        var chainAuth = false
        var chainUnguarded = false
        var i = 0
        while (i < body.length) {
            val m = CONTROL_HEAD_REGEX.find(body, i)
            if (m == null) {
                straight.append(body.substring(i))
                i = body.length
                break
            }
            straight.append(body, i, m.range.first)
            val keyword = m.groupValues[1]
            var pos = m.range.first
            var allGuarded = true
            var sawElse = false
            var earlyExitGuard = false
            var parseFailed = false
            while (true) {
                val parenStart = body.indexOf('(', pos)
                val parenEnd = if (parenStart >= 0) matchDelimiter(body, parenStart, '(', ')') else null
                if (parenEnd == null) { parseFailed = true; break }
                val cond = body.substring(parenStart + 1, parenEnd)
                val branch = extractBranch(body, parenEnd + 1)
                if (branch == null) { parseFailed = true; break }
                val sub = scanAuthPaths(branch.text, markers, authFns, mutFns)
                val condAuth = authIn(cond)
                val negated = COND_NEGATION_REGEX.containsMatchIn(cond)
                val guarded = sub.establishesAuth || (condAuth && !negated)
                if (!guarded) {
                    allGuarded = false
                    if (sub.unguardedMutation) chainUnguarded = true
                }
                if (keyword == "if" && condAuth && negated && RETURN_REGEX.containsMatchIn(branch.text)) {
                    earlyExitGuard = true
                }
                pos = branch.end
                if (keyword != "if") break // loops have no else-chain
                var j = pos
                while (j < body.length && body[j].isWhitespace()) j++
                val isElse = body.startsWith("else", j) &&
                    (j + 4 >= body.length || !(body[j + 4].isLetterOrDigit() || body[j + 4] == '_'))
                if (!isElse) break
                var k = j + 4
                while (k < body.length && body[k].isWhitespace()) k++
                val isElseIf = body.startsWith("if", k) &&
                    (k + 2 >= body.length || !(body[k + 2].isLetterOrDigit() || body[k + 2] == '_'))
                if (isElseIf) {
                    pos = k
                    continue
                }
                val elseBranch = extractBranch(body, j + 4)
                if (elseBranch == null) { parseFailed = true; break }
                val elseSub = scanAuthPaths(elseBranch.text, markers, authFns, mutFns)
                if (!elseSub.establishesAuth) {
                    allGuarded = false
                    if (elseSub.unguardedMutation) chainUnguarded = true
                }
                sawElse = true
                pos = elseBranch.end
                break
            }
            if (parseFailed) {
                // Unparseable control flow: bail in the QUIET direction and
                // treat the remainder as straight-line text.
                straight.append(body.substring(i))
                i = body.length
                break
            }
            if (keyword == "if" && sawElse && allGuarded) chainAuth = true
            i = pos
            if (earlyExitGuard) {
                // Everything after this point runs only once the gate passed;
                // mutations BEFORE it committed on the unauthenticated path.
                val preText = straight.toString()
                val preAuth = chainAuth || authIn(preText)
                return PathScan(
                    establishesAuth = true,
                    unguardedMutation = !preAuth && (mutatesIn(preText) || chainUnguarded)
                )
            }
        }
        val text = straight.toString()
        val auth = chainAuth || authIn(text)
        return PathScan(auth, !auth && (mutatesIn(text) || chainUnguarded))
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
        authMarkers: List<String> = AUTH_MARKERS,
        valueMutatingFunctions: Set<String> = emptySet(),
        ft4AuthCallers: Set<String> = emptySet(),
        emptyFlagsOnly: Boolean = false,
        requireFunctions: Set<String> = emptySet(),
        helpers: Map<String, List<FunctionDef>> = emptyMap(),
        entities: Set<String> = emptySet(),
        testModule: Boolean = false
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        val calls = calledNames(op.body)
        // The keyed rules (confused deputy, phantom signer gate) analyze the
        // operation with every reachable app-owned helper inlined.
        val effectiveBody = inlineHelpers(op.body, helpers, entities)
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
            findings += confusedDeputyFindings(path, op, effectiveBody, authFunctions)
        }
        if (mutates && hasAuth) {
            val scan = scanAuthPaths(op.body, authMarkers, authFunctions, mutatingFunctions)
            if (scan.unguardedMutation) {
                findings.add(
                    Finding(
                        "HIGH", "conditional-auth-bypass", path, op.line,
                        "operation ${op.name} mutates state, but its auth check(s) sit on a conditional " +
                            "path (an if-branch or loop body) that can be skipped - the mutation still " +
                            "runs on the path that never authenticates",
                        "Establish auth unconditionally before mutating (auth.authenticate() or " +
                            "require(op_context.is_signer(...)) at operation top level), or authenticate " +
                            "in EVERY branch of the conditional. An auth check the caller can steer " +
                            "around (if (as_admin) { ... }) is not a gate."
                    )
                )
            }
        }
        findings += phantomSignerGateFindings(path, op, effectiveBody, mutates)
        if (!testModule) {
            findings += bulkMutationNotCallerBoundFindings(path, op, effectiveBody, authFunctions, helpers.keys)
        }
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
