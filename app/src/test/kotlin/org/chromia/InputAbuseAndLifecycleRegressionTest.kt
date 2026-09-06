package org.chromia

import org.chromia.tools.propertiesOrEmpty

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.chromia.tools.LocalChain
import org.chromia.tools.PromptManager
import org.chromia.tools.RellCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * QA round on the input-abuse and resource-lifecycle lenses (2026-09-02).
 * Every test here failed on the code before its fix; each pins a case where a
 * tool answered ok:true (or a success shape) while silently doing less than
 * the caller asked.
 */
class InputAbuseAndLifecycleRegressionTest {

    private val executor = ToolExecutor(
        RecordingRepository(),
        PromptManager(),
        ragStoreFactory = { McpTestSupport.fixtureRagStore() }
    )

    private fun call(tool: String, args: JsonObject): CallToolResult = runBlocking {
        executor.executeTool(callToolRequest(name = tool, arguments = args))
    }

    private fun text(result: CallToolResult): String = (result.content.first() as TextContent).text.orEmpty()

    @AfterEach
    fun tearDown() {
        LocalChain.stopAll()
        LocalChain.starterOverrideForTests = null
    }

    // ---- Lens 1: undeclared arguments were silently ignored --------------------

    @Test
    fun undeclaredArgumentsAreRejectedNamingTheDeclaredOnes() {
        // run_rell_tests' own description says "module_args"; sending that
        // spelling ran the tests WITHOUT the args and reported ok:true.
        val snake = call(
            "run_rell_tests",
            buildJsonObject {
                put("files", buildJsonObject { put("t.rell", "@test module;\nfunction test_x() { assert_equals(1, 1); }") })
                put("module_args", buildJsonObject { put("lib.ft4.core.accounts", buildJsonObject { put("x", 1) }) })
            }
        )
        assertEquals(true, snake.isError, text(snake))
        assertTrue(text(snake).contains("`module_args` (did you mean `moduleArgs`?)"), text(snake))
        assertTrue(text(snake).contains("declared arguments: files, moduleArgs"), text(snake))

        val ttl = call(
            "local_chain_up",
            buildJsonObject {
                put("action", "status")
                put("ttl", 5)
            }
        )
        assertEquals(true, ttl.isError, text(ttl))
        assertTrue(text(ttl).contains("`ttl` (did you mean `ttlSeconds`?)"), text(ttl))

        val iccf = call("ft4_module_args", buildJsonObject { put("include_iccf", true) })
        assertEquals(true, iccf.isError, text(iccf))
        assertTrue(text(iccf).contains("did you mean `includeIccf`"), text(iccf))

        val scaffold = call("scaffold_dapp", buildJsonObject { put("dapp_name", "notes") })
        assertEquals(true, scaffold.isError, text(scaffold))
        assertTrue(text(scaffold).contains("`dapp_name` (did you mean `name`?)"), text(scaffold))

        val help = call("chr_build_help", buildJsonObject { put("topic", "x") })
        assertEquals(true, help.isError, text(help))
        assertTrue(text(help).contains("this tool declares no arguments"), text(help))
    }

