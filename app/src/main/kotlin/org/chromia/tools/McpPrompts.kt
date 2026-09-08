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

    /**
     * THE CAP ON A `prompts/get` ARGUMENT, in characters.
     *
     * Adversary round 18 (section 7) fetched all 82 prompts with a 5,000-character
     * value and got it back VERBATIM inside the prompt text of every one of the 48
     * that take an argument - the largest answer 15,316 B, roughly three times the
     * input, because several templates interpolate the same blank more than once.
     * Nothing errored and nothing was rejected. That is not an injection of its
     * own; it is an unbounded amplification into the agent's context from a value
     * the agent did not choose, and the amplification factor is the template's,
     * not the caller's.
     *
     * A prompt argument is a PHRASE the prompt reads back - "a lending market",
     * "escrow", a chain rid. 1,024 characters is far more than any of the 48
     * blanks is for, and it bounds the worst case at the longest template
     * (2,947 characters) plus the cap times its repeats, instead of at whatever
     * the caller sent.
     */
    const val MAX_ARGUMENT_CHARS = 1024

    /**
     * Control characters a prompt argument may not carry. Tab, newline and
     * carriage return are ordinary text in a multi-line phrase; the rest of C0,
     * DEL and the C1 block are not - they are what an ESC, a BEL or a NUL in the
     * value (round 18 sent all three) would use to break the structure of the
     * message the prompt renders into. Returns the index of the first one, or -1.
     */
    fun firstForbiddenControlChar(value: String): Int = value.indexOfFirst { c ->
        (c.code < 0x20 && c != '\n' && c != '\r' && c != '\t') || c.code == 0x7F || c.code in 0x80..0x9F
    }

    /**
     * The bound, applied to every argument of one `prompts/get`. Throws
     * [IllegalArgumentException] naming the prompt, the argument, the measurement
     * and the cap, so a client that hits it is told exactly what to change - the
     * previous behaviour was to echo the value back and say nothing.
     */
    fun checkArguments(promptName: String, arguments: Map<String, String>?) {
        arguments?.forEach { (argument, value) ->
            require(value.length <= MAX_ARGUMENT_CHARS) {
                "prompt `$promptName`: argument `$argument` is ${value.length} characters and the " +
                    "cap is $MAX_ARGUMENT_CHARS. A prompt argument is a phrase the prompt reads " +
                    "back to you, and a template may interpolate the same blank several times, so " +
                    "everything past the cap is amplified verbatim into the agent's context. " +
                    "Shorten it, or hand the long text to the tool this prompt names."
            }
            val at = firstForbiddenControlChar(value)
            require(at < 0) {
                "prompt `$promptName`: argument `$argument` carries the control character " +
                    "U+" + value[at].code.toString(16).uppercase().padStart(4, '0') +
                    " at index $at. A prompt argument is rendered into the message a model reads; " +
                    "tab, newline and carriage return are allowed and the rest of C0, DEL and C1 " +
                    "are not, because they are structure rather than text."
            }
        }
    }

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

        /**
         * The `prompts/get` answer. Every argument is bounded first
         * ([checkArguments]): an over-long or control-character-carrying value is
         * an error naming the cap, never a value echoed back.
         */
        fun toResult(arguments: Map<String, String>?): GetPromptResult {
            checkArguments(name, arguments)
            return GetPromptResult(
                description = "$description (primary tool: $tool)",
                messages = listOf(PromptMessage(role = Role.User, content = TextContent(render(arguments))))
            )
        }
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
