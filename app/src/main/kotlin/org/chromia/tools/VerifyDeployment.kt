package org.chromia.tools

/**
 * Pure helpers behind the `verify_deployment` tool: BRID normalization and
 * actionable classification of node failures. No network I/O lives here -
 * the strategy drives PostchainClientService through ChromiaRepository.
 */
object VerifyDeployment {

    /** Default pause between the two height reads; clamped by [MAX_WAIT_MS]. */
    const val DEFAULT_WAIT_MS = 2_000L

    /** Hard ceiling so the tool can never hang on a caller-supplied wait. */
    const val MAX_WAIT_MS = 10_000L

    /**
     * Overall wall-clock budget for the WHOLE verification - client
     * construction (which does signer discovery over the network), both height
     * reads, the wait between them, and the optional smoke query. Live probe
     * 2026-09-02 (D1): resolving a mainnet chain hosted in a non-system
     * cluster left the probe running past the hosting platform's 60s proxy
     * write timeout, so the caller got a transport error (socket closed)
     * instead of the actionable not-served hint. The deadline keeps the answer
     * well under that ceiling. Shared with the rest of the blocking
     * postchain-read family via [ProbeBudget].
     */
    const val DEFAULT_DEADLINE_MS = ProbeBudget.DEFAULT_DEADLINE_MS

    /** Hard cap on the configurable deadline - must stay well under the 60s proxy write timeout. */
    const val MAX_DEADLINE_MS = ProbeBudget.MAX_DEADLINE_MS

    /** Floor so a misconfigured deadline cannot make every probe fail instantly. */
    const val MIN_DEADLINE_MS = ProbeBudget.MIN_DEADLINE_MS

    /** Operator override for the overall deadline; clamped to [MIN_DEADLINE_MS]..[MAX_DEADLINE_MS]. */
    const val DEADLINE_ENV = "CHROMIA_MCP_VERIFY_DEADLINE_MS"

    /** Clamp a configured overall deadline; null/garbage means [DEFAULT_DEADLINE_MS]. */
    fun clampDeadlineMs(deadlineMs: Long?): Long = ProbeBudget.clampDeadlineMs(deadlineMs)

    /** The overall deadline from the environment (or the default), always clamped. */
    fun configuredDeadlineMs(raw: String? = System.getenv(DEADLINE_ENV)): Long =
        ProbeBudget.clampDeadlineMs(raw?.trim()?.toLongOrNull())

    /**
     * The notes text for a verification whose height probe outlived the
     * overall deadline. Named causes mirror [failureHint]'s unknown-chain
     * branch: on the predefined system-cluster nodes, a chain hosted in
     * another cluster stalls or 404s - the dapp's own node URL resolves both.
     *
     * A BOGUS BRID legitimately produces EITHER this hint or [failureHint]'s
     * unknown-chain hint, depending on upstream node health (live-verified
     * 2026-09-02): a healthy node answers 404 for an unknown BRID in under a
     * second, but postchain-client's TryNextOnError strategy only surfaces
     * that 404 after crawling EVERY endpoint in the pool (14 on mainnet, up
     * to 60s connect/response timeout each), so with any degraded endpoint
     * the deadline fires first. A shorter first-probe budget would not help:
     * it cannot surface the 404 any faster (the crawl is the bottleneck), it
     * would only deliver THIS hint sooner while flipping slow-but-live chains
     * to live:false. Both hints carry the same actionable core - re-check the
     * BRID, or verify via the dapp's own node URL - which the e2e sweep and
     * VerifyDeploymentToolTest.bogusBridAnswersAgreeOnTheActionableCore pin.
     */
    fun timeoutHint(network: String, deadlineMs: Long): String =
        "Height probe timed out: the node(s) produced no answer for this BRID within the " +
            "overall ${deadlineMs}ms deadline. Likely cause: the chain is not served by the " +
            "queried node(s) - a chain hosted in a cluster the predefined \"$network\" system " +
            "nodes do not serve stalls exactly like this - or the node is very slow. If the " +
            "chain is live, pass the dapp's own node URL as `network` to verify it directly; " +
            "otherwise re-check the BRID and network, or retry later." + ProbeBudget.abandonedNote()

