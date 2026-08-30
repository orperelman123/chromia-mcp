package org.chromia

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.chromia.App.Companion.logger
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class App(
    private val repository: ChromiaRepositoryImpl = ChromiaRepositoryImpl(),
    private val promptManager: PromptManager = PromptManager(),
    private val toolExecutor: ToolExecutor = ToolExecutor(repository, promptManager)
) {
    companion object {
        private const val SERVER_NAME = "chromia-mcp-server"
        private const val SERVER_VERSION = "0.0.1"
        val logger: Logger = LoggerFactory.getLogger(App::class.java)
    }

    private fun createMcpServer(): Server {
        return Server(
            serverInfo = Implementation(
                name = SERVER_NAME,
                version = SERVER_VERSION
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(
                        subscribe = true,
                        listChanged = true
                    ),
                    prompts = ServerCapabilities.Prompts(listChanged = true)
                )
            )
        ).apply {
            registerTools()
        }
    }

    private fun Server.registerTools() {
        val tools = listOf(
            McpTools.getPromptsTool(),
            McpTools.getBlockchainsTransactionsTool(),
            McpTools.getTransactionsByClusterTool(),
            McpTools.getAllAssetsTool(),
            McpTools.getTotalRewardsPaidTool(),
            McpTools.getAssetTopHoldersTool(),
            McpTools.getAssetDistributionTool(),
            McpTools.getBlockchainAnalyticsTool(),
            McpTools.filterBlockchains(),
            McpTools.getBlockchainDetailsTool(),
            McpTools.getMonthlyActiveAccountsPerChainTool(),
            McpTools.getAllTransactionsTool(),
            McpTools.getAllOperationsTool(),
            McpTools.getFilterAssetsTool(),
            McpTools.getChrAggregateseTool(),
            McpTools.getAssetBlockchainsTool(),
            McpTools.getSignerBlockchainsTool(),
            McpTools.getAccountBlockchainsTool(),
            McpTools.getNodeUnavailabilityTool(),
            McpTools.getNetworkStats(),
            McpTools.fetchDocsTool(),
            McpTools.fetchMockTool(),
            McpTools.searchMockTool(),
            McpTools.runDappQueriesTool()
        )

        val registeredTools = tools.map { tool ->
            RegisteredTool(tool) { request ->
                logger.info("Request: $request")
                val response = toolExecutor.executeTool(request)
                logger.info("Response : \n${response.structuredContent}")

                response
            }
        }

        addTools(registeredTools)
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

    fun runSseMcpServer(host: String, port: Int): EmbeddedServer<*, *> {
        val server = embeddedServer(CIO, host = host, port = port) {
            installCors()
            installHealthEndpoint()
            mcp {
                return@mcp createMcpServer()
            }
        }.start(wait = true)
        return server
    }

    private fun Application.installHealthEndpoint() {
        routing {
            get("/health") {
                call.respondText(
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                    text = """
                    {
                        "status": "healthy",
                        "server": "$SERVER_NAME",
                        "version": "$SERVER_VERSION"
                    }
                    """.trimIndent()
                )
            }
        }
    }
}

fun main(args: Array<String>): Unit = runBlocking {
    val arg = args.firstOrNull() ?: "--stdio"
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
