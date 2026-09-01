package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.AssetDistributionStrategy
import org.chromia.tools.DappInteractionStrategy
import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    private val repo = RecordingRepository()
    private val validBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    private fun dappQueryRequest(arguments: JsonElement) = CallToolRequest(
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
        assertNull(repo.lastDapp, "the query must not run with silently emptied arguments")
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
        assertNull(repo.lastDapp)
    }

    @Test
    fun dappQueryObjectAndAbsentArgumentsStillWork() = runBlocking {
        // Valid object still reaches the repository.
        DappInteractionStrategy().execute(
            dappQueryRequest(buildJsonObject { put("name", "CHR") }),
            repo
        )
        assertEquals(mapOf<String, Any?>("name" to "CHR"), repo.lastDapp?.arguments)

        // Absent arguments still means "no arguments".
        DappInteractionStrategy().execute(
            CallToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("blockchainRid", validBrid)
                    put("query", "q")
                }
            ),
            repo
        )
        assertEquals(emptyMap<String, Any?>(), repo.lastDapp?.arguments)
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
                    CallToolRequest(
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
                    CallToolRequest(
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
                    CallToolRequest(
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

    @Test
    fun validAndAbsentListFiltersStillWork() = runBlocking {
        AssetDistributionStrategy().execute(
            CallToolRequest(
                name = "get_asset_distribution",
                arguments = buildJsonObject {
                    put("assetId", "chr")
                    put("brids", buildJsonArray { add(" brid-1 "); add("brid-2") })
                }
            ),
            repo
        )
        assertEquals(listOf("brid-1", "brid-2"), repo.lastAssetFilters?.brids, "entries are trimmed")
        assertNull(repo.lastAssetFilters?.accountTypes, "absent list filter stays unfiltered")
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

    @Test
    fun integerAndStringNumericDappQueryArgumentsStillWork() = runBlocking {
        DappInteractionStrategy().execute(
            dappQueryRequest(
                buildJsonObject {
                    put("page_size", 10)
                    put("big", Long.MAX_VALUE)
                    put("price", "3.14") // decimals as strings pass through
                }
            ),
            repo
        )
        val args = repo.lastDapp!!.arguments
        assertEquals(10, args["page_size"])
        assertEquals(Long.MAX_VALUE, args["big"])
        assertEquals("3.14", args["price"])
    }
}
