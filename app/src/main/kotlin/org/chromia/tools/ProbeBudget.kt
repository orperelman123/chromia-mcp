package org.chromia.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeoutOrNull

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
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return try {
            val deferred = scope.async { work() }
            withTimeoutOrNull(budgetMs) { deferred.await() }
        } finally {
            scope.cancel()
        }
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
            "${MAX_DEADLINE_MS}ms)."

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
            "${MAX_DEADLINE_MS}ms)"
}
