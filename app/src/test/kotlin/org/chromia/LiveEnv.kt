package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.config.ChromiaConfig
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
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
    // SECOND observation, of a query that is known to be cheap and known to
    // work: `{ totalRewardsPaid }` - the one field that kept answering through
    // the 2026-09-04 and 2026-09-07 explorer incidents (docs/UPSTREAM.md #3a
    // records it answering 200 while every dashboard field was INTERNAL_ERROR),
    // and it costs about a second.
    //
    //   canary ANSWERS            -> the explorer is up, and an upstream marker
    //                                in a live test is a PLAIN RED until a dated
    //                                ledger entry says that specific query is
    //                                broken;
    //   canary FAILED_SIGNATURE   -> the third party said something only it can
    //                                say, on a path that shares nothing with the
    //                                failing tool - the outage is measured;
    //   canary FAILED_OTHER       -> it refused with something else, which MAY
    //                                WELL BE OURS, and proves nothing.
    //
    // It runs AT MOST ONCE PER JVM (`by lazy`): the point is one measurement of
    // the third party per run, not one per test, and a canary that hammered the
    // explorer once per failing assertion would be part of the outage.
    //
    // WHY THE PATH IS ITS OWN (adversary round 19, section 4, the deep finding).
    // Until 2026-09-09 this probe built `ChromiaConfig()` and
    // `HttpClientService(config)` and called `config.explorerUrl` - the SAME
    // config, the SAME client and the SAME bounds as the tool that had just
    // failed. A fault on OUR side of the wire therefore satisfied the signature
    // guard and the canary guard at once: set `HttpTimeouts.requestTimeout`
    // small enough and ktor says `Request timeout has expired` - an allowlisted
    // signature, described in the allowlist itself as "OUR outbound hop" - for
    // the tool AND for the canary, and our own bug is reported as ChromaWay
    // being down. Of the four allowlisted signatures, `explorer-request-timeout`
    // and `explorer-http-5xx` are both reachable from our own code, so this was
    // not theoretical.
    //
    // The canary therefore shares NOTHING with the code under test except the
    // endpoint it is asking about:
    //
    //   - a plain `java.net.http.HttpClient` built here, not `HttpClientService`
    //     and not ktor, so nothing in our client stack is on both sides;
    //   - its OWN bounds ([CANARY_REQUEST_TIMEOUT], [CANARY_CONNECT_TIMEOUT]),
    //     not `ChromiaConfig.httpTimeouts`, so tightening the production timeout
    //     cannot make the canary fail;
    //   - its own request body and its own reading of the answer, so a bug in
    //     our query building or response parsing cannot fail both;
    //   - the URL from the same constant production reads
    //     (`ChromiaConfig().explorerUrl`), because a canary against a different
    //     endpoint would be evidence about a different service.
    //
    // AssumptionLedgerTest.ourOwnRequestTimeoutThroughTheRealSeamStaysARed measures
    // exactly that end to end: it tightens the production `requestTimeout` to
    // 1 ms through the real `ChromiaConfig`/`HttpTimeouts` seam, makes a real
    // live tool call, and proves the result is a RED and not a warning.

    /**
     * The canary's OWN request bound, deliberately NOT
     * `ChromiaConfig.httpTimeouts.requestTimeout`. Sharing that field is exactly
     * how one fault of ours satisfied both guards; 20 s is far above the ~1 s
     * this query has taken through every measured incident and far below the
     * production 60 s, so the two cannot be confused for one another.
     */
    val CANARY_REQUEST_TIMEOUT: Duration = Duration.ofSeconds(20)

    /** The canary's own connect bound, for the same reason. */
    val CANARY_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)

    /** The smallest real query the explorer must answer if it is up at all. */
    const val CANARY_QUERY = "query { totalRewardsPaid }"

    /** The explorer network the public service serves; it answers 4xx for others. */
    private const val CANARY_NETWORK = "mainnet"

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
     *
     * TWO OF THESE ARE REACHABLE FROM OUR OWN CODE, and round 19 measured it:
     * `explorer-request-timeout` is produced by tightening
     * `HttpTimeouts.requestTimeout`, and `explorer-http-5xx` by any 5xx our own
     * handling lets through. They stay on the list because they are also what a
     * real outage looks like - what makes them safe is the INDEPENDENT canary
     * (see [probeExplorer]): our own bound expiring cannot make a client that
     * does not use that bound fail too, so the two guards can no longer be
     * satisfied by one fault of ours. The other two are not reachable from here
     * at all: `INTERNAL_ERROR for <hex>` needs the explorer's own request id,
     * and `reCAPTCHA`'s rule id is not in `UPSTREAM_RULE_IDS`, so its prose is
     * never appended to a tool error.
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
            "independent canary($explorerUrl { totalRewardsPaid }, java.net.http, own " +
                "${CANARY_REQUEST_TIMEOUT.toSeconds()}s bound) = $state" +
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

    /**
     * The independent measurement. The URL is the production constant; every
     * other thing that carries the request belongs to this function.
     */
    private fun probeExplorer(): Canary {
        // The endpoint - and ONLY the endpoint - comes from the production
        // config, because the question is about that service.
        val explorerUrl = ChromiaConfig().explorerUrl
        val target = "$explorerUrl?network=$CANARY_NETWORK"
        val body = buildJsonObject { put("query", CANARY_QUERY) }.toString()
        val started = System.currentTimeMillis()
        val (state, said) = runCatching {
            val client = HttpClient.newBuilder()
                .connectTimeout(CANARY_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
            val request = HttpRequest.newBuilder(URI.create(target))
                .timeout(CANARY_REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            readCanaryAnswer(response.statusCode(), response.body().orEmpty())
        }.getOrElse { e -> readCanaryFailure(e, target) }
        val elapsed = System.currentTimeMillis() - started
        val canary = Canary(
            state = state,
            signature = if (state == CanaryState.FAILED_SIGNATURE) upstreamSignature(said) else null,
            explorerSaid = said,
            at = Instant.now().toString(),
            elapsedMs = elapsed,
            explorerUrl = explorerUrl
        )
        record(canary)
        return canary
    }

    /**
     * The canary's own reading of an HTTP answer - deliberately not
     * `GraphQLResponseParser`, so a bug in our parsing cannot fail the tool and
     * the canary together.
     */
    private fun readCanaryAnswer(status: Int, text: String): Pair<CanaryState, String> {
        if (status !in 200..299) return classify("HTTP $status from the explorer: ${text.take(400)}")
        val parsed = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: return CanaryState.FAILED_OTHER to "a non-JSON answer with HTTP $status: ${text.take(400)}"
        val errors = parsed["errors"]
        // "errors": null and "errors": [] mean no error, per the GraphQL spec.
        val listed = runCatching { errors?.jsonArray }.getOrNull().orEmpty()
        if (listed.isNotEmpty()) {
            val words = listed.mapNotNull { one ->
                runCatching { one.jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
            }.ifEmpty { listOf(errors.toString()) }
            return classify(words.joinToString("; ").take(600))
        }
        val paid = runCatching { parsed["data"]?.jsonObject?.get("totalRewardsPaid") }.getOrNull()
        // A 200 with no field is the explorer answering nothing, and it is
        // exactly the shape a swallowed failure wears.
        if (paid == null || paid is JsonNull) {
            return CanaryState.FAILED_OTHER to "200 with no data.totalRewardsPaid: ${text.take(400)}"
        }
        return CanaryState.ANSWERED to "totalRewardsPaid = $paid"
    }

    /** A transport failure of the canary's own client, classified honestly. */
    private fun readCanaryFailure(e: Throwable, target: String): Pair<CanaryState, String> {
        val chain = generateSequence(e) { it.cause }.toList()
        return when {
            // A CONNECT timeout is the network between us and them, and that
            // half is as likely to be ours as theirs.
            chain.any { it is HttpConnectTimeoutException } -> CanaryState.FAILED_OTHER to
                "the canary could not connect to $target within ${CANARY_CONNECT_TIMEOUT.toSeconds()} s " +
                    "(may well be ours): $e"

            // The canary's OWN request bound expiring is the third party not
            // answering the cheapest query it has in 20 s, measured on a client
            // that shares nothing with production. That is an outage, and the
            // words say whose bound expired so no reader confuses it with
            // ChromiaConfig.httpTimeouts.
            chain.any { it is HttpTimeoutException } -> CanaryState.FAILED_SIGNATURE to
                "Request timeout has expired - the canary's OWN " +
                    "${CANARY_REQUEST_TIMEOUT.toSeconds()} s java.net.http bound (NOT " +
                    "ChromiaConfig.httpTimeouts) expired against $target: $e"

            else -> CanaryState.FAILED_OTHER to
                "the canary's request to $target failed before any answer (may well be ours): $e"
        }
    }

    /** Third party's words -> a state. The allowlist decides; nothing else does. */
    private fun classify(said: String): Pair<CanaryState, String> =
        (if (upstreamSignature(said) != null) CanaryState.FAILED_SIGNATURE else CanaryState.FAILED_OTHER) to said

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
        put("network", CANARY_NETWORK)
        put("outcome", canary.state.name)
        // The three fields the GATE reads to know this measurement did not
        // share a path with the tool that failed. `independent` is a claim the
        // gate requires; it is worth something only because the evidence file
        // is bound to the failing test by digest (see upstreamOutage), so a
        // file that says it cannot also be a file anyone wrote.
        put("independent", true)
        put("client", "java.net.http.HttpClient")
        put("requestTimeoutMs", CANARY_REQUEST_TIMEOUT.toMillis())
        put("connectTimeoutMs", CANARY_CONNECT_TIMEOUT.toMillis())
        put("signature", canary.signature)
        put("explorerSaid", canary.explorerSaid.take(1000))
        put("elapsedMs", canary.elapsedMs)
        put("at", canary.at)
        put("pid", ProcessHandle.current().pid())
    }

    // =====================================================================
    // THE THIRD STATUS: an UPSTREAM WARNING
    // =====================================================================
    //
    // Or, 2026-09-08: a live test whose third party is PROVEN down is neither a
    // pass nor a red. It is an UPSTREAM WARNING - a third status the gate counts
    // and prints separately - and it can never be produced by our own code
    // failing.
    //
    // Three things are true of it at once, and all three matter:
    //
    //  1. **It is still a FAILURE in the JUnit XML.** Nothing here returns, no
    //     caller may continue, and the test's claim is NOT reported as verified.
    //     A warning that reported a pass would be round 18 section 4 again with
    //     better manners.
    //  2. **It must be PROVEN, twice over, ON TWO PATHS.** An allowlisted
    //     signature says the text is something only the third party can say; the
    //     INDEPENDENT canary refusing with that SAME signature, or a dated
    //     ledger entry, says the third party really is not serving it. A marker
    //     alone has never been enough - that belief is what made eight live
    //     INTERNAL_ERRORs look green - and since round 19 a second observation
    //     down the SAME wire is not enough either: two of the four allowlisted
    //     signatures are reachable from our own code, so the canary is measured
    //     on a path that shares nothing with the failing tool but the endpoint.
    //  3. **It leaves EVIDENCE on disk, BOUND TO THIS FAILURE.**
    //     `app/build/upstream/warnings/` holds one file per warning, and the
    //     gate refuses to count a warning whose file is missing or whose file
    //     carries no proof. The failure message carries `[evidence sha256:<hex>]`
    //     over that file's bytes and the gate recomputes it: the message is
    //     written by THIS process from THIS throwable, so a file someone else
    //     wrote cannot match it. The classification is therefore auditable after
    //     the fact by someone who was not there, and unforgeable by someone who
    //     was.
    //
    // The partial outage is the case this shape exists for. On 2026-09-08 the
    // canary answered in about a second while `blockchainAnalytics` took 15-41 s
    // per chain and had been dropping the connection at 60 s (docs/UPSTREAM.md
    // #11), and `allBlockchains(state:)` answered INTERNAL_ERROR every time
    // (#3b). "The explorer is up" and "this query is down" were both true, so a
    // canary-only guardrail would have called every one of those a plain red and
    // a signature-only guardrail would have waved through anything that smelled
    // upstream. The dated ledger entry is what separates them, and writing one
    // is a deliberate act: the debt is recorded, with a date, or there is no
    // warning.

    /** The tool, the query and the words - everything a reader needs later. */
    data class UpstreamEvidence(
        /** The GraphQL field or query the third party would not serve. */
        val query: String,
        /** The third party's OWN text, unedited. */
        val errorText: String,
        /**
         * The `docs/UPSTREAM.md` entry recording this query as broken, e.g.
         * "11" or "3b". Required whenever the canary answers: a partial outage
         * has to be written down before it can excuse anything.
         */
        val ledgerEntry: String? = null
    )

    /** Prefix of every proven-upstream failure message; the gate keys on it. */
    const val UPSTREAM_WARNING_PREFIX = "UPSTREAM WARNING (proven): "

    /**
     * Ends the calling test as an UPSTREAM WARNING when - and only when - the
     * failure is demonstrably the third party's. Anything less proven throws a
     * PLAIN AssertionError, which is an ordinary red: the two guardrails below
     * fail CLOSED.
     *
     * Never returns, so no caller can treat an outage as a reason to continue.
     */
    fun upstreamOutage(tool: String, evidence: UpstreamEvidence): Nothing {
        val (testClass, testMethod) = callingTest()

        // GUARDRAIL 1: the words are the third party's.
        val signature = upstreamSignature(evidence.errorText)
            ?: throw AssertionError(
                "$tool failed and nothing in the message is an allowlisted upstream signature, so " +
                    "the failure is OURS and stays a plain red. Allowlisted: " +
                    UPSTREAM_SIGNATURES.joinToString(", ") { it.first } +
                    ". The message was: ${evidence.errorText}"
            )

        // GUARDRAIL 2: the third party really is not serving it. Either the
        // INDEPENDENT canary refused with the SAME signature - the whole
        // explorer is down, measured on a path that shares nothing with the tool
        // that just failed - or this specific query is written down as broken,
        // with a date.
        //
        // Round 19 (a2) closed two doors here. `!canary.answered` counted
        // FAILED_OTHER - the state this file itself documents as "may well be
        // ours" - as proof; it does not any more. And a canary that failed with
        // a DIFFERENT allowlisted signature from the tool is two faults, not one
        // outage, so it does not agree either.
        val canary = explorerCanary()
        val canaryAgrees = canary.state == CanaryState.FAILED_SIGNATURE && canary.signature == signature
        val ledger = evidence.ledgerEntry?.let { datedLedgerEntry(it, evidence.query) }
        val proof = when {
            canaryAgrees -> "independent canary agrees on [$signature]: ${canary.summary()}"
            ledger != null -> "docs/UPSTREAM.md #${evidence.ledgerEntry}: $ledger"
            else -> throw AssertionError(
                "$tool failed with an upstream signature ($signature) but NOTHING PROVES the upstream " +
                    "is down, so this stays a plain red. " +
                    when (canary.state) {
                        CanaryState.ANSWERED ->
                            "The independent canary answered in this JVM (${canary.summary()}), which " +
                                "means the explorer is up"

                        CanaryState.FAILED_OTHER ->
                            "The independent canary failed with FAILED_OTHER (${canary.summary()}) - " +
                                "a refusal that may well be OURS, and a maybe is not a measurement"

                        CanaryState.FAILED_SIGNATURE ->
                            "The independent canary failed with [${canary.signature}] while $tool failed " +
                                "with [$signature] (${canary.summary()}) - two different faults are not " +
                                "one outage"
                    } +
                    (if (evidence.ledgerEntry == null) {
                        ", and no docs/UPSTREAM.md entry was named for `${evidence.query}`. A partial " +
                            "outage is excused by a DATED LEDGER ENTRY or not at all - write one, or " +
                            "fix this."
                    } else {
                        ", and docs/UPSTREAM.md #${evidence.ledgerEntry} does not record `" +
                            "${evidence.query}` with a date. An entry that does not name the query it " +
                            "excuses is not evidence about it."
                    }) +
                    " The third party said: ${evidence.errorText}"
            )
        }

        val at = Instant.now().toString()
        val warning = buildJsonObject {
            put("test", "$testClass.$testMethod")
            put("testClass", testClass)
            put("testMethod", testMethod)
            put("tool", tool)
            put("query", evidence.query)
            put("signature", signature)
            put("errorText", evidence.errorText.take(2000))
            put("canaryOutcome", canary.state.name)
            put("canary", canaryJson(canary))
            put("ledgerEntry", evidence.ledgerEntry)
            put("ledgerHeading", ledger)
            put("proof", proof)
            put("at", at)
        }
        // THE BINDING (adversary round 19, a2). The evidence file is an
        // ordinary file, and nothing in the repository stops anyone writing one;
        // what nobody but the failing test can write is the message the JUnit
        // reporter copies into the XML, because that is the throwable this
        // process threw. So hash the bytes just written and carry the digest in
        // that message. The gate recomputes it from the file and matches - a
        // hand-written file cannot match a message it did not produce.
        val digest = runCatching {
            val dir = upstreamDir.resolve("warnings")
            Files.createDirectories(dir)
            val bytes = warning.toString().toByteArray(StandardCharsets.UTF_8)
            Files.write(dir.resolve("$testClass.$testMethod.json"), bytes)
            sha256Hex(bytes)
        }.getOrElse { e ->
            // Without the file the gate counts this as an ordinary red, which is
            // the safe direction - but say why rather than letting the operator
            // wonder which of the two statuses they are looking at.
            throw AssertionError(
                "$tool failed upstream ($signature, $proof) but the warning file could not be " +
                    "written to ${upstreamDir.resolve("warnings")} (${e.message}), so this is reported " +
                    "as a plain red: an unauditable warning is not a warning. " +
                    "The third party said: ${evidence.errorText}"
            )
        }

        throw AssertionError(
            UPSTREAM_WARNING_PREFIX +
                "$tool could not be verified because the third party would not serve `" +
                "${evidence.query}`. This is a RED FOR THE UPSTREAM, not for us: nothing about $tool " +
                "was proven by this run, and the remedy is to fix or wait for the third party and " +
                "RE-RUN - never to pass, never to skip, never to record an answer. Proof: $proof. " +
                "Signature: $signature. Evidence: app/build/upstream/warnings/$testClass.$testMethod.json " +
                "[evidence sha256:$digest]. " +
                "The third party said: ${evidence.errorText.take(600)}"
        )
    }

    /**
     * The digest the gate recomputes. Lowercase hex of SHA-256 over the exact
     * bytes on disk; `scripts/gate-tally.mjs` reads the file as bytes for the
     * same reason.
     */
    internal fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }

    /**
     * The heading of `docs/UPSTREAM.md` entry [entry] when that entry both NAMES
     * [query] and carries a date, else null.
     *
     * Both conditions are the point. An undated entry cannot be aged out and
     * would excuse a query forever; an entry that does not name the query is
     * evidence about something else. The ledger is the debt, and a debt with no
     * date and no subject is not a debt.
     */
    fun datedLedgerEntry(entry: String, query: String): String? {
        val text = runCatching { RepoFiles.text("docs/UPSTREAM.md") }.getOrNull() ?: return null
        val heading = Regex("""(?m)^##\s+${Regex.escape(entry)}\.\s+(.*)$""").find(text) ?: return null
        val rest = text.substring(heading.range.last)
        val end = Regex("""(?m)^##\s""").find(rest)?.range?.first ?: rest.length
        val section = heading.groupValues[1] + rest.substring(0, end)
        if (!Regex("""\b20\d\d-\d\d-\d\d\b""").containsMatchIn(section)) return null
        if (!section.contains(query)) return null
        return heading.groupValues[1].trim()
    }

    /**
     * The `@Test` method this call came from, found by walking the stack and
     * asking each frame's method whether JUnit would run it.
     *
     * The name is not passed in on purpose: a caller that could name the test
     * could also name a different one, and the warning file is the audit record
     * the gate trusts. Reflection over the frames that are actually on the stack
     * cannot be talked into naming a test that is not running.
     */
    private fun callingTest(): Pair<String, String> {
        for (frame in Thread.currentThread().stackTrace) {
            val type = runCatching { Class.forName(frame.className) }.getOrNull() ?: continue
            val method = type.declaredMethods.firstOrNull { candidate ->
                candidate.name == frame.methodName &&
                    candidate.annotations.any {
                        it.annotationClass.qualifiedName?.startsWith("org.junit.jupiter.") == true &&
                            it.annotationClass.simpleName?.endsWith("Test") == true
                    }
            } ?: continue
            return type.simpleName to method.name
        }
        throw AssertionError(
            "LiveEnv.upstreamOutage was called from outside a @Test method, so there is no test to " +
                "attribute the warning to and no file the gate could match it against. An upstream " +
                "warning is a status a TEST carries; it is not a way for a helper to report weather."
        )
    }
}
