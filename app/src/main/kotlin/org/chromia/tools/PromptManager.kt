package org.chromia.tools

import kotlinx.serialization.json.*

open class PromptManager {
    private companion object {
        /** Real MCP tool prefix, e.g. mcp__chromia__validate_chromia_yml. */
        private val MCP_SERVER_PREFIX = Regex("^mcp__[a-z0-9-]+__")
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val templates: JsonObject? by lazy {
        loadTemplatesInternal()
    }

    private fun loadTemplatesInternal(): JsonObject? {
        val resourceName = "prompt_templates.json"

        val inputStream = javaClass.classLoader.getResourceAsStream(resourceName)
            ?: return null

        return inputStream.use { stream ->
            stream.bufferedReader()
                .use {
                    it.readText()
                }.takeIf { it.isNotBlank() }
                ?.let {
                    json.decodeFromString<JsonObject>(it)
                }
        }
    }

    open fun getCategories(): List<String> = templates?.keys?.toList() ?: emptyList()

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
