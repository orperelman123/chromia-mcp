package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ParseSseArgsTest {

    @Test
    fun defaults() {
        val options = parseSseArgs(emptyList())
        assertEquals("127.0.0.1", options.host)
        assertEquals(3001, options.port)
    }

    @Test
    fun customHostAndPort() {
        val options = parseSseArgs(listOf("--host", "0.0.0.0", "--port", "8080"))
        assertEquals("0.0.0.0", options.host)
        assertEquals(8080, options.port)
    }

    @Test
    fun invalidPort() {
        assertThrows(IllegalArgumentException::class.java) {
            parseSseArgs(listOf("--port", "not-a-number"))
        }
    }

    @Test
    fun portOutOfRange() {
        assertThrows(IllegalArgumentException::class.java) {
            parseSseArgs(listOf("--port", "0"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseSseArgs(listOf("--port", "65536"))
        }
    }

    // Audit F5: unknown option keys were silently ignored - `--sse --prot 8080`
    // started on the default port 3001 with no warning. The error must name the
    // unknown option and the valid set.
    @Test
    fun unknownOptionIsRejectedNamingItAndTheValidSet() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            parseSseArgs(listOf("--prot", "8080"))
        }
        assertTrue(e.message!!.contains("--prot"), e.message)
        assertTrue(e.message!!.contains("--host"), e.message)
        assertTrue(e.message!!.contains("--port"), e.message)
    }

    @Test
    fun usageHelpDocumentsJarEmbeddingsLookup() {
        assertTrue(USAGE_HELP.contains("CHROMIA_EMBEDDINGS_PATH"))
        assertTrue(USAGE_HELP.contains("app/build/embeddings.json"))
        assertTrue(USAGE_HELP.contains("build/embeddings.json"))
    }
}
