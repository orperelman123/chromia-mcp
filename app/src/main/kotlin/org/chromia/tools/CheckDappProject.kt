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

    data class Result(
        val ok: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    ) {
        fun toJson() = buildJsonObject {
            put("ok", ok)
            put("errors", buildJsonArray { errors.forEach { add(JsonPrimitive(it)) } })
            put("warnings", buildJsonArray { warnings.forEach { add(JsonPrimitive(it)) } })
        }
    }

    fun check(yaml: String, rellFiles: Map<String, String>): Result {
        val yml = ChromiaYmlValidator.validate(yaml)
        val errors = mutableListOf<String>()
        val warnings = yml.warnings.map { "$YAML_PATH: $it" }.toMutableList()
        yml.errors.forEach { errors += "$YAML_PATH: $it" }
        if (rellFiles.isEmpty()) {
            errors += "missing .rell file contents"
        }
        rellFiles.forEach { (path, content) ->
            val label = path.trim().ifEmpty { "rell" }
            Ft4ImportCheck.scan(content).errors.forEach { err ->
                errors += "$label: $err"
            }
        }
        return Result(ok = errors.isEmpty(), errors = errors, warnings = warnings)
    }
}
