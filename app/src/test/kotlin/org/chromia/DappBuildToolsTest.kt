package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.CheckDappProject
import org.chromia.tools.CheckFt4ImportsStrategy
import org.chromia.tools.ChrBuildHelp
import org.chromia.tools.ChrBuildHelpStrategy
import org.chromia.tools.ChrEifHelp
import org.chromia.tools.ChrEifHelpStrategy
import org.chromia.tools.ChromiaYmlDefinitionsHelp
import org.chromia.tools.ChromiaYmlDefinitionsHelpStrategy
import org.chromia.tools.ChrCompletionHelp
import org.chromia.tools.ChrCompletionHelpStrategy
import org.chromia.tools.ChromiaProjectStructureHelp
import org.chromia.tools.ChromiaProjectStructureHelpStrategy
import org.chromia.tools.ChrMultiSignatureHelp
import org.chromia.tools.ChrMultiSignatureHelpStrategy
import org.chromia.tools.ChrDeployHelp
import org.chromia.tools.ChrDeployHelpStrategy
import org.chromia.tools.ChrNodeHelp
import org.chromia.tools.ChrNodeHelpStrategy
import org.chromia.tools.ChrQueryHelp
import org.chromia.tools.ChrQueryHelpStrategy
import org.chromia.tools.BlockchainPropertiesHelp
import org.chromia.tools.BlockchainPropertiesHelpStrategy
import org.chromia.tools.ChrReplHelp
import org.chromia.tools.ChrReplHelpStrategy
import org.chromia.tools.ChrSeederHelp
import org.chromia.tools.ChrSeederHelpStrategy
import org.chromia.tools.ChrToolsHelp
import org.chromia.tools.ChrToolsHelpStrategy
import org.chromia.tools.ChromiaYmlSections
import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.DappScaffold
import org.chromia.tools.Ft4ModuleArgs
import org.chromia.tools.Ft4ModuleArgsStrategy
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.ValidateChromiaYmlStrategy
import org.chromia.tools.VaultLeaseHelp
import org.chromia.tools.VaultLeaseHelpStrategy
import org.chromia.tools.WriteDeploymentConfig
import org.chromia.tools.WriteDeploymentConfigStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DappBuildToolsTest {

    private val forbidden = listOf(
        "lib.ft4.admin",
        "lib.ft4.core.admin",
        "admin.crosschain",
        "ras_open",
        "ras_transfer_open"
    )

    private fun hasLiveConfig(source: String, module: String): Boolean =
        source.lineSequence().any { line ->
            val trimmed = line.trim()
            !trimmed.startsWith("#") && (
                trimmed == module ||
                    trimmed.startsWith("$module:") ||
                    trimmed.startsWith("import $module")
                )
        }


    private fun goodYml(): String = DappScaffold.files("hello").getValue("chromia.yml")

    @Test
    fun goodYmlIsOk() {
        val result = ChromiaYmlValidator.validate(goodYml())
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(result.ok)
    }

    @Test
    fun officialWebStaticIsAccepted() {
        val yaml = goodYml().replace(
            "module: main",
            "module: main\n    webStatic: out"
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(result.ok)
        assertTrue("webStatic" in ChromiaYmlValidator.officialBlockchainKeys)
        assertFalse(result.warnings.any { it.contains("webStatic") && it.contains("unknown") }, result.warnings.toString())
    }


    @Test
    fun scaffoldYmlValidatesThroughTool() = runBlocking {
        val result = ValidateChromiaYmlStrategy().execute(
            CallToolRequest(
                name = "validate_chromia_yml",
                arguments = buildJsonObject { put("yaml", goodYml()) }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(true, payload["ok"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(0, payload["errors"]!!.jsonArray.size)
        assertEquals(payload, result.structuredContent)
    }

    @Test
    fun badMerkleHashVersionIsError() {
        val yaml = """
            blockchains:
              hello:
                module: main
                config:
                  features:
                    merkle_hash_version: 1
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(
            result.errors.any { it.contains("merkle_hash_version") && it.contains("1") },
            result.errors.toString()
        )
    }

    @Test
    fun missingMerkleHashVersionWarnsByDefaultAndErrorsInStrict() {
        val yaml = """
            blockchains:
              hello:
                module: main
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        // Round 2 D3: chr builds official configs without the pin - warning by
        // default, error only under strict.
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.warnings.any { it.contains("merkle_hash_version") }, result.warnings.toString())

        val strict = ChromiaYmlValidator.validate(yaml, strict = true)
        assertFalse(strict.ok)
        assertTrue(strict.errors.any { it.contains("merkle_hash_version") }, strict.errors.toString())
    }

    @Test
    fun forbiddenAdminLibIsError() {
        val yaml = """
            blockchains:
              hello:
                module: main
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
            libs:
              lib.ft4.admin:
                registry: https://gitlab.com/chromaway/ft4-lib.git
                path: rell/src/lib/ft4
                tagOrBranch: v1.1.0r
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("lib.ft4.admin") }, result.errors.toString())
    }

    @Test
    fun adminModuleArgsKeyIsAccepted() = runBlocking {
        // Round 2 D3: a moduleArgs KEY names the module being CONFIGURED, not
        // imported - setting lib.ft4.core.admin's admin_pubkey is standard
        // documented practice and must validate clean.
        val yaml = """
            blockchains:
              hello:
                module: main
                moduleArgs:
                  lib.ft4.core.admin:
                    admin_pubkey: 03028A31DBA82E46DE26A608249147A6A1A88C62A1A65B640C9B4369D4CAD928BE
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ValidateChromiaYmlStrategy().execute(
            CallToolRequest(
                name = "validate_chromia_yml",
                arguments = buildJsonObject { put("yaml", yaml) }
            ),
            RecordingRepository()
        )
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(true, payload["ok"]!!.jsonPrimitive.content.toBoolean(), payload.toString())
        val errors = payload["errors"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(errors.none { it.contains("lib.ft4.core.admin") }, errors.toString())
    }

    @Test
    fun missingModuleAndStaleRellVersion() {
        val yaml = """
            blockchains:
              hello:
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.14.9
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("module") }, result.errors.toString())
        assertTrue(result.warnings.any { it.contains("0.14.9") && it.contains("0.16.1") }, result.warnings.toString())
    }

    @Test
    fun ft4ModuleArgsNeverEmitsAdminOrRasOpen() = runBlocking {
        val result = Ft4ModuleArgsStrategy().execute(
            CallToolRequest(
                name = "ft4_module_args",
                arguments = buildJsonObject {
                    put("name", "wallet")
                    put("includeIccf", true)
                }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("wallet", payload["name"]!!.jsonPrimitive.content)
        assertEquals("v1.1.0r", payload["ft4Version"]!!.jsonPrimitive.content)
        assertEquals("1", payload["ft4Api"]!!.jsonPrimitive.content)
        assertEquals("default", payload["DEFAULT_LOGIN_CONFIG_NAME"]!!.jsonPrimitive.content)
        assertTrue(payload["require_mandatory_flags"]!!.jsonPrimitive.content.contains("main"))
        val emitted = listOf("libs", "moduleArgs", "yaml").joinToString("\n") { key ->
            payload[key]!!.jsonPrimitive.content
        }
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(emitted.contains("tagOrBranch: v1.1.0r"))
        assertTrue(emitted.contains("lib.ft4.core.accounts"))
        assertTrue(emitted.contains("auth_flags"))
        assertTrue(emitted.contains("mandatory"))
        assertTrue(emitted.contains("auth_descriptor"))
        assertTrue(emitted.contains("max_rules"))
        assertTrue(emitted.contains("query_max_page_size"))
        assertTrue(payload["libs"]!!.jsonPrimitive.content.contains("iccf"))
        assertTrue(payload["libs"]!!.jsonPrimitive.content.contains("com.chromia.iccf"))
        assertTrue(payload["libs"]!!.jsonPrimitive.content.contains("version: 1.90.1"))
        assertTrue(payload["libs"]!!.jsonPrimitive.content.contains("insecure: false"))
        assertTrue(emitted.contains("insecure: false"))
        assertTrue(emitted.contains("net.postchain.d1.iccf.IccfGTXModule"))
        assertEquals("net.postchain.d1.iccf.IccfGTXModule", payload["iccfGtxModule"]!!.jsonPrimitive.content)
        assertEquals("com.chromia.iccf", payload["iccfLibraryChainId"]!!.jsonPrimitive.content)
        assertEquals("1.90.1", payload["iccfLibraryChainVersion"]!!.jsonPrimitive.content)
        assertEquals("1.87.0", payload["iccfGitTag"]!!.jsonPrimitive.content)
        assertTrue(payload["library_chain_yaml"]!!.jsonPrimitive.content.contains("com.chromia.iccf"))
        assertTrue(payload["library_chain_yaml"]!!.jsonPrimitive.content.contains("version: 1.90.1"))
        val gitIccf = payload["iccf_git_yaml"]!!.jsonPrimitive.content
        assertTrue(gitIccf.contains("tagOrBranch: 1.87.0"))
        assertTrue(gitIccf.contains("https://gitlab.com/chromaway/core/directory-chain"))
        assertTrue(gitIccf.contains("src/lib/iccf"))
        assertTrue(gitIccf.contains(Ft4ModuleArgs.ICCF_GIT_RID))
        assertFalse(payload["libs"]!!.jsonPrimitive.content.contains("tagOrBranch: 1.87.0"))
        assertTrue(payload["gtx"]!!.jsonPrimitive.content.contains("IccfGTXModule"))
        assertTrue(payload["yaml"]!!.jsonPrimitive.content.contains("gtx:"))
        assertTrue(payload["yaml"]!!.jsonPrimitive.content.contains("com.chromia.iccf"))
        assertTrue(payload["yaml"]!!.jsonPrimitive.content.contains("merkle_hash_version: 2"))
        assertTrue(payload["yaml"]!!.jsonPrimitive.content.contains("rellVersion: 0.16.1"))
        val validated = ChromiaYmlValidator.validate(payload["yaml"]!!.jsonPrimitive.content)
        assertTrue(validated.ok, validated.errors.toString())
        assertTrue(payload["notes"]!!.jsonPrimitive.content.contains("IccfGTXModule"))
        assertTrue(payload["notes"]!!.jsonPrimitive.content.contains("1.90.1"))
        assertTrue(payload["notes"]!!.jsonPrimitive.content.contains("1.87.0"))
        assertTrue(payload["notes"]!!.jsonPrimitive.content.contains("lib.iccf"))
        val pins = payload["pins"]!!.jsonObject
        assertEquals("1.90.1", pins["iccfLibraryChainVersion"]!!.jsonPrimitive.content)
        assertEquals("1.87.0", pins["iccfGitTag"]!!.jsonPrimitive.content)
        forbidden.forEach { module ->
            assertFalse(hasLiveConfig(emitted, module), "ft4_module_args must not emit $module")
            assertTrue(notes.contains(module), "notes must document forbidden $module")
        }
        assertFalse(emitted.contains("admin_pubkey"))
        assertEquals("https://docs.chromia.com/build/ft4/configuration-values", payload["config_values_docs"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/ft4/setup/imports", payload["imports_docs"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/ft4/releases/ft4", payload["releases_docs"]!!.jsonPrimitive.content)
        assertEquals("1.1.0r", payload["docs_latest_ft4"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("max_auth_descriptor_rules"))
        assertTrue(notes.contains("max_points"))
        assertFalse(notes.contains("03028A31"))
        assertFalse(hasLiveConfig(emitted, "ras_transfer_open"))
        assertEquals(payload, result.structuredContent)
    }

    @Test
    fun ft4ModuleArgsDefaultOmitsIccfAndAdmin() {
        val libs = Ft4ModuleArgs.libsYaml(includeIccf = false)
        val args = Ft4ModuleArgs.moduleArgsYaml()
        assertTrue(libs.contains("tagOrBranch: v1.1.0r"))
        assertTrue(libs.contains("insecure: false"))
        assertFalse(libs.contains("iccf"))
        val noIccf = Ft4ModuleArgs.toJson("hello", false)
        assertEquals("", noIccf["gtx"]!!.jsonPrimitive.content)
        assertEquals("", noIccf["iccfGtxModule"]!!.jsonPrimitive.content)
        assertEquals("", noIccf["iccfLibraryChainId"]!!.jsonPrimitive.content)
        assertEquals("", noIccf["library_chain_yaml"]!!.jsonPrimitive.content)
        assertEquals("", noIccf["iccf_git_yaml"]!!.jsonPrimitive.content)
        assertFalse(noIccf["yaml"]!!.jsonPrimitive.content.contains("IccfGTXModule"))
        assertFalse(noIccf["yaml"]!!.jsonPrimitive.content.contains("com.chromia.iccf"))
        assertTrue(noIccf["yaml"]!!.jsonPrimitive.content.contains("merkle_hash_version: 2"))
        assertTrue(noIccf["yaml"]!!.jsonPrimitive.content.contains("rellVersion: 0.16.1"))
        val validated = ChromiaYmlValidator.validate(noIccf["yaml"]!!.jsonPrimitive.content)
        assertTrue(validated.ok, validated.errors.toString())
        forbidden.forEach { module ->
            assertFalse(libs.contains(module))
            assertFalse(args.contains(module))
        }
        assertFalse(args.contains("admin_pubkey"))
        assertTrue(args.contains("lib.ft4.core.accounts"))
        assertTrue(Ft4ModuleArgs.notes("hello").contains("default"))
        assertTrue(Ft4ModuleArgs.notes("hello").contains("main"))
    }

    @Test
    fun chrBuildHelpReturnsOfficialCommands() = runBlocking {
        val result = ChrBuildHelpStrategy().execute(
            CallToolRequest(name = "chr_build_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr install", commands["install_libs"]!!.jsonPrimitive.content)
        assertEquals("chr build", commands["build"]!!.jsonPrimitive.content)
        assertEquals("chr build --hide-lib-warnings", commands["build_hide_lib_warnings"]!!.jsonPrimitive.content)
        assertEquals("chr build --skip-lib-check", commands["build_skip_lib_check"]!!.jsonPrimitive.content)
        assertEquals("chr build --format=GTV", commands["build_format"]!!.jsonPrimitive.content)
        assertEquals("chr test", commands["test"]!!.jsonPrimitive.content)
        assertEquals("chr code check", commands["code_check"]!!.jsonPrimitive.content)
        assertEquals("chr code lint", commands["code_lint"]!!.jsonPrimitive.content)
        assertEquals("chr code format", commands["code_format"]!!.jsonPrimitive.content)
        assertEquals("chr code check --hide-lib-warnings", commands["code_check_hide_lib_warnings"]!!.jsonPrimitive.content)
        assertEquals("chr test --hide-lib-warnings", commands["test_hide_lib_warnings"]!!.jsonPrimitive.content)
        assertEquals("chr repl --sql-log", commands["repl_sql_log"]!!.jsonPrimitive.content)
        val buildFlags = payload["build_flags"]!!.jsonObject
        assertTrue(buildFlags["hide_lib_warnings"]!!.jsonPrimitive.content.contains("--hide-lib-warnings"))
        assertTrue(buildFlags["skip_lib_check"]!!.jsonPrimitive.content.contains("--skip-lib-check"))
        assertTrue(buildFlags["format"]!!.jsonPrimitive.content.contains("GTV"))
        assertTrue(buildFlags["format"]!!.jsonPrimitive.content.contains("XML"))
        val compileKeys = payload["compile_keys"]!!.jsonObject
        assertTrue(compileKeys["rellVersion"]!!.jsonPrimitive.content.contains("0.16.1"))
        assertTrue(compileKeys["source"]!!.jsonPrimitive.content.contains("src"))
        assertTrue(compileKeys["target"]!!.jsonPrimitive.content.contains("build"))
        assertTrue(compileKeys["strictGtvConversion"]!!.jsonPrimitive.content.contains("true"))
        val codeFlags = payload["code_flags"]!!.jsonObject
        assertTrue(codeFlags["check_hide_lib_warnings"]!!.jsonPrimitive.content.contains("--hide-lib-warnings"))
        assertTrue(codeFlags["lint_fix"]!!.jsonPrimitive.content.contains("--fix"))
        val testFlags = payload["test_flags"]!!.jsonObject
        assertTrue(testFlags["hide_lib_warnings"]!!.jsonPrimitive.content.contains("--hide-lib-warnings"))
        assertTrue(testFlags["db"]!!.jsonPrimitive.content.contains("--no-db"))
        assertTrue(testFlags["fail_on_error"]!!.jsonPrimitive.content.contains("--fail-on-error"))
        assertFalse(testFlags.toString().contains("--sql-log"))
        val install = payload["install"]!!.jsonObject
        assertTrue(install["macos"]!!.jsonPrimitive.content.contains("homebrew-chromia.git"))
        assertTrue(install["linux"]!!.jsonPrimitive.content.contains("apt.chromia.com"))
        assertTrue(install["windows"]!!.jsonPrimitive.content.contains("scoop-chromia.git"))
        val shape = payload["chromia_yml_shape"]!!.jsonPrimitive.content
        assertTrue(shape.contains("merkle_hash_version: 2"))
        assertTrue(shape.contains("rellVersion: 0.16.1"))
        assertTrue(shape.contains("v1.1.0r"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("no top-level `chr compile`") || notes.contains("no top-level"))
        assertTrue(notes.contains("chr code lint"))
        assertTrue(notes.contains("chr code format"))
        assertTrue(notes.contains("--hide-lib-warnings"))
        assertTrue(notes.contains("--format=(GTV|XML)") || notes.contains("--format"))
        assertTrue(notes.contains("strictGtvConversion"))
        assertTrue(notes.contains("compile.source") || notes.contains("source is src"))
        assertTrue(notes.contains("chr_repl_help"))
        assertTrue(notes.contains("/build/cli/introduction"))
        assertTrue(notes.contains("cli-release-notes"))
        assertTrue(notes.contains("0.30.0"))
        assertEquals("0.30.0", payload["docs_latest_cli"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/build",
            payload["build_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/build",
            payload["build_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/build/",
            payload["build_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("build", payload["build_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/code",
            payload["code_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/code",
            payload["code_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/code/",
            payload["code_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("code", payload["code_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("chr build [<options>]"))
        assertTrue(notes.contains("https://docs.chromia.com/build/cli/commands/build/"))
        assertTrue(notes.contains("Official BUILD cli/commands/build"))
        assertTrue(notes.contains("Official BUILD cli/commands/code"))
        assertTrue(notes.contains("https://docs.chromia.com/build/cli/commands/code/"))
        assertFalse(notes.contains("execute_transaction"))
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrBuildHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrReplHelpIsOfficialFlags() = runBlocking {
        val result = ChrReplHelpStrategy().execute(
            CallToolRequest(name = "chr_repl_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chr_repl_help", payload["tool"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr repl", commands["repl"]!!.jsonPrimitive.content)
        assertEquals("chr repl --sql-log", commands["sql_log"]!!.jsonPrimitive.content)
        assertEquals("chr repl --sql-log --use-db --module main", commands["sql_log_analyze"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/rell/analyze-rell-dapp-code",
            payload["analyze_docs"]!!.jsonPrimitive.content
        )
        assertTrue(payload["analyze_optimized_query"]!!.jsonPrimitive.content.contains("get_house_key"))
        assertTrue(commands["module"]!!.jsonPrimitive.content.contains("--module"))
        assertTrue(commands["use_db"]!!.jsonPrimitive.content.contains("--use-db"))
        val flags = payload["flags"]!!.jsonObject
        assertTrue(flags["sql_log"]!!.jsonPrimitive.content.contains("--sql-log"))
        assertTrue(flags["module"]!!.jsonPrimitive.content.contains("not a file path"))
        assertTrue(flags["use_db"]!!.jsonPrimitive.content.contains("--use-db"))
        assertTrue(flags["output_format"]!!.jsonPrimitive.content.contains("YAML"))
        assertTrue(flags["raw_output"]!!.jsonPrimitive.content.contains("deprecated"))
        assertTrue(payload["sql_log_from_test"]!!.jsonPrimitive.content.contains("0.31.0"))
        assertEquals("rell.test.tx(<operation>...).run()", payload["operation_wrapper"]!!.jsonPrimitive.content)
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, commands.toString(), flags.toString()).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("--sql-log"))
        assertTrue(notes.contains("--use-db"))
        assertTrue(notes.contains("analyze-rell-dapp-code"))
        assertTrue(notes.contains("house-key-example"))
        assertTrue(notes.contains("0.31.0"))
        assertTrue(notes.contains("experimental"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrReplHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrToolsHelpIsOfficialFlags() = runBlocking {
        val result = ChrToolsHelpStrategy().execute(
            CallToolRequest(name = "chr_tools_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chr_tools_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/tools",
            payload["tools_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/tools/",
            payload["tools_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("tools", payload["tools_index_title"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr tools", commands["tools"]!!.jsonPrimitive.content)
        assertEquals("chr tools gtv", commands["gtv"]!!.jsonPrimitive.content)
        assertEquals("chr gtv", commands["gtv_alias_of"]!!.jsonPrimitive.content)
        assertTrue(commands["validate_config"]!!.jsonPrimitive.content.contains("validate-config"))
        assertTrue(commands["lib_model"]!!.jsonPrimitive.content.contains("lib-model"))
        val gtvFlags = payload["gtv_flags"]!!.jsonObject
        assertTrue(gtvFlags["hex"]!!.jsonPrimitive.content.contains("--hex"))
        assertTrue(gtvFlags["output_format"]!!.jsonPrimitive.content.contains("YAML"))
        assertTrue(gtvFlags["hash"]!!.jsonPrimitive.content.contains("--hash"))
        val validateFlags = payload["validate_config_flags"]!!.jsonObject
        assertTrue(validateFlags["file"]!!.jsonPrimitive.content.contains("--file"))
        val libFlags = payload["lib_model_flags"]!!.jsonObject
        assertTrue(libFlags["library_source"]!!.jsonPrimitive.content.contains("--library-source"))
        assertTrue(libFlags["insecure"]!!.jsonPrimitive.content.contains("false"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, commands.toString(), gtvFlags.toString(), libFlags.toString()).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("validate-config"))
        assertTrue(notes.contains("lib-model"))
        assertTrue(notes.contains("Official BUILD cli/commands/tools"))
        assertTrue(notes.contains("https://docs.chromia.com/build/cli/commands/tools/"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrToolsHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrSeederHelpIsOfficialFlags() = runBlocking {
        val result = ChrSeederHelpStrategy().execute(
            CallToolRequest(name = "chr_seeder_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chr_seeder_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/seeder",
            payload["commands_seeder_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/seeder/",
            payload["commands_seeder_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("seeder", payload["commands_seeder_index_title"]!!.jsonPrimitive.content)
        assertEquals(true, payload["early_stage"]!!.jsonPrimitive.boolean)
        assertEquals(".chromia/seeder", payload["default_config_folder"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr seeder", commands["seeder"]!!.jsonPrimitive.content)
        assertEquals("chr seeder init", commands["init"]!!.jsonPrimitive.content)
        assertEquals("chr seeder generate", commands["generate"]!!.jsonPrimitive.content)
        val initFlags = payload["init_flags"]!!.jsonObject
        assertTrue(initFlags["blockchain"]!!.jsonPrimitive.content.contains("--blockchain"))
        val generateFlags = payload["generate_flags"]!!.jsonObject
        assertTrue(generateFlags["alternative_config_folder"]!!.jsonPrimitive.content.contains("--alternative-config-folder"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, commands.toString(), initFlags.toString(), generateFlags.toString()).joinToString("\n")
        assertTrue(notes.contains("early-stage"))
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("do not invent keys") || notes.contains("Do not invent keys"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrSeederHelp.toJson(), result.structuredContent)
    }

    @Test
    fun blockchainPropertiesHelpIsOfficialKeys() = runBlocking {
        val result = BlockchainPropertiesHelpStrategy().execute(
            CallToolRequest(name = "blockchain_properties_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("blockchain_properties_help", payload["tool"]!!.jsonPrimitive.content)
        val keys = payload["keys"]!!.jsonObject
        val gtx = keys["gtx"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("max_transaction_size" in gtx)
        assertTrue("modules" in gtx)
        val blockstrategy = keys["blockstrategy"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("maxblocksize" in blockstrategy)
        assertTrue("mininterblockinterval" in blockstrategy)
        assertTrue("maxtxdelay" in blockstrategy)
        val core = keys["core"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("query_timeout_seconds" in core)
        assertTrue("query_cache_ttl_seconds" in core)
        assertTrue("async_query_timeout_seconds" in core)
        val modules = payload["allowed_gtx_modules"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("net.postchain.rell.module.RellPostchainModuleFactory" in modules)
        assertTrue("net.postchain.gtx.StandardOpsGTXModule" in modules)
        assertTrue("net.postchain.eif.EifGTXModule" in modules)
        assertFalse("net.postchain.gtx.extensions.vectordb.VectorDbGTXModule" in modules)
        val yaml = payload["config_yaml"]!!.jsonPrimitive.content
        assertTrue(yaml.contains("merkle_hash_version: 2"))
        assertTrue(yaml.contains("maxblocksize: 27262976"))
        assertTrue(yaml.contains("mininterblockinterval: 1000"))
        assertTrue(yaml.contains("max_transaction_size: 26214400"))
        assertTrue(yaml.contains("query_timeout_seconds: 60"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, yaml, keys.toString(), modules.toString()).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("Do not invent GTX module"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("theme"))
        assertFalse(allText.contains("VectorDbGTXModule"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(BlockchainPropertiesHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrEifHelpIsOfficialFlags() = runBlocking {
        val result = ChrEifHelpStrategy().execute(
            CallToolRequest(name = "chr_eif_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chr_eif_help", payload["tool"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr eif", commands["eif"]!!.jsonPrimitive.content)
        assertEquals("chr eif generate-events-config", commands["generate_events_config"]!!.jsonPrimitive.content)
        assertTrue(commands["example"]!!.jsonPrimitive.content.contains("--abi"))
        val flags = payload["flags"]!!.jsonObject
        assertTrue(flags["abi"]!!.jsonPrimitive.content.contains("--abi"))
        assertTrue(flags["events"]!!.jsonPrimitive.content.contains("--events"))
        assertTrue(flags["target"]!!.jsonPrimitive.content.contains("--target"))
        assertTrue(flags["format"]!!.jsonPrimitive.content.contains("XML"))
        assertTrue(flags["format"]!!.jsonPrimitive.content.contains("YAML"))
        assertEquals("build/eif-events.yaml", payload["default_target"]!!.jsonPrimitive.content)
        assertEquals("net.postchain.eif.EifGTXModule", payload["eif_gtx_module"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/eif",
            payload["eif_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/eif/",
            payload["eif_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("eif", payload["eif_index_title"]!!.jsonPrimitive.content)
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, commands.toString(), flags.toString()).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("generate-events-config"))
        assertTrue(notes.contains("Official BUILD cli/commands/eif"))
        assertTrue(notes.contains("https://docs.chromia.com/build/cli/commands/eif/"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic phrase"))
        assertTrue(notes.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrEifHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaYmlDefinitionsHelpIsOfficialExamples() = runBlocking {
        val result = ChromiaYmlDefinitionsHelpStrategy().execute(
            CallToolRequest(name = "chromia_yml_definitions_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chromia_yml_definitions_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/configuration/project-config", payload["project_config"]!!.jsonPrimitive.content)
        val anchors = payload["anchors_yaml"]!!.jsonPrimitive.content
        assertTrue(anchors.contains("definitions:"))
        assertTrue(anchors.contains("&test"))
        assertTrue(anchors.contains("*test"))
        assertTrue(anchors.contains("test.arithmetic_test"))
        assertTrue(anchors.contains("test.data_test"))
        val includeSrc = payload["include_source_yaml"]!!.jsonPrimitive.content
        assertTrue(includeSrc.contains("a: 13"))
        assertTrue(includeSrc.contains("b: 15"))
        val whole = payload["include_whole_file_yaml"]!!.jsonPrimitive.content
        assertTrue(whole.contains("!include test.yml"))
        assertFalse(whole.contains("!include test.yml#"))
        val wholeResult = payload["include_whole_file_result_yaml"]!!.jsonPrimitive.content
        assertTrue(wholeResult.contains("a: 13"))
        assertTrue(wholeResult.contains("b: 15"))
        val tag = payload["include_tag_yaml"]!!.jsonPrimitive.content
        assertTrue(tag.contains("!include test.yml#a"))
        val tagResult = payload["include_tag_result_yaml"]!!.jsonPrimitive.content
        assertTrue(tagResult.contains("modules: 13"))
        assertEquals("!include test.yml", payload["include_whole"]!!.jsonPrimitive.content)
        assertEquals("!include test.yml#a", payload["include_tag"]!!.jsonPrimitive.content)
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, anchors, whole, tag, wholeResult, tagResult).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("Do not invent include"))
        assertTrue(notes.contains("definitions"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("<<:"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaYmlDefinitionsHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrCompletionHelpIsOfficialFlags() = runBlocking {
        val result = ChrCompletionHelpStrategy().execute(
            CallToolRequest(name = "chr_completion_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chr_completion_help", payload["tool"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr help", commands["help"]!!.jsonPrimitive.content)
        assertEquals("chr help [<options>]", commands["help_usage"]!!.jsonPrimitive.content)
        assertEquals("chr version", commands["version"]!!.jsonPrimitive.content)
        assertEquals("chr version [<options>]", commands["version_usage"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/help",
            payload["help_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/help",
            payload["help_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/help/",
            payload["help_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("help", payload["help_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/version",
            payload["version_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/introduction",
            payload["intro_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/cli-release-notes",
            payload["release_notes_docs"]!!.jsonPrimitive.content
        )
        assertEquals("0.30.0", payload["docs_latest_cli"]!!.jsonPrimitive.content)
        assertEquals("0.33.x", payload["source_cli"]!!.jsonPrimitive.content)
        assertEquals("chr query hello_world", commands["official_local_query"]!!.jsonPrimitive.content)
        assertTrue(payload["help_flags"]!!.jsonObject["help"]!!.jsonPrimitive.content.contains("-h, --help"))
        assertTrue(payload["version_flags"]!!.jsonObject["help"]!!.jsonPrimitive.content.contains("-h, --help"))
        assertTrue(commands["completion_bash"]!!.jsonPrimitive.content.contains("--generate-completion bash"))
        assertTrue(commands["completion_zsh"]!!.jsonPrimitive.content.contains("--generate-completion zsh"))
        assertTrue(commands["completion_fish"]!!.jsonPrimitive.content.contains("--generate-completion fish"))
        assertEquals("chr de cr", commands["shortcut_example"]!!.jsonPrimitive.content)
        assertEquals("chr deployment create", commands["shortcut_equals"]!!.jsonPrimitive.content)
        val shells = payload["shells"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("bash", "zsh", "fish"), shells)
        val skipped = payload["skipped"]!!.jsonObject
        assertTrue(skipped["fetch_config"]!!.jsonPrimitive.content.contains("hidden"))
        assertTrue(skipped["deployment_lease_info"]!!.jsonPrimitive.content.contains("hidden"))
        assertTrue(skipped["deployment_remove_container"]!!.jsonPrimitive.content.contains("signed"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, commands.toString(), skipped.toString()).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("generate-completion"))
        assertTrue(notes.contains("/build/cli/introduction"))
        assertTrue(notes.contains("cli-release-notes"))
        assertTrue(notes.contains("0.30.0"))
        assertTrue(notes.contains("cannot contain hyphens"))
        assertTrue(notes.contains("chr help [<options>]"))
        assertTrue(notes.contains("-h, --help"))
        assertTrue(notes.contains("fetch-config"))
        assertTrue(notes.contains("lease-info"))
        assertTrue(notes.contains("remove-container"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic phrase"))
        assertTrue(notes.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrCompletionHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaProjectStructureHelpIsOfficialLayouts() = runBlocking {
        val result = ChromiaProjectStructureHelpStrategy().execute(
            CallToolRequest(name = "chromia_project_structure_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chromia_project_structure_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/configuration/project-structure", payload["docs"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/rell/modules", payload["modules_docs"]!!.jsonPrimitive.content)
        val create = payload["create_rell_dapp_layout"]!!.jsonPrimitive.content
        assertTrue(create.contains("chromia.yml"))
        assertTrue(create.contains("main.rell"))
        assertTrue(create.contains("arithmetic_test.rell"))
        assertTrue(create.contains("data_test.rell"))
        val multi = payload["multi_file_layout"]!!.jsonPrimitive.content
        assertTrue(multi.contains("module_a"))
        assertTrue(multi.contains("module.rell"))
        assertTrue(multi.contains("operations.rell"))
        val rec = payload["recommended_app_layout"]!!.jsonPrimitive.content
        assertTrue(rec.contains("app"))
        assertTrue(rec.contains("entities.rell"))
        assertTrue(rec.contains("structs.rell"))
        val single = payload["single_file_module"]!!.jsonPrimitive.content
        assertTrue(single.contains("module;"))
        val imports = payload["import_examples"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("import app.single;" in imports)
        assertTrue("import alias: app.multi;" in imports)
        assertTrue("import foo.*;" in imports)
        assertTrue("import foo.{a: f, b: g};" in imports)
        assertTrue(payload["entry_point_rule"]!!.jsonPrimitive.content.contains("not a file path"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, create, multi, rec, single, imports.toString()).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("never a file path") || notes.contains("not a file path"))
        assertTrue(notes.contains("lib.ft4.admin") || notes.contains("NEVER ship"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaProjectStructureHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrMultiSignatureHelpIsReadOnlyView() = runBlocking {
        val result = ChrMultiSignatureHelpStrategy().execute(
            CallToolRequest(name = "chr_multi_signature_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chr_multi_signature_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/cli/commands/multi-signature", payload["docs"]!!.jsonPrimitive.content)
        assertEquals(
            ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_URL,
            payload["multi_signature_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/multi-signature/",
            payload["multi_signature_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("multi-signature", payload["multi_signature_index_title"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr multi-signature view --file <transaction-file>", commands["view"]!!.jsonPrimitive.content)
        assertFalse(commands.containsKey("create"))
        assertFalse(commands.containsKey("sign"))
        assertFalse(commands.containsKey("send"))
        val flags = payload["flags"]!!.jsonObject
        val view = flags["view"]!!.jsonObject
        assertTrue(view["file"]!!.jsonPrimitive.content.contains("--file"))
        assertFalse(view.containsKey("key_id"))
        assertFalse(view.containsKey("secret"))
        val skipped = payload["skipped"]!!.jsonObject
        assertTrue(skipped["create"]!!.jsonPrimitive.content.contains("sign"))
        assertTrue(skipped["sign"]!!.jsonPrimitive.content.contains("sign"))
        assertTrue(skipped["send"]!!.jsonPrimitive.content.contains("send"))
        assertTrue(skipped["tx"]!!.jsonPrimitive.content.contains("chr tx"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, commands.toString(), flags.toString(), skipped.toString()).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr"))
        assertTrue(notes.contains("multi-signature view"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrMultiSignatureHelp.toJson(), result.structuredContent)
    }

    @Test
    fun buildToolsAreRegisteredAndPrompted() {
        val names = McpTools.allTools().map { it.name }.toSet()
        assertTrue("validate_chromia_yml" in names)
        assertTrue("ft4_module_args" in names)
        assertTrue("chr_build_help" in names)
        assertTrue("chr_repl_help" in names)
        assertTrue("chr_tools_help" in names)
        assertTrue("chr_seeder_help" in names)
        assertTrue("blockchain_properties_help" in names)
        assertTrue("chr_eif_help" in names)
        assertTrue("chromia_yml_definitions_help" in names)
        assertTrue("chr_completion_help" in names)
        assertTrue("chromia_project_structure_help" in names)
        assertTrue("chr_multi_signature_help" in names)
        assertTrue("write_deployment_config" in names)
        assertTrue("chr_deploy_help" in names)
        assertTrue("chr_node_help" in names)
        assertTrue("chr_query_help" in names)
        assertTrue("vault_lease_help" in names)
        assertTrue("chr_generate_client_help" in names)
        assertTrue("chromia_docs_yml_help" in names)
        assertTrue("chr_library_help" in names)
        assertTrue("chr_create_rell_dapp_help" in names)
        assertTrue("check_dapp_project" in names)
        assertTrue("check_ft4_imports" in names)
        assertTrue("chromia_cookbook_help" in names)
        assertTrue("chr_key_id_help" in names)
        assertTrue("chromia_language_clients_help" in names)
        assertTrue("chromia_rell_language_help" in names)
        assertTrue("chromia_rell_types_help" in names)
        assertTrue("chromia_rell_expressions_help" in names)
        assertTrue("chromia_rell_statements_help" in names)
        assertTrue("chromia_rell_database_help" in names)
        assertTrue("chromia_rell_systemlib_help" in names)
        assertTrue("chromia_rell_practices_help" in names)
        assertTrue("chromia_ft4_queries_help" in names)
        assertTrue("chromia_integrations_help" in names)
        assertTrue("chromia_vector_search_help" in names)
        val prompts = PromptManager()
        assertEquals(
            "validate_chromia_yml",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Validate chromia.yml")!!))
        )
        assertEquals(
            "ft4_module_args",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "FT4 module_args")!!))
        )
        assertEquals(
            "chr_build_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI build help")!!))
        )
        assertEquals(
            "chr_repl_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI repl help")!!))
        )
        assertEquals(
            "chr_tools_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI tools help")!!))
        )
        assertEquals(
            "chr_seeder_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI seeder help")!!))
        )
        assertEquals(
            "blockchain_properties_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "chromia.yml blockchain-properties help")!!))
        )
        assertEquals(
            "chr_eif_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI eif help")!!))
        )
        assertEquals(
            "chromia_yml_definitions_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "chromia.yml definitions / YAML include help")!!))
        )
        assertEquals(
            "chr_completion_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI completion help")!!))
        )
        assertEquals(
            "chromia_project_structure_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "chromia.yml project-structure / Rell modules help")!!))
        )
        assertEquals(
            "chr_multi_signature_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI multi-signature view help")!!))
        )
        assertEquals(
            "write_deployment_config",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Write deployment config")!!))
        )
        assertEquals(
            "chr_deploy_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI deploy help")!!))
        )
        assertEquals(
            "chr_node_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI node help")!!))
        )
        assertEquals(
            "chr_query_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI query help")!!))
        )
        assertEquals(
            "vault_lease_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Vault / PMC lease help")!!))
        )
        assertEquals(
            "chr_generate_client_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI generate-client help")!!))
        )
        assertEquals(
            "chromia_docs_yml_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "chromia.yml docs section help")!!))
        )
        assertEquals(
            "chr_library_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI library help")!!))
        )
        assertEquals(
            "chr_create_rell_dapp_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI create-rell-dapp help")!!))
        )
        assertEquals(
            "check_dapp_project",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Check dapp project")!!))
        )
        assertEquals(
            "check_ft4_imports",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Check FT4 imports")!!))
        )
        assertEquals(
            "chromia_cookbook_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia cookbook help")!!))
        )
        assertEquals(
            "chr_key_id_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI existing-key reference")!!))
        )
        assertEquals(
            "chromia_language_clients_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia language clients help")!!))
        )
        assertEquals(
            "chromia_rell_language_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell language definition help")!!))
        )
        assertEquals(
            "chromia_rell_types_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell types help")!!))
        )
        assertEquals(
            "chromia_rell_expressions_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell expressions help")!!))
        )
        assertEquals(
            "chromia_rell_statements_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell statements help")!!))
        )
        assertEquals(
            "chromia_rell_database_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell database language help")!!))
        )
        assertEquals(
            "chromia_rell_systemlib_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell system library help")!!))
        )
        assertEquals(
            "chromia_rell_practices_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell security and best-practices help")!!))
        )
        assertEquals(
            "chromia_ft4_queries_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "FT4 read-only query catalog")!!))
        )
        assertEquals(
            "chromia_integrations_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia integrations hub help")!!))
        )
        assertEquals(
            "chromia_vector_search_help",
            prompts.canonicalToolName(prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia vector-search help")!!))
        )
        val ft4Prompt = prompts.getPrompt("dapp_build", "FT4 module_args")!!["prompt"]!!.jsonPrimitive.content
        assertTrue(ft4Prompt.contains("v1.1.0r"))
        assertTrue(ft4Prompt.contains("lib.ft4.admin"))
        assertTrue(ft4Prompt.contains("ras_open"))
    }

    @Test
    fun writeDeploymentConfigTestnetShape() = runBlocking {
        val result = WriteDeploymentConfigStrategy().execute(
            CallToolRequest(
                name = "write_deployment_config",
                arguments = buildJsonObject {
                    put("network", "testnet")
                    put("name", "wallet")
                }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("testnet", payload["network"]!!.jsonPrimitive.content)
        assertEquals("wallet", payload["name"]!!.jsonPrimitive.content)
        assertEquals("https://node0.testnet.chromia.com:7740", payload["url"]!!.jsonPrimitive.content)
        assertEquals(
            """x"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}"""",
            payload["brid"]!!.jsonPrimitive.content
        )
        assertEquals(WriteDeploymentConfig.TESTNET_DIRECTORY_BRID, payload["directoryBrid"]!!.jsonPrimitive.content)
        val yaml = payload["yaml"]!!.jsonPrimitive.content
        val full = payload["chromia_yml"]!!.jsonPrimitive.content
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(yaml.contains("deployments:"))
        assertTrue(yaml.contains("testnet:"))
        assertTrue(yaml.contains("url:"))
        WriteDeploymentConfig.TESTNET_URLS.forEach { url ->
            assertTrue(yaml.contains(url), "testnet yaml missing $url")
        }
        assertFalse(yaml.contains("url: https://node0.testnet.chromia.com:7740"))
        assertTrue(notes.contains("node0") && notes.contains("node3"))
        assertTrue(notes.contains("Do not invent a single required URL") || notes.contains("do not invent a single required URL"))
        assertTrue(yaml.contains("""brid: x"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}""""))
        assertTrue(yaml.contains("chains:"))
        assertTrue(yaml.contains("wallet:"))
        assertTrue(yaml.contains("container: <containerIID>"))
        assertTrue(full.contains("merkle_hash_version: 2"))
        assertTrue(full.contains("rellVersion: 0.16.1"))
        assertTrue(notes.contains("chr deployment create"))
        assertTrue(notes.contains("writes"))
        assertTrue(notes.contains("does not send signed", ignoreCase = true) || notes.contains("does not send signed"))
        assertFalse(notes.contains("execute_transaction"))
        forbidden.forEach { module ->
            assertFalse(hasLiveConfig(yaml, module), "deployments yaml must not emit $module")
            assertFalse(hasLiveConfig(full, module), "full yml must not emit $module")
            assertTrue(notes.contains(module), "notes must document forbidden $module")
        }
        val validated = ChromiaYmlValidator.validate(full)
        assertTrue(validated.ok, validated.errors.toString())
        assertTrue(validated.warnings.none { it.contains("container") }, validated.warnings.toString())
        assertEquals(payload, result.structuredContent)
    }

    @Test
    fun writeDeploymentConfigMainnetShape() = runBlocking {
        val result = WriteDeploymentConfigStrategy().execute(
            CallToolRequest(
                name = "write_deployment_config",
                arguments = buildJsonObject { put("network", "MAINNET") }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("mainnet", payload["network"]!!.jsonPrimitive.content)
        assertEquals("hello", payload["name"]!!.jsonPrimitive.content)
        assertEquals("https://system.chromaway.com", payload["url"]!!.jsonPrimitive.content)
        assertEquals(WriteDeploymentConfig.MAINNET_DIRECTORY_BRID, payload["directoryBrid"]!!.jsonPrimitive.content)
        val yaml = payload["yaml"]!!.jsonPrimitive.content
        val full = payload["chromia_yml"]!!.jsonPrimitive.content
        assertTrue(yaml.contains("mainnet:"))
        assertTrue(yaml.contains("url:"))
        assertEquals(
            listOf(
                "https://system.chromaway.com",
                "https://mainnet-dapp1.sunube.net:7740"
            ),
            WriteDeploymentConfig.MAINNET_URLS
        )
        WriteDeploymentConfig.MAINNET_URLS.forEach { url ->
            assertTrue(yaml.contains(url), "mainnet yaml missing $url")
        }
        WriteDeploymentConfig.MAINNET_EXPLORER_SNAPSHOT_URLS.forEach { url ->
            assertFalse(yaml.contains(url), "explorer snapshot host must not be required in yaml: $url")
        }
        assertFalse(yaml.contains("url: https://system.chromaway.com"))
        val snapshot = payload["explorer_snapshot_urls"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(WriteDeploymentConfig.MAINNET_EXPLORER_SNAPSHOT_URLS, snapshot)
        assertTrue(payload["explorer_snapshot_note"]!!.jsonPrimitive.content.contains("not required"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("https://system.chromaway.com"), notes)
        assertTrue(notes.contains("https://mainnet-dapp1.sunube.net:7740"), notes)
        assertTrue(notes.contains("explorer snapshot") || notes.contains("not required"), notes)
        assertTrue(notes.contains("string") && notes.contains("list"), notes)
        assertFalse(notes.contains("validatrium") && yaml.contains("validatrium"))
        assertTrue(yaml.contains("""brid: x"${WriteDeploymentConfig.MAINNET_DIRECTORY_BRID}""""))
        assertTrue(yaml.contains("hello:"))
        assertTrue(full.contains("merkle_hash_version: 2"))
        assertFalse(full.contains("merkle_hash_version: 1"))
        forbidden.forEach { module ->
            assertFalse(hasLiveConfig(yaml + "\n" + full, module))
        }
        assertTrue(ChromiaYmlValidator.validate(full).ok)
        assertEquals(payload, result.structuredContent)
    }

    @Test
    fun writeDeploymentConfigUnknownNetworkIsError() = runBlocking {
        val result = WriteDeploymentConfigStrategy().execute(
            CallToolRequest(
                name = "write_deployment_config",
                arguments = buildJsonObject { put("network", "devnet") }
            ),
            RecordingRepository()
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Unknown network"), text)
        assertTrue(text.contains("devnet"), text)
        assertTrue(text.contains("testnet") && text.contains("mainnet"), text)
        assertEquals("Unknown network: devnet. Use testnet or mainnet.", result.structuredContent!!["error"]!!.jsonPrimitive.content)
    }

    @Test
    fun validateAcceptsDeploymentsBlockAndErrorsWrongBridLength() {
        val yaml = """
            blockchains:
              hello:
                module: main
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
            deployments:
              testnet:
                url: https://node0.testnet.chromia.com:7740
                brid: x"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}"
                chains:
                  hello:
        """.trimIndent()
        val ok = ChromiaYmlValidator.validate(yaml)
        assertTrue(ok.ok, ok.errors.toString())
        assertTrue(ok.warnings.none { it.contains("brid") }, ok.warnings.toString())

        val shortBrid = """
            blockchains:
              hello:
                module: main
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
            deployments:
              testnet:
                url: https://node0.testnet.chromia.com:7740
                brid: x"DEADBEEF"
        """.trimIndent()
        val warned = ChromiaYmlValidator.validate(shortBrid)
        assertFalse(warned.ok, warned.errors.toString())
        assertTrue(
            warned.errors.any { it.contains("brid") && (it.contains("64") || it.contains("length")) },
            warned.errors.toString()
        )
    }

    @Test
    fun validateWarnsMissingMerkleOnChainConfigWhenDeploymentsPresent() {
        val yaml = """
            blockchains:
              hello:
                module: main
            compile:
              rellVersion: 0.16.1
            deployments:
              testnet:
                url: https://node0.testnet.chromia.com:7740
                brid: x"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}"
        """.trimIndent()
        // Round 2 D3: a missing merkle pin is ONE warning by default (chr
        // builds such configs) - not a global error plus a per-chain warning.
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertEquals(
            1,
            (result.errors + result.warnings).count { it.contains("merkle_hash_version") },
            "one finding, not a global+per-chain double report: ${result.warnings} ${result.errors}"
        )
        assertTrue(result.warnings.any { it.contains("merkle_hash_version") }, result.warnings.toString())
    }

    @Test
    fun chrDeployHelpReturnsOfficialFlags() = runBlocking {
        val result = ChrDeployHelpStrategy().execute(
            CallToolRequest(name = "chr_deploy_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr deployment create --settings chromia.yml --network testnet --blockchain hello", commands["create"]!!.jsonPrimitive.content)
        assertEquals("chr deployment update --settings chromia.yml --network testnet --blockchain hello", commands["update"]!!.jsonPrimitive.content)
        assertEquals("chr deployment inspect --settings chromia.yml --network testnet --blockchain hello", commands["inspect"]!!.jsonPrimitive.content)
        assertEquals("chr deployment info --settings chromia.yml --network testnet --blockchain hello", commands["info"]!!.jsonPrimitive.content)
        assertEquals("chr deployment proposal list --settings chromia.yml --network testnet", commands["proposal_list"]!!.jsonPrimitive.content)
        assertEquals("chr deployment proposal info --settings chromia.yml --network testnet --id <id>", commands["proposal_info"]!!.jsonPrimitive.content)
        assertEquals("chr deployment voterset list --settings chromia.yml --network testnet", commands["voterset_list"]!!.jsonPrimitive.content)
        assertEquals("chr deployment voterset info --settings chromia.yml --network testnet --name <voter-set>", commands["voterset_info"]!!.jsonPrimitive.content)
        val flags = payload["flags"]!!.jsonObject
        val create = flags["create"]!!.jsonObject
        val update = flags["update"]!!.jsonObject
        val inspect = flags["inspect"]!!.jsonObject
        val info = flags["info"]!!.jsonObject
        val proposalList = flags["proposal_list"]!!.jsonObject
        val proposalInfo = flags["proposal_info"]!!.jsonObject
        val votersetInfo = flags["voterset_info"]!!.jsonObject
        val votersetList = flags["voterset_list"]!!.jsonObject
        assertTrue(create["yes"]!!.jsonPrimitive.content.contains("-y"))
        assertTrue(create["key_id"]!!.jsonPrimitive.content.contains("--key-id"))
        assertTrue(create["key_id"]!!.jsonPrimitive.content.contains("does not generate a key"))
        assertTrue(update["key_id"]!!.jsonPrimitive.content.contains("--key-id"))
        assertTrue(update["verify_only"]!!.jsonPrimitive.content.contains("--verify-only"))
        assertTrue(update["skip_verification"]!!.jsonPrimitive.content.contains("--skip-verification"))
        assertTrue(update["height"]!!.jsonPrimitive.content.contains("--height"))
        assertTrue(inspect["definitions"]!!.jsonPrimitive.content.contains("queries"))
        assertTrue(inspect["module_args"]!!.jsonPrimitive.content.contains("--module-args"))
        assertTrue(info["verbose"]!!.jsonPrimitive.content.contains("--verbose"))
        assertTrue(info["output_format"]!!.jsonPrimitive.content.contains("table|JSON"))
        assertFalse(info.containsKey("key_id"))
        assertFalse(info.containsKey("secret"))
        assertTrue(proposalList["all"]!!.jsonPrimitive.content.contains("--all"))
        assertTrue(proposalList["pending"]!!.jsonPrimitive.content.contains("--pending"))
        assertTrue(proposalList["from"]!!.jsonPrimitive.content.contains("YYYY-MM-DD"))
        assertFalse(proposalList.containsKey("key_id"))
        assertFalse(proposalList.containsKey("secret"))
        assertTrue(proposalInfo["id"]!!.jsonPrimitive.content.contains("--id"))
        assertFalse(proposalInfo.containsKey("key_id"))
        assertTrue(votersetInfo["name"]!!.jsonPrimitive.content.contains("--name"))
        assertTrue(votersetInfo["container"]!!.jsonPrimitive.content.contains("--container"))
        assertFalse(votersetInfo.containsKey("key_id"))
        assertTrue(votersetList["container"]!!.jsonPrimitive.content.contains("--container"))
        assertFalse(votersetList.containsKey("key_id"))
        assertFalse(flags.containsKey("proposal_vote"))
        assertFalse(flags.containsKey("proposal_retract_vote"))
        assertFalse(flags.containsKey("proposal_revoke"))
        assertFalse(flags.containsKey("proposal_rename"))
        assertFalse(flags.containsKey("voterset_update"))
        assertFalse(flags.containsKey("voterset_add_dapp_provider"))
        val writeBack = payload["write_back"]!!.jsonPrimitive.content
        val schema = payload["schema_compare"]!!.jsonPrimitive.content
        val notes = payload["notes"]!!.jsonPrimitive.content
        val container = payload["container"]!!.jsonPrimitive.content
        val allText = listOf(
            writeBack,
            schema,
            notes,
            container,
            commands.toString(),
            flags.toString(),
            payload["drop_warning"]!!.jsonPrimitive.content,
            payload["database"]!!.jsonPrimitive.content,
            payload["test"]!!.jsonPrimitive.content,
            payload["vault_find_dapp_details"]!!.jsonPrimitive.content,
            payload["vault_query_empty"]!!.jsonPrimitive.content,
            payload["vault_query_all"]!!.jsonPrimitive.content,
            payload["deploy_frontend_note"]!!.jsonPrimitive.content,
            payload["web_static_yaml"]!!.jsonPrimitive.content,
            payload["nextjs_export_config"]!!.jsonPrimitive.content,
            payload["vault_hardcoded_vs_db"]!!.jsonPrimitive.content,
            payload["vault_setupmocks"]!!.jsonPrimitive.content,
            payload["vault_checkmark"]!!.jsonPrimitive.content,
            payload["testnet_deploy_dapp_note"]!!.jsonPrimitive.content,
            payload["tchr_chromia_note"]!!.jsonPrimitive.content,
            payload["tchr_binance_note"]!!.jsonPrimitive.content,
            payload["explorer_verify_note"]!!.jsonPrimitive.content,
            payload["connect_client_note"]!!.jsonPrimitive.content,
            payload["get_container_note"]!!.jsonPrimitive.content,
            payload["multi_deployment_note"]!!.jsonPrimitive.content
        ).joinToString("\n")
        assertTrue(writeBack.contains("writes") && writeBack.contains("deployments"), writeBack)
        assertTrue(writeBack.contains("chains"), writeBack)
        assertTrue(
            schema.contains("DROPPED") || payload["drop_warning"]!!.jsonPrimitive.content.contains("DROPPED"),
            schema
        )
        assertTrue(allText.contains("does not generate a key") || notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed") || notes.contains("does not send signed transactions"))
        assertTrue(notes.contains("info is official") || notes.contains("info is official and read-only"))
        assertTrue(notes.contains("proposal list"))
        assertTrue(notes.contains("voterset info"))
        assertTrue(notes.contains("vote") && notes.contains("Skipped"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertTrue(container.contains("lease", ignoreCase = true) && container.contains("do not invent", ignoreCase = true), container)
        assertFalse(container.contains("15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304"))
        val hex64 = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(hex64.findAll(allText).none(), "must not invent a lease id or BRID: ${hex64.findAll(allText).map { it.value }.toList()}")
        val database = payload["database"]!!.jsonPrimitive.content
        val testSection = payload["test"]!!.jsonPrimitive.content
        assertTrue(database.contains("org.postgresql.Driver"))
        assertTrue(database.contains("host: localhost"))
        assertTrue(database.contains("CHR_DB_PASSWORD") || payload["chromia_yml_sections_notes"]!!.jsonPrimitive.content.contains("CHR_DB_PASSWORD"))
        assertFalse(database.contains("schema_version"))
        assertTrue(testSection.contains("failOnError"))
        assertTrue(testSection.contains("test.main_test"))
        assertFalse(testSection.contains("timeout"))
        assertFalse(testSection.contains("parallel"))
        assertEquals("https://docs.chromia.com/build/deployment/vault-listing/quick-vault-listing", payload["vault_listing_quick"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/deployment/vault-listing/dynamic-vault-listing", payload["vault_listing_dynamic"]!!.jsonPrimitive.content)
        assertTrue(payload["vault_find_dapp_details"]!!.jsonPrimitive.content.contains("find_dapp_details"))
        assertTrue(payload["vault_query_empty"]!!.jsonPrimitive.content.contains("dapp_rowid=0"))
        assertTrue(payload["vault_content_types"]!!.jsonPrimitive.content.contains("landscape"))
        assertTrue(notes.contains("find_dapp_details"))
        assertTrue(notes.contains("/build/deployment/vault-listing/"))
        assertTrue(notes.contains("quick_vault_listing"))
        assertTrue(notes.contains("dynamic_vault_listing"))
        assertTrue(notes.contains("admin_pubkey"))
        assertTrue(notes.contains("filehub-gw.chromia.com"))
        assertTrue(notes.contains("create_or_update_dapp"))
        assertEquals("https://docs.chromia.com/build/deployment/deploy-frontend-dapp", payload["deploy_frontend"]!!.jsonPrimitive.content)
        assertEquals("webStatic", payload["web_static_key"]!!.jsonPrimitive.content)
        assertTrue(payload["web_static_yaml"]!!.jsonPrimitive.content.contains("webStatic: out"))
        assertTrue(payload["web_static_local_url"]!!.jsonPrimitive.content.contains("/web_query/<blockchainRid>/web_static"))
        assertTrue(payload["vault_query_all"]!!.jsonPrimitive.content.contains("[\"landscape\", \"portrait\", \"promotional\", \"video\", \"icon\"]"))
        assertTrue(payload["vault_query_empty"]!!.jsonPrimitive.content.contains("requested_content_types=[]"))
        assertTrue(notes.contains("webStatic"))
        assertTrue(notes.contains("deploy-frontend-dapp"))
        assertEquals("https://docs.chromia.com/build/deployment/testnet/list-dapp-vault", payload["testnet_list_dapp_vault"]!!.jsonPrimitive.content)
        assertTrue(payload["vault_ignore_rowid"]!!.jsonPrimitive.content.contains("keep it in the query signature"))
        assertTrue(notes.contains("list-dapp-vault"))
        assertTrue(notes.contains("ignore the rowid"))
        assertEquals("https://docs.chromia.com/build/deployment/testnet-tokens/", payload["testnet_tokens"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-chromia", payload["testnet_tchr_chromia"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-binance", payload["testnet_tchr_binance"]!!.jsonPrimitive.content)
        assertEquals("https://faucet.testnet.chromia.com/", payload["testnet_faucet"]!!.jsonPrimitive.content)
        assertEquals("https://vault.testnet.chromia.com/en/dapps/", payload["testnet_vault_dapps"]!!.jsonPrimitive.content)
        assertTrue(payload["vault_prerequisites"]!!.jsonPrimitive.content.contains("tCHR"))
        assertTrue(payload["vault_prerequisites"]!!.jsonPrimitive.content.contains("1000 tCHR every 7 days"))
        assertTrue(payload["vault_prerequisites"]!!.jsonPrimitive.content.contains("no real-world value"))
        assertTrue(payload["vault_hardcoded_vs_db"]!!.jsonPrimitive.content.contains("get_dapp_media"))
        assertTrue(payload["vault_hardcoded_vs_db"]!!.jsonPrimitive.content.contains("map_dapp_details"))
        assertTrue(payload["vault_hardcoded_vs_db"]!!.jsonPrimitive.content.contains("require_admin_signer"))
        assertTrue(payload["vault_hardcoded_vs_db"]!!.jsonPrimitive.content.contains("ec_media_tuple"))
        assertTrue(payload["vault_hardcoded_vs_db"]!!.jsonPrimitive.content.contains("quick_vault_listing"))
        assertTrue(payload["vault_auto_listing"]!!.jsonPrimitive.content.contains("automatically listed"))
        assertTrue(payload["vault_checkmark"]!!.jsonPrimitive.content.contains("Chromia team admin"))
        assertTrue(payload["vault_checkmark"]!!.jsonPrimitive.content.contains("quality standards"))
        assertEquals("https://gitlab.com/chromaway/dapp-aggregator/-/blob/dev/scripts/setUpMocks.ts", payload["vault_setupmocks_url"]!!.jsonPrimitive.content)
        assertTrue(payload["vault_setupmocks"]!!.jsonPrimitive.content.contains("setUpMocks.ts"))
        assertTrue(payload["vault_setupmocks"]!!.jsonPrimitive.content.contains("seedChain"))
        assertTrue(payload["vault_setupmocks"]!!.jsonPrimitive.content.contains("seedImages"))
        assertEquals("https://docs.chromia.com/intro/installation/postchain-clients", payload["stale_postchain_clients_install_404"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/overview", payload["clients_overview"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("checkmark"))
        assertTrue(notes.contains("setUpMocks.ts"))
        assertTrue(notes.contains("intro/installation/postchain-clients"))
        assertFalse(allText.contains("--secret ~/.chromia"))
        assertFalse(allText.contains("create_or_update_dapp <description>"))
        assertFalse(allText.contains("chr tx \\"))
        assertEquals("https://docs.chromia.com/build/deployment/testnet/deploy-dapp", payload["testnet_deploy_dapp"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/deployment/testnet/getting-started", payload["testnet_getting_started"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/deployment/mainnet/deploy-dapp", payload["mainnet_deploy_dapp"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/deployment/mainnet/deploy-dapp",
            payload["mainnet_deploy_dapp_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/deployment/mainnet/deploy-dapp/",
            payload["mainnet_deploy_dapp_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Deploy your dapp to Mainnet", payload["mainnet_deploy_dapp_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("Official BUILD deployment/mainnet/deploy-dapp"))
        assertEquals(
            "https://docs.chromia.com/build/deployment/testnet/list-dapp-vault",
            payload["testnet_list_dapp_vault_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/deployment/testnet/list-dapp-vault/",
            payload["testnet_list_dapp_vault_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("List your dapp on the Chromia Testnet Vault", payload["testnet_list_dapp_vault_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("Official BUILD deployment/testnet/list-dapp-vault"))
        assertEquals(
            "https://docs.chromia.com/build/deployment/vault-listing/quick-vault-listing",
            payload["quick_vault_listing_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/deployment/vault-listing/quick-vault-listing/",
            payload["quick_vault_listing_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Quick Vault listing (hardcoded metadata)", payload["quick_vault_listing_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("Official BUILD deployment/vault-listing/quick-vault-listing"))
        assertEquals(
            "https://docs.chromia.com/build/deployment/vault-listing/dynamic-vault-listing",
            payload["dynamic_vault_listing_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/deployment/vault-listing/dynamic-vault-listing/",
            payload["dynamic_vault_listing_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Dynamic Vault listing (database-based metadata)", payload["dynamic_vault_listing_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("Official BUILD deployment/vault-listing/dynamic-vault-listing"))
        assertEquals(
            "https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-chromia",
            payload["get_tchr_chromia_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-chromia/",
            payload["get_tchr_chromia_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Get Chromia test tokens (tCHR) on the Chromia Testnet", payload["get_tchr_chromia_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("Official BUILD deployment/testnet-tokens/get-tchr-chromia"))
        assertEquals(
            "https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-binance",
            payload["get_tchr_binance_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-binance/",
            payload["get_tchr_binance_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Get Chromia test tokens (tCHR) on Binance Smart Chain Testnet", payload["get_tchr_binance_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("Official BUILD deployment/testnet-tokens/get-tchr-binance"))
        val deployNote = payload["testnet_deploy_dapp_note"]!!.jsonPrimitive.content
        assertTrue(deployNote.contains("node0.testnet.chromia.com:7740"))
        assertTrue(deployNote.contains("five minutes"))
        assertTrue(deployNote.contains("--network testnet"))
        assertTrue(deployNote.contains("add the chains key by hand"))
        assertTrue(deployNote.contains("Specify your network (mainnet)"))
        assertTrue(deployNote.contains("auto-configures brid and url") || deployNote.contains("reserved name mainnet"))
        assertTrue(deployNote.contains("0.29.8"))
        assertTrue(deployNote.contains("/build/deployment/mainnet/deploy-dapp"))
        assertTrue(notes.contains("deploy-dapp"))
        assertTrue(notes.contains("getting-started"))
        assertFalse(allText.contains("15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304"))
        assertFalse(allText.contains("testnet_container_key"))
        assertFalse(allText.contains("chr keygen"))
        assertEquals("https://docs.chromia.com/build/deployment/testnet/connect-client", payload["testnet_connect_client"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/deployment/mainnet/connect-client", payload["mainnet_connect_client"]!!.jsonPrimitive.content)
        assertEquals("https://explorer.chromia.com", payload["explorer"]!!.jsonPrimitive.content)
        assertEquals("https://testnet.bscscan.com/address/0x0e61dfdbd5b979adbf3e5b5fe7ee40f85b6daa8d#writeContract", payload["testnet_bscscan_faucet"]!!.jsonPrimitive.content)
        assertEquals("0x8e59d72e4dda56f26963c6b8c77ca1959e9a74f0", payload["testnet_bsc_tchr_token"]!!.jsonPrimitive.content)
        assertEquals("https://vault.testnet.chromia.com/en/dapps/dapp/?dapp=1-Chromia+Economy+Chain", payload["testnet_economy_chain_vault"]!!.jsonPrimitive.content)
        val tchrBinance = payload["tchr_binance_note"]!!.jsonPrimitive.content
        assertTrue(tchrBinance.contains("Testnet token differences"))
        assertTrue(tchrBinance.contains("any EVM wallet address"))
        assertTrue(tchrBinance.contains("once a week"))
        assertTrue(tchrBinance.contains("tBNB"))
        assertTrue(tchrBinance.contains("Access the Chromia testnet faucet"))
        assertTrue(tchrBinance.contains("BscScan faucet URL win"))
        assertFalse(tchrBinance.contains("Connect to web3 button"))
        val tchrChromia = payload["tchr_chromia_note"]!!.jsonPrimitive.content
        assertTrue(tchrChromia.contains("weekly allowace"))
        assertTrue(tchrChromia.contains("allowance wins"))
        val explorerNote = payload["explorer_verify_note"]!!.jsonPrimitive.content
        assertTrue(explorerNote.contains("explorer.chromia.com"))
        assertTrue(explorerNote.contains("Current network to Testnet"))
        assertTrue(explorerNote.contains("directory_chain"))
        assertTrue(explorerNote.contains("no explorer copy steps"))
        val connectNote = payload["connect_client_note"]!!.jsonPrimitive.content
        assertTrue(connectNote.contains("directoryNodeUrlPool"))
        assertTrue(connectNote.contains("creatClient"))
        assertTrue(connectNote.contains("createClient"))
        assertTrue(connectNote.contains("node0.testnet.chromia.com:7740"))
        assertTrue(connectNote.contains("system.chromaway.com"))
        assertTrue(notes.contains("get-tchr-binance"))
        assertTrue(notes.contains("connect-client"))
        assertFalse(allText.contains("PubkeyLink"))
        assertFalse(allText.contains("<BlockchainRID>"))
        assertEquals("https://docs.chromia.com/build/deployment/testnet/get-container", payload["testnet_get_container"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/deployment/mainnet/get-container", payload["mainnet_get_container"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/deployment/mainnet/multi-deployment", payload["mainnet_multi_deployment"]!!.jsonPrimitive.content)
        assertEquals("https://www.npmjs.com/package/postchain-client", payload["postchain_client_npm"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart", payload["hello_world_quickstart"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/get-started/about/hosting", payload["hosting_about"]!!.jsonPrimitive.content)
        val getContainer = payload["get_container_note"]!!.jsonPrimitive.content
        assertTrue(getContainer.contains("at least 10 CHR"))
        assertTrue(getContainer.contains("auto-renewal"))
        assertTrue(getContainer.contains("refund"))
        assertTrue(getContainer.contains("vault.testnet.chromia.com/en/containers"))
        assertTrue(getContainer.contains("2 GB"))
        assertTrue(getContainer.contains("0.5 vCPU"))
        assertTrue(getContainer.contains("16 GB"))
        assertTrue(getContainer.contains("25 MiB/s"))
        assertTrue(getContainer.contains("20 MiB/s"))
        assertTrue(payload["scu_note"]!!.jsonPrimitive.content.contains("90 USD"))
        assertFalse(getContainer.contains("chr keygen"))
        val multi = payload["multi_deployment_note"]!!.jsonPrimitive.content
        assertTrue(multi.contains("fraction of total members") || multi.contains("fraction of"))
        assertTrue(multi.contains("voterset list"))
        assertTrue(multi.contains("Skip voterset add-dapp-provider"))
        assertTrue(connectNote.contains("npmjs.com/package/postchain-client"))
        assertTrue(connectNote.contains("hello-world-quickstart"))
        assertTrue(connectNote.contains("Directory Chain"))
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/deployment",
            payload["commands_deployment_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/deployment/",
            payload["commands_deployment_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("deployment", payload["commands_deployment_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("Official BUILD cli/commands/deployment"))
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrDeployHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrDeployHelpEncodesTchrBinanceAndExplorerVerify() {
        val binance = ChrDeployHelp.tchrBinanceNote()
        val explorer = ChrDeployHelp.explorerVerifyNote()
        val connect = ChrDeployHelp.connectClientNote()
        val chromia = ChrDeployHelp.tchrChromiaNote()
        assertTrue(binance.contains("/build/deployment/testnet-tokens/get-tchr-binance"))
        assertTrue(binance.contains("cross-chain bridge testing"))
        assertTrue(binance.contains("1000 tCHR weekly"))
        assertTrue(explorer.contains("https://explorer.chromia.com"))
        assertTrue(explorer.contains("Under Clusters"))
        assertTrue(explorer.contains("system"))
        assertTrue(connect.contains("pcl.creatClient"))
        assertTrue(connect.contains("pcl.createClient"))
        assertTrue(chromia.contains("weekly allowace"))
        assertFalse(binance.contains("15ddfc"))
        assertFalse(explorer.contains("15ddfc"))
        assertFalse(connect.contains("chr keygen"))
        val container = ChrDeployHelp.getContainerNote()
        val multi = ChrDeployHelp.multiDeploymentNote()
        assertTrue(container.contains("/build/deployment/testnet/get-container"))
        assertTrue(container.contains("/build/deployment/mainnet/get-container"))
        assertTrue(container.contains("10 CHR"))
        assertTrue(ChrDeployHelp.scuNote().contains("2 GB RAM"))
        assertTrue(ChrDeployHelp.scuNote().contains("25 MiB/s"))
        assertTrue(multi.contains("/build/deployment/mainnet/multi-deployment"))
        assertTrue(multi.contains("proposal list"))
        assertFalse(container.contains("testnet_container_key"))
        assertFalse(multi.contains("02FEA5C0"))
    }

    @Test
    fun chromiaYmlSectionsAreOfficial() {
        val db = ChromiaYmlSections.databaseYaml()
        val test = ChromiaYmlSections.testYaml()
        val notes = ChromiaYmlSections.notes()
        assertTrue(db.contains("driver: org.postgresql.Driver"))
        assertTrue(db.contains("database: postchain"))
        assertTrue(db.contains("host: localhost"))
        assertTrue(db.contains("schema: rell_app"))
        assertTrue(db.contains("CHR_DB_PASSWORD") || notes.contains("CHR_DB_PASSWORD"))
        assertFalse(db.contains("schema_version"))
        assertTrue(test.contains("modules:"))
        assertTrue(test.contains("test.main_test"))
        assertTrue(test.contains("failOnError: true"))
        assertFalse(test.contains("timeout"))
        assertFalse(test.contains("parallel"))
        assertTrue(notes.contains("Java 21"))
        assertTrue(notes.contains("Postgres 16"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("/build/database/getting-started"))
        assertTrue(notes.contains("chr node start"))
        assertTrue(notes.contains("NOT a chr command"))
        assertTrue(notes.contains("webStatic"))
        assertTrue(notes.contains("chainId"))
        assertFalse(db.contains("chromia start"))
        assertFalse(notes.contains("execute_transaction"))
    }

    @Test
    fun chrNodeHelpReturnsOfficialFlags() = runBlocking {
        val result = ChrNodeHelpStrategy().execute(
            CallToolRequest(name = "chr_node_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("21+", payload["java"]!!.jsonPrimitive.content)
        assertEquals("16+", payload["postgres"]!!.jsonPrimitive.content)
        assertEquals("http://localhost:7740", payload["default_api_url"]!!.jsonPrimitive.content)
        assertEquals("jdbc:postgresql://localhost:5432/postchain", payload["default_jdbc"]!!.jsonPrimitive.content)
        assertEquals(
            ChrNodeHelp.NODE_INDEX_URL,
            payload["node_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/node/",
            payload["node_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("node", payload["node_index_title"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr node start", commands["start"]!!.jsonPrimitive.content)
        assertEquals("chr node start --wipe", commands["start_wipe"]!!.jsonPrimitive.content)
        assertEquals("chr node start --blockchain-config build/hello.xml", commands["start_from_build"]!!.jsonPrimitive.content)
        assertEquals("chr node update", commands["update"]!!.jsonPrimitive.content)
        val flags = payload["flags"]!!.jsonObject
        val start = flags["start"]!!.jsonObject
        val update = flags["update"]!!.jsonObject
        assertTrue(start["wipe"]!!.jsonPrimitive.content.contains("--wipe"))
        assertTrue(start["wipe"]!!.jsonPrimitive.content.contains("--no-wipe"))
        assertTrue(start["directory_chain_mock"]!!.jsonPrimitive.content.contains("--directory-chain-mock"))
        assertTrue(start["sql_log"]!!.jsonPrimitive.content.contains("--sql-log"))
        assertTrue(start["node_properties"]!!.jsonPrimitive.content.contains("--node-properties"))
        assertTrue(update["preemption"]!!.jsonPrimitive.content.contains("--preemption"))
        val wipe = payload["wipe"]!!.jsonPrimitive.content
        val relation = payload["relation"]!!.jsonPrimitive.content
        val notes = payload["notes"]!!.jsonPrimitive.content
        val postgres = payload["postgres_note"]!!.jsonPrimitive.content
        val allText = listOf(
            wipe, relation, notes, postgres,
            payload["database"]!!.jsonPrimitive.content
        ).joinToString("\n")
        assertTrue(wipe.contains("height=0") || wipe.contains("height 0") || wipe.contains("height=0"), wipe)
        assertTrue(relation.contains("chr build"), relation)
        assertTrue(relation.contains("chr test"), relation)
        assertTrue(relation.contains("does not start") || relation.contains("does not start or require"), relation)
        assertTrue(notes.contains("Postgres 16") || postgres.contains("Postgres 16"))
        assertTrue(notes.contains("does not start a node"), notes)
        assertTrue(notes.contains("does not generate a key"), notes)
        assertTrue(notes.contains("does not send signed"), notes)
        assertTrue(notes.contains("Official BUILD cli/commands/node"), notes)
        assertTrue(notes.contains("https://docs.chromia.com/build/cli/commands/node/"), notes)
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("7E5BE539EF62B48A"))
        assertTrue(allText.contains(WriteDeploymentConfig.MAINNET_DIRECTORY_BRID), "official E48D Directory BRID")
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        val hexes = invented.findAll(allText).map { it.value.uppercase() }.toSet()
        val allowed = setOf(
            WriteDeploymentConfig.MAINNET_DIRECTORY_BRID.uppercase(),
            WriteDeploymentConfig.TESTNET_DIRECTORY_BRID.uppercase()
        )
        assertTrue(
            hexes.all { it in allowed },
            "must not invent a Directory/dapp BRID: ${hexes - allowed}"
        )
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrNodeHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrQueryHelpIsReadOnlyOfficialFlags() = runBlocking {
        val result = ChrQueryHelpStrategy().execute(
            CallToolRequest(name = "chr_query_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("true", payload["read_only"]!!.jsonPrimitive.content)
        assertEquals("http://localhost:7740", payload["default_api_url"]!!.jsonPrimitive.content)
        assertEquals(
            ChrQueryHelp.QUERY_INDEX_URL,
            payload["query_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/query/",
            payload["query_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("query", payload["query_index_title"]!!.jsonPrimitive.content)
        assertEquals(WriteDeploymentConfig.MAINNET_DIRECTORY_BRID, payload["mainnet_directory_brid"]!!.jsonPrimitive.content)
        assertEquals(WriteDeploymentConfig.TESTNET_DIRECTORY_BRID, payload["testnet_directory_brid"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertTrue(commands["local"]!!.jsonPrimitive.content.contains("chr query"))
        assertTrue(commands["local"]!!.jsonPrimitive.content.contains("<BlockchainRID>"))
        assertEquals(
            "chr query --network testnet --blockchain hello hello_world",
            commands["named_deployment"]!!.jsonPrimitive.content
        )
        val flags = payload["flags"]!!.jsonObject
        assertTrue(flags["blockchain_rid"]!!.jsonPrimitive.content.contains("--blockchain-rid"))
        assertTrue(flags["network_alias"]!!.jsonPrimitive.content.contains("--mainnet"))
        assertTrue(flags["network_alias"]!!.jsonPrimitive.content.contains("--testnet"))
        assertTrue(flags["output_format"]!!.jsonPrimitive.content.contains("pretty"))
        val target = payload["target"]!!.jsonPrimitive.content
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(target, notes).joinToString("\n")
        assertTrue(target.contains("chr node start"), target)
        assertTrue(target.contains(WriteDeploymentConfig.MAINNET_DIRECTORY_BRID), target)
        assertTrue(notes.contains("does not sign"), notes)
        assertTrue(notes.contains("does not send signed") || notes.contains("does not send a transaction"), notes)
        assertTrue(notes.contains("not `chr tx`") || notes.contains("not chr tx") || notes.contains("This is not `chr tx`"), notes)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/tx",
            payload["tx_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            ChrQueryHelp.TX_INDEX_URL,
            payload["tx_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/tx/",
            payload["tx_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("tx", payload["tx_index_title"]!!.jsonPrimitive.content)
        assertEquals("true", payload["tx_help_only"]!!.jsonPrimitive.content)
        val officialTx = payload["official_tx_flags"]!!.jsonObject
        assertTrue(officialTx["await"]!!.jsonPrimitive.content.contains("--await"))
        assertTrue(officialTx["ft_auth"]!!.jsonPrimitive.content.contains("--ft-auth"))
        assertTrue(notes.contains("HELP ONLY"))
        assertTrue(notes.contains("commands/tx"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(notes.contains("chr tx set_name"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("FC17B67D66F6F35A5D8B75ED3F83AE222FB8C8FCA241624F06285150F10C6BAC"))
        assertFalse(allText.contains("2D17B27D4F69E0A91B0CA39AF53EFA9B82CDAF698EF906A67C71C266983EEB7A"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("7E5BE539EF62B48A"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        val hexes = invented.findAll(allText).map { it.value.uppercase() }.toSet()
        val allowed = setOf(
            WriteDeploymentConfig.MAINNET_DIRECTORY_BRID.uppercase(),
            WriteDeploymentConfig.TESTNET_DIRECTORY_BRID.uppercase()
        )
        assertTrue(
            hexes.all { it in allowed },
            "must not invent a Directory/dapp BRID: ${hexes - allowed}"
        )
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrQueryHelp.toJson(), result.structuredContent)
    }

    @Test
    fun vaultLeaseHelpUsesOfficialDirectoryBridsOnly() = runBlocking {
        val result = VaultLeaseHelpStrategy().execute(
            CallToolRequest(name = "vault_lease_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("21+", payload["java"]!!.jsonPrimitive.content)
        assertEquals("16+", payload["postgres"]!!.jsonPrimitive.content)
        assertEquals(WriteDeploymentConfig.MAINNET_DIRECTORY_BRID, payload["mainnet_directory_brid"]!!.jsonPrimitive.content)
        assertEquals(WriteDeploymentConfig.TESTNET_DIRECTORY_BRID, payload["testnet_directory_brid"]!!.jsonPrimitive.content)
        assertEquals("<containerIID>", payload["container_placeholder"]!!.jsonPrimitive.content)
        val vault = payload["vault"]!!.jsonObject
        assertTrue(vault["testnet"]!!.jsonPrimitive.content.contains("vault.testnet.chromia.com"))
        assertTrue(vault["mainnet"]!!.jsonPrimitive.content.contains("vault.chromia.com"))
        val yamlTest = payload["yaml_testnet"]!!.jsonPrimitive.content
        val yamlMain = payload["yaml_mainnet"]!!.jsonPrimitive.content
        assertTrue(yamlTest.contains("container: <containerIID>"))
        assertTrue(yamlMain.contains("container: <containerIID>"))
        assertTrue(yamlTest.contains(WriteDeploymentConfig.TESTNET_DIRECTORY_BRID))
        assertTrue(yamlMain.contains(WriteDeploymentConfig.MAINNET_DIRECTORY_BRID))
        val workflow = payload["workflow"]!!.jsonPrimitive.content
        val notes = payload["notes"]!!.jsonPrimitive.content
        val pmc = payload["pmc"]!!.jsonPrimitive.content
        assertEquals("https://docs.chromia.com/get-started/about/hosting", payload["hosting_about"]!!.jsonPrimitive.content)
        assertTrue(payload["scu_note"]!!.jsonPrimitive.content.contains("2 GB"))
        assertTrue(notes.contains("25 MiB/s"))
        assertTrue(notes.contains("/get-started/about/hosting"))
        val allText = listOf(workflow, notes, pmc, yamlTest, yamlMain).joinToString("\n")
        assertTrue(workflow.contains("deployments.<net>.container") || notes.contains("deployments.<net>.container"))
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not invent") || workflow.contains("Do not invent"))
        assertTrue(notes.contains("does not send signed"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304"))
        assertFalse(allText.contains("7E5BE539EF62B48A"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        val hexes = invented.findAll(allText).map { it.value.uppercase() }.toSet()
        val allowed = setOf(
            WriteDeploymentConfig.MAINNET_DIRECTORY_BRID.uppercase(),
            WriteDeploymentConfig.TESTNET_DIRECTORY_BRID.uppercase()
        )
        assertTrue(hexes.all { it in allowed }, "must not invent a lease/BRID: ${hexes - allowed}")
        assertEquals(payload, result.structuredContent)
        assertEquals(VaultLeaseHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrVersionLiveProbe() {
        val chr = findOnPath("chr")
        // Environment-dependent live probe: skip (not fail) where chr is absent or
        // not directly launchable (e.g. Windows shims), so CI stays green without chr.
        org.junit.jupiter.api.Assumptions.assumeTrue(chr != null, "chr not on PATH; skipping live probe")
        val proc = try {
            ProcessBuilder(chr, "version")
                .redirectErrorStream(true)
                .start()
        } catch (e: java.io.IOException) {
            org.junit.jupiter.api.Assumptions.abort<Nothing>("chr found but not launchable: ${e.message}")
        }
        val output = proc.inputStream.bufferedReader().readText()
        val code = proc.waitFor()
        assertEquals(0, code, output)
        assertTrue(
            output.contains("chromia", ignoreCase = true) ||
                output.contains("rell", ignoreCase = true) ||
                output.contains("0."),
            output
        )
        assertFalse(output.contains("chr keygen"))
        assertFalse(output.contains("mnemonic"))
        assertFalse(output.contains("BEGIN PRIVATE"))
        assertFalse(output.contains("privkey"))
        assertFalse(output.contains("15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304"))
        assertFalse(output.contains("7E5BE539EF62B48A"))
    }


    @Test
    fun checkDappProjectCleanScaffoldPasses() {
        val files = DappScaffold.files("hello_dapp")
        val result = CheckDappProject.check(
            files.getValue("chromia.yml"),
            files.filterKeys { it.endsWith(".rell") }
        )
        assertTrue(result.ok, result.errors.toString())
        assertEquals(emptyList<String>(), result.errors)
    }

    @Test
    fun checkDappProjectMerkleHashVersion1Fails() {
        val files = DappScaffold.files("hello_dapp")
        val yaml = files.getValue("chromia.yml").replace(
            "merkle_hash_version: ${DappScaffold.MERKLE_HASH_VERSION}",
            "merkle_hash_version: 1"
        )
        val result = CheckDappProject.check(
            yaml,
            files.filterKeys { it.endsWith(".rell") }
        )
        assertFalse(result.ok)
        assertTrue(
            result.errors.any { it.contains("merkle_hash_version") && it.contains("1") },
            result.errors.toString()
        )
        assertTrue(result.errors.any { it.startsWith("chromia.yml:") }, result.errors.toString())
    }

    @Test
    fun checkDappProjectForbiddenFt4ImportFails() {
        val yaml = DappScaffold.files("hello_dapp").getValue("chromia.yml")
        val result = CheckDappProject.check(
            yaml,
            mapOf(
                "src/main.rell" to "module;\nimport lib.ft4.admin;\nimport ras_open;\n"
            )
        )
        assertFalse(result.ok)
        // Findings use the normalized source-root path + line, matching the
        // compile/security format (audit 2026-09-01, F8).
        assertTrue(
            result.errors.any { it.startsWith("main.rell:2:") && it.contains("lib.ft4.admin") },
            result.errors.toString()
        )
        assertTrue(
            result.errors.any { it.startsWith("main.rell:3:") && it.contains("ras_open") },
            result.errors.toString()
        )
    }

    @Test
    fun checkDappProjectToolChecksScaffoldWithoutRagOrDisk() = runBlocking {
        val files = DappScaffold.files("hello_dapp")
        val ragLoads = AtomicInteger(0)
        val result = ToolExecutor(
            RecordingRepository(),
            PromptManager(),
            ragStoreFactory = {
                ragLoads.incrementAndGet()
                error("check_dapp_project must not load RagStore")
            }
        ).executeTool(
            CallToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("yaml", files.getValue("chromia.yml"))
                    put(
                        "rell",
                        buildJsonObject {
                            files.filterKeys { it.endsWith(".rell") }.forEach { (path, content) ->
                                put(path, content)
                            }
                        }
                    )
                }
            )
        )
        assertTrue(result.isError != true)
        assertEquals(0, ragLoads.get())
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(true, payload["ok"]!!.jsonPrimitive.boolean)
        assertEquals(0, payload["errors"]!!.jsonArray.size)
        assertEquals(payload, result.structuredContent)
    }

    @Test
    fun checkFt4ImportsToolFlagsForbiddenAndIgnoresComments() = runBlocking {
        val result = CheckFt4ImportsStrategy().execute(
            CallToolRequest(
                name = "check_ft4_imports",
                arguments = buildJsonObject {
                    put(
                        "rell",
                        buildJsonObject {
                            put("src/main.rell", "module;\nimport lib.ft4.accounts;\nimport lib.ft4.admin;\n")
                            put("src/ok.rell", "module;\n// import ras_open\nimport lib.ft4.assets;\n")
                        }
                    )
                }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(false, payload["ok"]!!.jsonPrimitive.boolean)
        val errors = payload["errors"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(errors.any { it.contains("src/main.rell") && it.contains("lib.ft4.admin") }, errors.toString())
        assertTrue(errors.none { it.contains("src/ok.rell") }, errors.toString())
        assertEquals("https://docs.chromia.com/build/ft4/setup/imports", payload["imports_docs"]!!.jsonPrimitive.content)
        assertEquals("lib.ft4.crosschain", payload["crosschain_import"]!!.jsonPrimitive.content)
        assertEquals(payload, result.structuredContent)
    }

    @Test
    fun checkFt4ImportsWarnsOnHyphenatedCrossChainListLabel() {
        val one = org.chromia.tools.Ft4ImportCheck.scan("module;\nimport lib.ft4.cross-chain;\n")
        assertEquals(true, one.ok)
        assertTrue(one.warnings.any { it.contains("lib.ft4.crosschain") }, one.warnings.toString())
        assertTrue(one.warnings.any { it.contains("cross-chain") }, one.warnings.toString())
    }

    private fun findOnPath(name: String): String? {
        val paths = System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
        return paths.asSequence()
            .map { File(it, name) }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
    }
}
