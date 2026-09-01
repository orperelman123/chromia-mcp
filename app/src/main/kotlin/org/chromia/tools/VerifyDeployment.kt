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
    fun failureHint(message: String, network: String): String {
        val m = message.lowercase()
        val unknownChain = listOf(
            "can't find blockchain", "cannot find blockchain", "unknown blockchain",
            "blockchain not found", "404"
        ).any { it in m }
        if (unknownChain) {
            // Live-verified 2026-09-02: the predefined mainnet/testnet endpoints are
            // SYSTEM-cluster nodes, so a dapp chain hosted in another cluster (e.g.
            // "pink") answers 404 here even though it is live on this network.
            return "the queried node(s) do not serve this BRID; check the BRID and network " +
                "(network \"$network\") - a testnet BRID queried on mainnet (or vice versa) fails " +
                "exactly like this. If the BRID is right, the chain may be not on this network, or " +
                "live but hosted in a cluster the predefined \"$network\" system nodes do not serve - " +
                "pass the dapp's own node URL as `network` to verify it directly."
        }
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
