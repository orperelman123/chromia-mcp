package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AUDIT F6 (2026-09-06) - every plausible template alias silently scaffolded
 * `hello`, and the schema promised the opposite.
 *
 * Schema text, verbatim: "An unknown template name is answered with the closest
 * shipped template and what it does NOT cover."
 *
 * Measured on the eighteen names an agent would guess - dex, orderbook,
 * order_book, nft, token, dao, allowance, escrow, crosschain, oracle, swap,
 * auction, payroll, vesting, treasury, erc20, stable, Bridge: 18 of 18 returned
 * "template":"hello" with the hello query-only files, isError:false. Not one
 * returned the closest template's code. The `warnings` text was the best
 * security guidance in the product - dex -> "Use `template=amm`", nft ->
 * marketplace, dao -> governance - and a complete, compilable, guard-free
 * skeleton for a different problem was attached to it. An agent that checks
 * ok/isError and reads `files` - the normal pattern for every other tool here -
 * built an NFT marketplace on the query-only `hello` skeleton, which then
 * passed rell_check and rell_security_check cleanly.
 */
class UnknownTemplateNeverShipsHelloCodeTest {

    /** The audit's own eighteen, in the audit's own order. */
    private val theEighteen = listOf(
        "dex", "orderbook", "order_book", "nft", "token", "dao", "allowance", "escrow",
        "crosschain", "oracle", "swap", "auction", "payroll", "vesting", "treasury",
        "erc20", "stable", "Bridge"
    )

    private val executor = ToolExecutor(RecordingRepository(), PromptManager())

    private fun scaffold(template: String) = runBlocking {
        executor.executeTool(
            CallToolRequest(
                name = "scaffold_dapp",
                arguments = buildJsonObject { put("name", "my_dapp"); put("template", template) }
            )
        )
    }

    private fun helloFiles() = DappScaffold.files("my_dapp", "hello")

    @Test
    fun noneOfTheEighteenGetsHelloCodeForSomethingElse() {
        val redirected = mutableListOf<String>()
        val refused = mutableListOf<String>()
        theEighteen.forEach { asked ->
            val result = scaffold(asked)
            val payload = result.structuredContent!!
            val template = payload["template"]!!.jsonPrimitive.content
            val ok = payload["ok"]!!.jsonPrimitive.boolean
            val warnings = payload["warnings"]!!.jsonArray.joinToString(" ") { it.jsonPrimitive.content }

            val isExactName = DappScaffold.templates.any { it.equals(asked.trim(), ignoreCase = true) }
            if (ok) {
                redirected += asked
                assertTrue(template in DappScaffold.templates, "$asked -> $template")
                val files = payload["files"]!!.jsonObject
                assertTrue(files.isNotEmpty(), "$asked -> $template must come with that template's files")
                // The decisive check: it is NOT the hello skeleton wearing another
                // template's name. `hello` is query-only; a redirected template is not.
                if (template != "hello") {
                    assertNotEquals(
                        helloFiles()["src/main.rell"],
                        files["src/main.rell"]?.jsonPrimitive?.content,
                        "$asked -> $template still shipped the hello skeleton"
                    )
                }
                if (isExactName) {
                    // `Bridge` is a shipped template under any casing - an exact
                    // name is not an unknown one, and carries no warning.
                    assertEquals(asked.trim().lowercase(), template, asked)
                    assertTrue(warnings.isEmpty(), "$asked is a real template name: $warnings")
                } else {
                    assertEquals(
                        DappScaffold.closestTemplate(asked),
                        template,
                        "$asked must scaffold the template its own redirect names, not a substitute"
                    )
                    assertTrue(warnings.contains("template=$template"), "$asked: $warnings")
                    assertTrue(warnings.contains("CLOSEST shipped template"), "$asked: $warnings")
                }
                assertTrue(result.isError != true, "a redirect that scaffolds real code is not an error: $asked")
            } else {
                refused += asked
                assertNull(payload["files"], "$asked: nothing close, so NO files - not a skeleton to build on")
                assertTrue(result.isError == true, "$asked must stop an agent that checks isError")
                assertTrue(warnings.contains("NOTHING was scaffolded"), "$asked: $warnings")
                assertTrue(warnings.contains("No shipped template covers that name"), "$asked: $warnings")
            }
        }
        // The three the audit called out by name must route, not refuse.
        assertTrue("dex" in redirected && "nft" in redirected && "dao" in redirected, "redirected=$redirected")
        assertTrue(redirected.size >= 14, "the great majority must route: redirected=$redirected refused=$refused")
    }

