package org.chromia.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shared wall-clock budget for the tool family that drives BLOCKING
 * postchain-client reads: chromia_dapp_query, verify_deployment's height
 * probes + smoke query, and deployment_preflight's reachability probe.
 *
 * Why the family needs one: for a chain the queried node pool does not serve,
 * postchain-client's TryNextOnError strategy crawls EVERY endpoint in the pool
 * (14 on mainnet) at up to 60s connect/response timeout each. Live evidence:
 * verify_deployment hit this on 2026-09-02 (D1 - the hosted proxy's 60s write
 * timeout closed the socket first), and CI run 33601190754 saw
 * chromia_dapp_query outlive even the e2e sweep's 240s rpc timeout, surfacing
 * as a transport error instead of an actionable answer.
 *
 * Every other outbound path is already bounded elsewhere: explorer GraphQL and
 * docs fetches run under ktor HttpTimeout (HttpClientService / DocsFetcher),
 * git/chr subprocesses under awaitProcess timeouts. This object exists for the
 * blocking postchain-client read family only.
 *
 * All deadlines share one clamp: default 20s, capped at 45s (well under the
 * 60s proxy write timeout), floored at 100ms so a misconfigured value cannot
 * make every call fail instantly. Per-tool env overrides:
 * [VerifyDeployment.DEADLINE_ENV], [QUERY_DEADLINE_ENV],
 * [PREFLIGHT_DEADLINE_ENV].
 */
object ProbeBudget {

    /** Overall wall-clock budget default - client construction (signer discovery) included. */
    const val DEFAULT_DEADLINE_MS = 20_000L

    /** Hard cap on any configured deadline - must stay well under the 60s proxy write timeout. */
    const val MAX_DEADLINE_MS = 45_000L

    /** Floor so a misconfigured deadline cannot make every call fail instantly. */
    const val MIN_DEADLINE_MS = 100L

    /** Operator override for chromia_dapp_query's overall deadline; clamped. */
    const val QUERY_DEADLINE_ENV = "CHROMIA_MCP_QUERY_DEADLINE_MS"

    /** Operator override for deployment_preflight's reachability-probe deadline (shared by all probed URLs); clamped. */
    const val PREFLIGHT_DEADLINE_ENV = "CHROMIA_MCP_PREFLIGHT_PROBE_DEADLINE_MS"

    /** Clamp a configured deadline; null/garbage means [DEFAULT_DEADLINE_MS]. */
    fun clampDeadlineMs(deadlineMs: Long?): Long =
        (deadlineMs ?: DEFAULT_DEADLINE_MS).coerceIn(MIN_DEADLINE_MS, MAX_DEADLINE_MS)

    /** The deadline configured via [env] (or the default), always clamped. */
    fun configuredDeadlineMs(env: String, raw: String? = System.getenv(env)): Long =
        clampDeadlineMs(raw?.trim()?.toLongOrNull())

    /**
     * Runs [work] on its own IO-dispatched job and abandons it when [budgetMs]
     * runs out. The postchain client's network calls are blocking, so a plain
     * withTimeout around them would not return until the blocking call itself
     * does - instead the deferred is awaited with a timeout and cancelled,
     * letting the abandoned call finish (or fail) on a pool thread while the
     * tool answers within its deadline. Returns null on an exhausted budget
     * (including budgetMs <= 0, so a shared budget enforced ACROSS attempts
     * costs nothing once spent).
     */
    suspend fun <T : Any> withBudget(budgetMs: Long, work: suspend () -> T): T? {
        if (budgetMs <= 0) return null
        val scope = CoroutineScope(probeDispatcher + SupervisorJob())
        val deferred = scope.async { work() }
        return try {
            withTimeoutOrNull(budgetMs) { deferred.await() }
        } finally {
            scope.cancel()
            if (!deferred.isCompleted) {
                // Still blocked in the client after the deadline: count it until
                // the blocking call really returns (cancel cannot interrupt it).
                abandonedProbes.incrementAndGet()
                deferred.invokeOnCompletion { abandonedProbes.decrementAndGet() }
            }
        }
    }

    /**
     * Abandoned probes used to keep running on Dispatchers.IO - the SAME pool
     * every other tool blocks on (rell_check, run_rell_tests, local_chain_up,
     * docs). Each one holds an IO thread for the whole endpoint crawl (up to
     * 14 x 60s on mainnet), and the pool has 64 threads: an agent retrying a
     * not-served chain filled it, after which every IO-dispatched tool call
     * waited for a thread with no message at all - the server looked hung,
     * not busy (QA concurrency lens 2026-09-02). Probes now run on their own
     * daemon pool so a stuck crawl can only ever cost probes, and the
     * abandoned count is surfaced in the timeout hints.
     */
    private val probeDispatcher = Executors.newCachedThreadPool { r ->
        Thread(r, "probe-budget").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private val abandonedProbes = AtomicInteger()

    /** Probes that outlived their deadline and are still blocked in the client. Released when each returns. */
    fun abandonedCount(): Int = abandonedProbes.get()

    /** "" when nothing is stuck; otherwise names the backlog so a slow answer is explained, not mysterious. */
    fun abandonedNote(): String {
        val n = abandonedCount()
        if (n <= 0) return ""
        return " $n earlier probe(s) abandoned at their deadline are still blocked in the postchain client " +
            "(released when the node crawl gives up; they run on a dedicated pool and do not delay other tools)."
    }

    /**
     * Tool-error text for a chromia_dapp_query that outlived its overall
     * deadline. Mirrors [VerifyDeployment.timeoutHint]'s actionable core:
     * the dominant cause is a chain the predefined system-cluster nodes do
     * not serve, and the escape hatch is the dapp's own node URL as `network`.
     */
    fun queryTimeoutHint(network: String?, deadlineMs: Long): String =
        "Query timed out: the node(s) produced no answer within the overall ${deadlineMs}ms " +
            "deadline. Likely cause: the chain is not served by the queried " +
            "${network?.let { "\"$it\"" } ?: "default-network"} node(s) - " +
            "a chain hosted in a cluster the predefined system nodes do not serve stalls exactly " +
            "like this - or the node is very slow. If the chain is live, pass the dapp's own node " +
            "URL as `network` to query it directly; otherwise re-check the blockchainRid and " +
            "network, or retry later ($QUERY_DEADLINE_ENV tunes the deadline, capped at " +
            "${MAX_DEADLINE_MS}ms)." + abandonedNote()

    /**
     * The node-error text deployment_preflight records for a reachability
     * probe that outlived the deadline shared by ALL probed URLs. Worded so
     * [VerifyDeployment.failureHint] classifies it as unreachable ("timed
     * out") while the raw text still names the dominant not-served cause.
     */
    fun preflightProbeTimeoutMessage(deadlineMs: Long): String =
        "height probe timed out - no answer within the overall ${deadlineMs}ms reachability " +
            "deadline shared by all probed URLs; a node that does not serve this chain stalls " +
            "exactly like this ($PREFLIGHT_DEADLINE_ENV tunes the deadline, capped at " +
            "${MAX_DEADLINE_MS}ms)" + abandonedNote()
}
