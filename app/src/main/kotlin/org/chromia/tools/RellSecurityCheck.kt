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
        "auth_handler"
    )

    private val MUTATION_REGEX = Regex("""\b(create|update|delete)\b\s""")
    // [ \t]* (not \s*): under MULTILINE, \s* would swallow preceding newlines and
    // shift the reported line number to the blank line above the operation.
    private val OPERATION_REGEX = Regex("""^[ \t]*operation\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(""", RegexOption.MULTILINE)
    private val HEX_SECRET_REGEX = Regex("""x?["']([0-9a-fA-F]{64,})["']""")

    fun analyze(files: Map<String, String>): Result {
        val findings = mutableListOf<Finding>()
        var operationsScanned = 0

        files.forEach { (path, content) ->
            findings += bannedModuleFindings(path, content)
            findings += hardcodedSecretFindings(path, content)
            val ops = scanOperations(path, content)
            operationsScanned += ops.size
            ops.forEach { op ->
                findings += operationFindings(path, op)
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
                if (trimmed.contains(banned)) {
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
            val braceEnd = matchDelimiter(content, braceStart, '{', '}') ?: content.length - 1
            val body = content.substring(braceStart + 1, braceEnd)
            blocks.add(OperationBlock(name, startLine, params, body))
        }
        return blocks
    }

    private fun matchDelimiter(content: String, start: Int, open: Char, close: Char): Int? {
        var depth = 0
        for (i in start until content.length) {
            when (content[i]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    private fun operationFindings(path: String, op: OperationBlock): List<Finding> {
        val findings = mutableListOf<Finding>()
        val hasAuth = AUTH_MARKERS.any { op.body.contains(it) }
        val hasRequire = op.body.contains("require(") || op.body.contains("require_not_empty(")
        val mutates = MUTATION_REGEX.containsMatchIn(op.body)

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
