package org.chromia.tools

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.EnqueueTransactionResult
import net.postchain.common.tx.TransactionStatus
import net.postchain.concurrent.util.get
import net.postchain.core.BlockchainEngine
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.makeStrictGtvGson
import net.postchain.gtx.GtxQuery
import net.postchain.gtx.NON_STRICT_QUERY_ARGUMENT
import java.util.concurrent.TimeUnit

/**
 * Minimal Postchain REST facade over a locally running in-process chain,
 * served with ktor on 127.0.0.1 for the `local_chain_up` tool.
 *
 * Why not postchain's own RestApi: it is http4k-based and its 6.53 ABI cannot
 * coexist with postchain-client's required http4k 6.0.1.0 (both crash with
 * NoSuchMethodError on the other's version - verified empirically 2026-09-02;
 * see the pin comment in build.gradle.kts). This bridge implements the subset
 * agents and postchain clients actually use, with the same paths, bodies and
 * semantics as postchain's RestApi (source-mirrored from RestApi.kt /
 * PostchainEBFTModel.kt at 3.49.x):
 *
 *   GET  /brid/iid_0                     -> text: blockchain RID hex
 *   GET  /query/{brid}?type=name&a=b     -> JSON query result
 *   POST /query/{brid}  {"type": ...}    -> JSON query result
 *   POST /query_gtv/{brid} (GTV binary)  -> GTV binary query result
 *   POST /tx/{brid}    {"tx": "<hex>"}   -> {} (enqueued)
 *   GET  /tx/{brid}/{txRid}/status       -> {"status": "waiting|confirmed|rejected|unknown"}
 */
