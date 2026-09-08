package org.chromia

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.chromia.data.client.HttpClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.data.queries.NetworkQueries
import org.chromia.domain.NetworkResult
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

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

    // =====================================================================
    // THE CANARY: is the explorer up AT ALL, measured in this JVM?
    // =====================================================================
    //
    // A live test that fails with an upstream-looking message has said nothing
    // about WHOSE failure it is. "INTERNAL_ERROR", "Request timeout has
    // expired" and an HTTP 503 are all things the explorer says - and all three
    // are also what a broken query, a wrong endpoint or a hung box would look
    // like from in here. Round 18 section 4 is the proof that guessing costs
    // more than measuring: `get_asset_top_holders` answered eight consecutive
    // live INTERNAL_ERRORs and the suite reported eight passes, because a
    // marker in the text was treated as evidence of an outage.
    //
    // So nothing in this file believes a marker on its own. The canary is a
    // SECOND, INDEPENDENT observation, made through the production HTTP client
    // against the same explorer, of a query that is known to be cheap and known
    // to work: `{ totalRewardsPaid }` - the one field that kept answering
    // through the 2026-09-04 and 2026-09-07 explorer incidents (docs/UPSTREAM.md
    // #3a records it answering 200 while every dashboard field was
    // INTERNAL_ERROR), and it costs about a second.
    //
    //   canary ANSWERS  -> the explorer is up, and an upstream marker in a live
    //                      test is a PLAIN RED until a dated ledger entry says
    //                      that specific query is broken;
    //   canary FAILS    -> the explorer is down for everything, and a live test
    //                      that hit it proved nothing about our code.
    //
    // It runs AT MOST ONCE PER JVM (`by lazy`): the point is one measurement of
    // the third party per run, not one per test, and a canary that hammered the
    // explorer once per failing assertion would be part of the outage.

    /**
     * THINGS ONLY THE THIRD PARTY CAN SAY, and the ONLY texts that may take part
     * in a downgrade. This is deliberately NARROWER than the `upstreamMarkers`
     * lists the live helpers use to word a failure: those lists exist to write a
     * better red, this one decides whether a red may be reported as an upstream
     * warning instead, so every entry has to be a string our own code cannot
     * produce by being wrong.
     *
     * It mirrors `scripts/upstream-classifier.mjs` (the e2e sweep's allowlist),
     * minus everything that could be ours:
     *
     *  - `INTERNAL_ERROR for <uuid>` carries the EXPLORER'S OWN request id. A
     *    malformed query of ours is a `Validation error`, never this.
     *  - `Request timeout has expired` is ktor's text for OUR outbound hop
     *    exceeding `ChromiaConfig.httpTimeouts.requestTimeout` - the third party
     *    not answering in 60 s.
     *  - `reCAPTCHA` is a bot gate no API client can pass (docs/UPSTREAM.md #7a).
     *  - a 5xx is the third party's own server-side failure.
     *
     * DELIBERATELY ABSENT, though the sweep and the helpers both mention them:
     * HTTP 4xx (a 400 is normally OUR malformed query - docs/UPSTREAM.md #9 is
     * an explorer-side 400 and it is handled by a dated ledger entry, not by a
     * blanket signature), `Connection refused` / `Connection reset` (a closed
     * local port is how several tests assert offline behaviour on purpose), and
     * the bare word `timeout` (ours hangs look like that too).
     *
     * Scoping to the explorer is STRUCTURAL rather than textual: the only
     * callers of [upstreamOutage] are the explorer live helpers, which
     * [AssumptionLedgerTest] pins, and both guardrails below are explorer
     * measurements.
     */
    val UPSTREAM_SIGNATURES: List<Pair<String, Regex>> = listOf(
        "explorer-graphql-internal-error" to
            Regex("""\bINTERNAL_ERROR\b\s+for\s+[0-9A-Fa-f][0-9A-Fa-f-]{7,}"""),
        "explorer-request-timeout" to
            Regex("""Request timeout has expired""", RegexOption.IGNORE_CASE),
        "explorer-recaptcha" to
            Regex("""reCAPTCHA""", RegexOption.IGNORE_CASE),
        "explorer-http-5xx" to
            Regex("""\bHTTP 5\d\d\b|\bBad Gateway\b|\bService Unavailable\b|\bGateway Time-?out\b""",
                RegexOption.IGNORE_CASE)
    )

    /** The matched signature name for an error text, or null - which means OURS. */
    fun upstreamSignature(text: String?): String? {
        val t = text ?: return null
        return UPSTREAM_SIGNATURES.firstOrNull { (_, re) -> re.containsMatchIn(t) }?.first
    }

    enum class CanaryState {
        /** The explorer served the known-good query. */
        ANSWERED,

        /** It refused, with something only the third party can say. */
        FAILED_SIGNATURE,

        /** It refused with something else - which may well be ours. */
        FAILED_OTHER
    }

    data class Canary(
        val state: CanaryState,
        /** The allowlisted signature the refusal matched, or null. */
        val signature: String?,
        /** The explorer's OWN text - never our paraphrase of it. */
        val explorerSaid: String,
        val at: String,
        val elapsedMs: Long,
        val explorerUrl: String
    ) {
        val answered: Boolean get() = state == CanaryState.ANSWERED

        /** One line, for a failure message that has to fit in a JUnit XML attribute. */
        fun summary(): String =
            "canary($explorerUrl { totalRewardsPaid }) = $state" +
                (signature?.let { " [$it]" } ?: "") +
                " in ${elapsedMs}ms at $at: ${explorerSaid.take(160)}"
    }

    /** Everything this file writes for the gate to read. */
    val upstreamDir: Path get() = RepoFiles.root.resolve("app/build/upstream")

    private val canary: Canary by lazy { probeExplorer() }

    /**
     * The one explorer measurement of this JVM. Records itself to
     * `app/build/upstream/canary.json` so the gate - and a person reading a red
     * run an hour later - can see the third party's state at the moment the
     * suite ran, rather than re-probing an explorer that has since recovered.
     */
    fun explorerCanary(): Canary = canary

    private fun probeExplorer(): Canary {
        val config = ChromiaConfig()
        val started = System.currentTimeMillis()
        // The PRODUCTION client, the production query, the production endpoint.
        // A canary that used a different HTTP stack from the tools would be
        // measuring a different network path than the one that just failed.
        val result = runBlocking {
            HttpClientService(config).executeGraphQLQuery(
                NetworkQueries.getTotalRewardsPaid(),
                "mainnet"
            )
        }
        val elapsed = System.currentTimeMillis() - started
        val at = Instant.now().toString()
        val canary = when (result) {
            is NetworkResult.Success -> {
                val paid = result.data["data"]?.jsonObject?.get("totalRewardsPaid")
                if (paid == null) {
                    // A 200 with no field is the explorer answering nothing, and
                    // it is exactly the shape a swallowed failure wears.
                    Canary(
                        CanaryState.FAILED_OTHER, null,
                        "200 with no data.totalRewardsPaid: ${result.data}", at, elapsed, config.explorerUrl
                    )
                } else {
                    Canary(CanaryState.ANSWERED, null, "totalRewardsPaid = $paid", at, elapsed, config.explorerUrl)
                }
            }

            is NetworkResult.Error -> {
                val signature = upstreamSignature(result.message)
                Canary(
                    if (signature != null) CanaryState.FAILED_SIGNATURE else CanaryState.FAILED_OTHER,
                    signature, result.message, at, elapsed, config.explorerUrl
                )
            }
        }
        record(canary)
        return canary
    }

    private fun record(canary: Canary) {
        runCatching {
            Files.createDirectories(upstreamDir)
            Files.writeString(upstreamDir.resolve("canary.json"), canaryJson(canary).toString())
        }
        // A canary that cannot write its file is not a reason to fail a test:
        // the measurement still happened and is still returned. The gate says
        // so loudly when the file is missing, which is where it matters.
    }

    internal fun canaryJson(canary: Canary): JsonObject = buildJsonObject {
        put("query", "totalRewardsPaid")
        put("explorerUrl", canary.explorerUrl)
        put("network", "mainnet")
        put("outcome", canary.state.name)
        put("signature", canary.signature)
        put("explorerSaid", canary.explorerSaid.take(1000))
        put("elapsedMs", canary.elapsedMs)
        put("at", canary.at)
        put("pid", ProcessHandle.current().pid())
    }
}
