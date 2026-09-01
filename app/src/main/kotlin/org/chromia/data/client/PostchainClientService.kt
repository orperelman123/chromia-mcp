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

fun interface BlockchainQueryClient {
    fun query(blockchainRid: BlockchainRid, queryName: String, arguments: Gtv): Gtv
}

class PostchainClientService(
    private val config: ChromiaConfig,
    /** Test seam for the client cache; production uses [createRealClient]. */
    private val clientFactory: ((List<String>, BlockchainRid) -> CachedQueryClient)? = null,
    // Last so existing trailing-lambda test callers keep SAM-converting to it.
    private val queryClient: BlockchainQueryClient? = null
) {

    /** A per-chain query client plus how to release it when evicted. */
    class CachedQueryClient(
        val client: net.postchain.client.core.PostchainQuery,
        val close: () -> Unit
    )

    companion object {
        internal const val MAX_CACHED_CLIENTS = 32

        /**
         * Eviction used to close the evictee immediately, so with >32 live keys a
         * concurrent query on the evicted client failed spuriously (audit round 4
         * F4). There is no per-call refcount; instead the close is deferred long
         * enough for any in-flight query to finish. The evictee is out of the map
         * at once - new calls build a fresh client.
         */
        internal const val EVICTION_CLOSE_GRACE_MS = 30_000L

        private val evictionCloser =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "postchain-client-eviction-closer").apply { isDaemon = true }
            }
    }

    /** Test seam; production keeps [EVICTION_CLOSE_GRACE_MS]. */
    internal var evictionCloseGraceMs: Long = EVICTION_CLOSE_GRACE_MS

    // Every chromia_dapp_query used to build a fresh StandardChromiaClient (whose
    // constructor eagerly creates a directory-chain PostchainClientImpl with its
    // own Apache HC5 connection pool) plus a second per-chain PostchainClientImpl
    // via getClient(), and close neither - a 24/7 server accrued sockets and heap
    // on every call (audit F2; the default request strategy's close() is even a
    // no-op, so per-call closing could not release the pools). Cache and reuse:
    // one StandardChromiaClient per endpoint pool, one per-chain client per
    // (endpoint pool, brid) in a bounded LRU whose evictees are closed.
    private val chromiaClients =
        java.util.concurrent.ConcurrentHashMap<String, StandardChromiaClient>()

    // Access-order LRU; guarded by its own monitor (LinkedHashMap is not thread-safe).
    private val cachedClients =
        object : LinkedHashMap<String, CachedQueryClient>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, CachedQueryClient>
            ): Boolean {
                if (size <= MAX_CACHED_CLIENTS) return false
                // Deferred: a query may still be in flight on the evictee (audit
                // round 4 F4). See EVICTION_CLOSE_GRACE_MS.
                val evicted = eldest.value
                evictionCloser.schedule(
                    { runCatching { evicted.close() } },
                    evictionCloseGraceMs,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                return true
            }
        }

    internal fun cachedClientCount(): Int = synchronized(cachedClients) { cachedClients.size }

    private fun queryClientFor(
        urls: List<String>,
        blockchainRid: BlockchainRid
    ): net.postchain.client.core.PostchainQuery {
        val key = "${urls.joinToString(",")}|${blockchainRid.toHex()}"
        synchronized(cachedClients) { cachedClients[key] }?.let { return it.client }
        // Creation performs signer-node discovery over the network - keep it
        // outside the lock so a slow node does not stall unrelated cached calls.
        val created = (clientFactory ?: ::createRealClient)(urls, blockchainRid)
        synchronized(cachedClients) {
            cachedClients[key]?.let { raced ->
                runCatching { created.close() }
                return raced.client
            }
            cachedClients[key] = created
            return created.client
        }
    }

    private fun createRealClient(urls: List<String>, blockchainRid: BlockchainRid): CachedQueryClient {
        val chromiaClient = chromiaClients.computeIfAbsent(urls.joinToString(",")) {
            StandardChromiaClient(EndpointPool.default(urls))
        }
        val client = chromiaClient.getClient(blockchainRid)
        return CachedQueryClient(client) { client.close() }
    }

    fun executeBlockchainQuery(
        network: String?,
        blockchainRid: BlockchainRid,
        queryName: String?,
        arguments: Map<String, Any?>
    ): JsonResult = runCatching {
        val query = queryName ?: "rell.get_app_structure"
        val networkName = network ?: config.defaultNetwork

        val urls = config.predefinedNetworks[networkName]
            ?: throw NetworkConfigurationException(networkName, config.predefinedNetworks.keys)

        val gtvArgs = listMapAndPrimitivesToGtv(arguments)
        val queryResult = queryClient?.query(blockchainRid, query, gtvArgs)
            ?: queryClientFor(urls, blockchainRid).query(query, gtvArgs)

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
            NetworkResult.Error(error.message!!, error)
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
