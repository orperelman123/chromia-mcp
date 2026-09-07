package org.chromia

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolDocs
import org.chromia.tools.ToolExecutor
import org.chromia.tools.callToolRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Audit F9(a): the long form of the 14 over-bound tool descriptions was MOVED,
 * not deleted. `tool-descriptions-baseline.json` is the advertised description of
 * all 74 tools on the base commit (b982fbb), cross-checked against the audit's
 * own live `tools/list` capture. Every sentence in it must still be reachable by
 * name - through describe_tool, and for the moved ones through chromia_help too.
 *
 * The security guidance in scaffold_dapp / verify_guards / rell_security_check is
 * the product. This test is what stops "shorter" from becoming "less".
 */
class ToolDescriptionBudgetTest {

    private val executor = ToolExecutor(McpTestSupport.offlineRepository(), PromptManager())

    private val baseline: Map<String, String> by lazy {
        val text = checkNotNull(javaClass.classLoader.getResourceAsStream("tool-descriptions-baseline.json")) {
            "tool-descriptions-baseline.json is missing from the test resources"
        }.bufferedReader().use { it.readText() }
        Json.decodeFromString<Map<String, String>>(text)
    }

    private fun call(name: String, arguments: JsonObject): JsonObject {
        val result = runBlocking { executor.executeTool(callToolRequest(name = name, arguments = arguments)) }
        assertTrue(result.isError != true, "$name $arguments returned an error: ${result.content}")
        return checkNotNull(result.structuredContent) { "$name $arguments returned no structured content" }
    }

    private fun describe(tool: String): JsonObject =
        call("describe_tool", buildJsonObject { put("tool", tool) })

