package org.chromia

import org.chromia.tools.RagStore
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RagStoreGeneratePathTest {

    @Test
    fun noUploadConstructionDoesNotLoadRegistryEmbeddings() {
        val store = RagStore(loadFromRegistry = false)
        assertNull(store.embeddingStore)
    }
}
