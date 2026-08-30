package org.chromia

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvFactory
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.NetworkResult
import org.chromia.domain.exceptions.NetworkConfigurationException
import org.chromia.domain.exceptions.PostchainClientException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PostchainClientServiceTest {

    private val rid = BlockchainRid.buildFromHex(
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    )

    @Test
    fun unknownNetworkAttachesPostchainClientExceptionCause() {
        val service = PostchainClientService(ChromiaConfig())
        val result = service.executeBlockchainQuery(
            "not-a-real-network",
            rid,
            "rell.get_app_structure",
            emptyMap()
        )
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("not-a-real-network"))
        assertTrue(error.cause is PostchainClientException)
        assertTrue(error.cause?.cause is NetworkConfigurationException)
    }

    @Test
    fun missingDefaultNetworkInConfigIsErrorWithoutLiveChain() {
        val service = PostchainClientService(
            ChromiaConfig(
                defaultNetwork = "staging",
                predefinedNetworks = emptyMap()
            )
        )
        val result = service.executeBlockchainQuery(
            null,
            rid,
            "rell.get_app_structure",
            emptyMap()
        )
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("staging"))
        assertTrue(error.cause is PostchainClientException)
        assertTrue(error.cause?.cause is NetworkConfigurationException)
    }

    @Test
    fun toKotlinxJsonParsesNestedGsonWithoutLiveChain() {
        val service = PostchainClientService(ChromiaConfig())
        val gson = com.google.gson.JsonParser.parseString(
            """{"name":"CHR","decimals":6,"ok":true,"nested":{"a":1},"tags":["x",null],"empty":null}"""
        )
        val converted = with(service) { gson.toKotlinxJson() }
        assertTrue(converted is JsonObject)
        val obj = converted as JsonObject
        assertEquals("CHR", obj.getValue("name").jsonPrimitive.content)
        assertEquals("6", obj.getValue("decimals").jsonPrimitive.content)
        assertEquals(true, obj.getValue("ok").jsonPrimitive.boolean)
        assertEquals("1", obj.getValue("nested").jsonObject.getValue("a").jsonPrimitive.content)
        val tags = obj.getValue("tags").jsonArray
        assertEquals("x", tags[0].jsonPrimitive.content)
        assertTrue(tags[1] is JsonNull)
        assertTrue(obj.getValue("empty") is JsonNull)

        val array = with(service) {
            com.google.gson.JsonParser.parseString("""["a",2,false]""").toKotlinxJson()
        }
        assertTrue(array is JsonArray)
        assertEquals("a", (array as JsonArray)[0].jsonPrimitive.content)
        assertEquals("2", array[1].jsonPrimitive.content)
        assertEquals(false, array[2].jsonPrimitive.boolean)
    }

    @Test
    fun fakeQueryClientConvertsArgsViaListMapAndDecodesSuccessWithoutLiveNode() {
        var capturedQuery: String? = null
        var capturedRid: String? = null
        val service = PostchainClientService(ChromiaConfig()) { blockchainRid, queryName, args ->
            capturedRid = blockchainRid.toHex()
            capturedQuery = queryName
            val dict = args.asDict()
            assertEquals("CHR", dict.getValue("name").asString())
            assertEquals(10L, dict.getValue("page_size").asInteger())
            assertEquals(listOf("a", "b"), dict.getValue("ids").asArray().map { it.asString() })
            assertEquals("test", dict.getValue("meta").asDict().getValue("source").asString())
            GtvFactory.gtv(
                mapOf(
                    "name" to GtvFactory.gtv("CHR"),
                    "decimals" to GtvFactory.gtv(6L),
                    "symbol" to GtvFactory.gtv("ok")
                )
            )
        }
        val result = service.executeBlockchainQuery(
            "testnet",
            rid,
            "ft4.get_assets_by_name",
            mapOf(
                "name" to "CHR",
                "page_size" to 10,
                "ids" to listOf("a", "b"),
                "meta" to mapOf("source" to "test")
            )
        )
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("CHR", data.getValue("name").jsonPrimitive.content)
        assertEquals("6", data.getValue("decimals").jsonPrimitive.content)
        assertEquals("ok", data.getValue("symbol").jsonPrimitive.content)
        assertEquals("ft4.get_assets_by_name", capturedQuery)
        assertEquals(rid.toHex(), capturedRid)
    }
}
