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
import org.chromia.tools.AssetTopHoldersStrategy
import org.chromia.tools.AllTransactionsStrategy
import org.chromia.tools.CheckDappProject
import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.DappScaffold
import org.chromia.tools.RellCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.ScaffoldDappStrategy
import org.chromia.tools.WriteDeploymentConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Regressions for the adversarial QA edge-case findings (2026-08-31). */
class QaEdgeCaseRegressionTest {

    private val repo = RecordingRepository()

    private fun call(strategy: org.chromia.tools.ToolStrategy, args: kotlinx.serialization.json.JsonObject) =
        runBlocking { strategy.execute(callToolRequest(name = "t", arguments = args), repo) }

    /** Real agent path: the executor converts validation failures into tool errors. */
    private fun callViaExecutor(tool: String, args: kotlinx.serialization.json.JsonObject) = runBlocking {
        org.chromia.tools.ToolExecutor(repo, org.chromia.tools.PromptManager())
            .executeTool(callToolRequest(name = tool, arguments = args))
    }

    // 1. scaffold_dapp must never silently rename
    @Test
    fun invalidScaffoldNameProducesWarning() {
        val json = DappScaffold.toJson("my-dapp")
        assertEquals("hello", json.getValue("name").jsonPrimitive.content)
        val warnings = json.getValue("warnings").jsonArray.map { it.jsonPrimitive.content }
        assertTrue(warnings.any { it.contains("my-dapp") }, warnings.toString())
    }

    @Test
    fun unknownTemplateProducesWarning() {
        val json = DappScaffold.toJson("notes", template = "nonexistent")
        val warnings = json.getValue("warnings").jsonArray.map { it.jsonPrimitive.content }
        assertTrue(warnings.any { it.contains("nonexistent") }, warnings.toString())
    }

    @Test
    fun validNameHasNoWarnings() {
        val json = DappScaffold.toJson("notes_app", template = "ft4")
        assertEquals("notes_app", json.getValue("name").jsonPrimitive.content)
        assertTrue(json.getValue("warnings").jsonArray.isEmpty())
    }

