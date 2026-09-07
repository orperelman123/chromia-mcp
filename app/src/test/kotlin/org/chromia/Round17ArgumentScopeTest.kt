package org.chromia

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.chromia.tools.McpTools
import org.chromia.tools.propertiesOrEmpty
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ROUND 17, SECTION 2: the two `verify_guards` arguments that were documented
 * WITHOUT QUALIFICATION and read in one shape only.
 *
 * The input schema described `stillRefused` as "Optional error fragment that
 * means the attack was STILL refused ... Its presence yields still_refused
 * instead of load_bearing" and `attackLanded` as "Optional error fragment that
 * proves the attack SUCCEEDED", both flatly. In the shipped code both are
 * referenced only inside the local `landed()` helper, and every call to
 * `landed()` is in section 4e - SHAPE B. A `run_must_fail` test takes the
 * `if (stmt.runMustFail)` branch above it and returns without reading either.
 * So in the must-fail shape the tool is named for, a caller's own pins were
 * silently ignored.
 *
 * THE ROUND-17 DECISION IS TO QUALIFY RATHER THAN TO WIDEN, and the reason is
 * structural rather than a preference. A shape A statement is a single-operation
 * `rell.test.tx().op(<the declaration>(...)).run_must_fail(...)`; the one
 * operation it carries IS a declaration the guard runs in (that is what made
 * the statement a shape A statement); and an operation refusal always carries
 * its own `[module:declaration(file:line)]` frame. So a shape A red is either
 * "did not fail" - the transaction went through, the attack landed, whatever
 * the caller pinned - or a frame naming the guard's own declaration. There is
 * no third red for a fragment to decide, and letting a caller-supplied
 * substring outrank that frame is precisely how round 11 got four false
 * `ok:true` (`attackLanded` was a free substring, so the guard's OWN refusal
 * could be read as the attack succeeding).
 *
 * A qualification nobody can read is not a qualification, so the words are
 * pinned here: in the schema the agent is handed, in the `describe_tool` long
 * form, and in the advertised description.
 */
class Round17ArgumentScopeTest {

    private fun schemaDescription(argument: String): String =
        McpTools.verifyGuardsTool().inputSchema.propertiesOrEmpty
            .getValue("guards").jsonObject
            .getValue("items").jsonObject
            .getValue("properties").jsonObject
            .getValue(argument).jsonObject
            .getValue("description").jsonPrimitive.content

    @Test
    fun `the schema says which shape reads stillRefused and attackLanded`() {
        val stillRefused = schemaDescription("stillRefused")
        assertTrue(
            stillRefused.contains("SHAPE B ONLY"),
            "stillRefused is read in shape B only and the schema must say so: $stillRefused"
        )
        assertTrue(
            stillRefused.contains("still_refused instead of load_bearing"),
            "the qualification must not lose what the argument DOES: $stillRefused"
        )
        assertTrue(
            stillRefused.contains("changes no SHAPE A verdict"),
            "the schema must say what it does NOT do, not only what it does: $stillRefused"
        )
        assertTrue(
            stillRefused.contains("did not fail") && stillRefused.contains("frame"),
            "the schema must give the REASON - the runner has already answered it in shape A: $stillRefused"
        )

        val attackLanded = schemaDescription("attackLanded")
        assertTrue(
            attackLanded.contains("SHAPE B ONLY"),
            "a custom attackLanded is read in shape B only and the schema must say so: $attackLanded"
        )
        assertTrue(
            attackLanded.contains("in both shapes"),
            "the DEFAULT \"did not fail\" is read in both shapes and the schema must keep saying so: $attackLanded"
        )
        assertTrue(
            attackLanded.contains("red_for_another_reason"),
            "the schema must keep the verdict a missing custom fragment produces: $attackLanded"
        )
        assertTrue(
            attackLanded.contains("changes no SHAPE A verdict"),
            "the schema must say what it does NOT do: $attackLanded"
        )
    }

    @Test
    fun `the long form and the advertised description carry the same qualification`() {
        val long = McpTools.fullDescription("verify_guards").orEmpty()
        assertTrue(
            long.contains("stillRefused and attackLanded are read in SHAPE B ONLY"),
            "describe_tool{tool:\"verify_guards\"} must qualify the two arguments"
        )
        assertTrue(
            long.contains("no third red for a fragment to decide"),
            "the long form must give the structural reason, not just the rule"
        )
        assertTrue(
            long.contains("round 11 got") && long.contains("false ok:true"),
            "the long form must say why a pin may not outrank the guard's own frame"
        )
        val advertised = McpTools.advertisedDescription("verify_guards").orEmpty()
        assertTrue(
            advertised.contains("stillRefused") && advertised.contains("attackLanded"),
            "the 1200-byte description must name the two arguments it qualifies: $advertised"
        )
        assertTrue(
            advertised.contains("must-hold shape only") || advertised.contains("SHAPE B only"),
            "the advertised description must carry the scope too - an agent reads it first: $advertised"
        )
    }
}
