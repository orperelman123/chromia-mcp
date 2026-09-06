package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.WriteDeploymentConfig
import org.chromia.tools.callToolRequest
import org.chromia.tools.propertiesOrEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ROUND 16, SECTION 6: the audit fixes as new surface. Three of the eight
 * fixes shipped a way to be silently wrong, and each is pinned here with the
 * exact input round 16 used.
 *
 *  (a) THE ALIAS CHAIN DISCARDS THE SECOND ALIAS. `args["files"] ?: args["rell"]
 *      ?: args["source"] ...` returns the first spelling present and drops the
 *      rest with no warning, on `rell_check`, `rell_security_check`,
 *      `run_rell_tests` AND `verify_guards`. Passing `files` with clean code and
 *      `source` with the tree actually being shipped returned ok:true about a
 *      tree the tool never saw. Two aliases with DIFFERENT content is now an
 *      error naming both; identical content stays silent, which is the whole
 *      point of having aliases.
 *  (b) `moduleArgs: {}` MEANT TWO THINGS. `moduleArgs.isEmpty() && yaml != null`
 *      made an explicit empty object the same call as sending none, so "run
 *      with no module args" ran with whatever the submitted yml said. The
 *      precedence is pinned: an explicit `moduleArgs`, empty included, is the
 *      whole set; the yml is read only when the argument is absent.
 *  (c) `write_deployment_config` APPENDED A SECOND `deployments:` KEY when the
 *      first one carried a comment, because the section was found with
 *      `it.trimEnd() == "deployments:"`. YAML takes the last key, so the
 *      caller's real section was orphaned and the tool reported success.
 */
class Round16ArgumentSurfaceTest {

    private val executor = ToolExecutor(RecordingRepository(), PromptManager())

    private fun call(name: String, args: JsonObject) = runBlocking {
        executor.executeTool(callToolRequest(name = name, arguments = args))
    }

    private fun errorText(name: String, args: JsonObject): String {
        val result = call(name, args)
        assertEquals(true, result.isError, "expected an error from $name, got ${result.structuredContent}")
        return (result.content.first() as TextContent).text!!
    }

    // The tree an agent says it is shipping, and the tree it is actually
    // shipping. The second one is the one that must never be analysed silently.
    private val cleanApp = """
        module;
        entity vault { key owner: byte_array; mutable balance: integer = 0; }
        operation deposit(amount: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require(amount > 0, "amount out of range");
            val v = require(vault @? { .owner == op_context.get_signers()[0] }, "no vault");
            update v ( .balance += amount );
        }
    """.trimIndent()

    private val shippedApp = """
        module;
        entity vault { key owner: byte_array; mutable balance: integer = 0; }
        operation drain(to: byte_array, amount: integer) {
            val v = require(vault @? { .owner == to }, "no vault");
            update v ( .balance += amount );
        }
    """.trimIndent()

    // =====================================================================
    // (a) two aliases, two trees
    // =====================================================================

    @Test
    fun `every code taking tool refuses two aliases with different content and names both`() {
        val cases = listOf(
            "rell_check" to ("files" to "source"),
            "rell_security_check" to ("files" to "rell"),
            "run_rell_tests" to ("files" to "source"),
            "verify_guards" to ("rell" to "source")
        )
        cases.forEach { (tool, keys) ->
            val (first, second) = keys
            val text = errorText(
                tool,
                buildJsonObject {
                    put(first, buildJsonObject { put("main.rell", cleanApp) })
                    put(second, buildJsonObject { put("main.rell", shippedApp) })
                }
            )
            assertTrue(text.contains("`$first`"), "$tool must name the first alias: $text")
            assertTrue(text.contains("`$second`"), "$tool must name the second alias: $text")
            assertTrue(
                text.contains("DIFFERENT content"),
                "$tool must say WHY it refused - identical content is fine: $text"
            )
        }
    }

