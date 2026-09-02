package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.put
import org.chromia.domain.NetworkResult
import org.chromia.tools.DappInteractionStrategy
import org.chromia.tools.DeploymentPreflightStrategy
import org.chromia.tools.ProbeBudget
import org.chromia.tools.VerifyDeployment
import org.chromia.tools.WriteDeploymentConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The whole blocking postchain-read family must be deadline-bounded, not just
 * verify_deployment (which VerifyDeploymentToolTest pins). CI run 33601190754:
 * chromia_dapp_query had NO deadline, so a chain the predefined system nodes
 * do not serve made postchain-client crawl all ~14 endpoints at up to 60s
 * each, outliving the e2e sweep's 240s rpc timeout and surfacing as a
 * transport error. deployment_preflight's reachability probe shared the same
 * class, multiplied by up to MAX_PROBED_URLS candidates.
 *
 * Unit-level only: RecordingRepository's artificial latencies replace all
 * network I/O.
 */
class ProbeBudgetFamilyTest {

    private val hexBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val repo = RecordingRepository()

    // ---- shared clamp + env parsing -----------------------------------------

    @Test
    fun sharedClampStaysBoundedUnderTheProxyTimeout() {
        assertTrue(
            ProbeBudget.MAX_DEADLINE_MS < 60_000L,
            "the family-wide cap must stay well under the 60s proxy write timeout"
        )
        assertEquals(ProbeBudget.DEFAULT_DEADLINE_MS, ProbeBudget.clampDeadlineMs(null))
        assertEquals(ProbeBudget.MIN_DEADLINE_MS, ProbeBudget.clampDeadlineMs(0))
        assertEquals(ProbeBudget.MAX_DEADLINE_MS, ProbeBudget.clampDeadlineMs(999_999))
        assertEquals(5_000L, ProbeBudget.clampDeadlineMs(5_000))
        // Env parsing per tool: garbage falls back to the default, huge values clamp.
        assertEquals(
            ProbeBudget.DEFAULT_DEADLINE_MS,
            ProbeBudget.configuredDeadlineMs(ProbeBudget.QUERY_DEADLINE_ENV, "soon")
        )
        assertEquals(
            ProbeBudget.MAX_DEADLINE_MS,
            ProbeBudget.configuredDeadlineMs(ProbeBudget.PREFLIGHT_DEADLINE_ENV, "999999")
        )
        assertEquals(
            15_000L,
            ProbeBudget.configuredDeadlineMs(ProbeBudget.QUERY_DEADLINE_ENV, " 15000 ")
        )
        // verify_deployment's constants are the same family-wide values.
        assertEquals(ProbeBudget.DEFAULT_DEADLINE_MS, VerifyDeployment.DEFAULT_DEADLINE_MS)
        assertEquals(ProbeBudget.MAX_DEADLINE_MS, VerifyDeployment.MAX_DEADLINE_MS)
    }

    // ---- chromia_dapp_query -------------------------------------------------

