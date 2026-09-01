package org.chromia

import org.chromia.tools.CheckDappProject
import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.ErrorTranslator
import org.chromia.tools.Ft4ImportCheck
import org.chromia.tools.RellCheck
import org.chromia.tools.RellLibs
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for real-world discover round 2 (7 genuine Chromia projects):
 *
 * D1 - the vendored FT4 zip lacked its lib/iccf sibling, so ANY dapp using
 *      FT4's standard test helpers or crosschain modules failed with
 *      "Module 'lib.iccf' not found" (lib.ft4.test.core ->
 *      lib.ft4.admin.crosschain -> lib.ft4.external.crosschain -> ^^^.iccf).
 * D2 - the default all-modules compile raised mount conflicts `chr build`
 *      never sees for multi-chain repos (vector-db-extension, zkp-extension):
 *      sibling modules that are separate chains both mount the same name.
 * D3 - validate_chromia_yml failed official Chromia configs over missing
 *      production pins, flagged admin moduleArgs KEYS, and double-reported
 *      the missing merkle key.
 * D4 - the forbidden-module scan flagged @test modules (crc2-lib) for
 *      exercising registration strategies - exactly what test modules do.
 * D5 - the vendored-library scanning exemption was FT4-only; chromunity's
 *      vendored lib/ft3 produced findings inside library code.
 * D6 - translate_error gaps: lib.* module-not-found got path-vs-import
 *      advice, and this server's own validator messages had no rules.
 */
class RealWorldRound2RegressionTest {

    // ------------------------------------------------------------------ D1 --

    @Test
    fun vendoredZipShipsIccfNextToFt4() {
        val files = RellLibs.vendoredFt4Files()
        assertTrue(files.containsKey("lib/iccf/module.rell"), "lib/iccf must be vendored")
        assertTrue(files.containsKey("lib/iccf_test/module.rell"), "lib/iccf_test must be vendored")
        val roots = files.keys.map { it.removePrefix("lib/").substringBefore('/') }.toSet()
        assertEquals(RellLibs.VENDORED_LIB_ROOTS, roots, "zip roots must match VENDORED_LIB_ROOTS")
    }

    // The round-2 repro verbatim: FT4's standard test helpers pull in the
    // crosschain closure, which imports lib.iccf.
    @Test
    fun ft4TestHelpersRunGreenWithVendoredIccf() {
        val result = RunRellTests.run(
            files = mapOf(
                "my_test.rell" to """
                    @test module;
                    import lib.ft4.test.core.{ ft_auth_operation_for };
                    function test_trivial() { assert_equals(1, 1); }
                """.trimIndent()
            ),
            databaseUrl = null
        )
        assertTrue(result.ok, "FT4 test helpers must compile and run: ${result.notes} ${result.cases}")
        assertEquals(1, result.total)
        assertEquals(1, result.passed)
    }

    @Test
    fun rellCheckResolvesFt4CrosschainAndDirectIccfImports() {
        val crosschain = RellCheck.check(
            mapOf("main.rell" to "module;\nimport lib.ft4.external.crosschain;\n"),
            null
        )
        assertTrue(crosschain.ok, crosschain.errors.toString())
        val direct = RellCheck.check(
            mapOf("main.rell" to "module;\nimport lib.iccf;\nquery q() = 1;\n"),
            null
        )
        assertTrue(direct.ok, direct.errors.toString())
    }

    // A submitted lib/iccf file identical to the vendored copy is exempt from
    // scanning; a differing one gets the differs-note treatment like lib/ft4.
    @Test
    fun submittedLibIccfGetsHashGateTreatment() {
        val vendored = RellLibs.vendoredFt4Files().getValue("lib/iccf/module.rell")
        val clean = Ft4ImportCheck.scanFiles(
            mapOf("src/lib/iccf/module.rell" to vendored, "src/main.rell" to "module;\n")
        )
        assertTrue(clean.ok, clean.errors.toString())
        assertEquals(1, clean.exemptedLibFiles)

        val forked = Ft4ImportCheck.scanFiles(
            mapOf(
                "src/lib/iccf/module.rell" to vendored + "\nimport lib.ft4.admin;\n",
                "src/main.rell" to "module;\n"
            )
        )
        assertFalse(forked.ok, "a forked lib/iccf file must be scanned: ${forked.errors}")
        assertTrue(
            forked.warnings.any { it.contains("lib/iccf/module.rell differs from vendored FT4") },
            "the differs-note must explain why the lib file was scanned: ${forked.warnings}"
        )
    }

    // ------------------------------------------------------------------ D2 --

    private val chainA = "module;\n@mount('receive_message') operation msg_a(topic: text) {}\n"
    private val chainB = "module;\n@mount('receive_message') operation msg_b(topic: text) {}\n"

