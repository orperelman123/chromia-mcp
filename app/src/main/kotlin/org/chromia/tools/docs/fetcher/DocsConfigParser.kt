package org.chromia.tools.docs.fetcher

import kotlinx.serialization.json.Json
import org.chromia.getResourcePath
import java.io.File

class DocsConfigParser {
    val docsConfigPath = "docs-repositories.json"
    private val classLoader = javaClass.classLoader
    val docsConfig = getResourcePath(docsConfigPath)?.let { File(it) }?.takeIf { it.isFile }
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun parseConfig(): DocsRepositoriesConfig {
        val jsonContent = classLoader.getResourceAsStream(docsConfigPath)?.bufferedReader()?.use { it.readText() }
            ?: docsConfig?.readText()
            ?: error("Configuration file for docs is missing!")
        return parseConfig(jsonContent)
    }

    fun parseConfig(jsonContent: String): DocsRepositoriesConfig {
        require(jsonContent.isNotBlank()) { "Configuration file for docs is missing!" }
        val parsed = json.decodeFromString<DocsRepositoriesConfig>(jsonContent)
        validate(parsed)
        return parsed
    }

    companion object {
        fun validate(config: DocsRepositoriesConfig) {
            require(config.repositories.isNotEmpty()) { "docs-repositories.json must list at least one repository" }
            config.repositories.forEach { repo ->
                require(repo.name.isNotBlank()) { "repository name must not be blank" }
                require(repo.url.isNotBlank()) { "repository ${repo.name} url must not be blank" }
                require(repo.branch.isNotBlank()) { "repository ${repo.name} branch must not be blank" }
            }
            require(config.settings.concurrentFetches >= 1) { "concurrent_fetches must be >= 1" }
            require(config.settings.timeoutSeconds >= 1) { "timeout_seconds must be >= 1" }
        }
    }
}