    @Test
    fun argumentNamesCarriedFromASiblingToolAreMappedToTheDeclaredOne() {
        // The tools are not uniform: verify_deployment says `brid`, the explorer
        // tools `rid`, chromia_dapp_query `blockchainRid` + `arguments`. An agent
        // reusing the previous call's names got the declared list and no hint
        // (DX audit 2026-09-04, five probes in a row).
        val q = call(
            "chromia_dapp_query",
            buildJsonObject {
                put("brid", "A".repeat(64)); put("query", "rell.get_app_structure")
                put("args", buildJsonObject { }); put("url", "https://node0.testnet.chromia.com:7740")
            }
        )
        assertEquals(true, q.isError, text(q))
        // AUDIT F10: "chain identifier ... the canonical name is the one the
        // majority already used - `brid`". chromia_dapp_query now DECLARES
        // `brid`, so it is no longer an unknown name to be guessed at; the
        // guess would have been a rename, and the mapping never renames a
        // caller's argument. `blockchainRid` survives as the declared alias.
        assertFalse(text(q).contains("`brid` (did you mean"), text(q))
        assertTrue(text(q).contains("declared arguments: arguments, blockchainRid, brid"), text(q))
        assertTrue(text(q).contains("`args` (did you mean `arguments`?)"), text(q))
        assertTrue(text(q).contains("`url` (did you mean `network`?)"), text(q))

        val v = call("verify_deployment", buildJsonObject { put("blockchainRid", "A".repeat(64)); put("network", "testnet") })
        assertEquals(true, v.isError, text(v))
        assertTrue(text(v).contains("`blockchainRid` (did you mean `brid`?)"), text(v))

        val accepted = setOf("blockchainRid", "network", "query", "arguments")
        assertEquals("blockchainRid", executor.suggestArgument("rid", accepted))
        assertEquals("blockchainRid", executor.suggestArgument("blockchain_rid", accepted))
        assertEquals("arguments", executor.suggestArgument("params", accepted))
        assertEquals("network", executor.suggestArgument("nodeUrl", accepted))
        assertNull(executor.suggestArgument("signer", accepted), "an unrelated name gets no guess")
        assertEquals("rell", executor.suggestArgument("source", setOf("yaml", "rell", "files")))
    }

    @Test
    fun networkNamesAndGoalsAreCaseInsensitive() {
        // verify_deployment network="Testnet" answered live:false with the real
        // cause ("Network 'Testnet' not found") one hop down inside a node error
        // (DX audit 2026-09-04).
        val service = org.chromia.data.client.PostchainClientService(org.chromia.data.config.ChromiaConfig())
        assertEquals(service.resolveUrls("testnet"), service.resolveUrls("Testnet"))
        assertEquals(service.resolveUrls("mainnet"), service.resolveUrls(" MAINNET "))
        assertEquals(listOf("https://node.example:7740"), service.resolveUrls("https://node.example:7740/"))
        val unknown = assertThrows(org.chromia.domain.exceptions.NetworkConfigurationException::class.java) { service.resolveUrls("tesnet") }
        assertTrue(unknown.message!!.contains("Available networks: mainnet, testnet, devnet1, devnet2"), unknown.message)
        assertTrue(unknown.message!!.contains("or pass a node URL (https://host:7740)"), unknown.message)

        val plan = call(
            "onboarding_next_step",
            buildJsonObject { put("hasProject", true); put("compiles", true); put("securityClean", true); put("testsPass", true); put("goal", "Testnet") }
        )
        assertTrue(plan.isError != true, text(plan))
        assertFalse(text(plan).contains("goal must be one of"), text(plan))
        val typo = call("onboarding_next_step", buildJsonObject { put("goal", "tesnet") })
        assertEquals(true, typo.isError, text(typo))
        assertTrue(text(typo).contains("goal must be one of local|testnet|mainnet (got \"tesnet\")"), text(typo))
    }

    @Test
    fun unknownPromptCategoryIsNamedWithTheValidOnes() {
        val unknown = call("get_prompts", buildJsonObject { put("category", "stablecoins") })
        assertTrue(unknown.isError != true, text(unknown))
        val notes = unknown.structuredContent!!.getValue("notes").jsonPrimitive.content
        assertTrue(notes.startsWith("No prompt category named 'stablecoins'. Valid categories: "), notes)
        assertTrue(notes.contains("dapp_build") && notes.contains("chromia_stack"), notes)
        assertTrue(notes.contains("Use `search` to match prompt text across every category."), notes)

        val near = call("get_prompts", buildJsonObject { put("category", "build") })
        val nearNotes = near.structuredContent!!.getValue("notes").jsonPrimitive.content
        assertTrue(nearNotes.contains("Did you mean 'dapp_build'?"), nearNotes)

        // Case is not a different category, and a real category carries no note.
        val cased = call("get_prompts", buildJsonObject { put("category", "Dapp_Build") })
        assertTrue(cased.isError != true, text(cased))
        assertNull(cased.structuredContent!!["notes"], text(cased))
        assertTrue(cased.structuredContent!!.getValue("prompts").jsonObject.containsKey("dapp_build"), text(cased))
    }

