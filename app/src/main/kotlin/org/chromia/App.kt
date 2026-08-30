package org.chromia

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.server.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
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
         * Default is gradle.properties `version` (0.2.2, latest official GitLab
         * tag of chromaway/core-tools/chromia-mcp). Publish jobs override with
         * `-Pversion=$CI_COMMIT_TAG`.
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

    internal fun createMcpServer(): Server {
        return Server(
            serverInfo = Implementation(
                name = SERVER_NAME,
                version = SERVER_VERSION
            ),
            options = ServerOptions(
                capabilities = SERVER_CAPABILITIES
            )
        ).apply {
            registerTools()
            registerResources()
        }
    }

    private fun Server.registerTools() {
        val tools = McpTools.allTools()

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

    private fun Application.installCors() {
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
            allowNonSimpleContentTypes = true
        }
    }

    fun runStdioMcpServer() = runBlocking {
        val server = createMcpServer()
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered(),
        )

        runBlocking {
            server.createSession(transport)
            val done = Job()
            server.onClose {
                done.complete()
            }
            done.join()
        }
    }

    fun runSseMcpServer(host: String, port: Int, wait: Boolean = true): EmbeddedServer<*, *> {
        val server = embeddedServer(CIO, host = host, port = port) {
            installCors()
            installHealthEndpoint()
            mcp {
                return@mcp createMcpServer()
            }
        }.start(wait = wait)
        return server
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

fun main(args: Array<String>): Unit = runBlocking {
    val arg = args.firstOrNull() ?: "--stdio"
    if (arg == "--generate-embeddings" || arg == "--generate-embeddings-no-upload") {
        val upload = arg == "--generate-embeddings"
        logger.info(if (upload) "Starting embeddings generation" else "Starting embeddings generation (no upload)")
        RagStore(loadFromRegistry = false).createAndUploadEmbeddings(upload = upload)
        logger.info("Embeddings generation finished")
        return@runBlocking
    }
    val app = App()
    when (arg) {
        "--sse" -> {
            try {
                val options = parseSseArgs(args.drop(1))
                app.runSseMcpServer(options.host, options.port)
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to start SSE server --> ${e.message}")
                logger.error(USAGE_HELP)
            }
        }
        "--stdio" -> app.runStdioMcpServer()
        else -> {
            logger.error("""
                Unknown command argument: $arg
                
                $USAGE_HELP
            """.trimMargin())
        }
    }
}
