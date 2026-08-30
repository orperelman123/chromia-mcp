package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.chromia.tools.docs.fetcher.SitemapDocsFetcher
import org.chromia.tools.docs.fetcher.htmlToText
import org.chromia.tools.docs.fetcher.looksLikeLoginWall
import org.chromia.tools.docs.fetcher.parseSitemapLocs
import org.chromia.tools.docs.fetcher.shouldIngest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

class SitemapDocsFetcherTest {

    @Test
    fun parseAndFilterPublicDocsUrls() {
        val xml = """
            <urlset>
              <url><loc>https://docs.chromia.com/ignore_me</loc></url>
              <url><loc>https://docs.chromia.com/search</loc></url>
              <url><loc>https://docs.chromia.com/build/cli/commands/create-rell-dapp</loc></url>
              <url><loc>https://example.com/not-chromia</loc></url>
            </urlset>
        """.trimIndent()
        val locs = parseSitemapLocs(xml)
        assertEquals(4, locs.size)
        val kept = locs.filter { shouldIngest(it) }
        assertEquals(listOf("https://docs.chromia.com/build/cli/commands/create-rell-dapp"), kept)
        assertFalse(shouldIngest("https://docs.chromia.com/ignore_me"))
        assertFalse(shouldIngest("https://docs.chromia.com/search"))
    }

    @Test
    fun htmlToTextPrefersArticleAndStripsTags() {
        val html = """
            <html><body>
              <nav>Menu</nav>
              <article><h1>create-rell-dapp</h1><p>Generates a template project</p></article>
              <script>window.x=1</script>
            </body></html>
        """.trimIndent()
        val text = htmlToText(html)
        assertTrue(text.contains("create-rell-dapp"))
        assertTrue(text.contains("Generates a template project"))
        assertFalse(text.contains("window.x"))
        assertFalse(text.contains("<h1>"))
    }

    @Test
    fun publicSitemapIsParseableWhenReachable() {
        val xml = runCatching {
            URI("https://docs.chromia.com/sitemap.xml").toURL().readText()
        }.getOrNull()
        if (xml.isNullOrBlank()) {
            System.err.println("Sitemap unreachable; skipped live check")
            return
        }
        val kept = parseSitemapLocs(xml).filter { shouldIngest(it) }
        assertTrue(kept.size > 50, "expected many public docs.chromia.com URLs, got ${kept.size}")
        assertTrue(kept.all { it.startsWith("https://docs.chromia.com/") })
    }

    @Test
    fun bitbucketMentionIsNotALoginWall() {
        val page = """
            Library install can use registry: https://bitbucket.org/chromawallet/ft3-lib
            This is the public docs.chromia.com page for chr library install.
        """.trimIndent()
        assertFalse(looksLikeLoginWall(page))
        assertFalse(SitemapDocsFetcher.loginMarkers.any { it.contains("bitbucket") })
    }

    @Test
    fun loginPhrasesAreLoginWalls() {
        assertTrue(looksLikeLoginWall("Please sign in to continue to this workspace"))
        assertTrue(looksLikeLoginWall("You need to log in before viewing this page"))
    }

    @Test
    fun blankSitemapUrlSkipsWithoutWriting() {
        val dir = kotlin.io.path.createTempDirectory("sitemap-blank-")
        try {
            val written = runBlocking {
                SitemapDocsFetcher(sitemapUrl = "").fetchInto(dir)
            }
            org.junit.jupiter.api.Assertions.assertEquals(0, written)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun failedSitemapDownloadSkips() {
        val dir = kotlin.io.path.createTempDirectory("sitemap-fail-")
        try {
            val written = runBlocking {
                SitemapDocsFetcher(sitemapUrl = "http://127.0.0.1:1/sitemap.xml").fetchInto(dir)
            }
            org.junit.jupiter.api.Assertions.assertEquals(0, written)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun httpErrorSitemapSkips() {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        "nope",
                        HttpStatusCode.InternalServerError
                    )
                }
            }
        }
        val dir = kotlin.io.path.createTempDirectory("sitemap-http-")
        try {
            val written = runBlocking {
                SitemapDocsFetcher(
                    sitemapUrl = "https://docs.chromia.com/sitemap.xml",
                    client = client
                ).fetchInto(dir)
            }
            org.junit.jupiter.api.Assertions.assertEquals(0, written)
        } finally {
            client.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun emptyUrlsetSkips() {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        "<urlset></urlset>",
                        HttpStatusCode.OK
                    )
                }
            }
        }
        val dir = kotlin.io.path.createTempDirectory("sitemap-empty-")
        try {
            val written = runBlocking {
                SitemapDocsFetcher(
                    sitemapUrl = "https://docs.chromia.com/sitemap.xml",
                    client = client
                ).fetchInto(dir)
            }
            org.junit.jupiter.api.Assertions.assertEquals(0, written)
        } finally {
            client.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun headingsBecomeMarkdown() {
        val html = """
            <html><body>
              <article><h1>create-rell-dapp</h1><p>Generates a template project</p></article>
            </body></html>
        """.trimIndent()
        val text = htmlToText(html)
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("# create-rell-dapp"))
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("Generates a template project"))
    }
}
