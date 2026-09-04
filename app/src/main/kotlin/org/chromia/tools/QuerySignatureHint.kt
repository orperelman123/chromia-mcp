package org.chromia.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Turns a node's bare "Invalid argument(s): account" / unknown-query refusal
 * into the fix, using the chain's own `rell.get_app_structure` answer.
 *
 * Live stablecoin chain (2026-09-04): `get_cdp` called with `account` instead of
 * `owner` came back as "Query 'get_cdp' failed: Invalid argument(s): account" -
 * true, and useless without the signature the node itself publishes. The
 * structure is one extra read, paid only on that class of failure.
 */
object QuerySignatureHint {
    /** Node messages that mean "the query exists but you called it wrong" or "no such query". */
    private val TRIGGER = Regex(
        """Invalid argument(?:\(s\))?|Unknown query|No such query|Query '[^']*' not found|query .{0,40} not found|Missing argument""",
        RegexOption.IGNORE_CASE
    )

    fun applies(nodeError: String): Boolean = TRIGGER.containsMatchIn(nodeError)

    /** One line naming the query's real parameters, or the mounted queries when [queryName] is not one of them; null when the structure is unusable. */
    fun hint(structure: JsonObject, queryName: String, provided: Collection<String>): String? {
        val modules = structure["modules"] as? JsonObject ?: return null
        val signatures = linkedMapOf<String, List<Pair<String, String>>>() // mount -> (name, type)
        modules.values.forEach { module ->
            val queries = (module as? JsonObject)?.get("queries") as? JsonObject ?: return@forEach
            queries.forEach { (name, def) ->
                val obj = def as? JsonObject ?: return@forEach
                val mount = obj["mount"]?.jsonPrimitive?.contentOrNull ?: name
                val params = (obj["parameters"] as? JsonArray).orEmpty().mapNotNull { p ->
                    val po = p as? JsonObject ?: return@mapNotNull null
                    val pn = po["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    pn to typeName(po["type"])
                }
                signatures[mount] = params
            }
        }
        if (signatures.isEmpty()) return null
        val params = signatures[queryName]
        if (params != null) {
            val expected = if (params.isEmpty()) "no arguments" else params.joinToString(", ", "(", ")") { "${it.first}: ${it.second}" }
            val missing = params.map { it.first }.filter { it !in provided }
            val unknown = provided.filter { p -> params.none { it.first == p } }
            val detail = buildList {
                if (unknown.isNotEmpty()) add("not a parameter: ${unknown.joinToString(", ")}")
                if (missing.isNotEmpty()) add("missing: ${missing.joinToString(", ")}")
            }.joinToString("; ")
            return "The chain's `$queryName` takes $expected (from rell.get_app_structure)" +
                (if (detail.isEmpty()) "" else " - $detail") +
                ". Argument names must match exactly; byte_array values are hex strings."
        }
        val names = signatures.keys
        val similar = names.filter { it.contains(queryName, ignoreCase = true) || queryName.contains(it, ignoreCase = true) || editDistance(it, queryName) <= 3 }
        val listed = (similar + names.filterNot { it in similar }).take(30)
        return "No query is mounted as `$queryName` on this chain. " +
            (if (similar.isNotEmpty()) "Did you mean ${similar.take(3).joinToString(", ") { "`$it`" }}? " else "") +
            "Mounted queries (${names.size}): ${listed.joinToString(", ")}" + (if (names.size > listed.size) ", ..." else "") +
            " (from rell.get_app_structure; use mount.name for queries in mounted modules)."
    }

    private fun typeName(type: Any?): String = when (type) {
        is JsonPrimitive -> type.contentOrNull ?: "?"
        is JsonObject -> {
            val kind = type["type"]?.jsonPrimitive?.contentOrNull
            when (kind) {
                "nullable" -> typeName(type["value"]) + "?"
                "list" -> "list<${typeName(type["value"])}>"
                "set" -> "set<${typeName(type["value"])}>"
                "map" -> "map<${typeName(type["key"])}, ${typeName(type["value"])}>"
                "tuple" -> "tuple"
                "struct" -> type["name"]?.jsonPrimitive?.contentOrNull ?: "struct"
                "entity" -> type["name"]?.jsonPrimitive?.contentOrNull ?: "entity"
                "enum" -> type["name"]?.jsonPrimitive?.contentOrNull ?: "enum"
                null -> type.entries.firstOrNull()?.value?.let { if (it is JsonPrimitive) it.contentOrNull else null } ?: "?"
                else -> kind
            }
        }
        else -> "?"
    }

    private fun editDistance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        }
        return d[a.length][b.length]
    }
}
