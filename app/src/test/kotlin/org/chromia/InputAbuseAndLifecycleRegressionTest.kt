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

}
