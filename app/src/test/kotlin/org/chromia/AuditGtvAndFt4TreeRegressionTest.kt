package org.chromia

import java.math.BigInteger
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvBigInteger
import net.postchain.gtv.GtvInteger
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

    // 2^70: far past Long. Still used by the F4 moduleArgs test below, which is
    // about a value an AGENT supplies, not one a chain returns - so it is input
    // data, not a stand-in for anything.
    private val big = BigInteger.TWO.pow(70)

    /**
     * F1, ON THE REAL CHAIN.
     *
     * The bug was that `make_gtv_gson()`'s BIGINTEGER branch throws, so every
     * FT4 balance / total_supply / amount query reported an error although the
     * chain had succeeded. The regression test used to supply the big_integer
     * itself, through a trailing-lambda query client - which meant the thing it
     * proved was that `makeStrictGtvGson` serializes a Gtv the TEST built.
     *
     * `get_chr_asset` on the live Economy Chain returns a real `big_integer`
     * supply next to a real `integer`, real `text`s and a real `byte_array`, so
     * the whole discrimination the fix rests on - big_integer becomes a JSON
     * string, everything else is untouched - is asserted against GTV that came
     * off the wire. If FT4 ever stops returning big_integer here, this goes red
     * for the right reason instead of staying green against a memory of 2026.
     */
    @Test
    fun liveBigIntegerResponseSerializesAsStringAndNothingElseChanges() {
        LiveChromia.requireLive("reads a real big_integer supply off the live Economy Chain")
        val result = LiveChromia.postchain().executeBlockchainQuery(
            LiveChromia.NETWORK, LiveChromia.economyChainRid, "get_chr_asset", emptyMap()
        )
        assertTrue(result is NetworkResult.Success, "live get_chr_asset failed: $result")
        val obj = (result as NetworkResult.Success<JsonObject>).data

        // big_integer -> JSON string (the fix). Rell's total_supply is a
        // big_integer; a plain gson would have thrown before producing this.
        val supply = obj.getValue("supply").jsonPrimitive
        assertTrue(supply.isString, "big_integer must serialize as a JSON string: $supply")
        assertTrue(
            supply.content.matches(Regex("""[0-9]+""")) && BigInteger(supply.content) > BigInteger.ZERO,
            "supply must be a positive integer in a string: ${supply.content}"
        )

        // integer stays a JSON number - the strict gson differs ONLY in the
        // big_integer branch, and this is the assertion that pins that.
        val decimals = obj.getValue("decimals").jsonPrimitive
        assertFalse(decimals.isString, "integer must stay a JSON number: $decimals")
        assertTrue(decimals.content.toInt() >= 0, decimals.content)

        // text stays text; byte_array stays hex.
        assertTrue(obj.getValue("name").jsonPrimitive.isString)
        val id = obj.getValue("id").jsonPrimitive.content
        assertEquals(64, id.length, "an FT4 asset id is 32 bytes of hex: $id")
        assertTrue(id.uppercase().matches(Regex("""[0-9A-F]+""")), id)
    }

    /**
     * The same discrimination one level down: a big_integer nested inside an
     * array of dictionaries, plus a real `null` cursor. Nested and null are the
     * two shapes the fixture used to fabricate.
     */
    @Test
    fun liveNestedBigIntegerAndNullSurviveTheStrictGson() {
        LiveChromia.requireLive("reads a nested big_integer and a null cursor off the live Economy Chain")
        val result = LiveChromia.postchain().executeBlockchainQuery(
            LiveChromia.NETWORK,
            LiveChromia.economyChainRid,
            "ft4.get_assets_filtered",
            mapOf(
                "asset_filter" to mapOf("ids" to null, "name" to null, "symbol" to null, "type" to null),
                "page_size" to 1,
                "page_cursor" to null
            )
        )
        assertTrue(result is NetworkResult.Success, "live filtered query failed: $result")
        val page = (result as NetworkResult.Success<JsonObject>).data
        val rows = page.getValue("data").jsonArray
        assertTrue(rows.isNotEmpty(), "the Economy Chain has at least one asset: $page")
        val nestedSupply = rows[0].jsonObject.getValue("supply").jsonPrimitive
        assertTrue(nestedSupply.isString, "a big_integer inside an array of dicts must be a string too: $page")
        assertTrue(BigInteger(nestedSupply.content) >= BigInteger.ZERO)
        assertTrue(page.getValue("next_cursor") is JsonNull, "a real null must survive as JsonNull: $page")
    }

    /**
     * A real `null` and a real nested ARRAY, from the auth-descriptor of a
     * public, known-registered account. `rules` is null on a main descriptor and
     * `args` is a nested array whose first element is itself an array of flags.
     */
    @Test
    fun liveNullAndNestedArrayFromARealAuthDescriptor() {
        LiveChromia.requireLive("reads a real main auth descriptor - a null field and a nested array")
        val result = LiveChromia.postchain().executeBlockchainQuery(
            LiveChromia.NETWORK,
            LiveChromia.economyChainRid,
            "ft4.get_account_main_auth_descriptor",
            mapOf("account_id" to LiveChromia.KNOWN_REGISTERED_ACCOUNT_ID_HEX.hexStringToByteArray())
        )
        assertTrue(result is NetworkResult.Success, "live auth-descriptor query failed: $result")
        val descriptor = (result as NetworkResult.Success<JsonObject>).data
        assertTrue(
            descriptor.getValue("rules") is JsonNull,
            "a main auth descriptor has no rules; a real null must arrive as JsonNull: $descriptor"
        )
        val args = descriptor.getValue("args").jsonArray
        assertTrue(args.isNotEmpty(), "auth descriptor args must not be empty: $descriptor")
        val flags = args[0].jsonArray.map { it.jsonPrimitive.content }
        assertTrue(flags.contains("A"), "the account flags must include A (account): $flags")
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
