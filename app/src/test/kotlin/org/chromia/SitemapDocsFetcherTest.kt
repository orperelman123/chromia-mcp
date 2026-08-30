package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.chromia.tools.docs.fetcher.DOCS_HTTP_CONNECT_TIMEOUT_MS
import org.chromia.tools.docs.fetcher.DOCS_HTTP_REQUEST_TIMEOUT_MS
import org.chromia.tools.docs.fetcher.DocsFetcher
import org.chromia.tools.docs.fetcher.SitemapDocsFetcher
import org.chromia.tools.docs.fetcher.htmlToText
import org.chromia.tools.docs.fetcher.installDocsHttpTimeout
import org.chromia.tools.docs.fetcher.looksLikeLoginWall
import org.chromia.tools.docs.fetcher.parseSitemapLocs
import org.chromia.tools.docs.fetcher.shouldIngest
import org.chromia.tools.docs.fetcher.sitemapPageRelativePath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
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

    // F1: query strings and invalid filename chars must not reach the file system.
    @Test
    fun pageRelativePathStripsQueryAndInvalidChars() {
        assertEquals("page", sitemapPageRelativePath("https://docs.chromia.com/page?x=1"))
        assertEquals("build/page", sitemapPageRelativePath("https://docs.chromia.com/build/page?x=1&y=2"))
        assertEquals("page", sitemapPageRelativePath("https://docs.chromia.com/page#frag"))
        assertEquals("index", sitemapPageRelativePath("https://docs.chromia.com/"))
        val odd = sitemapPageRelativePath("https://docs.chromia.com/a:b<c>d\"e|f*g")
        assertFalse(odd.any { it in "<>:\"\\|?*" }, odd)
        // Must resolve into a Path without InvalidPathException (the Windows failure mode).
        java.nio.file.Paths.get("out").resolve("$odd.md")
        java.nio.file.Paths.get("out").resolve(sitemapPageRelativePath("https://docs.chromia.com/page?x=1") + ".md")
    }

    // F1: end-to-end - a sitemap URL with a query string is fetched and written to disk.
    @Test
    fun queryStringUrlIsWrittenToSanitizedFile() {
        val article = "<article><h1>page</h1><p>${"content ".repeat(50)}</p></article>"
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.toString().endsWith("sitemap.xml")) {
                        respond(
                            "<urlset><url><loc>https://docs.chromia.com/build/page?x=1</loc></url></urlset>",
                            HttpStatusCode.OK
                        )
                    } else {
                        respond("<html><body>$article</body></html>", HttpStatusCode.OK)
                    }
                }
            }
        }
        val dir = kotlin.io.path.createTempDirectory("sitemap-query-")
        try {
            val written = runBlocking {
                SitemapDocsFetcher(
                    sitemapUrl = "https://docs.chromia.com/sitemap.xml",
                    client = client
                ).fetchInto(dir)
            }
            assertEquals(1, written)
            assertTrue(dir.resolve("build").resolve("page.md").toFile().exists())
        } finally {
            client.close()
            dir.toFile().deleteRecursively()
        }
    }

    // F2: &amp; is decoded LAST so double-encoded entities decode exactly once.
    @Test
    fun ampersandIsDecodedLast() {
        val text = htmlToText("<article>Tom &amp; Jerry &amp;lt;tag&amp;gt; end padding padding padding</article>")
        assertTrue(text.contains("Tom & Jerry"), text)
        assertTrue(text.contains("&lt;tag&gt;"), text)
        assertFalse(text.contains("<tag>"), text)
    }

    // F3: docs HTTP clients must carry HttpTimeout.
    @Test
    fun docsHttpClientsInstallTimeouts() {
        assertEquals(30_000L, DOCS_HTTP_REQUEST_TIMEOUT_MS)
        assertEquals(10_000L, DOCS_HTTP_CONNECT_TIMEOUT_MS)
        val client = HttpClient(MockEngine) {
            engine { addHandler { respond("ok", HttpStatusCode.OK) } }
            installDocsHttpTimeout(this)
        }
        try {
            assertNotNull(client.pluginOrNull(HttpTimeout))
        } finally {
            client.close()
        }
    }

    // G: sitemap ingest must rethrow CancellationException instead of swallowing it.
    @Test
    fun safeSitemapIngestRethrowsCancellation() {
        val fetcher = DocsFetcher()
        assertThrows(CancellationException::class.java) {
            runBlocking {
                fetcher.safeSitemapIngest { throw CancellationException("stop") }
            }
        }
        // Ordinary failures are still swallowed and logged.
        runBlocking {
            fetcher.safeSitemapIngest { throw RuntimeException("boom") }
        }
    }
}
