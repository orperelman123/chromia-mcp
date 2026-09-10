package org.chromia

import org.chromia.tools.DatabaseSessionGuards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.DriverManager

/**
 * THE RUNNER'S SESSIONS EXPIRE ON THEIR OWN - proved on the URL and then on the server.
 *
 * The merge cases are pinned first because a URL that already carries `options` (an operator's
 * search_path, say) must keep it. Then a real connection is opened from the guarded URL and the
 * server is asked what the session's settings ARE: `SHOW` answers from the session, not from the
 * driver, so a driver that dropped the parameter would fail here.
 */
class DatabaseSessionGuardsTest {

    private val guards = "options=--idle_in_transaction_session_timeout%3D600000&tcpKeepAlive=true"

    @Test
    fun `a bare URL and a URL with parameters both get the guards appended`() {
        assertEquals(
            "jdbc:postgresql://h:5433/db?$guards",
            DatabaseSessionGuards.withSessionGuards("jdbc:postgresql://h:5433/db")
        )
        assertEquals(
            "jdbc:postgresql://h:5433/db?user=u&password=p&$guards",
            DatabaseSessionGuards.withSessionGuards("jdbc:postgresql://h:5433/db?user=u&password=p")
        )
    }

    @Test
    fun `an options parameter already present is extended, not replaced`() {
        val merged = DatabaseSessionGuards.withSessionGuards(
            "jdbc:postgresql://h/db?options=-c%20search_path%3Dapp&user=u"
        )
        val options = java.net.URLDecoder.decode(merged.substringAfter("options=").substringBefore("&"), Charsets.UTF_8)
        assertTrue(options.startsWith("-c search_path=app "), options)
        assertTrue(options.contains("--idle_in_transaction_session_timeout=600000"), options)
        assertEquals(1, merged.split("options=").size - 1, "exactly one options parameter: $merged")
        assertTrue(merged.contains("user=u"), merged)
        // Applying it twice changes nothing.
        assertEquals(merged, DatabaseSessionGuards.withSessionGuards(merged))
    }

    @Test
    fun `a URL that is not PostgreSQL's is returned untouched`() {
        assertEquals("jdbc:h2:mem:x", DatabaseSessionGuards.withSessionGuards("jdbc:h2:mem:x"))
    }

    @Test
    fun theServerConfirmsTheSessionCarriesTheGuards() {
        val url = DatabaseSessionGuards.withSessionGuards(LiveEnv.requireDatabaseUrl("the session guards are proven on the real server"))
        DriverManager.getConnection(url).use { connection ->
            fun show(setting: String): String = connection.createStatement().use { statement ->
                statement.executeQuery("SHOW $setting").use { rs -> rs.next(); rs.getString(1) }
            }
            assertEquals("10min", show("idle_in_transaction_session_timeout"))
        }
    }
}
