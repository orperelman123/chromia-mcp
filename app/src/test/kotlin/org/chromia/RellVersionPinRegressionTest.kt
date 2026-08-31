package org.chromia

import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.DappScaffold
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the Rell version split (source tag vs chromia.yml pin).
 *
 * Chromia CLI 0.33.x bundles Rell 0.16.1 whose SUPPORTED_VERSIONS list stops at
 * 0.16.1; a generated chromia.yml pinned to the 0.16.7 source tag fails
 * `chr build` with an "Unknown version" error. The scaffolder must therefore
 * write 0.16.1 into chromia.yml, and the validator must ERROR (not warn) on any
 * rellVersion newer than 0.16.1.
 */
class RellVersionPinRegressionTest {

    // ---- Constants encode the split ----

    @Test
    fun rellVersionConstantsEncodeSourceTagVsYmlPinSplit() {
        assertEquals("0.16.1", DappScaffold.RELL_VERSION, "chromia.yml pin must match the CLI-bundled Rell")
        assertEquals("0.16.7", DappScaffold.RELL_SOURCE_TAG, "source tag stays at the Rell git tag")
        assertEquals(DappScaffold.RELL_VERSION, ChromiaYmlValidator.RELL_VERSION)
    }

    // ---- Scaffold output ----

    @Test
    fun scaffoldedHelloChromiaYmlPinsRell0161() {
        val yml = DappScaffold.files("demo").getValue("chromia.yml")
        assertTrue(yml.contains("rellVersion: 0.16.1"), yml)
        assertFalse(yml.contains("0.16.7"), "scaffolded chromia.yml must never carry the source tag: $yml")
        val validated = ChromiaYmlValidator.validate(yml)
        assertTrue(validated.ok, validated.errors.toString())
    }

    @Test
    fun scaffoldedFt4ChromiaYmlPinsRell0161() {
        val yml = DappScaffold.files("demo", template = "ft4").getValue("chromia.yml")
        assertTrue(yml.contains("rellVersion: 0.16.1"), yml)
        assertFalse(yml.contains("0.16.7"), "scaffolded FT4 chromia.yml must never carry the source tag: $yml")
        val validated = ChromiaYmlValidator.validate(yml)
        assertTrue(validated.ok, validated.errors.toString())
    }

    // ---- compareRellVersions ----

    @Test
    fun compareRellVersionsEqual() {
        assertEquals(0, ChromiaYmlValidator.compareRellVersions("0.16.1", "0.16.1"))
        assertEquals(0, ChromiaYmlValidator.compareRellVersions("1.2.3", "1.2.3"))
    }

    @Test
    fun compareRellVersionsGreaterAndLess() {
        assertTrue(ChromiaYmlValidator.compareRellVersions("0.16.7", "0.16.1")!! > 0)
        assertTrue(ChromiaYmlValidator.compareRellVersions("0.16.1", "0.16.7")!! < 0)
        assertTrue(ChromiaYmlValidator.compareRellVersions("0.17.0", "0.16.9")!! > 0)
        assertTrue(ChromiaYmlValidator.compareRellVersions("1.0.0", "0.99.99")!! > 0)
    }

    @Test
    fun compareRellVersionsIsNumericNotLexicographic() {
        // String comparison would call "0.16.10" < "0.16.9"; numeric must not.
        assertTrue(ChromiaYmlValidator.compareRellVersions("0.16.10", "0.16.9")!! > 0)
        assertTrue(ChromiaYmlValidator.compareRellVersions("0.16.9", "0.16.10")!! < 0)
    }

    @Test
    fun compareRellVersionsDifferingComponentCounts() {
        // Missing components count as zero.
        assertEquals(0, ChromiaYmlValidator.compareRellVersions("1.0", "1.0.0"))
        assertEquals(0, ChromiaYmlValidator.compareRellVersions("1.0.0.0", "1.0"))
        assertTrue(ChromiaYmlValidator.compareRellVersions("1.0.1", "1.0")!! > 0)
        assertTrue(ChromiaYmlValidator.compareRellVersions("1", "1.0.1")!! < 0)
    }

    @Test
    fun compareRellVersionsMalformedInputReturnsNullNotCrash() {
        assertNull(ChromiaYmlValidator.compareRellVersions("latest", "0.16.1"))
        assertNull(ChromiaYmlValidator.compareRellVersions("0.16.1", "latest"))
        assertNull(ChromiaYmlValidator.compareRellVersions("0.16.x", "0.16.1"))
        assertNull(ChromiaYmlValidator.compareRellVersions("", "0.16.1"))
        assertNull(ChromiaYmlValidator.compareRellVersions("0..1", "0.16.1"))
        assertNull(ChromiaYmlValidator.compareRellVersions("0.-1.2", "0.16.1"))
        // Component overflow must degrade to "unparseable", never throw.
        assertNull(ChromiaYmlValidator.compareRellVersions("0.99999999999999999999.0", "0.16.1"))
    }

    // ---- Validator behavior ----

    private fun ymlWithRellVersion(version: String): String = """
        blockchains:
          hello:
            module: main
            config:
              features:
                merkle_hash_version: 2
        compile:
          rellVersion: $version
    """.trimIndent()

    @Test
    fun validatorErrorsOnRellVersionNewerThanCliBundle() {
        val result = ChromiaYmlValidator.validate(ymlWithRellVersion("0.16.7"))
        assertFalse(result.ok, "0.16.7 must be rejected: ${result.warnings}")
        assertTrue(
            result.errors.any { it.contains("0.16.7") && it.contains("0.16.1") },
            result.errors.toString()
        )
        assertTrue(
            result.errors.any { it.contains("chr build") && it.contains("Unknown version") },
            "error must tell the agent the installed CLI will reject it: ${result.errors}"
        )
    }

    @Test
    fun validatorErrorsOnDoubleDigitPatchNewerThanCliBundle() {
        // Numeric comparison: 0.16.10 > 0.16.1 even though it is lexicographically smaller than 0.16.2.
        val result = ChromiaYmlValidator.validate(ymlWithRellVersion("0.16.10"))
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("0.16.10") }, result.errors.toString())
    }

    @Test
    fun validatorPassesOnRellVersion0161() {
        val result = ChromiaYmlValidator.validate(ymlWithRellVersion("0.16.1"))
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(
            result.warnings.none { it.contains("rellVersion") },
            result.warnings.toString()
        )
    }

    @Test
    fun validatorOnlyWarnsOnOlderRellVersion() {
        val result = ChromiaYmlValidator.validate(ymlWithRellVersion("0.14.9"))
        assertTrue(result.ok, result.errors.toString())
        assertTrue(
            result.warnings.any { it.contains("0.14.9") && it.contains("0.16.1") },
            result.warnings.toString()
        )
    }
}
