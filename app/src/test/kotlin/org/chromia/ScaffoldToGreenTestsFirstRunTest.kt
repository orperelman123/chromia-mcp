package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.ChromiaYmlModuleArgs
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RunRellTests
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * AUDIT F4 (2026-09-06) - the flagship `ft4` template's own shipped tests were
 * RED on the honest first run of `run_rell_tests{files}` from the scaffold
 * output. The literal "Secure dapp workflow" prompt, step 3:
 *
 *   scaffold_dapp{name:"fee_token",template:"ft4"}      88 ms  -> files{...}
 *   rell_check{files:<the .rell files>}             17,561 ms  -> ok:true
 *   rell_security_check{files:<same>}                4,730 ms  -> ok:true
 *   run_rell_tests{files:<same>}                    22,131 ms  -> ok:false, 0/3
 *
 * every case failing with
 *   "System function 'rell.test.tx.run': Block execution failed: ...
 *    Unable to create GTX module: net.postchain.rell.module.RellPostchainModuleFactory"
 *
 * The cause: `moduleArgs` and `test.moduleArgs` in the scaffold's own
 * chromia.yml are two blocks that `chr test` merges and this tool did not - it
 * took module args as a parameter and never read the yml it had just handed
 * back. Re-running with the merge assembled by hand gave 3/3 after another
 * 35,824 ms, and the instructions for assembling it were 14 KB inside a 22 KB
 * notes blob.
 *
 * Two independent ways to get it right now, both pinned here:
 *   1. scaffold_dapp returns the merged object as its own top-level
 *      `moduleArgs` field (plus `nextCall`, the literal call to make);
 *   2. run_rell_tests reads chromia.yml - passed as `yaml`, or simply left in
 *      the `files` map the scaffold returned - and performs the same merge.
 *
 * The suite EXECUTION here covers `ft4` (the audit's own template) and
 * `governance` (the one whose main module reads a configured key, so it proves
 * the merge carries template-specific args, not just FT4's). Every OTHER
 * template is pinned structurally: its shipped `moduleArgs` field must equal
 * the merge of its own chromia.yml, which is what `chr test` reads - and the
 * per-template suites themselves already run in DappScaffoldSecureTemplatesTest.
 */
class ScaffoldToGreenTestsFirstRunTest {

    private val executor = ToolExecutor(RecordingRepository(), PromptManager())

    private fun call(name: String, args: JsonObject) = runBlocking {
        executor.executeTool(CallToolRequest(name = name, arguments = args))
    }

    private fun scaffold(template: String): JsonObject {
        val result = call(
            "scaffold_dapp",
            buildJsonObject { put("name", "fee_token"); put("template", template) }
        )
        assertTrue(result.isError != true, "scaffold_dapp{template:$template} errored: ${result.structuredContent}")
        return result.structuredContent!!
    }

    /**
     * `x"02C4..."` (the yml literal), `0x02c4...` and bare hex all decode to the
     * same bytes - the tool's own note says so. Compare what they MEAN.
     */
    private fun normalize(element: JsonElement): JsonElement = when (element) {
        is JsonPrimitive -> if (element.isString) {
            val t = element.content.trim()
            val hex = when {
                t.startsWith("x\"") && t.endsWith("\"") -> t.substring(2, t.length - 1)
                t.startsWith("0x") || t.startsWith("0X") -> t.substring(2)
                else -> t
            }
            JsonPrimitive(if (hex.matches(Regex("[0-9a-fA-F]{2,}"))) hex.uppercase() else t)
        } else {
            element
        }
        is JsonObject -> JsonObject(element.mapValues { (_, v) -> normalize(v) })
        is JsonArray -> JsonArray(element.map { normalize(it) })
    }

    private fun mergedFromYml(files: JsonObject): JsonObject {
        val yml = files["chromia.yml"]!!.jsonPrimitive.content
        return buildJsonObject {
            ChromiaYmlModuleArgs.merged(yml).forEach { (module, args) ->
                put(module, buildJsonObject { args.forEach { (k, v) -> put(k, v) } })
            }
        }
    }

    @Test
    fun everyTemplateShipsTheMergedModuleArgsItsOwnChromiaYmlDeclares() {
        DappScaffold.templates.forEach { template ->
            val payload = scaffold(template)
            assertEquals(template, payload["template"]!!.jsonPrimitive.content)
            assertNotNull(
                payload["moduleArgs"],
                "scaffold_dapp{template:$template} must return the moduleArgs its tests need"
            )
            val shipped = payload["moduleArgs"]!!
            val fromYml = mergedFromYml(payload["files"]!!.jsonObject)
            assertEquals(
                normalize(fromYml),
                normalize(shipped),
                "$template: the field an agent pastes and the yml `chr test` reads must be the SAME merge"
            )
            // hello needs none, and says so honestly rather than decoratively.
            if (template == "hello") {
                assertTrue(shipped.jsonObject.isEmpty(), "hello needs no module args: $shipped")
            } else {
                assertTrue(shipped.jsonObject.isNotEmpty(), "$template needs module args and must ship them")
                assertTrue(
                    shipped.jsonObject.containsKey("lib.ft4.core.admin"),
                    "$template: the test-scoped admin wiring is exactly what the GTX-module failure was about: $shipped"
                )
            }
            val nextCall = payload["nextCall"]!!.jsonPrimitive.content
            assertTrue(nextCall.contains("run_rell_tests"), "$template: $nextCall")
        }
    }

    @Test
    fun theToolAcceptsTheScaffoldsChromiaYmlInsideFilesInsteadOfRejectingIt() {
        // Before: run_rell_tests{files:<everything scaffold_dapp returned>} failed
        // with "Only .rell files are supported: chromia.yml" - so an agent that
        // forwarded exactly what it was given could not even reach the tests.
        // Reaching the "No @test modules found" refusal (no test module in this
        // fixture) proves the yml was consumed as configuration, not compiled.
        val payload = scaffold("ft4")
        val files = payload["files"]!!.jsonObject
        assertTrue(files.containsKey("chromia.yml"))
        val result = call(
            "run_rell_tests",
            buildJsonObject {
                put(
                    "files",
                    buildJsonObject {
                        put("chromia.yml", files["chromia.yml"]!!)
                        put("main.rell", files["src/main.rell"]!!)
                    }
                )
            }
        )
        val text = result.structuredContent.toString()
        assertFalse(text.contains("Only .rell files are supported"), text)
        assertTrue(text.contains("No @test modules found"), text)
    }

    @Test
    fun theMergeIsTheOneChrTestPerformsIncludingTheTestScopedAdminWiring() {
        val yml = scaffold("ft4")["files"]!!.jsonObject["chromia.yml"]!!.jsonPrimitive.content
        val merged = ChromiaYmlModuleArgs.merged(yml)
        // blockchains.<name>.moduleArgs
        assertTrue(merged.containsKey("lib.ft4"), merged.keys.toString())
        assertTrue(merged["lib.ft4.core.accounts"]!!.containsKey("auth_flags"), merged.toString())
        // test.moduleArgs - the half whose absence produced the GTX-module error
        assertTrue(merged.containsKey("lib.ft4.core.admin"), merged.keys.toString())
        assertTrue(merged.containsKey("lib.ft4.test.core.auth"), merged.keys.toString())
        // Types survive the yml: an integer is an integer, a flag list is a list.
        assertEquals(JsonPrimitive(100), merged["lib.ft4"]!!["query_max_page_size"])
    }

    @Test
    fun runRellTestsSchemaDeclaresTheYamlItNowReads() {
        val tool = McpTools.allTools().single { it.name == "run_rell_tests" }
        assertNotNull(tool.inputSchema.properties["yaml"], "the tool reads `yaml`, so it must declare it")
        // An undeclared argument is never honoured - the executor rejects it - so
        // the declaration IS the contract.
        assertTrue(
            tool.inputSchema.properties["moduleArgs"]!!.jsonObject["description"]!!.jsonPrimitive.content
                .contains("RARELY NEED TO BUILD THIS BY HAND"),
            "the description must send an agent to the easy path first"
        )
    }

    // ---- the audit's exact sequence, executed ---------------------------------

    private fun rellOnly(files: JsonObject): JsonObject = buildJsonObject {
        files.forEach { (path, content) -> if (path.endsWith(".rell")) put(path, content) }
    }

    private fun assertGreen(result: io.modelcontextprotocol.kotlin.sdk.CallToolResult, what: String) {
        val s = result.structuredContent!!
        assertTrue(
            s["ok"]!!.jsonPrimitive.boolean,
            "$what must be GREEN on the FIRST honest run: ${s["notes"]?.jsonPrimitive?.content} ${s["cases"]}"
        )
        assertTrue((s["passed"]!!.jsonPrimitive.content.toInt()) > 0, s.toString())
        assertEquals(0, s["failed"]!!.jsonPrimitive.content.toInt(), s.toString())
    }

    private fun runFirstHonestPass(template: String) {
        assumeTrue(
            System.getenv(RunRellTests.DATABASE_URL_ENV) != null,
            "${RunRellTests.DATABASE_URL_ENV} is required: these are real transactions, not a simulation"
        )
        val payload = scaffold(template)
        val files = payload["files"]!!.jsonObject

        // Path 1 - copy TWO fields of the response: files (the .rell ones, exactly
        // as the audit's call did) and the moduleArgs the response handed back.
        assertGreen(
            call(
                "run_rell_tests",
                buildJsonObject {
                    put("files", rellOnly(files))
                    put("moduleArgs", payload["moduleArgs"]!!)
                }
            ),
            "$template with the response's own moduleArgs"
        )

        // Path 2 - forward everything the scaffold returned and pass no module
        // args at all. This is the call the audit made, minus the hand-merging.
        val fromYml = call(
            "run_rell_tests",
            buildJsonObject {
                put(
                    "files",
                    buildJsonObject {
                        put("chromia.yml", files["chromia.yml"]!!)
                        files.forEach { (p, c) -> if (p.endsWith(".rell")) put(p, c) }
                    }
                )
            }
        )
        assertGreen(fromYml, "$template with chromia.yml in files and no moduleArgs argument")
        assertEquals("chromia.yml", fromYml.structuredContent!!["moduleArgsSource"]!!.jsonPrimitive.content)
    }

    @Test
    fun theFt4TemplatesShippedTestsAreGreenOnTheFirstHonestRun() = runFirstHonestPass("ft4")

    @Test
    fun theGovernanceTemplatesShippedTestsAreGreenOnTheFirstHonestRun() = runFirstHonestPass("governance")
}