    @Test
    fun siblingChainMountConflictExplainsPerChainAlternatives() {
        val result = RellCheck.check(
            mapOf("chain_a/module.rell" to chainA, "chain_b/module.rell" to chainB),
            null
        )
        assertFalse(result.ok, "the conflict itself is real under an all-modules compile")
        assertTrue(
            result.notes.contains("per-chain alternatives"),
            "mount conflicts between independent sibling modules must explain the modules argument: ${result.notes}"
        )
        assertTrue(result.notes.contains("chain_a") && result.notes.contains("chain_b"), result.notes)
    }

    @Test
    fun explicitModulesArgumentCompilesOneChainGreen() {
        val result = RellCheck.check(
            mapOf("chain_a/module.rell" to chainA, "chain_b/module.rell" to chainB),
            listOf("chain_a")
        )
        assertTrue(result.ok, result.errors.toString())
        assertFalse(result.notes.contains("per-chain alternatives"), result.notes)
    }

    // Modules that DO import each other are one app - a mount conflict there is
    // a genuine bug and must not get the per-chain note.
    @Test
    fun mountConflictWithinImportChainGetsNoPerChainNote() {
        val result = RellCheck.check(
            mapOf(
                "chain_a/module.rell" to "module;\nimport chain_b;\n@mount('receive_message') operation msg_a(topic: text) {}\n",
                "chain_b/module.rell" to chainB
            ),
            null
        )
        assertFalse(result.ok)
        assertFalse(
            result.notes.contains("per-chain alternatives"),
            "importing modules are one app; no per-chain note: ${result.notes}"
        )
    }

    private val multiChainYaml = """
        blockchains:
          chain_a:
            module: chain_a
            config:
              features:
                merkle_hash_version: 2
          chain_b:
            module: chain_b
            config:
              features:
                merkle_hash_version: 2
        compile:
          rellVersion: 0.16.1
    """.trimIndent()

    @Test
    fun checkDappProjectCompilesPerBlockchain() {
        val result = CheckDappProject.check(
            multiChainYaml,
            mapOf("chain_a/module.rell" to chainA, "chain_b/module.rell" to chainB)
        )
        assertTrue(result.ok, "per-chain compile must not see the sibling mount conflict: ${result.errors}")
        assertTrue(
            result.notes.any { it.contains("compiled per blockchain") },
            "the per-blockchain compile must be noted: ${result.notes}"
        )
    }

    @Test
    fun perBlockchainModulesFallsBackOnPartialSubmission() {
        // chain_b's module is not among the submitted files - keep the old
        // single all-modules compile instead of failing each chain.
        val map = CheckDappProject.perBlockchainModules(
            multiChainYaml,
            mapOf("chain_a/module.rell" to chainA)
        )
        assertTrue(map.isEmpty(), map.toString())
        // Single-chain projects keep the old behavior too.
        val single = CheckDappProject.perBlockchainModules(
            "blockchains:\n  hello:\n    module: main\n",
            mapOf("main.rell" to "module;\n")
        )
        assertTrue(single.isEmpty(), single.toString())
    }

    // ------------------------------------------------------------------ D3 --

    /** Official-style config: no compile.rellVersion, no merkle_hash_version. */
    private val officialStyleYaml = """
        blockchains:
          my_chain:
            module: main
    """.trimIndent()

    @Test
    fun officialConfigWithoutPinsIsOkWithWarnings() {
        val result = ChromiaYmlValidator.validate(officialStyleYaml)
        assertTrue(result.ok, "chr builds official configs without pins: ${result.errors}")
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(
            result.warnings.any { it.contains("compile.rellVersion") },
            result.warnings.toString()
        )
        assertTrue(
            result.warnings.any { it.contains("merkle_hash_version") },
            result.warnings.toString()
        )
    }

