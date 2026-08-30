package org.chromia

import org.chromia.tools.PromptManager
import org.chromia.tools.docs.fetcher.DocsConfigParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DocsRepositoriesConfigTest {

    @Test
    fun publicGitHubRemotesOnly() {
        val config = DocsConfigParser().parseConfig()
        val names = config.repositories.map { it.name }
        val urls = config.repositories.map { it.url }
        val byName = config.repositories.associateBy { it.name }

        assertEquals(
            listOf("rell", "postchain", "ft4-lib", "directory-chain", "postchain-eif", "chromia-cli", "postchain-client"),
            names
        )
        assertEquals(7, urls.size)
        assertTrue(urls.all { it.startsWith("https://github.com/ChromiaProject/") && it.endsWith(".git") })
        assertTrue(urls.none { it.contains("bitbucket", ignoreCase = true) })
        assertTrue(urls.none { "{{" in it })
        assertEquals(
            listOf("dev", "dev", "development", "dev", "dev", "dev", "dev"),
            config.repositories.map { it.branch }
        )

        assertEquals(
            listOf("doc", "rell-base", "rell-gtx", "rell-api-base", "rell-api-gtx", "rell-api-native", "rell-api-shell"),
            byName.getValue("rell").subdirectories
        )
        assertEquals(
            listOf(
                "doc",
                "postchain-base",
                "postchain-common",
                "postchain-gtv",
                "postchain-gtx-data",
                "postchain-server",
                "postchain-cli",
                "postchain-spi"
            ),
            byName.getValue("postchain").subdirectories
        )
        assertEquals(listOf("doc", "rell", "client"), byName.getValue("ft4-lib").subdirectories)
        assertEquals(listOf("doc", "src"), byName.getValue("directory-chain").subdirectories)
        assertEquals(listOf("doc"), byName.getValue("postchain-eif").subdirectories)
        assertEquals(listOf("docs"), byName.getValue("chromia-cli").subdirectories)
        assertEquals(listOf("postchain-client/doc"), byName.getValue("postchain-client").subdirectories)
        assertTrue(byName.getValue("postchain-client").url.contains("postchain-client"))
        assertEquals("https://docs.chromia.com/sitemap.xml", config.settings.sitemapUrl)
    }

    @Test
    fun chromiaStackExpertPromptIsRegistered() {
        val prompts = PromptManager()
        assertTrue(prompts.getCategories().contains("chromia_stack"))
        val expert = prompts.getPrompt("chromia_stack", "Chromia stack expert")
        assertNotNull(expert)
        val text = expert!!["prompt"]?.toString().orEmpty()
        assertTrue(text.contains("Source wins over docs"))
        assertTrue(text.contains("0.16.7"))
        assertTrue(text.contains("merkle_hash_version"))
    }

}
