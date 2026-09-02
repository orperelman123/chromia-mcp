package org.chromia

import org.chromia.tools.TestnetProvisioning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * The zero-human funding-key path, tested without anyone's keys.
 *
 * `TestnetProvisioningLiveTest.liveDefaultChrKeystorePathResolvesARegisteredAccount`
 * proves two separate things at once: that a funding key RESOLVES from a chr
 * keystore, and that the resulting account is registered on the live Economy
 * Chain. Only the second needs a real registered key, so on a machine without
 * one - every CI runner - the first went untested too, hidden behind the same
 * assumption. These cases cover the resolution half with keystores they build
 * themselves: no network, no secret, nothing to skip.
 */
class FundingKeyResolutionTest {

    private val samplePriv = "00CED79962D1150BF844CACB76310D4746C4426558A7FD9C827B30203DACC4CE"

    private fun keystore(dir: Path, keyId: String, bare: Boolean): Map<String, String> {
        Files.writeString(dir.resolve("config"), "key.id = $keyId\n")
        if (bare) {
            Files.writeString(dir.resolve(keyId), "$samplePriv\n")
        } else {
            Files.writeString(dir.resolve("$keyId.secret"), "pubkey = 02AA\nprivkey = $samplePriv\n")
        }
        return mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString())
    }

    @Test
    fun resolvesTheKeyNamedByTheKeystoreConfig(@TempDir dir: Path) {
        val key = TestnetProvisioning.resolveFundingKey(keystore(dir, "container_deploy_bare", bare = true))
        assertNotNull(key, "config names key.id and the bare key file exists")
        assertEquals(samplePriv, key!!.privKey.joinToString("") { "%02X".format(it) })
        assertEquals("chr-keystore:container_deploy_bare", key.sourceLabel)
    }

    /** chr writes `<id>.secret` with a `privkey = ...` line; both layouts must resolve. */
    @Test
    fun resolvesTheSecretFileLayoutToo(@TempDir dir: Path) {
        val key = TestnetProvisioning.resolveFundingKey(keystore(dir, "testnet_key", bare = false))
        assertNotNull(key, "the .secret layout must resolve")
        assertEquals(samplePriv, key!!.privKey.joinToString("") { "%02X".format(it) })
    }

    /** An explicit key id overrides the config's default. */
    @Test
    fun anExplicitKeyIdWins(@TempDir dir: Path) {
        val env = keystore(dir, "container_deploy_bare", bare = true) +
            mapOf(TestnetProvisioning.FUNDING_KEY_ID_ENV to "other")
        Files.writeString(dir.resolve("other"), "$samplePriv\n")
        assertEquals("chr-keystore:other", TestnetProvisioning.resolveFundingKey(env)?.sourceLabel)
    }

    /** An explicit private key beats the keystore entirely - the CI/secret path. */
    @Test
    fun anExplicitPrivateKeyBeatsTheKeystore(@TempDir dir: Path) {
        val env = keystore(dir, "container_deploy_bare", bare = true) +
            mapOf(TestnetProvisioning.FUNDING_KEY_ENV to samplePriv)
        assertEquals("env:${TestnetProvisioning.FUNDING_KEY_ENV}", TestnetProvisioning.resolveFundingKey(env)?.sourceLabel)
    }

    @Test
    fun noKeystoreResolvesToNothingRatherThanThrowing(@TempDir dir: Path) {
        assertNull(TestnetProvisioning.resolveFundingKey(mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString())))
    }

    /** A config naming a key whose file is absent must not resolve a half-built key. */
    @Test
    fun aConfigNamingAMissingKeyResolvesToNothing(@TempDir dir: Path) {
        Files.writeString(dir.resolve("config"), "key.id = absent\n")
        assertNull(TestnetProvisioning.resolveFundingKey(mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString())))
    }

    /** key.id is used to build a path, so a traversal attempt must be refused. */
    @Test
    fun aTraversingKeyIdIsRefused(@TempDir dir: Path) {
        Files.writeString(dir.resolve("config"), "key.id = ../../etc/passwd\n")
        assertNull(TestnetProvisioning.resolveFundingKey(mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString())))
    }
}
