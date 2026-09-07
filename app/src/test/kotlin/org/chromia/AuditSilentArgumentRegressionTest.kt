package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import org.chromia.tools.callToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.AssetDistributionStrategy
import org.chromia.tools.DappInteractionStrategy
import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Regressions for the 2026-09-01 silent-argument audit round:
 * F1 chromia_dapp_query with a wrong-typed `arguments` value (JSON-encoded
 *    string, array) used to run the query with NO arguments - wrong-but-
 *    plausible results whenever the Rell query has parameter defaults,
 * F2 the security check's per-name conservative merge only saw CROSS-file
 *    duplicate functions; same-named functions in different namespaces of ONE
 *    file clobbered each other (last definition won), so a benign shadow body
 *    hid an unauthenticated mutation,
 * F3 a present-but-empty list filter (brids: [], and null/blank entries) used
 *    to collapse to "no filter", returning network-wide data as success,
 * minor: decimal / oversized-integer dapp query arguments died in postchain
 *    with the opaque "Cannot convert object of type Double to GTV".
 */
class AuditSilentArgumentRegressionTest {

    /**
     * THE REPOSITORY FOR THE VALIDATION TESTS: the production one, aimed at a
     * closed loopback port ([McpTestSupport.offlineRepository]).
     *
     * Every test that uses it asserts that the tool THROWS before any network
     * call happens, and this repository is what makes that assertion mean
     * something. The old `RecordingRepository` - a double of our own
     * `ChromiaRepository` - answered a cheerful `{"ok":true}` to anything that
     * got through, so `assertNull(repo.lastDapp)` was the test asking the double
     * whether it had been called. Here, a silently-emptied argument list would
     * put the call on a real socket and come back as a CallToolResult carrying
     * the operating system's connection refusal - NOT as the
     * IllegalArgumentException these tests demand. The throw is the proof.
     */
    private val repo = McpTestSupport.offlineRepository()
    private val validBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    /**
     * Mainnet CHR. A permanent, public asset id (docs quote it in the
     * get_asset_blockchains tool description); it is the subject of the live
     * explorer-filter test below and nothing else. If it ever moved, that test
     * would go red for a real reason.
     */
    private val chrAssetId = "5F16D1545A0881F971B164F1601CBBF51C29EFD0633B2730DA18C403C3B428B5"

    private fun dappQueryRequest(arguments: JsonElement) = callToolRequest(
        name = "chromia_dapp_query",
        arguments = buildJsonObject {
            put("blockchainRid", validBrid)
            put("query", "q")
            put("arguments", arguments)
        }
    )

    // ---------------------------------------------------------------- F1

