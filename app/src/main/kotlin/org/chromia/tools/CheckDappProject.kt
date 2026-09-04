package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Combined read-only in-memory Chromia dapp project check.
 * Runs [ChromiaYmlValidator] on a chromia.yml string and [Ft4ImportCheck]
 * on one or more .rell file contents. Does not write to disk, run chr,
 * generate keys, or send signed transactions.
 */
object CheckDappProject {
    const val YAML_PATH = "chromia.yml"

    private val LINE_REF = Regex("""^line (\d+): (.*)$""")

    /** "line 3: msg" -> "main.rell:3: msg", matching the compile/security format. */
    private fun locate(label: String, finding: String): String =
        LINE_REF.matchEntire(finding)
            ?.let { "$label:${it.groupValues[1]}: ${it.groupValues[2]}" }
            ?: "$label: $finding"

    data class Result(
        val ok: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
        val notes: List<String> = emptyList()
    ) {
        fun toJson() = buildJsonObject {
            put("ok", ok)
            put("errors", buildJsonArray { errors.forEach { add(JsonPrimitive(it)) } })
            put("warnings", buildJsonArray { warnings.forEach { add(JsonPrimitive(it)) } })
            put("notes", notes.joinToString(" "))
        }
    }

    /**
     * @param compile also compile the sources (rell_check) and run the security
     * pass, so `ok:true` means "this project actually builds and is not obviously
     * insecure" - not merely "the yml parses". Disable only for pure-text checks.
     * @param allowAdminModules downgrade banned admin-module/open-strategy
     * findings from errors to warnings (admin/ops tooling escape hatch; the
     * scaffold policy and default behavior are unchanged).
     * @param usedDefaultYaml the caller omitted `yaml` and [DappScaffold.defaultChromiaYml]
     * was substituted - said in notes so ok=true is never mistaken for a
     * validation of the caller's own (absent) config.
     */
    fun check(
        yaml: String,
        rellFiles: Map<String, String>,
        compile: Boolean = true,
        allowAdminModules: Boolean = false,
        usedDefaultYaml: Boolean = false
    ): Result {
        val yml = ChromiaYmlValidator.validate(yaml)
        val errors = mutableListOf<String>()
        val warnings = yml.warnings.map { "$YAML_PATH: $it" }.toMutableList()
        val notes = mutableListOf<String>()
        if (usedDefaultYaml) {
            notes += "No yaml provided - used a default chromia.yml (rellVersion ${DappScaffold.RELL_VERSION})."
        }
        yml.errors.forEach { errors += "$YAML_PATH: $it" }
        if (rellFiles.isEmpty()) {
            errors += "missing .rell file contents"
        }
        var exemptedLibFiles = 0
        var thirdPartyLibFiles = 0
        var exemptedTestModules = 0
        rellFiles.forEach { (path, content) ->
            // FT4's own library files contain `operation ras_open(` and
            // `import lib.ft4.admin;` - scanning a submitted lib/ft4 tree
            // reported forbidden-module errors pointing INTO the library (audit
            // F2 follow-up). Skip vendored-library files; app files stay scanned.
            // Content-gated: only a file bit-identical (modulo line endings) to
            // the vendored copy is trusted - a differing lib file could be
            // planted code and is scanned like app code, with a note why.
            if (RellLibs.isVendoredLibraryPath(path)) {
                if (RellLibs.matchesVendoredFt4(path, content)) {
                    exemptedLibFiles++
                    return@forEach
                }
                notes += RellLibs.modifiedFt4Note(path)
            } else if (RellLibs.isThirdPartyLibPath(path)) {
                // lib/** with no vendored copy to compare (lib/ft3, lib/icmf,
                // ...): third-party library code, skipped (real-world round 2 D5).
                thirdPartyLibFiles++
                return@forEach
            }
            // @test modules legitimately exercise admin modules and registration
            // strategies (real-world round 2 D4) - exempt from the forbidden scan.
            if (RunRellTests.isTestModuleSource(content)) {
                exemptedTestModules++
                return@forEach
            }
            // Same normalized path and "<path>:<line>: ..." shape as the compile
            // and security findings below - FT4 findings used to say
            // "src/main.rell: line 3: ..." for the same file (audit 2026-09-01).
            val label = path.trim().removePrefix("./").removePrefix("src/").ifEmpty { "rell" }
            val one = Ft4ImportCheck.scan(content, allowAdminModules)
            one.errors.forEach { err -> errors += locate(label, err) }
            one.warnings.forEach { warn -> warnings += locate(label, warn) }
        }
        if (exemptedLibFiles > 0) {
            notes += RellLibs.exemptedFt4Note(exemptedLibFiles)
        }
        if (thirdPartyLibFiles > 0) {
            notes += RellLibs.thirdPartyLibNote(thirdPartyLibFiles)
        }
        if (exemptedTestModules > 0) {
            notes += "$exemptedTestModules @test module file(s) exempt from the forbidden-module scan - " +
                "test code legitimately exercises admin modules and registration strategies."
        }

        if (compile && rellFiles.isNotEmpty()) {
            // A project check that never compiles can report ok:true on code that
            // does not parse. Compile, then security-scan when it builds.
            val normalized = rellFiles.keys.filter { it.trim().endsWith(".rell") }
                .groupBy { it.trim().removePrefix("./").removePrefix("src/") }
            // src/main.rell and main.rell normalize to the same compile path - one
            // would silently clobber the other (audit 2026-09-01).
            val collisions = normalized.filterValues { it.size > 1 }
            if (collisions.isNotEmpty()) {
                errors += "rell: paths ${collisions.values.first()} resolve to the same file after " +
                    "normalization - rename one so both can be checked."
            }
            val compilable = normalized.mapValues { (_, paths) -> rellFiles.getValue(paths.first()) }
            // The yaml's `module:` must name a module that was actually sent.
            // `module: app` with main.rell submitted compiled main.rell, found it
            // fine, and reported ok=true on a project `chr build` rejects with
            // "Module 'app' not found" (DX audit 2026-09-04, Q3). A gate that
            // vouches for a chain must at least see that chain's root module.
            declaredModulesNotSubmitted(yaml, compilable).forEach { (chain, module, found) ->
                errors += "$YAML_PATH: blockchains.$chain.module '$module' is not among the submitted sources" +
                    " (submitted modules: ${found.ifEmpty { "none" }}) - `chr build` fails with \"Module '$module' not found\"." +
                    " Set module to the submitted root module's name, or submit the file(s) that define '$module'."
            }
            if (compilable.isEmpty()) {
                // Skipping the gate silently reported ok=true on unaudited code
                // (audit 2026-09-01): rell input was supplied but nothing is
                // compilable, which must be an error, not a pass.
                errors += "rell: no compilable .rell files - key each source by a path ending in .rell " +
                    "(e.g. {\"main.rell\": \"module; ...\"}) so the compile and security checks can run."
            } else if (collisions.isEmpty()) {
                // Multi-chain repos keep sibling modules that are SEPARATE chains
                // in chromia.yml (both mounting e.g. __icmf_message); chr compiles
                // per blockchain, so an all-modules compile false-reds with a
                // mount conflict (real-world round 2 D2). When the yaml declares
                // several chains whose modules all match submitted modules,
                // compile each chain's module set separately, like chr does.
                val perChain = perBlockchainModules(yaml, compilable)
                val moduleScopes: List<List<String>?> =
                    if (perChain.size >= 2) {
                        notes += "Multiple blockchains in chromia.yml - compiled per blockchain: " +
                            perChain.entries.joinToString(", ") { (chain, module) -> "$chain (module $module)" } + "."
                        perChain.values.distinct().map { listOf(it) }
                    } else {
                        listOf(null)
                    }
                var allOk = true
                val seenErrors = mutableSetOf<String>()
                val seenWarnings = mutableSetOf<String>()
                moduleScopes.forEach { scope ->
                    runCatching { RellCheck.check(compilable, scope) }.fold(
                        onSuccess = { result ->
                            // Compile notes carry load-bearing context (e.g. "Using your
                            // submitted lib/ft4 sources ...") that was silently dropped
                            // (audit F1 follow-up).
                            if (result.notes.isNotBlank() && result.notes !in notes) notes += result.notes
                            result.errors.forEach { d ->
                                val where = listOfNotNull(d.file, d.line?.toString()).joinToString(":")
                                val line = if (where.isEmpty()) "rell: ${d.text}" else "$where: ${d.text}"
                                if (seenErrors.add(line)) errors += line
                            }
                            result.warnings.forEach { d ->
                                val where = listOfNotNull(d.file, d.line?.toString()).joinToString(":")
                                val line = if (where.isEmpty()) "rell: ${d.text}" else "$where: ${d.text}"
                                if (seenWarnings.add(line)) warnings += line
                            }
                            if (!result.ok) allOk = false
                            // chr compiles WITH the yml's moduleArgs and refuses a
                            // module whose module_args are not configured; this
                            // compile ran without them, so check coverage here.
                            if (result.ok && !usedDefaultYaml) {
                                moduleArgsNotConfigured(yaml, result.requiredModuleArgs, result.modules)
                                    .forEach { (chain, module, fields) ->
                                        val line = moduleArgsNotConfiguredError(chain, module, fields)
                                        if (seenErrors.add(line)) errors += line
                                    }
                            }
                        },
                        onFailure = { e ->
                            allOk = false
                            errors += "rell: compile check failed: ${e.message}"
                        }
                    )
                }
                if (allOk) {
                    val sec = RellSecurityCheck.analyze(compilable, allowAdminModules)
                    sec.findings.forEach { f ->
                        val line = "${f.file}:${f.line}: [${f.severity}] ${f.rule} - ${f.text}. Fix: ${f.fix}"
                        if (f.severity == "CRITICAL" || f.severity == "HIGH") errors += line else warnings += line
                    }
                }
            }
        }
        return Result(ok = errors.isEmpty(), errors = errors, warnings = warnings, notes = notes)
    }

