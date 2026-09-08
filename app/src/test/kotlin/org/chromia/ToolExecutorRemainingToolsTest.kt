package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.AccountBlockchainsStrategy
import org.chromia.tools.AllOperationsStrategy
import org.chromia.tools.AssetBlockchainsStrategy
import org.chromia.tools.AssetDistributionStrategy
import org.chromia.tools.BlockchainAnalyticsStrategy
import org.chromia.tools.ChrAggregatesStrategy
import org.chromia.tools.MonthlyActiveAccountsPerChainStrategy
import org.chromia.tools.SignerBlockchainsStrategy
import org.chromia.tools.TotalRewardsPaidStrategy
import org.chromia.tools.callToolRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

/**
 * THE REMAINING EXPLORER TOOLS, AGAINST THE REAL EXPLORER.
 *
 * Every test in this file used to be the same shape: hand the strategy a
 * recording double of our own `ChromiaRepository`, pre-load it with an invented
 * `{"ok": "<tool name>"}` body, then assert on what the double had captured -
 * `lastCall`, `lastNetwork`, `lastAssetFilters`. A test that supplies the answer
 * and then checks it was asked for proves the strategy called a method; it
 * cannot notice that the explorer renamed a field, retired a query or started
 * refusing the call, and on 2026-09-07 four of the tools tested here turned out
 * to be exactly there (see the DELETED notes below). The double is gone from the
 * repository entirely.
 *
 * What replaces it:
 *
 *  - **live**, for every claim about what a tool does. "The strategy forwarded
 *    brids=[X]" becomes "the live explorer answered with rows for X and nothing
 *    else", which is only true if the argument really travelled.
 *  - **[McpTestSupport.offlineRepository]**, the production repository pointed
 *    at a closed loopback port, for the argument-validation tests. Those throw
 *    before any call is made, and if one ever stopped throwing it would fail
 *    with a real `ConnectException` instead of being handed an invented answer.
 */
class ToolExecutorRemainingToolsTest {

    /** The production repository against the real explorer; no engine, no seam. */
    private val live by lazy { LiveChromia.repository() }

    /**
     * The production repository pointed at a closed port. Used where the
     * repository is only a required parameter of `execute` that the test does
     * not care about, because the call under test throws first.
     */
    private fun offline() = McpTestSupport.offlineRepository()

    /**
     * CHR on mainnet. Public chain data; if the explorer ever stops answering
     * for it, that is a real red and not something a fixture should hide.
     */
    private val chrAssetId = "5F16D1545A0881F971B164F1601CBBF51C29EFD0633B2730DA18C403C3B428B5"

    /** Economy Chain, mainnet - where the bulk of CHR lives. */
    private val economyChainBrid = "15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA"

    /** A second real mainnet chain holding CHR, used as the excludeBrids subject. */
    private val secondChrChainBrid = "19571DCB739CCDDC4BC8B96A01C7BDE9FCC389B566DD1B85737E892695674288"

    /** my_neighbor_alice, a real mainnet dapp chain with real accounts. */
    private val myNeighborAliceBrid = "F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2"

    private fun textOf(result: CallToolResult): String =
        (result.content.first() as TextContent).text!!

    /** Things only the third party can say; matched case-insensitively. */
    private val upstreamMarkers = listOf(
        "internal_error", "recaptcha", "http 4", "http 5", "bad request",
        "service unavailable", "gateway", "timeout", "timed out", "connection reset",
        "connection refused", "connection closed", "no route to host", "unknownhost",
        "request timeout"
    )

    /**
     * NOT upstream. A GraphQL *validation* error means we asked the schema for a
     * field it does not have - the explorer moved and our query did not follow.
     * That is ours, and it is the single most valuable thing a live call catches
     * that a recorded body never can.
     */
    private val ourBugMarkers = listOf("Validation error", "FieldUndefined", "OperationNotSupported")

