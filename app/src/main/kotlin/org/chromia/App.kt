package org.chromia

import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.*
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor

class App(
    private val repository: ChromiaRepositoryImpl = ChromiaRepositoryImpl(),
    private val promptManager: PromptManager = PromptManager(),
    private val toolExecutor: ToolExecutor = ToolExecutor(repository, promptManager)
) {
    companion object {
        private const val SERVER_NAME = "chromia-mcp-server"
        private const val SERVER_VERSION = "1.0.0"
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
            McpTools.fetchDocsTool()
        )

        val registeredTools = tools.map { tool ->
            RegisteredTool(tool) { request ->
                toolExecutor.executeTool(request)
            }
        }

        addTools(registeredTools)
    }

    suspend fun startServer() {
        val server = createMcpServer()
        val transport = createStdioTransport()

        server.connect(transport)
        awaitTermination(server)
    }

    private fun createStdioTransport(): StdioServerTransport {
        return StdioServerTransport(
            System.`in`.asInput().buffered(),
            System.out.asSink().buffered()
        )
    }

    private suspend fun awaitTermination(server: Server) {
        val terminationJob = CompletableDeferred<Unit>()
        
        server.onClose {
            terminationJob.complete(Unit)
        }
        
        Runtime.getRuntime().addShutdownHook(Thread {
            terminationJob.complete(Unit)
        })
        
        terminationJob.await()
    }
}

fun main(): Unit = runBlocking {
    App().startServer()
}