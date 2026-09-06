package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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
        val manager = object : PromptManager() {
            override fun getCategories(): List<String> = error("catalog boom")
        }
        val result = PromptsToolStrategy(manager).execute(
            callToolRequest(name = "get_prompts", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(true, result.isError)
        assertTrue(text.contains("Failed to get prompts"))
        assertTrue(text.contains("catalog boom"))
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
    fun graphQlErrorsHttp200FlowsThroughParserIntoHandleResultIsError() = runBlocking {
        val fixture = """{"errors":[{"message":"field boom"},{"message":"also bad"}]}"""
        val engine = MockEngine {
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, HttpClient(engine))
        )
        val result = NetworkStatsStrategy().execute(
            callToolRequest(
                name = "get_network_stats",
                arguments = buildJsonObject { put("network", "mainnet") }
            ),
            repository
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Failed to get network stats"))
        assertTrue(text.contains("field boom"))
        assertTrue(text.contains("GraphQL Error"))
        assertEquals(text, result.structuredContent!!["error"]!!.jsonPrimitive.content)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
    }

    @Test
    fun filterBlockchainsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"allBlockchains":[{"rid":"abc","name":"directory_chain","system":true}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", "testnet")
                    put("name", "directory")
                    put("limit", 5)
                    put("system", true)
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        assertEquals(
            "directory_chain",
            structured
                .getValue("data")
                .jsonObject
                .getValue("allBlockchains")
                .jsonArray[0]
                .jsonObject
                .getValue("name")
                .jsonPrimitive
                .content
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("testnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("allBlockchains"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("directory", variables["name"]!!.jsonPrimitive.content)
        assertEquals("5", variables["limit"]!!.jsonPrimitive.content)
        assertEquals("true", variables["system"]!!.jsonPrimitive.content)
    }

    @Test
    fun filterAssetsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"filterAssets":{"assets":[{"name":"Chromia","symbol":"CHR","type":"FT"}],"totalCount":1}}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = FilterAssetsStrategy().execute(
            callToolRequest(
                name = "filter_assets",
                arguments = buildJsonObject {
                    put("network", "mainnet")
                    put("searchQuery", "CHR")
                    put("type", "FT")
                    put("limit", 10)
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        assertEquals(
            "CHR",
            structured
                .getValue("data")
                .jsonObject
                .getValue("filterAssets")
                .jsonObject
                .getValue("assets")
                .jsonArray[0]
                .jsonObject
                .getValue("symbol")
                .jsonPrimitive
                .content
        )
        assertEquals("1", structured.getValue("data").jsonObject.getValue("filterAssets").jsonObject.getValue("totalCount").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("filterAssets"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("CHR", variables["searchQuery"]!!.jsonPrimitive.content)
        assertEquals("FT", variables["type"]!!.jsonPrimitive.content)
        assertEquals("10", variables["limit"]!!.jsonPrimitive.content)
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

    @Test
    fun getAllTransactionsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"allTransactions":{"transactions":[{"rid":"tx-1","timestamp":"2026-01-02T00:00:00Z"}]}}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = AllTransactionsStrategy().execute(
            callToolRequest(
                name = "get_all_transactions",
                arguments = buildJsonObject {
                    put("network", "mainnet")
                    put("rid", "tx-rid")
                    put("blockId", "block-9")
                    put("timestampFrom", "2026-01-01T00:00:00Z")
                    put("limit", 20)
                    put("offset", 5)
                    put("sortBy", "timestamp")
                    put("sortDirection", "DESC")
                    put("blockchainIds", buildJsonArray { add("brid-a"); add("brid-b") })
                    put("operations", buildJsonArray { add("ft4.transfer") })
                    put("accounts", buildJsonArray { add("acc-1") })
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        assertEquals(
            "tx-1",
            structured
                .getValue("data")
                .jsonObject
                .getValue("allTransactions")
                .jsonObject
                .getValue("transactions")
                .jsonArray[0]
                .jsonObject
                .getValue("rid")
                .jsonPrimitive
                .content
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("allTransactions"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("tx-rid", variables["rid"]!!.jsonPrimitive.content)
        assertEquals("block-9", variables["blockId"]!!.jsonPrimitive.content)
        assertEquals("2026-01-01T00:00:00Z", variables["timestampFrom"]!!.jsonPrimitive.content)
        assertEquals(listOf("brid-a", "brid-b"), variables["blockchainIds"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("ft4.transfer"), variables["operations"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("acc-1"), variables["accounts"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("20", variables["limit"]!!.jsonPrimitive.content)
        assertEquals("5", variables["offset"]!!.jsonPrimitive.content)
        assertEquals("timestamp", variables["sortBy"]!!.jsonPrimitive.content)
        assertEquals("DESC", variables["sortDirection"]!!.jsonPrimitive.content)
    }

    @Test
    fun chromiaDappQueryNestedListMapArgsFlowThroughListMapAndPrimitivesToGtv() = runBlocking {
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val postchain = PostchainClientService(config) { blockchainRid, queryName, args ->
            assertEquals(validBrid.uppercase(), blockchainRid.toHex())
            assertEquals("ft4.get_transfers", queryName)
            val dict = args.asDict()
            val items = dict.getValue("items").asArray()
            assertEquals(2, items.size)
            assertEquals("a", items[0].asDict().getValue("id").asString())
            assertEquals(1L, items[0].asDict().getValue("n").asInteger())
            assertEquals("b", items[1].asDict().getValue("id").asString())
            assertEquals(2L, items[1].asDict().getValue("n").asInteger())
            val meta = dict.getValue("meta").asDict()
            assertEquals(listOf("x", "y"), meta.getValue("tags").asArray().map { it.asString() })
            assertEquals("test", meta.getValue("source").asString())
            GtvFactory.gtv(
                mapOf(
                    "ok" to GtvFactory.gtv("nested"),
                    "count" to GtvFactory.gtv(2L)
                )
            )
        }
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, McpTestSupport.errorEngine()),
            postchain
        )
        val result = DappInteractionStrategy().execute(
            callToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("network", "testnet")
                    put("blockchainRid", validBrid)
                    put("query", "ft4.get_transfers")
                    put(
                        "arguments",
                        buildJsonObject {
                            put(
                                "items",
                                buildJsonArray {
                                    add(buildJsonObject { put("id", "a"); put("n", 1) })
                                    add(buildJsonObject { put("id", "b"); put("n", 2) })
                                }
                            )
                            put(
                                "meta",
                                buildJsonObject {
                                    put("tags", buildJsonArray { add("x"); add("y") })
                                    put("source", "test")
                                }
                            )
                        }
                    )
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        assertEquals("nested", result.structuredContent!!["ok"]!!.jsonPrimitive.content)
        assertEquals("2", result.structuredContent!!["count"]!!.jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(result.structuredContent, Json.parseToJsonElement(text).jsonObject)
    }

    @Test
    fun getAssetTopHoldersHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"getAssetTopHolders":[{"accountId":"holder-1","totalBalance":"1000","chainCount":2,"chainBrid":"brid-1","accountType":"FT4_USER"}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = AssetTopHoldersStrategy().execute(
            callToolRequest(
                name = "get_asset_top_holders",
                arguments = buildJsonObject {
                    put("assetId", "chr-asset")
                    put("network", "testnet")
                    put("limit", 3)
                    put("brids", buildJsonArray { add("brid-1") })
                    put("excludeAccounts", buildJsonArray { add("treasury") })
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val holder = structured
            .getValue("data")
            .jsonObject
            .getValue("getAssetTopHolders")
            .jsonArray[0]
            .jsonObject
        assertEquals("holder-1", holder.getValue("accountId").jsonPrimitive.content)
        assertEquals("1000", holder.getValue("totalBalance").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("testnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("getAssetTopHolders"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("chr-asset", variables["assetId"]!!.jsonPrimitive.content)
        assertEquals("3", variables["limit"]!!.jsonPrimitive.content)
        assertEquals(listOf("brid-1"), variables["brids"]!!.jsonArray.map { it.jsonPrimitive.content })
        // Explorer schema uses `excludedAccounts` on getAssetTopHolders (unlike getAssetDistribution).
        assertEquals(listOf("treasury"), variables["excludedAccounts"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun getAssetDistributionHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"getAssetDistribution":[{"brid":"brid-1","type":"FT4_USER","totalAmount":"5000"}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = AssetDistributionStrategy().execute(
            callToolRequest(
                name = "get_asset_distribution",
                arguments = buildJsonObject {
                    put("assetId", "chr-asset")
                    put("network", "mainnet")
                    put("brids", buildJsonArray { add("brid-1") })
                    put("accountTypes", buildJsonArray { add("FT4_USER") })
                    put("excludeAccounts", buildJsonArray { add("treasury") })
                    put("excludeBrids", buildJsonArray { add("brid-x") })
                    put("excludeAccountTypes", buildJsonArray { add("SYSTEM") })
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val row = structured
            .getValue("data")
            .jsonObject
            .getValue("getAssetDistribution")
            .jsonArray[0]
            .jsonObject
        assertEquals("brid-1", row.getValue("brid").jsonPrimitive.content)
        assertEquals("FT4_USER", row.getValue("type").jsonPrimitive.content)
        assertEquals("5000", row.getValue("totalAmount").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("getAssetDistribution"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("chr-asset", variables["assetId"]!!.jsonPrimitive.content)
        assertEquals(listOf("brid-1"), variables["brids"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("FT4_USER"), variables["accountTypes"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("treasury"), variables["excludeAccounts"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("brid-x"), variables["excludeBrids"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("SYSTEM"), variables["excludeAccountTypes"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun getNetworkStatsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"dashboardData":{"countAllAccounts":42,"countAllTransfers":7,"countAllTransactions":99,"monthlyActiveAccounts":3}}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = NetworkStatsStrategy().execute(
            callToolRequest(
                name = "get_network_stats",
                arguments = buildJsonObject { put("network", "testnet") }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val dashboard = structured
            .getValue("data")
            .jsonObject
            .getValue("dashboardData")
            .jsonObject
        assertEquals("42", dashboard.getValue("countAllAccounts").jsonPrimitive.content)
        assertEquals("7", dashboard.getValue("countAllTransfers").jsonPrimitive.content)
        assertEquals("99", dashboard.getValue("countAllTransactions").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("testnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("dashboardData"),
            posted["query"]!!.jsonPrimitive.content
        )
        assertTrue("variables" !in posted, "get_network_stats has no GraphQL variables")
    }

    @Test
    fun getChrAggregatesHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"chrAggregates":{"groupedDeposits":[{"address":"0xabc","networkId":"1","total":"100"}],"groupedWithdrawals":[],"totals":{"depositsTotal":"100","withdrawalsTotal":"0"}}}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = ChrAggregatesStrategy().execute(
            callToolRequest(
                name = "get_chr_aggregates",
                arguments = buildJsonObject {
                    put("network", "mainnet")
                    put("includeTotals", false)
                    put("includeGroupedDeposits", true)
                    put("includeGroupedWithdrawals", false)
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val aggregates = structured
            .getValue("data")
            .jsonObject
            .getValue("chrAggregates")
            .jsonObject
        assertEquals(
            "0xabc",
            aggregates.getValue("groupedDeposits").jsonArray[0].jsonObject.getValue("address").jsonPrimitive.content
        )
        assertEquals("100", aggregates.getValue("totals").jsonObject.getValue("depositsTotal").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("chrAggregates"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("false", variables["includeTotals"]!!.jsonPrimitive.content)
        assertEquals("true", variables["includeGroupedDeposits"]!!.jsonPrimitive.content)
        assertEquals("false", variables["includeGroupedWithdrawals"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAssetBlockchainsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"getAssetBlockchains":[{"brid":"brid-1","transfersCount":12,"isSource":true,"blockchain":{"name":"economy_chain"}}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = AssetBlockchainsStrategy().execute(
            callToolRequest(
                name = "get_asset_blockchains",
                arguments = buildJsonObject {
                    put("assetId", "chr-asset")
                    put("network", "mainnet")
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val row = structured
            .getValue("data")
            .jsonObject
            .getValue("getAssetBlockchains")
            .jsonArray[0]
            .jsonObject
        assertEquals("brid-1", row.getValue("brid").jsonPrimitive.content)
        assertEquals("12", row.getValue("transfersCount").jsonPrimitive.content)
        assertEquals("true", row.getValue("isSource").jsonPrimitive.content)
        assertEquals("economy_chain", row.getValue("blockchain").jsonObject.getValue("name").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("getAssetBlockchains"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("chr-asset", variables["assetId"]!!.jsonPrimitive.content)
    }

    @Test
    fun getBlockchainAnalyticsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"blockchainAnalytics":{"totalTransactions":88,"totalOperations":12,"totalActiveAccounts":4,"transactionsByDay":[{"date":"2026-01-02","value":3}]}}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = BlockchainAnalyticsStrategy().execute(
            callToolRequest(
                name = "get_blockchain_analytics",
                arguments = buildJsonObject {
                    put("brid", "brid-9")
                    put("network", "testnet")
                    put("fromTimestamp", "2026-01-01T00:00:00Z")
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val analytics = structured
            .getValue("data")
            .jsonObject
            .getValue("blockchainAnalytics")
            .jsonObject
        assertEquals("88", analytics.getValue("totalTransactions").jsonPrimitive.content)
        assertEquals("12", analytics.getValue("totalOperations").jsonPrimitive.content)
        assertEquals(
            "2026-01-02",
            analytics.getValue("transactionsByDay").jsonArray[0].jsonObject.getValue("date").jsonPrimitive.content
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("testnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("blockchainAnalytics"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("brid-9", variables["brid"]!!.jsonPrimitive.content)
        assertEquals("2026-01-01T00:00:00Z", variables["fromTimestamp"]!!.jsonPrimitive.content)
    }

    @Test
    fun getNodeUnavailabilityHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"getNodeUnavailability":[{"blockchainRid":"brid-node","intervals":[{"start":"1736373600000","end":"1736377200000"}]}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = NodeUnavailabilityStrategy().execute(
            callToolRequest(
                name = "get_node_unavailability",
                arguments = buildJsonObject {
                    put("pubkey", "02DDAEA3")
                    put("startTimestamp", "1736373600000")
                    put("network", "mainnet")
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val row = structured
            .getValue("data")
            .jsonObject
            .getValue("getNodeUnavailability")
            .jsonArray[0]
            .jsonObject
        assertEquals("brid-node", row.getValue("blockchainRid").jsonPrimitive.content)
        assertEquals(
            "1736373600000",
            row.getValue("intervals").jsonArray[0].jsonObject.getValue("start").jsonPrimitive.content
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("getNodeUnavailability"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("02DDAEA3", variables["pubkey"]!!.jsonPrimitive.content)
        assertEquals("1736373600000", variables["startTimestamp"]!!.jsonPrimitive.content)
    }

    @Test
    fun getSignerBlockchainsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"signerBlockchains":[{"blockchain":{"rid":"brid-s","name":"economy_chain"},"transactionCount":7}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = SignerBlockchainsStrategy().execute(
            callToolRequest(
                name = "get_signer_blockchains",
                arguments = buildJsonObject {
                    put("signer", "025C06D4")
                    put("network", "testnet")
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val row = structured
            .getValue("data")
            .jsonObject
            .getValue("signerBlockchains")
            .jsonArray[0]
            .jsonObject
        assertEquals("brid-s", row.getValue("blockchain").jsonObject.getValue("rid").jsonPrimitive.content)
        assertEquals("economy_chain", row.getValue("blockchain").jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("7", row.getValue("transactionCount").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("testnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("signerBlockchains"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("025C06D4", variables["signer"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAllAssetsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"allAssets":[{"name":"Chromia","symbol":"CHR","id":"chr-asset","brid":"brid-a","type":"ft4","decimals":6,"supply":"1000","transferCount":9,"blockchainCount":2}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = AllAssetsStrategy().execute(
            callToolRequest(
                name = "get_all_assets",
                arguments = buildJsonObject { put("network", "mainnet") }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val row = structured
            .getValue("data")
            .jsonObject
            .getValue("allAssets")
            .jsonArray[0]
            .jsonObject
        assertEquals("Chromia", row.getValue("name").jsonPrimitive.content)
        assertEquals("CHR", row.getValue("symbol").jsonPrimitive.content)
        assertEquals("chr-asset", row.getValue("id").jsonPrimitive.content)
        assertEquals("9", row.getValue("transferCount").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("allAssets"),
            posted["query"]!!.jsonPrimitive.content
        )
        assertTrue("variables" !in posted, "get_all_assets has no GraphQL variables")
    }

    @Test
    fun getAccountBlockchainsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"accountBlockchains":[{"blockchain":{"rid":"brid-acc","name":"user_chain"},"transactionCount":4,"transfersCount":11}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = AccountBlockchainsStrategy().execute(
            callToolRequest(
                name = "get_account_blockchains",
                arguments = buildJsonObject {
                    put("accountId", "acc-42")
                    put("network", "mainnet")
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val row = structured
            .getValue("data")
            .jsonObject
            .getValue("accountBlockchains")
            .jsonArray[0]
            .jsonObject
        assertEquals("brid-acc", row.getValue("blockchain").jsonObject.getValue("rid").jsonPrimitive.content)
        assertEquals("user_chain", row.getValue("blockchain").jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("4", row.getValue("transactionCount").jsonPrimitive.content)
        assertEquals("11", row.getValue("transfersCount").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("accountBlockchains"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("acc-42", variables["accountId"]!!.jsonPrimitive.content)
    }

    @Test
    fun getTransactionsByClusterHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        // Real explorer responses nest this under dashboardData since the top-level
        // groupedTransactionsByCluster field was removed from the schema.
        val fixture = """{"data":{"dashboardData":{"groupedTransactionsByCluster":[{"cluster":"system","count":88},{"cluster":"dapp","count":12}]}}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = TransactionsByClusterStrategy().execute(
            callToolRequest(
                name = "get_transactions_by_cluster",
                arguments = buildJsonObject { put("network", "testnet") }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val rows = structured
            .getValue("data")
            .jsonObject
            .getValue("dashboardData")
            .jsonObject
            .getValue("groupedTransactionsByCluster")
            .jsonArray
        assertEquals("system", rows[0].jsonObject.getValue("cluster").jsonPrimitive.content)
        assertEquals("88", rows[0].jsonObject.getValue("count").jsonPrimitive.content)
        assertEquals("dapp", rows[1].jsonObject.getValue("cluster").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("testnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("groupedTransactionsByCluster"),
            posted["query"]!!.jsonPrimitive.content
        )
        assertTrue("variables" !in posted, "get_transactions_by_cluster has no GraphQL variables")
    }

    @Test
    fun getBlockchainsTransactionsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"groupedTransactionsByBlockchain":[{"brid":"brid-tx","blockchain":{"name":"directory_chain","system":true,"cluster":"system","state":"RUNNING"},"blockHeight":99,"throughput":1.5,"count":42}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = BlockchainsTransactionsStrategy().execute(
            callToolRequest(
                name = "get_blockchains_transactions",
                arguments = buildJsonObject { put("network", "mainnet") }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val row = structured
            .getValue("data")
            .jsonObject
            .getValue("groupedTransactionsByBlockchain")
            .jsonArray[0]
            .jsonObject
        assertEquals("brid-tx", row.getValue("brid").jsonPrimitive.content)
        assertEquals("directory_chain", row.getValue("blockchain").jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("99", row.getValue("blockHeight").jsonPrimitive.content)
        assertEquals("42", row.getValue("count").jsonPrimitive.content)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("groupedTransactionsByBlockchain"),
            posted["query"]!!.jsonPrimitive.content
        )
        assertTrue("variables" !in posted, "get_blockchains_transactions has no GraphQL variables")
    }

    @Test
    fun getBlockchainDetailsHttp200FlowsThroughRepositoryIntoHandleResultStructuredContent() = runBlocking {
        val fixture = """{"data":{"blockchain":{"rid":"rid-1","name":"directory_chain","system":true,"container":"sys","cluster":"system","state":"RUNNING"}}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val config = ChromiaConfig(explorerUrl = "https://example.test/graphql")
        val repository = ChromiaRepositoryImpl(
            config,
            HttpClientService(config, engine)
        )
        val result = BlockchainDetailsStrategy().execute(
            callToolRequest(
                name = "get_blockchain_details",
                arguments = buildJsonObject {
                    put("rid", "rid-1")
                    put("network", "mainnet")
                }
            ),
            repository
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val chain = structured
            .getValue("data")
            .jsonObject
            .getValue("blockchain")
            .jsonObject
        assertEquals("rid-1", chain.getValue("rid").jsonPrimitive.content)
        assertEquals("directory_chain", chain.getValue("name").jsonPrimitive.content)
        assertEquals("true", chain.getValue("system").jsonPrimitive.content)
        assertEquals("RUNNING", chain.getValue("state").jsonPrimitive.content)
        val textContent = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(textContent).jsonObject)
        assertEquals(1, engine.requestHistory.size)
        assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
        assertEquals(1, capturedBodies.size)
        val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
        assertTrue(
            posted["query"]!!.jsonPrimitive.content.contains("blockchain"),
            posted["query"]!!.jsonPrimitive.content
        )
        val variables = posted.getValue("variables").jsonObject
        assertEquals("rid-1", variables["rid"]!!.jsonPrimitive.content)
    }
}