    /**
     * The same three-branch contract [ToolExecutorStrategiesTest] applies:
     *
     *   OK        the explorer served it - returns `data.<field>` for the caller
     *             to assert on;
     *   UPSTREAM  the explorer refused it - a FAILURE carrying the explorer's own
     *             words and the retry advice. It never returns;
     *   NEVER     a success whose data field is absent, null or unparsed. That is
     *             a swallowed upstream failure dressed as an answer, and it fails
     *             here too.
     */
    private fun assertLiveExplorerTool(
        tool: String,
        result: CallToolResult,
        field: String
    ): JsonElement {
        val text = textOf(result)
        if (result.isError == true) {
            assertFalse(
                ourBugMarkers.any { text.contains(it) },
                "$tool asked the explorer's schema for something it does not have. The explorer " +
                    "changed and the query did not follow - this is ours to fix: $text"
            )
            val lower = text.lowercase()
            fail<Nothing>(
                if (upstreamMarkers.any { lower.contains(it) }) {
                    "$tool FAILED UPSTREAM, and an upstream failure is a RED here. The explorer " +
                        "refused the call, so nothing about $tool was verified by this run: the " +
                        "remedy is to fix or wait for the upstream and RE-RUN, never to pass. A " +
                        "live test that returns early on an upstream marker reports a PASS for a " +
                        "call that answered nothing - which is how get_asset_top_holders stayed " +
                        "green through eight consecutive live INTERNAL_ERRORs (adversary round " +
                        "18, section 4). Only the e2e sweep may tag WARN-UPSTREAM, under its own " +
                        "guardrail. The explorer said: $text"
                } else {
                    "$tool failed and nothing in the message is an upstream signature, so the " +
                        "failure is ours: $text"
                }
            )
        }
        val structured = result.structuredContent
        assertNotNull(structured, "$tool answered without structured content: $text")
        assertEquals(
            structured, Json.parseToJsonElement(text).jsonObject,
            "$tool: the text and the structured content must say the same thing"
        )
        val data = structured!!["data"]
        assertNotNull(
            data,
            "$tool returned a success with no `data` envelope. A swallowed upstream failure dressed " +
                "as an answer is the one outcome this test refuses: $structured"
        )
        val value = data!!.jsonObject[field]
        assertNotNull(
            value,
            "$tool succeeded but `data.$field` is missing - either the explorer renamed it or the " +
                "repository is reading the wrong field: $data"
        )
        assertFalse(
            value is JsonNull,
            "$tool succeeded with `data.$field` null; upstream failures must be errors, not nulls: $data"
        )
        return value!!
    }

    // ==================================================================
    // RETIRED TOOLS
    // ==================================================================
    //
    // DELETED 2026-09-07 (zero-doubles): getBlockchainsTransactionsForwardsNetwork
    // asserted that get_blockchains_transactions passed `network` to the
    // repository and relayed the body back.
    // No real input can produce it: the tool is gone. The explorer answers
    // GraphQL INTERNAL_ERROR for top-level `groupedTransactionsByBlockchain` on
    // every selection set, so the tool, its strategy, its repository method and
    // its query were removed from production on 2026-09-07 (docs/UPSTREAM.md
    // #3a). A tool that advertises data the explorer will not serve is exactly
    // the fake this pass exists to remove, and the recorded body was green over
    // it the whole time. Nothing covers it now, because there is nothing left to
    // cover.
    //
    // DELETED 2026-09-07 (zero-doubles): getTransactionsByClusterForwardsNetwork
    // asserted the same for get_transactions_by_cluster.
    // No real input can produce it: the tool is gone - `dashboardData` answers
    // INTERNAL_ERROR for every selection set (docs/UPSTREAM.md #3a). Nothing
    // covers it now; there is nothing left to cover.
    //
    // DELETED 2026-09-07 (zero-doubles): getNodeUnavailabilityForwardsParams,
    // getNodeUnavailabilityMissingPubkeyThrows and
    // getNodeUnavailabilityMissingStartTimestampThrows asserted that
    // get_node_unavailability forwarded pubkey/startTimestamp and rejected each
    // when missing.
    // No real input can produce it: the tool is gone. explorer.chromia.com gates
    // `getNodeUnavailability` behind an `X-reCAPTCHA-Token` header that no
    // programmatic client can produce (docs/UPSTREAM.md #7a), so the tool was
    // retired on 2026-09-07. Nothing covers it now; there is nothing left to
    // cover. ErrorTranslator still carries the `recaptcha` rule so an agent that
    // hits the gate through another path is told what happened.

    // ==================================================================
    // THE TOOLS THAT SURVIVED, LIVE
    // ==================================================================

