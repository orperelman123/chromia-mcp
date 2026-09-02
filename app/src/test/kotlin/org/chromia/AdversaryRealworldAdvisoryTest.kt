package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs the gate over the REAL adversary dApps preserved verbatim in
 * exploit-corpus/realworld/adversary-1 - not the distilled samples. This is
 * the ground truth the economic advisories were built against: four dApps an
 * agent built through this MCP, all four certified ok:true, two drainable.
 *
 * Two things are pinned per dApp:
 *  1. the economic advisory for its real exploit FIRES on the real code, and
 *  2. every economic advisory is MEDIUM - the dApps still pass the gate on
 *     the advisory rules alone, by design: these are design judgments the
 *     gate cannot prove, and a blocking heuristic trains agents to route
 *     around it. The advisory is the trace that the designer was told.
 */
class AdversaryRealworldAdvisoryTest {

    private val advisoryRules = setOf(
        "majority-without-quorum",
        "unbounded-voting-period",
        "unbacked-conversion-credit",
        "value-sink-without-withdrawal"
    )

    private fun analyzeDapp(name: String): RellSecurityCheck.Result {
        val url = javaClass.classLoader.getResource("exploit-corpus/realworld/adversary-1/$name/src/main.rell")
            ?: error("adversary dApp source not on test classpath: $name")
        return RellSecurityCheck.analyze(mapOf("main.rell" to File(url.toURI()).readText()))
    }

    private fun assertAdvisory(result: RellSecurityCheck.Result, rule: String, dapp: String) {
        assertTrue(
            result.findings.any { it.rule == rule },
            "$rule must fire on the real $dapp; got ${result.findings.map { "${it.severity} ${it.rule}" }}"
        )
        assertTrue(
            result.findings.filter { it.rule in advisoryRules }.all { it.severity == "MEDIUM" },
            "economic advisories must all be MEDIUM on $dapp; got ${result.findings}"
        )
    }

    /** V1: zero-contribution account proposes paying itself, votes 1-0, executes. */
    @Test
    fun daoTreasuryGetsQuorumAndVotingWindowAdvisories() {
        val result = analyzeDapp("dapp_c_dao")
        assertAdvisory(result, "majority-without-quorum", "dapp_c_dao")
        assertAdvisory(result, "unbounded-voting-period", "dapp_c_dao")
    }

    /** V2/V3: transient oracle price turns 100 USD into 2e8 - no reserve backs the credit. */
    @Test
    fun oracleVaultGetsUnbackedConversionAdvisory() {
        val result = analyzeDapp("dapp_d_oracle")
        assertAdvisory(result, "unbacked-conversion-credit", "dapp_d_oracle")
    }

    /** V6: dapp_a's treasury collects transfer fees and has no withdrawal path. */
    @Test
    fun pointsLedgerGetsLockedTreasuryAdvisory() {
        val result = analyzeDapp("dapp_a_points")
        assertAdvisory(result, "value-sink-without-withdrawal", "dapp_a_points")
    }

    /** V6: dapp_b's fee_pot - same locked-sink shape through the helper alias. */
    @Test
    fun marketEscrowGetsLockedFeePotAdvisory() {
        val result = analyzeDapp("dapp_b_market")
        assertAdvisory(result, "value-sink-without-withdrawal", "dapp_b_market")
    }
}