    @Test
    fun dappQueryStringArgumentsIsValidationErrorNotEmptyArguments() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                DappInteractionStrategy().execute(
                    dappQueryRequest(JsonPrimitive("""{"name":"CHR"}""")),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("arguments must be an object"), error.message)
        assertTrue(error.message!!.contains("do not JSON-encode it"), error.message)
        // The throw IS the "did not run with silently emptied arguments" claim:
        // the repository behind this call is real, so a query that got through
        // would have returned a CallToolResult from the socket instead of
        // raising IllegalArgumentException, and assertThrows would have failed.
    }

    @Test
    fun dappQueryArrayArgumentsIsValidationError() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                DappInteractionStrategy().execute(
                    dappQueryRequest(buildJsonArray { add("CHR") }),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("arguments must be an object"), error.message)
    }

    /**
     * F1, DECIDED BY THE CHAIN.
     *
     * The bug was that a wrong-typed `arguments` value ran the query with NO
     * arguments, which produces wrong-but-plausible results whenever the Rell
     * query has parameter defaults. The old version of this test handed a
     * recorder an object and then asked the recorder what it had received - a
     * restatement, and one that could never have caught the bug's real
     * signature, which is a SUCCESS where there should have been a refusal.
     *
     * `get_chr_asset` on the live Economy Chain takes NO parameters, so the
     * chain itself draws the line:
     *
     *   - send `{"name": "CHR"}` and the chain refuses with "Invalid
     *     argument(s): name". That refusal is only possible if the object
     *     actually reached the query - had it been silently emptied, the query
     *     would have SUCCEEDED, which is exactly the failure F1 describes;
     *   - send no `arguments` key at all and the chain answers the asset. So
     *     "absent means no arguments" is the chain's verdict too, not ours.
     */
    @Test
    fun dappQueryObjectAndAbsentArgumentsStillWork() = runBlocking {
        LiveChromia.requireLive("sends an object argument to a zero-parameter query on the live Economy Chain")
        val repository = LiveChromia.repository()

        val withArguments = DappInteractionStrategy().execute(
            callToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("network", LiveChromia.NETWORK)
                    put("blockchainRid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                    put("query", "get_chr_asset")
                    put("arguments", buildJsonObject { put("name", "CHR") })
                }
            ),
            repository
        )
        assertEquals(
            true, withArguments.isError,
            "get_chr_asset takes no parameters; a success here means the `arguments` object was " +
                "dropped on the way to the chain, which is finding F1 itself: $withArguments"
        )
        assertTrue(
            (withArguments.content.first() as TextContent).text!!.contains("Invalid argument(s): name"),
            "the chain must name the argument it received: ${(withArguments.content.first() as TextContent).text}"
        )

        val withoutArguments = DappInteractionStrategy().execute(
            callToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("network", LiveChromia.NETWORK)
                    put("blockchainRid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                    put("query", "get_chr_asset")
                }
            ),
            repository
        )
        assertTrue(
            withoutArguments.isError != true,
            "absent `arguments` must mean no arguments: ${(withoutArguments.content.first() as TextContent).text}"
        )
        assertEquals(
            64,
            withoutArguments.structuredContent!!.getValue("id").jsonPrimitive.content.length,
            "an FT4 asset id is 32 bytes of hex: ${withoutArguments.structuredContent}"
        )
    }

    // ---------------------------------------------------------------- F2

    @Test
    fun sameFileNamespaceShadowedMutatingHelperIsStillFlagged() {
        // Both definitions live in ONE file: the mutating impl.do_write and a
        // later benign shadow.do_write. The name-keyed functionBodies map used
        // to keep only the last body, so the mutation vanished and go() got no
        // unauthenticated-mutation finding - while the identical two-file split
        // WAS flagged.
        val result = RellSecurityCheck.analyze(
            linkedMapOf(
                "main.rell" to "module;\nentity item { key id: text; }\n" +
                    "namespace impl { function do_write(id: text) { create item(id); } }\n" +
                    "namespace shadow { function do_write() { require(true, 'x'); } }\n" +
                    "operation go(id: text) { do_write(id); }\n"
            )
        )
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" && it.text.contains("go") },
            "intra-file shadowed mutating helper must still be flagged: ${result.findings}"
        )
    }

    @Test
    fun sameFileAuthShadowDoesNotCountNameAsAuth() {
        // Mirror case: a non-auth definition first, an auth-marker definition
        // last in the SAME file. Auth only if ALL definitions establish auth,
        // so check_user must NOT suppress the finding.
        val result = RellSecurityCheck.analyze(
            linkedMapOf(
                "main.rell" to "module;\nentity note { key id: text; }\n" +
                    "namespace a { function check_user() { require(true, 'noop'); } }\n" +
                    "namespace b { function check_user() { auth.authenticate(); } }\n" +
                    "operation add_note(id: text) { check_user(); create note(id); }\n"
            )
        )
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" && it.text.contains("add_note") },
            "ambiguous same-named auth helper in one file must not count as auth: ${result.findings}"
        )
    }

    // ---------------------------------------------------------------- F3

    @Test
    fun emptyListFilterIsValidationErrorNotNetworkWideData() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("brids", buildJsonArray {})
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("brids is an empty array"), error.message)
        assertTrue(error.message!!.contains("omit the parameter"), error.message)
    }

    @Test
    fun nullListEntryIsValidationErrorNamingTheIndex() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("brids", buildJsonArray { add("brid-1"); add(JsonNull) })
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("brids[1] is null"), error.message)
    }

    @Test
    fun blankListEntriesAreValidationErrors() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("brids", buildJsonArray { add(""); add(" ") })
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("brids[0] is blank"), error.message)
    }

    /**
     * F3's other half, DECIDED BY THE EXPLORER.
     *
     * Two claims used to be checked by reading them back off a recorder: that a
     * present list filter is trimmed and forwarded, and that an absent one stays
     * unfiltered. The explorer settles both, and it is strict about the first in
     * a way the recorder could not have known: live on 2026-09-07, the same brid
     * with surrounding whitespace matched NOTHING
     * (`getAssetDistribution(brids: ["  <brid>  "]) -> []`) while the trimmed
     * form returned rows. So rows coming back for a padded brid is proof that
     * OUR trim ran; a recorder asserting `listOf("brid-1")` would have been just
     * as green if the explorer had trimmed for us, or if nothing ever trimmed.
     */
    @Test
    fun validAndAbsentListFiltersStillWork() = runBlocking {
        LiveChromia.requireLive("filters get_asset_distribution by a whitespace-padded brid on the live explorer")
        val repository = LiveChromia.repository()

        // Absent list filter: network-wide distribution, and the source of a
        // real brid to filter by.
        val unfiltered = AssetDistributionStrategy().execute(
            callToolRequest(
                name = "get_asset_distribution",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("assetId", chrAssetId)
                }
            ),
            repository
        )
        assertTrue(
            unfiltered.isError != true,
            "the live explorer must serve CHR's distribution: ${(unfiltered.content.first() as TextContent).text}"
        )
        val allRows = unfiltered.structuredContent!!
            .getValue("data").jsonObject.getValue("getAssetDistribution").jsonArray
        assertTrue(allRows.size > 1, "CHR is spread over more than one chain: $allRows")
        val brid = allRows.first().jsonObject.getValue("brid").jsonPrimitive.content

        // Present list filter, with the whitespace an agent's copy-paste brings.
        val filtered = AssetDistributionStrategy().execute(
            callToolRequest(
                name = "get_asset_distribution",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("assetId", chrAssetId)
                    put("brids", buildJsonArray { add("  $brid  ") })
                }
            ),
            repository
        )
        assertTrue(
            filtered.isError != true,
            "the filtered call must be served too: ${(filtered.content.first() as TextContent).text}"
        )
        val filteredRows = filtered.structuredContent!!
            .getValue("data").jsonObject.getValue("getAssetDistribution").jsonArray
        assertTrue(
            filteredRows.isNotEmpty(),
            "the explorer matches brids exactly and does not trim, so an empty answer here means the " +
                "padded brid went out untrimmed: $filteredRows"
        )
        assertTrue(
            filteredRows.size < allRows.size,
            "the brid filter did not filter: ${filteredRows.size} of ${allRows.size} rows came back"
        )
        assertTrue(
            filteredRows.all { it.jsonObject.getValue("brid").jsonPrimitive.content == brid },
            "every row must be on the brid that was asked for: $filteredRows"
        )
    }

    // ------------------------------------------------------------ minor

    @Test
    fun decimalDappQueryArgumentFailsLocallyWithActionableMessage() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                DappInteractionStrategy().execute(
                    dappQueryRequest(buildJsonObject { put("amount", 3.14) }),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("numeric argument 3.14 is not an integer"), error.message)
        assertTrue(error.message!!.contains("send decimals as strings"), error.message)
    }

    @Test
    fun oversizedIntegerDappQueryArgumentFailsLocallyWithActionableMessage() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                DappInteractionStrategy().execute(
                    // Parsed, not built: an integer past Long range stays an
                    // unquoted numeric literal exactly as an agent would send it.
                    dappQueryRequest(Json.parseToJsonElement("""{"amount": 99999999999999999999}""")),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("does not fit a 64-bit integer"), error.message)
        assertTrue(error.message!!.contains("send it as a string"), error.message)
    }

    /**
     * THE OTHER SIDE OF THE MINOR FINDING, BOUND BY THE REAL CHAIN.
     *
     * The two tests above prove a decimal and a past-Long integer fail LOCALLY
     * with an actionable message. This one proves the values that are supposed
     * to work still do - and it used to prove it by reading `repo.lastDapp`,
     * i.e. by asking the recorder to confirm the Kotlin types the converter had
     * just produced. Rell is the authority on that question, and it disagrees
     * out loud: sending `page_size` as a string comes back
     * "Decoding type 'integer': expected INTEGER, actual STRING (parameter:
     * page_size)" (live, 2026-09-07). So the successful call below is not
     * vacuous - the chain would have refused a GtvString - and it covers both
     * halves at once:
     *
     *   - `page_size` = [Long.MAX_VALUE], an unquoted numeric literal at the top
     *     of Long range, binds to a Rell `integer`;
     *   - `name` = "3.14", the documented "send decimals as strings" workaround,
     *     binds to a Rell `text` and is matched against real asset names.
     *
     * (An ordinary small integer is bound live by
     * `ToolExecutorStrategiesTest.chromiaDappQueryNestedListMapArgsAreBoundByTheLiveChain`.)
     */
    @Test
    fun integerAndStringNumericDappQueryArgumentsStillWork() = runBlocking {
        LiveChromia.requireLive("binds a Long.MAX_VALUE integer and a decimal-as-string on the live Economy Chain")
        val repository = LiveChromia.repository()

        val bound = DappInteractionStrategy().execute(
            callToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("network", LiveChromia.NETWORK)
                    put("blockchainRid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                    put("query", "ft4.get_assets_filtered")
                    put(
                        "arguments",
                        buildJsonObject {
                            put(
                                "asset_filter",
                                buildJsonObject {
                                    put("ids", JsonNull)
                                    put("name", "3.14")
                                    put("symbol", JsonNull)
                                    put("type", JsonNull)
                                }
                            )
                            put("page_size", Long.MAX_VALUE)
                            put("page_cursor", JsonNull)
                        }
                    )
                }
            ),
            repository
        )
        assertTrue(
            bound.isError != true,
            "Long.MAX_VALUE must reach Rell as an integer and \"3.14\" as a text: " +
                (bound.content.first() as TextContent).text
        )
        assertEquals(
            0,
            bound.structuredContent!!.getValue("data").jsonArray.size,
            "no asset on the Economy Chain is named \"3.14\"; the text filter bound and matched nothing: " +
                bound.structuredContent
        )

        // The discrimination that makes the call above mean something: the same
        // number sent as a STRING is refused by the chain's own type decoder.
        val asString = DappInteractionStrategy().execute(
            callToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("network", LiveChromia.NETWORK)
                    put("blockchainRid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                    put("query", "ft4.get_assets_filtered")
                    put(
                        "arguments",
                        buildJsonObject {
                            put(
                                "asset_filter",
                                buildJsonObject {
                                    put("ids", JsonNull)
                                    put("name", JsonNull)
                                    put("symbol", JsonNull)
                                    put("type", JsonNull)
                                }
                            )
                            put("page_size", "${Long.MAX_VALUE}")
                            put("page_cursor", JsonNull)
                        }
                    )
                }
            ),
            repository
        )
        assertEquals(true, asString.isError, "the chain type-checks page_size: $asString")
        assertTrue(
            (asString.content.first() as TextContent).text!!.contains("expected INTEGER, actual STRING"),
            "the refusal must be Rell's type decoder: ${(asString.content.first() as TextContent).text}"
        )
    }

    // ------------------------------------- round 6 residuals (entries/strings)

    @Test
    fun objectListEntryIsValidationErrorNamingTheIndex() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("brids", buildJsonArray {
                                add(buildJsonObject { put("accountId", "3008") })
                            })
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("brids[0] must be a string"), error.message)
        assertTrue(error.message!!.contains("an object"), error.message)
        // As above: the throw is the "did not run with a coerced JSON-text
        // filter" claim, because the repository is real and a call that got
        // through would have come back as a result rather than an exception.
    }

    @Test
    fun numberListEntryIsValidationError() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("brids", buildJsonArray { add(123) })
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("brids[0] must be a string"), error.message)
        assertTrue(error.message!!.contains("a number (123)"), error.message)
    }

    @Test
    fun objectForStringParamIsValidationErrorNotLiteralJsonText() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                org.chromia.tools.FilterAssetsStrategy().execute(
                    callToolRequest(
                        name = "filter_assets",
                        arguments = buildJsonObject {
                            put("searchQuery", buildJsonObject { put("q", "CHR") })
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("searchQuery must be a string"), error.message)
        assertTrue(error.message!!.contains("an object"), error.message)
    }

    /**
     * The counterpart of the test above, ON THE LIVE EXPLORER.
     *
     * Numeric-timestamp-as-string coercion is a documented reliance: only
     * object/array values are rejected, a bare number becomes its text. The old
     * version asserted `isError != true` against a recorder that answered
     * `{"ok":true}` to everything, so it could not tell a coerced query from one
     * the explorer would have refused. Here the explorer answers it.
     */
    @Test
    fun numericPrimitiveForStringParamStillCoerces() = runBlocking {
        LiveChromia.requireLive("sends a numeric searchQuery to the live explorer's filter_assets")
        val result = org.chromia.tools.FilterAssetsStrategy().execute(
            callToolRequest(
                name = "filter_assets",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("searchQuery", 42)
                    put("limit", 5)
                }
            ),
            LiveChromia.repository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertFalse(
            text.contains("searchQuery must be a string"),
            "a bare number must coerce to its text, not be rejected: $text"
        )
        assertTrue(result.isError != true, "the live explorer answered the coerced searchQuery: $text")
        assertTrue(
            result.structuredContent!!.getValue("data").jsonObject
                .containsKey("filterAssets"),
            "the explorer served the query it was given: ${result.structuredContent}"
        )
    }
}