    /**
     * DELETED 2026-09-07 (zero-doubles): getAllAssetsForwardsNetwork asserted
     * that get_all_assets passed `network` to the repository and echoed the body.
     * No real input can produce it: the body was `{"ok":"get_all_assets"}`, a
     * shape the explorer never returns. The two real claims inside it are both
     * covered live already - `ToolExecutorStrategiesTest.liveGetAllAssetsAnswers`
     * asserts the explorer's real `data.allAssets`, and
     * `ToolExecutorStrategiesTest.anExplorerRefusalFlowsIntoAnIsErrorResultCarryingItsWords`
     * proves the `network` argument reaches the explorer by taking its real
     * HTTP 400 for network=testnet through this same tool. Duplicating either
     * one here would only cost a second live round trip.
     */
    @Test
    fun liveGetTotalRewardsPaidAnswersARealNumber() = runBlocking {
        LiveChromia.requireLive("calls get_total_rewards_paid against the live explorer")
        val result = TotalRewardsPaidStrategy().execute(
            callToolRequest(
                name = "get_total_rewards_paid",
                arguments = buildJsonObject { put("network", LiveChromia.EXPLORER_NETWORK) }
            ),
            live
        )
        val total = assertLiveExplorerTool("get_total_rewards_paid", result, "totalRewardsPaid")
        // Live, 2026-09-07: the explorer answers a decimal string, not a number.
        // The recorded body had it as whatever the test felt like writing.
        val raw = total.jsonPrimitive.content
        assertTrue(raw.all { it.isDigit() }, "mainnet rewards paid is a decimal string: $raw")
        assertTrue(raw.toBigInteger() > java.math.BigInteger.ZERO, "mainnet has paid rewards: $raw")
    }

    @Test
    fun liveGetAllOperationsAnswersRealOperationsWithTheirChains() = runBlocking {
        LiveChromia.requireLive("calls get_all_operations against the live explorer")
        val result = AllOperationsStrategy().execute(
            callToolRequest(
                name = "get_all_operations",
                arguments = buildJsonObject { put("network", LiveChromia.EXPLORER_NETWORK) }
            ),
            live
        )
        val operations = assertLiveExplorerTool("get_all_operations", result, "operations")
        val rows = operations.jsonArray
        assertTrue(rows.isNotEmpty(), "mainnet chains expose operations: $operations")
        val first = rows.first().jsonObject
        assertTrue(
            first.getValue("operation").jsonPrimitive.content.isNotBlank(),
            "every row names an operation: $first"
        )
        assertEquals(
            64, first.getValue("brid").jsonPrimitive.content.length,
            "every operation is attributed to a 32-byte chain rid: $first"
        )
    }

    /**
     * ALL FIVE ASSET-DISTRIBUTION FILTERS, BOUND BY THE EXPLORER.
     *
     * The old test asserted that `brids`, `accountTypes`, `excludeAccounts`,
     * `excludeBrids` and `excludeAccountTypes` arrived on the recorder as the
     * lists the test had just written. Live, the filters have to actually
     * filter: CHR's unfiltered distribution on mainnet carries EIF_BRIDGE,
     * DEPOSIT, REWARD_POOL, FOUNDATION and more across a dozen chains, so a
     * result that contains only Economy-Chain FT4_USER rows is only reachable if
     * the include lists narrowed it AND the exclude lists removed what they
     * name. `excludeAccounts` is exercised with an all-zero account id, which
     * excludes nothing real but must still bind as `[String]` or the explorer
     * answers a validation error - which [assertLiveExplorerTool] calls ours.
     */
    @Test
    fun liveGetAssetDistributionFiltersActuallyFilter() = runBlocking {
        LiveChromia.requireLive("calls get_asset_distribution for CHR with every filter set")
        val result = AssetDistributionStrategy().execute(
            callToolRequest(
                name = "get_asset_distribution",
                arguments = buildJsonObject {
                    put("assetId", chrAssetId)
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("brids", buildJsonArray { add(economyChainBrid); add(secondChrChainBrid) })
                    put("accountTypes", buildJsonArray { add("FT4_USER"); add("FT4_POOL") })
                    put("excludeBrids", buildJsonArray { add(secondChrChainBrid) })
                    put("excludeAccountTypes", buildJsonArray { add("FT4_POOL") })
                    put(
                        "excludeAccounts",
                        buildJsonArray {
                            add("0000000000000000000000000000000000000000000000000000000000000000")
                        }
                    )
                }
            ),
            live
        )
        val rows = assertLiveExplorerTool("get_asset_distribution", result, "getAssetDistribution")
            .jsonArray
        assertTrue(
            rows.isNotEmpty(),
            "CHR is held by FT4 users on the Economy Chain - an empty answer means brids/accountTypes " +
                "over-filtered: $rows"
        )
        rows.forEach { row ->
            val obj = row.jsonObject
            assertEquals(
                economyChainBrid, obj.getValue("brid").jsonPrimitive.content,
                "excludeBrids did not remove the chain it names, or brids did not bind: $rows"
            )
            assertEquals(
                "FT4_USER", obj.getValue("type").jsonPrimitive.content,
                "excludeAccountTypes did not remove FT4_POOL, or accountTypes did not bind: $rows"
            )
            assertTrue(
                obj.getValue("totalAmount").jsonPrimitive.content.toBigInteger() >
                    java.math.BigInteger.ZERO,
                "a distribution row carries a real balance: $obj"
            )
        }
    }

