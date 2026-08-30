package org.chromia.data.client

import kotlinx.serialization.json.*
import org.chromia.domain.JsonResult
import org.chromia.domain.NetworkResult
import org.chromia.domain.exceptions.GraphQLException
import org.chromia.domain.exceptions.JsonParsingException

object GraphQLResponseParser {

    fun parseResponse(responseText: String): JsonResult {
        return runCatching {
            Json.parseToJsonElement(responseText)
        }.fold(
            onSuccess = { jsonElement ->
                when (jsonElement) {
                    is JsonObject -> parseJsonObject(jsonElement)
                    else -> {
                        val error = JsonParsingException("Invalid response format: expected JSON object")
                        NetworkResult.Error(error.message!!, error)
                    }
                }
            },
            onFailure = { e ->
                val error = JsonParsingException(e.message ?: "Unknown parsing error", e)
                NetworkResult.Error(error.message!!, error)
            }
        )
    }

    private fun parseJsonObject(jsonObject: JsonObject): JsonResult {
        val errors = jsonObject["errors"]
        // "errors": null and "errors": [] mean no error per GraphQL spec.
        if (errors == null || errors is JsonNull || (errors is JsonArray && errors.isEmpty())) {
            return NetworkResult.Success(jsonObject)
        }
        val errorMessage = extractErrorMessage(errors)
        val errorList = extractErrorList(errors)
        val error = GraphQLException(errorMessage, errorList)
        return NetworkResult.Error(error.message!!, error)
    }

    private fun extractErrorMessage(errors: JsonElement): String {
        return try {
            errors.jsonArray.firstOrNull()
                ?.jsonObject?.get("message")
                ?.jsonPrimitive?.content
                ?: "Unknown GraphQL error"
        } catch (e: Exception) {
            "Failed to parse GraphQL error: ${e.message}"
        }
    }

    private fun extractErrorList(errors: JsonElement): List<String> {
        return try {
            errors.jsonArray.mapNotNull { error ->
                error.jsonObject["message"]?.jsonPrimitive?.content
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
