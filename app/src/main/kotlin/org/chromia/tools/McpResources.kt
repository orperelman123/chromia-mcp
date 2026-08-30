package org.chromia.tools

/**
 * Static MCP resources backed by data that already exists in this server.
 * Not a generated library: health JSON, classpath docs remotes, classpath prompt catalog.
 */
object McpResources {
    const val HEALTH_URI = "chromia://server/health"
    const val DOCS_REPOSITORIES_URI = "chromia://config/docs-repositories"
    const val PROMPT_CATALOG_URI = "chromia://config/prompt-catalog"
    const val JSON_MIME = "application/json"

    data class Definition(
        val uri: String,
        val name: String,
        val description: String,
        val mimeType: String = JSON_MIME,
        val readText: () -> String
    )

    fun all(healthJson: () -> String): List<Definition> = listOf(
        Definition(
            uri = HEALTH_URI,
            name = "server-health",
            description = "Server name, version, and health status (same JSON as GET /health)",
            readText = healthJson
        ),
        Definition(
            uri = DOCS_REPOSITORIES_URI,
            name = "docs-repositories",
            description = "Classpath docs-repositories.json: public GitHub remotes used for embeddings generation",
            readText = { classpathText("docs-repositories.json") }
        ),
        Definition(
            uri = PROMPT_CATALOG_URI,
            name = "prompt-catalog",
            description = "Classpath prompt_templates.json: same catalog served by the get_prompts tool",
            readText = { classpathText("prompt_templates.json") }
        )
    )

    fun classpathText(resourceName: String): String {
        val stream = McpResources::class.java.classLoader.getResourceAsStream(resourceName)
            ?: error("Missing classpath resource: $resourceName")
        return stream.bufferedReader().use { it.readText() }
    }
}
