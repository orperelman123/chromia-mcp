package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.CheckDappProject
import org.chromia.tools.DappScaffold
import org.chromia.tools.Ft4ImportCheck
import org.chromia.tools.PromptManager
import org.chromia.tools.RellCheck
import org.chromia.tools.RellCheckStrategy
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.RunRellTestsStrategy
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Regressions for the agent-UX/docs audit findings (2026-09-01): F1-F5, F7-F9. */
class AuditUxRegressionTest {

    private val repo = RecordingRepository()

    private fun callViaExecutor(tool: String, args: kotlinx.serialization.json.JsonObject) = runBlocking {
        ToolExecutor(repo, PromptManager())
            .executeTool(CallToolRequest(name = tool, arguments = args))
    }

    private fun scaffoldRell(): Map<String, String> =
        DappScaffold.files("hello").filterKeys { it.endsWith(".rell") }

    // ---- F1: scaffold-shaped src/ paths must compile and test green ----------

    @Test
    fun scaffoldPathsCompileVerbatimInRellCheck() {
        val result = RellCheck.check(scaffoldRell(), null)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.modules.contains("main"), result.modules.toString())
    }

    @Test
    fun scaffoldPathsRunVerbatimInRunRellTests() {
        // src/test/main_test.rell does `import main;` - the src/ prefix must not
        // become part of the module name (src.test.main_test / src.main). A
        // module-resolution failure aborts compilation before any case runs, so
        // a finished test_hello_world case proves the import resolved. The hello
        // template's `object` needs PostgreSQL - without CHROMIA_TEST_DATABASE_URL
        // the only acceptable failure is the environmental dbRequired one.
        val databaseUrl = System.getenv(RunRellTests.DATABASE_URL_ENV)
        val result = RunRellTests.run(scaffoldRell(), databaseUrl = databaseUrl)
        assertEquals(1, result.total, result.notes)
        val case = result.cases.single()
        assertTrue(case.name.contains("test_hello_world"), case.toString())
        if (databaseUrl.isNullOrBlank()) {
            assertTrue(case.ok || case.dbRequired, "unexpected failure: $case")
        } else {
            assertTrue(result.ok, result.notes + " " + result.cases.toString())
        }
    }

    @Test
    fun srcPrefixCollisionIsAClearErrorInRellCheck() {
        val e = runCatching {
            RellCheck.check(
                mapOf("src/main.rell" to "module;\n", "main.rell" to "module;\n"),
                null
            )
        }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException, e.toString())
        assertTrue(e!!.message!!.contains("resolve to the same file"), e.message)
    }

    @Test
    fun srcPrefixCollisionIsAClearErrorInRunRellTests() {
        val e = runCatching {
            RunRellTests.run(
                mapOf(
                    "src/main_test.rell" to "@test module;\nfunction test_x() {}\n",
                    "main_test.rell" to "@test module;\nfunction test_y() {}\n"
                ),
                databaseUrl = null
            )
        }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException, e.toString())
        assertTrue(e!!.message!!.contains("resolve to the same file"), e.message)
    }

    // ---- F2: invalid `rell` map entries are named, never "missing" -----------

    @Test
    fun mixedRellMapNamesTheInvalidKey() {
        val yaml = DappScaffold.files("hello").getValue("chromia.yml")
        val result = callViaExecutor(
            "check_dapp_project",
            buildJsonObject {
                put("yaml", yaml)
                put(
                    "rell",
                    buildJsonObject {
                        put("main.rell", "module;\n")
                        put("extra.rell", buildJsonObject { put("content", "module;\n") })
                    }
                )
            }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("extra.rell"), text)
        assertFalse(text.contains("Missing required parameter"), text)
    }

    @Test
    fun allInvalidRellMapNamesTheKeysNotMissingParameter() {
        val result = callViaExecutor(
            "check_ft4_imports",
            buildJsonObject {
                put(
                    "rell",
                    buildJsonObject {
                        put("a.rell", 42)
                        put("b.rell", buildJsonObject { put("x", "y") })
                    }
                )
            }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("a.rell") && text.contains("b.rell"), text)
        assertFalse(text.contains("Missing required parameter"), text)
    }

    @Test
    fun absentRellParameterIsStillReportedAsMissing() {
        val result = callViaExecutor(
            "check_ft4_imports",
            buildJsonObject { }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Missing required parameter: rell"), text)
    }

    // ---- F3: malformed moduleArgs is an error, not a silent no-op ------------

    @Test
    fun jsonEncodedModuleArgsIsRejectedWithGuidance() {
        val result = callViaExecutor(
            "run_rell_tests",
            buildJsonObject {
                put("files", buildJsonObject { put("t.rell", "@test module;\nfunction test_a() {}\n") })
                put("moduleArgs", "{\"main\": {\"greeting\": \"hi\"}}")
            }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("moduleArgs must be an object mapping module name"), text)
        assertTrue(text.contains("do not JSON-encode it"), text)
    }

    @Test
    fun perModuleNonObjectArgsNamesTheModule() {
        val result = callViaExecutor(
            "run_rell_tests",
            buildJsonObject {
                put("files", buildJsonObject { put("t.rell", "@test module;\nfunction test_a() {}\n") })
                put("moduleArgs", buildJsonObject { put("lib.ft4.core.accounts", "not-an-object") })
            }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("lib.ft4.core.accounts"), text)
        assertTrue(text.contains("args object"), text)
    }

    @Test
    fun wellFormedModuleArgsStillReachTheTests() {
        val result = callViaExecutor(
            "run_rell_tests",
            buildJsonObject {
                put(
                    "files",
                    buildJsonObject {
                        put(
                            "main.rell",
                            "module;\nstruct module_args { greeting: text; }\n" +
                                "function greet(): text = chain_context.args.greeting;\n"
                        )
                        put(
                            "main_test.rell",
                            "@test module;\nimport main;\n" +
                                "function test_greet() { assert_equals(main.greet(), \"hi\"); }\n"
                        )
                    }
                )
                put("moduleArgs", buildJsonObject { put("main", buildJsonObject { put("greeting", "hi") }) })
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
        assertEquals("1", structured.getValue("passed").jsonPrimitive.content)
    }

    // ---- F4: zero test_* functions gets an explanation -----------------------

    @Test
    fun zeroTestFunctionsIsExplainedInNotes() {
        val result = RunRellTests.run(
            mapOf("t.rell" to "@test module;\nfunction check_things() {}\n"),
            databaseUrl = null
        )
        assertFalse(result.ok)
        assertEquals(0, result.total)
        assertTrue(
            result.notes.contains("test functions must be named test_*"),
            result.notes
        )
    }

    // ---- F5: help tools no longer describe the 0.16.7 source tag as a pin ----

    @Test
    fun rellHelpToolsLabelSourceTagAndPinHonestly() {
        val outputs = mapOf(
            "language" to (org.chromia.tools.ChromiaRellLanguageHelp.notes() to org.chromia.tools.ChromiaRellLanguageHelp.toJson()),
            "database" to (org.chromia.tools.ChromiaRellDatabaseHelp.notes() to org.chromia.tools.ChromiaRellDatabaseHelp.toJson()),
            "expressions" to (org.chromia.tools.ChromiaRellExpressionsHelp.notes() to org.chromia.tools.ChromiaRellExpressionsHelp.toJson()),
            "statements" to (org.chromia.tools.ChromiaRellStatementsHelp.notes() to org.chromia.tools.ChromiaRellStatementsHelp.toJson()),
            "types" to (org.chromia.tools.ChromiaRellTypesHelp.notes() to org.chromia.tools.ChromiaRellTypesHelp.toJson()),
            "systemlib" to (org.chromia.tools.ChromiaRellSystemlibHelp.notes() to org.chromia.tools.ChromiaRellSystemlibHelp.toJson()),
            "practices" to (org.chromia.tools.ChromiaRellPracticesHelp.notes() to org.chromia.tools.ChromiaRellPracticesHelp.toJson())
        )
        // "pin" followed by 0.16.7 as the FIRST version number is the misleading
        // wording that made agents write compile.rellVersion 0.16.7 and hit the
        // validator error.
        val pinnedSourceTag = Regex("""pin[^0-9]{0,60}0\.16\.7""")
        outputs.forEach { (name, pair) ->
            val (notes, payload) = pair
            assertFalse(pinnedSourceTag.containsMatchIn(notes), "$name notes still call 0.16.7 a pin:\n$notes")
            assertTrue(
                notes.contains("source tag ${DappScaffold.RELL_SOURCE_TAG}"),
                "$name notes should label ${DappScaffold.RELL_SOURCE_TAG} as the language source tag:\n$notes"
            )
            assertTrue(
                notes.contains("pin is ${DappScaffold.RELL_VERSION}") || notes.contains("pin stays ${DappScaffold.RELL_VERSION}"),
                "$name notes should state the chromia.yml pin ${DappScaffold.RELL_VERSION}:\n$notes"
            )
            assertEquals(DappScaffold.RELL_SOURCE_TAG, payload.getValue("rellSourceTag").jsonPrimitive.content, name)
            assertEquals(DappScaffold.RELL_VERSION, payload.getValue("rellVersionPin").jsonPrimitive.content, name)
        }
    }

    // ---- F7: banned module names inside string literals are not imports ------

    @Test
    fun bannedModuleNameInsideStringIsNotAForbiddenImport() {
        val rell = "module;\nquery docs() = \"never use lib.ft4.admin\";\nimport lib.ft4.assets;\n"
        val result = Ft4ImportCheck.scan(rell)
        assertTrue(result.ok, result.errors.toString())
        // A real import on another line is still flagged.
        val bad = Ft4ImportCheck.scan(rell + "import lib.ft4.admin;\n")
        assertFalse(bad.ok)
        assertTrue(bad.errors.any { it.contains("line 4") && it.contains("lib.ft4.admin") }, bad.errors.toString())
    }

    @Test
    fun checkDappProjectAgreesWithSecurityCheckOnStringMentions() {
        val rell = "module;\nquery docs() = \"never use lib.ft4.admin\";\n"
        val security = RellSecurityCheck.analyze(mapOf("main.rell" to rell))
        assertTrue(security.findings.none { it.rule == "banned-module" }, security.findings.toString())
        val yaml = DappScaffold.files("hello").getValue("chromia.yml")
        val project = CheckDappProject.check(yaml, mapOf("main.rell" to rell))
        assertTrue(
            project.errors.none { it.contains("forbidden FT4") },
            project.errors.toString()
        )
    }

    // ---- F8: FT4 and compile findings share one location format --------------

    @Test
    fun ft4AndCompileFindingsShareTheLocationFormat() {
        val yaml = DappScaffold.files("hello").getValue("chromia.yml")
        val result = CheckDappProject.check(
            yaml,
            mapOf("src/main.rell" to "module;\nimport lib.ft4.admin;\nquery broken() = unknown_symbol_xyz;\n")
        )
        assertFalse(result.ok)
        val located = Regex("""^main\.rell:\d+: """)
        val ft4 = result.errors.filter { it.contains("forbidden FT4") }
        assertTrue(ft4.isNotEmpty(), result.errors.toString())
        ft4.forEach { assertTrue(located.containsMatchIn(it), "FT4 finding format: $it") }
        val other = result.errors.filter { !it.contains("forbidden FT4") && !it.startsWith(CheckDappProject.YAML_PATH) }
        assertTrue(other.isNotEmpty(), "expected a compile diagnostic too: ${result.errors}")
        assertTrue(
            other.any { located.containsMatchIn(it) },
            "compile finding format: $other"
        )
    }

    // ---- F9: get_asset_top_holders limit is validated and capped -------------

    @Test
    fun nonNumericTopHoldersLimitIsRejected() {
        val result = callViaExecutor(
            "get_asset_top_holders",
            buildJsonObject { put("assetId", "x"); put("limit", "twenty") }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must be an integer"), text)
        assertEquals(null, repo.lastCall, "must not reach the repository")
    }

    @Test
    fun overCapTopHoldersLimitIsRejected() {
        val result = callViaExecutor(
            "get_asset_top_holders",
            buildJsonObject { put("assetId", "x"); put("limit", 100000) }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("must not exceed"), text)
        assertEquals(null, repo.lastCall, "must not reach the repository")
    }

    // ---- Verification note: diagnostics carry the submitted relative path ----

    @Test
    fun compileDiagnosticsUseTheSubmittedRelativePath() {
        val result = RellCheck.check(
            mapOf("app/bad.rell" to "module;\nquery broken() = unknown_symbol_xyz;\n"),
            null
        )
        assertFalse(result.ok)
        val files = result.errors.mapNotNull { it.file }
        assertTrue(files.isNotEmpty(), result.errors.toString())
        files.forEach { file ->
            assertEquals("app/bad.rell", file.replace('\\', '/'), "temp-dir prefix must never leak: $file")
        }
    }

    @Test
    fun compileDiagnosticsUseTheNormalizedPathForSrcPrefixedInput() {
        val result = RellCheck.check(
            mapOf("src/app/bad.rell" to "module;\nquery broken() = unknown_symbol_xyz;\n"),
            null
        )
        assertFalse(result.ok)
        val files = result.errors.mapNotNull { it.file }
        assertTrue(files.isNotEmpty(), result.errors.toString())
        files.forEach { file ->
            assertEquals("app/bad.rell", file.replace('\\', '/'), file)
        }
    }

    @Test
    fun runRellTestsViaToolAcceptsScaffoldOutputVerbatim() {
        val files = scaffoldRell()
        val result = runBlocking {
            RunRellTestsStrategy().execute(
                CallToolRequest(
                    name = "run_rell_tests",
                    arguments = buildJsonObject {
                        put(
                            "files",
                            buildJsonObject { files.forEach { (k, v) -> put(k, v) } }
                        )
                    }
                ),
                repo
            )
        }
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals("1", structured.getValue("total").jsonPrimitive.content, structured.toString())
        val case = structured.getValue("cases").jsonArray.single().jsonObject
        assertTrue(
            case.getValue("name").jsonPrimitive.content.contains("test_hello_world"),
            case.toString()
        )
        // Green when a test database is configured; environmental dbRequired
        // failure otherwise (the hello template's `object` needs PostgreSQL).
        val ok = case.getValue("ok").jsonPrimitive.content.toBoolean()
        val dbRequired = case["dbRequired"]?.jsonPrimitive?.content?.toBoolean() == true
        assertTrue(ok || dbRequired, structured.toString())
    }

    @Test
    fun rellCheckViaToolAcceptsScaffoldOutputVerbatim() {
        val files = scaffoldRell()
        val result = runBlocking {
            RellCheckStrategy().execute(
                CallToolRequest(
                    name = "rell_check",
                    arguments = buildJsonObject {
                        put(
                            "files",
                            buildJsonObject { files.forEach { (k, v) -> put(k, v) } }
                        )
                    }
                ),
                repo
            )
        }
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
        val modules = structured.getValue("modules").jsonArray.map { it.jsonPrimitive.content }
        assertTrue(modules.contains("main"), modules.toString())
    }
}
