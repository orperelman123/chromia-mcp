package org.chromia.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class GraphQLQuery(
    val query: String,
    val variables: Map<String, Any> = emptyMap(),
) {
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("query", query)
        val present = variables.mapNotNull { (key, value) ->
            presentVariable(value)?.let { key to it }
        }
        if (present.isNotEmpty()) {
            put(
                "variables",
                buildJsonObject {
                    present.forEach { (key, value) ->
                        put(key, encodeVariable(value))
                    }
                }
            )
        }
    }

    override fun toString(): String = toJsonObject().toString()
}

/**
 * Drops blank strings and empty / all-blank lists so optional GraphQL
 * filters such as excludeAccounts=[], brids=['  '], brid='' are omitted
 * instead of being sent as empty values.
 */
internal fun presentVariable(value: Any): Any? = when (value) {
    is String -> value.takeIf { it.isNotBlank() }
    is List<*> -> {
        val items = value.mapNotNull { item ->
            when (item) {
                null -> null
                is String -> item.trim().takeIf { it.isNotEmpty() }
                else -> item
            }
        }
        items.takeIf { it.isNotEmpty() }
    }
    else -> value
}

internal fun encodeVariable(value: Any): kotlinx.serialization.json.JsonElement = when (value) {
    is String -> JsonPrimitive(value)
    is Int -> JsonPrimitive(value)
    is Long -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Double -> JsonPrimitive(value)
    is List<*> -> {
        val items = value.mapNotNull { item ->
            when (item) {
                null -> null
                is String -> item.trim().takeIf { it.isNotEmpty() }?.let { encodeVariable(it) }
                else -> encodeVariable(item)
            }
        }
        JsonArray(items)
    }
    else -> JsonPrimitive(value.toString())
}

class GraphQLQueryBuilder {
    private var query: String = ""
    private val variables = mutableMapOf<String, Any>()

    fun query(query: String) = apply { this.query = query }
    fun variable(name: String, value: Any) = apply {
        presentVariable(value)?.let { variables[name] = it }
    }
    fun build(): GraphQLQuery = GraphQLQuery(query, variables.toMap())
}

fun graphqlQuery(block: GraphQLQueryBuilder.() -> Unit): GraphQLQuery =
    GraphQLQueryBuilder().apply(block).build()
