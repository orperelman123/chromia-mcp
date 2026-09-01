package org.chromia

import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.RellCheck
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.SimpleYaml
import org.chromia.tools.YamlNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests from the round-1 real-world sweep: production Chromaway
 * projects (filehub/filechain, dapp-aggregator, core/price-oracle, ft4 demo,
 * iccf-example) fed through the Rell tools. Each case is minimized from the
 * real file that failed; no proprietary code is copied beyond tiny excerpts.
 */
class RealWorldRound1RegressionTest {

    // ---------------------------------------------------------------- YAML --

    // filehub/filechain chromia.yml: anchor on a sequence item (`- &gtx`) with
    // a nested block, later merged via `<<: *gtx`. Used to fail with
    // "bad indent at line 3".
    @Test
    fun sequenceItemAnchorAndMergeKeyParse() {
        val yaml = """
            definitions:
              - &gtx
                gtx:
                  modules:
                    - "net.postchain.d1.iccf.IccfGTXModule"
            blockchains:
              filechain:
                module: main
                config:
                  <<: *gtx
                  id: "abc"
        """.trimIndent()
        val root = SimpleYaml.parse(yaml) as YamlNode.Mapping
        val config = root.mapping("blockchains")!!.mapping("filechain")!!.mapping("config")!!
        assertEquals("abc", config.scalar("id"))
        val gtx = config.mapping("gtx")!!
        val modules = gtx.entries["modules"] as YamlNode.Sequence
        assertEquals(YamlNode.Scalar("net.postchain.d1.iccf.IccfGTXModule"), modules.items[0])
    }

    // dapp-aggregator chromia.yml: anchor on a mapping value
    // (`moduleArgs: &dappMetadataModuleArgs` + nested block) and alias reuse
    // (`moduleArgs: *dappMetadataModuleArgs`). Used to fail with "bad indent".
    @Test
    fun mappingValueAnchorAndAliasReuseParse() {
        val yaml = """
            definitions:
              moduleArgs: &commonArgs
                core:
                  admin_pubkey: 02897FAC
                lib.ft4.accounts: &ft4Args
                  rate_limit:
                    max_points: 10
            blockchains:
              dapp_metadata:
                module: core
                moduleArgs: *commonArgs
            test:
              moduleArgs: *commonArgs
        """.trimIndent()
        val root = SimpleYaml.parse(yaml) as YamlNode.Mapping
        val args = root.mapping("blockchains")!!.mapping("dapp_metadata")!!.mapping("moduleArgs")!!
        assertEquals("02897FAC", args.mapping("core")!!.scalar("admin_pubkey"))
        assertEquals(
            "10",
            args.mapping("lib.ft4.accounts")!!.mapping("rate_limit")!!.scalar("max_points")
        )
        // The nested anchor must also be registered.
        assertEquals(
            "10",
            (root.mapping("test")!!.mapping("moduleArgs")!!
                .mapping("lib.ft4.accounts")!!.mapping("rate_limit")!!).scalar("max_points")
        )
    }

    // core/price-oracle chromia.yml: a sequence item that is a MULTI-KEY block
    // mapping (`- topic: x` continued by `bc-rid: y`). Used to fail with
    // "bad indent at line 61" on the second key.
    @Test
    fun multiKeyMappingInSequenceItemParses() {
        val yaml = """
            icmf:
              receiver:
                local:
                  - topic: L_evm_block_events
                    bc-rid: x"00ff"
        """.trimIndent()
        val root = SimpleYaml.parse(yaml) as YamlNode.Mapping
        val local = root.mapping("icmf")!!.mapping("receiver")!!.entries["local"] as YamlNode.Sequence
        val item = local.items[0] as YamlNode.Mapping
        assertEquals("L_evm_block_events", item.scalar("topic"))
        assertEquals("x\"00ff\"", item.scalar("bc-rid"))
    }

    // filehub chromia.yml: FT4 transfer rules - first key of the item carries a
    // nested sequence, then sibling keys continue the same item.
    @Test
    fun sequenceItemWithNestedBlockThenSiblingKeysParses() {
        val yaml = """
            rules:
              - sender_blockchain:
                  - x"15c0"
                sender: "*"
                recipient: "*"
              - sender: other
        """.trimIndent()
        val root = SimpleYaml.parse(yaml) as YamlNode.Mapping
        val rules = root.entries["rules"] as YamlNode.Sequence
        assertEquals(2, rules.items.size)
        val first = rules.items[0] as YamlNode.Mapping
        val senders = first.entries["sender_blockchain"] as YamlNode.Sequence
        assertEquals(YamlNode.Scalar("x\"15c0\""), senders.items[0])
        assertEquals("*", first.scalar("sender"))
        assertEquals("*", first.scalar("recipient"))
        assertEquals("other", (rules.items[1] as YamlNode.Mapping).scalar("sender"))
    }