    private val HEX64 = Regex("^[0-9a-fA-F]{64}$")

    /**
     * Accepts a BRID as bare 64-hex, Rell literal form x"..." / X'...', or
     * 0x-prefixed hex; returns the canonical upper-case 64-hex string.
     * Anything else is a validation error with the accepted forms named.
     */
    fun parseBrid(raw: String): String {
        var s = raw.trim()
        if ((s.startsWith("x\"") || s.startsWith("X\"")) && s.endsWith("\"")) {
            s = s.substring(2, s.length - 1)
        } else if ((s.startsWith("x'") || s.startsWith("X'")) && s.endsWith("'")) {
            s = s.substring(2, s.length - 1)
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2)
        }
        require(HEX64.matches(s)) {
            "brid must be a 64-character hex blockchain RID (bare, 0x-prefixed, or x\"...\" form); " +
                "got \"${raw.take(80)}\". Find it in chromia.yml deployments.<network>.chains after " +
                "`chr deployment create`, or via filter_blockchains."
        }
        return s.uppercase()
    }

    /**
     * Turns a raw postchain-client/network failure message into an actionable
     * hint. Unknown-chain answers (the node responded, but not for this BRID)
     * and unreachable-node answers (nothing responded) need different fixes.
     */
    /** The node answered, but not for this BRID (as opposed to not answering at all). */
    fun isUnknownChain(message: String): Boolean {
        val m = message.lowercase()
        return listOf(
            "can't find blockchain", "cannot find blockchain", "unknown blockchain",
            "blockchain not found", "404"
        ).any { it in m }
    }

    /**
     * The Directory lists the chain on [hosts] but a host answered "unknown
     * chain": registered, not serving yet. First real deploy (2026-09-04):
     * ~5 minutes between `chr deployment create` and the first height answer,
     * during which the wrong-BRID hint below was the only thing this tool said.
     */
    fun startingHint(hosts: List<String>): String =
        "Chain is REGISTERED but not serving yet: the Directory chain lists it on ${hosts.joinToString(", ")} " +
            "and the node(s) answered 'unknown blockchain' - a chain created in the last minutes is still " +
            "starting on its cluster (observed: ~5 min on testnet). The BRID and network are right; re-run " +
            "verify_deployment in 1-2 minutes, and use chromia_dapp_query as soon as live=true."

    fun failureHint(message: String, network: String): String {
        if (isUnknownChain(message)) {
            // Live-verified 2026-09-02: the predefined mainnet/testnet endpoints are
            // SYSTEM-cluster nodes, so a dapp chain hosted in another cluster (e.g.
            // "pink") answers 404 here even though it is live on this network.
            return "the queried node(s) do not serve this BRID; check the BRID and network " +
                "(network \"$network\") - a testnet BRID queried on mainnet (or vice versa) fails " +
                "exactly like this. If the BRID is right, the chain may be not on this network, or " +
                "live but hosted in a cluster the predefined \"$network\" system nodes do not serve - " +
                "pass the dapp's own node URL as `network` to verify it directly."
        }
        val m = message.lowercase()
        val unreachable = listOf(
            "unknownhost", "unknown host", "connection refused", "connect timed out",
            "timed out", "timeout", "no route to host", "failed to connect",
            "connection reset", "service unavailable", "503", "502"
        ).any { it in m }
        if (unreachable) {
            return "the node could not be reached - check the network name or node URL; if it is " +
                "correct, the node may be down or slow (upstream issue), retry shortly."
        }
        return "the node answered with an unexpected error - paste it into translate_error for a " +
            "diagnosis."
    }

    /** Clamp a caller-supplied wait into [0, MAX_WAIT_MS]. */
    fun clampWaitMs(waitMs: Long?): Long =
        (waitMs ?: DEFAULT_WAIT_MS).coerceIn(0L, MAX_WAIT_MS)
}
