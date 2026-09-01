package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerVersionTest {

    @Test
    fun healthVersionIsGradleProjectVersion() {
        assertEquals(BuildInfo.VERSION, App.SERVER_VERSION)
        assertTrue(App.SERVER_VERSION.isNotBlank())
        // 0.0.1 was a silent hardcoded placeholder. gradle.properties holds this
        // fork's release version; CI/release still override with -Pversion.
        assertNotEquals("0.0.1", App.SERVER_VERSION)
        assertNotEquals("unspecified", App.SERVER_VERSION)
    }
}