    @Test
    fun getAssetDistributionMissingAssetIdThrows() {
        // The repository is the production one pointed at a closed port: this
        // must throw before any call is made, and if it ever stops throwing the
        // failure is a real ConnectException rather than an invented answer.
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    offline()
                )
            }
        }
        assertTrue(error.message!!.contains("assetId"))
    }

    /**
     * DELETED 2026-09-07 (zero-doubles): getBlockchainAnalyticsForwardsParams
     * asserted that get_blockchain_analytics passed `brid`, `network` and
     * `fromTimestamp` to the repository.
     * No real input can produce it: the recorded body was `{"ok":...}`. The
     * served path is covered live by
     * `ToolExecutorStrategiesTest.liveGetBlockchainAnalyticsAnswersForARealChain`,
     * which asserts the explorer's real `data.blockchainAnalytics` for the
     * directory chain. The `fromTimestamp` arm has no live coverage and is now
     * UNVERIFIED: measured on 2026-09-07, `blockchainAnalytics` on mainnet
     * exceeds the production 60s client timeout ("Request timeout has expired
     * [url=https://explorer.chromia.com/api/explorer-service?network=mainnet,
     * request_timeout=60000 ms]"), so a live test of that argument would spend a
     * minute to reach the upstream branch and assert nothing. It comes back the
     * day the explorer serves the query inside the timeout.
     */
    @Test
    fun getBlockchainAnalyticsMissingBridThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                BlockchainAnalyticsStrategy().execute(
                    callToolRequest(
                        name = "get_blockchain_analytics",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    offline()
                )
            }
        }
        assertTrue(error.message!!.contains("brid"))
    }

    /**
     * `brid` AND `untilTimestamp`, BOTH BOUND BY THE EXPLORER.
     *
     * Two live calls, because the claim is a comparison and neither half means
     * anything alone: my_neighbor_alice has monthly active accounts today, and
     * asking the same question with `untilTimestamp` set to September 2020 -
     * before the chain existed - has to answer zero. An `untilTimestamp` that
     * never left the process would answer the same number twice.
     */
    @Test
    fun liveGetMonthlyActiveAccountsPerChainBindsBridAndUntilTimestamp() = runBlocking {
        LiveChromia.requireLive("calls get_monthly_active_accounts_per_chain twice against the live explorer")
        suspend fun ask(untilTimestamp: String?): JsonElement {
            val result = MonthlyActiveAccountsPerChainStrategy().execute(
                callToolRequest(
                    name = "get_monthly_active_accounts_per_chain",
                    arguments = buildJsonObject {
                        put("brid", myNeighborAliceBrid)
                        put("network", LiveChromia.EXPLORER_NETWORK)
                        untilTimestamp?.let { put("untilTimestamp", it) }
                    }
                ),
                live
            )
            return assertLiveExplorerTool(
                "get_monthly_active_accounts_per_chain", result, "monthlyActiveAccountsPerChain"
            )
        }

        val now = ask(null)
        assertTrue(
            now.jsonPrimitive.content.toLong() > 0,
            "my_neighbor_alice is a live mainnet chain with active accounts - a zero here means the " +
                "brid did not bind: $now"
        )

        // 1600000000000 ms = 2020-09-13, before this chain existed.
        val beforeItExisted = ask("1600000000000")
        assertEquals(
            0L, beforeItExisted.jsonPrimitive.content.toLong(),
            "untilTimestamp did not reach the explorer: it answered $beforeItExisted for a cutoff " +
                "predating the chain, the same shape of answer as the unfiltered call ($now)"
        )
    }

    @Test
    fun getMonthlyActiveAccountsPerChainMissingBridThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                MonthlyActiveAccountsPerChainStrategy().execute(
                    callToolRequest(
                        name = "get_monthly_active_accounts_per_chain",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    offline()
                )
            }
        }
        assertTrue(error.message!!.contains("brid"))
    }

    /**
     * `includeGroupedDeposits: false` / `includeGroupedWithdrawals: false`, as
     * the explorer honours them.
     *
     * Live, 2026-09-07: an excluded breakdown comes back as an EMPTY array while
     * `totals` still carries real numbers. The old test asserted the three
     * booleans landed on a recorder, which is true of a flag that is read and
     * then dropped just as much as of one that is sent.
     */
    @Test
    fun liveGetChrAggregatesExcludedBreakdownsComeBackEmpty() = runBlocking {
        LiveChromia.requireLive("calls get_chr_aggregates with the grouped breakdowns switched off")
        val result = ChrAggregatesStrategy().execute(
            callToolRequest(
                name = "get_chr_aggregates",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("includeTotals", true)
                    put("includeGroupedDeposits", false)
                    put("includeGroupedWithdrawals", false)
                }
            ),
            live
        )
        val aggregates = assertLiveExplorerTool("get_chr_aggregates", result, "chrAggregates")
            .jsonObject
        assertTrue(
            aggregates.getValue("groupedDeposits").jsonArray.isEmpty(),
            "includeGroupedDeposits=false did not reach the explorer: $aggregates"
        )
        assertTrue(
            aggregates.getValue("groupedWithdrawals").jsonArray.isEmpty(),
            "includeGroupedWithdrawals=false did not reach the explorer: $aggregates"
        )
        val totals = aggregates.getValue("totals").jsonObject
        assertTrue(
            totals.getValue("depositsTotal").jsonPrimitive.content.toBigInteger() >
                java.math.BigInteger.ZERO,
            "includeTotals=true must still bring the totals back: $totals"
        )
        assertTrue(
            totals.getValue("withdrawalsTotal").jsonPrimitive.content.toBigInteger() >
                java.math.BigInteger.ZERO,
            "includeTotals=true must still bring the totals back: $totals"
        )
        // The summariser only adds its `note` when it truncated something; with
        // both breakdowns switched off there is nothing to truncate.
        assertFalse(
            result.structuredContent!!.containsKey("note"),
            "nothing was truncated, so no truncation note belongs on the response: " +
                result.structuredContent
        )
    }

    /**
     * The absent flags default to TRUE, as seen from the other side.
     *
     * With no flags at all the explorer returns both breakdowns populated -
     * 6576 deposit and 2459 withdrawal groups on 2026-09-07 - which the
     * strategy's summariser caps at [org.chromia.tools.CHR_AGGREGATES_ARRAY_CAP]
     * and names in a top-level `note`. A default that had silently become false
     * would come back with the empty arrays the test above asserts.
     */
    @Test
    fun liveGetChrAggregatesDefaultsIncludeBothBreakdowns() = runBlocking {
        LiveChromia.requireLive("calls get_chr_aggregates with no flags, taking the strategy defaults")
        val result = ChrAggregatesStrategy().execute(
            callToolRequest(
                name = "get_chr_aggregates",
                arguments = buildJsonObject { put("network", LiveChromia.EXPLORER_NETWORK) }
            ),
            live
        )
        val aggregates = assertLiveExplorerTool("get_chr_aggregates", result, "chrAggregates")
            .jsonObject
        val deposits = aggregates.getValue("groupedDeposits").jsonArray
        val withdrawals = aggregates.getValue("groupedWithdrawals").jsonArray
        assertTrue(
            deposits.isNotEmpty(),
            "includeGroupedDeposits defaults to true, so the breakdown must come back: $aggregates"
        )
        assertTrue(
            withdrawals.isNotEmpty(),
            "includeGroupedWithdrawals defaults to true, so the breakdown must come back: $aggregates"
        )
        assertTrue(
            deposits.size <= org.chromia.tools.CHR_AGGREGATES_ARRAY_CAP,
            "the summariser caps the grouped arrays: got ${deposits.size}"
        )
        assertTrue(
            withdrawals.size <= org.chromia.tools.CHR_AGGREGATES_ARRAY_CAP,
            "the summariser caps the grouped arrays: got ${withdrawals.size}"
        )
        val first = deposits.first().jsonObject
        assertTrue(first.getValue("address").jsonPrimitive.content.isNotBlank())
        assertTrue(first.getValue("total").jsonPrimitive.content.toBigInteger() > java.math.BigInteger.ZERO)
        // Mainnet has far more groups than the cap, so the note is the honest
        // signal that the agent is looking at a page and not the whole thing.
        assertTrue(
            result.structuredContent!!.getValue("note").jsonPrimitive.content.contains("full:true"),
            "a truncated response must point at the uncapped form: ${result.structuredContent}"
        )
        assertTrue(
            aggregates.getValue("totals").jsonObject
                .getValue("depositsTotal").jsonPrimitive.content.toBigInteger() >
                java.math.BigInteger.ZERO,
            "includeTotals defaults to true: $aggregates"
        )
    }

    /**
     * DELETED 2026-09-07 (zero-doubles): getAssetBlockchainsForwardsParams
     * asserted that get_asset_blockchains passed `assetId` and `network` to the
     * repository and echoed the body back.
     * No real input can produce it: the body was `{"ok":"get_asset_blockchains"}`.
     * `ToolExecutorStrategiesTest.liveGetAssetBlockchainsAnswersForARealAsset`
     * covers it live - it looks CHR up on the explorer and asserts the real
     * `data.getAssetBlockchains` rows carry 32-byte chain rids - so repeating it
     * here would only buy a second round trip.
     */
    @Test
    fun getAssetBlockchainsMissingAssetIdThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AssetBlockchainsStrategy().execute(
                    callToolRequest(
                        name = "get_asset_blockchains",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    offline()
                )
            }
        }
        assertTrue(error.message!!.contains("assetId"))
    }

    /**
     * DELETED 2026-09-07 (zero-doubles): getSignerBlockchainsForwardsParams
     * asserted that get_signer_blockchains forwarded `signer` and `network`, on
     * the strength of the truncated stand-in pubkey "025C06D4".
     * No real input can produce it: "025C06D4" is not a pubkey any signer has,
     * and the recorded `{"ok":...}` body is not a shape the explorer returns.
     * `ToolExecutorStrategiesTest.liveGetSignerBlockchainsAnswersForAnUnknownSigner`
     * covers the tool live with a real 33-byte pubkey that signs nothing. A live
     * test with a signer that DOES sign was tried on 2026-09-07 and abandoned:
     * `signerBlockchains` for an active mainnet signer ran past two minutes and
     * came back as the explorer's own `FUNCTION_INVOCATION_TIMEOUT`, so the
     * populated branch stays unverified rather than being bought with a
     * two-minute test that ends in the upstream branch anyway.
     */
    @Test
    fun getSignerBlockchainsMissingSignerThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                SignerBlockchainsStrategy().execute(
                    callToolRequest(
                        name = "get_signer_blockchains",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    offline()
                )
            }
        }
        assertTrue(error.message!!.contains("signer"))
    }

    /**
     * DELETED 2026-09-07 (zero-doubles): getAccountBlockchainsForwardsParams
     * asserted that get_account_blockchains forwarded the stand-in account id
     * "acc-42" and `network`.
     * No real input can produce it: "acc-42" is not an account id and
     * `{"ok":"get_account_blockchains"}` is not an explorer response.
     * `ToolExecutorStrategiesTest.liveGetAccountBlockchainsAnswersForARealAccount`
     * covers it live, against a real top CHR holder discovered from the explorer
     * in the same test.
     */
    @Test
    fun getAccountBlockchainsMissingAccountIdThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AccountBlockchainsStrategy().execute(
                    callToolRequest(
                        name = "get_account_blockchains",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    offline()
                )
            }
        }
        assertTrue(error.message!!.contains("accountId"))
    }
}
