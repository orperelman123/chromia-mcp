package org.chromia.tools

import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.Prompt
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The `prompts/list` + `prompts/get` view of the prompt catalogue behind
 * `get_prompts` (audit F8: `prompts/list` answered JSON-RPC -32601, so a client
 * that speaks only the prompts protocol saw none of them, and the one 42,120-byte
 * `get_prompts` call was the only way in).
 *
 * The catalogue itself is unchanged - the same
 * `app/src/main/resources/prompt_templates.json` entries, with their `tool` field
 * now storing the BARE tool name (`scaffold_dapp`, not
 * `mcp_chromia-mcp_scaffold_dapp`, which named 73 tools that do not exist).
 */
object McpPrompts {

    /**
     * The prompt that goes first: the opinionated idea -> deployed-dapp loop, and
     * the best single artefact in the server per the audit.
     */
    const val FLAGSHIP = "secure_dapp_workflow"

    /** `{placeholder}` in a prompt template becomes a prompt argument. */
    private val PLACEHOLDER = Regex("\\{([A-Za-z][A-Za-z0-9_]*)}")

    /** One catalogue entry, as MCP sees it. */
    data class CataloguePrompt(
        val name: String,
        val category: String,
        val title: String,
        val description: String,
        val tool: String,
        val template: String,
        val argumentNames: List<String>
    ) {
        fun toPrompt(): Prompt = Prompt(
            name = name,
            title = title,
            description = description,
            // No per-argument description on purpose: the name IS the placeholder
            // it fills, and 89 prompts x "Value for {x} in the prompt text." was
            // 9 KB of an agent's first contact saying nothing (audit F9).
            arguments = argumentNames.map { argument -> PromptArgument(name = argument, required = true) }
        )

        /**
         * The template with the arguments the client supplied substituted in.
         * A placeholder with no value is LEFT AS `{name}` on purpose: the prompt
         * still reads as an instruction with a blank to fill, which is more useful
         * to a model than an empty string that silently changes the meaning.
         */
        fun render(arguments: Map<String, String>?): String =
            PLACEHOLDER.replace(template) { match ->
                arguments?.get(match.groupValues[1]) ?: match.value
            }

        fun toResult(arguments: Map<String, String>?): GetPromptResult = GetPromptResult(
            description = "$description (primary tool: $tool)",
            messages = listOf(PromptMessage(role = Role.User, content = TextContent(render(arguments))))
        )
    }

    /** MCP prompt name for a catalogue title: lower snake_case, stable across runs. */
    fun promptName(title: String): String =
        title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun string(entry: JsonObject, key: String): String? =
        runCatching { entry[key]?.jsonPrimitive?.content }.getOrNull()?.takeIf { it.isNotBlank() }

    /**
     * Every catalogue entry as a [CataloguePrompt], [FLAGSHIP] first and the rest
     * in catalogue order. A duplicate slug keeps the first and suffixes the rest,
     * so a name always resolves to exactly one prompt.
     */
    fun catalogue(promptManager: PromptManager = PromptManager()): List<CataloguePrompt> {
        val taken = HashSet<String>()
        val all = promptManager.getCategories().flatMap { category ->
            promptManager.getPromptsForCategory(category).orEmpty().mapNotNull { entry ->
                val title = string(entry, "title") ?: return@mapNotNull null
                val template = string(entry, "prompt") ?: return@mapNotNull null
                var name = promptName(title)
                if (name.isEmpty()) name = "prompt"
                if (!taken.add(name)) {
                    var suffix = 2
                    while (!taken.add("${name}_$suffix")) suffix++
                    name = "${name}_$suffix"
                }
                CataloguePrompt(
                    name = name,
                    category = category,
                    title = title,
                    description = string(entry, "description").orEmpty(),
                    tool = promptManager.canonicalToolName(string(entry, "tool")),
                    template = template,
                    argumentNames = PLACEHOLDER.findAll(template)
                        .map { it.groupValues[1] }.distinct().toList()
                )
            }
        }
        return all.sortedBy { if (it.name == FLAGSHIP) 0 else 1 }
    }

    /**
     * Every tool this catalogue tells an agent to call. The `tool` field of each
     * entry plus any tool-shaped identifier in the prompt text that is not in
     * [NON_TOOL_IDENTIFIERS] - so a prompt that names a tool this server does not
     * implement fails PromptCatalogueMcpTest instead of sending the agent into
     * "Tool ... not found".
     */
    fun referencedToolNames(promptManager: PromptManager = PromptManager()): Set<String> {
        val identifier = Regex("\\b[a-z][a-z0-9]*(?:_[a-z0-9]+)+\\b")
        return catalogue(promptManager).flatMap { prompt ->
            val fromText = identifier.findAll(prompt.template + " " + prompt.description)
                .map { it.value }
                .filter { it !in NON_TOOL_IDENTIFIERS }
            (fromText + prompt.tool).toList()
        }.filter { it.isNotBlank() }.toSet()
    }

    /**
     * Snake_case identifiers the prompts legitimately mention that are NOT MCP
     * tools: Rell/FT4 vocabulary, dapp query names, and verify_guards verdicts.
     * Anything else in a prompt must be a real tool on this server.
     */
    val NON_TOOL_IDENTIFIERS: Set<String> = setOf(
        // FT4 / Rell vocabulary
        "ras_open", "ras_transfer_open", "merkle_hash_version", "require_mandatory_flags",
        "does_account_require_memo", "module_args", "api_version", "config_delay",
        "hello_world", "directory_chain", "page_size", "page_cursor", "test_name",
        // verify_guards verdicts
        "load_bearing", "baseline_red", "run_must_fail",
        // dapp queries an agent runs through chromia_dapp_query, not tools
        "get_app_structure", "get_assets_by_name", "get_account_by_id", "propose_token",
        "get_token_chain_constants", "get_proposals_by_proposer", "get_all_bridges"
    )
}
