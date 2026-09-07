package org.chromia

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File

/**
 * THE ONLY PLACE IN THIS SUITE THAT MAY SKIP A TEST.
 *
 * A skip is a test that did not run, and this repo has been burned twice by the
 * difference between "green" and "green over everything it claims to cover": CI
 * once carried eight silent skips, and the merge gate carried a `--allow-skip`
 * flag that let a local run pass with a test CI would have refused. Both are
 * gone. What is left is a small, pinned set of tests that depend on a THIRD
 * PARTY the JVM cannot conjure - a PostgreSQL cluster, the `chr` CLI, the live
 * Chromia testnet - and those obey one contract:
 *
 *  - **enabled by default.** The gate refuses to run unless every enabling
 *    variable below is set (`scripts/loop-gate.mjs`, live-environment
 *    preflight), and CI sets all of them. The normal state is zero skips.
 *  - **broken != absent.** When the enabling variable IS set, a missing or
 *    broken resource FAILS. It never degrades back to a skip, because a skip
 *    with the environment enabled is exactly the silent hole this contract
 *    exists to close.
 *  - **pinned.** [AssumptionLedgerTest] enumerates the call sites below by
 *    scanning the test sources and refuses any raw `assumeTrue` elsewhere, so a
 *    new skip cannot appear without appearing in the ledger too.
 *
 * Nothing here may gate on our own code, or on anything this repository
 * controls. Those are conditions we can simply make true.
 */
object LiveEnv {

    /** The C.UTF-8 PostgreSQL that Postchain's collation gate needs. */
    const val DATABASE_URL = "CHROMIA_TEST_DATABASE_URL"

    /** The live Chromia testnet (read-only probes and unsigned/throwaway-key dry runs). */
    const val LIVE_PROVISIONING = "CHROMIA_LIVE_PROVISIONING_TESTS"

    /**
     * The real `chr` CLI. Unlike the two above, the resource is not addressed by
     * the variable - it is found on PATH - so this flag is the assertion that it
     * MUST be there: with it set, a missing or unlaunchable chr is a failure.
     * CI installs chr from apt.chromia.com and sets this.
     */
    const val REQUIRE_CHR = "CHROMIA_REQUIRE_CHR"

    private fun flag(key: String): Boolean =
        System.getenv(key)?.trim()?.equals("true", ignoreCase = true) == true

    /**
     * The database URL, or a skip when [DATABASE_URL] is unset. A URL that is
     * set but points at a dead cluster is NOT handled here: the test runs and
     * fails with the connection error, which is the honest outcome (the gate's
     * preflight names it as infrastructure before the suite starts).
     */
    fun requireDatabaseUrl(what: String): String {
        val url = System.getenv(DATABASE_URL)
        assumeTrue(
            !url.isNullOrBlank(),
            "needs $DATABASE_URL - $what. The gate refuses to run without it; set it in " +
                "local-test-env.properties (gitignored, one per worktree)."
        )
        return url!!
    }

    /** Skips only when the live flag is unset; with it set every failure is a failure. */
    fun requireLiveNetwork(what: String) {
        assumeTrue(
            flag(LIVE_PROVISIONING),
            "live testnet tests disabled (set $LIVE_PROVISIONING=true) - $what"
        )
    }

    /**
     * The absolute path of [name] on PATH, or null.
     *
     * PATHEXT is deliberately NOT honoured here: this is the bare-PATH question
     * ("would a plain `chr` lookup find an executable file?"), which is what the
     * launch-path probe needs to know. ChrLocator is the thing that knows about
     * .cmd shims, and the test drives it separately.
     */
    fun onPath(name: String): String? =
        System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
            .asSequence()
            .map { File(it, name) }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath

    /**
     * Requires the `chr` CLI. With [REQUIRE_CHR] set - CI, and every provisioned
     * lane - an absent chr FAILS; without it the probe skips. Returns the
     * resolved path when present.
     */
    fun requireChrOnPath(what: String): String? {
        val found = onPath("chr")
        if (flag(REQUIRE_CHR)) {
            assertNotNull(
                found,
                "$REQUIRE_CHR is set but `chr` is not an executable file on PATH - $what. " +
                    "Install it (apt.chromia.com on Linux, scoop on Windows) or unset $REQUIRE_CHR; " +
                    "do not let this degrade back into a skip."
            )
            return found
        }
        assumeTrue(found != null, "chr not on PATH - $what")
        return found
    }

    /**
     * Requires that a `chr` invocation actually SUCCEEDED. With [REQUIRE_CHR]
     * set, a chr that is present but cannot run is a failure - it used to skip,
     * which is the worst of the three outcomes: the box that most needed the
     * launch-path coverage (a scoop .cmd shim ProcessBuilder cannot start) was
     * exactly the box that silently opted out of it.
     */
    fun requireChrRan(ok: Boolean, detail: String) {
        if (flag(REQUIRE_CHR)) {
            assertTrue(
                ok,
                "$REQUIRE_CHR is set and `chr` is installed, but running it did not succeed: $detail"
            )
            return
        }
        assumeTrue(ok, "no working chr on this machine: $detail")
    }
}
