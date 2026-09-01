package org.chromia

import kotlinx.coroutines.runBlocking
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
}