    @Test
    fun `a single source string and a files map holding it are the same submission`() {
        // The harmless case aliases exist for: one tree, two spellings. It must
        // NOT be an error, and the tool must actually run.
        val result = call(
            "rell_check",
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", cleanApp) })
                put("source", cleanApp)
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        assertTrue(result.structuredContent!!.getValue("ok").jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `identical maps under two aliases are not a conflict`() {
        val result = call(
            "rell_security_check",
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", cleanApp) })
                put("rell", buildJsonObject { put("main.rell", cleanApp) })
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    @Test
    fun `the discarded alias is no longer analysed as if it were the whole submission`() {
        // Round 16's exact shape: clean code under the canonical name, the real
        // tree under an alias. The answer used to be ok:true about the first.
        val text = errorText(
            "rell_security_check",
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", cleanApp) })
                put("source", shippedApp)
            }
        )
        assertTrue(text.contains("silently discarded"), text)
        assertTrue(text.contains("Pass the sources once"), text)
    }

    @Test
    fun `deployment_preflight's comment no longer contradicts its own next line`() {
        val src = java.io.File("src/main/kotlin/org/chromia/tools/ToolExecutor.kt").readText()
        assertFalse(
            src.contains("`rell` wins when both are present"),
            "the comment said `rell` wins and the code reads `files` first - an agent that believed it " +
                "shipped the wrong tree past the deployment gate"
        )
        assertTrue(
            src.contains("canonical name (audit F10) and it is read first"),
            "the comment must state the real precedence"
        )
    }

    // =====================================================================
    // (b) moduleArgs precedence
    // =====================================================================

    /** A module that takes NO module args: giving it any is what breaks the run. */
    private val pureLogicFiles = buildJsonObject {
        put("lib.rell", "module;\nfunction double(x: integer): integer = x * 2;")
        put(
            "lib_test.rell",
            "@test module;\nimport lib;\nfunction test_double() { assert_equals(lib.double(2), 4); }"
        )
    }

    /** A module that DOES take module args, with a test that reports the value it got. */
    private fun configuredFiles(expected: Int) = buildJsonObject {
        put(
            "cfg.rell",
            "module;\nstruct module_args { scale: integer; }\n" +
                "function scale(): integer = chain_context.args.scale;"
        )
        put(
            "cfg_test.rell",
            "@test module;\nimport cfg;\nfunction test_scale() { assert_equals(cfg.scale(), $expected); }"
        )
    }

    private fun ymlScale(scale: Int) = """
        blockchains:
          demo:
            module: cfg
            moduleArgs:
              cfg:
                scale: $scale
    """.trimIndent()

    /** module args for a module the sources do not contain: reading it fails the call. */
    private val ymlForAModuleThatIsNotThere = """
        blockchains:
          demo:
            module: lib
            moduleArgs:
              not_a_module_in_this_submission:
                scale: 3
    """.trimIndent()

    @Test
    fun `an explicit empty moduleArgs is the whole set and the yaml is not read`() {
        val result = call(
            "run_rell_tests",
            buildJsonObject {
                put("files", pureLogicFiles)
                put("yaml", ymlForAModuleThatIsNotThere)
                put("moduleArgs", buildJsonObject { })
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(
            "moduleArgs argument", structured["moduleArgsSource"]?.jsonPrimitive?.content,
            "`moduleArgs: {}` means NO module args, not \"whatever the yml says\": $structured"
        )
        assertEquals(
            true, structured.getValue("ok").jsonPrimitive.content.toBoolean(),
            "the yml names a module these sources do not contain - if it had been read the call would " +
                "have failed, which is exactly what `moduleArgs: {}` used to do: $structured"
        )
        val notes = structured.getValue("notes").jsonPrimitive.content
        assertTrue(notes.contains("NO module args at all"), notes)
    }

    @Test
    fun `an explicit non empty moduleArgs also wins over the yaml and the answer says so`() {
        val result = call(
            "run_rell_tests",
            buildJsonObject {
                put("files", configuredFiles(expected = 7))
                put("yaml", ymlScale(3))
                put("moduleArgs", buildJsonObject { put("cfg", buildJsonObject { put("scale", 7) }) })
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals("moduleArgs argument", structured["moduleArgsSource"]?.jsonPrimitive?.content, structured.toString())
        assertEquals(
            true, structured.getValue("ok").jsonPrimitive.content.toBoolean(),
            "the test asserts the value the ARGUMENT carried, not the yml's: $structured"
        )
        assertTrue(
            structured.getValue("notes").jsonPrimitive.content.contains("nothing from the yml"),
            structured.getValue("notes").jsonPrimitive.content
        )
    }

    @Test
    fun `omitting moduleArgs entirely is what reads the yaml`() {
        val result = call(
            "run_rell_tests",
            buildJsonObject {
                put("files", configuredFiles(expected = 3))
                put("yaml", ymlScale(3))
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(
            "chromia.yml", structured["moduleArgsSource"]?.jsonPrimitive?.content,
            "with no `moduleArgs` argument the yml is the source: $structured"
        )
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
    }

    @Test
    fun `the schema states the precedence rather than leaving it to be discovered`() {
        val tool = McpTools.runRellTestsTool()
        val moduleArgs = tool.inputSchema.propertiesOrEmpty.getValue("moduleArgs")
            .jsonObject.getValue("description").jsonPrimitive.content
        assertTrue(moduleArgs.contains("it is not a merge"), moduleArgs)
        assertTrue(moduleArgs.contains("RUN WITH NO MODULE ARGS"), moduleArgs)
        val yaml = tool.inputSchema.propertiesOrEmpty.getValue("yaml")
            .jsonObject.getValue("description").jsonPrimitive.content
        assertTrue(yaml.contains("ONLY when `moduleArgs` is omitted entirely"), yaml)
        assertTrue(yaml.contains("never merged"), yaml)
    }

    // =====================================================================
    // (c) one `deployments:` key, found by parsing
    // =====================================================================

    private val spec = WriteDeploymentConfig.resolveNetwork("testnet")!!

    private fun topLevelDeploymentsKeys(yaml: String): Int =
        yaml.lines().count { WriteDeploymentConfig.topLevelKeyOf(it) == "deployments" }

    @Test
    fun `a comment after the deployments key does not create a second one`() {
        val existing = """
            blockchains:
              demo:
                module: main
            deployments: # prod
              testnet:
                url: https://node0.testnet.chromia.com:7740
                brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"
                container: my-real-lease-id
        """.trimIndent()
        val merged = WriteDeploymentConfig.mergeDeployments(existing, spec, "demo")
        assertEquals(1, topLevelDeploymentsKeys(merged), "YAML takes the LAST key, so a second one orphans the first:\n$merged")
        assertTrue(merged.contains("deployments: # prod"), "the caller's own comment survives a text merge:\n$merged")
        assertTrue(merged.contains("container: my-real-lease-id"), "the lease id this tool cannot invent survives:\n$merged")
        assertEquals(1, merged.lines().count { it.trim() == "testnet:" }, merged)
    }

    @Test
    fun `a column zero comment inside the section does not end it early`() {
        val existing = """
            blockchains:
              demo:
                module: main
            deployments:
              mainnet:
                url: https://system.chromaway.com
                brid: x"7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4"
            # the staging target, kept for reference
              testnet:
                url: https://node0.testnet.chromia.com:7740
                brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"
                container: staging-lease
        """.trimIndent()
        val merged = WriteDeploymentConfig.mergeDeployments(existing, spec, "demo")
        assertEquals(1, topLevelDeploymentsKeys(merged), merged)
        assertEquals(
            1, merged.lines().count { it.trim() == "testnet:" },
            "the rebuilt block must REPLACE the old one, not be emitted beside it under the same key:\n$merged"
        )
        assertTrue(merged.contains("container: staging-lease"), merged)
        assertTrue(merged.contains("mainnet:"), "the other network's block is untouched:\n$merged")
    }

    @Test
    fun `a deployments key that exists only inside a comment block is not the section`() {
        val existing = """
            blockchains:
              demo:
                module: main
            # deployments:
            #   testnet:
            #     container: <containerIID>
        """.trimIndent()
        val merged = WriteDeploymentConfig.mergeDeployments(existing, spec, "demo")
        assertEquals(1, topLevelDeploymentsKeys(merged), "the commented-out example is not a key:\n$merged")
        assertTrue(merged.contains("# deployments:"), "and it is left exactly as the caller wrote it:\n$merged")
        assertTrue(merged.contains("  testnet:"), merged)
    }

    @Test
    fun `a hash inside a quoted value is not a comment`() {
        assertEquals("url: \"https://host/path#frag\"", WriteDeploymentConfig.stripComment("url: \"https://host/path#frag\""))
        assertEquals("deployments:", WriteDeploymentConfig.stripComment("deployments: # prod").trimEnd())
        assertTrue(WriteDeploymentConfig.isCommentLine("   # a note"))
        assertFalse(WriteDeploymentConfig.isCommentLine("deployments: # prod"))
        assertEquals("deployments", WriteDeploymentConfig.topLevelKeyOf("deployments: # prod"))
        assertEquals(null, WriteDeploymentConfig.topLevelKeyOf("  deployments:"))
        assertEquals(null, WriteDeploymentConfig.topLevelKeyOf("# deployments:"))
    }
}
