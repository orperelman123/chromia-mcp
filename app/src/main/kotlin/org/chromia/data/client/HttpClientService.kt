package org.chromia.data.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.GraphQLQuery
import org.chromia.domain.JsonResult
import org.chromia.domain.NetworkResult
import org.chromia.domain.exceptions.HttpRequestException

class HttpClientService(private val config: ChromiaConfig) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.httpTimeouts.requestTimeout.inWholeMilliseconds
            connectTimeoutMillis = config.httpTimeouts.connectTimeout.inWholeMilliseconds
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun executeGraphQLQuery(
        query: GraphQLQuery,
        network: String?
    ): JsonResult = withContext(Dispatchers.IO) {
        val targetNetwork = network ?: config.defaultNetwork

        runCatching {
            httpClient.post(config.explorerUrl) {
                parameter("network", targetNetwork)
                setBody(query.toString())
            }
        }.fold(
            onSuccess = { response ->
                when {
                    !response.status.isSuccess() -> {
                        val error = HttpRequestException(response.status.description, response.status.value)
                        NetworkResult.Error(error.message!!)
                    }

                    else -> GraphQLResponseParser.parseResponse(response.body())
                }
            },
            onFailure = { e ->
                NetworkResult.Error("Request failed: ${e.message}", e)
            }
        )
    }
}
