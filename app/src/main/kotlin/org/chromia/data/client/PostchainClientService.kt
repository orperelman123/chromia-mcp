package org.chromia.data.client

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.StandardChromiaClient
import net.postchain.gtv.*
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.JsonResult
import org.chromia.domain.NetworkResult
import org.chromia.domain.exceptions.NetworkConfigurationException
import org.chromia.domain.exceptions.PostchainClientException

class PostchainClientService(private val config: ChromiaConfig) {

    fun executeBlockchainQuery(
        network: String?,
        blockchainRid: BlockchainRid,
        queryName: String?,
        arguments: Map<String, Any>
    ): JsonResult = runCatching {
        val query = queryName ?: "rell.get_app_structure"
        val networkName = network ?: config.defaultNetwork

        val urls = config.predefinedNetworks[networkName]
            ?: throw NetworkConfigurationException(networkName, config.predefinedNetworks.keys)

        val dcClient = StandardChromiaClient(EndpointPool.default(urls)).getClient(blockchainRid)

        val queryResult = dcClient.query(query, listMapAndPrimitivesToGtv(arguments))

        val gsonJsonElement = make_gtv_gson().toJsonTree(queryResult)
        val kotlinxJsonElement = gsonJsonElement.toKotlinxJson()

        val jsonObject = kotlinxJsonElement as? JsonObject ?: JsonObject(mapOf("data" to kotlinxJsonElement))

        NetworkResult.Success(jsonObject)
    }.fold(
        onSuccess = { it },
        onFailure = { e ->
            val error = PostchainClientException(
                e.message ?: "Unknown error",
                blockchainRid.toHex(),
                e
            )
            NetworkResult.Error(error.message!!, e)
        }
    )

    fun com.google.gson.JsonElement.toKotlinxJson(): JsonElement = when {
        isJsonNull -> JsonNull
        isJsonPrimitive -> {
            val prim = asJsonPrimitive
            when {
                prim.isBoolean -> JsonPrimitive(prim.asBoolean)
                prim.isNumber -> JsonPrimitive(prim.asNumber)
                prim.isString -> JsonPrimitive(prim.asString)
                else -> JsonNull
            }
        }
        isJsonObject -> {
            JsonObject(
                asJsonObject.entrySet().associate { (key, value) ->
                    key to value.toKotlinxJson()
                }
            )
        }
        isJsonArray -> {
            JsonArray(
                asJsonArray.map { it.toKotlinxJson() }
            )
        }
        else -> JsonNull
    }
}
