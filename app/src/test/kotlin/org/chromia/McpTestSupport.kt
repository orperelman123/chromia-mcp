package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import io.ktor.client.engine.mock.MockEngine
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor

internal object McpTestSupport {
    const val EXPLORER_URL = "https://explorer.test/explorer"

    val AUTH_SEGMENT: TextSegment = TextSegment.from(
        "FT4 authentication uses auth descriptors and require_mandatory_flags on the main descriptor.",
        Metadata.from("file_name", "ft4-auth.md")
    )
    val RELL_SEGMENT: TextSegment = TextSegment.from(
        "Rell compiler pipeline is S_ then C_ passes then R_ then RR_ then Rt.",
        Metadata.from("file_name", "rell-compiler.md")
    )

    fun errorEngine(): MockEngine = MockEngine {
        error("in-process MCP must not use live HTTP")
    }

    fun errorPostchain(config: ChromiaConfig = ChromiaConfig(explorerUrl = EXPLORER_URL)): PostchainClientService =
        PostchainClientService(config) { _, _, _ ->
            error("in-process MCP must not use live Postchain")
        }

    /**
     * In-memory docs store for in-process MCP sessions: a REAL [RagStore] over a
     * two-segment index (see [TestDocsIndex]). It does not download anything, but
     * `query()`, `fetchById()` and the segment-id index are the production ones -
     * this used to override `query()`, which is our own retrieval, so every
     * in-process docs assertion passed without running it.
     */
    fun fixtureRagStore(): RagStore = TestDocsIndex.store(AUTH_SEGMENT, RELL_SEGMENT)

    fun testApp(
        engine: MockEngine = errorEngine(),
        postchain: PostchainClientService? = null,
        config: ChromiaConfig = ChromiaConfig(explorerUrl = EXPLORER_URL),
        ragStoreFactory: () -> RagStore = { fixtureRagStore() }
    ): App {
        val repository = ChromiaRepositoryImpl(
            config = config,
            httpClientService = HttpClientService(config, engine),
            postchainClientService = postchain ?: errorPostchain(config)
        )
        return App(
            repository = repository,
            toolExecutor = ToolExecutor(
                repository,
                PromptManager(),
                ragStoreFactory = ragStoreFactory
            )
        )
    }
}