    @Test
    fun deliberateAliasesAndDeclaredArgumentsStillPass() {
        // `chain` is an undeclared alias write_deployment_config honours.
        val alias = call("write_deployment_config", buildJsonObject { put("network", "testnet"); put("chain", "notes") })
        assertTrue(alias.isError != true, text(alias))
        assertTrue(alias.structuredContent!!.getValue("name").jsonPrimitive.content == "notes", text(alias))

        // `files` is a DECLARED alias for check_dapp_project's `rell`.
        val files = call(
            "check_dapp_project",
            buildJsonObject { put("files", buildJsonObject { put("main.rell", "module; function f() = 1;") }) }
        )
        assertTrue(files.isError != true, text(files))
        assertFalse(text(files).contains("Unknown argument"), text(files))

        // Every declared argument of every tool passes the check by construction.
        org.chromia.tools.McpTools.allTools().forEach { tool ->
            assertNull(executor.unknownArgumentsError(tool.name, tool.inputSchema.propertiesOrEmpty.keys), tool.name)
        }
    }

    // ---- Lens 1: an all-blank submission was a green gate on no code -----------

    @Test
    fun allBlankSubmissionsAreRefusedByEveryGate() {
        val rellCheck = call("rell_check", buildJsonObject { put("source", "") })
        assertEquals(true, rellCheck.isError, text(rellCheck))
        assertTrue(text(rellCheck).contains("Every submitted Rell file is blank (main.rell)"), text(rellCheck))

        val security = call(
            "rell_security_check",
            buildJsonObject { put("files", buildJsonObject { put("a.rell", " \n"); put("b.rell", "﻿") }) }
        )
        assertEquals(true, security.isError, text(security))
        assertTrue(text(security).contains("blank (a.rell, b.rell)"), text(security))

        val project = call("check_dapp_project", buildJsonObject { put("rell", "   ") })
        assertTrue(project.isError != true, text(project))
        val projectJson = project.structuredContent!!
        assertEquals("false", projectJson.getValue("ok").jsonPrimitive.content, text(project))
        assertTrue(
            projectJson.getValue("errors").jsonArray.any { it.jsonPrimitive.content.contains("Every submitted Rell file is blank") },
            text(project)
        )

        val imports = call("check_ft4_imports", buildJsonObject { put("rell", "") })
        assertEquals(true, imports.isError, text(imports))
        assertTrue(text(imports).contains("Every submitted Rell file is blank"), text(imports))

        val tests = call("run_rell_tests", buildJsonObject { put("files", buildJsonObject { put("t.rell", "") }) })
        assertEquals(true, tests.isError, text(tests))
        assertTrue(text(tests).contains("Every submitted Rell file is blank"), text(tests))

        assertThrows(IllegalArgumentException::class.java) {
            LocalChain.prepare(mapOf("main.rell" to ""), "jdbc:postgresql://localhost:1/x", emptyMap(), null)
        }
    }

    @Test
    fun aBlankDirectoryModuleMarkerBesideRealCodeStillCompiles() {
        // Only ALL-blank submissions are refused: an empty module.rell is legal Rell.
        val result = RellCheck.check(
            mapOf("main.rell" to "module; function f() = 1;", "util/module.rell" to ""),
            null
        )
        assertTrue(result.ok, result.errors.toString())
    }

    // ---- Lens 1: moduleArgs under a wrong module name were silently dropped ----

    private val logicTest = buildJsonObject {
        put("math_test.rell", "@test module;\nfunction test_math() { assert_equals(2 + 2, 4); }")
    }

    private fun runTests(files: JsonObject, moduleArgs: JsonObject): CallToolResult =
        call("run_rell_tests", buildJsonObject { put("files", files); put("moduleArgs", moduleArgs) })

