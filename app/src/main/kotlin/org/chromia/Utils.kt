package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import org.chromia.App.Companion.logger
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

const val USAGE_HELP = """
    Usage: program [--sse --host <host> --port <port> | --stdio]
      --sse                    Start SSE server (127.0.0.1:3001)
      --sse --host <host>        Custom host (default: 127.0.0.1)
      --sse --port <port>       Custom port (default: 3001)
      --stdio                  Start stdio server (default)
    
    Examples:
      --sse                             # 127.0.0.1:3001
      --sse --port 8080                 # 127.0.0.1:8080
      --sse --host 0.0.0.0 --port 8080  # All interfaces:8080
"""

data class SseOption(val host: String, val port: Int)

fun parseSseArgs(args: List<String>): SseOption {
    require(args.size % 2 == 0) { "Arguments must be in [--key value] pairs" }
    val options = args.chunked(2).associate {
        it[0].removePrefix("--") to it[1]
    }
    val host = options["host"] ?: "127.0.0.1"
    val port = try {
        options["port"]?.toInt() ?: 3001
    } catch (_: NumberFormatException) {
        throw IllegalArgumentException("Invalid port: ${options["port"]}")
    }
    require(port in 1..65535) { "Port must be between 1-65535" }

    return SseOption(host, port)
}

fun getResourcePath(pathStr: String) = object {}.javaClass.classLoader.getResource(pathStr)?.path

fun File.safeDelete(): Boolean = if (isDirectory) deleteRecursively() else delete()

inline fun <T, R> Collection<T>.ifNotEmpty(block: (Collection<T>) -> R): R? =
    if (isNotEmpty()) block(this) else null

suspend fun HttpClient.downloadFile(url: String) = runCatching {
    val response: HttpResponse = get(url)

    when (response.status) {
        HttpStatusCode.OK -> {
            val tempFile = createTempFile("embedding")
            tempFile.outputStream().use { output ->
                response.body<ByteArray>().let { output.write(it) }
            }
            tempFile
        }
        HttpStatusCode.NotFound -> {
            logger.info("Embeddings not found in registry")
            null
        }
        else -> {
            logger.warn("Download failed with HTTP ${response.status}")
            null
        }
    }
}.getOrNull()

suspend fun HttpClient.uploadFile(url: String, token: String, file: File) {
    val response: HttpResponse = put(url) {
        header("PRIVATE-TOKEN", token)
        // Use Multipart if docs size gets bigger
        setBody(file.readBytes())
    }

    if (!response.status.isSuccess()) {
        throw ClientRequestException(response, "Upload failed: ${response.status}")
    }
}