    private fun dappCall(deadlineMs: Long?, network: String? = "mainnet") = runBlocking {
        DappInteractionStrategy(deadlineMs = deadlineMs).execute(
            CallToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("blockchainRid", hexBrid)
                    network?.let { put("network", it) }
                    put("query", "rell.get_app_structure")
                }
            ),
            repo
        )
    }

    @Test
    fun hangingDappQueryReturnsBoundedActionableToolError() {
        repo.dappDelayMs = 60_000L // an endpoint crawl that never answers in time
        val startNanos = System.nanoTime()
        val result = dappCall(deadlineMs = 300)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(elapsedMs < 10_000, "answered in ${elapsedMs}ms - must be bounded, never hang")
        assertTrue(result.isError == true, "an exhausted query deadline is a tool ERROR")
        val text = (result.content.first() as TextContent).text.orEmpty()
        assertTrue(text.contains("timed out"), text)
        assertTrue(text.contains("not served by the queried \"mainnet\" node(s)"), text)
        assertTrue(text.contains("node URL as `network`"), text)
        assertTrue(text.contains(ProbeBudget.QUERY_DEADLINE_ENV), text)
    }

    @Test
    fun hangingDappQueryWithoutNetworkStillNamesTheEscapeHatch() {
        repo.dappDelayMs = 60_000L
        val result = dappCall(deadlineMs = 300, network = null)
        assertTrue(result.isError == true)
        val text = (result.content.first() as TextContent).text.orEmpty()
        assertTrue(text.contains("node URL as `network`"), text)
    }

    @Test
    fun fastHealthyDappQueryIsUnaffectedByTheDeadline() {
        repo.dappDelayMs = 0
        val result = dappCall(deadlineMs = 5_000)
        assertTrue(result.isError != true, result.content.toString())
        val s = result.structuredContent!!
        assertTrue(s["ok"]!!.jsonPrimitive.boolean, s.toString())
        assertEquals("rell.get_app_structure", repo.lastDapp?.query)
    }

    // ---- deployment_preflight reachability probe ----------------------------

    private fun preflightYaml(urls: List<String>): String = buildString {
        appendLine("blockchains:")
        appendLine("  my_chain:")
        appendLine("    module: main")
        appendLine("    config:")
        appendLine("      features:")
        appendLine("        merkle_hash_version: 2")
        appendLine("compile:")
        appendLine("  rellVersion: 0.16.1")
        appendLine("deployments:")
        appendLine("  testnet:")
        appendLine("    url:")
        urls.forEach { appendLine("      - $it") }
        appendLine("    brid: x\"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}\"")
        appendLine("    container: abc123containerlease")
        appendLine("    chains:")
        appendLine("      my_chain:")
    }

    private fun preflightCall(deadlineMs: Long?, urls: List<String>) = runBlocking {
        DeploymentPreflightStrategy(deadlineMs = deadlineMs).execute(
            CallToolRequest(
                name = "deployment_preflight",
                arguments = buildJsonObject {
                    put("yaml", preflightYaml(urls))
                    put("target", "testnet")
                }
            ),
            repo
        )
    }

    @Test
    fun hangingPreflightProbeReturnsBoundedReachabilityBlocker() {
        repo.heightDelaysMs.add(60_000L)
        repo.nextHeight = NetworkResult.Success(42L)
        val startNanos = System.nanoTime()
        val result = preflightCall(300, listOf("https://node0.testnet.chromia.com:7740"))
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(elapsedMs < 10_000, "answered in ${elapsedMs}ms - must be bounded, never hang")
        assertTrue(result.isError != true, "a timed-out probe is a finding, not a tool crash")
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val reach = s["findings"]!!.jsonArray.map { it.jsonObject }
            .first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertEquals("BLOCKER", reach["severity"]!!.jsonPrimitive.content)
        val msg = reach["message"]!!.jsonPrimitive.content
        assertTrue(msg.contains("timed out"), msg)
        assertTrue(msg.contains("deadline"), msg)
    }

    @Test
    fun preflightDeadlineIsSharedAcrossCandidateUrlsNotPerUrl() {
        // The FIRST candidate eats the entire budget; the second must get what
        // is left (nothing), never a fresh crawl of its own.
        repo.heightDelaysMs.addAll(listOf(60_000L, 60_000L))
        repo.nextHeight = NetworkResult.Success(42L)
        val startNanos = System.nanoTime()
        val result = preflightCall(
            400,
            listOf(
                "https://node0.testnet.chromia.com:7740",
                "https://node1.testnet.chromia.com:7740"
            )
        )
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(elapsedMs < 10_000, "answered in ${elapsedMs}ms - the budget must be shared, not per candidate")
        // The second candidate's exhausted budget answers WITHOUT touching the
        // repository - one deadline across all attempts, not per attempt.
        assertEquals(1, repo.heightCalls, "the spent budget must not start a second crawl")
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val reach = s["findings"]!!.jsonArray.map { it.jsonObject }
            .first { it["check"]!!.jsonPrimitive.content == "reachability" }
        val msg = reach["message"]!!.jsonPrimitive.content
        // Both candidates are reported, each with the deadline message.
        assertTrue(msg.contains("node0.testnet.chromia.com"), msg)
        assertTrue(msg.contains("node1.testnet.chromia.com"), msg)
        assertTrue(msg.contains("deadline"), msg)
    }

    @Test
    fun fastHealthyPreflightProbeIsUnaffectedByTheDeadline() {
        repo.nextHeight = NetworkResult.Success(42L)
        val result = preflightCall(5_000, listOf("https://node0.testnet.chromia.com:7740"))
        val s = result.structuredContent!!
        val reach = s["findings"]!!.jsonArray.map { it.jsonObject }
            .first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertEquals("INFO", reach["severity"]!!.jsonPrimitive.content, s.toString())
        assertTrue(reach["message"]!!.jsonPrimitive.content.contains("height 42"))
    }
}
