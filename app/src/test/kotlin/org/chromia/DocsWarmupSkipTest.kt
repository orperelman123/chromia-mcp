package org.chromia

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The startup docs-index warmup must be a no-op when every RAG-backed docs
 * tool (search/fetch_docs/fetch) is disabled - a lite hosted config on a
 * small instance must never pay the embeddings load memory spike at boot
 * (the 512MB Render container crash-looped on exactly that).
 */
class DocsWarmupSkipTest {

    // --- the skip predicate -------------------------------------------------

    @Test
    fun noDisabledToolsMeansWarmupRuns() {
        assertFalse(McpTools.docsToolsDisabled(emptySet()))
    }

    @Test
    fun allThreeDocsToolsDisabledSkipsWarmup() {
        assertTrue(McpTools.docsToolsDisabled(setOf("search", "fetch_docs", "fetch")))
    }

    @Test
    fun anyDocsToolStillEnabledKeepsWarmup() {
        // Every proper subset of the docs tools leaves the index reachable.
        assertFalse(McpTools.docsToolsDisabled(setOf("search", "fetch_docs")))
        assertFalse(McpTools.docsToolsDisabled(setOf("search", "fetch")))
        assertFalse(McpTools.docsToolsDisabled(setOf("fetch_docs", "fetch")))
        assertFalse(McpTools.docsToolsDisabled(setOf("search")))
    }

    @Test
    fun unrelatedDisabledToolsDoNotSkipWarmup() {
        // The documented lite config before docs tools are turned off.
        assertFalse(
            McpTools.docsToolsDisabled(
                setOf("rell_check", "rell_security_check", "run_rell_tests", "chromia_dapp_query")
            )
        )
    }

    @Test
    fun supersetIncludingAllDocsToolsSkipsWarmup() {
        assertTrue(
            McpTools.docsToolsDisabled(
                setOf(
                    "rell_check", "rell_security_check", "run_rell_tests",
                    "chromia_dapp_query", "search", "fetch_docs", "fetch"
                )
            )
        )
    }

    @Test
    fun predicateComposesWithEnvParsing() {
        val disabled = McpTools.disabledTools(
            mapOf("CHROMIA_MCP_DISABLE_TOOLS" to " search , fetch_docs ,fetch,")
        )
        assertTrue(McpTools.docsToolsDisabled(disabled))
    }

    // --- name drift guard ---------------------------------------------------

    @Test
    fun docsToolNamesMatchAdvertisedAndExecutableTools() {
        val advertised = McpTools.allTools().map { it.name }.toSet()
        val executable = ToolExecutor(RecordingRepository(), PromptManager()).registeredToolNames()
        McpTools.DOCS_TOOL_NAMES.forEach { name ->
            assertTrue(name in advertised, "docs tool '$name' must exist in the advertised tool list")
            assertTrue(name in executable, "docs tool '$name' must exist in the executor strategies")
        }
    }

    @Test
    fun compactModeNeverHidesDocsTools() {
        // The predicate ignores compact mode on purpose: compact drops only the
        // *_help schemas, docs tools stay advertised and the index stays needed.
        val compact = McpTools.allTools(compact = true).map { it.name }.toSet()
        McpTools.DOCS_TOOL_NAMES.forEach { name ->
            assertTrue(name in compact, "compact mode must keep docs tool '$name'")
        }
    }

    // --- App wiring: the skip must never touch the RagStore -----------------

    @Test
    fun appWarmupSkipsWithoutTouchingRagStore() = runBlocking {
        val ragStoreTouched = AtomicBoolean(false)
        val executor = ToolExecutor(
            RecordingRepository(),
            PromptManager(),
            ragStoreFactory = {
                ragStoreTouched.set(true)
                RagStore(loadFromRegistry = false)
            }
        )
        val app = App(RecordingRepository(), PromptManager(), executor)

        app.warmUpDocs(docsToolsDisabled = true)
        assertFalse(ragStoreTouched.get(), "skipped warmup must not lazy-init the RagStore")

        app.warmUpDocs(docsToolsDisabled = false)
        assertTrue(ragStoreTouched.get(), "enabled warmup must still pre-load the RagStore")
    }

    // --- stdio warms from spawn, and EOF still ends the process ------------

    /**
     * 2026-09-05: stdio (the Claude Code path) loaded the index lazily on the
     * first docs call, so that call paid the whole load. It now warms from
     * spawn like SSE does - detached, so a client that leaves mid-download
     * gets its EOF honoured immediately instead of after the download.
     */
    @Test
    fun stdioStartsTheDocsWarmupAndStillReturnsOnEofWhileItRuns() = runBlocking {
        val loadStarted = CountDownLatch(1)
        val releaseLoad = CountDownLatch(1)
        val executor = ToolExecutor(
            RecordingRepository(),
            PromptManager(),
            ragStoreFactory = {
                loadStarted.countDown()
                releaseLoad.await(30, TimeUnit.SECONDS) // a download that is still in flight
                RagStore(loadFromRegistry = false)
            }
        )
        val app = App(RecordingRepository(), PromptManager(), executor)
        val originalIn = System.`in`
        System.setIn(ByteArrayInputStream(ByteArray(0))) // the client is already gone
        try {
            val started = System.nanoTime()
            val code = withTimeout(20_000) { withContext(Dispatchers.IO) { runMain(arrayOf("--stdio")) { app } } }
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertEquals(0, code)
            assertTrue(loadStarted.await(5, TimeUnit.SECONDS), "--stdio must start the docs warmup at spawn")
            assertTrue(elapsedMs < 10_000, "EOF must end the stdio run while the warmup is still blocked (took $elapsedMs ms)")
            assertFalse(app.docsWarmup!!.isCompleted, "the warmup was still in flight when the server returned")
        } finally {
            System.setIn(originalIn)
            releaseLoad.countDown()
        }
        withTimeout(10_000) { app.docsWarmup!!.join() }
    }
}
