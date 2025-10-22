package org.chromia.tools.docs.fetcher

import kotlinx.serialization.json.Json
import org.chromia.App.Companion.logger
import org.chromia.getResourcePath
import java.io.File

class DocsConfigParser {
    val docsConfigPath = "docs-repositories.json"
    val docsConfig = getResourcePath(docsConfigPath)?.run { File(this) }
        ?: error("Configuration file for docs is missing!")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        isLenient = false
    }

    fun parseConfig() = docsConfig.readText().let { jsonContent ->
        json.decodeFromString<DocsRepositoriesConfig>(jsonContent)
    }

    fun updateConfig(config: DocsRepositoriesConfig) = runCatching {
        val jsonContent = json.encodeToString(config)
        docsConfig.writeText(jsonContent)
    }.getOrElse { logger.error("Unable to update docs configuration: ${it.message}") }
}
