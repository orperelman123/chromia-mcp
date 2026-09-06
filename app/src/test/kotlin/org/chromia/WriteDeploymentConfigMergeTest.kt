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
import org.chromia.tools.ChromiaYmlModuleArgs
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.WriteDeploymentConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AUDIT F7 (2026-09-06) - `deployment_preflight`'s own fix line pointed at a
 * tool that could not do it, and using that tool's output silently deleted the
 * FT4 configuration.
 *
 *   deployment_preflight{yaml:<scaffold chromia.yml>, target:"testnet", files:...}
 *   -> {"severity":"BLOCKER","check":"target",
 *       "message":"deployments.testnet not found in chromia.yml (available: (none))",
 *       "fix":"Add the block with write_deployment_config, or pass an existing
 *              deployment target name."}
 *
 *   write_deployment_config{yaml:<that yml>, target:"testnet"}
 *   -> ERROR: Unknown argument(s) for write_deployment_config: `yaml`, `target`
 *      - declared arguments: chain, name, network.
 *
 * The tool never read the caller's yml. Its `chromia_yml` field was a freshly
 * synthesised file, and diffing it against the ft4 scaffold's showed it dropped
 *   - the whole blockchains.<name>.moduleArgs block (lib.ft4.query_max_page_size,
 *     lib.ft4.core.accounts.rate_limit, auth_descriptor, auth_flags.mandatory ["A","T"]),
 *   - the entire test.moduleArgs block (the FT4 test admin wiring),
 *   - the libs.iccf entry the shipped tests need,
 * and validate_chromia_yml on the gutted file returned
 * {"ok":true,"errors":[],"warnings":[]}. An agent that followed the fix line
 * deployed a chain with FT4 auth flags and rate limits unset, green everywhere.
 */
class WriteDeploymentConfigMergeTest {

    private val executor = ToolExecutor(RecordingRepository(), PromptManager())

    private fun call(name: String, args: JsonObject) = runBlocking {
        executor.executeTool(CallToolRequest(name = name, arguments = args))
    }

    private val ft4Yml = DappScaffold.files("fee_token", template = "ft4").getValue("chromia.yml")
    private val ft4Rell = DappScaffold.files("fee_token", template = "ft4")
        .filterKeys { it.endsWith(".rell") }
        .mapKeys { (path, _) -> path.removePrefix("src/") }

    // ---- the merge -----------------------------------------------------------

    @Test
    fun theCallersYamlIsMergedIntoAndNothingItDeclaresIsLost() {
        val result = call(
            "write_deployment_config",
            buildJsonObject {
                put("network", "testnet")
                put("name", "fee_token")
                put("yaml", ft4Yml)
            }
        )
        assertTrue(result.isError != true, result.structuredContent.toString())
        val merged = result.structuredContent!!["chromia_yml"]!!.jsonPrimitive.content

        // Every key the caller declared, still there - by MEANING, not by luck.
        val before = ChromiaYmlModuleArgs.merged(ft4Yml)
        val after = ChromiaYmlModuleArgs.merged(merged)
        assertEquals(before, after, "module args must survive the merge")
        assertTrue(before.isNotEmpty(), "the fixture must actually declare module args")
        listOf(
            "query_max_page_size", "rate_limit", "auth_descriptor",
            "mandatory", "admin_pubkey", "admin_priv_key", "iccf",
            "merkle_hash_version: 2", "rellVersion: ${DappScaffold.RELL_VERSION}"
        ).forEach {
            assertTrue(merged.contains(it), "the merge dropped `$it`:\n$merged")
        }
        // The comments the scaffold wrote are guidance an agent needs; a text
        // merge keeps them, a re-serialisation would not.
        assertTrue(merged.contains("# Test-only FT4 admin wiring"), merged)

        // And the block it DOES own is written.
        assertTrue(merged.contains("deployments:"), merged)
        assertTrue(merged.contains("  testnet:"), merged)
        assertTrue(merged.contains("brid: x\"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}\""), merged)
    }

    @Test
    fun withNoYamlThereIsNoInventedProjectFileAtAll() {
        val result = call(
            "write_deployment_config",
            buildJsonObject { put("network", "testnet"); put("name", "fee_token") }
        )
        val payload = result.structuredContent!!
        assertNull(
            payload["chromia_yml"],
            "inventing a project file from the hello scaffold is exactly how the FT4 config was lost: $payload"
        )
        assertNotNull(payload["yaml"], "the block itself is still returned")
        assertTrue(payload["merge_note"]!!.jsonPrimitive.content.contains("`yaml`"), payload.toString())
    }

    @Test
    fun anExistingBlockIsReplacedButARealLeaseIdAndTheChainsMapSurvive() {
        val withDeployments = ft4Yml.trimEnd() + "\n\n" + buildString {
            appendLine("deployments:")
            appendLine("  testnet:")
            appendLine("    url:")
            appendLine("      - https://stale.example.com:7740")
            appendLine("    brid: x\"00\"")
            appendLine("    container: real_lease_9f2c")
            appendLine("    chains:")
            appendLine("      fee_token: x\"00AA00AA\"")
        }
        val merged = call(
            "write_deployment_config",
            buildJsonObject {
                put("network", "testnet")
                put("name", "fee_token")
                put("yaml", withDeployments)
            }
        ).structuredContent!!["chromia_yml"]!!.jsonPrimitive.content

        assertFalse(merged.contains("stale.example.com"), "the stale url block is replaced:\n$merged")
        assertTrue(merged.contains(WriteDeploymentConfig.TESTNET_DIRECTORY_BRID), merged)
        // The two things this tool cannot invent.
        assertTrue(merged.contains("container: real_lease_9f2c"), "a real lease id must survive:\n$merged")
        assertFalse(merged.contains("<containerIID>"), "and must not be overwritten by the placeholder:\n$merged")
        assertTrue(merged.contains("fee_token: x\"00AA00AA\""), "the chains map chr wrote back must survive:\n$merged")
        assertEquals(ChromiaYmlModuleArgs.merged(ft4Yml), ChromiaYmlModuleArgs.merged(merged))
    }

    @Test
    fun aDeploymentsSectionForAnotherNetworkIsUntouched() {
        val withMainnet = ft4Yml.trimEnd() + "\n\n" + buildString {
            appendLine("deployments:")
            appendLine("  mainnet:")
            appendLine("    url:")
            appendLine("      - https://system.chromaway.com")
            appendLine("    brid: x\"${WriteDeploymentConfig.MAINNET_DIRECTORY_BRID}\"")
            appendLine("    container: mainnet_lease_1")
        }
        val merged = call(
            "write_deployment_config",
            buildJsonObject { put("network", "testnet"); put("name", "fee_token"); put("yaml", withMainnet) }
        ).structuredContent!!["chromia_yml"]!!.jsonPrimitive.content
        assertTrue(merged.contains("container: mainnet_lease_1"), merged)
        assertTrue(merged.contains("  mainnet:") && merged.contains("  testnet:"), merged)
    }

    @Test
    fun theSchemaDeclaresYamlSoTheCallIsNotRefused() {
        val tool = McpTools.allTools().single { it.name == "write_deployment_config" }
        assertNotNull(tool.inputSchema.properties["yaml"])
        // `target` is deliberately NOT a name here - `network` is the one this
        // server uses in 22 other tools - and an undeclared argument is refused
        // with the accepted names rather than silently ignored.
        assertNull(tool.inputSchema.properties["target"])
        val refused = call(
            "write_deployment_config",
            buildJsonObject { put("network", "testnet"); put("target", "testnet") }
        )
        assertTrue(refused.isError == true)
        val text = refused.structuredContent!!["error"]!!.jsonPrimitive.content
        assertTrue(text.contains("`target`"), text)
        assertTrue(text.contains("network"), "the message names the accepted ones: $text")
        assertTrue(text.contains("yaml"), text)
    }

    // ---- the validator that used to call the loss ok:true ---------------------

    private fun validate(yaml: String, rell: Map<String, String>? = null) = call(
        "validate_chromia_yml",
        buildJsonObject {
            put("yaml", yaml)
            if (rell != null) put("files", buildJsonObject { rell.forEach { (p, c) -> put(p, c) } })
        }
    ).structuredContent!!

    @Test
    fun theGuttedConfigIsNoLongerOkWhenTheValidatorCanSeeWhatTheModuleNeeds() {
        // The exact file the old write_deployment_config produced: the hello
        // scaffold's yml, with an ft4 project's sources.
        val gutted = DappScaffold.files("fee_token").getValue("chromia.yml")
        assertTrue(ChromiaYmlModuleArgs.merged(gutted).isEmpty(), "the fixture really is gutted")

        val blind = validate(gutted)
        assertTrue(blind["ok"]!!.jsonPrimitive.boolean, "with no sources it still checks the yml alone")

        val seeing = validate(gutted, ft4Rell)
        assertFalse(seeing["ok"]!!.jsonPrimitive.boolean, "the FT4 configuration is gone: $seeing")
        val errors = seeing["errors"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(errors.any { it.contains("lib.ft4.core.accounts") }, errors.toString())
        assertTrue(
            errors.any { it.contains("auth_flags.mandatory") },
            "name what is actually unset on the chain: $errors"
        )
        assertTrue(errors.any { it.contains("lib.ft4.core.admin") }, errors.toString())
    }

    @Test
    fun theRealFt4ConfigPassesWithTheSameSources() {
        val ok = validate(ft4Yml, ft4Rell)
        assertTrue(
            ok["ok"]!!.jsonPrimitive.boolean,
            "the scaffold's own yml must validate against its own sources: ${ok["errors"]}"
        )
    }

    @Test
    fun aModuleDeclaringItsOwnModuleArgsMustBeSet() {
        val yml = DappScaffold.files("fee_token").getValue("chromia.yml")
        val rell = mapOf(
            "main.rell" to "module;\n\nstruct module_args { oracle_pubkey: byte_array; }\n\nquery who() = chain_context.args.oracle_pubkey;\n"
        )
        val errors = validate(yml, rell)["errors"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(errors.any { it.contains("struct module_args") && it.contains("'main'") }, errors.toString())
        assertTrue(errors.any { it.contains("Missing module_args") }, errors.toString())

        // Set it, and the finding is gone.
        val withArgs = yml.trimEnd() + "\n" + buildString {
            appendLine("")
            appendLine("test:")
            appendLine("  moduleArgs:")
            appendLine("    main:")
            appendLine("      oracle_pubkey: x\"02C4\"")
        }
        val after = validate(withArgs, rell)["errors"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertFalse(after.any { it.contains("struct module_args") }, after.toString())
    }

    @Test
    fun theWholeChainFromPreflightToDeployNoLongerLosesTheConfig() {
        // preflight -> "Add the block with write_deployment_config" -> merge ->
        // preflight again, on the SAME file the project actually has.
        val first = call(
            "deployment_preflight",
            buildJsonObject {
                put("yaml", ft4Yml)
                put("target", "testnet")
                put("rell", buildJsonObject { ft4Rell.forEach { (p, c) -> put(p, c) } })
            }
        ).structuredContent!!
        assertFalse(first["ready"]!!.jsonPrimitive.boolean)
        val fix = first["findings"]!!.jsonArray.map { it.jsonObject }
            .first { it["check"]!!.jsonPrimitive.content == "target" }["fix"]!!.jsonPrimitive.content
        assertTrue(fix.contains("write_deployment_config"), fix)

        val merged = call(
            "write_deployment_config",
            buildJsonObject { put("network", "testnet"); put("name", "fee_token"); put("yaml", ft4Yml) }
        ).structuredContent!!["chromia_yml"]!!.jsonPrimitive.content

        assertEquals(
            ChromiaYmlModuleArgs.merged(ft4Yml),
            ChromiaYmlModuleArgs.merged(merged),
            "following the fix line must not cost the project its FT4 configuration"
        )
        assertTrue(validate(merged, ft4Rell)["ok"]!!.jsonPrimitive.boolean, validate(merged, ft4Rell).toString())
    }
}
