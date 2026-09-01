package org.chromia

import kotlinx.serialization.json.jsonPrimitive
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptTemplatesTest {

    private val registeredTools = McpTools.allTools().map { it.name }.toSet()

    @Test
    fun promptToolsMatchRegisteredMcpTools() {
        val prompts = PromptManager()
        val prefix = "mcp_chromia-mcp_"
        val referenced = mutableSetOf<String>()
        prompts.getCategories().forEach { category ->
            prompts.getPromptsForCategory(category).orEmpty().forEach { prompt ->
                val raw = prompts.getToolForPrompt(prompt).orEmpty()
                val name = raw.removePrefix(prefix)
                referenced.add(name)
                val title = prompt["title"]?.jsonPrimitive?.content
                assertTrue(
                    name in registeredTools,
                    "prompt category=$category title=$title references unknown tool $raw"
                )
            }
        }
        assertTrue(referenced.contains("fetch_docs"))
        assertTrue(referenced.contains("search"))
        assertTrue(referenced.contains("fetch"))
        assertTrue(referenced.contains("filter_blockchains"))
        assertTrue(referenced.contains("chromia_dapp_query"))
        assertTrue(referenced.contains("get_blockchains_transactions"))
        assertTrue(referenced.contains("get_transactions_by_cluster"))
        assertTrue(referenced.contains("get_all_assets"))
        assertFalse(referenced.contains("list_documentation"))
        assertFalse(referenced.contains("read_documentation"))
        assertFalse(referenced.contains("get_dashboard_data"))
        assertFalse(referenced.contains("get_network_account_count"))
        assertFalse(referenced.contains("get_network_transfer_count"))
        assertFalse(referenced.contains("get_providers_rewards"))
        assertFalse(referenced.contains("execute_transaction"))
        assertFalse(referenced.contains("send_transaction"))
    }

    @Test
    fun liveToolsHavePrompts() {
        val prompts = PromptManager()
        val toolsWithPrompts = registeredTools.filter { tool ->
            prompts.getPromptsByTool(tool).isNotEmpty()
        }.toSet()
        val expected = registeredTools - setOf("get_prompts")
        assertEquals(expected, toolsWithPrompts)
        assertFalse(prompts.getPromptsByTool("get_prompts").isNotEmpty())
    }

    @Test
    fun dappQueryCategoryHasDedicatedPrompts() {
        val prompts = PromptManager()
        assertTrue(prompts.getCategories().contains("dapp_query"))
        val discover = prompts.getPrompt("dapp_query", "Discover dApp Structure")
        val execute = prompts.getPrompt("dapp_query", "Execute dApp Query")
        assertNotNull(discover)
        assertNotNull(execute)
        assertEquals(
            "chromia_dapp_query",
            prompts.canonicalToolName(prompts.getToolForPrompt(discover!!))
        )
        assertEquals(
            "chromia_dapp_query",
            prompts.canonicalToolName(prompts.getToolForPrompt(execute!!))
        )
        val executeDescription = execute["description"]?.jsonPrimitive?.content.orEmpty()
        assertTrue(executeDescription.contains("not signed", ignoreCase = true))
    }

    @Test
    fun dappAssetUsagePointsAtFilterAssets() {
        val prompts = PromptManager()
        val prompt = prompts.getPrompt("discovery", "DApp Asset Usage")
        assertNotNull(prompt)
        val tool = prompts.canonicalToolName(prompts.getToolForPrompt(prompt!!))
        assertEquals("filter_assets", tool)
        assertFalse(tool == "get_blockchain_analytics")
        val description = prompt["description"]?.jsonPrimitive?.content.orEmpty()
        assertTrue(description.contains("asset", ignoreCase = true))
        assertFalse(description.contains("transaction counts"))
    }

    @Test
    fun canonicalToolNameStripsLegacyAndRealMcpPrefixes() {
        val prompts = PromptManager()
        assertEquals("validate_chromia_yml", prompts.canonicalToolName("mcp_chromia-mcp_validate_chromia_yml"))
        assertEquals("validate_chromia_yml", prompts.canonicalToolName("mcp__chromia__validate_chromia_yml"))
        assertEquals("fetch_docs", prompts.canonicalToolName("mcp__chromia-mcp__fetch_docs"))
        assertEquals("fetch_docs", prompts.canonicalToolName("fetch_docs"))
        assertEquals("", prompts.canonicalToolName(null))
        val prompt = kotlinx.serialization.json.buildJsonObject {
            put("tool", kotlinx.serialization.json.JsonPrimitive("mcp__chromia__fetch_docs"))
        }
        assertTrue(prompts.matchesTool(prompt, "fetch_docs"))
    }

    @Test
    fun registeredToolSetIsComplete() {
        assertTrue(
            registeredTools.containsAll(
                listOf(
                    "get_prompts",
                    "fetch_docs",
                    "search",
                    "fetch",
                    "chromia_dapp_query",
                    "get_network_stats",
                    "filter_blockchains",
                    "get_blockchains_transactions",
                    "get_transactions_by_cluster",
                    "get_all_assets",
                    "filter_assets",
                    "get_blockchain_analytics",
                    "scaffold_dapp",
                    "validate_chromia_yml",
                    "ft4_module_args",
                    "chr_build_help",
                    "write_deployment_config",
                    "chr_deploy_help",
                    "chr_node_help",
                    "chr_query_help",
                    "vault_lease_help",
                    "chr_generate_client_help",
                    "chr_library_help",
                    "chr_create_rell_dapp_help",
                    "chr_tools_help",
                    "chr_seeder_help",
                    "blockchain_properties_help",
                    "chr_eif_help",
                    "chromia_yml_definitions_help",
                    "chr_completion_help",
                    "chromia_project_structure_help",
                    "chr_multi_signature_help",
                    "chromia_cookbook_help",
                    "chr_key_id_help",
                    "chromia_language_clients_help",
                    "chromia_rell_language_help",
                    "chromia_rell_types_help",
                    "chromia_rell_expressions_help",
                    "chromia_rell_statements_help",
                    "chromia_rell_database_help",
                    "chromia_rell_systemlib_help",
                    "chromia_rell_practices_help",
                    "chromia_ft4_queries_help",
                    "chromia_integrations_help",
                    "chromia_vector_search_help",
                    "check_dapp_project",
                    "check_ft4_imports",
                    "onboarding_next_step",
                    "verify_deployment"
                )
            )
        )
        assertFalse(registeredTools.contains("list_documentation"))
        assertFalse(registeredTools.contains("get_providers_rewards"))
        assertFalse(registeredTools.contains("execute_transaction"))
    }

    @Test
    fun promptToolFilterAcceptsUnprefixedName() {
        val prompts = PromptManager()
        val matches = prompts.getPromptsByTool("filter_blockchains")
        assertTrue(matches.isNotEmpty())
        val prefixed = prompts.getPromptsByTool("mcp_chromia-mcp_filter_blockchains")
        assertEquals(matches.keys, prefixed.keys)
    }
}