    /** Whitespace-insensitive comparison: the move re-wraps lines, it must not drop words. */
    private fun normalize(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    /** Sentence-ish fragments worth pinning; punctuation-free fragments are kept whole. */
    private fun sentences(text: String): List<String> =
        text.split(Regex("(?<=\\.)\\s+|\\n"))
            .map { normalize(it) }
            .filter { it.length >= 25 }

    @Test
    fun baselineCoversEveryToolOnTheBaseCommit() {
        assertEquals(74, baseline.size, "the baseline fixture must hold all 74 base-commit tools")
        val current = McpTools.allTools(compact = false).map { it.name }.toSet()
        val missing = baseline.keys - current
        assertEquals(
            RETIRED_TOOLS.keys, missing,
            "a base-commit tool disappeared without a reason. Retiring a tool is allowed - it is\n" +
                "how a tool the upstream service will not serve stops being advertised - but the\n" +
                "reason belongs in RETIRED_TOOLS where a reviewer can check it."
        )
    }

    /**
     * Tools deliberately RETIRED, with the reason. A retired tool takes its whole
     * description with it, so it is excluded from the sentence pin below - the
     * guidance is not "moved somewhere else", it described data that cannot be
     * fetched at all. Probed live 2026-09-07 against explorer.chromia.com.
     */
    private val RETIRED_TOOLS: Map<String, String> = mapOf(
        "get_network_stats" to
            "explorer `dashboardData` answers INTERNAL_ERROR for every selection set, and the " +
            "schema has no top-level replacement for its counts",
        "get_transactions_by_cluster" to
            "the same `dashboardData` resolver; top-level groupedTransactionsByCluster was " +
            "removed upstream (docs/UPSTREAM.md #3)",
        "get_blockchains_transactions" to
            "top-level `groupedTransactionsByBlockchain` answers INTERNAL_ERROR with no arguments",
        "get_node_unavailability" to
            "the explorer demands an X-reCAPTCHA-Token header (docs/UPSTREAM.md #7a); there is " +
            "no programmatic path and bypassing a CAPTCHA is not one"
    )

    /**
     * Sentences deliberately RETIRED, with the reason. The rule is that guidance
     * is moved, never deleted - but a sentence that describes behaviour this work
     * changed would be a lie if it were preserved. Each entry is a decision a
     * reviewer can check, not a way round the pin.
     */
    private val retired: Map<String, List<String>> = mapOf(
        // chromia_help no longer returns the whole payload for a topic: it returns
        // that topic's table of contents, and `section` (or section:"all") the text.
        "chromia_help" to listOf(
            "Call with no arguments to list all topics; call with a topic to get that payload.",
            "The same content as the individual *_help tools, through one schema.",
            "best practices), FT4 queries, integrations, vector search, and the cookbook."
        ),
        // Round 16 replaced the round-15 account of HOW a refusal is attributed
        // (statement order, frame-less literal ownership, differential
        // truncation) with the two canonical test shapes - the tool no longer
        // works the way these sentences describe, so they are retired, and the
        // long form now carries the shapes in full (ToolDocs.VERIFY_GUARDS).
        "verify_guards" to listOf(
            "the declaration the guard sits in, MODULE INCLUDED, or",
            "it through your own call graph; and when it came from the",
            "FIRST statement of the test that invokes that declaration.",
            "A refusal by any other declaration, by the same one in a",
            "LATER statement, or a test-side failure, is the damage",
            "being noticed and counts as load_bearing.",
            "An error with NO frame is never an operation (a refusing",
            "operation always carries one): it is the test module or a",
            "QUERY, and which one is read off the string literals each",
            "owns - so a query whose own second guard refuses is",
            "still_refused, while a rell.test assert_* or a test-side",
            "require is the damage being measured.",
            "When the frame names the guard's declaration and the test",
            "invokes it more than once, the tool CUTS the test after",
            "the first such statement and runs the mutant again",
            "(DIFFERENTIAL TRUNCATION): still refused there means the",
            "attack was refused; passing there means it landed.",
            "ambiguous_refusal - the mutant went red and the tool CANNOT SAY which of the",
            "two it is: a frame-less error whose words BOTH the test",
            "module and a production query the test invokes can",
            "produce (or that neither can), or a repeated invocation",
            "whose first call cannot be located exactly - a loop, or a",
            "helper with several call sites - so the truncated re-run",
            "would not measure the attack.",
            "reason to weaken the test - the evidence text says what",
            "to add (an assertion on the state the attack changes, a",
            "run_must_fail on the attack, distinct wording for the two",
            "messages, or one call to the declaration instead of",
            "several)."
        )
    )

    @Test
    fun everySentenceThatLeftADescriptionIsReachableByName() {
        val unreachable = mutableListOf<String>()
        baseline.forEach { (tool, original) ->
            if (tool in RETIRED_TOOLS) return@forEach
            val full = normalize(describe(tool).getValue("description").jsonPrimitive.content)
            val allowed = retired[tool].orEmpty().map { normalize(it) }
            sentences(original).forEach { sentence ->
                if (!full.contains(sentence) && sentence !in allowed) unreachable += "$tool :: $sentence"
            }
        }
        assertTrue(
            unreachable.isEmpty(),
            "sentences that left a tool description and are no longer reachable via describe_tool:\n" +
                unreachable.joinToString("\n")
        )
    }

    @Test
    fun movedDescriptionsAreAlsoReachableAsAHelpTopic() {
        assertTrue(ToolDocs.TOPICS.isNotEmpty(), "nothing was moved - F9(a) did not happen")
        // A name that is BOTH a help tool and a moved tool description stays the
        // help tool - chromia_help{topic:"chr_deploy_help"} is its content, and
        // describe_tool is where its prose lives.
        (ToolDocs.TOPICS - McpTools.HELP_TOOL_NAMES).forEach { tool ->
            val viaHelp = call("chromia_help", buildJsonObject { put("topic", tool) })
            assertEquals(tool, viaHelp.getValue("tool").jsonPrimitive.content)
            assertEquals(
                normalize(describe(tool).getValue("description").jsonPrimitive.content),
                normalize(viaHelp.getValue("description").jsonPrimitive.content),
                "chromia_help{topic:\"$tool\"} and describe_tool disagree"
            )
            assertTrue(
                tool in McpTools.HELP_TOPIC_NAMES,
                "$tool is not advertised in the chromia_help topic enum"
            )
        }
        assertTrue("scaffold_dapp" in ToolDocs.TOPICS)
        assertTrue("verify_guards" in ToolDocs.TOPICS)
        assertTrue("rell_security_check" in ToolDocs.TOPICS)
    }

    @Test
    fun shortenedDescriptionsPointAtWhereTheRestWent() {
        ToolDocs.LONG.keys.forEach { tool ->
            val advertised = checkNotNull(McpTools.advertisedDescription(tool))
            assertTrue(
                advertised.contains("describe_tool"),
                "$tool was shortened but never says where the rest is"
            )
            assertTrue(
                advertised.toByteArray().size < ToolDocs.LONG.getValue(tool).toByteArray().size,
                "$tool advertised form is not shorter than the moved long form"
            )
        }
    }

    @Test
    fun describeToolReturnsTheUntrimmedSchemaAndListsNamesWithoutArguments() {
        val scaffold = describe("scaffold_dapp")
        assertEquals("scaffold_dapp", scaffold.getValue("tool").jsonPrimitive.content)
        assertTrue(scaffold.containsKey("inputSchema"), "describe_tool must return the input schema")
        val schemaText = scaffold.getValue("inputSchema").toString()
        assertTrue(schemaText.contains("template"), "the scaffold_dapp schema lost its template property")
        assertTrue(schemaText.contains("description"), "describe_tool must keep the schema's own prose")

        val index = call("describe_tool", buildJsonObject {})
        val names = index.getValue("tools").toString()
        McpTools.allTools(compact = false).forEach { tool ->
            assertTrue(names.contains("\"${tool.name}\""), "describe_tool's index is missing ${tool.name}")
        }

        val unknown = call("describe_tool", buildJsonObject { put("tool", "no_such_tool") })
        assertTrue(unknown.getValue("notes").jsonPrimitive.content.contains("no_such_tool"))
        assertNotNull(unknown["tools"])
    }

    /** Compact mode strips schema prose; describe_tool is what hands it back. */
    @Test
    fun compactSchemasKeepTypesAndEnumsButNoProse() {
        val compact = McpTools.allTools(compact = true).associateBy { it.name }
        val scaffold = checkNotNull(compact["scaffold_dapp"]).inputSchema.properties.toString()
        assertTrue(scaffold.contains("\"template\""), "compact schema lost the template property")
        assertTrue(scaffold.contains("\"enum\""), "compact schema must keep enums - they are the vocabulary")
        assertTrue(!scaffold.contains("\"description\""), "compact schema still carries prose")
        assertEquals(null, checkNotNull(compact["scaffold_dapp"]).outputSchema)
    }
}
