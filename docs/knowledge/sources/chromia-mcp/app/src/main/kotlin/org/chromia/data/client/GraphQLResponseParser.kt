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
                    else -> NetworkResult.Error(
                        JsonParsingException("Invalid response format: expected JSON object").message!!
                    )
                }
            },
            onFailure = { e ->
                NetworkResult.Error(JsonParsingException(e.message ?: "Unknown parsing error", e).message!!, e)
            }
        )
    }

    private fun parseJsonObject(jsonObject: JsonObject): JsonResult {
        return jsonObject["errors"]?.let { errors ->
            val errorMessage = extractErrorMessage(errors)
            val errorList = extractErrorList(errors)
            NetworkResult.Error(GraphQLException(errorMessage, errorList).message!!)
        } ?: NetworkResult.Success(jsonObject)
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