    @Test
    fun moduleArgsMustNameAModuleThatDeclaresModuleArgs() {
        val unknown = runTests(logicTest, buildJsonObject { put("nope", buildJsonObject { put("x", 1) }) })
        assertEquals(true, unknown.isError, text(unknown))
        assertTrue(text(unknown).contains("moduleArgs names module 'nope', but no such module"), text(unknown))

        val path = runTests(logicTest, buildJsonObject { put("math_test.rell", buildJsonObject { put("x", 1) }) })
        assertEquals(true, path.isError, text(path))
        assertTrue(text(path).contains("looks like a file path"), text(path))

        val empty = runTests(logicTest, buildJsonObject { put("", buildJsonObject { put("x", 1) }) })
        assertEquals(true, empty.isError, text(empty))
        assertTrue(text(empty).contains("is not a valid Rell module name"), text(empty))

        val undeclared = runTests(logicTest, buildJsonObject { put("math_test", buildJsonObject { put("x", 1) }) })
        assertEquals(true, undeclared.isError, text(undeclared))
        assertTrue(text(undeclared).contains("declares no `struct module_args`"), text(undeclared))

        // Traversal-shaped keys never touch the file system.
        val traversal = runTests(logicTest, buildJsonObject { put("../x", buildJsonObject { put("x", 1) }) })
        assertEquals(true, traversal.isError, text(traversal))
        assertTrue(text(traversal).contains("looks like a file path"), text(traversal))
        val dots = runTests(logicTest, buildJsonObject { put("..", buildJsonObject { put("x", 1) }) })
        assertEquals(true, dots.isError, text(dots))
        assertTrue(text(dots).contains("is not a valid Rell module name"), text(dots))
    }

    @Test
    fun moduleArgsForRealModulesStillReachTheTests() {
        // A user module (file form) declaring module_args.
        val userModule = runTests(
            buildJsonObject {
                put("cfg.rell", "module;\nstruct module_args { greeting: text; }\nfunction greet() = chain_context.args.greeting;")
                put("cfg_test.rell", "@test module;\nimport cfg;\nfunction test_greet() { assert_equals(cfg.greet(), \"hi\"); }")
            },
            buildJsonObject { put("cfg", buildJsonObject { put("greeting", "hi") }) }
        )
        assertTrue(userModule.isError != true, text(userModule))
        assertEquals("true", userModule.structuredContent!!.getValue("ok").jsonPrimitive.content, text(userModule))

        // A directory module whose header-less file declares module_args resolves
        // at the resolver level. (The QA round's "module_args + directory test
        // module fails to compile" suspicion was a syntax error masked by the
        // compiler - `limit` is a keyword; see ModuleArgsLayoutMatrixTest.)
        val dir = java.nio.file.Files.createTempDirectory("module-args-resolve")
        try {
            java.nio.file.Files.createDirectories(dir.resolve("app"))
            java.nio.file.Files.writeString(dir.resolve("app/module.rell"), "module;")
            java.nio.file.Files.writeString(dir.resolve("app/config.rell"), "// struct module_args in a comment does not count\nstruct module_args { limit: integer; }")
            java.nio.file.Files.writeString(dir.resolve("app/logic.rell"), "function limit() = chain_context.args.limit;")
            java.nio.file.Files.createDirectories(dir.resolve("app/sub"))
            java.nio.file.Files.writeString(dir.resolve("app/sub/x.rell"), "module; // no module_args here")
            RunRellTests.requireModuleArgsResolve(dir, listOf("app"))
            val sub = assertThrows(IllegalArgumentException::class.java) {
                RunRellTests.requireModuleArgsResolve(dir, listOf("app.sub.x"))
            }
            assertTrue(sub.message!!.contains("declares no `struct module_args`"), sub.message)
        } finally {
            dir.toFile().deleteRecursively()
        }

        // The vendored FT4 tree resolves too: the scaffold's full test set names
        // lib.ft4, lib.ft4.core.accounts, lib.ft4.core.admin and
        // lib.ft4.test.core.auth (a FILE module) - none may be refused.
        val ft4 = DappScaffold.files("notes", template = "ft4")
            .filterKeys { it.endsWith(".rell") }
            .mapKeys { (path, _) -> path.removePrefix("src/") }
        val result = RunRellTests.run(ft4, databaseUrl = null, moduleArgs = DappScaffold.ft4TestModuleArgs())
        assertEquals(3, result.total, result.notes)
        assertFalse(result.notes.contains("moduleArgs names module"), result.notes)
    }

