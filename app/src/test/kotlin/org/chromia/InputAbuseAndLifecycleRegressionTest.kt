package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
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
        executor.executeTool(CallToolRequest(name = tool, arguments = args))
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
            assertNull(executor.unknownArgumentsError(tool.name, tool.inputSchema.properties.keys), tool.name)
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
        // at the resolver level (the compiler's own handling of module_args in
        // directory layouts is out of scope here - see the QA report).
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

}
