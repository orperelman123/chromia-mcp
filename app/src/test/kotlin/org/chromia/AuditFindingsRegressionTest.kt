package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.CheckDappProject
import org.chromia.tools.DappScaffold
import org.chromia.tools.RellCheck
import org.chromia.tools.RellCheckStrategy
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RellSecurityCheckStrategy
import org.chromia.tools.RunRellTests
import org.chromia.tools.RunRellTestsStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the 2026-09-01 audit of the Rell tool chain, verified
 * against the Rell compiler sources (C_ModuleUtils.getModuleInfo, grammar.kt):
 *
 * 1. Module derivation must consider file CONTENT: a file with no module header
 *    belongs to its directory's module, so the recommended app/module.rell +
 *    app/entities.rell layout must not derive a phantom "app.entities" module.
 * 2. Mutation call graph: an operation that mutates only via a helper function
 *    still needs an auth finding.
 * 3. MUTATION_REGEX must catch paren forms: delete(u), update(u)(...).
 * 4. Operation/function declarations not at line start must still be scanned.
 * 5. check_dapp_project: a bare-string rell input must be compiled, not
 *    silently skipped; normalization collisions must error.
 * 6. Banned-module rules must ignore string literals and respect boundaries.
 * 7. Auth analysis is global across files; aliased FT4 auth imports count.
 * 8. A timed-out test run reports cleanly and the tool remains usable.
 * 9. Non-string `files` values are a validation error, not a silent drop.
 */
class AuditFindingsRegressionTest {

    private val repo = RecordingRepository()

    // ---- Finding 1: header-aware module derivation -------------------------

    @Test
    fun headerlessFileBelongsToDirectoryModule() {
        assertEquals("app", RunRellTests.moduleNameForPath("app/entities.rell", "entity account { key id: text; }"))
        assertEquals("", RunRellTests.moduleNameForPath("main.rell", "entity account { key id: text; }"))
        // A real module header keeps the file-module mapping.
        assertEquals("app.entities", RunRellTests.moduleNameForPath("app/entities.rell", "module;\nentity a { key id: text; }"))
        // module.rell is a directory module regardless of content.
        assertEquals("app", RunRellTests.moduleNameForPath("app/module.rell", "module;"))
    }

    @Test
    fun moduleHeaderDetectorMirrorsCompilerGrammar() {
        assertTrue(RunRellTests.hasModuleHeader("module;"))
        assertTrue(RunRellTests.hasModuleHeader("  \n module ;"))
        assertTrue(RunRellTests.hasModuleHeader("@test module;"))
        assertTrue(RunRellTests.hasModuleHeader("@mount('acc') module;"))
        assertTrue(RunRellTests.hasModuleHeader("@test\n// note\nmodule;"))
        assertTrue(RunRellTests.hasModuleHeader("abstract module;"))
        assertTrue(RunRellTests.hasModuleHeader("// leading comment\nmodule;"))
        assertTrue(RunRellTests.hasModuleHeader("/* block */ module;"))
        assertFalse(RunRellTests.hasModuleHeader("entity user { key id: text; }"))
        assertFalse(RunRellTests.hasModuleHeader("// module; only a comment"))
        assertFalse(RunRellTests.hasModuleHeader("@mount('acc') entity user { key id: text; }"))
        assertFalse(RunRellTests.hasModuleHeader("function module_helper() = 1;"))
        assertFalse(RunRellTests.hasModuleHeader("import app; module;"))
    }

    @Test
    fun moduleRellPlusHeaderlessSiblingCompilesGreen() {
        val files = mapOf(
            "app/module.rell" to "module;\n",
            "app/entities.rell" to "entity account { key id: text; }\n"
        )
        val result = RellCheck.check(files, null)
        assertTrue(result.ok, result.errors.toString())
        assertEquals(listOf("app"), result.modules)
    }

    @Test
    fun singleHeaderlessFileCompilesGreen() {
        val result = RellCheck.check(mapOf("main.rell" to "entity account { key id: text; }\n"), null)
        assertTrue(result.ok, result.errors.toString())
    }

