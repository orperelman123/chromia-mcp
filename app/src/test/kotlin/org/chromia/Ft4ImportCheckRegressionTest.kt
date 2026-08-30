package org.chromia

import org.chromia.tools.CheckDappProject
import org.chromia.tools.DappScaffold
import org.chromia.tools.Ft4ImportCheck
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for audited Ft4ImportCheck / CheckDappProject bugs:
 * dotted-path prefix matching, single-quoted string handling, warning forwarding.
 */
class Ft4ImportCheckRegressionTest {

    // B1: lib.ft4.admin must match itself and any dotted sub-path.
    @Test
    fun adminImportsAreFlaggedIncludingDottedSubPaths() {
        assertFalse(Ft4ImportCheck.scan("module;\nimport lib.ft4.admin;\n").ok)
        assertFalse(Ft4ImportCheck.scan("module;\nimport lib.ft4.admin.crosschain;\n").ok)
        assertFalse(Ft4ImportCheck.scan("module;\nimport admin.crosschain;\n").ok)
        val nested = Ft4ImportCheck.scan("module;\nimport lib.ft4.admin.crosschain;\n")
        assertTrue(nested.errors.any { it.contains("lib.ft4.admin") }, nested.errors.toString())
    }

    // B1: the ALLOWED lib.ft4.crosschain import must not be flagged.
    @Test
    fun allowedCrosschainImportIsNotFlagged() {
        val result = Ft4ImportCheck.scan("module;\nimport lib.ft4.crosschain;\n")
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.errors.isEmpty(), result.errors.toString())
    }

    // B1: ras_open needs word boundaries; extras_open_config is not a hit.
    @Test
    fun rasOpenMatchesOnWordBoundariesOnly() {
        assertTrue(Ft4ImportCheck.scan("module;\nval x = extras_open_config();\n").ok)
        assertFalse(Ft4ImportCheck.scan("module;\nimport lib.ft4.core.accounts.strategies: ras_open;\n").ok)
        assertTrue(Ft4ImportCheck.containsModule("use ras_open here", "ras_open"))
        assertFalse(Ft4ImportCheck.containsModule("use extras_open_config here", "ras_open"))
    }

    // B2: '//' inside a single-quoted string is not a comment; the import after it counts.
    @Test
    fun singleQuotedStringDoesNotHideImports() {
        val rell = "module;\nval u = 'https://x'; import lib.ft4.admin;\n"
        val result = Ft4ImportCheck.scan(rell)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("lib.ft4.admin") }, result.errors.toString())
        val stripped = Ft4ImportCheck.stripComments(rell)
        assertTrue(stripped.contains("import lib.ft4.admin"), stripped)
    }

    // C: CheckDappProject forwards Ft4ImportCheck warnings, not only errors.
    @Test
    fun checkDappProjectForwardsFt4Warnings() {
        val yaml = DappScaffold.files("hello").getValue("chromia.yml")
        val rell = "module;\nimport lib.ft4.cross-chain;\n"
        val direct = Ft4ImportCheck.scan(rell)
        assertTrue(direct.warnings.isNotEmpty(), "expected a cross-chain warning")
        val result = CheckDappProject.check(yaml, mapOf("src/main.rell" to rell))
        assertTrue(
            result.warnings.any { it.startsWith("src/main.rell:") && it.contains("cross-chain") },
            result.warnings.toString()
        )
    }
}
