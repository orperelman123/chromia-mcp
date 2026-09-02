package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RellSecurityCheckStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RellSecurityCheckToolTest {

    private val repo = RecordingRepository()

    private fun run(arguments: kotlinx.serialization.json.JsonObject) = runBlocking {
        RellSecurityCheckStrategy().execute(
            CallToolRequest(name = "rell_security_check", arguments = arguments),
            repo
        )
    }

    @Test
    fun unauthenticatedMutationIsHighFinding() {
        val result = run(
            buildJsonObject {
                put(
                    "source",
                    """
                    module;
                    entity vault { key owner: text; mutable amount: integer; }
                    operation drain(owner: text, amount: integer) {
                        update vault @ { .owner == owner } ( .amount -= amount );
                    }
                    """.trimIndent()
                )
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(false, structured.getValue("ok").jsonPrimitive.content.toBoolean())
        val rules = structured.getValue("findings").jsonArray.map { it.jsonObject.getValue("rule").jsonPrimitive.content }
        assertTrue("unauthenticated-mutation" in rules, rules.toString())
    }

    @Test
    fun authenticatedValidatedOperationIsClean() {
        val result = run(
            buildJsonObject {
                put(
                    "source",
                    """
                    module;
                    entity note { key id: text; body: text; }
                    operation add_note(id: text, content: text) {
                        require(op_context.is_signer(x"03"), "not authorized");
                        require(id.size() > 0, "empty id");
                        create note(id, body = content);
                    }
                    """.trimIndent()
                )
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
    }

    @Test
    fun bannedModuleIsCriticalEvenWithoutOperations() {
        val findings = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nimport lib.ft4.admin;\n")
        )
        assertEquals(false, findings.ok)
        assertTrue(findings.findings.any { it.rule == "banned-module" && it.severity == "CRITICAL" })
    }

    @Test
    fun hardcodedKeyMaterialIsFlagged() {
        val findings = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to
                    "module;\nval k = x\"15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304\";\n"
            )
        )
        assertTrue(findings.findings.any { it.rule == "hardcoded-key-material" && it.line == 2 })
    }

    @Test
    fun uncompilableCodeReturnsCompileErrorsFirst() {
        val result = run(
            buildJsonObject { put("source", "module;\nquery broken() = nope_not_defined;") }
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        assertEquals(false, structured.getValue("ok").jsonPrimitive.content.toBoolean())
        assertTrue(structured.containsKey("compileErrors"), structured.toString())
        val notes = structured.getValue("notes").jsonPrimitive.content
        assertTrue(notes.contains("rell_check"), notes)
    }

    /**
     * The open-registration rule used to scan only for the `ras_open` /
     * `ras_transfer_open` tokens. Those operations are declared inside FT4
     * itself, and FT4's tree is exempt from this scan, so the rule never fired
     * on real app code - a dApp enabling permissionless registration the only
     * way anyone actually does (importing the strategy module) came back clean
     * while the tool's notes claimed it checked registration strategies.
     */
    @Test
    fun importingTheOpenRegistrationStrategyIsCritical() {
        val findings = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to "module;\n" +
                    "import lib.ft4.accounts.strategies.open;\n" +
                    "entity note { key id: integer; msg: text; }\n"
            )
        ).findings
        val hit = findings.singleOrNull { it.rule == "open-registration-strategy" }
        assertTrue(hit != null, "open-strategy import must be flagged; got $findings")
        assertEquals("CRITICAL", hit!!.severity)
        assertEquals(2, hit.line)
    }

    @Test
    fun openTransferStrategyImportIsCriticalToo() {
        val findings = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nimport lib.ft4.accounts.strategies.transfer.open;\n")
        ).findings
        assertTrue(
            findings.any { it.rule == "open-registration-strategy" && it.severity == "CRITICAL" },
            findings.toString()
        )
    }

    /** Boundary: a differently-named sibling module must not trip the rule. */
    @Test
    fun gatedStrategyImportIsNotFlagged() {
        val findings = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nimport lib.ft4.accounts.strategies.open_gated;\n")
        ).findings
        assertTrue(
            findings.none { it.rule == "open-registration-strategy" },
            "open_gated is a different module and must not trip the rule; got $findings"
        )
    }

    @Test
    fun operationScannerFindsBlocksAndLines() {
        val ops = RellSecurityCheck.scanOperations(
            "main.rell",
            "module;\n\noperation first(a: text) {\n    require(a != \"\");\n}\n\noperation second() {\n}\n"
        )
        assertEquals(listOf("first" to 3, "second" to 7), ops.map { it.name to it.line })
    }
}
