package org.chromia.domain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class GraphQLQuery(
    val query: String,
    val variables: Map<String, Any> = emptyMap(),
) {
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("query", query)
        if (variables.isNotEmpty()) {
            put("variables", buildJsonObject {
                variables.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Boolean -> put(key, value)
                        is List<*> -> put(key, value.toString()) // Simplified for now
                        else -> put(key, value.toString())
                    }
                }
            })
        }
    }

    override fun toString(): String = toJsonObject().toString()
}

class GraphQLQueryBuilder {
    private var query: String = ""
    private val variables = mutableMapOf<String, Any>()

    fun query(query: String) = apply { this.query = query }
    fun variable(name: String, value: Any) = apply { this.variables[name] = value }
    fun build(): GraphQLQuery = GraphQLQuery(query, variables.toMap())
}

fun graphqlQuery(block: GraphQLQueryBuilder.() -> Unit): GraphQLQuery =
    GraphQLQueryBuilder().apply(block).build() 