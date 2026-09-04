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

class HttpClientService(
    private val config: ChromiaConfig,
    httpClient: HttpClient? = null
) {
    constructor(config: ChromiaConfig, engine: io.ktor.client.engine.HttpClientEngine) : this(
        config,
        HttpClient(engine)
    )

    private val httpClient = httpClient ?: createProductionClient(config)

    companion object {
        /**
         * CIO client used in production when no client/engine is injected.
         * ContentNegotiation + HttpTimeout + JSON defaultRequest.
         */
        fun createProductionClient(config: ChromiaConfig): HttpClient = HttpClient(CIO) {
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
    }

    suspend fun executeGraphQLQuery(
        query: GraphQLQuery,
        network: String?
    ): JsonResult = withContext(Dispatchers.IO) {
        // `Mainnet` is not a different network: fold case/whitespace onto the
        // canonical name before the typo gate below (DX audit 2026-09-04).
        val targetNetwork = network?.trim()?.let { asked ->
            config.predefinedNetworks.keys.firstOrNull { it.equals(asked, ignoreCase = true) } ?: asked
        } ?: config.defaultNetwork

        // The explorer takes a network NAME as a query parameter and is not
        // guaranteed to reject unknown values - a typo ("tesnet") could come
        // back as data from the explorer's default network labelled as the
        // requested one (reality audit D5). The postchain path validates the
        // name against predefinedNetworks; do the same here. There is no
        // custom-URL form on this path: the explorer endpoint is fixed and
        // node URLs only make sense for node-direct tools.
        if (targetNetwork !in config.predefinedNetworks) {
            val valid = config.predefinedNetworks.keys.joinToString(", ")
            val urlHint = if (
                targetNetwork.startsWith("http://") || targetNetwork.startsWith("https://")
            ) {
                " Direct node URLs work only for node-direct tools (chromia_dapp_query / " +
                    "verify_deployment); explorer analytics tools take a network name."
            } else {
                ""
            }
            return@withContext NetworkResult.Error(
                "Unknown network \"$targetNetwork\" - explorer-backed tools accept: $valid.$urlHint",
                IllegalArgumentException("unknown explorer network: $targetNetwork")
            )
        }

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
                        // The public explorer service rejects network=testnet with a 4xx
                        // (upstream limitation, docs/UPSTREAM.md #9); append a hint so the
                        // caller is not left with an opaque "Bad Request".
                        val testnetHint = if (
                            response.status.value in 400..499 &&
                            targetNetwork.equals("testnet", ignoreCase = true)
                        ) {
                            " (the public explorer service currently rejects network=testnet" +
                                " (upstream limitation, see docs/UPSTREAM.md); use mainnet or a" +
                                " direct node query via chromia_dapp_query)"
                        } else {
                            ""
                        }
                        NetworkResult.Error(error.message!! + testnetHint, error)
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