    @Test
    fun localChainPrepareRefusesModuleArgsForUnknownModules() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            LocalChain.prepare(
                mapOf("main.rell" to "module; entity item { name; }"),
                "jdbc:postgresql://localhost:1/x",
                mapOf("lib.ft4.accounts" to mapOf("x" to JsonPrimitive(1))),
                null
            )
        }
        assertTrue(e.message!!.contains("moduleArgs names module 'lib.ft4.accounts', but no such module"), e.message)
    }

    // ---- Lens 2: a failed restart silently took the previous chain down --------

    @Test
    fun failedRestartSaysThePreviousChainIsGone() {
        var calls = 0
        LocalChain.starterOverrideForTests = { plan ->
            calls++
            if (calls > 1) throw IllegalStateException("simulated node start failure")
            LocalChain.Running(
                node = null,
                brid = plan.brid,
                apiPort = plan.apiPort,
                fingerprint = plan.fingerprint,
                nodePubkey = plan.pubKeyHex,
                expiresAtMillis = Long.MAX_VALUE,
                ttlTask = null
            )
        }
        val dbUrl = "jdbc:postgresql://localhost:5432/db?user=u&password=p"
        val first = LocalChain.up(mapOf("main.rell" to "module; entity item { name; }"), databaseUrl = dbUrl)
        assertTrue(first.ok, first.notes)

        val second = LocalChain.up(mapOf("main.rell" to "module; entity other { name; }"), databaseUrl = dbUrl)
        assertFalse(second.ok, second.notes)
        assertTrue(second.notes.contains("simulated node start failure"), second.notes)
        assertTrue(second.notes.contains("previously running chain (${first.brid}) was stopped"), second.notes)
        assertEquals("not_running", LocalChain.status().status)
    }

    // ---- Lens 3: the freshly merged FT4 module_args error path -----------------

    @Test
    fun ft4TestsWithoutTheAdminArgsGetTheModuleArgsHint() {
        val dbUrl = System.getenv(RunRellTests.DATABASE_URL_ENV)
        org.junit.jupiter.api.Assumptions.assumeTrue(!dbUrl.isNullOrBlank(), "needs ${RunRellTests.DATABASE_URL_ENV}")
        val rell = DappScaffold.files("notes", template = "ft4")
            .filterKeys { it.endsWith(".rell") }
            .mapKeys { (path, _) -> path.removePrefix("src/") }
        // The production args only - exactly what an agent that read
        // ft4_module_args but not the scaffold's test block would send.
        val productionOnly = DappScaffold.ft4TestModuleArgs()
            .filterKeys { it != "lib.ft4.core.admin" && it != "lib.ft4.test.core.auth" }
        val result = RunRellTests.run(rell, databaseUrl = dbUrl, moduleArgs = productionOnly)
        assertFalse(result.ok, result.notes)
        assertEquals(3, result.total, result.cases.toString())
        assertTrue(result.cases.all { it.error?.contains("Unable to create GTX module") == true }, result.cases.toString())
        assertTrue(result.notes.contains("lib.ft4.core.admin (admin_pubkey)"), result.notes)
        assertTrue(result.notes.contains("lib.ft4.test.core.auth (admin_priv_key)"), result.notes)
        // The note must NAME the modules that were not supplied, computed from
        // the compiled app - not describe them (DX audit 2026-09-04). Only
        // lib.ft4.core.admin blocks GTX module creation: lib.ft4.test.core.auth's
        // admin_priv_key defaults to null and fails later, at the `!!`, so the
        // compiler-derived list is exactly the one module.
        assertTrue(result.notes.contains("MISSING module_args for: lib.ft4.core.admin. Take the values"), result.notes)
    }

    /**
     * DX audit 2026-09-04: the stablecoin template's tests run with no moduleArgs
     * at all failed six times with the same opaque GTX error and a note that
     * pointed at template=ft4 - whose args do not include `main.oracle_pubkey`.
     * The compiler knows exactly which modules declare module_args without
     * defaults; the note must list them, and a complete set must not be blamed.
     */
    @Test
    fun missingModuleArgsAreNamedFromTheCompiledApp() {
        val dbUrl = System.getenv(RunRellTests.DATABASE_URL_ENV)
        org.junit.jupiter.api.Assumptions.assumeTrue(!dbUrl.isNullOrBlank(), "needs ${RunRellTests.DATABASE_URL_ENV}")
        val rell = DappScaffold.files("peg", template = "stablecoin")
            .filterKeys { it.endsWith(".rell") }
            .mapKeys { (path, _) -> path.removePrefix("src/") }
        val none = RunRellTests.run(rell, databaseUrl = dbUrl, moduleArgs = emptyMap())
        assertFalse(none.ok, none.notes)
        assertTrue(none.cases.all { it.error?.contains("Unable to create GTX module") == true }, none.cases.toString())
        val missing = none.notes.substringAfter("MISSING module_args for: ", "").substringBefore(". Take the values")
        assertTrue(missing.isNotEmpty(), "the note must list the unsupplied modules: ${none.notes}")
        val named = missing.split(", ")
        assertTrue("main" in named && "lib.ft4.core.admin" in named, "missing list: $missing")
        assertFalse("lib.ft4.test.core.auth" in named, "a module whose module_args all default is not what blocks GTX module creation: $missing")
        assertTrue(none.notes.contains("chromia.yml scaffold_dapp returned for THIS template"), none.notes)
        assertFalse(none.notes.contains("template=ft4 for a working set"), "must not point a stablecoin build at another template's keys: ${none.notes}")

        // Only main missing: the note narrows to it.
        val ft4Only = RunRellTests.run(rell, databaseUrl = dbUrl, moduleArgs = DappScaffold.ft4TestModuleArgs())
        assertFalse(ft4Only.ok, ft4Only.notes)
        assertTrue(ft4Only.notes.contains("MISSING module_args for: main."), ft4Only.notes)
        assertFalse(ft4Only.notes.contains("lib.ft4.core.admin, lib.ft4.test.core.auth, main"), ft4Only.notes)
    }

    /**
     * DX audit 2026-09-04: `rate_limit` filed under `lib.ft4` instead of
     * `lib.ft4.core.accounts` came back as "Rell test sources do not compile: Bad
     * module_args for module 'lib.ft4': Wrong key ... 'rate_limit'" - a binding
     * error headed as a compile error, with no word on where the key belongs.
     * The compiled app knows which module declares the field; say so.
     */
    @Test
    fun strayModuleArgsKeyIsRoutedToTheModuleThatDeclaresIt() {
        val rell = DappScaffold.files("notes", template = "ft4")
            .filterKeys { it.endsWith(".rell") }
            .mapKeys { (path, _) -> path.removePrefix("src/") }
        val misfiled = DappScaffold.ft4TestModuleArgs().toMutableMap()
        misfiled["lib.ft4"] = misfiled.getValue("lib.ft4") + ("rate_limit" to misfiled.getValue("lib.ft4.core.accounts").getValue("rate_limit"))
        val e = assertThrows(IllegalArgumentException::class.java) { RunRellTests.run(rell, databaseUrl = null, moduleArgs = misfiled) }
        val msg = e.message.orEmpty()
        assertTrue(msg.startsWith("module_args do not bind (the Rell sources compiled; this is the moduleArgs argument):"), msg)
        assertFalse(msg.contains("do not compile"), "a binding failure must not be headed as a compile failure: $msg")
        assertTrue(msg.contains("'rate_limit' is not a field of lib.ft4's module_args (its fields: query_max_page_size)"), msg)
        assertTrue(msg.contains("It is declared by lib.ft4.core.accounts - move it under that module name in moduleArgs."), msg)

        // The hint never guesses: a key nobody declares is said to be undeclared.
        val fields = mapOf("lib.ft4" to listOf("query_max_page_size"), "main" to listOf("oracle_pubkey"))
        val unknown = RunRellTests.moduleArgsKeyHint("main", "oracle_key", fields)
        assertTrue(unknown.contains("'oracle_key' is not a field of main's module_args (its fields: oracle_pubkey)"), unknown)
        assertTrue(unknown.contains("No compiled module declares a module_args field named 'oracle_key'"), unknown)
        val regex = RunRellTests.BAD_MODULE_ARGS_KEY_REGEX.find(
            "Bad module_args for module 'lib.ft4': Wrong key in Gtv dictionary for type 'lib.ft4:module_args': 'rate_limit'"
        )
        assertEquals(listOf("lib.ft4", "rate_limit"), regex?.destructured?.toList())
    }
}
