package org.chromia

import org.chromia.tools.McpPrompts
import org.chromia.tools.McpResources
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class McpResourcesTest {

    @Test
    fun capabilitiesMatchRegisteredSurfaces() {
        assertNotNull(App.SERVER_CAPABILITIES.tools)
        assertEquals(false, App.SERVER_CAPABILITIES.tools?.listChanged)

        val resources = App.SERVER_CAPABILITIES.resources
        assertNotNull(resources)
        assertEquals(false, resources!!.subscribe)
        assertEquals(false, resources.listChanged)

        // Audit F8: prompts/list used to answer -32601 and the capability was
        // absent, so a client that speaks only the prompts protocol saw nothing.
        // The catalogue is now served three ways - prompts/list + prompts/get,
        // the get_prompts tool, and chromia://config/prompt-catalog.
        assertNotNull(App.SERVER_CAPABILITIES.prompts)
        assertEquals(false, App.SERVER_CAPABILITIES.prompts?.listChanged)
    }

    @Test
    fun serverRegistersExistingStaticResourcesOnly() {
        val server = App().createMcpServer()
        assertEquals(
            setOf(
                McpResources.HEALTH_URI,
                McpResources.DOCS_REPOSITORIES_URI,
                McpResources.PROMPT_CATALOG_URI
            ),
            server.resources.keys
        )
        assertEquals(
            McpPrompts.catalogue(PromptManager()).map { it.name }.toSet(),
            server.prompts.keys,
            "the whole prompt catalogue must be registered (audit F8)"
        )
        assertEquals(McpPrompts.FLAGSHIP, server.prompts.keys.first())
        assertEquals(
            McpTools.allTools().map { it.name }.toSet(),
            server.tools.keys
        )
    }

    @Test
    fun resourceBodiesAreExistingServerData() {
        val byUri = McpResources.all(healthJson = App::healthJson).associate { it.uri to it.readText() }

        val health = byUri.getValue(McpResources.HEALTH_URI)
        assertTrue(health.contains("\"status\": \"healthy\""))
        assertTrue(health.contains(App.SERVER_NAME))
        assertTrue(health.contains(App.SERVER_VERSION))
        assertEquals(App.healthJson(), health)

        val repos = byUri.getValue(McpResources.DOCS_REPOSITORIES_URI)
        assertTrue(repos.contains("\"name\": \"rell\""))
        assertTrue(repos.contains("https://github.com/ChromiaProject/rell.git"))
        assertFalse(repos.contains("bitbucket"))

        val prompts = byUri.getValue(McpResources.PROMPT_CATALOG_URI)
        assertTrue(prompts.contains("chromia_stack"))
        assertTrue(prompts.contains("Chromia stack expert"))
        assertTrue(prompts.contains("dapp_query"))
        assertTrue(prompts.contains("chromia_dapp_query"))
        assertFalse(prompts.contains("execute_transaction"))
    }
}