    @Test
    fun theThreeTheAuditNamedRouteWhereItsOwnWarningSaid() {
        mapOf("dex" to "amm", "nft" to "marketplace", "dao" to "governance").forEach { (asked, expected) ->
            val payload = scaffold(asked).structuredContent!!
            assertEquals(expected, payload["template"]!!.jsonPrimitive.content, asked)
            assertEquals(
                DappScaffold.files("my_dapp", expected).keys,
                payload["files"]!!.jsonObject.keys,
                "$asked must get $expected's own files"
            )
            // ...and the security guidance that made the old warning worth reading
            // is still attached to it.
            val warnings = payload["warnings"]!!.jsonArray.joinToString(" ") { it.jsonPrimitive.content }
            assertTrue(warnings.contains("does NOT cover") || warnings.contains("NOT cover"), warnings)
        }
    }

    @Test
    fun aNameNoTemplateCoversScaffoldsNothingAtAll() {
        val payload = scaffold("quantum_widget_registry").structuredContent!!
        assertFalse(payload["ok"]!!.jsonPrimitive.boolean)
        assertNull(payload["files"])
        assertNull(payload["moduleArgs"], "no template, no module args to pretend about")
        assertEquals("", payload["template"]!!.jsonPrimitive.content)
        val warnings = payload["warnings"]!!.jsonArray.joinToString(" ") { it.jsonPrimitive.content }
        assertTrue(warnings.contains("write the economic invariant test FIRST"), warnings)
        assertTrue(
            payload["nextCall"]!!.jsonPrimitive.content.contains("EXPLOIT class"),
            payload["nextCall"].toString()
        )
    }

    @Test
    fun everyShippedTemplateStillScaffoldsItselfAndIsOk() {
        DappScaffold.templates.forEach { template ->
            val payload = scaffold(template).structuredContent!!
            assertTrue(payload["ok"]!!.jsonPrimitive.boolean, template)
            assertEquals(template, payload["template"]!!.jsonPrimitive.content)
            assertTrue(payload["warnings"]!!.jsonArray.isEmpty(), "$template: ${payload["warnings"]}")
            assertNotNull(payload["files"])
        }
        // Case and whitespace are still not a different ask.
        assertEquals("amm", scaffold("AMM").structuredContent!!["template"]!!.jsonPrimitive.content)
        assertEquals("bridge", scaffold(" Bridge ").structuredContent!!["template"]!!.jsonPrimitive.content)
    }

    @Test
    fun theSchemaNowPromisesWhatTheToolDoes() {
        val tool = McpTools.allTools().single { it.name == "scaffold_dapp" }
        val enumDescription = tool.inputSchema.properties["template"]!!.jsonObject["description"]!!
            .jsonPrimitive.content
        assertTrue(enumDescription.contains("SCAFFOLDS the closest shipped template"), enumDescription.takeLast(400))
        assertTrue(enumDescription.contains("NOTHING is scaffolded"), enumDescription.takeLast(400))
        val out = tool.outputSchema!!
        assertNotNull(out.properties["ok"])
        assertTrue(out.required.orEmpty().contains("ok"))
        assertTrue(out.required.orEmpty().contains("template"))
        assertFalse(
            out.required.orEmpty().contains("files"),
            "`files` cannot be required when the honest answer is sometimes no files at all"
        )
    }

    private fun assertNotEquals(unexpected: Any?, actual: Any?, message: String) {
        assertFalse(unexpected == actual, message)
    }
}