    /**
     * chain name -> its chromia.yml `module`, for per-blockchain compilation
     * (real-world round 2 D2). Empty (= fall back to the single all-modules
     * compile) unless the yaml parses, declares 2+ chains, and EVERY chain's
     * module matches a submitted module - a partial submission must keep the
     * old behavior rather than fail each chain with "module not found".
     */
    /**
     * (chain, declared module, submitted module names) for every
     * `blockchains.<chain>.module` that no submitted file defines. A module is
     * considered present when a submitted module equals it, is one of its
     * submodules (`main.x` for `main` - a directory module compiles with its
     * children), or when a file sits under its directory. `lib.*` modules are
     * vendored, never submitted, and are skipped. Empty when the yaml does not
     * parse or declares no blockchains - those are the validator's findings.
     */
    /**
     * For every chain in `blockchains:`, the compiled modules that declare a
     * `struct module_args` without defaults and have NO entry under that chain's
     * `moduleArgs:` - the exact set `chr build` names in "Missing module_args for
     * module(s): ...". The stablecoin scaffold went through a dry run that said
     * "ready" and died there on the real deploy (2026-09-04): its yml leaves
     * main.oracle_pubkey deliberately unset (commented, so no placeholder key can
     * reach a chain), and no gate checked coverage. Returns
     * (chain, module, fields) triples; only chains whose root module is among
     * the compiled modules are judged (another chain's modules are not its
     * business - multi-chain repos).
     */
    internal fun moduleArgsNotConfigured(
        yaml: String,
        requiredModuleArgs: Map<String, List<String>>,
        compiledModules: Collection<String>
    ): List<Triple<String, String, List<String>>> {
        if (requiredModuleArgs.isEmpty()) return emptyList()
        val root = runCatching { SimpleYaml.parse(yaml) }.getOrNull() as? YamlNode.Mapping ?: return emptyList()
        val blockchains = root.mapping("blockchains") ?: return emptyList()
        return blockchains.entries.flatMap { (chain, node) ->
            val mapping = node as? YamlNode.Mapping ?: return@flatMap emptyList()
            val rootModule = mapping.scalar("module")?.trim().orEmpty()
            if (rootModule.isEmpty() || rootModule !in compiledModules) return@flatMap emptyList()
            val configured = mapping.mapping("moduleArgs")?.entries?.keys.orEmpty()
            requiredModuleArgs.filterKeys { it !in configured }
                .map { (module, fields) -> Triple(chain, module, fields) }
        }
    }

