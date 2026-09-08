package org.chromia

import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.client.GraphQLResponseParser
import org.chromia.domain.NetworkResult
import org.chromia.tools.McpResources
import org.chromia.tools.AccountBlockchainsStrategy
import org.chromia.tools.AllAssetsStrategy
import org.chromia.tools.AllTransactionsStrategy
import org.chromia.tools.AssetBlockchainsStrategy
import org.chromia.tools.AssetDistributionStrategy
import org.chromia.tools.AssetTopHoldersStrategy
import org.chromia.tools.BlockchainAnalyticsStrategy
import org.chromia.tools.BlockchainDetailsStrategy
import org.chromia.tools.ChrAggregatesStrategy
import org.chromia.tools.DappInteractionStrategy
import org.chromia.tools.FilterAssetsStrategy
import org.chromia.tools.FilterBlockchainsStrategy
import org.chromia.tools.SignerBlockchainsStrategy
import org.chromia.tools.PromptManager
import org.chromia.tools.PromptsToolStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ToolExecutorStrategiesTest {

    private val validBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    /**
     * The production repository pointed at a REAL closed loopback port. Used by
     * the tests below that must not reach the network at all: they assert a
     * validation failure raised BEFORE any call, so a tool that started making
     * one would fail with a genuine ConnectException instead of being handed an
     * invented answer. This replaced `RecordingRepository`, a double of our own
     * `ChromiaRepository` that these tests were handed purely to be ignored.
     */
    private fun offline() = McpTestSupport.offlineRepository()

    // ------------------------------------------------------------------
    // ARGUMENT MAPPING, PROVED BY THE THING THAT RECEIVES THE ARGUMENTS
    // ------------------------------------------------------------------
    //
    // Twelve tests used to live here in the shape "hand the strategy a
    // RecordingRepository, then assert on what the recorder recorded". That
    // proves the strategy called a method with the arguments the test then
    // looked for - a restatement, with no explorer and no chain in it, and it
    // was green over four tools the explorer had stopped serving entirely.
    //
    // What replaces each of them is the same claim asserted where it can
    // actually fail: the LIVE explorer either filters by the argument or it does
    // not. Each deletion names its replacement.
    //
    //   filterBlockchainsForwardsFiltersAndReturnsSuccessJson
    //       -> liveFilterBlockchainsFiltersByNameAndSystem (name, system, limit,
    //          cluster and the structuredContent/text equality, all live)
    //   filterAssetsForwardsSearchAndReturnsSuccessJson
    //       -> liveFilterAssetsFiltersBySearchQuery
    //   getAllTransactionsForwardsFiltersAndReturnsSuccessJson
    //       -> liveGetAllTransactionsPagesAndSorts (limit, sortBy, sortDirection
    //          and a real blockchainIds filter)
    //   getAssetTopHoldersForwardsFiltersAndReturnsSuccessJson
    //       -> liveGetAssetTopHoldersAnswersForARealAsset (limit and a real
    //          excludeAccounts, checked against the account it excludes)
    //   getBlockchainDetailsForwardsRidAndReturnsSuccessJson
    //       -> liveGetBlockchainDetailsAnswersForARealRid
    //   chromiaDappQuerySuccessExtractsArguments and
    //   chromiaDappQueryJsonNullArgumentIsPreservedAsNull
    //       -> chromiaDappQueryNestedListMapArgsAreBoundByTheLiveChain, where a
    //          nested struct with a list<byte_array>, a text, an integer and
    //          three explicit nulls is BOUND by Rell on the live Economy Chain.
    //          A dropped null cannot bind that struct at all. (Not covered
    //          there: a boolean argument - no FT4 query on the Economy Chain
    //          takes one, so boolean argument conversion is now unverified.)
    //   getNetworkStatsReturnsSuccessJson, getNetworkStatsRepositoryErrorSetsIsError,
    //   graphQlSuccessFixtureFlowsIntoHandleResultStructuredContent
    //       -> the tool is retired (see the retirement note further down); the
    //          success shape is asserted live by every live explorer test, which
    //          all check `structuredContent == Json.parse(text)`.

    @Test
    fun blankOptionalListItemsAreValidationErrors() {
        // Blank entries used to be silently dropped (shortening the filter),
        // and an all-blank list collapsed to "no filter" - both now fail fast,
        // consistent with the strictness convention for wrong-typed filters.
        val blankEntry = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AssetTopHoldersStrategy().execute(
                    callToolRequest(
                        name = "get_asset_top_holders",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("excludeAccounts", buildJsonArray { add(""); add("   "); add("keep") })
                        }
                    ),
                    offline()
                )
            }
        }
        assertTrue(blankEntry.message!!.contains("excludeAccounts[0] is blank"), blankEntry.message)

        val allBlank = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("excludeAccounts", buildJsonArray { add(""); add("  ") })
                        }
                    ),
                    offline()
                )
            }
        }
        assertTrue(allBlank.message!!.contains("excludeAccounts[0] is blank"), allBlank.message)
    }

    @Test
    fun getAssetTopHoldersMissingAssetIdThrows() {
        val request = callToolRequest(
            name = "get_asset_top_holders",
            arguments = buildJsonObject { put("network", "mainnet") }
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { AssetTopHoldersStrategy().execute(request, offline()) }
        }
        assertTrue(error.message!!.contains("assetId"))
    }

    @Test
    fun getAssetTopHoldersBlankAssetIdThrows() {
        val request = callToolRequest(
            name = "get_asset_top_holders",
            arguments = buildJsonObject {
                put("assetId", "  ")
                put("network", "mainnet")
            }
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { AssetTopHoldersStrategy().execute(request, offline()) }
        }
        assertTrue(error.message!!.contains("Missing required parameter"))
        assertTrue(error.message!!.contains("assetId"))
    }

    @Test
    fun getBlockchainDetailsBlankRidThrows() {
        val request = callToolRequest(
            name = "get_blockchain_details",
            arguments = buildJsonObject {
                put("rid", "\n")
                put("network", "mainnet")
            }
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { BlockchainDetailsStrategy().execute(request, offline()) }
        }
        assertTrue(error.message!!.contains("Missing required parameter"))
        assertTrue(error.message!!.contains("rid"))
    }

    @Test
    fun chromiaDappQueryMissingBlockchainRidThrows() {
        val request = callToolRequest(
            name = "chromia_dapp_query",
            arguments = buildJsonObject { put("query", "rell.get_app_structure") }
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { DappInteractionStrategy().execute(request, offline()) }
        }
        assertTrue(error.message!!.contains("blockchainRid"))
    }

    @Test
    fun jsonNullRequiredParameterIsMissing() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AssetTopHoldersStrategy().execute(
                    callToolRequest(
                        name = "get_asset_top_holders",
                        arguments = buildJsonObject { put("assetId", JsonNull) }
                    ),
                    offline()
                )
            }
        }
        assertTrue(error.message!!.contains("Missing required parameter"))
        assertTrue(error.message!!.contains("assetId"))
    }

    /**
     * THE GRAPHQL ENVELOPE, PARSED BY THE REAL PARSER.
     *
     * `GraphQLResponseParser` is our own code and a response body is DATA, so
     * this drives the parser directly with an envelope of the shape the explorer
     * sends. What it used to do as well - push the parsed value through a
     * strategy via a RecordingRepository and check it came out the other end -
     * is now asserted live by every test in the live section, each of which
     * requires `structuredContent` to equal the parsed text.
     */
    @Test
    fun theGraphQlEnvelopeIsUnwrappedIntoTheDataObject() {
        val parsed = GraphQLResponseParser.parseResponse(
            """{"data":{"blockchain":{"name":"directory_chain","state":"RUNNING"}}}"""
        )
        assertTrue(parsed is NetworkResult.Success, parsed.toString())
        val data = (parsed as NetworkResult.Success).data
        assertEquals(
            "directory_chain",
            data.getValue("data").jsonObject.getValue("blockchain").jsonObject
                .getValue("name").jsonPrimitive.content
        )
    }

    @Test
    fun getPromptsFiltersByCategoryAndSearch() = runBlocking {
        val request = callToolRequest(
            name = "get_prompts",
            arguments = buildJsonObject {
                put("category", "dapp_query")
                put("search", "not signed")
            }
        )
        val result = PromptsToolStrategy(PromptManager()).execute(request, offline())
        val text = (result.content.first() as TextContent).text!!
        val payload = Json.parseToJsonElement(text).jsonObject
        val prompts = payload["prompts"]!!.jsonObject
        assertTrue(prompts.containsKey("dapp_query"))
        assertEquals(setOf("dapp_query"), prompts.keys)
        val titles = prompts["dapp_query"]!!.jsonArray.map {
            it.jsonObject["title"]!!.jsonPrimitive.content
        }
        assertEquals(listOf("Execute dApp Query"), titles)
        assertEquals(payload, result.structuredContent)
        assertTrue(result.isError != true)
    }

    @Test
    fun getPromptsFiltersByToolName() = runBlocking {
        val request = callToolRequest(
            name = "get_prompts",
            arguments = buildJsonObject { put("tool", "filter_blockchains") }
        )
        val result = PromptsToolStrategy(PromptManager()).execute(request, offline())
        val text = (result.content.first() as TextContent).text!!
        val payload = Json.parseToJsonElement(text).jsonObject
        val prompts = payload["prompts"]!!.jsonObject
        assertTrue(prompts.isNotEmpty())
        prompts.values.forEach { category ->
            category.jsonArray.forEach { prompt ->
                val tool = prompt.jsonObject["tool"]!!.jsonPrimitive.content
                assertTrue(tool.endsWith("filter_blockchains"), "unexpected tool $tool")
            }
        }
        assertEquals(payload, result.structuredContent)
        assertTrue(result.isError != true)
    }

    @Test
    fun getPromptsStructuredContentMatchesCatalog() = runBlocking {
        val result = PromptsToolStrategy(PromptManager()).execute(
            callToolRequest(name = "get_prompts", arguments = buildJsonObject {}),
            offline()
        )
        val text = (result.content.first() as TextContent).text!!
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals(payload, result.structuredContent)
        assertTrue(result.isError != true)
        val catalog = Json.parseToJsonElement(McpResources.classpathText("prompt_templates.json")).jsonObject
        assertEquals(catalog.keys, payload["prompts"]!!.jsonObject.keys)
        assertTrue(payload["prompts"]!!.jsonObject.containsKey("chromia_stack"))
        assertTrue(payload.containsKey("statistics"))
        assertTrue(result.structuredContent!!.containsKey("statistics"))
    }

    @Test
    fun getPromptsFailureSetsStructuredError() = runBlocking {
        // The REAL PromptManager over a REAL malformed catalogue, not a
        // subclass whose getCategories() throws on command: the failure has to
        // come out of the loader an actual broken prompt_templates.json would
        // hit, or this only proves the strategy catches a throw we invented.
        val manager = PromptManager("broken_prompt_templates.json")
        val result = PromptsToolStrategy(manager).execute(
            callToolRequest(name = "get_prompts", arguments = buildJsonObject {}),
            offline()
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(true, result.isError)
        assertTrue(text.contains("Failed to get prompts"))
        assertTrue(text.contains("broken_prompt_templates.json"), text)
        assertTrue(text.contains("not readable JSON"), text)
        assertEquals(text, result.structuredContent!!["error"]!!.jsonPrimitive.content)
    }

    /**
     * JSON NULL IS ABSENT, THE STRING "null" IS A VALUE - ASKED OF THE EXPLORER.
     *
     * Five tests used to assert this against a RecordingRepository by reading
     * the filter model the strategy had built. The bug the convention exists to
     * prevent is not "the model held a null"; it is a filter string `"null"`
     * reaching the explorer and silently matching nothing (audit round 4 F2), so
     * the explorer is the only place the difference shows. It does, exactly:
     * `name` absent returns rows, `name = "null"` returns none - verified live
     * 2026-09-07, and the same for `filter_assets{searchQuery}`.
     */
    @Test
    fun liveJsonNullFiltersAreAbsentAndTheLiteralStringNullIsAValue() = runBlocking {
        LiveChromia.requireLive("asks the explorer to tell a JSON null filter from the string \"null\"")
        val repository = liveRepository()

        val nulls = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("name", JsonNull)
                    put("rid", JsonNull)
                    put("cluster", JsonNull)
                    put("container", JsonNull)
                    put("sortBy", JsonNull)
                    put("sortDirection", JsonNull)
                    put("limit", 5)
                }
            ),
            repository
        )
        val unfiltered = assertLiveExplorerTool("filter_blockchains", nulls, "allBlockchains")
        assertTrue(
            unfiltered.jsonArray.isNotEmpty(),
            "every filter was an explicit JSON null, so nothing should have been filtered: $unfiltered"
        )

        val literal = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("name", "null")
                    put("limit", 5)
                }
            ),
            repository
        )
        val matched = assertLiveExplorerTool("filter_blockchains", literal, "allBlockchains")
        assertTrue(
            matched.jsonArray.isEmpty(),
            "the string \"null\" is a NAME, and no mainnet chain is called that - if this is not " +
                "empty the literal was dropped and the call was silently unfiltered: $matched"
        )

        val assets = FilterAssetsStrategy().execute(
            callToolRequest(
                name = "filter_assets",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("brid", JsonNull)
                    put("searchQuery", JsonNull)
                    put("type", JsonNull)
                    put("sortBy", JsonNull)
                    put("limit", 5)
                }
            ),
            repository
        )
        val rows = assertLiveExplorerTool("filter_assets", assets, "filterAssets")
        assertTrue(
            rows.jsonObject.getValue("assets").jsonArray.isNotEmpty(),
            "JSON-null asset filters must not filter: $rows"
        )
    }

    /**
     * The int and boolean extractors, same question, same judge. A JSON null
     * `limit` must be ABSENT (the explorer then answers its own default page,
     * which is larger than any limit this test would pass), and JSON-null
     * booleans must keep the strategy's defaults - `get_chr_aggregates` defaults
     * all three flags to true, and `includeGroupedDeposits = false` really does
     * empty that array, so a live non-empty `groupedDeposits` is the proof that
     * the null did not collapse to false.
     */
    @Test
    fun liveJsonNullIntsAndBooleansKeepTheirDefaults() = runBlocking {
        LiveChromia.requireLive("asks the explorer whether a JSON null limit and null flags were dropped")
        val repository = liveRepository()

        val limited = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("limit", 3)
                }
            ),
            repository
        )
        val threeRows = assertLiveExplorerTool("filter_blockchains", limited, "allBlockchains")
        assertEquals(3, threeRows.jsonArray.size, "limit = 3 did not bind: $threeRows")

        val nullLimit = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("limit", JsonNull)
                    put("offset", JsonNull)
                }
            ),
            repository
        )
        val defaultPage = assertLiveExplorerTool("filter_blockchains", nullLimit, "allBlockchains")
        assertTrue(
            defaultPage.jsonArray.size > 3,
            "a JSON null limit must be ABSENT, not parsed into one - the explorer then answers its " +
                "OWN default page, which is larger than the 3 above: got ${defaultPage.jsonArray.size} rows"
        )

        // A JSON null `system` must not become a boolean. Neither `true` nor
        // `false` can produce a page holding BOTH kinds of chain, so a page that
        // does is proof the argument was absent. (The explorer's default page is
        // too small to settle it: on 2026-09-07 its first ten rows were all
        // system=false, which is why the limit is explicit here.)
        val nullSystem = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("system", JsonNull)
                    put("limit", 300)
                }
            ),
            repository
        )
        val everyChain = assertLiveExplorerTool("filter_blockchains", nullSystem, "allBlockchains")
        val systems = everyChain.jsonArray.map { it.jsonObject.getValue("system").jsonPrimitive.content }
        assertTrue(
            systems.contains("true") && systems.contains("false"),
            "a JSON null `system` must not filter to one kind of chain - mainnet has both system and " +
                "application chains, and only an ABSENT filter returns both: ${systems.distinct()}"
        )

        val aggregates = ChrAggregatesStrategy().execute(
            callToolRequest(
                name = "get_chr_aggregates",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("includeTotals", JsonNull)
                    put("includeGroupedDeposits", JsonNull)
                    put("includeGroupedWithdrawals", JsonNull)
                }
            ),
            repository
        )
        val chr = assertLiveExplorerTool("get_chr_aggregates", aggregates, "chrAggregates")
        assertTrue(
            chr.jsonObject.getValue("groupedDeposits").jsonArray.isNotEmpty(),
            "JSON null boolean flags keep the strategy's `true` default; false really does empty " +
                "this array, so an empty one means the null became false: $chr"
        )
        assertNotNull(chr.jsonObject["totals"], "includeTotals defaulted away: $chr")
    }

    /**
     * NESTED LIST AND MAP ARGUMENTS, BOUND BY THE REAL CHAIN.
     *
     * This used to hand `PostchainClientService` a trailing-lambda query client
     * that inspected the Gtv it received and then produced the answer itself, so
     * what it proved was that `listMapAndPrimitivesToGtv` builds the Gtv the test
     * expected - a restatement of the test.
     *
     * `ft4.get_assets_filtered` on the live Economy Chain takes a nested struct
     * carrying a `list<byte_array>` and a `text`, next to a top-level `integer`.
     * Rell BINDS all of it: a conversion producing the wrong Gtv type cannot bind
     * at all, and one that bound the wrong values comes back with the wrong
     * asset. The whole tool path - JSON arguments, the strategy, the repository,
     * the converter, the wire, the strict gson, handleResult - is exercised, and
     * the verdict is the chain's.
     */
    @Test
    fun chromiaDappQueryNestedListMapArgsAreBoundByTheLiveChain() = runBlocking {
        LiveChromia.requireLive("sends a nested dict holding a list to the live Economy Chain via chromia_dapp_query")
        val repository = LiveChromia.repository()

        val asset = DappInteractionStrategy().execute(
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
        assertTrue(asset.isError != true, (asset.content.first() as TextContent).text)
        val assetId = asset.structuredContent!!.getValue("id").jsonPrimitive.content
        val assetName = asset.structuredContent!!.getValue("name").jsonPrimitive.content

        val result = DappInteractionStrategy().execute(
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
                                    put("ids", buildJsonArray { add(assetId) })
                                    put("name", assetName)
                                    put("symbol", JsonNull)
                                    put("type", JsonNull)
                                }
                            )
                            put("page_size", 2)
                            put("page_cursor", JsonNull)
                        }
                    )
                }
            ),
            repository
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val rows = result.structuredContent!!.getValue("data").jsonArray
        assertEquals(
            1, rows.size,
            "the nested id list and the name both had to bind for exactly this asset to come back: " +
                result.structuredContent
        )
        assertEquals(assetId, rows[0].jsonObject.getValue("id").jsonPrimitive.content)
        assertEquals(assetName, rows[0].jsonObject.getValue("name").jsonPrimitive.content)
    }

    // ==================================================================
    // THE EXPLORER TOOLS, LIVE
    // ==================================================================
    //
    // Seventeen tests used to live here, each one a MockEngine holding a
    // recorded envelope plus assertions over the request the engine had
    // captured. Between them they proved two things: that the repository builds
    // the GraphQL document and variables we think it builds, and that
    // handleResult copies a 200 body into structuredContent.
    //
    // The first half is a claim about our own code with no third party in it at
    // all, and it belongs - and already lives - in GraphQLQueryTest, which
    // asserts directly on GraphQLQuery.toJsonObject(): blank strings dropped,
    // empty and all-blank lists omitted, lists encoded as JSON arrays. Nothing
    // there needs an engine, and nothing there was lost.
    //
    // The second half was the problem. A recorded 200 cannot notice that the
    // explorer renamed a field, started requiring a reCAPTCHA token, or began
    // answering INTERNAL_ERROR - and when these were converted, on 2026-09-07,
    // four of the sixteen tools turned out to be exactly there:
    //
    //   get_network_stats            dashboardData -> INTERNAL_ERROR
    //   get_transactions_by_cluster  dashboardData -> INTERNAL_ERROR
    //   get_blockchains_transactions groupedTransactionsByBlockchain -> INTERNAL_ERROR
    //   get_node_unavailability      "reCAPTCHA verification failed: token is
    //                                required" - a gate our client cannot pass
    //
    // The fixtures had been green over all four. That is the whole argument for
    // this pass in one paragraph.
    //
    // So the live assertion has two honest branches and one forbidden one, and
    // [assertLiveExplorerTool] enforces all three:
    //
    //   OK        the explorer served it: the named field is present, and the
    //             argument that was supposed to filter actually filtered;
    //   UPSTREAM  the explorer refused it: the refusal must arrive as an error
    //             carrying the explorer's OWN words. This is the same contract
    //             scripts/upstream-classifier.mjs applies in the sweep - the
    //             failure is the third party's, and it is reported as such;
    //   NEVER     a success whose data field is absent, null or unparsed. That
    //             is our bug - a swallowed upstream failure dressed as an
    //             answer - and it is the only outcome that fails here.

    /** The production repository against the real explorer; no engine, no seam. */
    private fun liveRepository() = LiveChromia.repository()

    /**
     * Things only the third party can say. A tool error whose text contains one
     * of these is upstream's refusal; anything else is ours.
     */
    private val upstreamMarkers = listOf(
        "internal_error", "recaptcha", "http 4", "http 5", "bad request",
        "service unavailable", "gateway", "timeout", "timed out", "connection reset",
        "connection refused", "connection closed", "no route to host", "unknownhost",
        // Live, 2026-09-07: blockchainAnalytics exceeded the 60s request timeout on
        // mainnet. ChromiaConfig already carries the note that the heavy explorer
        // analytics were observed past 30s under load; this is that, and it is the
        // explorer's load, not ours. The list is matched case-insensitively because
        // the first version of it missed "Request timeout has expired" by a capital T.
        "request timeout"
    )

    /**
     * THE LEDGER, PER QUERY: which `docs/UPSTREAM.md` entry records this
     * explorer field as broken, dated, so that a refusal of it may be reported
     * as an upstream warning while the rest of the explorer is up.
     *
     * A partial outage is the normal shape of an explorer incident - on
     * 2026-09-08 the canary answered in about a second while these two did not -
     * and the entry is the DEBT: writing one is how an outage stops being
     * invisible, and deleting one is how a fixed upstream stops being excused.
     * `LiveEnv.datedLedgerEntry` refuses an entry that carries no date or does
     * not name the query, so a stale row here cannot quietly keep working.
     */
    private val upstreamLedgerEntries = mapOf(
        // #11: blockchainAnalytics is an unbounded per-transaction-per-account
        // aggregate over a stale index; measured 15-41 s per chain on 2026-09-08
        // and dropping the connection at the 60 s request timeout before that.
        "blockchainAnalytics" to "11",
        // #3b: allBlockchains answers INTERNAL_ERROR the moment `state` is
        // supplied, and has since 2026-09-07.
        "allBlockchains(state:)" to "3b"
    )

    /**
     * NOT upstream. A GraphQL *validation* error means we asked the schema for
     * a field it does not have - the explorer renamed something and our query
     * did not follow. That is our bug, and it is the single most valuable thing
     * a live test catches that a recorded fixture never can, so it must never be
     * waved through as "the third party's problem".
     */
    private val ourBugMarkers = listOf("Validation error", "FieldUndefined", "OperationNotSupported")

    /**
     * Asserts the contract above and returns the tool's data field. There is no
     * "the explorer refused, so we are done" branch: an upstream refusal is a
     * FAILURE carrying the explorer's own words and the retry advice. It cannot
     * return null, so no caller can leave a live test early on one.
     */
    private fun assertLiveExplorerTool(
        tool: String,
        result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult,
        field: String,
        /** The GraphQL form the ledger names, when it is narrower than [field]. */
        upstreamQuery: String = field,
        /** The `docs/UPSTREAM.md` entry recording [upstreamQuery] as broken, if any. */
        ledgerEntry: String? = upstreamLedgerEntries[upstreamQuery]
    ): JsonElement {
        val text = (result.content.first() as TextContent).text.orEmpty()
        if (result.isError == true) {
            assertFalse(
                ourBugMarkers.any { text.contains(it) },
                "$tool asked the explorer's schema for something it does not have. The explorer " +
                    "changed and the query did not follow - this is ours to fix, and it is precisely " +
                    "what the recorded fixture could not see: $text"
            )
            // THE THIRD STATUS (Or, 2026-09-08). A failure that is PROVEN the
            // third party's - an allowlisted signature the explorer alone can
            // produce, PLUS a canary or a dated docs/UPSTREAM.md entry saying it
            // really is not serving this query - ends the test as an UPSTREAM
            // WARNING. That is still a FAILURE in the XML and still verifies
            // nothing about $tool; what changes is that the gate counts and
            // names it separately, so the push waits on the upstream rather than
            // on us. Everything less proven throws a plain red from inside
            // upstreamOutage, and anything with no signature at all never gets
            // there and falls through to the two messages below.
            if (LiveEnv.upstreamSignature(text) != null) {
                LiveEnv.upstreamOutage(
                    tool,
                    LiveEnv.UpstreamEvidence(query = upstreamQuery, errorText = text, ledgerEntry = ledgerEntry)
                )
            }
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
            value is kotlinx.serialization.json.JsonNull,
            "$tool succeeded with `data.$field` null; upstream failures must be errors, not nulls: $data"
        )
        return value!!
    }

    /**
     * The directory chain's rid, discovered live. An explorer that will not
     * answer this is an upstream RED inside [assertLiveExplorerTool]; there is
     * no null for a caller to leave on.
     */
    private suspend fun assertLiveDirectoryChainRid(): String {
        val result = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("name", "directory")
                    put("limit", 1)
                }
            ),
            liveRepository()
        )
        val chains = assertLiveExplorerTool("filter_blockchains", result, "allBlockchains")
        assertTrue(chains.jsonArray.isNotEmpty(), "the live explorer knows the directory chain: $chains")
        return chains.jsonArray.first().jsonObject.getValue("rid").jsonPrimitive.content
    }

    /** The CHR asset, discovered live rather than pinned to a hex string that may move. */
    private suspend fun liveChrAsset(): JsonObject {
        val result = FilterAssetsStrategy().execute(
            callToolRequest(
                name = "filter_assets",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("searchQuery", "CHR")
                    put("limit", 10)
                }
            ),
            liveRepository()
        )
        val assets = assertLiveExplorerTool("filter_assets", result, "filterAssets")
            ?: error("the CHR asset lookup is a precondition for the id-taking explorer tools")
        val rows = assets.jsonObject.getValue("assets").jsonArray
        assertTrue(rows.isNotEmpty(), "searchQuery=CHR matched nothing on mainnet")
        return rows.first { it.jsonObject.getValue("symbol").jsonPrimitive.content == "CHR" }.jsonObject
    }

    @Test
    fun liveFilterBlockchainsFiltersByNameAndSystem() = runBlocking {
        LiveChromia.requireLive("calls filter_blockchains against the live explorer")
        val result = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("name", "directory")
                    put("limit", 5)
                    put("system", true)
                }
            ),
            liveRepository()
        )
        val chains = assertLiveExplorerTool("filter_blockchains", result, "allBlockchains")
        val names = chains.jsonArray.map { it.jsonObject.getValue("name").jsonPrimitive.content }
        assertTrue(
            names.contains("directory_chain"),
            "name=directory + system=true must reach the directory chain - the variables did not bind " +
                "if it did not. Got: $names"
        )
        assertTrue(
            names.all { it.contains("directory") },
            "the name filter did not filter: $names"
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(result.structuredContent, Json.parseToJsonElement(text).jsonObject)

        // cluster + limit, bound by the explorer - the other half of what
        // filterBlockchainsForwardsFiltersAndReturnsSuccessJson used to assert
        // by reading the filter model back out of a RecordingRepository.
        val cluster = chains.jsonArray.first().jsonObject.getValue("cluster").jsonPrimitive.content
        val byCluster = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("cluster", cluster)
                    put("limit", 2)
                }
            ),
            liveRepository()
        )
        val clustered = assertLiveExplorerTool("filter_blockchains", byCluster, "allBlockchains")
        assertEquals(2, clustered.jsonArray.size, "limit = 2 did not bind: $clustered")
        assertTrue(
            clustered.jsonArray.all {
                it.jsonObject.getValue("cluster").jsonPrimitive.content == cluster
            },
            "the cluster filter did not bind: $clustered"
        )
    }

    @Test
    fun liveFilterAssetsFiltersBySearchQuery() = runBlocking {
        LiveChromia.requireLive("calls filter_assets against the live explorer")
        val chr = liveChrAsset()
        assertEquals("CHR", chr.getValue("symbol").jsonPrimitive.content)
        assertEquals(64, chr.getValue("id").jsonPrimitive.content.length, "an FT4 asset id is 32 bytes of hex")
        assertTrue(chr.getValue("decimals").jsonPrimitive.content.toInt() > 0)
    }

    @Test
    fun liveGetAllAssetsAnswers() = runBlocking {
        LiveChromia.requireLive("calls get_all_assets against the live explorer")
        val result = AllAssetsStrategy().execute(
            callToolRequest(
                name = "get_all_assets",
                arguments = buildJsonObject { put("network", LiveChromia.EXPLORER_NETWORK) }
            ),
            liveRepository()
        )
        val assets = assertLiveExplorerTool("get_all_assets", result, "allAssets")
        assertTrue(assets.jsonArray.isNotEmpty(), "mainnet has assets: $assets")
        assertTrue(assets.jsonArray.first().jsonObject.containsKey("symbol"))
    }

    @Test
    fun liveGetAllTransactionsPagesAndSorts() = runBlocking {
        LiveChromia.requireLive("calls get_all_transactions against the live explorer")
        // A real chain id, discovered live, so the blockchainIds LIST variable is
        // bound by the explorer rather than recorded by a fixture. (This is what
        // getAllTransactionsForwardsFiltersAndReturnsSuccessJson used to assert
        // against a RecordingRepository.)
        val chainRid = assertLiveDirectoryChainRid()
        val result = AllTransactionsStrategy().execute(
            callToolRequest(
                name = "get_all_transactions",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("limit", 3)
                    put("sortBy", "timestamp")
                    put("sortDirection", "DESC")
                    // A real chain id, so the blockchainIds list filter is bound by
                    // the explorer rather than recorded by a fixture.
                    put("blockchainIds", buildJsonArray { add(chainRid) })
                }
            ),
            liveRepository()
        )
        val page = assertLiveExplorerTool("get_all_transactions", result, "allTransactions")
        val transactions = page.jsonObject.getValue("transactions").jsonArray
        assertTrue(transactions.size <= 3, "the limit variable did not bind: got ${transactions.size}")
        assertTrue(transactions.isNotEmpty(), "mainnet has transactions: $page")
        assertEquals(64, transactions.first().jsonObject.getValue("rid").jsonPrimitive.content.length)
        assertTrue(
            transactions.all {
                it.jsonObject.getValue("blockchain").jsonObject.getValue("rid").jsonPrimitive.content
                    .equals(chainRid, ignoreCase = true)
            },
            "the blockchainIds list filter did not bind - transactions came back from other chains: $page"
        )
    }

    @Test
    fun liveGetAssetTopHoldersAnswersForARealAsset() = runBlocking {
        LiveChromia.requireLive("calls get_asset_top_holders for a real asset id")
        val assetId = liveChrAsset().getValue("id").jsonPrimitive.content
        val result = AssetTopHoldersStrategy().execute(
            callToolRequest(
                name = "get_asset_top_holders",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("assetId", assetId)
                    put("limit", 3)
                }
            ),
            liveRepository()
        )
        val holders = assertLiveExplorerTool("get_asset_top_holders", result, "getAssetTopHolders")
        assertTrue(holders.jsonArray.isNotEmpty(), "CHR has holders: $holders")
        // LIVE BEHAVIOUR, found 2026-09-07: `limit` caps the ACCOUNTS, and the
        // explorer then appends one synthetic remainder row whose accountId and
        // accountType are both the literal "Others" - so `limit: 3` came back
        // with FOUR entries. A recorded fixture had `limit` meaning what it says
        // and nothing ever disagreed with it.
        //
        // The tool lifts that row out now, so what is asserted here is our
        // contract, live: the list is holders only, it honours `limit` exactly,
        // and the remainder is still reported - under a name that says what it
        // is - instead of being silently dropped or silently counted.
        val accounts = holders.jsonArray
        assertTrue(
            accounts.none { it.jsonObject.getValue("accountId").jsonPrimitive.content == "Others" },
            "the synthetic remainder row is still in the holders list: $holders"
        )
        assertTrue(
            accounts.size <= 3,
            "the limit variable did not bind - ${accounts.size} real accounts came back: $holders"
        )
        assertTrue(accounts.isNotEmpty(), "CHR has real holders, not only the Others row: $holders")
        assertEquals(64, accounts.first().jsonObject.getValue("accountId").jsonPrimitive.content.length)

        val structured = result.structuredContent!!
        assertEquals(
            accounts.size,
            structured.getValue("holderCount").jsonPrimitive.content.toInt(),
            "holderCount must count the holders it returned: $structured"
        )
        val remainder = structured["othersRemainder"]
        assertNotNull(
            remainder,
            "CHR has far more than 3 holders, so the explorer sends its remainder row and the " +
                "tool must report it rather than drop it: $structured"
        )
        assertTrue(
            remainder!!.jsonObject.getValue("totalBalance").jsonPrimitive.content.toBigInteger()
                > java.math.BigInteger.ZERO,
            "the remainder carries the balance of everyone outside the page: $remainder"
        )
        assertTrue(
            remainder.jsonObject.getValue("note").jsonPrimitive.content.contains("NOT a holder"),
            "the remainder must say what it is: $remainder"
        )

        // excludeAccounts, bound by the explorer. This is the half of
        // getAssetTopHoldersForwardsFiltersAndReturnsSuccessJson that a
        // RecordingRepository could only restate: the excluded account is a real
        // top holder, and it has to be absent from the second answer.
        val excluded = accounts.first().jsonObject.getValue("accountId").jsonPrimitive.content
        val filtered = AssetTopHoldersStrategy().execute(
            callToolRequest(
                name = "get_asset_top_holders",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("assetId", assetId)
                    put("limit", 3)
                    put("excludeAccounts", buildJsonArray { add(excluded) })
                }
            ),
            liveRepository()
        )
        val remaining = assertLiveExplorerTool("get_asset_top_holders", filtered, "getAssetTopHolders")
        assertTrue(
            remaining.jsonArray.none {
                it.jsonObject.getValue("accountId").jsonPrimitive.content == excluded
            },
            "excludeAccounts did not bind - $excluded is still there: $remaining"
        )
    }

    /**
     * LIVE, 2026-09-07: a well-formed 64-hex asset id that names nothing comes
     * back from the explorer as HTTP 200 with `{"getAssetTopHolders":[]}` -
     * indistinguishable, on its own, from a real asset whose holders were all
     * filtered out. Passed through unchanged that reads as "this asset has no
     * holders", which for a mistyped id is a wrong answer delivered as a
     * successful one. The tool asks get_asset_blockchains (also empty for an
     * unknown id, a list of chains for a real one) and says which it is.
     */
    @Test
    fun liveGetAssetTopHoldersCallsAnUnknownAssetIdWhatItIs() = runBlocking {
        LiveChromia.requireLive("asks the live explorer for the holders of an asset id it does not know")
        val result = AssetTopHoldersStrategy().execute(
            callToolRequest(
                name = "get_asset_top_holders",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    // Well-formed and deliberately unowned: all zeroes is a valid
                    // 64-hex id and is not an asset on any Chromia network.
                    put("assetId", "0".repeat(64))
                }
            ),
            liveRepository()
        )
        val text = (result.content.first() as TextContent).text.orEmpty()
        assertEquals(
            true, result.isError,
            "an asset id the explorer knows nothing about must be an error, not an empty list " +
                "that reads as \"no holders\": $text"
        )
        assertTrue(text.contains("No such asset"), text)
        assertTrue(text.contains("filter_assets"), "the error must name how to find the right id: $text")
    }

    @Test
    fun liveGetAssetDistributionAnswersForARealAsset() = runBlocking {
        LiveChromia.requireLive("calls get_asset_distribution for a real asset id")
        val assetId = liveChrAsset().getValue("id").jsonPrimitive.content
        val result = AssetDistributionStrategy().execute(
            callToolRequest(
                name = "get_asset_distribution",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("assetId", assetId)
                }
            ),
            liveRepository()
        )
        val rows = assertLiveExplorerTool("get_asset_distribution", result, "getAssetDistribution")
        assertTrue(rows.jsonArray.isNotEmpty(), "CHR is distributed across chains: $rows")
        assertTrue(rows.jsonArray.first().jsonObject.containsKey("totalAmount"))
    }

    @Test
    fun liveGetAssetBlockchainsAnswersForARealAsset() = runBlocking {
        LiveChromia.requireLive("calls get_asset_blockchains for a real asset id")
        val assetId = liveChrAsset().getValue("id").jsonPrimitive.content
        val result = AssetBlockchainsStrategy().execute(
            callToolRequest(
                name = "get_asset_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("assetId", assetId)
                }
            ),
            liveRepository()
        )
        val rows = assertLiveExplorerTool("get_asset_blockchains", result, "getAssetBlockchains")
        assertTrue(rows.jsonArray.isNotEmpty(), "CHR lives on chains: $rows")
        assertEquals(64, rows.jsonArray.first().jsonObject.getValue("brid").jsonPrimitive.content.length)
    }

    @Test
    fun liveGetBlockchainDetailsAnswersForARealRid() = runBlocking {
        LiveChromia.requireLive("calls get_blockchain_details for a real chain rid")
        val rid = assertLiveDirectoryChainRid()

        val result = BlockchainDetailsStrategy().execute(
            callToolRequest(
                name = "get_blockchain_details",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("rid", rid)
                }
            ),
            liveRepository()
        )
        val chain = assertLiveExplorerTool("get_blockchain_details", result, "blockchain")
        assertEquals(rid, chain.jsonObject.getValue("rid").jsonPrimitive.content, "the rid variable did not bind")
        assertEquals("directory_chain", chain.jsonObject.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun liveGetBlockchainAnalyticsAnswersForARealChain() = runBlocking {
        LiveChromia.requireLive("calls get_blockchain_analytics for a real chain rid")
        val rid = assertLiveDirectoryChainRid()

        val result = BlockchainAnalyticsStrategy().execute(
            callToolRequest(
                name = "get_blockchain_analytics",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("brid", rid)
                }
            ),
            liveRepository()
        )
        val analytics = assertLiveExplorerTool("get_blockchain_analytics", result, "blockchainAnalytics")
        assertTrue(
            analytics.jsonObject.getValue("totalTransactions").jsonPrimitive.content.toLong() > 0,
            "the directory chain has transactions: $analytics"
        )
    }

    @Test
    fun liveGetAccountBlockchainsAnswersForARealAccount() = runBlocking {
        LiveChromia.requireLive("calls get_account_blockchains for a real account id")
        val assetId = liveChrAsset().getValue("id").jsonPrimitive.content
        val topHolders = AssetTopHoldersStrategy().execute(
            callToolRequest(
                name = "get_asset_top_holders",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("assetId", assetId)
                    put("limit", 1)
                }
            ),
            liveRepository()
        )
        val holders = assertLiveExplorerTool("get_asset_top_holders", topHolders, "getAssetTopHolders")
        // "Others" is the explorer's synthetic remainder row, not an account.
        val accountId = holders.jsonArray
            .map { it.jsonObject.getValue("accountId").jsonPrimitive.content }
            .first { it != "Others" }

        val result = AccountBlockchainsStrategy().execute(
            callToolRequest(
                name = "get_account_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("accountId", accountId)
                }
            ),
            liveRepository()
        )
        val rows = assertLiveExplorerTool("get_account_blockchains", result, "accountBlockchains")
        assertTrue(
            rows.jsonArray.isNotEmpty(),
            "a top CHR holder must hold it somewhere: $rows"
        )
    }

    /**
     * A real signer pubkey that signs nothing. The explorer answers 200 with an
     * empty list, which is a real response to a real question and exercises the
     * same repository -> parser -> handleResult path a populated one does.
     */
    @Test
    fun liveGetSignerBlockchainsAnswersForAnUnknownSigner() = runBlocking {
        LiveChromia.requireLive("calls get_signer_blockchains for a signer the explorer does not know")
        val result = SignerBlockchainsStrategy().execute(
            callToolRequest(
                name = "get_signer_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("signer", "02000000000000000000000000000000000000000000000000000000000000008f")
                }
            ),
            liveRepository()
        )
        val rows = assertLiveExplorerTool("get_signer_blockchains", result, "signerBlockchains")
        assertTrue(rows.jsonArray.isEmpty(), "an unknown signer signs nothing: $rows")
    }

    @Test
    fun liveGetChrAggregatesAnswers() = runBlocking {
        LiveChromia.requireLive("calls get_chr_aggregates against the live explorer")
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
            liveRepository()
        )
        assertLiveExplorerTool("get_chr_aggregates", result, "chrAggregates")
        Unit
    }

    /**
     * THE FOUR TOOLS THAT WENT.
     *
     * Four live tests stood here on 2026-09-07, one per tool, each asserting
     * "whatever the explorer says must arrive carrying the explorer's own
     * words". They were written the morning the conversion found the tools
     * broken, and they were the wrong answer: a tool whose only honest outcome
     * is the explorer refusing it is a tool that advertises data nobody can get.
     *
     *   get_network_stats             dashboardData -> INTERNAL_ERROR
     *   get_transactions_by_cluster   dashboardData -> INTERNAL_ERROR
     *   get_blockchains_transactions  groupedTransactionsByBlockchain -> INTERNAL_ERROR
     *   get_node_unavailability       "reCAPTCHA verification failed: token is
     *                                 required" - a gate this client cannot pass,
     *                                 and must not try to
     *
     * Schema introspection (2026-09-07) says there is nowhere else to ask: no
     * top-level countAllAccounts / countAllTransfers / monthlyActiveAccounts, no
     * groupedTransactionsByCluster (removed upstream, docs/UPSTREAM.md #3), and
     * per-chain transaction counts only through `blockchainAnalytics(brid)`,
     * which is already its own tool. So the four tools are RETIRED - definitions,
     * strategies, repository methods, GraphQL queries, prompts, sweep checks -
     * and with them these tests and the claim they carried. docs/UPSTREAM.md #3a
     * records the probe.
     *
     * What still covers the shape they were testing: every live test above, and
     * [liveAnUpstreamInternalErrorIsNamedInlineWithTheNextAction] below, which
     * uses the one INTERNAL_ERROR a still-advertised tool can still produce.
     */

    /**
     * AN UPSTREAM INCIDENT, NAMED INLINE, FROM A REAL UPSTREAM INCIDENT.
     *
     * The claim (round 13, 2026-09-04): when the explorer answers INTERNAL_ERROR
     * the tool must not just relay the opaque line - it must say UPSTREAM, point
     * at the chain-direct alternative, and set `upstream` / `upstream_rule` /
     * `next_action` so a script can branch on it. That used to be asserted by
     * writing "GraphQL Error: INTERNAL_ERROR for <uuid>" into a
     * RecordingRepository, i.e. by the test supplying the incident.
     *
     * It does not have to. Verified live 2026-09-07: `allBlockchains` answers
     * normally for rid/name/cluster/container/system/limit/offset and
     * INTERNAL_ERROR the moment a `state` argument is supplied (docs/UPSTREAM.md
     * #3b). That is a real upstream failure, reachable through a real advertised
     * tool, with the explorer's own request id in it.
     *
     * If upstream fixes it this test starts failing on the isError assertion,
     * which is the correct outcome: the note in `filter_blockchains`'s schema
     * and in UPSTREAM.md would then be stale and must come out.
     */
    @Test
    fun liveAnUpstreamInternalErrorIsNamedInlineWithTheNextAction() = runBlocking {
        LiveChromia.requireLive("takes the explorer's real INTERNAL_ERROR on allBlockchains(state:) through a strategy")
        val result = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("state", "RUNNING")
                    put("limit", 5)
                }
            ),
            liveRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(
            true, result.isError,
            "the explorer refuses a `state` filter with INTERNAL_ERROR; if it now serves it, " +
                "delete this test, the note in filter_blockchains's schema and docs/UPSTREAM.md #3b: $text"
        )
        assertTrue(text.lowercase().contains("internal_error"), "the explorer's own words: $text")
        assertTrue(text.contains("UPSTREAM"), "the incident must be named inline: $text")
        assertTrue(text.contains("chromia_dapp_query"), "must point at the chain-direct alternative: $text")
        val structured = result.structuredContent!!
        assertEquals("true", structured.getValue("upstream").jsonPrimitive.content)
        assertEquals("graphql_internal_error", structured.getValue("upstream_rule").jsonPrimitive.content)
        assertTrue(
            structured.getValue("next_action").jsonPrimitive.content.contains("retry"),
            structured.toString()
        )
    }

    /**
     * THE CAPABILITY BEHIND THAT INCIDENT, AND THE DEBT IT LEAVES.
     *
     * The test above proves our error message is good. It does NOT prove
     * `filter_blockchains{state}` works - and `state` is an advertised argument
     * on an advertised tool, so "the explorer refuses it politely" is not the
     * same claim as "an agent can filter chains by state". That second claim has
     * had NO live coverage at all since the field broke, because there was no
     * honest way to write it: a test asserting it would have been red every run,
     * and a red that everyone learns to read as "oh, that one" is the gate
     * crying wolf (lane brief, principle 3).
     *
     * The third status is what makes it writable. This asks the real question of
     * the real explorer through the real tool. Today (docs/UPSTREAM.md #3b,
     * INTERNAL_ERROR every time since 2026-09-07) that is an UPSTREAM WARNING:
     * proven the third party's, counted separately by the gate, printed by name
     * with its evidence, and NOT a pass - nothing about the `state` filter is
     * verified while it warns. The ledger entry is the debt, and it is what
     * makes the warning legal.
     *
     * When ChromaWay fixes the field this test goes GREEN on the assertion
     * below, the warning disappears from the gate line on its own, and
     * docs/UPSTREAM.md #3b plus the note in the tool's schema must come out.
     */
    @Test
    fun liveFilterBlockchainsFiltersByChainState() = runBlocking {
        LiveChromia.requireLive("asks the live explorer to filter chains by `state`, an advertised argument")
        val result = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("state", "RUNNING")
                    put("limit", 5)
                }
            ),
            liveRepository()
        )
        val chains = assertLiveExplorerTool(
            "filter_blockchains", result, "allBlockchains",
            upstreamQuery = "allBlockchains(state:)", ledgerEntry = "3b"
        )
        assertTrue(
            chains.jsonArray.isNotEmpty(),
            "mainnet has RUNNING chains, so a bound `state` filter returns some: $chains"
        )
        // The filter has to have BOUND, not merely been accepted: an ignored
        // argument returns the unfiltered page and would pass the check above.
        val states = chains.jsonArray.map { it.jsonObject.getValue("state").jsonPrimitive.content }
        assertEquals(
            listOf("RUNNING"), states.distinct(),
            "`state: RUNNING` did not filter - these rows are whatever the explorer had: $chains"
        )
    }

    /**
     * AN ERROR THE TRANSLATOR CANNOT CLASSIFY KEEPS THE PLAIN SHAPE.
     *
     * No false reassurance: a failure with no upstream signature must NOT come
     * back wearing `upstream: true`. The honest input is a real closed loopback
     * port - the production client opens a real socket, the operating system
     * refuses it, and "Connection refused" matches no rule in the table (the one
     * connection-refused rule there is scoped to PostgreSQL).
     */
    @Test
    fun anUnclassifiableFailureKeepsThePlainErrorShape() = runBlocking {
        val result = AllAssetsStrategy().execute(
            callToolRequest(
                name = "get_all_assets",
                arguments = buildJsonObject { put("network", "mainnet") }
            ),
            McpTestSupport.offlineRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(true, result.isError, text)
        val structured = result.structuredContent!!
        assertTrue(
            !structured.containsKey("upstream"),
            "a refused socket is not a classified upstream incident, and must not be dressed as " +
                "one: $structured"
        )
        assertEquals(text, structured.getValue("error").jsonPrimitive.content)
    }

    /**
     * A REFUSAL FROM THE EXPLORER, END TO END THROUGH A STRATEGY.
     *
     * The claim is handleResult's: when the repository returns an error, the
     * tool result must be `isError`, its text must carry the explorer's own
     * words, and `structuredContent["error"]` must say the same thing as the
     * text - never an empty success.
     *
     * The first attempt at this used a bogus assetId, on the strength of a curl
     * that produced INTERNAL_ERROR. Live, through the production query, the
     * explorer answered 200 with an empty list instead - so that input is not a
     * refusal and the test was wrong to assume it. `network=testnet` IS a
     * refusal, every time, and it is documented in docs/UPSTREAM.md #9.
     */
    @Test
    fun anExplorerRefusalFlowsIntoAnIsErrorResultCarryingItsWords() = runBlocking {
        LiveChromia.requireLive("takes the explorer's real refusal of network=testnet through a strategy")
        val result = AllAssetsStrategy().execute(
            callToolRequest(
                name = "get_all_assets",
                arguments = buildJsonObject { put("network", "testnet") }
            ),
            liveRepository()
        )
        assertEquals(
            true, result.isError,
            "the explorer refuses network=testnet; a refusal must not come back as a success: $result"
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(
            upstreamMarkers.any { text.lowercase().contains(it) },
            "the refusal must carry the explorer's own words, not a message of ours: $text"
        )
        assertEquals(
            text,
            result.structuredContent!!["error"]!!.jsonPrimitive.content,
            "the structured error and the text must say the same thing"
        )
        // ...and it is classified, inline, as the documented upstream limitation
        // rather than left as an opaque 400. This is what
        // explorerTestnet400IsNamedUpstreamToo used to assert by writing the 400
        // into a RecordingRepository; the explorer writes it now.
        assertEquals(
            "explorer_testnet_400",
            result.structuredContent!!.getValue("upstream_rule").jsonPrimitive.content,
            "the testnet 400 must be named as the upstream limitation it is: $text"
        )
        assertTrue(text.contains("network=mainnet"), "the remedy must be in the text: $text")
    }

}
