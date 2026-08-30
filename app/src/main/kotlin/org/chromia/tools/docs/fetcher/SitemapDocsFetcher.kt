package org.chromia.tools.docs.fetcher

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.chromia.App.Companion.logger
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class SitemapDocsFetcher(
    private val sitemapUrl: String = DEFAULT_SITEMAP_URL,
    private val concurrentFetches: Int = 3,
    private val client: HttpClient = HttpClient()
) {
    companion object {
        const val DEFAULT_SITEMAP_URL = "https://docs.chromia.com/sitemap.xml"
        internal val skipExactPaths = setOf("/ignore_me", "/search")
        internal val loginMarkers = listOf("sign in to continue", "you need to log in")
    }

    suspend fun fetchInto(targetDir: Path): Int {
        if (sitemapUrl.isBlank()) {
            logger.info("Sitemap ingest disabled (empty sitemap_url)")
            return 0
        }
        val xml = fetchText(sitemapUrl)
        if (xml.isNullOrBlank()) {
            logger.warn("Sitemap ingest skipped: failed to download $sitemapUrl")
            return 0
        }
        val urls = parseSitemapLocs(xml).filter { shouldIngest(it) }
        if (urls.isEmpty()) {
            logger.warn("Sitemap ingest skipped: no usable URLs in $sitemapUrl")
            return 0
        }
        logger.info("Sitemap ingest: ${urls.size} public docs.chromia.com pages")
        targetDir.createDirectories()
        var written = 0
        urls.chunked(concurrentFetches.coerceAtLeast(1)).forEach { chunk ->
            val pages = coroutineScope {
                chunk.map { url ->
                    async { url to fetchPageText(url) }
                }.awaitAll()
            }
            pages.forEach { (url, text) ->
                if (text != null) {
                    writePage(targetDir, url, text)
                    written++
                }
            }
        }
        logger.info("Sitemap ingest wrote $written markdown files")
        return written
    }

    private suspend fun fetchPageText(url: String): String? {
        val html = fetchText(url) ?: return null
        val text = htmlToText(html)
        if (text.length < 200) {
            logger.info("Sitemap skip (too short): $url")
            return null
        }
        if (looksLikeLoginWall(text)) {
            logger.warn("Sitemap skip (login wall): $url")
            return null
        }
        return text
    }

    private suspend fun fetchText(url: String): String? = runCatching {
        val response = client.get(url)
        if (!response.status.isSuccess()) {
            logger.warn("Sitemap HTTP ${response.status} for $url")
            return@runCatching null
        }
        response.bodyAsText()
    }.onFailure { logger.warn("Sitemap fetch failed for $url: ${it.message}") }.getOrNull()

    private fun writePage(targetDir: Path, url: String, text: String) {
        val relative = url.removePrefix("https://docs.chromia.com/").trim('/').ifBlank { "index" }
        val file = targetDir.resolve("$relative.md")
        file.parent.createDirectories()
        file.writeText("# $url\n\n$text\n")
    }
}

internal fun parseSitemapLocs(xml: String): List<String> {
    val regex = "<loc>\\s*([^<]+)\\s*</loc>".toRegex(RegexOption.IGNORE_CASE)
    return regex.findAll(xml).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
}

internal fun shouldIngest(url: String): Boolean {
    if (!url.startsWith("https://docs.chromia.com/")) return false
    val path = url.removePrefix("https://docs.chromia.com").substringBefore("?").trimEnd('/')
    val normalized = if (path.isEmpty()) "/" else path
    return normalized !in SitemapDocsFetcher.skipExactPaths
}

internal fun htmlToText(html: String): String {
    val flags = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    val article = Regex("<article\\b[^>]*>(.*?)</article>", flags).find(html)?.groupValues?.get(1)
    val main = Regex("<main\\b[^>]*>(.*?)</main>", flags).find(html)?.groupValues?.get(1)
    val body = article ?: main ?: html
    val withHeadings = body.replace(Regex("<h([1-6])\\b[^>]*>(.*?)</h\\1>", flags)) { match ->
        val level = match.groupValues[1].toInt().coerceIn(1, 6)
        val title = match.groupValues[2].replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
        if (title.isEmpty()) " " else "\n\n${"#".repeat(level)} $title\n\n"
    }
    val withoutChrome = withHeadings
        .replace(Regex("<script\\b[^>]*>.*?</script>", flags), " ")
        .replace(Regex("<style\\b[^>]*>.*?</style>", flags), " ")
        .replace(Regex("<[^>]+>"), " ")
    return decodeEntities(withoutChrome)
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex(" *\\n+ *"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private fun decodeEntities(text: String): String = text
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")

internal fun looksLikeLoginWall(text: String): Boolean {
    val lower = text.lowercase()
    return SitemapDocsFetcher.loginMarkers.any { marker -> lower.contains(marker) }
}
