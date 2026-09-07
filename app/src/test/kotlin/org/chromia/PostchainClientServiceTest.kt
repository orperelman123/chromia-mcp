package org.chromia

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
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

    /**
     * THE ARGUMENT CONVERSION, BOUND BY THE REAL CHAIN.
     *
     * This used to hand `PostchainClientService` a trailing-lambda
     * `BlockchainQueryClient` that inspected the [net.postchain.gtv.Gtv] it was
     * given and then handed back a dictionary the test had written itself. It
     * proved that `listMapAndPrimitivesToGtv` produced a Gtv shaped the way the
     * test expected - which is a restatement of the test, not of Rell.
     *
     * The live version is strictly stronger. `ft4.get_assets_filtered` takes a
     * nested struct holding a `list<byte_array>` and a `text`, alongside a
     * top-level `integer`. The Economy Chain BINDS those arguments: a conversion
     * that produced the wrong Gtv type cannot bind at all (the node answers
     * "Missing struct attribute value" or a type error), and a conversion that
     * bound the wrong VALUES comes back with the wrong asset. The assertion is
     * the chain's, not the fixture's.
     */
    @Test
    fun structuredArgumentsAreConvertedAndBoundByTheLiveEconomyChain() {
        LiveChromia.requireLive(
            "binds a nested dict holding a list<byte_array> and a text, plus an integer, on the " +
                "live Economy Chain - the conversion listMapAndPrimitivesToGtv performs"
        )
        val service = LiveChromia.postchain()

        // What is actually on the chain right now. No arguments, so nothing here
        // depends on the conversion under test.
        val assetResult = service.executeBlockchainQuery(
            LiveChromia.NETWORK, LiveChromia.economyChainRid, "get_chr_asset", emptyMap()
        )
        assertTrue(assetResult is NetworkResult.Success, "live get_chr_asset failed: $assetResult")
        val asset = (assetResult as NetworkResult.Success).data
        val assetIdHex = asset.getValue("id").jsonPrimitive.content
        val assetName = asset.getValue("name").jsonPrimitive.content

        // Now the shape that exercises every branch of the converter at once.
        val filtered = service.executeBlockchainQuery(
            LiveChromia.NETWORK,
            LiveChromia.economyChainRid,
            "ft4.get_assets_filtered",
            mapOf(
                "asset_filter" to mapOf(
                    "ids" to listOf(assetIdHex.hexStringToByteArray()),
                    "name" to assetName,
                    "symbol" to null,
                    "type" to null
                ),
                "page_size" to 2,
                "page_cursor" to null
            )
        )
        assertTrue(filtered is NetworkResult.Success, "live filtered query failed: $filtered")
        val page = (filtered as NetworkResult.Success).data
        val rows = page.getValue("data").jsonArray
        assertEquals(
            1, rows.size,
            "the id list and the name both had to bind for exactly this asset to come back: $page"
        )
        assertEquals(assetIdHex, rows[0].jsonObject.getValue("id").jsonPrimitive.content)
        assertEquals(assetName, rows[0].jsonObject.getValue("name").jsonPrimitive.content)
    }

    /**
     * The same query with a filter the chain cannot match: a REAL empty page,
     * which is the shape a fixture used to be written for. `data` is an empty
     * array and `next_cursor` is a real null; both come off the wire.
     */
    @Test
    fun aFilterThatMatchesNothingReturnsARealEmptyPage() {
        LiveChromia.requireLive("asks the live Economy Chain for an asset name that does not exist")
        val result = LiveChromia.postchain().executeBlockchainQuery(
            LiveChromia.NETWORK,
            LiveChromia.economyChainRid,
            "ft4.get_assets_filtered",
            mapOf(
                "asset_filter" to mapOf(
                    "ids" to null,
                    "name" to "no-such-asset-8f2b41c9",
                    "symbol" to null,
                    "type" to null
                ),
                "page_size" to 2,
                "page_cursor" to null
            )
        )
        assertTrue(result is NetworkResult.Success, "live empty-page query failed: $result")
        val page = (result as NetworkResult.Success).data
        assertTrue(page.getValue("data").jsonArray.isEmpty(), "expected an empty page, got $page")
        assertTrue(page.getValue("next_cursor") is JsonNull, "a last page has a null cursor: $page")
    }

    /**
     * The chain-side failure path, produced by a real query name no chain
     * serves. The node answers `Unknown query`; the service must surface it as
     * an error carrying the [PostchainClientException] rather than throwing.
     */
    @Test
    fun anUnknownQueryNameIsTheChainsErrorNotAnException() {
        LiveChromia.requireLive("asks the live Economy Chain for a query name it does not serve")
        val result = LiveChromia.postchain().executeBlockchainQuery(
            LiveChromia.NETWORK, LiveChromia.economyChainRid, "no_such_query_8f2b41c9", emptyMap()
        )
        assertTrue(result is NetworkResult.Error, "expected the node's refusal, got $result")
        val error = result as NetworkResult.Error
        assertTrue(error.cause is PostchainClientException, "cause was ${error.cause}")
        assertTrue(
            error.message.contains("no_such_query_8f2b41c9") || error.message.lowercase().contains("query"),
            "the node's own words must survive into the error: ${error.message}"
        )
    }
}
