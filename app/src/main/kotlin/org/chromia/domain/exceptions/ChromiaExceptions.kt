package org.chromia.domain.exceptions

sealed class ChromiaException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class NetworkConfigurationException(
    networkName: String,
    availableNetworks: Set<String> = emptySet()
) : ChromiaException(
    "Network '$networkName' not found. Available networks: ${availableNetworks.joinToString(", ")}" +
        (if (availableNetworks.isEmpty()) "" else " - or pass a node URL (https://host:7740) to query that node directly.")
)

class HttpRequestException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null
) : ChromiaException(
    if (statusCode != null) "HTTP $statusCode: $message" else "HTTP Request failed: $message",
    cause
)

class GraphQLException(
    message: String,
    val errors: List<String> = emptyList()
) : ChromiaException("GraphQL Error: $message")

class JsonParsingException(
    message: String,
    cause: Throwable? = null
) : ChromiaException("JSON parsing failed: $message", cause)

class PostchainClientException(
    message: String,
    val blockchainRid: String? = null,
    cause: Throwable? = null
) : ChromiaException(
    if (blockchainRid != null) {
        "Postchain client error for blockchain $blockchainRid: $message"
    } else {
        "Postchain client error: $message"
    },
    cause
)

class GtvConversionException(
    message: String,
    expectedType: String? = null,
    actualType: String? = null
) : ChromiaException(
    if (expectedType != null && actualType != null) {
        "GTV conversion failed: expected $expectedType but got $actualType. $message"
    } else {
        "GTV conversion failed: $message"
    }
)
