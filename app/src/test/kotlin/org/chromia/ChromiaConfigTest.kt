package org.chromia

import org.chromia.data.config.ChromiaConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChromiaConfigTest {

    @Test
    fun defaultNetworksMatchDocumentedSet() {
        val config = ChromiaConfig()
        assertEquals("mainnet", config.defaultNetwork)
        assertEquals(
            setOf("mainnet", "testnet", "devnet1", "devnet2"),
            config.predefinedNetworks.keys
        )
        assertTrue(config.predefinedNetworks.getValue("mainnet").isNotEmpty())
        assertTrue(config.predefinedNetworks.getValue("testnet").isNotEmpty())
        assertTrue(config.explorerUrl.contains("explorer.chromia.com"))
    }
}
