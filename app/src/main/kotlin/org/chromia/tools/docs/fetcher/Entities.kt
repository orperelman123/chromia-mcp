package org.chromia.tools.docs.fetcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
data class DocsRepositoriesConfig(
    @SerialName("metadata")
    val metaData: MetaData? = null,
    val repositories: List<DocumentationRepository>,
    val settings: DocumentationSettings = DocumentationSettings()
)

@Serializable
data class DocumentationRepository(
    val name: String,
    @Serializable(with = PlaceholderResolverTransformer::class)
    val url: String,
    val branch: String = "main",
    val subdirectories: List<String> = emptyList()
)

@Serializable
data class DocumentationSettings(
    @SerialName("concurrent_fetches")
    val concurrentFetches: Int = 3,
    @SerialName("temp_dir_prefix")
    val tempDirPrefix: String = "chromia_docs_",
    @SerialName("timeout_seconds")
    val timeoutSeconds: Int = 200,
    @SerialName("cleanup_on_exit")
    val cleanupOnExit: Boolean = true,
    @SerialName("sitemap_url")
    val sitemapUrl: String = "https://docs.chromia.com/sitemap.xml"
)

// Catalog-level description stays on MetaData; docs-repositories.json has no per-repo descriptions to move.
@Serializable
data class MetaData(
    @SerialName("last_updated")
    val lastUpdated: String? = null,
    val description: String? = null,
)

object PlaceholderResolverTransformer : JsonTransformingSerializer<String>(String.serializer()) {
    private val pattern = "\\{\\{(\\w+)}}".toRegex()


    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive && element.isString) {
            val resolved = pattern.replace(element.content) { match ->
                val key = match.groupValues[1].trim()
                System.getenv(key) ?: "{{$key}}" // Keep placeholder if not found
            }
            return JsonPrimitive(resolved)
        }
        return element
    }
}