internal class LocalChainRestBridge(
    private val gateway: ChainGateway,
    private val brid: String,
    val port: Int
) : AutoCloseable {

    constructor(chainEngine: BlockchainEngine, brid: String, port: Int) :
        this(EngineGateway(chainEngine), brid, port)

    companion object {
        const val QUERY_TIMEOUT_SECONDS = 30L
    }

    /**
     * The narrow slice of a running chain the REST facade needs - lets unit
     * tests exercise the HTTP surface without a database-backed node.
     */
    internal interface ChainGateway {
        fun query(name: String, args: Gtv): Gtv
        fun queryWithHeight(name: String, args: Gtv): kotlin.Pair<Gtv, Long>
        /** Throws [DuplicateTx], [QueueFull], or UserMistake on refusal. */
        fun postTransaction(raw: ByteArray)
        fun transactionStatus(txRid: ByteArray): TransactionStatus
    }

    internal class DuplicateTx(message: String) : RuntimeException(message)
    internal class QueueFull(message: String) : RuntimeException(message)

    /** Production gateway over the in-process Postchain engine. */
    internal class EngineGateway(private val chainEngine: BlockchainEngine) : ChainGateway {
        override fun query(name: String, args: Gtv): Gtv =
            chainEngine.getBlockQueries().query(name, args)
                .toCompletableFuture().get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        override fun queryWithHeight(name: String, args: Gtv): kotlin.Pair<Gtv, Long> =
            chainEngine.getBlockQueries().queryWithHeight(name, args)
                .toCompletableFuture().get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        /** Mirrors PostchainEBFTModel.postTransaction (minus metrics). */
        override fun postTransaction(raw: ByteArray) {
            val tx = chainEngine.getConfiguration().getTransactionFactory().decodeAndValidateTransaction(raw)
            if (tx.isSpecial()) throw UserMistake("Cannot post special transaction")
            if (chainEngine.getBlockQueries().isTransactionConfirmed(tx.getRID()).get()) {
                throw DuplicateTx("Transaction already in database")
            }
            when (chainEngine.getTransactionQueue().enqueue(tx)) {
                EnqueueTransactionResult.FULL -> throw QueueFull("Transaction queue is full")
                EnqueueTransactionResult.INVALID -> throw UserMistake("Transaction is invalid")
                EnqueueTransactionResult.DUPLICATE -> throw DuplicateTx("Transaction already in queue")
                EnqueueTransactionResult.OK -> Unit
            }
        }

        /** Mirrors PostchainEBFTModel.getStatus: queue first, then confirmation. */
        override fun transactionStatus(txRid: ByteArray): TransactionStatus {
            val queued = chainEngine.getTransactionQueue().getTransactionStatus(txRid)
            if (queued != TransactionStatus.UNKNOWN) return queued
            return if (chainEngine.getBlockQueries().isTransactionConfirmed(txRid).get()) {
                TransactionStatus.CONFIRMED
            } else {
                TransactionStatus.UNKNOWN
            }
        }
    }

    private val gson = makeStrictGtvGson()

    private val server: EmbeddedServer<*, *> = embeddedServer(CIO, host = "127.0.0.1", port = port) {
        routing {
            get("/brid/iid_0") {
                call.respondText(brid, ContentType.Text.Plain)
            }
            get("/query/{brid}") {
                handle(call.parameters["brid"]) {
                    val params = call.request.queryParameters
                    val type = params["type"] ?: throw UserMistake("Missing query type")
                    val args = params.entries()
                        .filter { it.key != "type" }
                        .associate { (key, values) ->
                            val value = values.singleOrNull() ?: throw UserMistake("Repeated query argument: $key")
                            key to stringParamToGtv(value)
                        }
                    val result = gateway.query(type, gtv(args + (NON_STRICT_QUERY_ARGUMENT to gtv(true))))
                    call.respondText(gson.toJson(result, Gtv::class.java), ContentType.Application.Json)
                }
            }
            post("/query/{brid}") {
                handle(call.parameters["brid"]) {
                    val body = call.receive<ByteArray>().toString(Charsets.UTF_8)
                    val parsed = gson.fromJson(body, Gtv::class.java) ?: throw UserMistake("Empty query body")
                    val dict = runCatching { parsed.asDict() }.getOrElse { throw UserMistake("Query body must be a JSON object") }
                    val type = dict["type"] ?: throw UserMistake("Missing query type")
                    val args = dict.filterKeys { it != "type" } + (NON_STRICT_QUERY_ARGUMENT to gtv(true))
                    val result = gateway.query(type.asString(), gtv(args))
                    call.respondText(gson.toJson(result, Gtv::class.java), ContentType.Application.Json)
                }
            }
            post("/query_gtv/{brid}") {
                handle(call.parameters["brid"]) {
                    val body = call.receive<ByteArray>()
                    val gtxQuery = runCatching { GtxQuery.decode(body) }
                        .getOrElse { throw UserMistake("Invalid GTV data") }
                    val resultWithHeight: kotlin.Pair<Gtv, Long> =
                        gateway.queryWithHeight(gtxQuery.name, gtxQuery.args)
                    call.response.header("X-Block-Height", resultWithHeight.second.toString())
                    call.respondBytes(GtvEncoder.encodeGtv(resultWithHeight.first), ContentType.Application.OctetStream)
                }
            }
            post("/tx/{brid}") {
                handle(call.parameters["brid"]) {
                    val bytes = call.receive<ByteArray>()
                    val raw = if (call.request.contentType().match(ContentType.Application.Json)) {
                        val dict = gson.fromJson(bytes.toString(Charsets.UTF_8), Gtv::class.java)
                            ?.let { runCatching { it.asDict() }.getOrNull() }
                            ?: throw UserMistake("Transaction body must be a JSON object")
                        val hex = dict["tx"]?.asString() ?: throw UserMistake("Missing tx field")
                        runCatching { hex.hexStringToByteArray() }.getOrElse { throw UserMistake("Invalid tx hex") }
                    } else {
                        bytes
                    }
                    gateway.postTransaction(raw)
                    call.respondText("{}", ContentType.Application.Json)
                }
            }
            get("/tx/{brid}/{txRid}/status") {
                handle(call.parameters["brid"]) {
                    val txRid = call.parameters["txRid"].orEmpty()
                    val txRidBytes = runCatching { txRid.hexStringToByteArray() }
                        .getOrElse { throw UserMistake("Invalid txRid hex") }
                    if (txRidBytes.size != 32) throw UserMistake("txRid must be 32 bytes hex")
                    val status = gateway.transactionStatus(txRidBytes)
                    call.respondText("""{"status":"${status.name.lowercase()}"}""", ContentType.Application.Json)
                }
            }
        }
    }.start(wait = false)

    /** "true"/"false" become booleans, everything else stays a string (RestApi parity). */
    internal fun stringParamToGtv(value: String): Gtv = when (value) {
        "true" -> gtv(true)
        "false" -> gtv(false)
        else -> gtv(value)
    }

    private suspend inline fun io.ktor.server.routing.RoutingContext.handle(
        requestBrid: String?,
        crossinline block: suspend () -> Unit
    ) {
        if (requestBrid == null || !requestBrid.equals(brid, ignoreCase = true)) {
            call.respondText(
                """{"error":"Unknown blockchain RID: $requestBrid (this local node serves $brid)"}""",
                ContentType.Application.Json,
                HttpStatusCode.NotFound
            )
            return
        }
        try {
            block()
        } catch (e: Exception) {
            val cause = (e as? java.util.concurrent.ExecutionException)?.cause ?: e
            val (status, message) = when (cause) {
                is DuplicateTx -> HttpStatusCode.Conflict to cause.message
                is QueueFull -> HttpStatusCode.ServiceUnavailable to cause.message
                is UserMistake -> HttpStatusCode.BadRequest to cause.message
                is java.util.concurrent.TimeoutException ->
                    HttpStatusCode.GatewayTimeout to "Query exceeded ${QUERY_TIMEOUT_SECONDS}s"
                else -> HttpStatusCode.BadRequest to (cause.message ?: cause::class.simpleName)
            }
            call.respondText(
                """{"error":${gson.toJson(message ?: "unknown error")}}""",
                ContentType.Application.Json,
                status
            )
        }
    }

    override fun close() {
        runCatching { server.stop(gracePeriodMillis = 100, timeoutMillis = 1000) }
    }
}
