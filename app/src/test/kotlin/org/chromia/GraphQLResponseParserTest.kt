package org.chromia

import org.chromia.data.client.GraphQLResponseParser
import org.chromia.domain.NetworkResult
import org.chromia.domain.exceptions.GraphQLException
import org.chromia.domain.exceptions.JsonParsingException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GraphQLResponseParserTest {

    @Test
    fun successObjectWithoutErrors() {
        val result = GraphQLResponseParser.parseResponse("""{"data":{"ok":true}}""")
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue(data.containsKey("data"))
    }

    @Test
    fun graphqlErrorsUseFirstMessageAndKeepCause() {
        val result = GraphQLResponseParser.parseResponse(
            """{"errors":[{"message":"field boom"},{"message":"also bad"}]}"""
        )
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("field boom"))
        assertTrue(error.cause is GraphQLException)
        assertEquals(listOf("field boom", "also bad"), (error.cause as GraphQLException).errors)
    }

    @Test
    fun malformedErrorsFieldIsStillAnError() {
        val result = GraphQLResponseParser.parseResponse("""{"errors":{"message":"not-an-array"}}""")
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("GraphQL Error") || error.message.contains("Failed to parse"))
        assertTrue(error.cause is GraphQLException)
    }

    @Test
    fun invalidJsonKeepsParseCause() {
        val result = GraphQLResponseParser.parseResponse("not-json")
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("JSON parsing failed"))
        assertTrue(error.cause is JsonParsingException)
    }

    @Test
    fun jsonArrayIsInvalidFormatWithCause() {
        val result = GraphQLResponseParser.parseResponse("""[{"ok":true}]""")
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("expected JSON object"))
        assertTrue(error.cause is JsonParsingException)
    }

    @Test
    fun jsonPrimitiveIsInvalidFormatWithCause() {
        val result = GraphQLResponseParser.parseResponse("\"hello\"")
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("expected JSON object"))
        assertTrue(error.cause is JsonParsingException)
    }

    @Test
    fun successDoesNotSetCause() {
        val result = GraphQLResponseParser.parseResponse("""{"data":{}}""")
        assertTrue(result is NetworkResult.Success)
    }
}