    @Test
    fun strictModeRestoresMissingPinErrors() {
        val result = ChromiaYmlValidator.validate(officialStyleYaml, strict = true)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("compile.rellVersion") }, result.errors.toString())
        assertTrue(result.errors.any { it.contains("merkle_hash_version") }, result.errors.toString())
    }

    @Test
    fun missingMerkleIsReportedExactlyOnce() {
        val result = ChromiaYmlValidator.validate(officialStyleYaml)
        val mentions = (result.errors + result.warnings).count { it.contains("merkle_hash_version") }
        assertEquals(1, mentions, "no global+per-chain double report: ${result.warnings} ${result.errors}")
    }

    @Test
    fun perChainMerkleWarningRemainsWhenAnotherChainSetsIt() {
        val yaml = """
            blockchains:
              chain_a:
                module: main
                config:
                  features:
                    merkle_hash_version: 2
              chain_b:
                module: other
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(
            result.warnings.any { it.contains("blockchains.chain_b.config.features.merkle_hash_version") },
            result.warnings.toString()
        )
    }

    @Test
    fun adminModuleArgsKeyIsLegitimateConfiguration() {
        val yaml = """
            blockchains:
              my_chain:
                module: main
                config:
                  features:
                    merkle_hash_version: 2
                moduleArgs:
                  lib.ft4.core.admin:
                    admin_pubkey: x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
                  lib.ft4.admin:
                    admin_pubkey: x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(
            result.errors.none { it.contains("moduleArgs") },
            "configuring admin_pubkey via moduleArgs is documented practice: ${result.errors}"
        )
        assertTrue(result.ok, result.errors.toString())
    }

    @Test
    fun openStrategyModuleArgsKeyAndAdminLibsPathStayFlagged() {
        val openKey = ChromiaYmlValidator.validate(
            """
            blockchains:
              my_chain:
                module: main
                config:
                  features:
                    merkle_hash_version: 2
                moduleArgs:
                  ras_open:
                    a: 1
            compile:
              rellVersion: 0.16.1
            """.trimIndent()
        )
        assertTrue(openKey.errors.any { it.contains("ras_open") }, openKey.errors.toString())

        // Pulling admin CODE via libs is still an error - only moduleArgs KEYS changed.
        val adminLib = ChromiaYmlValidator.validate(
            """
            blockchains:
              my_chain:
                module: main
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
            libs:
              bad:
                registry: https://gitlab.com/chromaway/ft4-lib.git
                path: rell/src/lib/ft4/admin
                tagOrBranch: v1.1.0r
            """.trimIndent()
        )
        assertTrue(
            adminLib.errors.any { it.contains("forbidden FT4 production module") },
            adminLib.errors.toString()
        )
    }

    @Test
    fun codeImportOfAdminIsStillFlagged() {
        val result = Ft4ImportCheck.scan("module;\nimport lib.ft4.core.admin;\n")
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("lib.ft4.core.admin") }, result.errors.toString())
    }

    // ------------------------------------------------------------------ D4 --

    private val strategyTest = """
        @test module;
        import lib.ft4.accounts.strategies.open;
        function test_registration() { assert_equals(1, 1); }
    """.trimIndent()

    @Test
    fun testModulesAreExemptFromForbiddenModuleScan() {
        val scan = Ft4ImportCheck.scanFiles(mapOf("tests/strategy_test.rell" to strategyTest))
        assertTrue(scan.ok, "test modules legitimately exercise strategies: ${scan.errors}")
        assertEquals(1, scan.exemptedTestModules)

        val sec = RellSecurityCheck.analyze(mapOf("tests/strategy_test.rell" to strategyTest))
        assertTrue(
            sec.findings.none { it.rule == "banned-module" || it.rule == "open-registration-strategy" },
            sec.findings.toString()
        )
        assertTrue(sec.notes.contains("@test module"), sec.notes)

        val project = CheckDappProject.check(
            "blockchains:\n  hello:\n    module: main\n",
            mapOf("main.rell" to "module;\n", "tests/strategy_test.rell" to strategyTest),
            compile = false
        )
        assertTrue(
            project.errors.none { it.contains("forbidden FT4 production module") },
            project.errors.toString()
        )
    }

    @Test
    fun nonTestModulesAreStillScannedForForbiddenModules() {
        val scan = Ft4ImportCheck.scanFiles(
            mapOf("main.rell" to "module;\nimport lib.ft4.accounts.strategies.open;\n")
        )
        assertFalse(scan.ok, "production code importing the open strategy must stay flagged")

        // The security scan's banned list covers admin modules and ras_open /
        // ras_transfer_open; both rule families must still fire on NON-test modules.
        val sec = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nimport lib.ft4.admin;\noperation r() { ras_open(); }\n")
        )
        assertTrue(
            sec.findings.any { it.severity == "CRITICAL" && it.rule == "banned-module" },
            sec.findings.toString()
        )
        assertTrue(
            sec.findings.any { it.severity == "CRITICAL" && it.rule == "open-registration-strategy" },
            sec.findings.toString()
        )
    }

    // A masked "@test module" inside a comment must not earn the exemption.
    @Test
    fun commentedTestHeaderDoesNotExemptForbiddenScan() {
        val fake = "// @test module\nmodule;\nimport lib.ft4.admin;\n"
        val scan = Ft4ImportCheck.scanFiles(mapOf("main.rell" to fake))
        assertFalse(scan.ok, scan.errors.toString())
    }

    // ------------------------------------------------------------------ D5 --

    private val ft3Style = """
        module;
        entity user { mutable name: text; }
        operation update_user(name: text) { update user @* {} ( name ); }
    """.trimIndent()

    @Test
    fun thirdPartyLibFilesAreSkippedWithNote() {
        val sec = RellSecurityCheck.analyze(
            mapOf("src/lib/ft3/core/account.rell" to ft3Style, "src/main.rell" to "module;\n")
        )
        assertTrue(sec.ok, "findings inside third-party lib code are noise: ${sec.findings}")
        assertTrue(sec.findings.isEmpty(), sec.findings.toString())
        assertTrue(sec.notes.contains("third-party library code"), sec.notes)

        val scan = Ft4ImportCheck.scanFiles(
            mapOf("src/lib/ft3/core/account.rell" to "module;\nimport lib.ft4.admin;\n")
        )
        assertTrue(scan.ok, scan.errors.toString())
        assertEquals(1, scan.skippedThirdPartyLibFiles)

        val project = CheckDappProject.check(
            "blockchains:\n  hello:\n    module: main\n",
            mapOf("src/lib/ft3/core/account.rell" to ft3Style, "src/main.rell" to "module;\n"),
            compile = false
        )
        assertTrue(project.errors.isEmpty(), project.errors.toString())
        assertTrue(project.notes.any { it.contains("third-party library code") }, project.notes.toString())
    }

    @Test
    fun sameCodeOutsideLibIsStillScanned() {
        val sec = RellSecurityCheck.analyze(mapOf("src/vendor/account.rell" to ft3Style))
        assertTrue(
            sec.findings.any { it.rule == "unauthenticated-mutation" },
            "only lib/** is exempt; other paths stay scanned: ${sec.findings}"
        )
    }

    // The lib/ft4 hash gate keeps its semantics: identical exempt, modified scanned.
    @Test
    fun libFt4HashGateSemanticsUnchanged() {
        val vendored = RellLibs.vendoredFt4Files().getValue("lib/ft4/version.rell")
        val identical = RellSecurityCheck.analyze(
            mapOf("src/lib/ft4/version.rell" to vendored, "src/main.rell" to "module;\n")
        )
        assertTrue(identical.ok, identical.findings.toString())
        assertTrue(identical.notes.contains("vendored-library"), identical.notes)

        val modified = RellSecurityCheck.analyze(
            mapOf(
                "src/lib/ft4/version.rell" to vendored +
                    "\nentity planted { name; }\noperation plant(dest: text) { create planted(name = dest); }\n",
                "src/main.rell" to "module;\n"
            )
        )
        assertFalse(modified.ok, modified.notes)
        assertTrue(modified.notes.contains("differs from vendored FT4"), modified.notes)
    }

    // ------------------------------------------------------------------ D6 --

    @Test
    fun libModuleNotFoundGetsDependencyRule() {
        val t = ErrorTranslator.translate(
            "lib/ft4/external/crosschain/module.rell(10:1) ERROR: Module 'lib.iccf' not found"
        )
        assertTrue(t.matched)
        assertEquals("rell_lib_module_not_found", t.ruleId)
        assertTrue(t.likelyCause.contains("chromia.yml"), t.likelyCause)
        assertTrue(t.nextAction.contains("chr install"), t.nextAction)
    }

    @Test
    fun ft4ModuleNotFoundMentionsVersionMismatch() {
        val t = ErrorTranslator.translate("main.rell(2:8) ERROR: Module 'lib.ft4.test.utils' not found")
        assertEquals("rell_lib_module_not_found", t.ruleId)
        assertTrue(
            t.likelyCause.contains("FT4") || t.nextAction.contains("version mismatch"),
            t.likelyCause + " / " + t.nextAction
        )
    }

    @Test
    fun ownModuleNotFoundKeepsPathAdviceRule() {
        val t = ErrorTranslator.translate("main.rell(1:8) ERROR: Module 'my_app.utils' not found")
        assertEquals("rell_unknown_module", t.ruleId)
    }

    @Test
    fun forbiddenModuleValidatorMessageIsTranslated() {
        val t = ErrorTranslator.translate(
            "main.rell:2: forbidden FT4 production module lib.ft4.admin"
        )
        assertTrue(t.matched)
        assertEquals("own_forbidden_module", t.ruleId)
        assertTrue(t.nextAction.contains("allowAdminModules"), t.nextAction)
    }

    @Test
    fun securityFindingMessagesAreTranslated() {
        val mutation = ErrorTranslator.translate(
            "main.rell:5: [HIGH] unauthenticated-mutation - operation set_name mutates state without an auth check"
        )
        assertEquals("own_unauthenticated_mutation", mutation.ruleId)

        val secret = ErrorTranslator.translate(
            "main.rell:9: [HIGH] hardcoded-key-material - val k = x\"...\""
        )
        assertEquals("own_hardcoded_key_material", secret.ruleId)
    }
}
