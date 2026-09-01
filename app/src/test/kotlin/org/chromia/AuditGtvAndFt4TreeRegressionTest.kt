package org.chromia

import java.math.BigInteger
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvBigInteger
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvInteger
import net.postchain.gtv.GtvNull
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.NetworkResult
import org.chromia.tools.RellCheck
import org.chromia.tools.RunRellTests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the 2026-09-01 audit round:
 * F1 - chromia_dapp_query responses containing a Rell big_integer (every FT4
 *      balance/total_supply/amount query) errored although the chain succeeded,
 *      because make_gtv_gson()'s BIGINTEGER branch throws. makeStrictGtvGson()
 *      serializes big_integer as a JSON string; everything else is unchanged.
 * F2 - user files under lib/ft4/ were silently truncate-overwritten by the
 *      vendored FT4 zip, computing results against a substituted tree.
 * F4 - a moduleArgs integer past Long fell back to GtvString, producing a
 *      confusing Rell binding error for big_integer args.
 */
class AuditGtvAndFt4TreeRegressionTest {

    private val rid = BlockchainRid.buildFromHex(
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    )

    // 2^70: far past Long, definitely a GtvBigInteger on the wire.
    private val big = BigInteger.TWO.pow(70)

    // F1: a big_integer response value round-trips to a JSON string instead of erroring.
    @Test
    fun bigIntegerQueryResponseSerializesAsStringNotError() {
        val service = PostchainClientService(ChromiaConfig()) { _, _, _ ->
            GtvFactory.gtv(
                mapOf(
                    "amount" to GtvFactory.gtv(big),
                    "nested" to GtvFactory.gtv(mapOf("supply" to GtvFactory.gtv(big))),
                    "list" to GtvFactory.gtv(listOf(GtvFactory.gtv(big))),
                    "count" to GtvFactory.gtv(7L),
                    "name" to GtvFactory.gtv("CHR"),
                    "id" to GtvFactory.gtv(byteArrayOf(0xAB.toByte(), 0xCD.toByte())),
                    "missing" to GtvNull
                )
            )
        }
        val result = service.executeBlockchainQuery("testnet", rid, "ft4.get_asset_balance", emptyMap())
        assertTrue(result is NetworkResult.Success, result.toString())
        val obj = (result as NetworkResult.Success<JsonObject>).data
        val amount = obj.getValue("amount").jsonPrimitive
        assertTrue(amount.isString, "big_integer must serialize as a JSON string: $amount")
        assertEquals(big.toString(), amount.content)
        // Nested big_integer inside a dict and an array works too.
        assertEquals(big.toString(), obj.getValue("nested").jsonObject.getValue("supply").jsonPrimitive.content)
        assertEquals(big.toString(), obj.getValue("list").jsonArray[0].jsonPrimitive.content)
        // The strict gson differs ONLY in the big_integer branch - Long stays a
        // number, string stays a string, byte_array stays hex, null stays null.
        val count = obj.getValue("count").jsonPrimitive
        assertFalse(count.isString, "integer must stay a JSON number: $count")
        assertEquals("7", count.content)
        assertEquals("CHR", obj.getValue("name").jsonPrimitive.content)
        assertEquals("ABCD", obj.getValue("id").jsonPrimitive.content.uppercase())
        assertTrue(obj.getValue("missing") is JsonNull)
    }

    // F2: rell_check compiles the user's OWN lib/ft4 tree - a deliberate marker
    // error in it must surface (proving the vendored zip did not clobber it),
    // and the result must say the submitted tree was used.
    @Test
    fun rellCheckUsesSubmittedFt4TreeInsteadOfVendored() {
        val result = RellCheck.check(
            mapOf(
                "main.rell" to "module;\nimport lib.ft4.assets;\n",
                "lib/ft4/assets/module.rell" to "module;\nfunction marker() { zzz_user_ft4_marker(); }\n"
            ),
            null
        )
        assertFalse(result.ok, "the user's lib/ft4 marker error must surface: ${result.notes}")
        assertTrue(
            result.errors.any { it.text.contains("zzz_user_ft4_marker") },
            "expected the marker error from the submitted lib/ft4 file, got: ${result.errors}"
        )
        assertTrue(
            result.notes.contains("submitted lib/ft4"),
            "notes must say the submitted tree was used: ${result.notes}"
        )
    }

    // F2: run_rell_tests likewise - a test passes only because it calls a
    // function that exists solely in the USER's lib/ft4 tree.
    @Test
    fun runRellTestsUsesSubmittedFt4TreeInsteadOfVendored() {
        val result = RunRellTests.run(
            files = mapOf(
                "lib/ft4/assets/module.rell" to "module;\nfunction marker_value(): integer = 42;\n",
                "tests/asset_test.rell" to
                    "@test module;\nimport lib.ft4.assets;\nfunction test_marker() { assert_equals(assets.marker_value(), 42); }\n"
            ),
            databaseUrl = null
        )
        assertTrue(result.ok, "test against the submitted lib/ft4 must pass: ${result.notes} ${result.cases}")
        assertEquals(1, result.passed)
        assertTrue(
            result.notes.contains("submitted lib/ft4"),
            "notes must say the submitted tree was used: ${result.notes}"
        )
    }

    // F2: the src/ prefix is normalized away first, so an agent submitting its
    // whole project (src/lib/ft4/...) gets the same skip-provisioning behavior.
    @Test
    fun rellCheckNormalizesSrcPrefixBeforeFt4TreeDetection() {
        val result = RellCheck.check(
            mapOf(
                "src/main.rell" to "module;\nimport lib.ft4.assets;\n",
                "src/lib/ft4/assets/module.rell" to "module;\nfunction marker() { zzz_user_ft4_marker(); }\n"
            ),
            null
        )
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.text.contains("zzz_user_ft4_marker") }, result.errors.toString())
        assertTrue(result.notes.contains("submitted lib/ft4"), result.notes)
    }

    // F4: a moduleArgs integer past Long becomes GtvBigInteger, not GtvString.
    @Test
    fun jsonToGtvParsesPastLongIntegersAsBigInteger() {
        val gtv = RunRellTests.jsonToGtv(JsonPrimitive(big))
        assertTrue(gtv is GtvBigInteger, "expected GtvBigInteger, got ${gtv::class.simpleName}")
        assertEquals(big, gtv.asBigInteger())
        // In-range integers keep the existing GtvInteger path.
        val small = RunRellTests.jsonToGtv(JsonPrimitive(42))
        assertTrue(small is GtvInteger, "expected GtvInteger, got ${small::class.simpleName}")
        assertEquals(42L, small.asInteger())
        // Decimals keep the existing fallback behavior.
        assertEquals("2.5", RunRellTests.jsonToGtv(JsonPrimitive(2.5)).asString())
    }
}
