package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.chromia.App.Companion.logger
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

const val USAGE_HELP = """
    Usage: program [--sse --host <host> --port <port> | --stdio | --generate-embeddings | --generate-embeddings-no-upload]
      --sse                    Start SSE server (127.0.0.1:3001)
      --sse --host <host>        Custom host (default: 127.0.0.1)
      --sse --port <port>       Custom port (default: 3001)
      --stdio                  Start stdio server (default)
      --generate-embeddings    Fetch docs, create embeddings, persist locally, upload to GitLab packages
      --generate-embeddings-no-upload  Fetch docs, create embeddings, persist to local embeddings.json (no upload)
    
    Local embeddings path: CHROMIA_EMBEDDINGS_PATH if set; otherwise the first existing of
    build/embeddings.json or app/build/embeddings.json relative to cwd (so java -jar from
    the repo root finds the Gradle-generated file). If neither exists, app/build/embeddings.json
    when cwd is the repo root and app/build/ exists; otherwise build/embeddings.json.
    Runtime loads that local file first and falls back to the GitLab package only if it is missing.
    
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
    // Unknown keys used to be silently ignored: `--sse --prot 8080` started on
    // the default port 3001 with no warning (audit F5). Fail startup instead.
    val unknown = options.keys - setOf("host", "port")
    require(unknown.isEmpty()) {
        "Unknown option(s): ${unknown.joinToString(", ") { "--$it" }}. Valid options: --host, --port"
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


suspend fun HttpClient.downloadFile(
    url: String,
    /** Sees the successful (200) response before the body is read - for headers such as Last-Modified. */
    onResponse: (HttpResponse) -> Unit = {}
) = runCatching {
    // prepareGet + execute streams the body: `body<ByteArray>()` held the whole
    // download in heap, which for the 150 MB embeddings store is most of a
    // small container's budget on its own.
    prepareGet(url).execute { response ->
        when (response.status) {
            HttpStatusCode.OK -> {
                onResponse(response)
                val tempFile = createTempFile("embedding")
                try {
                    tempFile.outputStream().buffered().use { output ->
                        response.bodyAsChannel().toInputStream().copyTo(output)
                    }
                    tempFile
                } catch (t: Throwable) {
                    // A failed body read/write must not leave the temp file behind.
                    runCatching { tempFile.deleteIfExists() }
                    throw t
                }
            }
            HttpStatusCode.NotFound -> {
                logger.info("Embeddings not found at $url")
                null
            }
            else -> {
                logger.warn("Download of $url failed with HTTP ${response.status}")
                null
            }
        }
    }
}.getOrNull()

suspend fun HttpClient.uploadFile(
    url: String,
    token: String,
    file: File,
    tokenHeader: String = "PRIVATE-TOKEN"
) {
    val response: HttpResponse = put(url) {
        header(tokenHeader, token)
        // Use Multipart if docs size gets bigger
        setBody(file.readBytes())
    }

    if (!response.status.isSuccess()) {
        throw ClientRequestException(response, "Upload failed: ${response.status}")
    }
}
