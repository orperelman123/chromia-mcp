package org.chromia

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.sse
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.server.*
import io.ktor.server.request.path
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlin.system.exitProcess
import org.chromia.App.Companion.logger
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.domain.ChromiaRepository
import org.chromia.tools.McpResources
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class App(
    private val repository: ChromiaRepository = ChromiaRepositoryImpl(),
    private val promptManager: PromptManager = PromptManager(),
    private val toolExecutor: ToolExecutor = ToolExecutor(repository, promptManager)
) {
    companion object {
        const val SERVER_NAME = "chromia-mcp-server"
        /**
         * MCP Implementation.version and /health "version".
         * Sourced from Gradle `project.version` via generated [BuildInfo]
         * (`generateBuildInfo` in `app/build.gradle.kts`).
         * Default is gradle.properties `version` (this fork's release
         * version). CI and release jobs override with `-Pversion`.
         */
        val SERVER_VERSION: String = BuildInfo.VERSION
        val logger: Logger = LoggerFactory.getLogger(App::class.java)

        /**
         * Advertise only capabilities this server actually implements.
         * Tools are registered and static (no listChanged notifications).
         * Resources are the three static snapshots below
         * (no subscribe / listChanged notifications). Prompts are not advertised:
         * the catalog is the get_prompts tool plus chromia://config/prompt-catalog.
         */
        val SERVER_CAPABILITIES = ServerCapabilities(
            tools = ServerCapabilities.Tools(listChanged = false),
            resources = ServerCapabilities.Resources(
                subscribe = false,
                listChanged = false
            )
        )

        fun healthJson(): String = """
                    {
                        "status": "healthy",
                        "server": "$SERVER_NAME",
                        "version": "$SERVER_VERSION"
                    }
                    """.trimIndent()
    }

    internal fun createMcpServer(
        compact: Boolean = McpTools.compactToolsMode(),
        disabled: Set<String> = McpTools.disabledTools()
    ): Server {
        return Server(
            serverInfo = Implementation(
                name = SERVER_NAME,
                version = SERVER_VERSION
            ),
            options = ServerOptions(
                capabilities = SERVER_CAPABILITIES
            )
        ).apply {
            registerTools(compact, disabled)
            registerResources()
        }
    }

    private fun Server.registerTools(compact: Boolean, disabled: Set<String>) {
        val tools = McpTools.allTools(compact = compact, disabled = disabled)

        val registeredTools = tools.map { tool ->
            RegisteredTool(tool) { request ->
                logger.debug("Request: {}", request)
                val response = toolExecutor.executeTool(request)
                logger.debug("Response: {}", response.content)

                response
            }
        }

        addTools(registeredTools)
    }

    /**
     * Connects [server] to [transport] and replaces the session's tools/call
     * handler with a deployment-aware one. The SDK keeps ONE registry that feeds
     * both tools/list and tools/call, so a registered stub for a disabled tool
     * would also be advertised - defeating the point of disabling it. Overriding
     * the per-session call handler keeps tools/list exactly the registered set
     * while a call to a disabled-but-real tool gets an actionable refusal
     * ([McpTools.disabledToolRefusal]) instead of the SDK's bare
     * "Tool X not found" (hosted probe 2026-09-01). Compact-hidden help tools
     * intentionally get NO such refusal - chromia_help covers their content and
     * they are hidden for schema savings, not disabled.
     */
    internal suspend fun createGatedSession(
        server: Server,
        transport: io.modelcontextprotocol.kotlin.sdk.shared.Transport,
        disabled: Set<String>
    ): ServerSession {
        val session = server.createSession(transport)
        session.setRequestHandler<io.modelcontextprotocol.kotlin.sdk.CallToolRequest>(
            io.modelcontextprotocol.kotlin.sdk.Method.Defined.ToolsCall
        ) { request, _ ->
            callToolGated(server, request, disabled)
        }
        return session
    }

    internal suspend fun callToolGated(
        server: Server,
        request: io.modelcontextprotocol.kotlin.sdk.CallToolRequest,
        disabled: Set<String>
    ): io.modelcontextprotocol.kotlin.sdk.CallToolResult {
        val registered = server.tools[request.name]
            ?: return McpTools.disabledToolRefusal(request.name, disabled)
                ?.let { org.chromia.tools.toolErrorResult(it) }
            // Same shape and text as the SDK's unknown-tool answer.
            ?: io.modelcontextprotocol.kotlin.sdk.CallToolResult(
                content = listOf(io.modelcontextprotocol.kotlin.sdk.TextContent("Tool ${request.name} not found")),
                isError = true
            )
        return try {
            registered.handler(request)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Mirrors the SDK's handleCallTool error wrapping.
            logger.error("Error executing tool ${request.name}", e)
            io.modelcontextprotocol.kotlin.sdk.CallToolResult(
                content = listOf(
                    io.modelcontextprotocol.kotlin.sdk.TextContent("Error executing tool ${request.name}: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun Server.registerResources() {
        McpResources.all(healthJson = ::healthJson).forEach { resource ->
            addResource(
                uri = resource.uri,
                name = resource.name,
                description = resource.description,
                mimeType = resource.mimeType
            ) { request ->
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = resource.readText(),
                            uri = request.uri,
                            mimeType = resource.mimeType
                        )
                    )
                )
            }
        }
    }

    /**
     * CORS for browser-based MCP clients. Without any allowed host the ktor
     * plugin rejects every cross-origin request with 403 (QA finding), so no
     * browser client could ever connect. CHROMIA_MCP_ALLOWED_ORIGINS (comma
     * separated, e.g. "https://app.example.com,http://localhost:5173") narrows
     * access to specific origins; unset (or "*") allows any origin.
     * Credentials are never allowed - the wildcard-origin-with-credentials
     * combination is unsafe, and bearer auth uses the Authorization header,
     * which browsers only send when explicitly set by the client code.
     */
    internal fun Application.installCors(allowedOrigins: String?) {
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
            allowNonSimpleContentTypes = true
            allowHeader(HttpHeaders.Authorization)
            val origins = allowedOrigins?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
            if (origins.isEmpty() || "*" in origins) {
                anyHost()
            } else {
                origins.forEach { origin ->
                    val parts = origin.removeSuffix("/").split("://", limit = 2)
                    if (parts.size == 2) {
                        allowHost(parts[1], schemes = listOf(parts[0]))
                    } else {
                        allowHost(parts[0], schemes = listOf("http", "https"))
                    }
                }
            }
        }
    }

    /**
     * Runs the stdio transport and returns once the session ends. The SDK's
     * `Server.onClose` only fires on an explicit `Server.close()`; when the
     * client goes away and stdin reaches EOF only the *transport* closes, so
     * waiting on the server callback alone parked the process forever - one
     * zombie JVM per disconnected client (QA finding). Hooking the transport
     * close signal makes EOF end this call; requests in flight during normal
     * operation are unaffected because the transport only closes on EOF,
     * read error, or explicit shutdown.
     */
    fun runStdioMcpServer(
        inputStream: Source = System.`in`.asSource().buffered(),
        outputStream: Sink = System.out.asSink().buffered(),
        compact: Boolean = McpTools.compactToolsMode(),
        disabled: Set<String> = McpTools.disabledTools(),
    ) = runBlocking {
        val server = createMcpServer(compact, disabled)
        val transport = StdioServerTransport(
            inputStream = inputStream,
            outputStream = outputStream,
        )

        runBlocking {
            val done = Job()
            transport.onClose {
                done.complete()
            }
            server.onClose {
                done.complete()
            }
            createGatedSession(server, transport, disabled)
            done.join()
        }
    }

    /**
     * Startup docs-index warmup. A no-op when every RAG-backed docs tool
     * (search/fetch_docs/fetch) is disabled via CHROMIA_MCP_DISABLE_TOOLS:
     * the index can never be queried, so a lite hosted config must not pay
     * the embeddings load memory spike at boot (512MB instances crash-looped
     * on it). RagStore stays lazy for the enabled case - this only decides
     * whether to trigger the existing warmup, never how the store loads.
     */
    suspend fun warmUpDocs(docsToolsDisabled: Boolean = McpTools.docsToolsDisabled()) {
        if (docsToolsDisabled) {
            logger.info("docs tools disabled - skipping index warmup")
            return
        }
        toolExecutor.warmUpDocs()
    }

    fun runSseMcpServer(
        host: String,
        port: Int,
        wait: Boolean = true,
        authToken: String? = System.getenv("CHROMIA_MCP_AUTH_TOKEN")?.takeIf { it.isNotBlank() },
        allowedOrigins: String? = System.getenv("CHROMIA_MCP_ALLOWED_ORIGINS")?.takeIf { it.isNotBlank() },
        compact: Boolean = McpTools.compactToolsMode(),
        disabled: Set<String> = McpTools.disabledTools()
    ): EmbeddedServer<*, *> {
        val server = embeddedServer(CIO, host = host, port = port) {
            installCors(allowedOrigins)
            // Optional bearer auth for hosted deployments (CHROMIA_MCP_AUTH_TOKEN).
            // /health stays open for load-balancer checks. Off by default so
            // no-auth connectors (e.g. ChatGPT) keep working unless opted in.
            // Match on the PATH, not the raw URI: "/health?x=1" is still the
            // health endpoint, but the raw-URI comparison 401'd any query
            // string (QA finding). /health is the only public endpoint.
            if (authToken != null) {
                // Constant-time compare: a plain != leaks match-length timing.
                val expectedAuth = "Bearer $authToken".toByteArray()
                intercept(io.ktor.server.application.ApplicationCallPipeline.Plugins) {
                    val presented = call.request.headers[io.ktor.http.HttpHeaders.Authorization]
                        ?.toByteArray() ?: ByteArray(0)
                    if (call.request.path() != "/health" &&
                        !java.security.MessageDigest.isEqual(expectedAuth, presented)
                    ) {
                        call.respondText(
                            text = """{"error":"unauthorized"}""",
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.Unauthorized
                        )
                        finish()
                    }
                }
            }
            installHealthEndpoint()
            installMcpSse(compact, disabled)
        }.start(wait = wait)
        return server
    }

    /**
     * SSE transport wiring, functionally identical to the SDK's
     * `Application.mcp {}` plugin (same root SSE + POST endpoints, same status
     * codes) except each session is connected through [createGatedSession] so
     * disabled-but-real tools refuse with guidance. The SDK plugin never exposes
     * the session it creates, and the SDK's tools/list and tools/call share one
     * registry, so this is the only way to gate calls without also advertising
     * stubs in tools/list.
     */
    internal fun Application.installMcpSse(compact: Boolean, disabled: Set<String>) {
        install(io.ktor.server.sse.SSE)
        val transports = java.util.concurrent.ConcurrentHashMap<String, SseServerTransport>()
        routing {
            sse {
                val transport = SseServerTransport("", this)
                transports[transport.sessionId] = transport
                val server = createMcpServer(compact, disabled)
                server.onClose { transports.remove(transport.sessionId) }
                createGatedSession(server, transport, disabled)
                kotlinx.coroutines.awaitCancellation()
            }
            post {
                val sessionId = call.request.queryParameters["sessionId"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, "sessionId query parameter is not provided")
                    return@post
                }
                val transport = transports[sessionId]
                if (transport == null) {
                    call.respond(HttpStatusCode.NotFound, "Session not found")
                    return@post
                }
                transport.handlePostMessage(call)
            }
        }
    }

}

