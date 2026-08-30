package org.chromia

import kotlinx.serialization.SerializationException
import org.chromia.tools.docs.fetcher.DocsConfigParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DocsConfigParserTest {

    private val parser = DocsConfigParser()

    @Test
    fun defaultsWhenSettingsOmitted() {
        val config = parser.parseConfig("""{"repositories":[{"name":"rell","url":"https://example.com/rell.git"}]}""")
        assertEquals(3, config.settings.concurrentFetches)
        assertEquals(200, config.settings.timeoutSeconds)
        assertEquals(true, config.settings.cleanupOnExit)
        assertEquals("https://docs.chromia.com/sitemap.xml", config.settings.sitemapUrl)
        assertEquals("main", config.repositories.single().branch)
    }

    @Test
    fun ignoresUnknownKeys() {
        val config = parser.parseConfig(
            """{"unknown_top_level":true,"repositories":[{"name":"rell","url":"https://example.com/rell.git","branch":"dev"}],"settings":{"concurrent_fetches":2,"not_a_real_setting":1}}"""
        )
        assertEquals(2, config.settings.concurrentFetches)
    }

    @Test
    fun emptySitemapUrlIsAllowed() {
        val config = parser.parseConfig(
            """{"repositories":[{"name":"rell","url":"https://example.com/rell.git","branch":"dev"}],"settings":{"sitemap_url":""}}"""
        )
        assertEquals("", config.settings.sitemapUrl)
    }

    @Test
    fun blankContentFails() {
        assertThrows(IllegalArgumentException::class.java) { parser.parseConfig("   ") }
    }

    @Test
    fun emptyRepositoriesFail() {
        assertThrows(IllegalArgumentException::class.java) {
            parser.parseConfig("""{"repositories":[]}""")
        }
    }

    @Test
    fun blankRepoNameFails() {
        assertThrows(IllegalArgumentException::class.java) {
            parser.parseConfig("""{"repositories":[{"name":"","url":"https://example.com/x.git","branch":"dev"}]}""")
        }
    }

    @Test
    fun concurrentFetchesMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            parser.parseConfig(
                """{"repositories":[{"name":"rell","url":"https://example.com/rell.git","branch":"dev"}],"settings":{"concurrent_fetches":0}}"""
            )
        }
    }

    @Test
    fun malformedJsonFails() {
        assertThrows(SerializationException::class.java) {
            parser.parseConfig("{not-json")
        }
    }

    @Test
    fun unresolvedPlaceholderIsKept() {
        val config = parser.parseConfig(
            """{"repositories":[{"name":"rell","url":"https://example.com/{{MISSING_DOCS_TOKEN}}","branch":"dev"}]}"""
        )
        assertEquals("https://example.com/{{MISSING_DOCS_TOKEN}}", config.repositories.single().url)
    }

    @Test
    fun classpathConfigStillParses() {
        val config = parser.parseConfig()
        assertTrue(config.repositories.isNotEmpty())
    }
}
