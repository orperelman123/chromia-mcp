package org.chromia

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.*
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.chromia.App.Companion.logger
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.slf4j.LoggerFactory

class App(
    private val repository: ChromiaRepositoryImpl = ChromiaRepositoryImpl(),
    private val promptManager: PromptManager = PromptManager(),
    private val toolExecutor: ToolExecutor = ToolExecutor(repository, promptManager)
) {
    companion object {
        private const val SERVER_NAME = "chromia-mcp-server"
        private const val SERVER_VERSION = "0.0.1"
        val logger = LoggerFactory.getLogger(App::class.java)
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
                    // TODO: Add resources for other AI agents that supports them
                    // NOTE: Is not supported by Cursor
//                    resources = ServerCapabilities.Resources(
//                        subscribe = true,
//                        listChanged = true
//                    ),
//                    prompts = ServerCapabilities.Prompts(listChanged = true)
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
            McpTools.getNetworkAccountCountTool(),
            McpTools.getNetworkTransferCountTool(),
            McpTools.getMonthlyActiveAccountsTool(),
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
            McpTools.listDocSourcesTool(),
            McpTools.fetchDocsTool(),
            McpTools.runDappQueriesTool()
        )

        val registeredTools = tools.map { tool ->
            RegisteredTool(tool) { request ->
                toolExecutor.executeTool(request)
            }
        }

        addTools(registeredTools)
    }

    fun runStdioMcpServer() = runBlocking {
        val server = createMcpServer()
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )
            server.connect(transport)
            val done = Job()
            server.onClose { done.complete() }
            done.join()
    }

    suspend fun runSseMcpServer(host: String, port: Int) {
        logger.info("Starting SSE server on $host:$port")
        embeddedServer(CIO, host = host, port = port) {
            mcp {
                createMcpServer()
            }
        }.startSuspend(wait = true)
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
