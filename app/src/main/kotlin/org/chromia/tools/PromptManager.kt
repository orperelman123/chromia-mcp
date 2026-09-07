package org.chromia.tools

import kotlinx.serialization.json.*

/**
 * The prompt catalogue, read from a classpath resource.
 *
 * NOT `open`, and neither is [getCategories]: both used to be, for exactly one
 * reason - a test overrode `getCategories()` to `error("catalog boom")` so that
 * `PromptsToolStrategy`'s failure branch had something to catch. That made the
 * production class carry a subclassing seam it has no production use for, and
 * proved only that the strategy catches a throw the real loader cannot produce.
 * [resourceName] replaces it: the test points a REAL PromptManager at a REAL
 * malformed catalogue and the failure comes out of the real parse.
 */
class PromptManager(private val resourceName: String = DEFAULT_RESOURCE) {
    companion object {
        const val DEFAULT_RESOURCE = "prompt_templates.json"

        /** Real MCP tool prefix, e.g. mcp__chromia__validate_chromia_yml. */
        private val MCP_SERVER_PREFIX = Regex("^mcp__[a-z0-9-]+__")
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val templates: JsonObject? by lazy {
        loadTemplatesInternal()
    }

    private fun loadTemplatesInternal(): JsonObject? {
        val inputStream = javaClass.classLoader.getResourceAsStream(resourceName)
            ?: return null

        return inputStream.use { stream ->
            stream.bufferedReader()
                .use {
                    it.readText()
                }.takeIf { it.isNotBlank() }
                ?.let {
                    runCatching { json.decodeFromString<JsonObject>(it) }
                        .getOrElse { error ->
                            throw IllegalStateException(
                                "prompt catalogue '$resourceName' is not readable JSON: ${error.message}",
                                error
                            )
                        }
                }
        }
    }

    fun getCategories(): List<String> = templates?.keys?.toList() ?: emptyList()

    fun getPromptsForCategory(category: String) =
        templates?.get(category)?.jsonArray?.mapNotNull { element ->
            element.jsonObject
        }

    fun getPrompt(category: String, title: String): JsonObject? {
        val prompts = getPromptsForCategory(category) ?: return null

        return prompts.find { prompt ->
            prompt["title"]?.jsonPrimitive?.content == title
        }
    }

    fun getToolForPrompt(promptTemplate: JsonObject): String? {
        return promptTemplate["tool"]?.jsonPrimitive?.content
    }

    fun canonicalToolName(raw: String?): String {
        val name = raw.orEmpty().trim()
        return name
            .removePrefix("mcp_chromia-mcp_")
            .replace(MCP_SERVER_PREFIX, "")
    }

    fun matchesTool(promptTemplate: JsonObject, tool: String): Boolean {
        return canonicalToolName(getToolForPrompt(promptTemplate)) == canonicalToolName(tool)
    }

    fun searchPrompts(query: String): Map<String, List<JsonObject>> {
        if (query.isBlank()) return emptyMap()

        return getCategories().associateWith { category ->
            getPromptsForCategory(category)?.filter { prompt ->
                searchInPrompt(prompt, query)
            } ?: emptyList()
        }.filterValues { it.isNotEmpty() }
    }

    private fun searchInPrompt(prompt: JsonObject, query: String): Boolean {
        val searchFields = listOf("title", "description", "prompt", "tool")

        return searchFields.any { field ->
            prompt[field]?.jsonPrimitive?.content?.contains(query, ignoreCase = true) == true
        }
    }

    fun getPromptsByTool(toolName: String): Map<String, List<JsonObject>> {
        return getCategories().associateWith { category ->
            getPromptsForCategory(category)?.filter { prompt ->
                matchesTool(prompt, toolName)
            } ?: emptyList()
        }.filterValues { it.isNotEmpty() }
    }

    fun getStatistics(): PromptStatistics {
        val categories = getCategories()
        val totalPrompts = categories.sumOf { category ->
            getPromptsForCategory(category)?.size ?: 0
        }

        val toolsUsed = categories.flatMap { category ->
            getPromptsForCategory(category)?.mapNotNull { prompt ->
                getToolForPrompt(prompt)
            } ?: emptyList()
        }.distinct()

        return PromptStatistics(
            totalCategories = categories.size,
            totalPrompts = totalPrompts,
            toolsUsed = toolsUsed.size,
            categoriesWithPrompts = categories.filter { category ->
                (getPromptsForCategory(category)?.size ?: 0) > 0
            }
        )
    }
}

data class PromptStatistics(
    val totalCategories: Int,
    val totalPrompts: Int,
    val toolsUsed: Int,
    val categoriesWithPrompts: List<String>
)
