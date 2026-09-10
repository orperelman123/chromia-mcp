package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Duration

/**
 * WHAT AN EARLIER, KILLED JVM LEAVES BEHIND IS REAPED BEFORE THE FIRST RUN.
 *
 * An abandoned session is made for real: a second connection begins a transaction, runs a
 * statement, and is then left alone - `idle in transaction`, exactly what a killed JVM's
 * session looks like to the server. The reaper is asked to terminate anything older than zero
 * seconds, which is only sensible in a test; in the gate the threshold is ten minutes, and the
 * session of a live run never idles that long inside a transaction. A healthy idle connection
 * (no transaction) and the reaper's own connection are untouched.
 */
class AbandonedSessionReaperTest {

    @Test
    fun anIdleInTransactionSessionIsTerminatedAndNamedAnIdleConnectionIsNot() {
        val url = LiveEnv.requireDatabaseUrl("an abandoned session is reaped on the real server")

        val abandoned: Connection = DriverManager.getConnection(url)
        val healthy: Connection = DriverManager.getConnection(url)
        try {
            abandoned.autoCommit = false
            abandoned.createStatement().use { it.execute("SELECT 1") } // now 'idle in transaction'
            healthy.createStatement().use { it.execute("SELECT 1") }   // autocommit: plain 'idle'

            val reaped = LiveEnv.reapAbandonedSessions(url, olderThan = Duration.ZERO)

            assertEquals(1, reaped.size, "exactly the abandoned session: $reaped")
            assertTrue(reaped.single().contains("state='idle in transaction'"), reaped.single())
            assertTrue(reaped.single().contains("terminated=true"), reaped.single())

            // The abandoned session is gone: its next statement is refused by the server.
            val refused = assertThrows(SQLException::class.java) { abandoned.createStatement().use { it.execute("SELECT 1") } }
            assertTrue(
                // 57P01 "terminating connection due to administrator command", or the driver's
                // 08006 "I/O error" when the server closed the socket before the client read it.
                refused.sqlState == "57P01" || refused.sqlState == "08006" ||
                    refused.message.orEmpty().contains("terminating connection") ||
                    refused.message.orEmpty().contains("This connection has been closed"),
                "expected the server's termination, got ${refused.sqlState}: ${refused.message}"
            )
            // The healthy connection still answers.
            healthy.createStatement().use { it.executeQuery("SELECT 1").use { rs -> rs.next(); assertEquals(1, rs.getInt(1)) } }
        } finally {
            runCatching { abandoned.close() }
            runCatching { healthy.close() }
        }
    }

    @Test
    fun aDatabaseWithNothingAbandonedReapsNothing() {
        val url = LiveEnv.requireDatabaseUrl("the reaper is quiet on a clean database")
        // Ten minutes: the gate's own threshold. Nothing in this JVM is that old inside a transaction.
        assertEquals(emptyList<String>(), LiveEnv.reapAbandonedSessions(url, olderThan = Duration.ofMinutes(10)))
    }
}
