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
        // 0.0.1 was a silent hardcoded placeholder. gradle.properties pins 0.2.2
        // (latest official GitLab tag). Publish still overrides with -Pversion.
        assertNotEquals("0.0.1", App.SERVER_VERSION)
        assertNotEquals("unspecified", App.SERVER_VERSION)
    }
}