    @Test
    fun scaffoldStrategySurfacesWarnings() {
        val result = call(ScaffoldDappStrategy(), buildJsonObject { put("name", "My Dapp!") })
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("warnings"), text.take(200))
    }

    // 2. BOM must not break valid sources
    @Test
    fun bomPrefixedSourceCompiles() {
        val bom = "﻿"
        val result = RellCheck.check(mapOf("main.rell" to bom + "module;\nquery ping() = \"pong\";"), null)
        assertTrue(result.ok, "BOM-prefixed source must compile: ${result.errors}")
    }

    // Reality audit D7: an only-@test submission used to report "Compiled 0
    // module(s) successfully" although the test modules did compile.
    @Test
    fun onlyTestModuleSubmissionCountsTestModulesInNotes() {
        val result = RellCheck.check(
            mapOf("my_test.rell" to "@test module;\nfunction test_nothing() {}"),
            null
        )
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.notes.contains("1 @test module(s)"), result.notes)
        assertFalse(result.notes.contains("0 module(s)"), result.notes)
    }

    @Test
    fun mixedSubmissionCountsAppAndTestModulesSeparately() {
        val result = RellCheck.check(
            mapOf(
                "main.rell" to "module;\nquery ping() = \"pong\";",
                "my_test.rell" to "@test module;\nfunction test_nothing() {}"
            ),
            null
        )
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.notes.contains("1 module(s) and 1 @test module(s)"), result.notes)
    }

    @Test
    fun plainSubmissionNotesStayUnchanged() {
        val result = RellCheck.check(mapOf("main.rell" to "module;\nquery ping() = \"pong\";"), null)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.notes.contains("Compiled 1 module(s) successfully"), result.notes)
        assertFalse(result.notes.contains("@test"), result.notes)
    }

    @Test
    fun stripBomOnlyRemovesLeadingMarker() {
        assertEquals("module;", RellCheck.stripBom("﻿module;"))
        assertEquals("a﻿b", RellCheck.stripBom("a﻿b"))
    }

    // 3. Case-insensitive path collisions must be explicit
    @Test
    fun caseCollisionIsRejectedWithClearMessage() {
        val e = runCatching {
            RellCheck.check(mapOf("a.rell" to "module;", "A.rell" to "module;"), null)
        }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException, "expected validation error, got $e")
        assertTrue(e!!.message!!.contains("collision"), e.message!!)
    }

    @Test
    fun caseCollisionRejectedInTestRunner() {
        val e = runCatching {
            RunRellTests.run(mapOf("t.rell" to "@test module;", "T.rell" to "@test module;"), databaseUrl = null)
        }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException, "expected validation error, got $e")
        assertTrue(e!!.message!!.contains("collision"), e.message!!)
    }

    // 5. Invalid pagination/time ranges fail locally, not upstream
    @Test
    fun negativeLimitIsRejectedLocally() {
        val result = callViaExecutor("get_asset_top_holders", buildJsonObject { put("assetId", "x"); put("limit", -1) })
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must be a positive integer"), text.take(200))
        assertEquals(null, repo.lastCall, "must not reach the repository")
    }

    @Test
    fun zeroLimitIsRejectedLocally() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("limit", 0) })
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must be a positive integer"), text.take(200))
    }

    @Test
    fun invertedTimeRangeIsRejectedLocally() {
        val result = callViaExecutor(
            "get_all_transactions",
            buildJsonObject { put("timestampFrom", "2000000000000"); put("timestampTo", "1000000000000") }
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("must not be later than"), text.take(200))
    }

    @Test
    fun validPaginationStillPassesThrough() {
        val result = call(AllTransactionsStrategy(), buildJsonObject { put("limit", 5); put("offset", 0) })
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    // 6. ISO time windows were never validated: requireOrderedTimestamps only
    // tried toLongOrNull, so "ISO format" (the documented schema) was a no-op.
    @Test
    fun invertedIsoTimeRangeIsRejectedLocally() {
        val result = callViaExecutor(
            "get_all_transactions",
            buildJsonObject { put("timestampFrom", "2025-06-01T00:00:00Z"); put("timestampTo", "2024-01-01T00:00:00Z") }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("must not be later than"), text.take(200))
    }

    @Test
    fun orderedIsoTimeRangePassesThrough() {
        val result = call(
            AllTransactionsStrategy(),
            buildJsonObject { put("timestampFrom", "2024-01-01"); put("timestampTo", "2025-06-01T12:30:00Z") }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    @Test
    fun malformedTimestampsDoNotThrowLocally() {
        val result = call(
            AllTransactionsStrategy(),
            buildJsonObject { put("timestampFrom", "not-a-date"); put("timestampTo", "also-bad") }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    @Test
    fun mixedEpochAndIsoTimestampsAreNotRejected() {
        val result = call(
            AllTransactionsStrategy(),
            buildJsonObject { put("timestampFrom", "1700000000000"); put("timestampTo", "2020-01-01T00:00:00Z") }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    // 7. Malformed pagination used to be silently dropped by extractInt,
    // hiding the agent's mistake behind unpaginated results.
    @Test
    fun nonNumericLimitIsRejectedWithClearError() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("limit", "twenty") })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must be an integer"), text.take(200))
    }

    @Test
    fun nonNumericOffsetIsRejectedWithClearError() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("offset", "abc") })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("offset must be an integer"), text.take(200))
    }

    @Test
    fun outOfIntRangeLimitIsRejectedWithClearError() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("limit", 99_999_999_999L) })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit is out of range"), text.take(200))
    }

    @Test
    fun absurdlyLargeLimitIsRejectedWithClearError() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("limit", 1_000_000) })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must not exceed"), text.take(200))
    }

    // DX audit 2026-09-04 (P6): rell_security_check / rell_check with `source` =
    // the stablecoin TEST module alone filed it as main.rell, so `import main;`
    // resolved to itself and 20 "Unknown name: 'main.x'" errors described a file
    // that was never sent. The test module must be placed as a test module and
    // the note must name the module it imports and how to pass it.
    @Test
    fun testModuleAsSingleSourceIsPlacedAsATestModuleAndTheMissingImportIsNamed() {
        val test = DappScaffold.files("peg", template = "stablecoin").getValue("src/test/main_test.rell")
        for (tool in listOf("rell_check", "rell_security_check")) {
            val result = callViaExecutor(tool, buildJsonObject { put("source", test) })
            val text = (result.content.first() as TextContent).text!!
            assertFalse(text.contains("Unknown name: 'main."), "$tool: the test file must not be compiled AS main: ${text.take(400)}")
            assertTrue(text.contains("`source` is a @test module, placed at test/main_test.rell."), "$tool: ${text.take(600)}")
            assertTrue(text.contains("It imports main - not submitted"), "$tool: ${text.take(600)}")
            assertTrue(text.contains("Pass `files` with the app module(s) AND the test file"), "$tool: ${text.take(600)}")
        }
        // An app module as `source` is unchanged: main.rell, no note.
        val ok = callViaExecutor("rell_check", buildJsonObject { put("source", "module;\nquery ping() = \"pong\";") })
        val okText = (ok.content.first() as TextContent).text!!
        assertTrue(okText.contains("\"ok\":true"), okText)
        assertFalse(okText.contains("@test module, placed"), okText)
        // A self-contained test module compiles alone and says so only on failure.
        val alone = callViaExecutor("rell_check", buildJsonObject { put("source", "@test module;\nfunction test_x() { assert_true(true); }") })
        val aloneText = (alone.content.first() as TextContent).text!!
        assertTrue(aloneText.contains("\"ok\":true"), aloneText)
    }

    // DX audit 2026-09-04 round 2 (Q3): chromia.yml `module: app` with main.rell
    // submitted came back ok=true - the gate compiled the file it had and never
    // noticed the chain's root module was not among them; `chr build` fails.
    @Test
    fun projectGateFailsWhenTheYamlModuleWasNotSubmitted() {
        val yml = DappScaffold.files("peg", template = "hello").getValue("chromia.yml")
        val main = DappScaffold.files("peg", template = "hello").getValue("src/main.rell")
        val wrong = CheckDappProject.check(yml.replace("module: main", "module: app"), mapOf("src/main.rell" to main))
        assertFalse(wrong.ok, "a chain whose root module was never sent must not pass: ${wrong.errors}")
        val error = wrong.errors.single { it.startsWith("chromia.yml: blockchains.peg.module 'app'") }
        assertTrue(error.contains("(submitted modules: main)"), error)
        assertTrue(error.contains("`chr build` fails with \"Module 'app' not found\""), error)
        // Directory modules and submodules count as present; lib.* is vendored.
        assertTrue(CheckDappProject.declaredModulesNotSubmitted("blockchains:\n  a:\n    module: app\n", mapOf("app/module.rell" to "module;\n")).isEmpty())
        assertTrue(CheckDappProject.declaredModulesNotSubmitted("blockchains:\n  a:\n    module: app\n", mapOf("app/core.rell" to "module;\n")).isEmpty())
        assertTrue(CheckDappProject.declaredModulesNotSubmitted("blockchains:\n  a:\n    module: lib.ft4\n", mapOf("main.rell" to "module;\n")).isEmpty())
        assertEquals(
            listOf(Triple("a", "app", "main")),
            CheckDappProject.declaredModulesNotSubmitted("blockchains:\n  a:\n    module: app\n", mapOf("src/main.rell" to "module;\n"))
        )
        // The matching yaml still passes.
        assertTrue(CheckDappProject.check(yml, mapOf("src/main.rell" to main)).ok)
    }

    // Live round 8 (2026-09-04): the stablecoin scaffold passed the deploy dry
    // run ("ready") and the real `chr deployment create` died with "Missing
    // module_args for module(s): main" - its yml leaves main.oracle_pubkey
    // deliberately unset and no gate checked that the chain configures every
    // module_args its modules require. Both gates check it now, and the compile
    // result names what is required.
    @Test
    fun unconfiguredModuleArgsAreABlockerBeforeChrSeesThem() = runBlocking {
        val files = DappScaffold.files("peg", template = "stablecoin")
        val rell = files.filterKeys { it.endsWith(".rell") }
        val yml = files.getValue("chromia.yml")

        val compiled = RellCheck.check(rell, null)
        assertTrue(compiled.ok, compiled.notes)
        assertEquals(mapOf("main" to listOf("oracle_pubkey")), compiled.requiredModuleArgs,
            "only the CHAIN's modules count - lib.ft4.core.admin is pulled in by the @test modules alone")
        assertEquals(emptyMap<String, List<String>>(), RellCheck.check(DappScaffold.files("h", template = "hello").filterKeys { it.endsWith(".rell") }, null).requiredModuleArgs)

        val gate = CheckDappProject.check(yml, rell)
        assertFalse(gate.ok, "the scaffold yml configures no main.oracle_pubkey: ${gate.errors}")
        val error = gate.errors.single { it.contains("blockchains.peg.moduleArgs has no `main` entry") }
        assertTrue(error.contains("no default for oracle_pubkey"), error)
        assertTrue(error.contains("\"Missing module_args for module(s): main\""), error)
        assertTrue(error.contains("  main:\n    oracle_pubkey: <value>"), error)
        assertTrue(error.contains("never the test key from test.moduleArgs"), error)

        // Configured (the commented lines in the scaffold, uncommented with a real key): passes.
        val configured = yml.replace("      # main:\n      #   oracle_pubkey: x\"<your oracle public key>\"", "      main:\n        oracle_pubkey: x\"${DappScaffold.TEST_ADMIN_PUBKEY}\"")
        assertTrue(configured != yml, "the scaffold's commented placeholder must be where this test expects it")
        val okGate = CheckDappProject.check(configured, rell)
        assertTrue(okGate.errors.none { it.contains("moduleArgs has no") }, okGate.errors.toString())

        // The deploy preflight blocks on it too (it is what the dry run consults).
        val preflight = org.chromia.tools.DeploymentPreflight.run(
            yml.trimEnd() + "\n\n" + WriteDeploymentConfig.deploymentsYaml(WriteDeploymentConfig.resolveNetwork("testnet")!!, "peg").replace("<containerIID>", "c1"),
            "testnet", rell, null
        ) { _, _ -> org.chromia.domain.NetworkResult.Success(1L) }
        assertFalse(preflight.ready, preflight.toJson().toString())
        val blocker = preflight.findings.single { it.check == "module_args" }
        assertTrue(blocker.message.contains("has no `main` entry"), blocker.message)
        assertTrue(blocker.fix.startsWith("Add under blockchains.peg.moduleArgs:"), blocker.fix)
        val readyPreflight = org.chromia.tools.DeploymentPreflight.run(
            configured.trimEnd() + "\n\n" + WriteDeploymentConfig.deploymentsYaml(WriteDeploymentConfig.resolveNetwork("testnet")!!, "peg").replace("<containerIID>", "c1"),
            "testnet", rell, null
        ) { _, _ -> org.chromia.domain.NetworkResult.Success(1L) }
        assertTrue(readyPreflight.findings.none { it.check == "module_args" }, readyPreflight.toJson().toString())

        // Another chain in the same yml whose module is not this one is not judged.
        assertTrue(
            CheckDappProject.moduleArgsNotConfigured("blockchains:\n  other:\n    module: app\n", mapOf("main" to listOf("k")), listOf("main")).isEmpty()
        )

        // PARTIAL entry (round 9, lending through the real chr): main.oracle_pubkey
        // set, treasury_pubkey still a commented placeholder -> "Bad module_args
        // for module 'main': Missing struct attribute value: 'main:module_args.
        // treasury_pubkey'". Only fields WITHOUT a default count.
        val lending = DappScaffold.files("pool", template = "lending")
        val lendingRell = lending.filterKeys { it.endsWith(".rell") }
        val lendingYml = lending.getValue("chromia.yml")
        assertEquals(mapOf("main" to listOf("oracle_pubkey", "treasury_pubkey")), RellCheck.check(lendingRell, null).requiredModuleArgs)
        val half = lendingYml.replace("      # main:\n      #   oracle_pubkey: x\"<your oracle public key>\"", "      main:\n        oracle_pubkey: x\"${DappScaffold.TEST_ADMIN_PUBKEY}\"")
        assertTrue(half != lendingYml, "the lending scaffold's commented placeholder must be where this test expects it")
        assertTrue(half.contains("      #   treasury_pubkey: x\"<your protocol fee key>\""), half)
        val partial = CheckDappProject.check(half, lendingRell)
        val partialError = partial.errors.single { it.contains("moduleArgs.main is missing treasury_pubkey") }
        assertTrue(partialError.contains("\"Bad module_args for module 'main': Missing struct attribute value: 'main:module_args.treasury_pubkey'\""), partialError)
        assertTrue(partialError.contains("  main:\n    treasury_pubkey: <value>"), partialError)
        assertFalse(partialError.contains("oracle_pubkey: <value>"), "only the missing field is asked for: $partialError")
        val whole = half.replace("      #   treasury_pubkey: x\"<your protocol fee key>\"", "        treasury_pubkey: x\"03c1f231e767f93212f2e474ac3145ae50923bddce53e069548d4d11b851be4378\"")
        assertTrue(CheckDappProject.check(whole, lendingRell).errors.none { it.contains("moduleArgs") }, CheckDappProject.check(whole, lendingRell).errors.toString())
        assertEquals(
            listOf(Triple("c", "main", listOf("b"))),
            CheckDappProject.moduleArgsNotConfigured("blockchains:\n  c:\n    module: main\n    moduleArgs:\n      main:\n        a: 1\n", mapOf("main" to listOf("a", "b")), listOf("main"))
        )
        // A struct field WITH a default is never demanded.
        val defaulted = RellCheck.check(mapOf("main.rell" to "module;\nstruct module_args { admin: byte_array; max_items: integer = 10; }\nquery q() = chain_context.args.max_items;\n"), null)
        assertTrue(defaulted.ok, defaulted.errors.toString())
        assertEquals(mapOf("main" to listOf("admin")), defaulted.requiredModuleArgs)
    }

    // Live stablecoin chain 2922E3E2... (2026-09-04): `get_cdp` with `account`
    // instead of `owner` came back "Query 'get_cdp' failed: Invalid argument(s):
    // account" - the node names the wrong name and never the right one, although
    // it publishes the signature through rell.get_app_structure.
    @Test
    fun aRefusedQueryIsAnsweredWithItsRealSignature() = runBlocking {
        // Shape as the node returns it (captured from the live chain).
        val structure = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"modules":{"lib.ft4":{"name":"lib.ft4","functions":{}},"main":{"name":"main","queries":{
              "get_cdp":{"mount":"get_cdp","parameters":[{"name":"owner","type":"byte_array"}],"type":{"type":"nullable","value":{"type":"tuple","fields":[]}}},
              "get_system":{"mount":"get_system","parameters":[],"type":{"type":"tuple","fields":[]}},
              "get_tokens":{"mount":"get_tokens","parameters":[{"name":"owner","type":"byte_array"}],"type":"integer"},
              "paged":{"mount":"acct.paged","parameters":[{"name":"page_size","type":{"type":"nullable","value":"integer"}},{"name":"ids","type":{"type":"list","value":"byte_array"}}],"type":"integer"}
            }}}}"""
        ).jsonObject
        val wrongName = org.chromia.tools.QuerySignatureHint.hint(structure, "get_cdp", setOf("account"))!!
        assertTrue(wrongName.startsWith("The chain's `get_cdp` takes (owner: byte_array) (from rell.get_app_structure) - not a parameter: account; missing: owner."), wrongName)
        assertEquals("The chain's `get_system` takes no arguments (from rell.get_app_structure) - not a parameter: x. Argument names must match exactly; byte_array values are hex strings.",
            org.chromia.tools.QuerySignatureHint.hint(structure, "get_system", setOf("x")))
        val typed = org.chromia.tools.QuerySignatureHint.hint(structure, "acct.paged", emptySet())!!
        assertTrue(typed.contains("(page_size: integer?, ids: list<byte_array>)"), typed)
        val unknown = org.chromia.tools.QuerySignatureHint.hint(structure, "get_cdps", emptySet())!!
        assertTrue(unknown.startsWith("No query is mounted as `get_cdps` on this chain. Did you mean `get_cdp`"), unknown)
        assertTrue(unknown.contains("Mounted queries (4): get_cdp, "), unknown)
        assertTrue(unknown.contains("acct.paged"), "mounted names, not Rell names: $unknown")
        assertEquals(null, org.chromia.tools.QuerySignatureHint.hint(kotlinx.serialization.json.buildJsonObject { }, "q", emptySet()))
        assertTrue(org.chromia.tools.QuerySignatureHint.applies("query: 400 Bad Request  Query 'get_cdp' failed: Invalid argument(s): account from https://node8"))
        assertFalse(org.chromia.tools.QuerySignatureHint.applies("Connection refused"))

        // Through the tool: the node's refusal stays first, the signature follows, one extra read.
        val repo = RecordingRepository()
        repo.next = org.chromia.domain.NetworkResult.Error("Postchain client error for blockchain X: query: 400 Bad Request  Query 'get_cdp' failed: Invalid argument(s): account from https://node8")
        repo.dappAnswers["rell.get_app_structure"] = org.chromia.domain.NetworkResult.Success(structure)
        val result = org.chromia.tools.DappInteractionStrategy().execute(
            callToolRequest(name = "chromia_dapp_query", arguments = buildJsonObject {
                put("blockchainRid", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                put("query", "get_cdp")
                put("arguments", buildJsonObject { put("account", "abcd") })
            }),
            repo
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.startsWith("Failed to execute dapp query get_cdp --> {account=abcd}: Postchain client error"), text)
        assertTrue(text.contains("Invalid argument(s): account from https://node8. The chain's `get_cdp` takes (owner: byte_array)"), text)
        assertEquals(listOf("get_cdp", "rell.get_app_structure"), repo.dappCalls.map { it.query })

        // A refusal of another kind is passed through untouched - no structure read.
        val other = RecordingRepository()
        other.next = org.chromia.domain.NetworkResult.Error("Connection refused")
        org.chromia.tools.DappInteractionStrategy().execute(
            callToolRequest(name = "chromia_dapp_query", arguments = buildJsonObject {
                put("blockchainRid", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                put("query", "get_cdp")
            }),
            other
        )
        assertEquals(listOf("get_cdp"), other.dappCalls.map { it.query })
    }

    // DX audit 2026-09-04 round 2 (Q2): a tab in the indentation surfaced as
    // "bad indent at line 4" - one line after the tab, with no word about tabs.
    @Test
    fun tabIndentationIsNamedOnItsOwnLine() {
        val yml = "blockchains:\n  peg:\n\tmodule: main\n    config:\n      features:\n        merkle_hash_version: 2\n"
        val result = ChromiaYmlValidator.validate(yml)
        assertFalse(result.ok)
        assertEquals("YAML parse error: tab character in the indentation of line 3 - YAML indentation must be spaces only (chr rejects tabs as well); replace the tab(s) with spaces", result.errors.single())
    }

    // DX audit 2026-09-04 round 2 (Q4): yaml and rell swapped read as two broken files.
    @Test
    fun swappedYamlAndRellArgumentsAreNamedAsSwapped() {
        val yml = DappScaffold.files("peg", template = "hello").getValue("chromia.yml")
        val main = DappScaffold.files("peg", template = "hello").getValue("src/main.rell")
        val result = callViaExecutor("check_dapp_project", buildJsonObject { put("yaml", main); put("rell", yml) })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("the arguments look swapped"), text)
        assertTrue(text.contains("`yaml` starts with a Rell module header (`module;`)"), text)
        assertTrue(text.contains("`rell` has a chromia.yml root key (`blockchains:`)"), text)
    }

    // DX audit 2026-09-04 round 2 (Q6): write_deployment_config's invalid-name
    // error quoted the regex and nothing else.
    @Test
    fun deploymentConfigInvalidNameSuggestsTheValidOne() {
        val result = callViaExecutor("write_deployment_config", buildJsonObject { put("network", "testnet"); put("name", "My-Peg") })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Did you mean name=\"my_peg\"?"), text)
    }

    // DX audit 2026-09-04 (P9): `require(true, "oops);` is reported as a syntax
    // error at the `(` - true for the parser, useless for the agent. The note
    // must say what the line most likely is.
    @Test
    fun unterminatedStringLiteralIsNamedInTheNotes() {
        val result = RellCheck.check(mapOf("main.rell" to "module;\noperation x() {\n    require(true, \"oops);\n}\n"), null)
        assertFalse(result.ok)
        assertEquals(3, result.errors.first().line)
        assertTrue(result.notes.contains("Line 3 of main.rell has an odd number of double quotes - most likely an unterminated string literal"), result.notes)
        // A genuine syntax error on a line with balanced quotes gets no such guess.
        val plain = RellCheck.check(mapOf("main.rell" to "module;\noperation x() {\n    require(true, \"oops\")\n}\n"), null)
        assertFalse(plain.ok)
        assertFalse(plain.notes.contains("unterminated string literal"), plain.notes)
    }
}