    @Test
    fun securityCheckCompileGatePassesRecommendedLayout() {
        val result = runBlocking {
            RellSecurityCheckStrategy().execute(
                callToolRequest(
                    name = "rell_security_check",
                    arguments = buildJsonObject {
                        put(
                            "files",
                            buildJsonObject {
                                put("app/module.rell", "module;\n")
                                put("app/entities.rell", "entity account { key id: text; }\n")
                            }
                        )
                    }
                ),
                repo
            )
        }
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertFalse(structured.containsKey("compileErrors"), structured.toString())
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
    }

    @Test
    fun runRellTestsResolvesHeaderlessSiblingModules() {
        val result = RunRellTests.run(
            mapOf(
                "app/module.rell" to "module;\n",
                "app/helpers.rell" to "function triple(x: integer): integer = x * 3;\n",
                "tests/module.rell" to
                    "@test module;\nimport app;\nfunction test_triple() { assert_equals(app.triple(2), 6); }\n"
            ),
            databaseUrl = null
        )
        assertTrue(result.ok, result.notes + " " + result.cases.toString())
        assertEquals(1, result.passed)
    }

    // ---- Finding 2: transitive mutation via helper functions ---------------

    @Test
    fun helperMutationWithoutAuthIsFlagged() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity vault { key owner: text; mutable amount: integer; }
                    function do_transfer(owner: text, amount: integer) {
                        update vault @ { .owner == owner } ( .amount -= amount );
                    }
                    operation transfer(owner: text, amount: integer) {
                        do_transfer(owner, amount);
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" && it.severity == "HIGH" },
            result.findings.toString()
        )
    }

    @Test
    fun helperMutationWithAuthIsClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity vault { key owner: text; mutable amount: integer; }
                    function do_transfer(owner: text, amount: integer) {
                        update vault @ { .owner == owner } ( .amount -= amount );
                    }
                    operation transfer(owner: text, amount: integer) {
                        require(op_context.is_signer(x"03"), "not authorized");
                        do_transfer(owner, amount);
                    }
                """.trimIndent()
            )
        )
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    @Test
    fun nonMutatingHelperIsClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    function fee(amount: integer): integer = amount / 100;
                    operation quote(amount: integer) {
                        require(amount > 0, "positive");
                        val f = fee(amount);
                    }
                """.trimIndent()
            )
        )
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    // ---- Finding 3: paren-form mutations -----------------------------------

    @Test
    fun parenFormDeleteAndUpdateAreFlagged() {
        val deleteResult = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to
                    "module;\nentity user { key id: text; }\noperation kill(id: text) {\n    val u = user @ { .id == id };\n    delete(u);\n}\n"
            )
        )
        assertTrue(
            deleteResult.findings.any { it.rule == "unauthenticated-mutation" },
            deleteResult.findings.toString()
        )
        val updateResult = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to
                    "module;\nentity user { key id: text; mutable score: integer; }\noperation bump(id: text) {\n    val u = user @ { .id == id };\n    update(u)( .score = 1 );\n}\n"
            )
        )
        assertTrue(
            updateResult.findings.any { it.rule == "unauthenticated-mutation" },
            updateResult.findings.toString()
        )
    }

    @Test
    fun mutationLookalikeIdentifiersAreNotFlagged() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    function update_helper(x: integer): integer = x + 1;
                    operation touch(x: integer) {
                        require(x > 0, "positive");
                        val created = 1;
                        val y = update_helper(x);
                    }
                """.trimIndent()
            )
        )
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    // ---- Finding 4: declarations not at line start -------------------------

    @Test
    fun sameLineAnnotationOperationIsScanned() {
        val source = "module;\nentity acc { key id: text; }\n@mount('acc') operation withdraw(id: text) { delete acc @* { .id == id }; }\n"
        val result = RellSecurityCheck.analyze(mapOf("main.rell" to source))
        assertEquals(1, result.operationsScanned, result.toString())
        val finding = result.findings.first { it.rule == "unauthenticated-mutation" }
        assertEquals(3, finding.line)
    }

    @Test
    fun oneLineNamespaceOperationIsScanned() {
        val source = "module;\nentity acc { key id: text; }\nnamespace admin { operation drain() { delete acc @* {}; } }\n"
        val result = RellSecurityCheck.analyze(mapOf("main.rell" to source))
        assertEquals(1, result.operationsScanned, result.toString())
        assertTrue(result.findings.any { it.rule == "unauthenticated-mutation" && it.line == 3 }, result.findings.toString())
    }

    @Test
    fun operationAfterClosingBraceIsScanned() {
        val source = "module;\nentity acc { key id: text; }\noperation a() {\n    require(op_context.is_signer(x\"03\"), \"no\");\n} operation b() { delete acc @* {}; }\n"
        val result = RellSecurityCheck.analyze(mapOf("main.rell" to source))
        assertEquals(2, result.operationsScanned, result.toString())
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" && it.text.contains("operation b") && it.line == 5 },
            result.findings.toString()
        )
    }

    @Test
    fun normalLayoutLineNumbersUnchanged() {
        val ops = RellSecurityCheck.scanOperations(
            "main.rell",
            "module;\n\noperation first(a: text) {\n    require(a != \"\");\n}\n\noperation second() {\n}\n"
        )
        assertEquals(listOf("first" to 3, "second" to 7), ops.map { it.name to it.line })
    }

    // ---- Finding 5: check_dapp_project bare-string and collisions ----------

    @Test
    fun bareStringRellIsCompiledAndSecurityChecked() = runBlocking {
        val yaml = DappScaffold.files("hello_dapp").getValue("chromia.yml")
        val result = org.chromia.tools.CheckDappProjectStrategy().execute(
            callToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("yaml", yaml)
                    put(
                        "rell",
                        "module;\nentity acc { key id: text; }\noperation drain() { delete acc @* {}; }\n"
                    )
                }
            ),
            repo
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(false, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
        val errors = structured.getValue("errors").jsonArray.map { it.jsonPrimitive.content }
        assertTrue(errors.any { it.contains("unauthenticated-mutation") }, errors.toString())
    }

    @Test
    fun collidingNormalizedPathsAreAnError() {
        val yaml = DappScaffold.files("hello_dapp").getValue("chromia.yml")
        val result = CheckDappProject.check(
            yaml,
            mapOf(
                "src/main.rell" to "module;\n",
                "main.rell" to "module;\nentity acc { key id: text; }\n"
            )
        )
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("same file") }, result.errors.toString())
    }

    @Test
    fun uncompilableRellKeysAreAnErrorNotASilentSkip() {
        val yaml = DappScaffold.files("hello_dapp").getValue("chromia.yml")
        val result = CheckDappProject.check(yaml, mapOf("notes" to "module;\n"))
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("no compilable .rell files") }, result.errors.toString())
    }

    // ---- Finding 6: banned names in strings / boundary checks --------------

    @Test
    fun bannedModuleNameInsideStringIsNotAFinding() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to
                    "module;\noperation ping() {\n    require(true, \"moved out of lib.ft4.admin\");\n}\n"
            )
        )
        assertTrue(result.findings.none { it.rule == "banned-module" }, result.findings.toString())
    }

    @Test
    fun bannedModulePrefixOfLongerNameIsNotAFinding() {
        val result = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nimport lib.ft4.admin_utils;\n")
        )
        assertTrue(result.findings.none { it.rule == "banned-module" }, result.findings.toString())
    }

    @Test
    fun realBannedImportIsStillCritical() {
        val result = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nimport lib.ft4.admin;\n")
        )
        assertTrue(
            result.findings.any { it.rule == "banned-module" && it.severity == "CRITICAL" },
            result.findings.toString()
        )
    }

    // ---- Finding 7: cross-file auth helpers and aliased FT4 auth -----------

    @Test
    fun authHelperInSiblingFileIsRecognized() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "auth.rell" to """
                    module;
                    function require_admin() {
                        require(op_context.is_signer(x"03"), "not admin");
                    }
                """.trimIndent(),
                "ops.rell" to """
                    module;
                    entity acc { key id: text; }
                    operation wipe() {
                        require_admin();
                        delete acc @* {};
                    }
                """.trimIndent()
            )
        )
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    @Test
    fun aliasedFt4AuthImportIsRecognized() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import a: lib.ft4.auth;
                    entity note { key id: text; }
                    operation add(id: text) {
                        a.authenticate();
                        create note(id);
                    }
                """.trimIndent()
            )
        )
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    @Test
    fun genuinelyUnauthedOperationIsStillFlagged() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to "module;\nentity acc { key id: text; }\noperation wipe() { delete acc @* {}; }\n"
            )
        )
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" && it.severity == "HIGH" },
            result.findings.toString()
        )
    }

    // ---- Finding 8: timeout leaves the tool usable -------------------------

    @Test
    fun timedOutRunReportsCleanlyAndToolStaysUsable() {
        val timedOut = RunRellTests.run(
            mapOf(
                "spin_test.rell" to
                    "@test module;\nfunction test_spin() {\n    var i = 0;\n    while (i >= 0) { i = (i + 1) % 1000; }\n}\n"
            ),
            databaseUrl = null,
            timeoutSeconds = 3
        )
        assertFalse(timedOut.ok)
        assertTrue(timedOut.notes.contains("exceeded 3s"), timedOut.notes)
        assertTrue(timedOut.notes.contains("abandoned runner thread"), timedOut.notes)

        // Subsequent calls must work normally after a timeout.
        val next = RunRellTests.run(
            mapOf("math_test.rell" to "@test module;\nfunction test_math() { assert_equals(2 + 2, 4); }"),
            databaseUrl = null
        )
        assertTrue(next.ok, next.notes)
    }

    // ---- Finding 9: non-string files values --------------------------------

    @Test
    fun nonStringFilesValueIsAValidationError() = runBlocking {
        val badFiles = buildJsonObject {
            put("main.rell", "module;")
            put("bad.rell", buildJsonObject { put("content", "module;") })
        }
        listOf<Pair<String, suspend () -> io.modelcontextprotocol.kotlin.sdk.types.CallToolResult>>(
            "rell_check" to {
                RellCheckStrategy().execute(
                    callToolRequest(name = "rell_check", arguments = buildJsonObject { put("files", badFiles) }),
                    repo
                )
            },
            "rell_security_check" to {
                RellSecurityCheckStrategy().execute(
                    callToolRequest(name = "rell_security_check", arguments = buildJsonObject { put("files", badFiles) }),
                    repo
                )
            },
            "run_rell_tests" to {
                RunRellTestsStrategy().execute(
                    callToolRequest(name = "run_rell_tests", arguments = buildJsonObject { put("files", badFiles) }),
                    repo
                )
            }
        ).forEach { (tool, call) ->
            val result = call()
            assertEquals(true, result.isError, tool)
            val text = (result.content.first() as TextContent).text!!
            assertTrue(text.contains("bad.rell"), "$tool: $text")
        }
    }

    // ---- Minor: require with space, auth_handler boundary ------------------

    @Test
    fun requireWithSpaceBeforeParenCountsAsValidation() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to "module;\noperation ping(x: integer) {\n    require (x > 0, \"positive\");\n}\n"
            )
        )
        assertTrue(result.findings.none { it.rule == "unvalidated-inputs" }, result.findings.toString())
    }

    @Test
    fun authHandlerLikeIdentifierIsNotAnAuthMarker() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity acc { key id: text; }
                    function auth_handlers_cfg(): integer = 1;
                    operation wipe() {
                        val cfg = auth_handlers_cfg;
                        delete acc @* {};
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" },
            result.findings.toString()
        )
    }
}
