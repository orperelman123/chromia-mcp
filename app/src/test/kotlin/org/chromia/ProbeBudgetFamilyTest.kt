package org.chromia

import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.put
import org.chromia.domain.ChromiaRepository
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
 * ZERO DOUBLES (2026-09-07). These tests used to arm `RecordingRepository` with
 * a 60s artificial latency and then check that the tool gave up first - the
 * stall was the test's, so what was proved is that a coroutine timeout fires.
 * Nothing is slowed down here any more. The deadline is set to
 * [ProbeBudget.MIN_DEADLINE_MS] (100ms) and the repository is built COLD, so
 * the probe has to construct a postchain client - signer discovery over the
 * real network, measured 1.4-4.7s on 2026-09-07 - before it can read anything.
 * A real probe genuinely runs out of a real budget, which is exactly what an
 * agent on a slow link or against a not-served chain sees. The healthy-path
 * tests use the same live network with a budget it comfortably fits inside.
 */
class ProbeBudgetFamilyTest {

    private companion object {
        /** ONE production repository for the whole class, for the healthy paths. */
        val liveRepository by lazy { LiveChromia.repository() }
    }

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

    private fun dappCall(
        repository: ChromiaRepository,
        deadlineMs: Long?,
        network: String? = LiveChromia.NETWORK,
        query: String = "get_chr_asset"
    ) = runBlocking {
        DappInteractionStrategy(deadlineMs = deadlineMs).execute(
            callToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("blockchainRid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                    network?.let { put("network", it) }
                    put("query", query)
                }
            ),
            repository
        )
    }

    @Test
    fun liveDappQueryThatCannotFinishInARealBudgetIsABoundedToolError() {
        LiveChromia.requireLive("gives a real live dapp query a budget too small to answer in")
        val startNanos = System.nanoTime()
        // COLD repository on purpose: the client construction is inside the budget.
        val result = dappCall(LiveChromia.repository(), deadlineMs = ProbeBudget.MIN_DEADLINE_MS)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(elapsedMs < 10_000, "answered in ${elapsedMs}ms - must be bounded, never hang")
        assertTrue(result.isError == true, "an exhausted query deadline is a tool ERROR")
        val text = (result.content.first() as TextContent).text.orEmpty()
        assertTrue(text.contains("timed out"), text)
        assertTrue(text.contains("not served by the queried \"testnet\" node(s)"), text)
        assertTrue(text.contains("node URL as `network`"), text)
        assertTrue(text.contains(ProbeBudget.QUERY_DEADLINE_ENV), text)
    }

    @Test
    fun liveDappQueryWithoutNetworkStillNamesTheEscapeHatch() {
        LiveChromia.requireLive("times out a real query that fell back to the default network")
        val result = dappCall(
            LiveChromia.repository(), deadlineMs = ProbeBudget.MIN_DEADLINE_MS, network = null
        )
        assertTrue(result.isError == true)
        val text = (result.content.first() as TextContent).text.orEmpty()
        assertTrue(text.contains("timed out"), text)
        assertTrue(text.contains("node URL as `network`"), text)
    }

    /**
     * The healthy path is not collateral damage of the deadline: a real query
     * against the live Economy Chain answers well inside a normal budget, and
     * the answer is the chain's own (it names the chain it came from).
     */
    @Test
    fun liveHealthyDappQueryIsUnaffectedByTheDeadline() {
        LiveChromia.requireLive("runs a real Economy Chain query inside a normal deadline")
        val result = dappCall(liveRepository, deadlineMs = 15_000)
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val s = result.structuredContent!!
        assertEquals(
            LiveChromia.ECONOMY_CHAIN_BRID_HEX,
            s["blockchain_rid"]!!.jsonPrimitive.content,
            "the answer must come from the chain that was asked: $s"
        )
        assertTrue(s["symbol"]!!.jsonPrimitive.content.isNotBlank(), s.toString())
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

    private fun preflightCall(
        repository: ChromiaRepository,
        deadlineMs: Long?,
        urls: List<String>
    ) = runBlocking {
        DeploymentPreflightStrategy(deadlineMs = deadlineMs).execute(
            callToolRequest(
                name = "deployment_preflight",
                arguments = buildJsonObject {
                    put("yaml", preflightYaml(urls))
                    put("target", "testnet")
                }
            ),
            repository
        )
    }

    @Test
    fun livePreflightProbeThatRunsOutOfBudgetIsABoundedReachabilityBlocker() {
        LiveChromia.requireLive("gives the reachability probe a budget a real node cannot answer in")
        val startNanos = System.nanoTime()
        val result = preflightCall(
            // COLD repository: the client construction is inside the budget.
            LiveChromia.repository(),
            ProbeBudget.MIN_DEADLINE_MS,
            listOf(WriteDeploymentConfig.TESTNET_URL)
        )
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

    /**
     * EVERY CANDIDATE IS STILL REPORTED WHEN THE BUDGET IS GONE.
     *
     * What this replaces, and what it does NOT: the recorder version
     * (preflightDeadlineIsSharedAcrossCandidateUrlsNotPerUrl) armed two 60s
     * latencies and then asserted `heightCalls == 1` - that the second candidate
     * never reached the repository because the shared budget was already spent.
     * That distinction cannot be made from outside with real probes: with a
     * budget no real probe can answer in, a shared budget and a per-candidate
     * budget produce the same messages, and they differ only by one further
     * 100ms of wall clock - inside the noise of a live network on a loaded box.
     *
     * DELETED 2026-09-07 (zero-doubles): the "shared across candidates, never
     * per candidate" assertion itself. It needed a probe that stalls on command,
     * which is a double by construction. Now unverified: that
     * DeploymentPreflightStrategy computes `remainingMs` from the FIRST probe's
     * start rather than restarting the budget per URL - a regression there would
     * cost up to MAX_PROBED_URLS x the deadline (135s at the cap) and would not
     * be caught here. What IS still real below: with the budget spent, both
     * candidates are named in one bounded answer rather than one of them
     * silently disappearing.
     */
    @Test
    fun livePreflightNamesEveryCandidateWhenTheBudgetIsSpent() {
        LiveChromia.requireLive("probes two official testnet URLs with a budget neither can answer in")
        val urls = WriteDeploymentConfig.TESTNET_URLS.take(2)
        val startNanos = System.nanoTime()
        val result = preflightCall(LiveChromia.repository(), ProbeBudget.MIN_DEADLINE_MS, urls)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(elapsedMs < 10_000, "answered in ${elapsedMs}ms - must be bounded, never hang")
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val reach = s["findings"]!!.jsonArray.map { it.jsonObject }
            .first { it["check"]!!.jsonPrimitive.content == "reachability" }
        val msg = reach["message"]!!.jsonPrimitive.content
        urls.forEach { assertTrue(msg.contains(it), "candidate $it is missing from: $msg") }
        assertTrue(msg.contains("deadline"), msg)
    }

    @Test
    fun liveHealthyPreflightProbeIsUnaffectedByTheDeadline() {
        LiveChromia.requireLive("probes the official testnet node inside a normal deadline")
        val result = preflightCall(liveRepository, 15_000, listOf(WriteDeploymentConfig.TESTNET_URL))
        val s = result.structuredContent!!
        val reach = s["findings"]!!.jsonArray.map { it.jsonObject }
            .first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertEquals("INFO", reach["severity"]!!.jsonPrimitive.content, s.toString())
        assertTrue(
            reach["message"]!!.jsonPrimitive.content.contains("answers for the Directory Chain (height "),
            reach.toString()
        )
    }
}
