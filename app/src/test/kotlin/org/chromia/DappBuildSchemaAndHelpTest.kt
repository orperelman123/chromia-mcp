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
import org.chromia.tools.CheckDappProjectStrategy
import org.chromia.tools.ChrCreateRellDappHelp
import org.chromia.tools.ChrCreateRellDappHelpStrategy
import org.chromia.tools.ChromiaDocsYmlHelp
import org.chromia.tools.ChromiaDocsYmlHelpStrategy
import org.chromia.tools.ChrGenerateClientHelp
import org.chromia.tools.ChrGenerateClientHelpStrategy
import org.chromia.tools.ChrKeyIdHelp
import org.chromia.tools.ChrKeyIdHelpStrategy
import org.chromia.tools.ChromiaLanguageClientsHelp
import org.chromia.tools.ChromiaLanguageClientsHelpStrategy
import org.chromia.tools.ChromiaRellLanguageHelp
import org.chromia.tools.ChromiaRellLanguageHelpStrategy
import org.chromia.tools.ChromiaRellTypesHelp
import org.chromia.tools.ChromiaRellTypesHelpStrategy
import org.chromia.tools.ChromiaRellExpressionsHelp
import org.chromia.tools.ChromiaRellExpressionsHelpStrategy
import org.chromia.tools.ChromiaRellStatementsHelp
import org.chromia.tools.ChromiaRellStatementsHelpStrategy
import org.chromia.tools.ChromiaRellDatabaseHelp
import org.chromia.tools.ChromiaRellDatabaseHelpStrategy
import org.chromia.tools.ChromiaRellSystemlibHelp
import org.chromia.tools.ChromiaRellSystemlibHelpStrategy
import org.chromia.tools.ChromiaRellPracticesHelp
import org.chromia.tools.ChromiaRellPracticesHelpStrategy
import org.chromia.tools.ChromiaFt4QueriesHelp
import org.chromia.tools.ChromiaFt4QueriesHelpStrategy
import org.chromia.tools.ChromiaIntegrationsHelp
import org.chromia.tools.ChromiaIntegrationsHelpStrategy
import org.chromia.tools.ChromiaVectorSearchHelp
import org.chromia.tools.ChromiaVectorSearchHelpStrategy
import org.chromia.tools.ChrQueryHelpStrategy
import org.chromia.tools.ChromiaCookbookHelp
import org.chromia.tools.ChromiaCookbookHelpStrategy
import org.chromia.tools.ChrLibraryHelp
import org.chromia.tools.ChrLibraryHelpStrategy
import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ScaffoldDappStrategy
import org.chromia.tools.ValidateChromiaYmlStrategy
import org.chromia.tools.WriteDeploymentConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DappBuildSchemaAndHelpTest {

    private fun baseYml(): String = DappScaffold.files("hello").getValue("chromia.yml")

    private fun withDeployments(extra: String): String = baseYml().trimEnd() + "\n\n" + extra.trimIndent() + "\n"

    @Test
    fun scaffoldDappStrategyOutputPassesCheckDappProjectStrategy() = runBlocking {
        val scaffold = ScaffoldDappStrategy().execute(
            CallToolRequest(
                name = "scaffold_dapp",
                arguments = buildJsonObject { put("name", "wallet") }
            ),
            RecordingRepository()
        )
        assertTrue(scaffold.isError != true)
        val files = scaffold.structuredContent!!["files"]!!.jsonObject
        val yaml = files["chromia.yml"]!!.jsonPrimitive.content
        val check = CheckDappProjectStrategy().execute(
            CallToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("yaml", yaml)
                    put(
                        "rell",
                        buildJsonObject {
                            files.entries.filter { it.key.endsWith(".rell") }.forEach { (path, value) ->
                                put(path, value.jsonPrimitive.content)
                            }
                        }
                    )
                }
            ),
            RecordingRepository()
        )
        assertTrue(check.isError != true)
        val payload = Json.parseToJsonElement((check.content.first() as TextContent).text!!).jsonObject
        assertEquals(true, payload["ok"]!!.jsonPrimitive.boolean, payload.toString())
        assertEquals(0, payload["errors"]!!.jsonArray.size, payload.toString())
        assertEquals(payload, check.structuredContent)
    }

    @Test
    fun brokenYmlsErrorThroughValidateAndCheck() = runBlocking {
        val rell = mapOf("src/main.rell" to DappScaffold.files("hello").getValue("src/main.rell"))
        val cases = listOf(
            Triple(
                "merkle 1",
                """
                blockchains:
                  hello:
                    module: main
                    config:
                      features:
                        merkle_hash_version: 1
                compile:
                  rellVersion: 0.16.1
                """.trimIndent(),
                "merkle_hash_version"
            ),
            // A MISSING rellVersion is a warning since round 2 D3 (official
            // configs omit it and chr builds them); a rellVersion NEWER than
            // the CLI-bundled compiler genuinely breaks `chr build` and stays
            // an error.
            Triple(
                "too-new rellVersion",
                """
                blockchains:
                  hello:
                    module: main
                    config:
                      features:
                        merkle_hash_version: 2
                compile:
                  rellVersion: 9.9.9
                """.trimIndent(),
                "rellVersion"
            ),
            Triple(
                "admin lib",
                """
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
                """.trimIndent(),
                "lib.ft4.admin"
            ),
            Triple(
                "wrong-length BRID",
                withDeployments(
                    """
                    deployments:
                      testnet:
                        url: https://node0.testnet.chromia.com:7740
                        brid: x"DEADBEEF"
                        container: <containerIID>
                    """
                ),
                "brid"
            ),
        )
        cases.forEach { (label, yaml, needle) ->
            val validated = ChromiaYmlValidator.validate(yaml)
            assertFalse(validated.ok, "$label should error: ${validated.errors}")
            assertTrue(validated.errors.any { it.contains(needle) }, "$label errors=${validated.errors}")

            val tool = ValidateChromiaYmlStrategy().execute(
                CallToolRequest(
                    name = "validate_chromia_yml",
                    arguments = buildJsonObject { put("yaml", yaml) }
                ),
                RecordingRepository()
            )
            val payload = Json.parseToJsonElement((tool.content.first() as TextContent).text!!).jsonObject
            assertEquals(false, payload["ok"]!!.jsonPrimitive.content.toBoolean(), label)
            val errors = payload["errors"]!!.jsonArray.map { it.jsonPrimitive.content }
            assertTrue(errors.any { it.contains(needle) }, "$label tool errors=$errors")

            val project = CheckDappProject.check(yaml, rell)
            assertFalse(project.ok, "$label check should error: ${project.errors}")
            assertTrue(project.errors.any { it.contains(needle) }, "$label check errors=${project.errors}")
        }
    }

    @Test
    fun deploymentsWithoutContainerWarnsButDoesNotFailPins() {
        val yaml = withDeployments(
            """
            deployments:
              testnet:
                url: https://node0.testnet.chromia.com:7740
                brid: x"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}"
            """
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(
            result.warnings.any { it.contains("container") && it.contains("testnet") },
            result.warnings.toString()
        )
    }

    @Test
    fun leftoverMaxAuthDescriptorRulesSiblingKeyWarns() {
        val yaml = """
            blockchains:
              hello:
                module: main
                moduleArgs:
                  lib.ft4.core.accounts:
                    max_auth_descriptor_rules: 4
                    auth_descriptor:
                      max_rules: 8
                      max_number_per_account: 10
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.warnings.any { it.contains("max_auth_descriptor_rules") && it.contains("max_rules") }, result.warnings.toString())
    }

    @Test
    fun rellVersionFormatMustBeSemver() {
        val yaml = """
            blockchains:
              hello:
                module: main
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: latest
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("rellVersion") && it.contains("semver") }, result.errors.toString())
    }

    @Test
    fun modulePathIsError() {
        val yaml = """
            blockchains:
              hello:
                module: src/main.rell
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("module") && it.contains("path") }, result.errors.toString())
    }

    @Test
    fun reservedDeploymentNameWrongOfficialBridIsError() {
        val yaml = withDeployments(
            """
            deployments:
              testnet:
                url: https://node0.testnet.chromia.com:7740
                brid: x"${WriteDeploymentConfig.MAINNET_DIRECTORY_BRID}"
                container: <containerIID>
            """
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(
            result.errors.any { it.contains("official") && it.contains("testnet") },
            result.errors.toString()
        )
    }

    @Test
    fun customDeploymentNameRequiresBridAndUrl() {
        val yaml = withDeployments(
            """
            deployments:
              staging:
                container: <containerIID>
            """
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(
            result.errors.any { it.contains("staging") && it.contains("brid") && it.contains("url") },
            result.errors.toString()
        )
    }

    @Test
    fun reservedTestnetAcceptsAnyOfficialNodeUrl() {
        val yaml = withDeployments(
            """
            deployments:
              testnet:
                url: https://node2.testnet.chromia.com:7740
                brid: x"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}"
                container: <containerIID>
            """
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
    }

    @Test
    fun reservedMainnetAcceptsOfficialHostsAsStringOrList() {
        val cases = listOf(
            """
            deployments:
              mainnet:
                url: https://system.chromaway.com
                brid: x"${WriteDeploymentConfig.MAINNET_DIRECTORY_BRID}"
                container: <containerIID>
            """,
            """
            deployments:
              mainnet:
                url: https://mainnet-dapp1.sunube.net:7740
                brid: x"${WriteDeploymentConfig.MAINNET_DIRECTORY_BRID}"
                container: <containerIID>
            """,
            """
            deployments:
              mainnet:
                url:
                  - https://system.chromaway.com
                  - https://mainnet-dapp1.sunube.net:7740
                brid: x"${WriteDeploymentConfig.MAINNET_DIRECTORY_BRID}"
                container: <containerIID>
            """
        )
        cases.forEach { extra ->
            val result = ChromiaYmlValidator.validate(withDeployments(extra))
            assertTrue(result.ok, extra + " -> " + result.errors)
        }
    }

    @Test
    fun reservedDeploymentNameCanOmitBridAndUrl() {
        val yaml = withDeployments(
            """
            deployments:
              mainnet:
                container: <containerIID>
            """
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
    }

    @Test
    fun chrGenerateClientHelpIsOfficialFlags() = runBlocking {
        val result = ChrGenerateClientHelpStrategy().execute(
            CallToolRequest(name = "chr_generate_client_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/generate",
            payload["generate_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/generate/",
            payload["generate_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("generate", payload["generate_index_title"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr generate client-stubs", commands["client_stubs"]!!.jsonPrimitive.content)
        assertTrue(commands["kotlin"]!!.jsonPrimitive.content.contains("--kotlin"))
        assertTrue(commands["typescript"]!!.jsonPrimitive.content.contains("--typescript"))
        assertTrue(commands["javascript"]!!.jsonPrimitive.content.contains("--javascript"))
        assertTrue(commands["python"]!!.jsonPrimitive.content.contains("--python"))
        assertEquals("chr generate graph", commands["graph"]!!.jsonPrimitive.content)
        assertEquals("chr generate docs-site", commands["docs_site"]!!.jsonPrimitive.content)
        assertEquals(
            "chr generate client-stubs --hide-lib-warnings",
            commands["client_stubs_hide_lib_warnings"]!!.jsonPrimitive.content
        )
        val flags = payload["flags"]!!.jsonObject
        assertTrue(flags["language"]!!.jsonPrimitive.content.contains("--kotlin"))
        assertTrue(flags["module"]!!.jsonPrimitive.content.contains("not a file path"))
        assertTrue(flags["hide_lib_warnings"]!!.jsonPrimitive.content.contains("--hide-lib-warnings"))
        assertTrue(flags["hide_lib_warnings"]!!.jsonPrimitive.content.contains("client-stubs"))
        assertTrue(flags["hide_lib_warnings"]!!.jsonPrimitive.content.contains("graph"))
        assertTrue(flags["hide_lib_warnings"]!!.jsonPrimitive.content.contains("docs-site"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("Official BUILD cli/commands/generate"))
        assertTrue(notes.contains("https://docs.chromia.com/build/cli/commands/generate/"))
        assertTrue(notes.contains("client-stubs"))
        val docsYaml = payload["docs_yaml"]!!.jsonPrimitive.content
        val docsKeys = payload["docs_keys"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(docsYaml.contains("docs:"))
        assertTrue(docsYaml.contains("title:"))
        assertTrue(docsYaml.contains("footerMessage:"))
        assertTrue(docsYaml.contains("customStyleSheets:"))
        assertTrue(docsYaml.contains("customAssets:"))
        assertTrue(docsYaml.contains("additionalContent:"))
        assertTrue(docsYaml.contains("sourceLink:"))
        assertTrue(docsYaml.contains("remoteUrl:"))
        assertTrue(docsYaml.contains("remoteLineSuffix:"))
        assertFalse(docsYaml.contains("additionalModules"))
        assertFalse(docsYaml.contains("customComponents"))
        assertTrue(docsKeys.contains("title"))
        assertTrue(docsKeys.contains("sourceLink.remoteUrl"))
        assertTrue(docsKeys.contains("sourceLink.remoteLineSuffix"))
        val allText = listOf(notes, commands.toString(), flags.toString(), docsYaml).joinToString("\n")
        assertTrue(notes.contains("does not generate a key") || notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("generate client-stubs"))
        assertTrue(notes.contains("--hide-lib-warnings"))
        assertTrue(notes.contains("docs:"))
        assertTrue(notes.contains("footerMessage"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals("chromia_docs_yml_help", payload["docs_yml_help"]!!.jsonPrimitive.content)
        assertEquals(ChromiaDocsYmlHelp.docsYaml(), docsYaml)
        assertEquals(ChromiaDocsYmlHelp.keys, docsKeys)
        val packages = payload["packages"]!!.jsonObject
        assertEquals("postchain-client", packages["npm_postchain"]!!.jsonPrimitive.content)
        assertEquals("@chromia/ft4", packages["npm_ft4"]!!.jsonPrimitive.content)
        assertEquals("postchain-client-py", packages["pip_postchain"]!!.jsonPrimitive.content)
        assertEquals("net.postchain.client:postchain-client", packages["maven_postchain"]!!.jsonPrimitive.content)
        val settings = payload["create_client_settings"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("nodeUrlPool" in settings)
        assertTrue("directoryNodeUrlPool" in settings)
        assertTrue("blockchainRid" in settings)
        val local = payload["local_create_client"]!!.jsonPrimitive.content
        assertTrue(local.contains("postchain-client"))
        assertTrue(local.contains("blockchainRid"))
        assertTrue(local.contains("hello_world"))
        assertFalse(local.contains("privKey"))
        assertFalse(local.contains("signAndSend"))
        val testnet = payload["testnet_create_client"]!!.jsonPrimitive.content
        assertTrue(testnet.contains("directoryNodeUrlPool"))
        assertTrue(testnet.contains("node0.testnet.chromia.com:7740"))
        val ft4 = payload["ft4_local_connection"]!!.jsonPrimitive.content
        assertTrue(ft4.contains("@chromia/ft4"))
        assertTrue(ft4.contains("createConnection"))
        assertTrue(ft4.contains("getAllAssets"))
        assertEquals("{ data: [], nextCursor: null }", payload["ft4_empty_assets"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/ft4/client/client-setup/", payload["ft4_client_setup_slash"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("nextCursor"))
        assertEquals("chr_key_id_help", payload["key_id_help"]!!.jsonPrimitive.content)
        assertEquals("chromia_cookbook_help", payload["cookbook_help"]!!.jsonPrimitive.content)
        assertEquals("chromia_language_clients_help", payload["language_clients_help"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart", payload["js_quickstart"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/get-started/create-dapp/run-dapp-cli", payload["run_dapp_cli"]!!.jsonPrimitive.content)
        val helloRell = payload["hello_world_rell"]!!.jsonPrimitive.content
        assertTrue(helloRell.contains("object my_name"))
        assertTrue(helloRell.contains("query hello_world()"))
        assertTrue(helloRell.contains(".format(my_name.name)"))
        assertFalse(helloRell.contains("set_name"))
        assertEquals("Hello World!", payload["hello_world_result"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("hello-world-quickstart"))
        assertTrue(notes.contains("/javascript-typescript/reference"))
        assertTrue(notes.contains("succefull"))
        assertTrue(notes.contains("startegy"))
        assertTrue(notes.contains("abortOnErrror"))
        assertTrue(notes.contains("abortOnError"))
        assertTrue(notes.contains("Iid 0"))
        assertTrue(notes.contains("chromia. yml"))
        assertTrue(notes.contains("signAndSendUniqueTransaction"))
        assertTrue(notes.contains("query-only wins"))
        assertTrue(notes.contains("/javascript-typescript/reference"))
        assertTrue(notes.contains("succefull"))
        assertTrue(notes.contains("some details may be outdated"))
        assertTrue(notes.contains("sha256sum"))
        assertTrue(notes.contains("startegy"))
        assertTrue(notes.contains("abortOnErrror"))
        assertTrue(notes.contains("abortOnError"))
        assertTrue(notes.contains("blockchainRID"))
        assertTrue(notes.contains("Iid 0") || notes.contains("Iid $"))
        assertFalse(notes.contains("7d565d92fd15bd1cdac2dc276cbcbc5581349d05a9e94ba919e1155ef4daf8f9"))
        assertEquals(0, payload["js_reference_directory_iid"]!!.jsonPrimitive.content.toInt())
        assertEquals("blockchainRid", payload["js_reference_brid_setting"]!!.jsonPrimitive.content)
        assertEquals("blockchainRID", payload["js_reference_official_prose_brid"]!!.jsonPrimitive.content)
        assertEquals("startegy", payload["js_reference_official_strategy_typo"]!!.jsonPrimitive.content)
        assertEquals("abortOnErrror", payload["js_reference_official_abort_typo"]!!.jsonPrimitive.content)
        assertEquals("abortOnError", payload["js_reference_source_failover_abort"]!!.jsonPrimitive.content)
        assertTrue(payload["js_reference_query_foobar"]!!.jsonPrimitive.content.contains("get_foobar"))
        assertEquals("get_fobar", payload["js_reference_official_query_typo"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("get_fobar"))
        assertTrue(notes.contains("get_foobar"))
        val failover = payload["js_reference_failover_defaults"]!!.jsonObject
        assertEquals("Abort On Error", failover["strategy"]!!.jsonPrimitive.content)
        assertEquals(3, failover["attemptsPerEndpoint"]!!.jsonPrimitive.content.toInt())
        assertEquals(5000, failover["attemptInterval_ms"]!!.jsonPrimitive.content.toInt())
        assertEquals(30000, failover["unreachableDuration_ms"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, failover["statusPollCount"]!!.jsonPrimitive.content.toInt())
        val strategies = payload["js_reference_failover_strategies"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(4, strategies.size)
        assertTrue("Query majority" in strategies)
        val sticky = payload["js_reference_sticky_create_client"]!!.jsonPrimitive.content
        assertTrue(sticky.contains("useStickyNode"))
        assertTrue(sticky.contains("directoryNodeUrlPool"))
        assertTrue(sticky.contains("http://localhost:7740"))
        assertFalse(sticky.contains("secp256k1"))
        assertFalse(sticky.contains("signAndSend"))
        assertFalse(sticky.contains("newSignatureProvider"))
        assertFalse(sticky.contains("Buffer.alloc"))
        assertTrue("failOverConfig" in settings)
        assertTrue("useStickyNode" in settings)
        assertTrue("blockchainIid" in settings)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/kotlin-client", payload["kotlin_client"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/python-client", payload["python_client"]!!.jsonPrimitive.content)
        assertEquals("3.7+", payload["python_min"]!!.jsonPrimitive.content)
        assertEquals("http://127.0.0.1:7740", payload["kotlin_local_endpoint"]!!.jsonPrimitive.content)
        assertEquals("net.postchain.client:chromia-client", payload["kotlin_chromia_maven"]!!.jsonPrimitive.content)
        assertEquals("net.postchain.client:postchain-client", payload["kotlin_official_chromia_gradle"]!!.jsonPrimitive.content)
        assertTrue(payload["python_query_collections"]!!.jsonPrimitive.content.contains("get_collections"))
        assertTrue(payload["python_query_books"]!!.jsonPrimitive.content.contains("get_all_books"))
        assertTrue(payload["python_query_reviews"]!!.jsonPrimitive.content.contains("get_all_reviews_for_book"))
        assertTrue(payload["python_query_reviews"]!!.jsonPrimitive.content.contains("ISBN123"))
        assertTrue(payload["python_close"]!!.jsonPrimitive.content.contains("rest_client.close"))
        assertEquals("aiohttp", payload["python_async"]!!.jsonPrimitive.content)
        assertEquals("We are currently updating this documentation", payload["official_outdated_banner"]!!.jsonPrimitive.content)
        assertEquals("POSTCHAIN_TEST_NODE", payload["python_env_node"]!!.jsonPrimitive.content)
        assertEquals("BLOCKCHAIN_TEST_RID", payload["python_env_rid"]!!.jsonPrimitive.content)
        val pythonEnv = payload["python_env"]!!.jsonPrimitive.content
        assertTrue(pythonEnv.contains("POSTCHAIN_TEST_NODE=http://localhost:7740"))
        assertTrue(pythonEnv.contains("BLOCKCHAIN_TEST_RID=your_blockchain_rid"))
        assertFalse(pythonEnv.contains("PRIV_KEY"))
        val standardClient = payload["kotlin_standard_chromia_client"]!!.jsonPrimitive.content
        assertTrue(standardClient.contains("StandardChromiaClient"))
        assertTrue(standardClient.contains("http://127.0.0.1:7740"))
        assertFalse(standardClient.contains("awaitAnchoredTx"))
        assertFalse(standardClient.contains("buildFromHex"))
        val kotlinRepos = payload["kotlin_maven_repos"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(3, kotlinRepos.size)
        assertTrue(kotlinRepos.any { it.contains("50818999") })
        assertTrue(notes.contains("kotlin-client"))
        assertTrue(notes.contains("python-client"))
        assertTrue(notes.contains("chromia-client"))
        assertTrue(notes.contains("get_collections"))
        assertTrue(notes.contains("get_all_reviews_for_book"))
        assertTrue(notes.contains("POSTCHAIN_TEST_NODE"))
        assertTrue(notes.contains("BLOCKCHAIN_TEST_RID"))
        assertTrue(notes.contains("StandardChromiaClient"))
        assertTrue(notes.contains("We are currently updating this documentation"))
        assertTrue(notes.contains("PRIV_KEY"))
        assertFalse(notes.contains("4CB84F555AD0F93C938EB8EF0E10F1CE129D143D74A6516F7D8E89ED21954593"))
        assertEquals(WriteDeploymentConfig.MAINNET_DIRECTORY_BRID, payload["mainnet_directory_brid"]!!.jsonPrimitive.content)
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrGenerateClientHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaDocsYmlHelpIsOfficialProjectConfigKeys() = runBlocking {
        val result = ChromiaDocsYmlHelpStrategy().execute(
            CallToolRequest(name = "chromia_docs_yml_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chromia_docs_yml_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("chr generate docs-site", payload["command"]!!.jsonPrimitive.content)
        assertTrue(payload["project_config"]!!.jsonPrimitive.content.contains("project-config"))
        val keys = payload["keys"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(
            listOf(
                "title",
                "footerMessage",
                "customStyleSheets",
                "customAssets",
                "additionalContent",
                "sourceLink.remoteUrl",
                "sourceLink.remoteLineSuffix"
            ),
            keys
        )
        val yaml = payload["docs_yaml"]!!.jsonPrimitive.content
        keys.forEach { key ->
            val leaf = key.substringAfterLast(".")
            assertTrue(yaml.contains("$leaf:"), "docs yaml missing $leaf")
        }
        assertFalse(yaml.contains("additionalModules"))
        assertFalse(yaml.contains("theme:"))
        assertFalse(yaml.contains("nav:"))
        val notOfficial = payload["not_official_keys"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("theme" in notOfficial)
        assertTrue("nav" in notOfficial)
        assertTrue("logo" in notOfficial)
        assertTrue("additionalModules" in notOfficial)
        val leftover = payload["official_docs_keys_not_in_project_config"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("additionalModules" in leftover)
        assertTrue(payload["additional_modules_discrepancy"]!!.jsonPrimitive.content.contains("generating-doc-site"))
        assertTrue(payload["additional_modules_discrepancy"]!!.jsonPrimitive.content.contains("project-config"))
        val suffixes = payload["line_suffixes"]!!.jsonObject
        assertEquals("#L", suffixes["github"]!!.jsonPrimitive.content)
        assertEquals("#L", suffixes["gitlab"]!!.jsonPrimitive.content)
        assertEquals("#lines-", suffixes["bitbucket"]!!.jsonPrimitive.content)
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, yaml, keys.toString()).joinToString("\n")
        assertTrue(notes.contains("project-config") || notes.contains("Official keys"))
        assertTrue(notes.contains("#L"))
        assertTrue(notes.contains("#lines-"))
        assertTrue(notes.contains("rell-doc") || notes.contains("RellDoc"))
        assertEquals(
            "https://docs.chromia.com/rell/rell-doc",
            payload["relldoc_docs"]!!.jsonPrimitive.content
        )
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaDocsYmlHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrLibraryHelpIsOfficialPublicVerbs() = runBlocking {
        val result = ChrLibraryHelpStrategy().execute(
            CallToolRequest(name = "chr_library_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/library",
            payload["commands_library_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/library/",
            payload["commands_library_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("library", payload["commands_library_index_title"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr library install", commands["install"]!!.jsonPrimitive.content)
        assertEquals("chr install", commands["install_alias_of"]!!.jsonPrimitive.content)
        assertTrue(commands["install_id"]!!.jsonPrimitive.content.contains("com.chromia.ft4"))
        assertEquals("chr library list", commands["list"]!!.jsonPrimitive.content)
        assertTrue(commands["view"]!!.jsonPrimitive.content.contains("chr library view"))
        assertTrue(commands["versions"]!!.jsonPrimitive.content.contains("chr library versions"))
        val git = payload["git_yaml"]!!.jsonPrimitive.content
        assertTrue(git.contains("tagOrBranch: v1.1.0r"))
        assertTrue(git.contains("https://gitlab.com/chromaway/ft4-lib.git"))
        assertFalse(git.contains("bitbucket"))
        val chain = payload["library_chain_yaml"]!!.jsonPrimitive.content
        assertTrue(chain.contains("com.chromia.ft4"))
        assertTrue(chain.contains("registry: mainnet"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        val flags = payload["flags"]!!.jsonObject
        val iccfChain = payload["iccf_library_chain_yaml"]!!.jsonPrimitive.content
        val iccfGit = payload["iccf_git_yaml"]!!.jsonPrimitive.content
        val allText = listOf(notes, commands.toString(), flags.toString(), git, chain, iccfChain, iccfGit).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("do not invent") || flags["brid"]!!.jsonPrimitive.content.contains("do not invent"))
        assertTrue(notes.contains("lib.ft4.admin") || payload["forbidden"]!!.jsonArray.toString().contains("lib.ft4.admin"))
        assertEquals("com.chromia.iccf", payload["iccfLibraryChainId"]!!.jsonPrimitive.content)
        assertEquals("1.90.1", payload["iccfLibraryChainVersion"]!!.jsonPrimitive.content)
        assertEquals("1.87.0", payload["iccfGitTag"]!!.jsonPrimitive.content)
        assertTrue(iccfChain.contains("com.chromia.iccf"))
        assertTrue(iccfChain.contains("version: 1.90.1"))
        assertTrue(iccfGit.contains("tagOrBranch: 1.87.0"))
        assertTrue(iccfGit.contains("https://gitlab.com/chromaway/core/directory-chain"))
        assertTrue(commands["versions_iccf"]!!.jsonPrimitive.content.contains("com.chromia.iccf"))
        assertTrue(commands["install_iccf"]!!.jsonPrimitive.content.contains("1.90.1"))
        assertTrue(notes.contains("1.90.1"))
        assertTrue(notes.contains("1.87.0"))
        assertTrue(notes.contains("lib.iccf"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        val hexes = invented.findAll(allText).map { it.value.uppercase() }.toSet()
        val ft4Rid = DappScaffold.FT4_RID.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.uppercase()
        val iccfRid = org.chromia.tools.Ft4ModuleArgs.ICCF_GIT_RID.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.uppercase()
        val allowed = setOf(
            ft4Rid,
            iccfRid,
            WriteDeploymentConfig.MAINNET_DIRECTORY_BRID.uppercase(),
            WriteDeploymentConfig.TESTNET_DIRECTORY_BRID.uppercase()
        )
        assertTrue(hexes.all { it in allowed }, "invented hex: ${hexes - allowed}")
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrLibraryHelp.toJson(), result.structuredContent)
    }

    @Test
    fun requireMandatoryFlagsYamlKeyIsError() {
        val yaml = """
            blockchains:
              hello:
                module: main
                moduleArgs:
                  lib.ft4.core.accounts:
                    require_mandatory_flags: true
                    rate_limit:
                      active: true
                config:
                  features:
                    merkle_hash_version: 2
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.ok)
        assertTrue(
            result.errors.any {
                it.contains("require_mandatory_flags") && it.contains("main")
            },
            result.errors.toString()
        )
        val project = CheckDappProject.check(
            yaml,
            mapOf("src/main.rell" to DappScaffold.files("hello").getValue("src/main.rell"))
        )
        assertFalse(project.ok)
        assertTrue(project.errors.any { it.contains("require_mandatory_flags") }, project.errors.toString())
    }

    @Test
    fun insecureTrueWarnsAndDoesNotFailPins() {
        val yaml = baseYml().replace("insecure: false", "insecure: true")
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(
            result.warnings.any { it.contains("insecure") && it.contains("true") },
            result.warnings.toString()
        )
    }

    @Test
    fun chrCreateRellDappHelpIsOfficialTemplates() = runBlocking {
        val result = ChrCreateRellDappHelpStrategy().execute(
            CallToolRequest(name = "chr_create_rell_dapp_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("21+", payload["java"]!!.jsonPrimitive.content)
        assertEquals("16+", payload["postgres"]!!.jsonPrimitive.content)
        assertEquals("my-rell-dapp", payload["default_folder"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/create-rell-dapp",
            payload["docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/create-rell-dapp",
            payload["create_rell_dapp_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/create-rell-dapp/",
            payload["create_rell_dapp_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("create-rell-dapp", payload["create_rell_dapp_index_title"]!!.jsonPrimitive.content)
        assertTrue(payload["notes"]!!.jsonPrimitive.content.contains("Official BUILD cli/commands/create-rell-dapp"))
        assertTrue(payload["notes"]!!.jsonPrimitive.content.contains("https://docs.chromia.com/build/cli/commands/create-rell-dapp/"))
        assertTrue(payload["notes"]!!.jsonPrimitive.content.contains("chr create-rell-dapp [<options>] [<name>]"))
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr create-rell-dapp", commands["create"]!!.jsonPrimitive.content)
        assertEquals("chr query hello_world", commands["official_loop_query"]!!.jsonPrimitive.content)
        assertEquals("Hello World!", commands["official_loop_result"]!!.jsonPrimitive.content)
        assertEquals("chr node start", commands["official_loop_node"]!!.jsonPrimitive.content)
        assertTrue(commands["named"]!!.jsonPrimitive.content.contains("chr create-rell-dapp"))
        assertTrue(commands["plain"]!!.jsonPrimitive.content.contains("--template=plain"))
        assertTrue(commands["minimal"]!!.jsonPrimitive.content.contains("--template=minimal"))
        assertTrue(commands["plain_multi"]!!.jsonPrimitive.content.contains("--template=plain-multi"))
        assertTrue(commands["plain_library"]!!.jsonPrimitive.content.contains("--template=plain-library"))
        assertTrue(commands["asset_management"]!!.jsonPrimitive.content.contains("--template=asset-management"))
        assertTrue(commands["devcontainer"]!!.jsonPrimitive.content.contains("--devcontainer"))
        assertTrue(commands["docker_devcontainer"]!!.jsonPrimitive.content.contains("create-rell-dapp"))
        assertTrue(commands["docker_devcontainer"]!!.jsonPrimitive.content.contains("--devcontainer"))
        val flags = payload["flags"]!!.jsonObject
        assertTrue(flags["template"]!!.jsonPrimitive.content.contains("plain-multi"))
        assertTrue(flags["template"]!!.jsonPrimitive.content.contains("asset-management"))
        assertTrue(flags["base_dir"]!!.jsonPrimitive.content.contains("--base-dir"))
        assertTrue(flags["devcontainer"]!!.jsonPrimitive.content.contains("--devcontainer"))
        val templates = payload["templates"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(
            listOf("plain", "plain-multi", "minimal", "plain-library", "asset-management"),
            templates
        )
        val layout = payload["layout"]!!.jsonPrimitive.content
        assertTrue(layout.contains("chromia.yml"))
        assertTrue(layout.contains("arithmetic_test.rell"))
        assertTrue(layout.contains("data_test.rell"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, commands.toString(), flags.toString(), layout).joinToString("\n")
        assertTrue(notes.contains("0.16.1"))
        assertTrue(notes.contains("merkle_hash_version"))
        assertTrue(notes.contains("no top-level `chr compile`") || notes.contains("no top-level"))
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("does not run chr") || notes.contains("does not write files"))
        assertTrue(notes.contains("lib.ft4.admin") || notes.contains("ras_open"))
        assertTrue(notes.contains("schema_version") && notes.contains("not official"))
        assertFalse(notes.contains("execute_transaction"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("test.timeout:"))
        assertFalse(allText.contains("admin_pubkey"))
        assertEquals("chr_create_rell_dapp_help", payload["tool"]!!.jsonPrimitive.content)
        val dappBuild = payload["dapp_build_help"]!!.jsonObject
        assertEquals("chr_create_rell_dapp_help", dappBuild["create_help"]!!.jsonPrimitive.content)
        assertEquals("create-rell-dapp", dappBuild["create_rell_dapp_index_title"]!!.jsonPrimitive.content)
        assertEquals("Introduction to Rell", dappBuild["rell_intro_index_title"]!!.jsonPrimitive.content)
        assertEquals("chr_build_help", dappBuild["build_help"]!!.jsonPrimitive.content)
        assertEquals("chromia_yml_definitions_help", dappBuild["yml_help"]!!.jsonPrimitive.content)
        assertEquals("chromia_rell_language_help", dappBuild["rell_language_help"]!!.jsonPrimitive.content)
        assertEquals("chromia_ft4_queries_help", dappBuild["ft4_help"]!!.jsonPrimitive.content)
        assertEquals("chr_deploy_help", dappBuild["deploy_help"]!!.jsonPrimitive.content)
        assertEquals("chromia_vector_search_help", dappBuild["learn_install_cli_help"]!!.jsonPrimitive.content)
        assertEquals("Install Chromia CLI", dappBuild["learn_install_cli_index_title"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["official_loop"]!!.jsonPrimitive.content.contains("chr create-rell-dapp"))
        assertTrue(dappBuild["official_loop"]!!.jsonPrimitive.content.contains("chr node start"))
        assertTrue(dappBuild["official_loop"]!!.jsonPrimitive.content.contains("chr query hello_world"))
        assertEquals("Node is initialized", dappBuild["local_node_initialized"]!!.jsonPrimitive.content)
        assertEquals("http://localhost:7740", dappBuild["rest_url"]!!.jsonPrimitive.content)
        assertEquals("Hello World!", dappBuild["query_result"]!!.jsonPrimitive.content)
        assertEquals("0", dappBuild["chain_id"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["create_silent"]!!.jsonPrimitive.content.contains("silent"))
        assertEquals("build/my_rell_dapp.xml", dappBuild["build_artifact"]!!.jsonPrimitive.content)
        assertEquals("main", dappBuild["yml_module"]!!.jsonPrimitive.content)
        assertEquals("test", dappBuild["test_modules"]!!.jsonPrimitive.content)
        assertEquals("200", dappBuild["rest_get_root"]!!.jsonPrimitive.content)
        assertEquals("org.postgresql.Driver 17.11", dappBuild["postgres_driver"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["postgres"]!!.jsonPrimitive.content.contains("actually worked"))
        assertEquals("chr test", dappBuild["test"]!!.jsonPrimitive.content)
        assertEquals("0 FAILED / 3 PASSED / 3 TOTAL", dappBuild["test_result"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["generate_client"]!!.jsonPrimitive.content.contains("chr generate client-stubs"))
        assertEquals("generated-ts/main/main.ts", dappBuild["generate_client_output"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["generate_client_no_top_level"]!!.jsonPrimitive.content.contains("no top-level chr generate-client"))
        assertTrue(dappBuild["ft4_yml_import"]!!.jsonPrimitive.content.contains("libs.ft4"))
        assertTrue(dappBuild["ft4_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertEquals("generate", dappBuild["generate_index_title"]!!.jsonPrimitive.content)
        assertEquals("Import FT4 into your project", dappBuild["ft4_imports_index_title"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr generate client-stubs"))
        assertTrue(dappBuild["generate_graph"]!!.jsonPrimitive.content.contains("rell.mmd"))
        assertEquals("generated-graph/rell.mmd", dappBuild["generate_graph_file"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["generate_docs_site"]!!.jsonPrimitive.content.contains("chr generate docs-site"))
        assertEquals("generated-docs/index.html", dappBuild["generate_docs_site_output"]!!.jsonPrimitive.content)
        assertEquals("Generate documentation", dappBuild["docs_site_index_title"]!!.jsonPrimitive.content)
        assertEquals("chromia_docs_yml_help", dappBuild["docs_yml_help"]!!.jsonPrimitive.content)
        assertEquals("chr library versions com.chromia.ft4", dappBuild["library_versions"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["library_versions_printed"]!!.jsonPrimitive.content.contains("2.0.2"))
        assertTrue(dappBuild["library_versions_printed"]!!.jsonPrimitive.content.contains("Total: 5 versions"))
        assertTrue(dappBuild["library_versions_no_rid"]!!.jsonPrimitive.content.contains("do not invent a RID"))
        assertTrue(dappBuild["library_versions_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertEquals("chr library view com.chromia.ft4", dappBuild["library_view"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["library_view_printed"]!!.jsonPrimitive.content.contains("Version 1.2.0"))
        assertTrue(dappBuild["library_view_printed"]!!.jsonPrimitive.content.contains("com.chromia.ft4"))
        assertTrue(dappBuild["library_view_no_rid"]!!.jsonPrimitive.content.contains("do not invent a RID"))
        assertTrue(dappBuild["library_view_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["library_view_write_skip"]!!.jsonPrimitive.content.contains("v1.1.0r"))
        assertEquals("chr library list", dappBuild["library_list"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["library_list_no_rid"]!!.jsonPrimitive.content.contains("do not invent a RID"))
        assertTrue(dappBuild["library_list_printed"]!!.jsonPrimitive.content.contains("Total: 20 libraries"))
        assertTrue(dappBuild["library_list_ft4_row"]!!.jsonPrimitive.content.contains("1.2.0"))
        assertTrue(dappBuild["library_list_ft4_row"]!!.jsonPrimitive.content.contains("com.chromia.ft4"))
        assertTrue(dappBuild["library_list_vs_view_vs_versions"]!!.jsonPrimitive.content.contains("do not invent a FT4 semver pin"))
        assertTrue(dappBuild["library_list_vs_view_vs_versions"]!!.jsonPrimitive.content.contains("v1.1.0r"))
        assertTrue(dappBuild["library_list_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["library_list_write_skip"]!!.jsonPrimitive.content.contains("v1.1.0r"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr library list"))
        assertTrue(dappBuild["generate_graph_flags"]!!.jsonPrimitive.content.contains("--mdx"))
        assertTrue(dappBuild["generate_graph_flags"]!!.jsonPrimitive.content.contains("--class-diagram"))
        assertTrue(dappBuild["generate_graph_mdx"]!!.jsonPrimitive.content.contains("rell.mdx"))
        assertEquals("generated-graph-mdx/rell.mdx", dappBuild["generate_graph_mdx_file"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["generate_graph_class_diagram"]!!.jsonPrimitive.content.contains("--class-diagram"))
        assertEquals("generated-graph-class/rell.mmd", dappBuild["generate_graph_class_diagram_file"]!!.jsonPrimitive.content)
        assertEquals("JavaScript/TypeScript client", dappBuild["clients_js_ts_index_title"]!!.jsonPrimitive.content)
        assertEquals("Kotlin client", dappBuild["clients_kotlin_index_title"]!!.jsonPrimitive.content)
        assertEquals("Python client", dappBuild["clients_python_index_title"]!!.jsonPrimitive.content)
        assertEquals("Project settings file", dappBuild["project_config_index_title"]!!.jsonPrimitive.content)
        assertEquals("Getting started", dappBuild["database_getting_started_index_title"]!!.jsonPrimitive.content)
        assertEquals("Chromia Database overview", dappBuild["database_overview_index_title"]!!.jsonPrimitive.content)
        assertEquals("HELP ONLY WRITE SKIP", dappBuild["ft4"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr generate docs-site"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr library versions com.chromia.ft4"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr library view com.chromia.ft4"))
        assertEquals("chr code check", dappBuild["code_check"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["code_check_printed"]!!.jsonPrimitive.content.contains("exit 0"))
        assertTrue(dappBuild["code_lint_hello_world"]!!.jsonPrimitive.content.contains("src/main.rell"))
        assertTrue(dappBuild["code_lint_hello_world"]!!.jsonPrimitive.content.contains("exit 0"))
        assertTrue(dappBuild["code_lint_project"]!!.jsonPrimitive.content.contains("src/lib/ft4"))
        assertTrue(dappBuild["code_lint_project"]!!.jsonPrimitive.content.contains("import:not_found:lib.iccf"))
        assertTrue(dappBuild["code_lint_no_fix"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["code_format_printed"]!!.jsonPrimitive.content.contains("no changes"))
        assertTrue(dappBuild["code_rell_format"]!!.jsonPrimitive.content.contains("max_line_width=120"))
        assertTrue(dappBuild["code_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["code_write_skip"]!!.jsonPrimitive.content.contains("--fix"))
        assertEquals("code", dappBuild["code_index_title"]!!.jsonPrimitive.content)
        assertEquals("chr_repl_help", dappBuild["repl_help"]!!.jsonPrimitive.content)
        assertEquals("repl", dappBuild["repl_index_title"]!!.jsonPrimitive.content)
        assertEquals("chr repl", dappBuild["repl"]!!.jsonPrimitive.content)
        assertEquals("chr repl -c '...'", dappBuild["repl_command"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["repl_arithmetic"]!!.jsonPrimitive.content.contains("1+1"))
        assertTrue(dappBuild["repl_arithmetic"]!!.jsonPrimitive.content.contains("exit 0"))
        assertTrue(dappBuild["repl_module"]!!.jsonPrimitive.content.contains("--module main"))
        assertTrue(dappBuild["repl_blockchain"]!!.jsonPrimitive.content.contains("my_rell_dapp"))
        assertTrue(dappBuild["repl_hello_world_no_db"]!!.jsonPrimitive.content.contains("No database connection"))
        assertTrue(dappBuild["repl_hello_world_use_db"]!!.jsonPrimitive.content.contains("Hello World!"))
        assertTrue(dappBuild["repl_hello_world_use_db"]!!.jsonPrimitive.content.contains("--use-db"))
        assertTrue(dappBuild["repl_sql_log"]!!.jsonPrimitive.content.contains("--sql-log"))
        assertTrue(dappBuild["repl_sql_log"]!!.jsonPrimitive.content.contains("c0.my_name"))
        assertTrue(dappBuild["repl_local_vars"]!!.jsonPrimitive.content.contains("val x = 1"))
        assertTrue(dappBuild["repl_format_string"]!!.jsonPrimitive.content.contains("Hello World!"))
        assertTrue(dappBuild["repl_duration"]!!.jsonPrimitive.content.contains("--duration") or dappBuild["repl_duration"]!!.jsonPrimitive.content.contains("-d"))
        assertTrue(dappBuild["repl_op_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["repl_op_write_skip"]!!.jsonPrimitive.content.contains("rell.test.op"))
        assertTrue(dappBuild["repl_flags"]!!.jsonPrimitive.content.contains("--use-db"))
        assertTrue(dappBuild["repl_output_format_json"]!!.jsonPrimitive.content.contains("-f JSON"))
        assertTrue(dappBuild["repl_output_format_xml"]!!.jsonPrimitive.content.contains("<int>2</int>"))
        assertTrue(dappBuild["repl_output_format_yaml"]!!.jsonPrimitive.content.contains("Unsupported output format YAML"))
        assertTrue(dappBuild["repl_output_format_raw"]!!.jsonPrimitive.content.contains("-f raw"))
        assertTrue(dappBuild["repl_script_stdin"]!!.jsonPrimitive.content.contains("chr repl -"))
        assertTrue(dappBuild["repl_script_args"]!!.jsonPrimitive.content.contains("args: list<text>"))
        assertTrue(dappBuild["repl_command_not_with_script"]!!.jsonPrimitive.content.contains("Cannot use -c"))
        assertTrue(dappBuild["repl_sql_log_needs_use_db"]!!.jsonPrimitive.content.contains("No database connection"))
        assertEquals("chr_tools_help", dappBuild["tools_help"]!!.jsonPrimitive.content)
        assertEquals("tools", dappBuild["tools_index_title"]!!.jsonPrimitive.content)
        assertEquals("query", dappBuild["query_index_title"]!!.jsonPrimitive.content)
        assertEquals("chr tools", dappBuild["tools"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["tools_commands"]!!.jsonPrimitive.content.contains("gtv"))
        assertTrue(dappBuild["tools_commands"]!!.jsonPrimitive.content.contains("validate-config"))
        assertTrue(dappBuild["tools_commands"]!!.jsonPrimitive.content.contains("lib-model"))
        assertEquals("chr tools gtv", dappBuild["tools_gtv"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["tools_gtv_alias"]!!.jsonPrimitive.content.contains("chr gtv"))
        assertTrue(dappBuild["tools_gtv_hex"]!!.jsonPrimitive.content.contains("A41A3018300A0C0161A2050C03464F4F300A0C0162A2050C03424152"))
        assertTrue(dappBuild["tools_gtv_pretty"]!!.jsonPrimitive.content.contains("FOO"))
        assertTrue(dappBuild["tools_gtv_pretty"]!!.jsonPrimitive.content.contains("exit 0"))
        assertTrue(dappBuild["tools_gtv_json"]!!.jsonPrimitive.content.contains("-f JSON"))
        assertTrue(dappBuild["tools_gtv_xml"]!!.jsonPrimitive.content.contains("<dict>"))
        assertTrue(dappBuild["tools_gtv_raw"]!!.jsonPrimitive.content.contains("a=FOO"))
        assertTrue(dappBuild["tools_gtv_yaml"]!!.jsonPrimitive.content.contains("-f YAML"))
        assertTrue(dappBuild["tools_gtv_yaml"]!!.jsonPrimitive.content.contains("exit 0"))
        assertTrue(dappBuild["tools_gtv_alias_decode"]!!.jsonPrimitive.content.contains("chr gtv"))
        assertTrue(dappBuild["tools_gtv_missing_hex"]!!.jsonPrimitive.content.contains("Unexpected end of input stream"))
        assertTrue(dappBuild["tools_gtv_invalid_hex"]!!.jsonPrimitive.content.contains("not a hex digit"))
        assertEquals("chr tools validate-config", dappBuild["tools_validate_config"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["tools_validate_config_printed"]!!.jsonPrimitive.content.contains("No issues found in chromia.yml"))
        assertTrue(dappBuild["tools_validate_config_file_required"]!!.jsonPrimitive.content.contains("missing option --file"))
        assertTrue(dappBuild["tools_validate_config_file_required"]!!.jsonPrimitive.content.contains("Unsupported file format"))
        assertTrue(dappBuild["tools_validate_config_file_required"]!!.jsonPrimitive.content.contains("getParent"))
        assertEquals("chr tools lib-model", dappBuild["tools_lib_model"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["tools_lib_model_source_required"]!!.jsonPrimitive.content.contains("missing option --library-source"))
        assertTrue(dappBuild["tools_lib_model_source_required"]!!.jsonPrimitive.content.contains("Registry must be a valid git URL"))
        assertTrue(dappBuild["tools_lib_model_no_rid"]!!.jsonPrimitive.content.contains("computes"))
        assertTrue(dappBuild["tools_lib_model_no_rid"]!!.jsonPrimitive.content.contains("RID"))
        assertTrue(dappBuild["tools_flags"]!!.jsonPrimitive.content.contains("--library-source"))
        assertTrue(dappBuild["tools_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["tools_no_subcommand"]!!.jsonPrimitive.content.contains("Miscellaneous tools"))
        assertTrue(dappBuild["tools_no_subcommand"]!!.jsonPrimitive.content.contains("exit 0"))
        assertTrue(dappBuild["tools_gtv_yaml_lowercase"]!!.jsonPrimitive.content.contains("-f yaml"))
        assertTrue(dappBuild["tools_gtv_stdin_binary"]!!.jsonPrimitive.content.contains("--output-format yaml < data.gtv"))
        assertTrue(dappBuild["tools_gtv_hex_precedence"]!!.jsonPrimitive.content.contains("--hex wins over piped stdin"))
        assertTrue(dappBuild["tools_gtv_hash"]!!.jsonPrimitive.content.contains("--hash=2"))
        assertTrue(dappBuild["tools_gtv_hash"]!!.jsonPrimitive.content.contains("never record or invent"))
        assertTrue(dappBuild["tools_gtv_hash_zero"]!!.jsonPrimitive.content.contains("Merkle hash version must be greater than 0"))
        assertTrue(dappBuild["tools_validate_config_relative_workaround"]!!.jsonPrimitive.content.contains("No issues found in chromia.yml"))
        assertTrue(dappBuild["tools_validate_config_relative_workaround"]!!.jsonPrimitive.content.contains("getParent"))
        assertTrue(dappBuild["tools_validate_config_path_errors"]!!.jsonPrimitive.content.contains("does not exist"))
        assertTrue(dappBuild["tools_validate_config_path_errors"]!!.jsonPrimitive.content.contains("is a directory"))
        assertTrue(dappBuild["tools_validate_config_yaml_ext"]!!.jsonPrimitive.content.contains("Unsupported file format"))
        assertTrue(dappBuild["tools_validate_config_cookbook_keys"]!!.jsonPrimitive.content.contains("test->timeout"))
        assertTrue(dappBuild["tools_validate_config_cookbook_keys"]!!.jsonPrimitive.content.contains("database->schema_version"))
        assertTrue(dappBuild["tools_validate_config_cookbook_keys"]!!.jsonPrimitive.content.contains("exit 2"))
        assertTrue(dappBuild["tools_validate_config_unknown_section"]!!.jsonPrimitive.content.contains("not_a_section"))
        assertTrue(dappBuild["tools_validate_config_scaffold_dapp"]!!.jsonPrimitive.content.contains("No issues found"))
        assertTrue(dappBuild["tools_validate_config_scaffold_dapp"]!!.jsonPrimitive.content.contains("scaffold_dapp"))
        assertTrue(dappBuild["tools_lib_model_printed_shape"]!!.jsonPrimitive.content.contains("tagOrBranch"))
        assertTrue(dappBuild["tools_lib_model_printed_shape"]!!.jsonPrimitive.content.contains("insecure: false"))
        assertTrue(dappBuild["tools_lib_model_rid_matches_disk"]!!.jsonPrimitive.content.contains("do not invent a RID"))
        assertTrue(dappBuild["tools_lib_model_rid_matches_disk"]!!.jsonPrimitive.content.contains("v1.1.0r"))
        assertTrue(dappBuild["tools_lib_model_insecure"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("deep walk"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr code check"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr repl"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr tools"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("tools walk done"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr query"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr query after node walk done"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("Deployment"))
        assertEquals("chr query", dappBuild["query"]!!.jsonPrimitive.content)
        assertEquals("chr_query_help", dappBuild["query_help_tool"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["query_missing_name"]!!.jsonPrimitive.content.contains("missing argument <queryname>"))
        assertTrue(dappBuild["query_hello_world"]!!.jsonPrimitive.content.contains("Hello World!"))
        assertTrue(dappBuild["query_hello_world"]!!.jsonPrimitive.content.contains("exit 0"))
        assertTrue(dappBuild["query_hello_world"]!!.jsonPrimitive.content.contains("needs no --blockchain-rid"))
        assertTrue(dappBuild["query_output_format"]!!.jsonPrimitive.content.contains("pretty|raw|JSON|XML|YAML"))
        assertTrue(dappBuild["query_output_format_pretty_json"]!!.jsonPrimitive.content.contains("-f JSON"))
        assertTrue(dappBuild["query_output_format_xml"]!!.jsonPrimitive.content.contains("<string>Hello World!</string>"))
        assertTrue(dappBuild["query_output_format_raw"]!!.jsonPrimitive.content.contains("no quotes"))
        assertTrue(dappBuild["query_output_format_yaml"]!!.jsonPrimitive.content.contains("--- Hello World!"))
        assertTrue(dappBuild["query_output_format_yaml"]!!.jsonPrimitive.content.contains("unlike chr repl"))
        assertTrue(dappBuild["query_unknown"]!!.jsonPrimitive.content.contains("Unknown query: leftover_no_such_query"))
        assertTrue(dappBuild["query_cid"]!!.jsonPrimitive.content.contains("--cid 0"))
        assertTrue(dappBuild["query_api_url"]!!.jsonPrimitive.content.contains("http://localhost:7740"))
        assertTrue(dappBuild["query_settings"]!!.jsonPrimitive.content.contains("-s chromia.yml"))
        assertTrue(dappBuild["query_invalid_arg"]!!.jsonPrimitive.content.contains("Invalid argument(s): foo"))
        assertTrue(dappBuild["query_dashdash"]!!.jsonPrimitive.content.contains("hello_world --"))
        assertTrue(dappBuild["query_op_as_query"]!!.jsonPrimitive.content.contains("Unknown query: set_name"))
        assertTrue(dappBuild["query_op_as_query"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["query_blockchain_needs_network"]!!.jsonPrimitive.content.contains("missing option --network"))
        assertTrue(dappBuild["query_cid_missing"]!!.jsonPrimitive.content.contains("404 Not Found"))
        assertTrue(dappBuild["query_brid_invalid_hex"]!!.jsonPrimitive.content.contains("Char Z is not a hex digit"))
        assertTrue(dappBuild["query_brid_wrong_size"]!!.jsonPrimitive.content.contains("Wrong size of Blockchain RID"))
        assertTrue(dappBuild["query_brid_wrong_size"]!!.jsonPrimitive.content.contains("do not invent"))
        assertTrue(dappBuild["query_brid_alias"]!!.jsonPrimitive.content.contains("-brid"))
        assertTrue(dappBuild["query_mainnet_local_query"]!!.jsonPrimitive.content.contains("Unknown query: hello_world"))
        assertTrue(dappBuild["query_testnet_timeout"]!!.jsonPrimitive.content.contains("timed out"))
        assertTrue(dappBuild["query_from_parent"]!!.jsonPrimitive.content.contains("Hello World!"))
        assertTrue(dappBuild["query_parent_settings"]!!.jsonPrimitive.content.contains("my-rell-dapp/chromia.yml"))
        assertTrue(dappBuild["query_settings_missing"]!!.jsonPrimitive.content.contains("does not exist"))
        assertTrue(dappBuild["query_network_missing"]!!.jsonPrimitive.content.contains("Specified target [testnet] does not exist"))
        assertTrue(dappBuild["query_api_refused"]!!.jsonPrimitive.content.contains("Connection refused"))
        assertTrue(dappBuild["query_bad_format"]!!.jsonPrimitive.content.contains("invalid choice: FOO"))
        assertTrue(dappBuild["query_config_missing"]!!.jsonPrimitive.content.contains("--config"))
        assertTrue(dappBuild["query_empty_name"]!!.jsonPrimitive.content.contains("Unknown query:"))
        assertTrue(dappBuild["query_rest_root"]!!.jsonPrimitive.content.contains("Postchain REST API"))
        assertTrue(dappBuild["query_node_initialized"]!!.jsonPrimitive.content.contains("Node is initialized"))
        assertTrue(dappBuild["query_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["query_flags"]!!.jsonPrimitive.content.contains("--blockchain-rid"))
        assertTrue(dappBuild["query_help_examples"]!!.jsonPrimitive.content.contains("primitive_args"))
        assertTrue(dappBuild["query_help_examples"]!!.jsonPrimitive.content.contains("dict_arg"))
        assertTrue(dappBuild["query_help_groups"]!!.jsonPrimitive.content.contains("dApp target options"))
        assertTrue(dappBuild["query_help_arg_types"]!!.jsonPrimitive.content.contains("named parameters"))
        assertTrue(dappBuild["query_positional_arg"]!!.jsonPrimitive.content.contains("named parameters in a dict"))
        assertTrue(dappBuild["query_unknown_option"]!!.jsonPrimitive.content.contains("Did you mean -f?"))
        assertTrue(dappBuild["query_cid_not_integer"]!!.jsonPrimitive.content.contains("not a valid integer"))
        assertTrue(dappBuild["query_dict_arg_unknown"]!!.jsonPrimitive.content.contains("Invalid argument(s): arg"))
        assertTrue(dappBuild["query_output_format_long"]!!.jsonPrimitive.content.contains("--output-format JSON"))
        assertTrue(dappBuild["query_ft4_absent"]!!.jsonPrimitive.content.contains("Unknown query"))
        assertTrue(dappBuild["query_ft4_absent"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["query_gtx_metadata"]!!.jsonPrimitive.content.contains("last_block_info"))
        assertTrue(dappBuild["query_gtx_metadata"]!!.jsonPrimitive.content.contains("StandardOpsGTXModule"))
        assertTrue(dappBuild["query_last_block_info"]!!.jsonPrimitive.content.contains("blockRID"))
        assertTrue(dappBuild["query_last_block_info"]!!.jsonPrimitive.content.contains("never record"))
        assertTrue(dappBuild["query_last_block_info_formats"]!!.jsonPrimitive.content.contains("-f YAML"))
        assertTrue(dappBuild["query_last_block_info_extra_arg"]!!.jsonPrimitive.content.contains("ignores extra named args"))
        assertTrue(dappBuild["query_tx_confirmation_time_requires_arg"]!!.jsonPrimitive.content.contains("500 Internal Server Error"))
        assertTrue(dappBuild["query_rest_iid_alias"]!!.jsonPrimitive.content.contains("/query/iid_0?type=hello_world"))
        assertTrue(dappBuild["query_rest_iid_alias"]!!.jsonPrimitive.content.contains("Hello World!"))
        assertTrue(dappBuild["query_rest_missing_type"]!!.jsonPrimitive.content.contains("Missing query type"))
        assertTrue(dappBuild["query_rest_post"]!!.jsonPrimitive.content.contains("query-only"))
        assertTrue(dappBuild["query_rest_unknown_query"]!!.jsonPrimitive.content.contains("QUERY_NOT_FOUND"))
        assertTrue(dappBuild["query_rest_unknown_iid"]!!.jsonPrimitive.content.contains("chain Iid: 99"))
        assertTrue(dappBuild["query_rest_query_gtv"]!!.jsonPrimitive.content.contains("/query_gtv"))
        assertTrue(dappBuild["query_rest_dquery_web_query"]!!.jsonPrimitive.content.contains("path, query_params"))
        assertTrue(dappBuild["query_rest_height_state"]!!.jsonPrimitive.content.contains("RUNNING_VALIDATOR"))
        assertTrue(dappBuild["query_rest_config_xml"]!!.jsonPrimitive.content.contains("HEADER_HASH"))
        assertTrue(dappBuild["query_rest_version"]!!.jsonPrimitive.content.contains("3.49.16"))
        assertTrue(dappBuild["query_rest_debug_moved"]!!.jsonPrimitive.content.contains("7750"))
        assertTrue(dappBuild["query_rest_404s"]!!.jsonPrimitive.content.contains("/brid"))
        assertTrue(dappBuild["query_rest_cors"]!!.jsonPrimitive.content.contains("X-Accept-Query-Response-Signature"))
        assertTrue(dappBuild["query_rest_apidocs"]!!.jsonPrimitive.content.contains("postchain-restapi.yaml"))
        assertTrue(dappBuild["query_rest_openapi_query_group"]!!.jsonPrimitive.content.contains("QUERY_NOT_FOUND"))
        assertTrue(dappBuild["query_no_node_stop"]!!.jsonPrimitive.content.contains("no chr node stop"))
        assertTrue(dappBuild["query_brid_never_pasted"]!!.jsonPrimitive.content.contains("<BlockchainRID>"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("last_block_info"))
        assertEquals("chr_deploy_help", dappBuild["deploy_help_tool"]!!.jsonPrimitive.content)
        assertEquals("Deployment", dappBuild["deploy_index_title"]!!.jsonPrimitive.content)
        assertEquals("deployment", dappBuild["deploy_commands_index_title"]!!.jsonPrimitive.content)
        assertEquals("Deploy your dapp to testnet", dappBuild["deploy_testnet_deploy_dapp_index_title"]!!.jsonPrimitive.content)
        assertEquals("Deploy your dapp to Mainnet", dappBuild["deploy_mainnet_deploy_dapp_index_title"]!!.jsonPrimitive.content)
        assertEquals("Getting started", dappBuild["deploy_testnet_getting_started_index_title"]!!.jsonPrimitive.content)
        assertEquals("Get started with Mainnet deployment", dappBuild["deploy_mainnet_getting_started_index_title"]!!.jsonPrimitive.content)
        assertEquals("Deploy your dapp to Testnet", dappBuild["deploy_get_started_testnet_index_title"]!!.jsonPrimitive.content)
        assertEquals("Project settings file", dappBuild["deploy_yml_index_title"]!!.jsonPrimitive.content)
        assertEquals("chromia_yml_definitions_help", dappBuild["deploy_yml_help"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["deploy_no_such_subcommand"]!!.jsonPrimitive.content.contains("no such subcommand deploy"))
        assertTrue(dappBuild["deploy_no_such_subcommand"]!!.jsonPrimitive.content.contains("Did you mean deployment?"))
        assertTrue(dappBuild["deploy_subcommands"]!!.jsonPrimitive.content.contains("create"))
        assertTrue(dappBuild["deploy_subcommands"]!!.jsonPrimitive.content.contains("inspect"))
        assertTrue(dappBuild["deploy_subcommands"]!!.jsonPrimitive.content.contains("container"))
        assertTrue(dappBuild["deploy_create_flags"]!!.jsonPrimitive.content.contains("-d/--network"))
        assertTrue(dappBuild["deploy_create_flags"]!!.jsonPrimitive.content.contains("-bc/--blockchain"))
        assertTrue(dappBuild["deploy_create_flags"]!!.jsonPrimitive.content.contains("NOT RUN"))
        assertTrue(dappBuild["deploy_create_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["deploy_create_write_skip"]!!.jsonPrimitive.content.contains("Please specify -y option to force deployment"))
        assertTrue(dappBuild["deploy_update_flags"]!!.jsonPrimitive.content.contains("--verify-only"))
        assertTrue(dappBuild["deploy_update_flags"]!!.jsonPrimitive.content.contains("--skip-verification"))
        assertTrue(dappBuild["deploy_update_write_skip"]!!.jsonPrimitive.content.contains("never run"))
        assertTrue(dappBuild["deploy_info_flags"]!!.jsonPrimitive.content.contains("-f/--output-format=(table|JSON)"))
        assertTrue(dappBuild["deploy_info_local"]!!.jsonPrimitive.content.contains("cm_get_blockchain_cluster"))
        assertTrue(dappBuild["deploy_info_local"]!!.jsonPrimitive.content.contains("never paste the computed BRID"))
        assertTrue(dappBuild["deploy_inspect_flags"]!!.jsonPrimitive.content.contains("--list-modules"))
        assertTrue(dappBuild["deploy_inspect_local"]!!.jsonPrimitive.content.contains("hello_world"))
        assertTrue(dappBuild["deploy_inspect_local"]!!.jsonPrimitive.content.contains("set_name"))
        assertTrue(dappBuild["deploy_inspect_list_modules"]!!.jsonPrimitive.content.contains("main"))
        assertTrue(dappBuild["deploy_inspect_definitions"]!!.jsonPrimitive.content.contains("--definitions=queries"))
        assertTrue(dappBuild["deploy_inspect_signature"]!!.jsonPrimitive.content.contains("--signature=hello_world"))
        assertTrue(dappBuild["deploy_inspect_module_args"]!!.jsonPrimitive.content.contains("[]"))
        assertTrue(dappBuild["deploy_inspect_table"]!!.jsonPrimitive.content.contains("-f table"))
        assertTrue(dappBuild["deploy_network_missing"]!!.jsonPrimitive.content.contains("Specified target [testnet] does not exist"))
        assertTrue(dappBuild["deploy_brid_invalid_hex"]!!.jsonPrimitive.content.contains("Char Z is not a hex digit"))
        assertTrue(dappBuild["deploy_brid_wrong_size"]!!.jsonPrimitive.content.contains("Wrong size of Blockchain RID"))
        assertTrue(dappBuild["deploy_key_flags"]!!.jsonPrimitive.content.contains("--secret=<path>"))
        assertTrue(dappBuild["deploy_yml_deployments"]!!.jsonPrimitive.content.contains("deployments.<net>.url"))
        assertTrue(dappBuild["deploy_yml_write_back"]!!.jsonPrimitive.content.contains("0.30.0"))
        assertTrue(dappBuild["deploy_sign_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["deploy_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["deploy_write_skip"]!!.jsonPrimitive.content.contains("no invented BlockchainRID"))
        assertTrue(dappBuild["deploy_nothing_deployed"]!!.jsonPrimitive.content.contains("nothing was deployed"))
        assertEquals("chr deployment", dappBuild["deploy"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["deploy_commands"]!!.jsonPrimitive.content.contains("create"))
        assertTrue(dappBuild["deploy_commands"]!!.jsonPrimitive.content.contains("inspect"))
        assertTrue(dappBuild["deploy_commands"]!!.jsonPrimitive.content.contains("container"))
        assertTrue(dappBuild["deploy_missing_subcommand"]!!.jsonPrimitive.content.contains("no such subcommand deploy"))
        assertTrue(dappBuild["deploy_create_help"]!!.jsonPrimitive.content.contains("Deploy new blockchain instance"))
        assertTrue(dappBuild["deploy_create_help"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["deploy_update_help"]!!.jsonPrimitive.content.contains("--verify-only"))
        assertTrue(dappBuild["deploy_inspect_help"]!!.jsonPrimitive.content.contains("Inspect the API of a deployed blockchain"))
        assertTrue(dappBuild["deploy_inspect_help"]!!.jsonPrimitive.content.contains("hello_world"))
        assertTrue(dappBuild["deploy_info_help"]!!.jsonPrimitive.content.contains("Information about a deployed blockchain"))
        assertTrue(dappBuild["deploy_info_help"]!!.jsonPrimitive.content.contains("cm_get_blockchain_cluster"))
        assertTrue(dappBuild["deploy_no_deployments_block"]!!.jsonPrimitive.content.contains("Specified target [testnet] does not exist"))
        assertTrue(dappBuild["deploy_settings_missing"]!!.jsonPrimitive.content.contains("does not exist"))
        assertTrue(dappBuild["deploy_flags"]!!.jsonPrimitive.content.contains("-d/--network"))
        assertTrue(dappBuild["deploy_proposal_list_help"]!!.jsonPrimitive.content.contains("missing option --network"))
        assertTrue(dappBuild["deploy_voterset_info_help"]!!.jsonPrimitive.content.contains("must provide one of --name, --container"))
        assertTrue(dappBuild["deploy_container_help"]!!.jsonPrimitive.content.contains("Manage container operations"))
        assertTrue(dappBuild["deploy_container_help"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["deploy_inspect_brid_invalid_hex"]!!.jsonPrimitive.content.contains("Char Z is not a hex digit"))
        assertTrue(dappBuild["deploy_inspect_brid_wrong_size"]!!.jsonPrimitive.content.contains("Wrong size of Blockchain RID"))
        assertTrue(dappBuild["deploy_api_refused"]!!.jsonPrimitive.content.contains("Connection Refused"))
        assertTrue(dappBuild["deploy_blockchain_needs_network"]!!.jsonPrimitive.content.contains("missing option --network"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("deploy HELP ONLY walk done"))

        assertEquals("vault_lease_help", dappBuild["vault"]!!.jsonPrimitive.content)
        assertEquals("Get a container for your dapp", dappBuild["vault_testnet_get_container_index_title"]!!.jsonPrimitive.content)
        assertEquals("Hosting", dappBuild["vault_hosting_index_title"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["vault_workflow"]!!.jsonPrimitive.content.contains("never invent"))
        assertTrue(dappBuild["vault_workflow"]!!.jsonPrimitive.content.contains("<containerIID>"))
        assertTrue(dappBuild["vault_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertEquals("chr tx", dappBuild["tx"]!!.jsonPrimitive.content)
        assertEquals("tx", dappBuild["tx_index_title"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["tx_help_text"]!!.jsonPrimitive.content.contains("Make a transaction towards a node"))
        assertTrue(dappBuild["tx_missing_opname"]!!.jsonPrimitive.content.contains("missing argument <opname>"))
        assertTrue(dappBuild["tx_no_node"]!!.jsonPrimitive.content.contains("Connection Refused"))
        assertTrue(dappBuild["tx_brid_invalid_hex"]!!.jsonPrimitive.content.contains("Char Z is not a hex digit"))
        assertTrue(dappBuild["tx_brid_wrong_size"]!!.jsonPrimitive.content.contains("Wrong size of Blockchain RID"))
        assertTrue(dappBuild["tx_network_missing"]!!.jsonPrimitive.content.contains("Specified target [testnet] does not exist"))
        assertTrue(dappBuild["tx_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertEquals("chr multi-signature", dappBuild["multi"]!!.jsonPrimitive.content)
        assertEquals("multi-signature", dappBuild["multi_index_title"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["multi_view_help"]!!.jsonPrimitive.content.contains("missing option --file"))
        assertTrue(dappBuild["multi_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertEquals("chr seeder", dappBuild["seeder"]!!.jsonPrimitive.content)
        assertEquals("Seeder", dappBuild["seeder_index_title"]!!.jsonPrimitive.content)
        assertEquals("seeder", dappBuild["seeder_commands_index_title"]!!.jsonPrimitive.content)
        assertEquals("Available generators", dappBuild["seeder_generator_index_title"]!!.jsonPrimitive.content)
        assertEquals("Using the seeder", dappBuild["seeder_example_index_title"]!!.jsonPrimitive.content)
        assertEquals("Configurable generators", dappBuild["seeder_configurable_index_title"]!!.jsonPrimitive.content)
        assertEquals(".chromia/seeder", dappBuild["seeder_default_config_folder"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["seeder_help_text"]!!.jsonPrimitive.content.contains("Generate fake data for a local database"))
        assertTrue(dappBuild["seeder_missing_subcommand"]!!.jsonPrimitive.content.contains("Did you mean seeder?"))
        assertTrue(dappBuild["seeder_init_help"]!!.jsonPrimitive.content.contains("Create initial seeder configuration"))
        assertTrue(dappBuild["seeder_init_help"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["seeder_generate_help"]!!.jsonPrimitive.content.contains("--alternative-config-folder"))
        assertTrue(dappBuild["seeder_generate_help"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["seeder_settings_missing"]!!.jsonPrimitive.content.contains("does not exist"))
        assertTrue(dappBuild["seeder_no_project"]!!.jsonPrimitive.content.contains("Project settings file not found"))
        assertTrue(dappBuild["seeder_unknown_option"]!!.jsonPrimitive.content.contains("no such option --foo"))
        assertTrue(dappBuild["seeder_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["seeder_write_skip"]!!.jsonPrimitive.content.contains("never run"))
        assertEquals("chr eif", dappBuild["eif"]!!.jsonPrimitive.content)
        assertEquals("chr_eif_help", dappBuild["eif_help"]!!.jsonPrimitive.content)
        assertEquals("eif", dappBuild["eif_index_title"]!!.jsonPrimitive.content)
        assertEquals("Governance Tool EIF extension", dappBuild["eif_gov_index_title"]!!.jsonPrimitive.content)
        assertEquals("build/eif-events.yaml", dappBuild["eif_default_target"]!!.jsonPrimitive.content)
        assertTrue(dappBuild["eif_help_text"]!!.jsonPrimitive.content.contains("Ethereum Integration Framework"))
        assertTrue(dappBuild["eif_generate_help"]!!.jsonPrimitive.content.contains("generate-events-config"))
        assertTrue(dappBuild["eif_generate_help"]!!.jsonPrimitive.content.contains("--abi"))
        assertTrue(dappBuild["eif_generate_help"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["eif_missing_abi"]!!.jsonPrimitive.content.contains("missing option --abi"))
        assertTrue(dappBuild["eif_missing_events"]!!.jsonPrimitive.content.contains("missing option --events"))
        assertTrue(dappBuild["eif_unknown_option"]!!.jsonPrimitive.content.contains("Did you mean --format?"))
        assertTrue(dappBuild["eif_missing_subcommand"]!!.jsonPrimitive.content.contains("Did you mean generate-events-config?"))
        assertTrue(dappBuild["eif_write_skip"]!!.jsonPrimitive.content.contains("HELP ONLY WRITE SKIP"))
        assertTrue(dappBuild["eif_write_skip"]!!.jsonPrimitive.content.contains("never run"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("vault-lease HELP ONLY walk done"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("chr tx HELP ONLY walk done"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("multi-signature HELP ONLY walk done"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("seeder HELP ONLY walk done"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("eif HELP ONLY walk done"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("version HELP ONLY"))
        assertTrue(dappBuild["next_step_architecture"]!!.jsonPrimitive.content.contains("seeder"))
        assertEquals("node", dappBuild["query_node_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("dapp-build INDEX help map"))
        assertTrue(notes.contains("next-step architecture INDEX map"))
        assertTrue(notes.contains("library versions generate graph generate docs-site walk"))
        assertTrue(notes.contains("library view generate graph --mdx --class-diagram walk"))
        assertTrue(notes.contains("library list walk"))
        assertTrue(notes.contains("code check lint format walk"))
        assertTrue(notes.contains("repl walk"))
        assertTrue(notes.contains("tools walk"))
        assertTrue(notes.contains("tools deep walk"))
        assertTrue(notes.contains("query after node walk"))
        assertTrue(notes.contains("query deep walk"))
        assertTrue(notes.contains("deploy HELP ONLY walk"))
        assertTrue(notes.contains("vault lease HELP ONLY walk"))
        assertTrue(notes.contains("tx HELP ONLY walk"))
        assertTrue(notes.contains("multi-signature HELP ONLY walk"))
        assertTrue(notes.contains("seeder HELP ONLY walk"))
        assertTrue(notes.contains("eif HELP ONLY walk"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrCreateRellDappHelp.toJson(), result.structuredContent)
    }

    @Test
    fun officialIccfLibraryChainYamlValidatesOnScaffold() {
        val yaml = baseYml().trimEnd() + "\n  com.chromia.iccf:\n    version: 1.90.1\n"
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(yaml.contains("com.chromia.iccf"))
        assertTrue(yaml.contains("version: 1.90.1"))
        assertFalse(result.errors.any { it.contains("iccf") })
    }

    @Test
    fun newHelpToolsAreRegisteredAndPrompted() {
        val names = McpTools.allTools().map { it.name }.toSet()
        assertTrue("chr_generate_client_help" in names)
        assertTrue("chromia_docs_yml_help" in names)
        assertTrue("chr_library_help" in names)
        assertTrue("chr_create_rell_dapp_help" in names)
        assertTrue("chr_tools_help" in names)
        assertTrue("chr_seeder_help" in names)
        assertTrue("blockchain_properties_help" in names)
        assertTrue("chr_eif_help" in names)
        assertTrue("chromia_yml_definitions_help" in names)
        assertTrue("chr_completion_help" in names)
        assertTrue("chromia_project_structure_help" in names)
        assertTrue("chr_multi_signature_help" in names)
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
            "chr_generate_client_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI generate-client help")!!)
            )
        )
        assertEquals(
            "chromia_docs_yml_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "chromia.yml docs section help")!!)
            )
        )
        assertEquals(
            "chr_library_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI library help")!!)
            )
        )
        assertEquals(
            "chr_create_rell_dapp_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI create-rell-dapp help")!!)
            )
        )
        assertEquals(
            "chr_tools_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI tools help")!!)
            )
        )
        assertEquals(
            "chr_seeder_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI seeder help")!!)
            )
        )
        assertEquals(
            "blockchain_properties_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "chromia.yml blockchain-properties help")!!)
            )
        )
        assertEquals(
            "chr_eif_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI eif help")!!)
            )
        )
        assertEquals(
            "chromia_yml_definitions_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "chromia.yml definitions / YAML include help")!!)
            )
        )
        assertEquals(
            "chr_completion_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI completion help")!!)
            )
        )
        assertEquals(
            "chromia_project_structure_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "chromia.yml project-structure / Rell modules help")!!)
            )
        )
        assertEquals(
            "chr_multi_signature_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI multi-signature view help")!!)
            )
        )
        assertEquals(
            "chromia_cookbook_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia cookbook help")!!)
            )
        )
        assertEquals(
            "chr_key_id_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia CLI existing-key reference")!!)
            )
        )
        assertEquals(
            "chromia_language_clients_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia language clients help")!!)
            )
        )
        assertEquals(
            "chromia_rell_language_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell language definition help")!!)
            )
        )
        assertEquals(
            "chromia_rell_types_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell types help")!!)
            )
        )
        assertEquals(
            "chromia_rell_expressions_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell expressions help")!!)
            )
        )
        assertEquals(
            "chromia_rell_statements_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell statements help")!!)
            )
        )
        assertEquals(
            "chromia_rell_database_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell database language help")!!)
            )
        )
        assertEquals(
            "chromia_rell_systemlib_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell system library help")!!)
            )
        )
        assertEquals(
            "chromia_rell_practices_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Rell security and best-practices help")!!)
            )
        )
        assertEquals(
            "chromia_ft4_queries_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "FT4 read-only query catalog")!!)
            )
        )
        assertEquals(
            "chromia_integrations_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia integrations hub help")!!)
            )
        )
        assertEquals(
            "chromia_vector_search_help",
            prompts.canonicalToolName(
                prompts.getToolForPrompt(prompts.getPrompt("dapp_build", "Chromia vector-search help")!!)
            )
        )
    }

    @Test
    fun chromiaCookbookHelpIsQueryAndTestOnly() = runBlocking {
        val result = ChromiaCookbookHelpStrategy().execute(
            CallToolRequest(name = "chromia_cookbook_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr query hello_world", commands["query_local_default"]!!.jsonPrimitive.content)
        assertTrue(commands["query_local_brid"]!!.jsonPrimitive.content.contains("hello_world"))
        assertEquals("chr test", commands["test"]!!.jsonPrimitive.content)
        assertTrue(commands["test_modules"]!!.jsonPrimitive.content.contains("--modules"))
        val pages = payload["pages"]!!.jsonObject
        val included = pages["included"]!!.jsonArray.map { it.jsonPrimitive.content }
        val skipped = pages["skipped_sign_or_key"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(included.any { it.contains("query-creation/make-query") })
        assertTrue(included.any { it.contains("cli/run-tests") })
        assertTrue(included.any { it.contains("get-transaction-status") })
        assertEquals(
            listOf("Unknown", "Waiting", "Confirmed", "Rejected"),
            payload["tx_statuses"]!!.jsonArray.map { it.jsonPrimitive.content }
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-status",
            payload["tx_status_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-status/",
            payload["tx_status_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-data",
            payload["tx_data_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-data/",
            payload["tx_data_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to get and decode transaction data",
            payload["tx_data_title"]!!.jsonPrimitive.content
        )
        assertEquals(
            "JavaScript get and decode transaction",
            payload["tx_data_js_tab"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference#transactions",
            payload["tx_data_js_reference"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-block-data",
            payload["block_data_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-block-data/",
            payload["block_data_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to fetch and decode block data",
            payload["block_data_title"]!!.jsonPrimitive.content
        )
        assertEquals("JS/TS client", payload["block_data_js_tab"]!!.jsonPrimitive.content)
        assertEquals("npm install postchain-client", payload["block_data_npm"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("get-transaction-data") })
        assertTrue(included.any { it.contains("get-block-data") })
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-id",
            payload["account_by_id_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-id/",
            payload["account_by_id_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to get an account by ID",
            payload["account_by_id_title"]!!.jsonPrimitive.content
        )
        assertEquals("JS/TS client", payload["account_by_id_js_tab"]!!.jsonPrimitive.content)
        assertEquals("npm install @chromia/ft4", payload["account_by_id_ft4"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/ft4-client", payload["account_by_id_ft4_client_url"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/ft4/account-management/", payload["account_by_id_account_mgmt_url"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("get-account-by-id") })
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-signer",
            payload["account_by_signer_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-signer/",
            payload["account_by_signer_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to get accounts by signer",
            payload["account_by_signer_title"]!!.jsonPrimitive.content
        )
        assertEquals("JS/TS client", payload["account_by_signer_js_tab"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("get-account-by-signer") })
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-transfer-history",
            payload["account_transfer_history_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-transfer-history/",
            payload["account_transfer_history_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to get account transfer history",
            payload["account_transfer_history_title"]!!.jsonPrimitive.content
        )
        assertEquals("JS/TS client", payload["account_transfer_history_js_tab"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("get-account-transfer-history") })
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/pagination-with-ft4",
            payload["pagination_ft4_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/pagination-with-ft4/",
            payload["pagination_ft4_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to implement pagination with FT4",
            payload["pagination_ft4_title"]!!.jsonPrimitive.content
        )
        assertEquals("JS/TS client", payload["pagination_ft4_js_tab"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("pagination-with-ft4") })
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/check-account-memo-requirement",
            payload["memo_query_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/check-account-memo-requirement/",
            payload["memo_query_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to check account memo requirement",
            payload["memo_query_title"]!!.jsonPrimitive.content
        )
        assertEquals("JS/TS client", payload["memo_query_js_tab"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/ft4/asset-management/",
            payload["memo_asset_mgmt_url"]!!.jsonPrimitive.content
        )
        assertTrue(included.any { it.contains("check-account-memo-requirement") })
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/pagination",
            payload["pagination_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/pagination/",
            payload["pagination_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to implement custom pagination",
            payload["pagination_title"]!!.jsonPrimitive.content
        )
        assertEquals("JS/TS client", payload["pagination_js_tab"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("query-creation/pagination") })
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/make-query",
            payload["make_query_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/make-query/",
            payload["make_query_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals(
            "How to make queries with parameters",
            payload["make_query_title"]!!.jsonPrimitive.content
        )
        assertEquals("JS/TS client", payload["make_query_js_tab"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("query-creation/make-query") })
        assertTrue(skipped.any { it.contains("run-operations") })
        assertTrue(skipped.any { it.contains("account-creation") })
        assertTrue(skipped.any { it.contains("transaction-creation") })
        val notes = payload["notes"]!!.jsonPrimitive.content
        val builders = payload["rell_test_builders"]!!.jsonArray.map { it.jsonPrimitive.content }
        val asserts = payload["rell_test_asserts"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(builders.any { it.contains("rell.test.tx") })
        assertTrue(builders.any { it.contains(".run_must_fail") })
        assertTrue(asserts.any { it.contains("assert_equals") })
        assertTrue(asserts.any { it.contains("assert_events") })
        assertTrue(payload["rell_test_tx_example"]!!.jsonPrimitive.content.contains("rell.test.tx"))
        assertTrue(payload["rell_test_disabled_example"]!!.jsonPrimitive.content.contains("@disabled"))
        assertTrue(payload["rell_test_must_fail_example"]!!.jsonPrimitive.content.contains("run_must_fail"))
        assertEquals(
            "https://docs.chromia.com/rell/rell-best-practices",
            payload["best_practices"]!!.jsonPrimitive.content
        )
        val times = payload["rell_test_time"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(times.any { it.contains("block_interval") })
        assertTrue(notes.contains("@disabled"))
        assertTrue(notes.contains("rell.test.tx"))
        val allText = listOf(notes, commands.toString(), payload["rell_test"]!!.jsonPrimitive.content).joinToString("\n")
        assertTrue(notes.contains("hello_world"))
        assertTrue(notes.contains("Waiting"))
        assertTrue(notes.contains("get-transaction-status"))
        assertTrue(notes.contains("get-transaction-data"))
        assertTrue(notes.contains("get-block-data"))
        assertTrue(notes.contains("JavaScript get and decode transaction"))
        assertTrue(notes.contains("postchain-client"))
        assertTrue(notes.contains("#transactions"))
        assertTrue(notes.contains("get-account-by-id"))
        assertTrue(notes.contains("How to get an account by ID"))
        assertTrue(notes.contains("@chromia/ft4"))
        assertTrue(notes.contains("get-account-by-signer"))
        assertTrue(notes.contains("How to get accounts by signer"))
        assertTrue(notes.contains("get-account-transfer-history"))
        assertTrue(notes.contains("How to get account transfer history"))
        assertTrue(notes.contains("pagination-with-ft4"))
        assertTrue(notes.contains("How to implement pagination with FT4"))
        assertTrue(notes.contains("get_users_paginated"))
        assertTrue(notes.contains("lib.ft4.utils"))
        assertTrue(notes.contains("check-account-memo-requirement"))
        assertTrue(notes.contains("How to check account memo requirement"))
        assertTrue(notes.contains("does_account_require_memo"))
        assertTrue(notes.contains("How to implement custom pagination"))
        assertTrue(notes.contains("paginator.rell"))
        assertTrue(notes.contains("data_size"))
        assertTrue(notes.contains("make-query"))
        assertTrue(notes.contains("How to make queries with parameters"))
        assertTrue(notes.contains("How to run queries"))
        assertTrue(notes.contains("run-queries"))
        assertTrue(notes.contains("localhost:7740"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-queries",
            payload["run_queries_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-queries/",
            payload["run_queries_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to run queries", payload["run_queries_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-queries",
            payload["run_queries_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-queries/",
            payload["run_queries_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to run queries", payload["run_queries_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("How to run tests"))
        assertTrue(notes.contains("run-tests"))
        assertTrue(notes.contains("src/test"))
        assertTrue(notes.contains("chr repl --sql-log --use-db --module"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-tests",
            payload["run_tests_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-tests/",
            payload["run_tests_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to run tests", payload["run_tests_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-tests",
            payload["run_tests_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-tests/",
            payload["run_tests_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to run tests", payload["run_tests_index_title"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("cli/create-rell-dapp") })
        assertTrue(notes.contains("How to create a new Rell dapp"))
        assertTrue(notes.contains("create-rell-dapp"))
        assertTrue(notes.contains("my-rell-dapp"))
        assertTrue(notes.contains("src/main.rell"))
        assertTrue(notes.contains("arithmetic_test.rell"))
        assertTrue(notes.contains("data_test.rell"))
        assertTrue(notes.contains("chr node start"))
        assertTrue(notes.contains("chr query hello_world"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/create-rell-dapp",
            payload["create_rell_dapp_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/create-rell-dapp/",
            payload["create_rell_dapp_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to create a new Rell dapp", payload["create_rell_dapp_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/create-rell-dapp",
            payload["create_rell_dapp_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/create-rell-dapp/",
            payload["create_rell_dapp_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to create a new Rell dapp", payload["create_rell_dapp_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("Welcome to the Chromia Cookbook"))
        assertTrue(notes.contains("overview"))
        assertTrue(notes.contains("Create queries"))
        assertTrue(notes.contains("Data inspection"))
        assertTrue(notes.contains("this signs"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/overview",
            payload["overview_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/overview/",
            payload["overview_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Overview", payload["overview_title"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("cookbook/cli") && !it.contains("cookbook/cli/") })
        assertTrue(notes.contains("hands-on"))
        assertTrue(notes.contains("How to run operations"))
        assertTrue(notes.contains("cli/run-operations"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli",
            payload["cli_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/",
            payload["cli_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("CLI", payload["cli_title"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("cookbook/query-creation") && !it.contains("cookbook/query-creation/") })
        assertTrue(notes.contains("inspecting account states"))
        assertTrue(notes.contains("querying blockchain data"))
        assertTrue(notes.contains("How to get account balance"))
        assertTrue(notes.contains("query-creation/get-account-balance"))
        assertTrue(notes.contains("Learn the fundamental pattern"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation",
            payload["query_creation_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/",
            payload["query_creation_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Create queries", payload["query_creation_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation",
            payload["query_creation_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/query-creation/",
            payload["query_creation_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Create queries", payload["query_creation_index_title"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("cookbook/data-inspection") && !it.contains("cookbook/data-inspection/") })
        assertTrue(notes.contains("on-chain"))
        assertTrue(notes.contains("Tx RID"))
        assertTrue(notes.contains("block info"))
        assertTrue(notes.contains("paginated transfer history"))
        assertTrue(notes.contains("EVM address"))
        assertTrue(notes.contains("unique ID"))
        assertTrue(notes.contains("How to get transaction status"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection",
            payload["data_inspection_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/",
            payload["data_inspection_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Data inspection", payload["data_inspection_title"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("cookbook/account-creation") && !it.contains("cookbook/account-creation/") })
        assertTrue(notes.contains("How to create account with open strategy"))
        assertTrue(notes.contains("How to create account with transfer fee strategy"))
        assertTrue(notes.contains("How to create account with transfer open strategy"))
        assertTrue(notes.contains("How to create account with transfer subscription strategy"))
        assertTrue(notes.contains("account-creation/open-strategy"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation",
            payload["account_creation_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/",
            payload["account_creation_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Account creation", payload["account_creation_title"]!!.jsonPrimitive.content)
        assertTrue(included.any { it.contains("cookbook/transaction-creation") && !it.contains("cookbook/transaction-creation/") })
        assertTrue(notes.contains("creating transactions"))
        assertTrue(notes.contains("How to send a simple transaction"))
        assertTrue(notes.contains("How to make a transfer"))
        assertTrue(notes.contains("How to enable/disable memo for transfers"))
        assertTrue(notes.contains("How to make a transfer with memo"))
        assertTrue(notes.contains("How to create time-bound transactions"))
        assertTrue(notes.contains("How to call operations with FT4 authentication"))
        assertTrue(notes.contains("How to register crosschain assets"))
        assertTrue(notes.contains("How to register assets"))
        assertTrue(notes.contains("How to make crosschain transfers"))
        assertTrue(notes.contains("transaction-creation/simple-transaction"))
        assertTrue(notes.contains("call-operation-with-ft4-auth"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation",
            payload["transaction_creation_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/",
            payload["transaction_creation_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("Create & manage transactions", payload["transaction_creation_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction",
            payload["simple_transaction_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction/",
            payload["simple_transaction_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to send a simple transaction", payload["simple_transaction_title"]!!.jsonPrimitive.content)
        assertEquals("JS/TS client", payload["simple_transaction_js_tab"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction",
            payload["simple_transaction_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction/",
            payload["simple_transaction_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to send a simple transaction", payload["simple_transaction_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/time-bound-transactions",
            payload["time_bound_transactions_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/time-bound-transactions/",
            payload["time_bound_transactions_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to create time-bound transactions", payload["time_bound_transactions_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/call-operation-with-ft4-auth",
            payload["call_operation_ft4_auth_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/call-operation-with-ft4-auth/",
            payload["call_operation_ft4_auth_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to call operations with FT4 authentication", payload["call_operation_ft4_auth_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/register-crosschain-asset",
            payload["register_crosschain_asset_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/register-crosschain-asset/",
            payload["register_crosschain_asset_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to register crosschain assets", payload["register_crosschain_asset_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/register-asset",
            payload["register_asset_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/register-asset/",
            payload["register_asset_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to register assets", payload["register_asset_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/crosschain-transfer",
            payload["crosschain_transfer_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/transaction-creation/crosschain-transfer/",
            payload["crosschain_transfer_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to make crosschain transfers", payload["crosschain_transfer_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-data",
            payload["tx_data_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-data/",
            payload["tx_data_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to get and decode transaction data", payload["tx_data_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-block-data",
            payload["block_data_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-block-data/",
            payload["block_data_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to fetch and decode block data", payload["block_data_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-id",
            payload["account_by_id_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-id/",
            payload["account_by_id_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to get an account by ID", payload["account_by_id_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-signer",
            payload["account_by_signer_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-signer/",
            payload["account_by_signer_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to get accounts by signer", payload["account_by_signer_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-transfer-history",
            payload["account_transfer_history_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/data-inspection/get-account-transfer-history/",
            payload["account_transfer_history_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to get account transfer history", payload["account_transfer_history_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/open-strategy",
            payload["open_strategy_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/open-strategy/",
            payload["open_strategy_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to create account with open strategy", payload["open_strategy_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/transfer-fee-strategy",
            payload["transfer_fee_strategy_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/transfer-fee-strategy/",
            payload["transfer_fee_strategy_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to create account with transfer fee strategy", payload["transfer_fee_strategy_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/transfer-open-strategy",
            payload["transfer_open_strategy_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/transfer-open-strategy/",
            payload["transfer_open_strategy_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to create account with transfer open strategy", payload["transfer_open_strategy_index_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/transfer-subscription-strategy",
            payload["transfer_subscription_strategy_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/account-creation/transfer-subscription-strategy/",
            payload["transfer_subscription_strategy_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to create account with transfer subscription strategy", payload["transfer_subscription_strategy_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("signAndSendUniqueTransaction"))
        assertTrue(notes.contains("chr tx"))
        assertTrue(notes.contains("--await"))
        assertTrue(notes.contains("--nop"))
        assertTrue(notes.contains("--secret"))
        assertTrue(notes.contains("create_book"))
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-operations",
            payload["run_operations_url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-operations/",
            payload["run_operations_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to run operations", payload["run_operations_title"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-operations",
            payload["run_operations_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cookbook/cli/run-operations/",
            payload["run_operations_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("How to run operations", payload["run_operations_index_title"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("FC17B67D66F6F35A5D8B75ED3F83AE222FB8C8FCA241624F06285150F10C6BAC"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaCookbookHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrKeyIdHelpIsExistingKeyOnly() = runBlocking {
        val result = ChrKeyIdHelpStrategy().execute(
            CallToolRequest(name = "chr_key_id_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("true", payload["existing_key_only"]!!.jsonPrimitive.content)
        val flags = payload["flags"]!!.jsonObject
        assertTrue(flags["key_id"]!!.jsonPrimitive.content.contains("--key-id"))
        assertTrue(flags["key_id_property"]!!.jsonPrimitive.content.contains("key.id"))
        val precedence = payload["precedence"]!!.jsonObject
        assertEquals("--secret", precedence["1_secret"]!!.jsonPrimitive.content)
        assertEquals("--key-id", precedence["2_key_id"]!!.jsonPrimitive.content)
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, flags.toString(), precedence.toString()).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/keygen",
            payload["keygen_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/keygen",
            payload["keygen_index_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/cli/commands/keygen/",
            payload["keygen_index_url_slash"]!!.jsonPrimitive.content
        )
        assertEquals("keygen", payload["keygen_index_title"]!!.jsonPrimitive.content)
        assertEquals("true", payload["keygen_help_only"]!!.jsonPrimitive.content)
        val leftoverKeygen = payload["official_keygen_flags"]!!.jsonObject
        assertTrue(leftoverKeygen["key_id"]!!.jsonPrimitive.content.contains("--key-id"))
        assertTrue(leftoverKeygen["get_pubkey"]!!.jsonPrimitive.content.contains("--get-pubkey"))
        assertTrue(leftoverKeygen["dry"]!!.jsonPrimitive.content.contains("--dry"))
        assertTrue(notes.contains("HELP ONLY"))
        assertTrue(notes.contains("commands/keygen"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        assertFalse(allText.contains("lift employ"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChrKeyIdHelp.toJson(), result.structuredContent)
    }

    @Test
    fun languageClientsHelpIsQueryOnly() = runBlocking {
        val result = ChromiaLanguageClientsHelpStrategy().execute(
            CallToolRequest(name = "chromia_language_clients_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        val packages = payload["packages"]!!.jsonObject
        assertEquals("gitlab.com/chromaway/ft4-go-client", packages["go_module"]!!.jsonPrimitive.content)
        assertEquals("postchain-client", packages["rust_crate"]!!.jsonPrimitive.content)
        assertEquals("0.0.3", packages["rust_crate_version"]!!.jsonPrimitive.content)
        assertEquals("@chromia/react", packages["react_npm"]!!.jsonPrimitive.content)
        assertTrue(packages["csharp_nuget"]!!.jsonPrimitive.content.contains("does not print"))
        assertEquals("filehub", packages["filehub_npm"]!!.jsonPrimitive.content)
        assertEquals("@chromia/bridge-client", packages["bridge_npm"]!!.jsonPrimitive.content)
        assertTrue(packages["filehub_npm_source"]!!.jsonPrimitive.content.contains("BUILD client page prints none"))
        assertTrue(packages["bridge_npm_source"]!!.jsonPrimitive.content.contains("BUILD bridge client page prints none"))
        assertEquals("@chromia/chromia-lsp-mcp", packages["lsp_mcp_npm"]!!.jsonPrimitive.content)
        assertEquals("0.8.8", packages["lsp_mcp_rell"]!!.jsonPrimitive.content)
        val clientPages = payload["pages"]!!.jsonObject
        assertTrue(clientPages["filehub_client"]!!.jsonPrimitive.content.contains("/build/clients/filehub-client/"))
        assertTrue(clientPages["bridge_client"]!!.jsonPrimitive.content.contains("/build/clients/bridge-client/"))
        assertTrue(clientPages["iccf_protocol"]!!.jsonPrimitive.content.contains("/protocols/iccf/"))
        assertTrue(clientPages["filehub_configure"]!!.jsonPrimitive.content.contains("filehub-configure"))
        assertTrue(clientPages["mcp_server"]!!.jsonPrimitive.content.contains("/build/clients/mcp-server/"))
        assertEquals("https://docs.chromia.com/build/deployment/deploy-frontend-dapp", clientPages["deploy_frontend"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart", clientPages["js_quickstart"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart", payload["js_quickstart_index_docs"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart/", payload["js_quickstart_index_url_slash"]!!.jsonPrimitive.content)
        assertEquals("Hello World Quickstart", payload["js_quickstart_index_title"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/get-started/create-dapp/run-dapp-cli", clientPages["run_dapp_cli"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference", clientPages["js_reference"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/kotlin-client", clientPages["kotlin_client"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/clients/postchain-clients/python-client", clientPages["python_client"]!!.jsonPrimitive.content)
        assertEquals(0, payload["js_reference_directory_iid"]!!.jsonPrimitive.content.toInt())
        assertEquals("blockchainRid", payload["js_reference_brid_setting"]!!.jsonPrimitive.content)
        assertEquals("blockchainRID", payload["js_reference_official_prose_brid"]!!.jsonPrimitive.content)
        assertEquals("startegy", payload["js_reference_official_strategy_typo"]!!.jsonPrimitive.content)
        assertEquals("abortOnErrror", payload["js_reference_official_abort_typo"]!!.jsonPrimitive.content)
        assertEquals("abortOnError", payload["js_reference_source_failover_abort"]!!.jsonPrimitive.content)
        val sticky = payload["js_reference_sticky_create_client"]!!.jsonPrimitive.content
        assertTrue(sticky.contains("useStickyNode"))
        assertTrue(sticky.contains("directoryNodeUrlPool"))
        assertFalse(sticky.contains("secp256k1"))
        assertFalse(sticky.contains("signAndSend"))
        assertTrue(payload["hello_world_rell"]!!.jsonPrimitive.content.contains("object my_name"))
        assertEquals("Hello World!", payload["hello_world_result"]!!.jsonPrimitive.content)
        assertTrue(payload["web_static_local_url"]!!.jsonPrimitive.content.contains("/web_query/<blockchainRid>/web_static"))
        assertTrue(clientPages["filehub_work"]!!.jsonPrimitive.content.contains("filehub-work"))
        assertTrue(clientPages["filehub_work_build_404"]!!.jsonPrimitive.content.contains("/build/clients/filehub-client/work"))
        assertTrue(clientPages["filehub_build_index_404"]!!.jsonPrimitive.content.contains("/build/filehub/"))
        assertTrue(clientPages["bridge_configure"]!!.jsonPrimitive.content.contains("/bridge-client/client"))
        assertTrue(clientPages["bridge_work"]!!.jsonPrimitive.content.contains("work-with-client"))
        val csharp = payload["csharp_query"]!!.jsonPrimitive.content
        assertTrue(csharp.contains("ChromiaClient.Create"))
        assertTrue(csharp.contains("Query<string>"))
        assertTrue(csharp.contains("get_city"))
        assertFalse(csharp.contains("SignatureProvider"))
        assertTrue(payload["csharp_query_params"]!!.jsonPrimitive.content.contains("QueryParams"))
        assertTrue(payload["csharp_query_params"]!!.jsonPrimitive.content.contains("get_city"))
        assertTrue(payload["csharp_query_params"]!!.jsonPrimitive.content.contains("IGtvSerializable"))
        assertTrue(payload["csharp_query_params"]!!.jsonPrimitive.content.contains("JsonProperty"))
        assertTrue(payload["csharp_query_params"]!!.jsonPrimitive.content.contains("\"zip\""))
        assertFalse(payload["csharp_query_params"]!!.jsonPrimitive.content.contains("SendTransaction"))
        assertFalse(csharp.contains("SendTransaction"))
        assertEquals("http://localhost:7750", payload["csharp_directory_host"]!!.jsonPrimitive.content)
        val csharpDir = payload["csharp_create_directory"]!!.jsonPrimitive.content
        assertTrue(csharpDir.contains("CreateFromDirectory"))
        assertTrue(csharpDir.contains("http://localhost:7750"))
        assertFalse(csharpDir.contains("7d565d92fd15bd1cdac2dc276cbcbc5581349d05a9e94ba919e1155ef4daf8f9"))
        val csharpLeftoverDir = payload["csharp_official_directory_create"]!!.jsonPrimitive.content
        assertTrue(csharpLeftoverDir.contains("ChromiaClient.Create"))
        assertTrue(csharpLeftoverDir.contains("http://localhost:7750"))
        assertTrue(csharpLeftoverDir.contains("http://localhost:7751"))
        assertFalse(csharpLeftoverDir.contains("CreateFromDirectory"))
        val go = payload["go_query"]!!.jsonPrimitive.content
        assertTrue(go.contains("postchain.NewClient"))
        assertTrue(go.contains("client.Query"))
        assertFalse(go.contains("PostTransaction"))
        assertEquals("https://node1.example.com", payload["go_official_node"]!!.jsonPrimitive.content)
        val rust = payload["rust_query"]!!.jsonPrimitive.content
        assertTrue(rust.contains("client.query"))
        assertFalse(rust.contains("send_transaction"))
        assertEquals("BigDecima", payload["rust_official_decimal_typo"]!!.jsonPrimitive.content)
        assertEquals("BigDecimal", payload["rust_source_decimal"]!!.jsonPrimitive.content)
        assertEquals("Error(error: RestError)", payload["rust_official_error_arm"]!!.jsonPrimitive.content)
        assertEquals("Err(error: RestError)", payload["rust_source_err_arm"]!!.jsonPrimitive.content)
        assertEquals("Err(error: )", payload["rust_official_err_incomplete"]!!.jsonPrimitive.content)
        assertEquals("err", payload["rust_official_err_ident"]!!.jsonPrimitive.content)
        assertEquals("error", payload["rust_source_err_ident"]!!.jsonPrimitive.content)
        assertEquals("query_arguments_ref", payload["rust_official_query_args_ref"]!!.jsonPrimitive.content)
        assertEquals("query_arguments", payload["rust_source_query_args"]!!.jsonPrimitive.content)
        assertEquals("Params:: ByteArray", payload["rust_official_bytearray"]!!.jsonPrimitive.content)
        assertEquals("Params::ByteArray", payload["rust_source_bytearray"]!!.jsonPrimitive.content)
        assertEquals("serialize_bigint", payload["rust_official_decimal_serde"]!!.jsonPrimitive.content)
        assertEquals("serialize_bigdecimal", payload["rust_source_decimal_serde"]!!.jsonPrimitive.content)
        assertEquals("RestClient<'_>", payload["rust_official_restclient_lifetime"]!!.jsonPrimitive.content)
        assertEquals("RestClient&lt;'_>", payload["rust_official_restclient_html_entity"]!!.jsonPrimitive.content)
        assertTrue(payload["rust_client"]!!.jsonPrimitive.content.contains("RestClient {"))
        assertTrue(payload["rust_client"]!!.jsonPrimitive.content.contains("request_time_out: 30"))
        assertEquals("https://docs.chromia.com/build/clients/postchain-rest-api/", payload["rest_url_slash"]!!.jsonPrimitive.content)
        assertEquals("Postchain Rest API", payload["rest_title"]!!.jsonPrimitive.content)
        val rustStatus = payload["rust_status"]!!.jsonPrimitive.content
        assertTrue(rustStatus.contains("get_transaction_status"))
        assertFalse(rustStatus.contains("send_transaction"))
        assertFalse(rustStatus.contains("C70D5A77CC10552019179B7390545C46647C9FCA1B6485850F2B913F87270300"))
        val rustErr = payload["rust_query_error"]!!.jsonPrimitive.content
        assertTrue(rustErr.contains("client.query"))
        assertTrue(rustErr.contains("Error(error: RestError)"))
        assertFalse(rustErr.contains("send_transaction"))
        assertFalse(rustErr.contains("C70D5A77CC10552019179B7390545C46647C9FCA1B6485850F2B913F87270300"))
        val react = payload["react_hooks"]!!.jsonPrimitive.content
        assertTrue(react.contains("@chromia/react"))
        assertTrue(react.contains("createChromiaHooks"))
        assertTrue(react.contains("useChromiaQuery"))
        assertFalse(react.contains("FtProvider"))
        assertEquals("BLOCKCHAIN_URL", payload["react_official_pool"]!!.jsonPrimitive.content)
        val rest = payload["rest_read_paths"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("GET /query/{blockchainRid}" in rest)
        assertTrue("GET /brid/iid_{chainIid}" in rest)
        assertTrue("GET /tx/{blockchainRid}/{txRid}/status" in rest)
        assertTrue("GET /blockchain/{blockchainRid}/nodestate" in rest)
        assertFalse(rest.any { it.contains("POST /tx") })
        assertEquals(
            "curl -X GET 'localhost:7740/query/<BlockchainRID>?type=hello_world'",
            payload["rest_query_example"]!!.jsonPrimitive.content
        )
        val skipped = payload["skipped_sign_or_key"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("POST /tx") })
        assertTrue(skipped.any { it.contains("SignatureProvider") })
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(
            notes,
            csharp,
            go,
            rust,
            react,
            payload["rust_client"]!!.jsonPrimitive.content,
            payload["rust_query_error"]!!.jsonPrimitive.content,
            payload["csharp_query_params"]!!.jsonPrimitive.content,
            payload["filehub_get_file"]!!.jsonPrimitive.content,
            payload["mcp_prod_config"]!!.jsonPrimitive.content,
            rest.toString(),
            skipped.toString()
        ).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("filehub"))
        assertTrue(notes.contains("/build/clients/mcp-server/"))
        assertTrue(notes.contains("deploy-frontend-dapp"))
        assertTrue(notes.contains("hello-world-quickstart"))
        assertTrue(notes.contains("chromia. yml"))
        assertTrue(notes.contains("/javascript-typescript/reference"))
        assertTrue(notes.contains("succefull"))
        assertTrue(notes.contains("startegy"))
        assertTrue(notes.contains("abortOnErrror"))
        assertTrue(notes.contains("abortOnError"))
        assertTrue(notes.contains("Iid 0"))
        assertTrue(notes.contains("kotlin-client"))
        assertTrue(notes.contains("python-client"))
        assertTrue(notes.contains("chromia-client"))
        assertTrue(notes.contains("get_collections"))
        assertTrue(notes.contains("get_all_reviews_for_book"))
        assertTrue(notes.contains("POSTCHAIN_TEST_NODE"))
        assertTrue(notes.contains("BLOCKCHAIN_TEST_RID"))
        assertTrue(notes.contains("StandardChromiaClient"))
        assertTrue(notes.contains("We are currently updating this documentation"))
        assertEquals("We are currently updating this documentation", payload["official_outdated_banner"]!!.jsonPrimitive.content)
        assertEquals("POSTCHAIN_TEST_NODE", payload["python_env_node"]!!.jsonPrimitive.content)
        assertEquals("BLOCKCHAIN_TEST_RID", payload["python_env_rid"]!!.jsonPrimitive.content)
        val pythonEnv = payload["python_env"]!!.jsonPrimitive.content
        assertTrue(pythonEnv.contains("POSTCHAIN_TEST_NODE=http://localhost:7740"))
        assertFalse(pythonEnv.contains("PRIV_KEY"))
        assertTrue(payload["python_query_reviews"]!!.jsonPrimitive.content.contains("get_all_reviews_for_book"))
        val standardClient = payload["kotlin_standard_chromia_client"]!!.jsonPrimitive.content
        assertTrue(standardClient.contains("StandardChromiaClient"))
        assertFalse(standardClient.contains("awaitAnchoredTx"))
        assertTrue(notes.contains("webStatic"))
        assertTrue(notes.contains("getFile"))
        assertTrue(notes.contains("chromia-mcp-server"))
        assertTrue(notes.contains("https://mcp.chromia.dev"))
        assertTrue(notes.contains("127.0.0.1:3001"))
        assertTrue(notes.contains("YOU_NODE_URL_POOL"))
        assertTrue(notes.contains("eif.hbridge.bridge_mode"))
        assertTrue(notes.contains("prints Create"))
        assertTrue(notes.contains("CreateFromDirectory"))
        assertTrue(notes.contains("BigDecima"))
        assertTrue(notes.contains("BigDecimal"))
        assertTrue(notes.contains("Error(error: RestError)"))
        assertTrue(notes.contains("Err(error: RestError)"))
        assertTrue(notes.contains("Err(error: )"))
        assertTrue(notes.contains("query_arguments_ref"))
        assertTrue(notes.contains("Params:: ByteArray"))
        assertTrue(notes.contains("serialize_bigdecimal"))
        assertTrue(notes.contains("Postchain Rest API"))
        assertTrue(notes.contains("RestClient<'_>"))
        assertTrue(notes.contains("RestClient&lt;'_>"))
        assertTrue(notes.contains("get_transaction_status"))
        assertTrue(notes.contains("BLOCKCHAIN_URL"))
        assertTrue(notes.contains("QueryOrOperationType"))
        assertTrue(notes.contains("node1.example.com"))
        assertTrue(notes.contains("GetBlockchainRID"))
        assertTrue(payload["filehub_get_file"]!!.jsonPrimitive.content.contains("getFile(fileHash)"))
        assertFalse(payload["filehub_get_file"]!!.jsonPrimitive.content.contains("storeFile"))
        assertEquals("filehub-gw.chromia.com", payload["filehub_gateway_host"]!!.jsonPrimitive.content)
        assertTrue(payload["bridge_check_allowance"]!!.jsonPrimitive.content.contains("checkAllowance"))
        assertEquals("chromia-mcp", payload["mcp_official_page_key"]!!.jsonPrimitive.content)
        assertEquals("chromia-mcp-server", payload["mcp_fat_jar_server_name"]!!.jsonPrimitive.content)
        assertEquals("https://mcp.chromia.dev", payload["mcp_prod_url"]!!.jsonPrimitive.content)
        assertEquals("http://127.0.0.1:3001", payload["mcp_local_sse"]!!.jsonPrimitive.content)
        assertEquals("https://mcp.chromia.dev/sse", payload["mcp_official_prod_sse"]!!.jsonPrimitive.content)
        assertEquals("http://127.0.0.1:3001/sse", payload["mcp_official_local_sse"]!!.jsonPrimitive.content)
        assertEquals("/", payload["mcp_live_sse_path"]!!.jsonPrimitive.content)
        assertEquals("/sse", payload["mcp_official_sse_path"]!!.jsonPrimitive.content)
        assertEquals("/health", payload["mcp_health_path"]!!.jsonPrimitive.content)
        assertEquals("healthy", payload["mcp_health_status"]!!.jsonPrimitive.content)
        assertEquals("<version>", payload["mcp_health_version"]!!.jsonPrimitive.content)
        assertEquals("502", payload["mcp_official_host_status"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("https://mcp.chromia.dev/sse"))
        assertTrue(notes.contains("http://127.0.0.1:3001/sse"))
        assertTrue(notes.contains("GET /"))
        assertTrue(notes.contains("GET /sse"))
        assertTrue(notes.contains("404"))
        assertTrue(notes.contains("/health"))
        assertTrue(notes.contains("{status:healthy, server:chromia-mcp-server, version:<version>}"))
        assertTrue(notes.contains("(version tracks the running build)"))
        assertTrue(notes.contains("currently 502"))
        assertTrue(payload["mcp_prod_config"]!!.jsonPrimitive.content.contains("chromia-mcp"))
        assertTrue(skipped.any { it.contains("explorer-dump") })
        assertTrue(skipped.any { it.contains("FilehubAdministrator") })
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaLanguageClientsHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaRellLanguageHelpQuotesOfficialSyntax() = runBlocking {
        val result = ChromiaRellLanguageHelpStrategy().execute(
            CallToolRequest(name = "chromia_rell_language_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("0.16.7", payload["rell"]!!.jsonPrimitive.content)
        assertEquals("chromia_rell_language_help", payload["tool"]!!.jsonPrimitive.content)
        assertTrue(payload["query_short"]!!.jsonPrimitive.content.contains("query q(x: integer)"))
        assertTrue(payload["query_full"]!!.jsonPrimitive.content.contains("return x * x"))
        assertTrue(payload["operation_example"]!!.jsonPrimitive.content.contains("operation create_user"))
        assertTrue(payload["entity_example"]!!.jsonPrimitive.content.contains("entity user"))
        assertTrue(payload["object_example"]!!.jsonPrimitive.content.contains("object event_stats"))
        assertTrue(payload["struct_example"]!!.jsonPrimitive.content.contains("struct user"))
        assertTrue(payload["enum_example"]!!.jsonPrimitive.content.contains("enum currency"))
        assertTrue(payload["function_short"]!!.jsonPrimitive.content.contains("function f(x: integer)"))
        val hello = payload["hello_world_query"]!!.jsonPrimitive.content
        assertTrue(hello.contains("object my_name"))
        assertTrue(hello.contains("query hello_world()"))
        assertTrue(hello.contains("Hello %s!"))
        assertTrue(hello.contains(".format(my_name.name)"))
        assertEquals("Hello World!", payload["hello_world_result"]!!.jsonPrimitive.content)
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(payload["namespace_example"]!!.jsonPrimitive.content.contains("namespace foo"))
        assertTrue(payload["mount_example"]!!.jsonPrimitive.content.contains("@mount('foo.bar.user')"))
        assertTrue(payload["abstract_example"]!!.jsonPrimitive.content.contains("abstract module"))
        val sizeEx = payload["size_constraint_example"]!!.jsonPrimitive.content
        assertTrue(sizeEx.contains("@size(32)"))
        assertTrue(sizeEx.contains("@max_size(50) userName"))
        assertEquals(
            "https://docs.chromia.com/rell/language-features/modules/size-constraint-annotations",
            payload["size_constraint_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/rell/language-features/modules/namespace",
            payload["namespace_docs"]!!.jsonPrimitive.content
        )
        val skipped404 = payload["skipped_404"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped404.any { it.contains("/modules/size-constraint") && it.contains("404") })
        assertTrue(skipped404.any { it.contains("/modules/external") && it.contains("404") })
        assertTrue(notes.contains("entity and object attributes"))
        assertTrue(notes.contains("size-constraint-annotations"))
        assertFalse(notes.contains("parameters only"))
        assertEquals(
            "https://docs.chromia.com/rell/special-operations",
            payload["special_ops_docs"]!!.jsonPrimitive.content
        )
        val special = payload["special_ops"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(special.any { it.contains("__begin_block(height: integer)") })
        assertTrue(special.any { it.contains("__icmf_message") && it.contains("not on the official page") })
        assertTrue(payload["special_ops_example"]!!.jsonPrimitive.content.contains("__end_block(height: integer)"))
        assertEquals(
            "https://docs.chromia.com/rell/rell-doc",
            payload["relldoc_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/rell/language-features/identifiers-syntax",
            payload["identifiers_docs"]!!.jsonPrimitive.content
        )
        assertTrue(payload["rell_doc_example"]!!.jsonPrimitive.content.contains("@param username"))
        val tags = payload["rell_doc_tags"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(tags.any { it.contains("@return") })
        assertTrue(tags.any { it.contains("@author") })
        assertTrue(notes.contains("identifiers-syntax"))
        assertTrue(notes.contains("rell-doc"))
        assertEquals(
            "https://docs.chromia.com/rell/releases",
            payload["releases_docs"]!!.jsonPrimitive.content
        )
        val releases = payload["releases"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(releases.any { it.contains("0.16.4") && it.contains("0.16.7") })
        assertTrue(releases.any { it.contains("0.16.7") && it.contains("source notes") })
        assertTrue(releases.any { it.contains("0.16.3") && it.contains("rule_replace_if_with_when") })
        assertTrue(releases.any { it.contains("0.16.0") && it.contains("RR_") })
        assertTrue(releases.any { it.contains("0.14.5") && it.contains("V1") && it.contains("production pin 2") })
        assertTrue(releases.any { it.contains("0.14.0") && it.contains("@return") })
        assertTrue(releases.any { it.contains("crypto.get_signature skipped") })
        assertTrue(notes.contains("releases"))
        assertTrue(notes.contains("official default V1"))
        val allText = listOf(
            notes,
            hello,
            payload["query_short"]!!.jsonPrimitive.content,
            payload["namespace_example"]!!.jsonPrimitive.content,
            payload["mount_example"]!!.jsonPrimitive.content,
            payload["abstract_example"]!!.jsonPrimitive.content,
            sizeEx,
            skipped404.toString()
        ).joinToString("\n")
        assertTrue(notes.contains("0.16.7"))
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaRellLanguageHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaRellPracticesHelpIsReadOnlyOfficialPages() = runBlocking {
        val result = ChromiaRellPracticesHelpStrategy().execute(
            CallToolRequest(name = "chromia_rell_practices_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("0.16.7", payload["rell"]!!.jsonPrimitive.content)
        assertEquals("chromia_rell_practices_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("true", payload["read_only"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/rell/security",
            payload["security_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/rell/rell-best-practices",
            payload["best_practices_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/rell/analyze-rell-dapp-code",
            payload["analyze_docs"]!!.jsonPrimitive.content
        )
        assertTrue(payload["config_delay_yaml"]!!.jsonPrimitive.content.contains("config_delay: 86400000"))
        assertTrue(payload["config_delay_yaml"]!!.jsonPrimitive.content.contains("directory_chain"))
        assertFalse(payload["config_delay_yaml"]!!.jsonPrimitive.content.contains("house-key-example"))
        assertTrue(payload["require_example"]!!.jsonPrimitive.content.contains("require (from != to"))
        assertTrue(payload["composite_key_example"]!!.jsonPrimitive.content.contains("key accounts.account, asset"))
        assertTrue(payload["input_validation_example"]!!.jsonPrimitive.content.contains("symbol.matches"))
        assertTrue(payload["missing_balance_example"]!!.jsonPrimitive.content.contains("@?"))
        assertTrue(payload["run_must_fail_example"]!!.jsonPrimitive.content.contains("run_must_fail"))
        assertFalse(payload["run_must_fail_example"]!!.jsonPrimitive.content.contains(".sign("))
        val keys = payload["security_keys"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(keys.any { it.contains("config_delay") })
        assertTrue(keys.any { it.contains("rate_limit") })
        val skipped = payload["skipped"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("vote") })
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(
            notes,
            payload["config_delay_yaml"]!!.jsonPrimitive.content,
            payload["require_example"]!!.jsonPrimitive.content,
            payload["run_must_fail_example"]!!.jsonPrimitive.content,
            keys.toString(),
            skipped.toString()
        ).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("No exploit") || notes.contains("no exploit"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        assertFalse(allText.contains(".sign("))
        val invented = Regex("(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaRellPracticesHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaFt4QueriesHelpIsReadOnlyCatalog() = runBlocking {
        val result = ChromiaFt4QueriesHelpStrategy().execute(
            CallToolRequest(name = "chromia_ft4_queries_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("v1.1.0r", payload["ft4"]!!.jsonPrimitive.content)
        assertEquals("1", payload["ft4Api"]!!.jsonPrimitive.content)
        assertEquals("true", payload["read_only"]!!.jsonPrimitive.content)
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr query ft4.get_all_assets page_size=10 page_cursor=null", commands["get_all_assets"]!!.jsonPrimitive.content)
        assertEquals("chr query ft4.get_version", commands["get_version"]!!.jsonPrimitive.content)
        val assets = payload["asset_queries"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(assets.any { it.contains("get_all_assets") })
        assertTrue(assets.any { it.contains("get_assets_by_name") })
        assertTrue(assets.any { it.contains("get_asset_by_id") })
        assertTrue(assets.any { it.contains("get_asset_balances") })
        val accounts = payload["account_queries"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(accounts.any { it.contains("get_account_by_id") })
        assertTrue(accounts.any { it.contains("get_accounts_by_signer") })
        assertTrue(accounts.any { it.contains("get_config") })
        val memos = payload["memo_queries"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(memos.any { it.contains("does_account_require_memo") })
        assertEquals(
            "https://docs.chromia.com/build/ft4/prioritization",
            payload["prioritization_docs"]!!.jsonPrimitive.content
        )
        val priority = payload["priority_queries"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(priority.any { it.contains("priority_check_v1") && it.contains("gtx_transaction_body") })
        assertTrue(priority.any { it.contains("gtx_api") })
        val pImports = payload["priority_imports"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(pImports.any { it.contains("lib.ft4.core.prioritization.default") })
        assertTrue(pImports.any { it.contains("lib.ft4.core.prioritization") })
        val pStates = payload["priority_states"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(pStates.any { it.contains("priority_state_v1") && it.contains("account_points") })
        assertTrue(pStates.any { it.contains("no_op_priority_state") })
        assertTrue(payload["priority_extend_example"]!!.jsonPrimitive.content.contains("@extend(priority_check)"))
        val prioritization = payload["prioritization"]!!.jsonPrimitive.content
        assertTrue(prioritization.contains("gtx_api"))
        assertFalse(prioritization.contains("ft4.priority_check_v1"))
        val skipped = payload["skipped_write"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("lib.ft4.admin") })
        assertTrue(skipped.any { it.contains("ras_open") })
        val notes = payload["notes"]!!.jsonPrimitive.content
        val pagination = payload["pagination"]!!.jsonPrimitive.content
        val allText = listOf(notes, pagination, prioritization, assets.toString(), accounts.toString(), skipped.toString()).joinToString("\n")
        assertTrue(pagination.contains("page_cursor"))
        assertTrue(pagination.contains("query_max_page_size"))
        assertTrue(notes.contains("prioritization"))
        assertTrue(notes.contains("gtx_api"))
        assertEquals(
            "https://docs.chromia.com/build/ft4/terms",
            payload["terms_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/build/ft4/intro",
            payload["intro_docs"]!!.jsonPrimitive.content
        )
        val terms = payload["terms"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(terms.any { it.contains("Auth descriptor") && it.contains("flags") })
        assertTrue(terms.any { it.contains("User account") })
        assertTrue(terms.any { it.contains("Lock account") })
        assertTrue(terms.any { it.contains("System account") })
        val intro = payload["intro"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(intro.any { it.contains("hash(public_key)") })
        assertTrue(intro.any { it.contains("hash(evm_address)") })
        assertTrue(intro.any { it.contains("Do not invent a 64-hex") })
        assertTrue(intro.any { it.contains("When to use") })
        assertEquals("https://docs.chromia.com/build/ft4/releases/ft4", payload["releases_docs"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/ft4/releases", payload["releases_404"]!!.jsonPrimitive.content)
        assertEquals("1.1.0r", payload["docs_latest_ft4"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/ft4/setup/imports", payload["imports_docs"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/ft4/configuration-values", payload["config_values_docs"]!!.jsonPrimitive.content)
        assertTrue(notes.contains("get_api_version starts at 1"))
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        assertFalse(allText.contains("admin_pubkey"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaFt4QueriesHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaIntegrationsHelpIsReadOnlyHub() = runBlocking {
        val result = ChromiaIntegrationsHelpStrategy().execute(
            CallToolRequest(name = "chromia_integrations_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chromia_integrations_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("true", payload["read_only"]!!.jsonPrimitive.content)
        val pages = payload["pages"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(pages.any { it.contains("/build/integrations/") })
        assertTrue(pages.any { it.contains("memo-guide") })
        assertTrue(pages.any { it.contains("/build/token-chain/") })
        assertTrue(pages.any { it.contains("developer-token-proposal") })
        assertEquals("https://docs.chromia.com/build/token-chain/", payload["token_chain_docs"]!!.jsonPrimitive.content)
        assertTrue(payload["queries"]!!.jsonObject["get_token_chain_constants"]!!.jsonPrimitive.content.contains("get_token_chain_constants"))
        val queries = payload["queries"]!!.jsonObject
        assertTrue(queries["does_account_require_memo"]!!.jsonPrimitive.content.contains("does_account_require_memo"))
        assertTrue(queries["get_proposals_by_proposer"]!!.jsonPrimitive.content.contains("proposer="))
        assertTrue(queries["ft4.get_assets_by_name"]!!.jsonPrimitive.content.contains("page_size=null"))
        assertTrue(queries["get_evm_transaction_submitter_chain_rid"]!!.jsonPrimitive.content.contains("DIRECTORY_CHAIN_RID"))
        assertTrue(queries["get_all_bridges"]!!.jsonPrimitive.content.contains("EVM_TRANSACTION_SUBMITTER_CHAIN_RID"))
        assertTrue(payload["bridge_configuration"]!!.jsonPrimitive.content.contains("eif.hbridge.bridge_mode"))
        assertTrue(payload["bridge_configuration"]!!.jsonPrimitive.content.contains("network_id"))
        val fields = payload["proposal_fields"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(fields.any { it.contains("Token name") })
        val fees = payload["fees"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(fees.any { it.contains("100 CHR token") })
        assertTrue(fees.any { it.contains("100 CHR bridge") })
        assertEquals("https://docs.chromia.com/build/token-chain/developer-token-proposal", payload["token_chain_proposal_redirect"]!!.jsonPrimitive.content)
        assertEquals("chromia_vector_search_help", payload["vector_search_help"]!!.jsonPrimitive.content)
        assertEquals(
            "official page does not print a package id",
            payload["packages"]!!.jsonObject["csharp_nuget"]!!.jsonPrimitive.content
        )
        val skipped = payload["skipped_write_or_invented"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("NuGet") })
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(notes, pages.toString(), skipped.toString(), queries.toString(), payload["bridge_configuration"]!!.jsonPrimitive.content).joinToString("\n")
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(skipped.any { it.contains("propose_token") })
        assertTrue(notes.contains("100 CHR"))
        assertTrue(notes.contains("hard skip"))
        assertTrue(notes.contains("proposer"))
        assertTrue(notes.contains("eif.hbridge.bridge_mode"))
        assertTrue(notes.contains("03028A31"))
        assertTrue(skipped.any { it.contains("03028A31") })
        assertFalse(allText.contains("propose_token \${"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaIntegrationsHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaVectorSearchHelpIsReadOnlyLeftoverBuild() = runBlocking {
        val result = ChromiaVectorSearchHelpStrategy().execute(
            CallToolRequest(name = "chromia_vector_search_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("chromia_vector_search_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("true", payload["read_only"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/vector-search/overview/", payload["docs"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/vector-search/", payload["index_404"]!!.jsonPrimitive.content)
        assertEquals("https://docs.chromia.com/build/extensions/", payload["extensions_404"]!!.jsonPrimitive.content)
        assertEquals("pgvector cosine_distance", payload["official_operator"]!!.jsonPrimitive.content)
        assertEquals("text-embedding-3-small", payload["official_offchain_model_example"]!!.jsonPrimitive.content)
        val pages = payload["pages"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(pages.any { it.contains("/build/vector-search/overview/") })
        assertTrue(pages.any { it.contains("sample-workloads") })
        val caps = payload["capabilities"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(caps.any { it.contains("pgvector") })
        val workloads = payload["workloads"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(workloads.any { it.contains("cosine_distance") })
        val skipped = payload["skipped_404_or_write"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("404") })
        assertTrue(skipped.any { it.contains("ONNX") })
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertTrue(notes.contains("no module names"))
        assertTrue(notes.contains("filehub"))
        assertTrue(notes.contains("BUILD page prints no package id"))
        assertTrue(skipped.any { it.contains("filehub") })
        val allText = listOf(notes, pages.toString(), caps.toString(), workloads.toString(), skipped.toString()).joinToString("\n")
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaVectorSearchHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chrQueryHelpOfficialLocalIsHelloWorld() = runBlocking {
        val result = ChrQueryHelpStrategy().execute(
            CallToolRequest(name = "chr_query_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        val commands = payload["commands"]!!.jsonObject
        assertEquals("chr query hello_world", commands["official_local"]!!.jsonPrimitive.content)
        assertEquals("Hello World!", commands["official_local_result"]!!.jsonPrimitive.content)
        assertTrue(commands["local"]!!.jsonPrimitive.content.contains("--blockchain-rid"))
        assertTrue(payload["run_dapp"]!!.jsonPrimitive.content.contains("run-dapp-cli"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("chr query hello_world"))
        assertTrue(notes.contains("Hello World!"))
        assertTrue(notes.contains("commands/tx"))
        assertTrue(notes.contains("HELP ONLY"))
        assertFalse(notes.contains("chr keygen"))
        assertFalse(notes.contains("FC17B67D66F6F35A5D8B75ED3F83AE222FB8C8FCA241624F06285150F10C6BAC"))
        assertFalse(notes.contains("2D17B27D4F69E0A91B0CA39AF53EFA9B82CDAF698EF906A67C71C266983EEB7A"))
        assertEquals(payload, result.structuredContent)
    }

    @Test
    fun chromiaRellTypesHelpQuotesOfficialSimpleAndCollections() = runBlocking {
        val result = ChromiaRellTypesHelpStrategy().execute(
            CallToolRequest(name = "chromia_rell_types_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("0.33.x", payload["cli"]!!.jsonPrimitive.content)
        assertEquals("0.16.7", payload["rell"]!!.jsonPrimitive.content)
        assertEquals("chromia_rell_types_help", payload["tool"]!!.jsonPrimitive.content)
        val simple = payload["simple_types"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(simple.any { it.startsWith("boolean") })
        assertTrue(simple.any { it.startsWith("integer") })
        assertTrue(simple.any { it.startsWith("big_integer") })
        assertTrue(simple.any { it.startsWith("decimal") })
        assertTrue(simple.any { it.startsWith("text") })
        assertTrue(simple.any { it.startsWith("byte_array") })
        assertTrue(simple.any { it.startsWith("rowid") })
        assertTrue(simple.any { it.startsWith("json") })
        assertTrue(payload["integer_example"]!!.jsonPrimitive.content.contains("integer"))
        assertTrue(payload["big_integer_example"]!!.jsonPrimitive.content.contains("9223372036854775832L"))
        assertTrue(payload["collection_example"]!!.jsonPrimitive.content.contains("list<integer>"))
        assertTrue(payload["collection_example"]!!.jsonPrimitive.content.contains("map<text, integer>"))
        assertTrue(payload["combine_example"]!!.jsonPrimitive.content.contains("add_all_copy"))
        assertTrue(payload["nullable_example"]!!.jsonPrimitive.content.contains("integer?"))
        assertTrue(payload["tuple_example"]!!.jsonPrimitive.content.contains("(integer, text)"))
        assertTrue(payload["range_example"]!!.jsonPrimitive.content.contains("range(10)"))
        assertTrue(payload["gtv_example"]!!.jsonPrimitive.content.contains("from_gtv"))
        assertTrue(payload["virtual_example"]!!.jsonPrimitive.content.contains("virtual<list<Record>>"))
        assertEquals(
            "https://docs.chromia.com/rell/language-features/types/sub-types",
            payload["subtypes_docs"]!!.jsonPrimitive.content
        )
        val aliases = payload["aliases"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(aliases.any { it.contains("pubkey") && it.contains("byte_array") })
        assertTrue(aliases.any { it.contains("timestamp") && it.contains("integer") })
        val skipped = payload["skipped_404"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("/types/subtypes") && it.contains("404") })
        val notes = payload["notes"]!!.jsonPrimitive.content
        val allText = listOf(
            notes,
            simple.toString(),
            aliases.toString(),
            payload["collection_example"]!!.jsonPrimitive.content,
            payload["byte_array_example"]!!.jsonPrimitive.content,
            payload["virtual_example"]!!.jsonPrimitive.content
        ).joinToString("\n")
        assertTrue(notes.contains("simple-types"))
        assertTrue(notes.contains("collection-types"))
        assertTrue(notes.contains("complex-types"))
        assertTrue(notes.contains("sub-types"))
        assertTrue(notes.contains("virtual-types"))
        assertFalse(notes.contains("https://docs.chromia.com/rell/language-features/types/subtypes\n") || notes.endsWith("/types/subtypes"))
        assertTrue(notes.contains("/types/sub-types"))
        assertTrue(notes.contains("does not generate a key"))
        assertTrue(notes.contains("does not send signed"))
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("mnemonic"))
        assertFalse(allText.contains("BEGIN PRIVATE"))
        assertFalse(allText.contains("privkey"))
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaRellTypesHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaRellExpressionsHelpQuotesOfficialOperatorsOnly() = runBlocking {
        val result = ChromiaRellExpressionsHelpStrategy().execute(
            CallToolRequest(name = "chromia_rell_expressions_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("chromia_rell_expressions_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("0.16.7", payload["rell"]!!.jsonPrimitive.content)
        val ops = payload["official_operators"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(ops.any { it.contains("and or not") })
        assertTrue(ops.any { it.contains("??") })
        assertTrue(ops.any { it.contains("===") })
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("operators"))
        assertTrue(notes.contains("conditional-expressions"))
        assertTrue(notes.contains("jump-expressions"))
        assertTrue(notes.contains("lambda-expressions"))
        assertTrue(payload["lambda_example"]!!.jsonPrimitive.content.contains("x -> x * 2"))
        assertTrue(payload["jump_example"]!!.jsonPrimitive.content.contains("return -1"))
        assertFalse(ops.any { it.contains("&&") || it.contains("||") })
        assertTrue(notes.contains("and / or / not"))
        val allText = listOf(notes, ops.toString(), payload["lambda_example"]!!.jsonPrimitive.content).joinToString("\n")
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("chr tx"))
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaRellExpressionsHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaRellStatementsHelpQuotesOfficialValVarIfWhen() = runBlocking {
        val result = ChromiaRellStatementsHelpStrategy().execute(
            CallToolRequest(name = "chromia_rell_statements_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("chromia_rell_statements_help", payload["tool"]!!.jsonPrimitive.content)
        val statements = payload["statements"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(statements.any { it.startsWith("val") })
        assertTrue(statements.any { it.startsWith("var") })
        assertTrue(statements.any { it.startsWith("when") })
        assertTrue(payload["val_example"]!!.jsonPrimitive.content.contains("val x = 123"))
        assertTrue(payload["when_statement_example"]!!.jsonPrimitive.content.contains("when (x)"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("local-variable"))
        assertTrue(notes.contains("does not generate a key"))
        val allText = listOf(notes, statements.toString()).joinToString("\n")
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("chr tx"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaRellStatementsHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaRellDatabaseHelpIsRellSyntaxOnly() = runBlocking {
        val result = ChromiaRellDatabaseHelpStrategy().execute(
            CallToolRequest(name = "chromia_rell_database_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("chromia_rell_database_help", payload["tool"]!!.jsonPrimitive.content)
        assertTrue(payload["create_example"]!!.jsonPrimitive.content.contains("create user"))
        assertTrue(payload["update_example"]!!.jsonPrimitive.content.contains("update user"))
        assertTrue(payload["delete_example"]!!.jsonPrimitive.content.contains("delete user"))
        assertTrue(payload["at_example"]!!.jsonPrimitive.content.contains("user @"))
        val skipped = payload["skipped_404"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("create-copy") && it.contains("404") })
        assertTrue(skipped.any { it.contains("/database/at") && it.contains("404") })
        assertTrue(skipped.any { it.contains("architecture") && it.contains("404") })
        assertTrue(skipped.any { it.contains("scaling") && it.contains("404") })
        assertTrue(skipped.any { it.contains("chromia start") })
        assertEquals("https://docs.chromia.com/build/database/getting-started", payload["build_getting_started"]!!.jsonPrimitive.content)
        assertEquals("chr node start", payload["official_local_node"]!!.jsonPrimitive.content)
        assertTrue(payload["table_names_source"]!!.jsonPrimitive.content.contains("chainId"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("inside operations"))
        assertTrue(notes.contains("/build/database/getting-started"))
        assertTrue(notes.contains("does not document chr tx") || notes.contains("does not document chr tx"))
        assertFalse(notes.contains("chr tx set_name"))
        assertFalse(notes.contains("chr keygen"))
        val allText = listOf(notes, payload["create_example"]!!.jsonPrimitive.content, skipped.toString()).joinToString("\n")
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaRellDatabaseHelp.toJson(), result.structuredContent)
    }


    @Test
    fun chromiaRellSystemlibHelpStartedOfficialGlobals() = runBlocking {
        val result = ChromiaRellSystemlibHelpStrategy().execute(
            CallToolRequest(name = "chromia_rell_systemlib_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("chromia_rell_systemlib_help", payload["tool"]!!.jsonPrimitive.content)
        assertEquals("0.16.7", payload["rell"]!!.jsonPrimitive.content)
        val fns = payload["global_functions"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(fns.any { it.contains("abs") })
        assertTrue(fns.any { it.contains("empty") })
        assertTrue(fns.any { it.contains("require") })
        val queries = payload["system_queries"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(queries.any { it.contains("rell.get_app_structure") })
        assertTrue(queries.any { it.contains("rell.get_rell_version") })
        assertTrue(payload["require_example"]!!.jsonPrimitive.content.contains("require("))
        assertTrue(payload["system_entities_example"]!!.jsonPrimitive.content.contains("entity block"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("global-functions"))
        assertTrue(notes.contains("require-function"))
        assertTrue(notes.contains("does not generate a key"))
        assertFalse(notes.contains("chr keygen"))
        val skipped = payload["skipped"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("privkey") })
        val allText = listOf(notes, fns.toString(), queries.toString(), skipped.toString()).joinToString("\n")
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaRellSystemlibHelp.toJson(), result.structuredContent)
    }

    @Test
    fun chromiaRellSystemlibHelpExpandsOfficialNamespaces() = runBlocking {
        val result = ChromiaRellSystemlibHelpStrategy().execute(
            CallToolRequest(name = "chromia_rell_systemlib_help", arguments = buildJsonObject {}),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("true", payload["namespaces_expanded"]!!.jsonPrimitive.content)
        assertEquals(
            "https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context",
            payload["chain_context_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context",
            payload["op_context_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/rell/language-features/systemlib/namespaces/crypto",
            payload["crypto_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/rell/language-features/systemlib/namespaces/meta",
            payload["meta_docs"]!!.jsonPrimitive.content
        )
        assertEquals(
            "https://docs.chromia.com/rell/language-features/systemlib/namespaces/time",
            payload["time_docs"]!!.jsonPrimitive.content
        )
        val chain = payload["chain_context_members"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(chain.any { it.contains("chain_context.args") })
        assertTrue(chain.any { it.contains("blockchain_rid") })
        assertTrue(chain.any { it.contains("raw_config") })
        val op = payload["op_context_members"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(op.any { it.contains("op_context.exists") })
        assertTrue(op.any { it.contains("last_block_time") })
        assertTrue(op.any { it.contains("get_signers") })
        assertTrue(op.any { it.contains("emit_event") })
        val crypto = payload["crypto_hash_verify"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(crypto.any { it.contains("crypto.sha256") })
        assertTrue(crypto.any { it.contains("crypto.verify_signature") })
        assertFalse(crypto.any { it.contains("privkey_to_pubkey") })
        assertFalse(crypto.any { it.contains("eth_sign") })
        val meta = payload["meta_members"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(meta.any { it.contains("rell.meta") })
        assertTrue(meta.any { it.contains("mount_name") })
        val time = payload["time_members"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(time.any { it.contains("rell.time.format") })
        assertTrue(time.any { it.contains("ms_to_text") })
        assertTrue(time.any { it.contains("text_to_ms_or_null") })
        val specs = payload["time_specifiers"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(listOf("y", "M", "d", "H", "m", "s", "S").all { it in specs })
        assertTrue(payload["chain_context_example"]!!.jsonPrimitive.content.contains("struct module_args"))
        assertTrue(payload["op_context_example"]!!.jsonPrimitive.content.contains("op_context.block_height"))
        assertTrue(payload["crypto_verify_example"]!!.jsonPrimitive.content.contains("crypto.verify_signature"))
        assertTrue(payload["meta_example"]!!.jsonPrimitive.content.contains("rell.meta(my_op)"))
        assertTrue(payload["time_example"]!!.jsonPrimitive.content.contains("rell.time.format"))
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("not in a query"))
        assertTrue(notes.contains("transaction.block"))
        assertTrue(notes.contains("/namespaces/time"))
        assertTrue(notes.contains("HASH and VERIFY"))
        val skipped = payload["skipped"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(skipped.any { it.contains("/namespaces/rell.time") && it.contains("404") })
        assertTrue(skipped.any { it.contains("/namespaces/gtx") && it.contains("404") })
        assertTrue(skipped.any { it.contains("official printed sample keys") })
        val quoted = listOf(
            chain.toString(),
            op.toString(),
            crypto.toString(),
            meta.toString(),
            time.toString(),
            payload["chain_context_example"]!!.jsonPrimitive.content,
            payload["op_context_example"]!!.jsonPrimitive.content,
            payload["crypto_verify_example"]!!.jsonPrimitive.content,
            payload["meta_example"]!!.jsonPrimitive.content,
            payload["time_example"]!!.jsonPrimitive.content
        ).joinToString("\n")
        assertFalse(quoted.contains("privkey_to_pubkey"))
        assertFalse(quoted.contains("eth_sign"))
        assertFalse(quoted.contains("get_signature"))
        assertFalse(quoted.contains("eth_privkey_to_address"))
        val allText = listOf(notes, quoted, skipped.toString()).joinToString("\n")
        assertFalse(allText.contains("chr keygen"))
        assertFalse(allText.contains("execute_transaction"))
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        assertTrue(invented.findAll(allText).none(), invented.findAll(allText).map { it.value }.toList().toString())
        assertEquals(payload, result.structuredContent)
        assertEquals(ChromiaRellSystemlibHelp.toJson(), result.structuredContent)
    }

}
