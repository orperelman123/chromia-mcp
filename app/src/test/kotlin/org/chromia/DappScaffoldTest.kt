package org.chromia

import org.chromia.tools.propertiesOrEmpty
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ScaffoldDappStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DappScaffoldTest {

    private val importForbidden = listOf(
        "lib.ft4.admin",
        "lib.ft4.core.admin",
        "admin.crosschain",
        "ras_open",
        "ras_transfer_open",
        "lib.ft4.core.accounts.strategies.open",
        "lib.ft4.accounts.strategies.open"
    )

    private fun hasLiveImport(source: String, module: String): Boolean =
        source.lineSequence().any { line ->
            val trimmed = line.trim()
            !trimmed.startsWith("//") && trimmed.startsWith("import $module")
        }

    @Test
    fun skeletonContainsProductionPinsAndForbidsAdminModules() {
        val name = DappScaffold.normalizeName("demo_chain")
        assertEquals("demo_chain", name)
        val files = DappScaffold.files(name)
        val yml = files.getValue("chromia.yml")
        val main = files.getValue("src/main.rell")
        val test = files.getValue("src/test/main_test.rell")
        val all = files.values.joinToString("\n") + "\n" + DappScaffold.notes(name)

        assertTrue(yml.contains("merkle_hash_version: 2"))
        assertFalse(yml.contains("merkle_hash_version: 1"))
        assertTrue(yml.contains("rellVersion: 0.16.1"))
        assertTrue(yml.contains("tagOrBranch: v1.1.0r"))
        assertTrue(yml.contains("demo_chain:"))
        assertTrue(yml.contains("module: main"))
        assertTrue(main.contains("module;"))
        assertTrue(main.contains("query hello_world()"))
        assertTrue(main.contains("Hello %s!"))
        assertTrue(main.contains(".format(my_name.name)"))
        assertTrue(main.contains("object my_name"))
        assertTrue(main.contains("mutable name = \"World\""))
        assertTrue(main.contains("operation set_name(name)"))
        assertTrue(test.contains("test_hello_world"))
        assertTrue(test.contains("Hello World!"))
        assertTrue(test.contains("@test module"))
        assertTrue(all.contains("0.16.7"))
        assertTrue(all.contains("v1.1.0r"))
        assertTrue(all.contains("API 1") || all.contains("ft4Api"))
        assertTrue(all.contains("0.33.x"))

        importForbidden.forEach { module ->
            assertFalse(hasLiveImport(main, module), "main.rell must not import $module")
            assertFalse(hasLiveImport(test, module), "test must not import $module")
            assertFalse(yml.contains("import $module"), "chromia.yml must not import $module")
        }
        DappScaffold.forbiddenModules.forEach { module ->
            assertTrue(
                DappScaffold.notes(name).contains(module) || main.contains(module),
                "forbidden $module must be documented"
            )
        }
    }

    @Test
    fun invalidNameFallsBackToHello() {
        assertEquals("hello", DappScaffold.normalizeName(null))
        assertEquals("hello", DappScaffold.normalizeName("  "))
        assertEquals("hello", DappScaffold.normalizeName("Bad-Name"))
        assertEquals("hello", DappScaffold.normalizeName("1abc"))
    }

    @Test
    fun scaffoldDappToolReturnsPinsAndForbiddenList() = runBlocking {
        val result = ScaffoldDappStrategy().execute(
            callToolRequest(
                name = "scaffold_dapp",
                arguments = buildJsonObject { put("name", "wallet") }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("wallet", payload["name"]!!.jsonPrimitive.content)
        assertEquals("0.16.1", payload["rellVersion"]!!.jsonPrimitive.content)
        assertEquals("v1.1.0r", payload["ft4Version"]!!.jsonPrimitive.content)
        assertEquals("1", payload["ft4Api"]!!.jsonPrimitive.content)
        assertEquals(2, payload["merkleHashVersion"]!!.jsonPrimitive.content.toInt())
        val pins = payload["pins"]!!.jsonObject
        assertEquals("0.16.1", pins["rell"]!!.jsonPrimitive.content)
        assertEquals(2, pins["merkle_hash_version"]!!.jsonPrimitive.content.toInt())
        val forbidden = payload["forbidden"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("lib.ft4.admin" in forbidden)
        assertTrue("ras_open" in forbidden)
        assertTrue("ras_transfer_open" in forbidden)
        assertTrue("admin.crosschain" in forbidden)
        val files = payload["files"]!!.jsonObject
        val yml = files["chromia.yml"]!!.jsonPrimitive.content
        val main = files["src/main.rell"]!!.jsonPrimitive.content
        assertTrue(yml.contains("merkle_hash_version: 2"))
        assertTrue(yml.contains("rellVersion: 0.16.1"))
        assertTrue(yml.contains("v1.1.0r"))
        assertTrue(yml.contains("wallet:"))
        importForbidden.forEach { module ->
            assertFalse(hasLiveImport(main, module), "tool output must not import $module")
            assertFalse(yml.contains("import $module"))
        }
        assertTrue(payload["notes"]!!.jsonPrimitive.content.contains("not send signed", ignoreCase = true) ||
            payload["notes"]!!.jsonPrimitive.content.contains("does not send signed"))
        assertEquals(payload, result.structuredContent)
    }

    @Test
    fun scaffoldDappIsRegisteredAndPrompted() {
        assertTrue(McpTools.allTools().any { it.name == "scaffold_dapp" })
        val schema = McpTools.scaffoldDappTool().outputSchema
        assertNotNull(schema)
        // AUDIT F6: `files` cannot be REQUIRED, because the honest answer to a
        // template name nothing covers is no files at all - attaching a
        // compilable, guard-free `hello` skeleton to that refusal is how an
        // agent builds the wrong thing and passes every gate. It is still
        // declared, and `ok` / `template` are what a caller must always get.
        assertNotNull(schema!!.propertiesOrEmpty["files"])
        assertFalse(schema.required.orEmpty().contains("files"))
        assertTrue(schema.required.orEmpty().contains("ok"))
        assertTrue(schema.required.orEmpty().contains("template"))
        val prompts = PromptManager()
        assertTrue(prompts.getCategories().contains("dapp_build"))
        val prompt = prompts.getPrompt("dapp_build", "Scaffold a new Chromia dapp")
        assertNotNull(prompt)
        val text = prompt!!["prompt"]!!.jsonPrimitive.content
        assertTrue(text.contains("0.16.1"))
        assertTrue(text.contains("merkle_hash_version 2"))
        assertTrue(text.contains("v1.1.0r"))
        assertTrue(text.contains("lib.ft4.admin"))
        assertTrue(text.contains("ras_open"))
        assertEquals("scaffold_dapp", prompts.canonicalToolName(prompts.getToolForPrompt(prompt)))
    }
}
