package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor

internal object McpTestSupport {

    /**
     * A REAL address that nothing listens on.
     *
     * The in-process MCP sessions have to prove they answer WITHOUT reaching the
     * network - that a tools/list, a prompt, a docs lookup or a session handshake
     * never quietly depends on the explorer being up. That used to be enforced by
     * a `MockEngine { error("in-process MCP must not use live HTTP") }` and a
     * `PostchainClientService(config) { error(...) }`: two doubles whose whole job
     * was to throw.
     *
     * Port 1 on loopback is better at the same job and is not a double. The
     * production CIO client opens a real socket to a real address and the
     * operating system refuses it, so a session that reaches for the network
     * fails with a genuine connection error raised by the same code path a real
     * outage would take - not with a message the test wrote for itself.
     */
    const val EXPLORER_URL = "http://127.0.0.1:1/explorer-service"

    val AUTH_SEGMENT: TextSegment = TextSegment.from(
        "FT4 authentication uses auth descriptors and require_mandatory_flags on the main descriptor.",
        Metadata.from("file_name", "ft4-auth.md")
    )
    val RELL_SEGMENT: TextSegment = TextSegment.from(
        "Rell compiler pipeline is S_ then C_ passes then R_ then RR_ then Rt.",
        Metadata.from("file_name", "rell-compiler.md")
    )

    /**
     * Explorer and nodes both pointed at the closed port above. Any tool that
     * tries to leave the process gets a real ConnectException.
     */
    fun offlineConfig(): ChromiaConfig = ChromiaConfig(
        explorerUrl = EXPLORER_URL,
        predefinedNetworks = mapOf(
            "mainnet" to listOf("http://127.0.0.1:1"),
            "testnet" to listOf("http://127.0.0.1:1")
        )
    )

    /**
     * The production repository, pointed at the closed port.
     *
     * Most tool tests take a repository only because `execute(request, repository)`
     * demands one - rell_check, run_rell_tests, scaffold_dapp, the help and
     * schema tools never reach the network at all. Those used to be handed a
     * `RecordingRepository`, a double of OUR OWN `ChromiaRepository` interface,
     * for the privilege of being ignored.
     *
     * This is the real implementation instead. Nothing is faked, and if a tool
     * that is supposed to be offline ever does reach for the network, it fails
     * with a real connection error instead of being handed an invented answer -
     * which is a strictly better outcome than the recorder's silence.
     */
    fun offlineRepository(): ChromiaRepositoryImpl {
        val config = offlineConfig()
        return ChromiaRepositoryImpl(
            config = config,
            httpClientService = HttpClientService(config),
            postchainClientService = PostchainClientService(config)
        )
    }

    /**
     * In-memory docs store for in-process MCP sessions: a REAL [RagStore] over a
     * two-segment index (see [TestDocsIndex]). It does not download anything, but
     * `query()`, `fetchById()` and the segment-id index are the production ones -
     * this used to override `query()`, which is our own retrieval, so every
     * in-process docs assertion passed without running it.
     */
    fun fixtureRagStore(): RagStore = TestDocsIndex.store(AUTH_SEGMENT, RELL_SEGMENT)

    /**
     * The app under test, wired the way `App` wires it in production. There is no
     * engine seam and no query-client seam left: which network the app can reach
     * is decided by [config] alone, exactly as it is in production - the closed
     * port for the offline sessions, [LiveChromia.config] for the live ones.
     */
    fun testApp(
        config: ChromiaConfig = offlineConfig(),
        ragStoreFactory: () -> RagStore = { fixtureRagStore() }
    ): App {
        val repository = ChromiaRepositoryImpl(
            config = config,
            httpClientService = HttpClientService(config),
            postchainClientService = PostchainClientService(config)
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