    /** One error line per (chain, module) from [moduleArgsNotConfigured], naming chr's failure and the fix. */
    internal fun moduleArgsNotConfiguredError(chain: String, module: String, fields: List<String>): String =
        "$YAML_PATH: blockchains.$chain.moduleArgs has no `$module` entry, but module $module declares" +
            " `struct module_args` with no default for ${fields.joinToString(", ")} - `chr build` (and so every" +
            " `chr deployment`) fails with \"Missing module_args for module(s): $module\". Add under" +
            " blockchains.$chain.moduleArgs:\n  $module:\n" + fields.joinToString("\n") { "    $it: <value>" } +
            (if (fields.any { it.contains("pubkey") || it.contains("key") })
                "\n(a key field takes the 33-byte compressed public key as x\"...\"; never the test key from test.moduleArgs)"
            else "")

    internal fun declaredModulesNotSubmitted(yaml: String, rellFiles: Map<String, String>): List<Triple<String, String, String>> {
        val root = runCatching { SimpleYaml.parse(yaml) }.getOrNull() as? YamlNode.Mapping ?: return emptyList()
        val blockchains = root.mapping("blockchains") ?: return emptyList()
        if (rellFiles.isEmpty()) return emptyList()
        val normalizedPaths = rellFiles.keys.map { RellCheck.normalizeSourceRoot(it) }
        val submitted = rellFiles.map { (path, content) ->
            RunRellTests.moduleNameForPath(RellCheck.normalizeSourceRoot(path), content)
        }.filter { it.isNotEmpty() }.distinct().sorted()
        val found = submitted.joinToString(", ")
        return blockchains.entries.mapNotNull { (chain, node) ->
            val module = (node as? YamlNode.Mapping)?.scalar("module")?.trim().orEmpty()
            if (module.isEmpty() || module.startsWith("lib.")) return@mapNotNull null
            val dir = module.replace('.', '/') + "/"
            val present = submitted.any { it == module || it.startsWith("$module.") } ||
                normalizedPaths.any { it.startsWith(dir) }
            if (present) null else Triple(chain, module, found)
        }
    }

    internal fun perBlockchainModules(yaml: String, rellFiles: Map<String, String>): Map<String, String> {
        val root = runCatching { SimpleYaml.parse(yaml) }.getOrNull() as? YamlNode.Mapping ?: return emptyMap()
        val blockchains = root.mapping("blockchains") ?: return emptyMap()
        val chainModules = linkedMapOf<String, String>()
        blockchains.entries.forEach { (chain, node) ->
            val module = (node as? YamlNode.Mapping)?.scalar("module")?.trim().orEmpty()
            if (module.isEmpty()) return emptyMap()
            chainModules[chain] = module
        }
        if (chainModules.size < 2) return emptyMap()
        val submittedModules = rellFiles.map { (path, content) ->
            RunRellTests.moduleNameForPath(RellCheck.normalizeSourceRoot(path), content)
        }.toSet()
        if (!chainModules.values.all { it in submittedModules }) return emptyMap()
        return chainModules
    }
}