fun Application.installHealthEndpoint() {
    routing {
        get("/health") {
            call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
                text = App.healthJson()
            )
        }
    }
}

/**
 * Exits with [runMain]'s code. The explicit [exitProcess] both surfaces
 * startup failures to supervisors/CI (failures used to fall out of main and
 * exit 0, QA finding) and guarantees the JVM dies after stdin EOF instead of
 * being kept alive by lingering non-daemon worker threads (zombie JVM,
 * QA finding).
 */
fun main(args: Array<String>): Unit = exitProcess(runMain(args))

/**
 * Argument dispatch, separated from [main] so tests can assert exit codes
 * without killing the test JVM. Returns 0 on success, non-zero on any
 * startup failure (bad arguments, unknown command, bind failure, ...).
 */
internal fun runMain(args: Array<String>, appFactory: () -> App = { App() }): Int = runBlocking {
    val arg = args.firstOrNull() ?: "--stdio"
    if (arg == "--generate-embeddings" || arg == "--generate-embeddings-no-upload") {
        val upload = arg == "--generate-embeddings"
        logger.info(if (upload) "Starting embeddings generation" else "Starting embeddings generation (no upload)")
        RagStore(loadFromRegistry = false).createAndUploadEmbeddings(upload = upload)
        logger.info("Embeddings generation finished")
        return@runBlocking 0
    }
    val app = appFactory()
    when (arg) {
        "--sse" -> {
            val options = try {
                parseSseArgs(args.drop(1))
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to start SSE server --> ${e.message}")
                logger.error(USAGE_HELP)
                return@runBlocking 1
            }
            // Warm the RAG store/model in the background: production telemetry
            // showed ~15s first-search latency per fresh instance without it.
            val warmup = launch { app.warmUpDocs() }
            try {
                app.runSseMcpServer(options.host, options.port)
                0
            } catch (e: Exception) {
                // Bind/startup failures (port in use, bad address, ...) must
                // exit non-zero so supervisors and CI notice (QA finding).
                warmup.cancel()
                logger.error("Failed to start SSE server --> ${e.message}")
                1
            }
        }
        "--stdio" -> {
            app.runStdioMcpServer()
            0
        }
        else -> {
            logger.error("""
                Unknown command argument: $arg

                $USAGE_HELP
            """.trimMargin())
            1
        }
    }
}