    // An alias used before its anchor must be a clear error, not a crash.
    @Test
    fun unknownAliasReportsClearError() {
        val yaml = "config:\n  <<: *nope"
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(
            result.errors.any { it.contains("unknown alias *nope") },
            result.errors.toString()
        )
    }

    // Explicit keys override merge-key entries regardless of position (YAML spec).
    @Test
    fun explicitKeysOverrideMergedOnes() {
        val yaml = """
            base: &base
              a: 1
              b: 2
            child:
              <<: *base
              a: 9
        """.trimIndent()
        val root = SimpleYaml.parse(yaml) as YamlNode.Mapping
        val child = root.mapping("child")!!
        assertEquals("9", child.scalar("a"))
        assertEquals("2", child.scalar("b"))
    }

    // The full filechain-shaped yml validates end-to-end (was: hard parse error).
    @Test
    fun filechainShapedYmlValidates() {
        val yaml = """
            definitions:
              - &gtx
                gtx:
                  modules:
                    - "net.postchain.d1.iccf.IccfGTXModule"
            blockchains:
              filechain:
                module: main
                config:
                  <<: *gtx
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(result.ok)
    }

    // ------------------------------------------------------- compile scope --

    // price-oracle + lib.icmf: the library ships ALTERNATIVE receiver modules
    // that both mount `__icmf_message`; `chr build` compiles only what the app
    // imports, so submitting the vendored lib tree must not force-compile both
    // and false-red with a mount-name conflict.
    @Test
    fun alternativeLibModulesCompileOnlyWhenImported() {
        val files = mapOf(
            "src/main.rell" to """
                module;
                import lib.mylib.receiver;
            """.trimIndent(),
            "src/lib/mylib/receiver.rell" to """
                module;
                operation __my_message(topic: text) {}
            """.trimIndent(),
            "src/lib/mylib/metadata_receiver.rell" to """
                module;
                operation __my_message(topic: text) {}
            """.trimIndent()
        )
        val result = RellCheck.check(files, null)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.modules.contains("main"), result.modules.toString())
    }

    // ...but a lib module the app DOES import still fails when broken.
    @Test
    fun importedLibModuleErrorsStillSurface() {
        val files = mapOf(
            "src/main.rell" to "module;\nimport lib.mylib.receiver;\n",
            "src/lib/mylib/receiver.rell" to "module;\nval broken =\n"
        )
        val result = RellCheck.check(files, null)
        assertFalse(result.ok)
        assertTrue(result.errors.isNotEmpty())
    }

    // A lib-only submission (checking a library project itself) still compiles
    // everything via the ifEmpty-null fallback rather than compiling nothing.
    @Test
    fun libOnlySubmissionStillCompiles() {
        val files = mapOf(
            "src/lib/mylib/module.rell" to "module;\nfunction f(): integer = 1;\n"
        )
        val result = RellCheck.check(files, null)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.modules.isNotEmpty(), "lib-only submission must not compile nothing")
    }

    // ------------------------------------------------------- FT4 warnings --

    // ft4-demo: a one-line FT4 dapp came back with ~30 lib/ft4 nullability
    // warnings drowning its own output. Provisioned-library warnings are
    // suppressed and counted in notes; user warnings stay.
    @Test
    fun vendoredFt4WarningsAreSuppressed() {
        // Same import set as ft4-lib's demo example, which pulled ~30 lib/ft4
        // nullability warnings into the response.
        val files = mapOf(
            "src/main.rell" to """
                module;
                import lib.ft4.auth;
                import lib.ft4.accounts;
                import lib.ft4.admin;
                import lib.ft4.assets;
                operation dummy() {}
            """.trimIndent()
        )
        val result = RellCheck.check(files, null)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(
            result.warnings.none { it.file?.startsWith("lib/ft4/") == true },
            "vendored FT4 warnings must not leak: ${result.warnings}"
        )
        // "Warning:" is mixed case in compiler output; the parser must still
        // structure it (file=null warnings dodged the suppression entirely).
        assertTrue(
            result.warnings.none { it.file == null && it.text.contains("lib/ft4/") },
            "unparsed lib/ft4 warnings leaked: ${result.warnings}"
        )
        assertTrue(
            result.notes.contains("suppressed"),
            "notes must count the suppressed vendored warnings: ${result.notes}"
        )
    }

    @Test
    fun suppressionRewritesWarningsAndNotes() {
        val vendored = RellCheck.Diagnostic(
            "lib/ft4/core/accounts/module.rell", 237, 54, "WARNING", "cannot be null", "raw"
        )
        val user = RellCheck.Diagnostic("main.rell", 3, 1, "WARNING", "unused", "raw")
        val result = RellCheck.Result(true, listOf("main"), emptyList(), listOf(vendored, user), "ok.")
        val cleaned = RellCheck.suppressVendoredFt4Warnings(result)
        assertEquals(listOf(user), cleaned.warnings)
        assertTrue(cleaned.notes.contains("1 compiler warning(s) inside vendored FT4"), cleaned.notes)
        // No vendored warnings -> untouched.
        assertEquals(result.copy(warnings = listOf(user)), RellCheck.suppressVendoredFt4Warnings(result.copy(warnings = listOf(user))))
    }

    @Test
    fun ft4WarningLineFilterMatchesOnlyVendoredWarnings() {
        assertTrue(
            RunRellTests.isVendoredFt4Warning(
                "lib/ft4/core/accounts/strategies/module.rell(237:54) Warning: Expression 'details.disposable' cannot be null at this location"
            )
        )
        assertFalse(RunRellTests.isVendoredFt4Warning("main.rell(3:1) Warning: unused variable"))
        assertFalse(
            RunRellTests.isVendoredFt4Warning("lib/ft4/core/module.rell(1:1) ERROR: Module 'lib.iccf' not found")
        )
    }

    // price-oracle run_rell_tests: a RellCliException raised without printing
    // through cliEnv (e.g. module_args binding) produced "Rell test sources do
    // not compile:" followed by NOTHING. Fall back to the exception message.
    @Test
    fun testCompileFailureWithoutCapturedDiagnosticsReportsCauseMessage() {
        RunRellTests.runnerOverrideForTests = {
            throw net.postchain.rell.api.base.RellCliBasicException("Bad module args for module 'price_oracle'")
        }
        try {
            val e = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                RunRellTests.run(
                    mapOf("t_test.rell" to "@test module;\nfunction test_x() { assert_equals(1, 1); }")
                )
            }
            assertTrue(
                e.message!!.contains("Bad module args"),
                "fallback must surface the exception message: ${e.message}"
            )
        } finally {
            RunRellTests.runnerOverrideForTests = null
        }
    }

    // ------------------------------------------------------ security check --

    // filehub/filechain add_chunk_data: ICCF proof validation is the auth
    // mechanism for proof-carrying operations; require_valid_proof (bare or via
    // module import) must count as an auth marker.
    @Test
    fun iccfProofValidationCountsAsAuth() {
        val files = mapOf(
            "src/main.rell" to """
                module;
                entity stored_file { key hash: byte_array; }
                function require_valid_proof(tx: gtv, b: boolean) { require(b, "invalid proof"); }
                operation add_chunk_data(proof: gtv, data: byte_array) {
                    val hash = data.sha256();
                    require_valid_proof(proof, true);
                    create stored_file(hash);
                }
            """.trimIndent()
        )
        val result = RellSecurityCheck.analyze(files)
        assertTrue(
            result.findings.none { it.rule == "unauthenticated-mutation" },
            result.findings.toString()
        )
    }

    // iccf-example prove_that_i_was_invoked: mutations derived from
    // op_context.get_signers() are tied to the real transaction signers.
    @Test
    fun signerDerivedMutationCountsAsAuth() {
        val files = mapOf(
            "src/main.rell" to """
                module;
                entity invocation { key pubkey; }
                operation prove_that_i_was_invoked() {
                    for (signer in op_context.get_signers()) {
                        if (not(exists(invocation @? { signer }))) {
                            create invocation(signer);
                        }
                    }
                }
            """.trimIndent()
        )
        val result = RellSecurityCheck.analyze(files)
        assertTrue(
            result.findings.none { it.rule == "unauthenticated-mutation" },
            result.findings.toString()
        )
    }

    // The rule still fires on a genuinely unauthenticated mutation.
    @Test
    fun plainUnauthenticatedMutationStillFlagged() {
        val files = mapOf(
            "src/main.rell" to """
                module;
                entity e { key k: text; }
                operation open_write(t: text) { create e(t); }
            """.trimIndent()
        )
        val result = RellSecurityCheck.analyze(files)
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" },
            "plain unauthenticated mutation must still be flagged"
        )
    }
}
