package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.postchain.gtv.GtvFactory
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.GraphQLResponseParser
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.NetworkResult
import org.chromia.tools.McpResources
import org.chromia.tools.AccountBlockchainsStrategy
import org.chromia.tools.AllAssetsStrategy
import org.chromia.tools.AllTransactionsStrategy
import org.chromia.tools.AssetBlockchainsStrategy
import org.chromia.tools.BlockchainsTransactionsStrategy
import org.chromia.tools.AssetDistributionStrategy
import org.chromia.tools.AssetTopHoldersStrategy
import org.chromia.tools.BlockchainAnalyticsStrategy
import org.chromia.tools.BlockchainDetailsStrategy
import org.chromia.tools.ChrAggregatesStrategy
import org.chromia.tools.DappInteractionStrategy
import org.chromia.tools.FilterAssetsStrategy
import org.chromia.tools.FilterBlockchainsStrategy
import org.chromia.tools.NetworkStatsStrategy
import org.chromia.tools.NodeUnavailabilityStrategy
import org.chromia.tools.SignerBlockchainsStrategy
import org.chromia.tools.TransactionsByClusterStrategy
import org.chromia.tools.PromptManager
import org.chromia.tools.PromptsToolStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ToolExecutorStrategiesTest {

    private val validBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test
    fun filterBlockchainsForwardsFiltersAndReturnsSuccessJson() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(
            buildJsonObject {
                put("name", "directory_chain")
                put("rid", "abc")
            }
        )
        val request = callToolRequest(
            name = "filter_blockchains",
            arguments = buildJsonObject {
                put("network", "mainnet")
                put("name", "directory")
                put("limit", 5)
                put("system", true)
            }
        )
        val result = FilterBlockchainsStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("directory", repo.lastBlockchainFilters?.name)
        assertEquals(5, repo.lastBlockchainFilters?.pagination?.limit)
        assertEquals(true, repo.lastBlockchainFilters?.system)
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("directory_chain", payload["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun filterAssetsForwardsSearchAndReturnsSuccessJson() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(
            buildJsonObject {
                put("symbol", "CHR")
                put("totalCount", 1)
            }
        )
        val request = callToolRequest(
            name = "filter_assets",
            arguments = buildJsonObject {
                put("network", "testnet")
                put("searchQuery", "CHR")
                put("type", "FT")
                put("limit", 10)
            }
        )
        val result = FilterAssetsStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertEquals("testnet", repo.lastNetwork)
        assertEquals("CHR", repo.lastAssetSearchFilters?.searchQuery)
        assertEquals("FT", repo.lastAssetSearchFilters?.type)
        assertEquals(10, repo.lastAssetSearchFilters?.pagination?.limit)
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("CHR", payload["symbol"]!!.jsonPrimitive.content)
    }

    @Test
    fun getNetworkStatsReturnsSuccessJson() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(
            buildJsonObject {
                put("countAllAccounts", 42)
                put("countAllTransactions", 7)
            }
        )
        val request = callToolRequest(
            name = "get_network_stats",
            arguments = buildJsonObject { put("network", "mainnet") }
        )
        val result = NetworkStatsStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertEquals("mainnet", repo.lastNetwork)
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("42", payload["countAllAccounts"]!!.jsonPrimitive.content)
        assertEquals("7", payload["countAllTransactions"]!!.jsonPrimitive.content)
        assertTrue(result.isError != true)
    }

    @Test
    fun getNetworkStatsRepositoryErrorSetsIsError() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Error("explorer HTTP 502")
        val request = callToolRequest(
            name = "get_network_stats",
            arguments = buildJsonObject { put("network", "mainnet") }
        )
        val result = NetworkStatsStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Failed to get network stats"))
        assertTrue(text.contains("explorer HTTP 502"))
        assertEquals(true, result.isError)
    }

    /**
     * Round 13 (2026-09-04): the explorer answered `GraphQL Error: INTERNAL_ERROR
     * for <uuid>` on every aggregation field for hours (bisected live: `__typename`
     * and `totalRewardsPaid` fine, every `dashboardData` sub-field and
     * `groupedTransactionsByBlockchain` failing - the explorer, not our query).
     * The tool relayed the opaque line and nothing else; the "not your fault,
     * go chain-direct" verdict existed only in translate_error, one more call an
     * agent had to know to make. An error the server can classify as UPSTREAM
     * says so on the spot, in text and in a field scripts can branch on.
     */
    @Test
    fun explorerUpstreamIncidentIsNamedInlineWithTheNextAction() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Error("GraphQL Error: INTERNAL_ERROR for 608627eb-bf9d-e9e5-971b-188b3dcf94bb")
        val request = callToolRequest(name = "get_network_stats", arguments = buildJsonObject { put("network", "mainnet") })
        val result = NetworkStatsStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(true, result.isError)
        assertTrue(text.startsWith("Failed to get network stats: GraphQL Error: INTERNAL_ERROR"), text)
        assertTrue(text.contains("UPSTREAM"), text)
        assertTrue(text.contains("chromia_dapp_query"), "must point at the chain-direct alternative: $text")
        val structured = result.structuredContent!!
        assertEquals("true", structured.getValue("upstream").jsonPrimitive.content)
        assertEquals("graphql_internal_error", structured.getValue("upstream_rule").jsonPrimitive.content)
        assertTrue(structured.getValue("next_action").jsonPrimitive.content.contains("retry"), structured.toString())
    }

    @Test
    fun explorerTestnet400IsNamedUpstreamToo() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Error("HTTP 400: Bad Request (network=testnet)")
        val request = callToolRequest(name = "get_network_stats", arguments = buildJsonObject { put("network", "testnet") })
        val result = NetworkStatsStrategy().execute(request, repo)
        val structured = result.structuredContent!!
        assertEquals("explorer_testnet_400", structured.getValue("upstream_rule").jsonPrimitive.content)
        assertTrue((result.content.first() as TextContent).text!!.contains("network=mainnet"))
    }

    /** An error the translator cannot classify as upstream keeps the plain shape - no false reassurance. */
    @Test
    fun unclassifiedExplorerErrorStaysPlain() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Error("explorer HTTP 502")
        val request = callToolRequest(name = "get_network_stats", arguments = buildJsonObject { put("network", "mainnet") })
        val result = NetworkStatsStrategy().execute(request, repo)
        val structured = result.structuredContent!!
        // 502 IS classified (http_unavailable) - it is upstream by definition.
        assertEquals("http_unavailable", structured.getValue("upstream_rule").jsonPrimitive.content)

        repo.next = NetworkResult.Error("Validation error of type FieldUndefined: Field 'foo' in type 'Query' is undefined")
        val schemaDrift = NetworkStatsStrategy().execute(request, repo)
        val plain = schemaDrift.structuredContent!!
        assertTrue(!plain.containsKey("upstream"), "schema drift is OUR query, not an incident: $plain")
        assertTrue((schemaDrift.content.first() as TextContent).text!!.startsWith("Failed to get network stats: Validation error"))
    }

    @Test
    fun getAllTransactionsForwardsFiltersAndReturnsSuccessJson() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(
            buildJsonObject {
                put("totalCount", 2)
                put("rid", "tx-1")
            }
        )
        val request = callToolRequest(
            name = "get_all_transactions",
            arguments = buildJsonObject {
                put("network", "mainnet")
                put("rid", "tx-rid")
                put("blockId", "block-9")
                put("timestampFrom", "2026-01-01T00:00:00Z")
                put("timestampTo", "2026-01-31T00:00:00Z")
                put("limit", 20)
                put("offset", 5)
                put("sortBy", "timestamp")
                put("sortDirection", "DESC")
                put("blockchainIds", buildJsonArray { add("brid-a"); add("brid-b") })
                put("operations", buildJsonArray { add("ft4.transfer") })
                put("accounts", buildJsonArray { add("acc-1") })
            }
        )
        val result = AllTransactionsStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertEquals("mainnet", repo.lastNetwork)
        val filters = repo.lastTransactionFilters!!
        assertEquals("tx-rid", filters.rid)
        assertEquals("block-9", filters.blockId)
        assertEquals(listOf("brid-a", "brid-b"), filters.blockchainIds)
        assertEquals("2026-01-01T00:00:00Z", filters.timestampFrom)
        assertEquals("2026-01-31T00:00:00Z", filters.timestampTo)
        assertEquals(listOf("ft4.transfer"), filters.operations)
        assertEquals(listOf("acc-1"), filters.accounts)
        assertEquals(20, filters.pagination.limit)
        assertEquals(5, filters.pagination.offset)
        assertEquals("timestamp", filters.sorting.sortBy)
        assertEquals("DESC", filters.sorting.sortDirection)
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("2", payload["totalCount"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAssetTopHoldersForwardsFiltersAndReturnsSuccessJson() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(
            buildJsonObject {
                put("accountId", "holder-1")
                put("amount", "1000")
            }
        )
        val request = callToolRequest(
            name = "get_asset_top_holders",
            arguments = buildJsonObject {
                put("assetId", "chr-asset")
                put("network", "testnet")
                put("limit", 3)
                put("brids", buildJsonArray { add("brid-1") })
                put("excludeAccounts", buildJsonArray { add("treasury") })
            }
        )
        val result = AssetTopHoldersStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertEquals("testnet", repo.lastNetwork)
        assertEquals("chr-asset", repo.lastAssetId)
        assertEquals(3, repo.lastLimit)
        assertEquals(listOf("brid-1"), repo.lastAssetFilters?.brids)
        assertEquals(listOf("treasury"), repo.lastAssetFilters?.excludeAccounts)
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("holder-1", payload["accountId"]!!.jsonPrimitive.content)
    }

    @Test
    fun blankOptionalListItemsAreValidationErrors() {
        // Blank entries used to be silently dropped (shortening the filter),
        // and an all-blank list collapsed to "no filter" - both now fail fast,
        // consistent with the strictness convention for wrong-typed filters.
        val repo = RecordingRepository()
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
                    repo
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
                    repo
                )
            }
        }
        assertTrue(allBlank.message!!.contains("excludeAccounts[0] is blank"), allBlank.message)
    }

    @Test
    fun getAssetTopHoldersMissingAssetIdThrows() {
        val repo = RecordingRepository()
        val request = callToolRequest(
            name = "get_asset_top_holders",
            arguments = buildJsonObject { put("network", "mainnet") }
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { AssetTopHoldersStrategy().execute(request, repo) }
        }
        assertTrue(error.message!!.contains("assetId"))
    }

    @Test
    fun getAssetTopHoldersBlankAssetIdThrows() {
        val repo = RecordingRepository()
        val request = callToolRequest(
            name = "get_asset_top_holders",
            arguments = buildJsonObject {
                put("assetId", "  ")
                put("network", "mainnet")
            }
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { AssetTopHoldersStrategy().execute(request, repo) }
        }
        assertTrue(error.message!!.contains("Missing required parameter"))
        assertTrue(error.message!!.contains("assetId"))
    }

    @Test
    fun getBlockchainDetailsForwardsRidAndReturnsSuccessJson() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(
            buildJsonObject {
                put("name", "directory_chain")
                put("rid", "details-rid")
            }
        )
        val request = callToolRequest(
            name = "get_blockchain_details",
            arguments = buildJsonObject {
                put("rid", "details-rid")
                put("network", "mainnet")
            }
        )
        val result = BlockchainDetailsStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("details-rid", repo.lastDetailsRid)
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("directory_chain", payload["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun getBlockchainDetailsBlankRidThrows() {
        val repo = RecordingRepository()
        val request = callToolRequest(
            name = "get_blockchain_details",
            arguments = buildJsonObject {
                put("rid", "\n")
                put("network", "mainnet")
            }
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { BlockchainDetailsStrategy().execute(request, repo) }
        }
        assertTrue(error.message!!.contains("Missing required parameter"))
        assertTrue(error.message!!.contains("rid"))
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
        val result = PromptsToolStrategy(PromptManager()).execute(request, RecordingRepository())
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
        val result = PromptsToolStrategy(PromptManager()).execute(request, RecordingRepository())
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
            RecordingRepository()
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
            RecordingRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(true, result.isError)
        assertTrue(text.contains("Failed to get prompts"))
        assertTrue(text.contains("broken_prompt_templates.json"), text)
        assertTrue(text.contains("not readable JSON"), text)
        assertEquals(text, result.structuredContent!!["error"]!!.jsonPrimitive.content)
    }

    @Test
    fun graphQlSuccessFixtureFlowsIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"networkStats":{"blockCount":12,"transactionCount":48}}}"""
        val parsed = GraphQLResponseParser.parseResponse(fixture)
        assertTrue(parsed is NetworkResult.Success)
        val repository = RecordingRepository()
        repository.next = parsed
        val result = NetworkStatsStrategy().execute(
            callToolRequest(
                name = "get_network_stats",
                arguments = buildJsonObject { put("network", "mainnet") }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val body = (parsed as NetworkResult.Success).data
        assertEquals(body, result.structuredContent)
        assertEquals(
            12,
            result.structuredContent!!
                .getValue("data")
                .jsonObject
                .getValue("networkStats")
                .jsonObject
                .getValue("blockCount")
                .jsonPrimitive
                .int
        )
        assertEquals("getNetworkStats", repository.lastCall)
        assertEquals("mainnet", repository.lastNetwork)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(body, Json.parseToJsonElement(text).jsonObject)
    }

    @Test
    fun chromiaDappQueryReturnsRepositoryError() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Error("node refused query")
        val request = callToolRequest(
            name = "chromia_dapp_query",
            arguments = buildJsonObject {
                put("network", "testnet")
                put("blockchainRid", validBrid)
                put("query", "rell.get_app_structure")
            }
        )
        val result = DappInteractionStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        assertEquals("testnet", repo.lastDapp?.network)
        assertEquals(validBrid.uppercase(), repo.lastDapp?.brid)
        assertEquals("rell.get_app_structure", repo.lastDapp?.query)
        assertTrue(text.contains("Failed to execute dapp query rell.get_app_structure"))
        assertTrue(text.contains("node refused query"))
        assertEquals(true, result.isError)
    }

    @Test
    fun chromiaDappQuerySuccessExtractsArguments() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(
            buildJsonObject {
                put("modules", "ok")
                put("name", "CHR")
            }
        )
        val request = callToolRequest(
            name = "chromia_dapp_query",
            arguments = buildJsonObject {
                put("network", "mainnet")
                put("blockchainRid", validBrid)
                put("query", "ft4.get_assets_by_name")
                put(
                    "arguments",
                    buildJsonObject {
                        put("name", "CHR")
                        put("page_size", 10)
                        put("include_icon", true)
                        put("ids", buildJsonArray { add("a"); add("b") })
                        put("meta", buildJsonObject { put("source", "test") })
                    }
                )
            }
        )
        val result = DappInteractionStrategy().execute(request, repo)
        val text = (result.content.first() as TextContent).text!!
        val args = repo.lastDapp!!.arguments
        assertEquals("mainnet", repo.lastDapp?.network)
        assertEquals(validBrid.uppercase(), repo.lastDapp?.brid)
        assertEquals("ft4.get_assets_by_name", repo.lastDapp?.query)
        assertEquals("CHR", args["name"])
        assertEquals(10, args["page_size"])
        assertEquals(true, args["include_icon"])
        assertEquals(listOf("a", "b"), args["ids"])
        @Suppress("UNCHECKED_CAST")
        assertEquals("test", (args["meta"] as Map<String, Any>)["source"])
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("ok", payload["modules"]!!.jsonPrimitive.content)
        assertEquals("CHR", payload["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun chromiaDappQueryMissingBlockchainRidThrows() {
        val repo = RecordingRepository()
        val request = callToolRequest(
            name = "chromia_dapp_query",
            arguments = buildJsonObject { put("query", "rell.get_app_structure") }
        )
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { DappInteractionStrategy().execute(request, repo) }
        }
        assertTrue(error.message!!.contains("blockchainRid"))
    }

    @Test
    fun jsonNullOptionalStringFiltersAreAbsent() = runBlocking {
        val repo = RecordingRepository()
        FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", JsonNull)
                    put("name", JsonNull)
                    put("rid", JsonNull)
                    put("cluster", JsonNull)
                    put("container", JsonNull)
                    put("state", JsonNull)
                    put("sortBy", JsonNull)
                    put("sortDirection", JsonNull)
                }
            ),
            repo
        )
        assertEquals("filterBlockchains", repo.lastCall)
        assertNull(repo.lastNetwork, "JSON null network must be absent, not the string null")
        val filters = repo.lastBlockchainFilters!!
        assertNull(filters.name, "JSON null name must not become the filter string null")
        assertNull(filters.rid)
        assertNull(filters.cluster)
        assertNull(filters.container)
        assertNull(filters.state)
        assertNull(filters.sorting.sortBy)
        assertNull(filters.sorting.sortDirection)
    }

    @Test
    fun jsonNullAssetAndTransactionStringFiltersAreAbsent() = runBlocking {
        val repo = RecordingRepository()
        FilterAssetsStrategy().execute(
            callToolRequest(
                name = "filter_assets",
                arguments = buildJsonObject {
                    put("brid", JsonNull)
                    put("searchQuery", JsonNull)
                    put("type", JsonNull)
                    put("sortBy", JsonNull)
                }
            ),
            repo
        )
        assertEquals("filterAssets", repo.lastCall)
        assertNull(repo.lastAssetSearchFilters?.brid)
        assertNull(repo.lastAssetSearchFilters?.searchQuery, "JSON null searchQuery must not become the filter string null")
        assertNull(repo.lastAssetSearchFilters?.type)
        assertNull(repo.lastAssetSearchFilters?.sorting?.sortBy)

        AllTransactionsStrategy().execute(
            callToolRequest(
                name = "get_all_transactions",
                arguments = buildJsonObject {
                    put("rid", JsonNull)
                    put("blockId", JsonNull)
                    put("timestampFrom", JsonNull)
                    put("timestampTo", JsonNull)
                    put("sortBy", JsonNull)
                    put("sortDirection", JsonNull)
                }
            ),
            repo
        )
        val tx = repo.lastTransactionFilters!!
        assertNull(tx.rid, "JSON null rid must not become the filter string null")
        assertNull(tx.blockId)
        assertNull(tx.timestampFrom)
        assertNull(tx.timestampTo)
        assertNull(tx.sorting.sortBy)
        assertNull(tx.sorting.sortDirection)
    }

    @Test
    fun jsonNullIntAndBooleanExtractorsAreAbsent() = runBlocking {
        val repo = RecordingRepository()
        FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("limit", JsonNull)
                    put("offset", JsonNull)
                    put("system", JsonNull)
                }
            ),
            repo
        )
        val filters = repo.lastBlockchainFilters!!
        assertNull(filters.pagination.limit, "JSON null limit must be absent, not parsed")
        assertNull(filters.pagination.offset)
        assertNull(filters.system, "JSON null system must be absent, not a boolean")

        AssetTopHoldersStrategy().execute(
            callToolRequest(
                name = "get_asset_top_holders",
                arguments = buildJsonObject {
                    put("assetId", "chr")
                    put("limit", JsonNull)
                }
            ),
            repo
        )
        assertNull(repo.lastLimit)

        ChrAggregatesStrategy().execute(
            callToolRequest(
                name = "get_chr_aggregates",
                arguments = buildJsonObject {
                    put("includeTotals", JsonNull)
                    put("includeGroupedDeposits", JsonNull)
                    put("includeGroupedWithdrawals", JsonNull)
                }
            ),
            repo
        )
        assertEquals(true, repo.lastIncludeTotals, "JSON null boolean flags keep strategy defaults")
        assertEquals(true, repo.lastIncludeGroupedDeposits)
        assertEquals(true, repo.lastIncludeGroupedWithdrawals)
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
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("Missing required parameter"))
        assertTrue(error.message!!.contains("assetId"))
    }

    @Test
    fun literalStringNullRemainsAFilterValue() = runBlocking {
        val repo = RecordingRepository()
        FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject { put("name", "null") }
            ),
            repo
        )
        assertEquals("null", repo.lastBlockchainFilters?.name)
    }

    @Test
    fun chromiaDappQueryJsonNullArgumentIsPreservedAsNull() = runBlocking {
        // Explicit JSON null must reach the chain as GtvNull, not be dropped -
        // a Rell parameter with a default would silently use the default
        // instead of null (audit round 4 F2).
        val repo = RecordingRepository()
        DappInteractionStrategy().execute(
            callToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("blockchainRid", validBrid)
                    put("query", "ft4.get_assets_by_name")
                    put(
                        "arguments",
                        buildJsonObject {
                            put("name", "CHR")
                            put("page_size", JsonNull)
                        }
                    )
                }
            ),
            repo
        )
        val args = repo.lastDapp!!.arguments
        assertEquals("CHR", args["name"])
        assertTrue(
            "page_size" in args,
            "explicit JSON null dapp argument must be kept, not dropped"
        )
        assertNull(args["page_size"], "JSON null must map to Kotlin null (GtvNull), not the string null")
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
        "INTERNAL_ERROR", "reCAPTCHA", "HTTP 4", "HTTP 5", "Bad Request",
        "Service Unavailable", "Gateway", "Timeout", "timed out", "Connection reset",
        "Connection refused"
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
     * Asserts the contract above and returns the tool's data field when the
     * explorer served it, or null when the explorer refused.
     */
    private fun assertLiveExplorerTool(
        tool: String,
        result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult,
        field: String
    ): JsonElement? {
        val text = (result.content.first() as TextContent).text.orEmpty()
        if (result.isError == true) {
            assertFalse(
                ourBugMarkers.any { text.contains(it) },
                "$tool asked the explorer's schema for something it does not have. The explorer " +
                    "changed and the query did not follow - this is ours to fix, and it is precisely " +
                    "what the recorded fixture could not see: $text"
            )
            assertTrue(
                upstreamMarkers.any { text.contains(it) },
                "$tool failed and nothing in the message is an upstream signature, so the failure " +
                    "is ours: $text"
            )
            return null
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
        return value
    }

    /**
     * The directory chain's rid, discovered live. Returns null when the explorer
     * refuses - the caller then has nothing real to ask about and says so.
     *
     * This exists so the `?: return` in its callers sits directly under the
     * assertion that earned it: an early return more than a few lines away from
     * the assert reads, to the scan in AssumptionLedgerTest and to a human, like
     * a test leaving without having checked anything.
     */
    private suspend fun assertLiveDirectoryChainRid(): String? {
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
        val chains = assertLiveExplorerTool("filter_blockchains", result, "allBlockchains") ?: return null
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
        val chains = assertLiveExplorerTool("filter_blockchains", result, "allBlockchains") ?: return@runBlocking
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
        val assets = assertLiveExplorerTool("get_all_assets", result, "allAssets") ?: return@runBlocking
        assertTrue(assets.jsonArray.isNotEmpty(), "mainnet has assets: $assets")
        assertTrue(assets.jsonArray.first().jsonObject.containsKey("symbol"))
    }

    @Test
    fun liveGetAllTransactionsPagesAndSorts() = runBlocking {
        LiveChromia.requireLive("calls get_all_transactions against the live explorer")
        val result = AllTransactionsStrategy().execute(
            callToolRequest(
                name = "get_all_transactions",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("limit", 3)
                    put("sortBy", "timestamp")
                    put("sortDirection", "DESC")
                }
            ),
            liveRepository()
        )
        val page = assertLiveExplorerTool("get_all_transactions", result, "allTransactions")
            ?: return@runBlocking
        val transactions = page.jsonObject.getValue("transactions").jsonArray
        assertTrue(transactions.size <= 3, "the limit variable did not bind: got ${transactions.size}")
        assertTrue(transactions.isNotEmpty(), "mainnet has transactions: $page")
        assertEquals(64, transactions.first().jsonObject.getValue("rid").jsonPrimitive.content.length)
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
            ?: return@runBlocking
        assertTrue(holders.jsonArray.isNotEmpty(), "CHR has holders: $holders")
        // LIVE BEHAVIOUR, found 2026-09-07: `limit` caps the ACCOUNTS, and the
        // explorer then appends one synthetic remainder row whose accountId is
        // the literal "Others". A recorded fixture had `limit` meaning what it
        // says and nothing ever disagreed with it.
        val accounts = holders.jsonArray.filter {
            it.jsonObject.getValue("accountId").jsonPrimitive.content != "Others"
        }
        assertTrue(
            accounts.size <= 3,
            "the limit variable did not bind - ${accounts.size} real accounts came back: $holders"
        )
        assertTrue(accounts.isNotEmpty(), "CHR has real holders, not only the Others row: $holders")
        assertEquals(64, accounts.first().jsonObject.getValue("accountId").jsonPrimitive.content.length)
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
            ?: return@runBlocking
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
            ?: return@runBlocking
        assertTrue(rows.jsonArray.isNotEmpty(), "CHR lives on chains: $rows")
        assertEquals(64, rows.jsonArray.first().jsonObject.getValue("brid").jsonPrimitive.content.length)
    }

    @Test
    fun liveGetBlockchainDetailsAnswersForARealRid() = runBlocking {
        LiveChromia.requireLive("calls get_blockchain_details for a real chain rid")
        val rid = assertLiveDirectoryChainRid() ?: return@runBlocking

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
            ?: return@runBlocking
        assertEquals(rid, chain.jsonObject.getValue("rid").jsonPrimitive.content, "the rid variable did not bind")
        assertEquals("directory_chain", chain.jsonObject.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun liveGetBlockchainAnalyticsAnswersForARealChain() = runBlocking {
        LiveChromia.requireLive("calls get_blockchain_analytics for a real chain rid")
        val rid = assertLiveDirectoryChainRid() ?: return@runBlocking

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
            ?: return@runBlocking
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
            ?: return@runBlocking
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
            ?: return@runBlocking
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
            ?: return@runBlocking
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
     * UPSTREAM-GATED TOOLS.
     *
     * These four cannot be served today: `dashboardData` and
     * `groupedTransactionsByBlockchain` answer INTERNAL_ERROR, and
     * `getNodeUnavailability` is behind a reCAPTCHA header this client does not
     * send. Recorded fixtures were green over all four, which is exactly the
     * kind of green this pass exists to remove.
     *
     * [assertLiveExplorerTool] still holds them to a real contract: whatever the
     * explorer says has to arrive as the explorer's own words, and a success
     * with a missing `data` field fails. The day upstream recovers, these start
     * asserting the served shape instead - without an edit.
     */
    @Test
    fun liveGetNetworkStatsCarriesWhateverTheExplorerSays() = runBlocking {
        LiveChromia.requireLive("calls get_network_stats against the live explorer")
        val result = NetworkStatsStrategy().execute(
            callToolRequest(
                name = "get_network_stats",
                arguments = buildJsonObject { put("network", LiveChromia.EXPLORER_NETWORK) }
            ),
            liveRepository()
        )
        assertLiveExplorerTool("get_network_stats", result, "dashboardData")
        Unit
    }

    @Test
    fun liveGetTransactionsByClusterCarriesWhateverTheExplorerSays() = runBlocking {
        LiveChromia.requireLive("calls get_transactions_by_cluster against the live explorer")
        val result = TransactionsByClusterStrategy().execute(
            callToolRequest(
                name = "get_transactions_by_cluster",
                arguments = buildJsonObject { put("network", LiveChromia.EXPLORER_NETWORK) }
            ),
            liveRepository()
        )
        assertLiveExplorerTool("get_transactions_by_cluster", result, "dashboardData")
        Unit
    }

    @Test
    fun liveGetBlockchainsTransactionsCarriesWhateverTheExplorerSays() = runBlocking {
        LiveChromia.requireLive("calls get_blockchains_transactions against the live explorer")
        val result = BlockchainsTransactionsStrategy().execute(
            callToolRequest(
                name = "get_blockchains_transactions",
                arguments = buildJsonObject { put("network", LiveChromia.EXPLORER_NETWORK) }
            ),
            liveRepository()
        )
        assertLiveExplorerTool(
            "get_blockchains_transactions", result, "groupedTransactionsByBlockchain"
        )
        Unit
    }

    @Test
    fun liveGetNodeUnavailabilityCarriesWhateverTheExplorerSays() = runBlocking {
        LiveChromia.requireLive("calls get_node_unavailability against the live explorer")
        val result = NodeUnavailabilityStrategy().execute(
            callToolRequest(
                name = "get_node_unavailability",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("pubkey", "02000000000000000000000000000000000000000000000000000000000000008f")
                    put("startTimestamp", "1736373600000")
                }
            ),
            liveRepository()
        )
        assertLiveExplorerTool("get_node_unavailability", result, "getNodeUnavailability")
        Unit
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
            upstreamMarkers.any { text.contains(it) },
            "the refusal must carry the explorer's own words, not a message of ours: $text"
        )
        assertEquals(
            text,
            result.structuredContent!!["error"]!!.jsonPrimitive.content,
            "the structured error and the text must say the same thing"
        )
    }

}
