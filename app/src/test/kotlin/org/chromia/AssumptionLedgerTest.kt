package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.tools.FilterBlockchainsStrategy
import org.chromia.tools.callToolRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds

/**
 * THE ASSUMPTION LEDGER: every test in this suite that can decline to run, why,
 * and what enables it.
 *
 * An external review made the point that this repo's zero-skip guarantee had two
 * holes. One was `--allow-skip` in the merge gate (removed). The other is
 * quieter: eleven `assumeTrue` sites, each of which turns into a silent
 * non-result whenever its variable happens to be unset - and a variable that
 * lives in a gitignored file is unset by default in every fresh worktree. A
 * "green 1568" that is really "green 1557 and eleven abstentions" is precisely
 * the plausible-but-false green the project refuses.
 *
 * So the assumptions are enumerated here, mechanically, and three properties are
 * pinned:
 *
 *  1. **They are the only ones.** The scan below reads the test sources; a raw
 *     `assumeTrue` / `Assumptions.` anywhere but [LiveEnv] fails this test. A
 *     new skip cannot be introduced without appearing in the ledger.
 *  2. **Every one is a THIRD PARTY.** Each row names an external resource - a
 *     PostgreSQL cluster, the `chr` CLI, the live Chromia testnet - and the
 *     [Resource] enum is closed. Nothing may abstain on our own code or on a
 *     condition this repository controls: those we can simply make true.
 *  3. **Every one is enabled where it matters.** CI's workflow must set each
 *     enabling variable (asserted against `.github/workflows/ci.yml` below), and
 *     `scripts/loop-gate.mjs` must refuse to start without it (asserted too), so
 *     the enabled state is the default rather than a thing to remember.
 *
 * The fourth property - broken must FAIL, not skip - is structural rather than
 * assertable from here: [LiveEnv] escalates to an assertion whenever the
 * enabling variable is set, so a chr that exists but cannot launch, or a
 * database URL that points at a dead cluster, reds the suite.
 */
class AssumptionLedgerTest {

    enum class Resource(val envVar: String, val what: String) {
        POSTGRES(
            LiveEnv.DATABASE_URL,
            "an external C.UTF-8 PostgreSQL cluster (Postchain's collation gate needs C ordering AND " +
                "Unicode case mapping at once, which native Windows PostgreSQL cannot provide)"
        ),
        CHR_CLI(
            LiveEnv.REQUIRE_CHR,
            "the third-party `chr` command-line tool, installed on the machine (apt.chromia.com / scoop)"
        ),
        CHROMIA_TESTNET(
            LiveEnv.LIVE_PROVISIONING,
            "the live Chromia testnet - Economy Chain queries and the faucet, over the public network"
        )
    }

    data class Row(
        val file: String,
        val function: String,
        val call: String,
        val resource: Resource,
        val reason: String
    )

    /**
     * The ledger. Order is irrelevant; the SET must match what the scan finds.
     */
    private val ledger = listOf(
        Row(
            "AbandonedSessionReaperTest.kt", "anIdleInTransactionSessionIsTerminatedAndNamedAnIdleConnectionIsNot",
            "requireDatabaseUrl", Resource.POSTGRES,
            "opens a real second connection, leaves it idle in transaction and requires pg_terminate_backend " +
                "to end it: what a killed test JVM leaves behind can only be reproduced on the real server."
        ),
        Row(
            "AbandonedSessionReaperTest.kt", "aDatabaseWithNothingAbandonedReapsNothing",
            "requireDatabaseUrl", Resource.POSTGRES,
            "reads pg_stat_activity on the real server and requires the reaper to name nothing on a clean " +
                "database; the negative half of the same contract."
        ),
        Row(
            "DatabaseSessionGuardsTest.kt", "theServerConfirmsTheSessionCarriesTheGuards",
            "requireDatabaseUrl", Resource.POSTGRES,
            "connects with the guarded URL and asks the server for the session's own " +
                "idle_in_transaction_session_timeout: only PostgreSQL can say what the options= token became."
        ),
        Row(
            "DappBuildToolsTest.kt", "chrVersionLiveProbe", "requireChrOnPath", Resource.CHR_CLI,
            "launches the installed chr the way the server does, through ChrLocator - the launcher agents " +
                "depend on. With CHROMIA_REQUIRE_CHR set, an absent chr fails; a chr that is present but " +
                "cannot be started already threw an AssertionError before this contract existed."
        ),
        Row(
            "InputAbuseAndLifecycleRegressionTest.kt", "ft4TestsWithoutTheAdminArgsGetTheModuleArgsHint",
            "requireDatabaseUrl", Resource.POSTGRES,
            "compiles and RUNS the ft4 scaffold's Rell tests to prove the missing-module_args hint names " +
                "the modules; there is no way to produce a real GTX-module failure without a database."
        ),
        Row(
            "InputAbuseAndLifecycleRegressionTest.kt", "missingModuleArgsAreNamedFromTheCompiledApp",
            "requireDatabaseUrl", Resource.POSTGRES,
            "the same note, derived from the COMPILED stablecoin app - the list comes out of the compiler, " +
                "so the app has to be built against a real database."
        ),
        Row(
            "LocalChainIntegrationTest.kt", "chainStartsAnswersQueriesAndStops", "requireDatabaseUrl",
            Resource.POSTGRES,
            "starts a REAL Postchain node and drives its REST surface end to end - compile, boot, query, " +
                "sign, post, await a block, shut down. Nothing in the JVM can stand in for the database it " +
                "writes those blocks to."
        ),
        Row(
            "RunRellTestsToolTest.kt", "concurrentDatabaseBackedRunsDoNotCollide", "requireDatabaseUrl",
            Resource.POSTGRES,
            "the regression is two concurrent runs sharing ONE schema ('Missing metadata entities for " +
                "existing tables'); it cannot be reproduced without the schema."
        ),
        Row(
            "ScaffoldToGreenTestsFirstRunTest.kt", "runFirstHonestPass", "requireDatabaseUrl",
            Resource.POSTGRES,
            "runs every scaffold template's own test suite as real transactions on a real chain - the " +
                "'an agent's first honest pass is green' claim is exactly the claim a simulation cannot make."
        ),
        Row(
            "LocalChainRestBridgeTest.kt", "startTheRealChainOnce", "requireDatabaseUrl", Resource.POSTGRES,
            "the REST facade is answered by the production EngineGateway over a REAL running node's " +
                "BlockchainEngine; with no database there is no engine to answer, and the anonymous " +
                "ChainGateway that used to answer instead is exactly what this file stopped doing."
        ),
        Row(
            "LocalChainToolTest.kt", "upIsIdempotentForIdenticalSourcesAndRestartsOnChange",
            "requireDatabaseUrl", Resource.POSTGRES,
            "reuse-versus-restart is a claim about the IDENTITY of a real Running registration across two " +
                "real node starts; a starter override could report either and the test could not tell."
        ),
        Row(
            "RealTxPosterTest.kt", "startTheRealChainOnce", "requireDatabaseUrl", Resource.POSTGRES,
            "the poster's whole pipeline - GTX build, merkle digest, secp256k1 signature, REST post, status " +
                "polling, confirmation in a block - is exercised against a real node; the scripted http4k " +
                "handler it replaced could only replay what the test had written."
        ),
        Row(
            "AuditRound4RegressionTest.kt", "interruptedDbRunDefersPermitReleaseUntilTheRealRunnerFinishes",
            "requireDatabaseUrl", Resource.POSTGRES,
            "audit round 4 F5 is about a real run_rell_tests run STILL OWNING the shared test database " +
                "when its caller is interrupted; the lambda that used to stand in for the runner proved " +
                "only that a latch blocks, and a real run needs the schema it holds."
        ),
        Row(
            "ConcurrencyLensRegressionTest.kt", "ttlTaskAlreadyWaitingForTheLockMustNotStopAJustRefreshedChain",
            "requireDatabaseUrl", Resource.POSTGRES,
            "the race is between a real TTL task and a real local_chain_up over a REAL running node; with " +
                "the starter override that used to fake the chain, the thing being raced over did not exist."
        ),
        Row(
            "InputAbuseAndLifecycleRegressionTest.kt", "failedRestartSaysThePreviousChainIsGone",
            "requireDatabaseUrl", Resource.POSTGRES,
            "the claim is that a FAILED restart really took a running chain down, so there has to be a real " +
                "chain to lose first - and the failure is a real refused PostgreSQL connection, not an " +
                "invented one."
        ),
        Row(
            "ProvisioningToolsTest.kt", "realChrResolvesWithoutOverrideAndRuns", "requireChrRan",
            Resource.CHR_CLI,
            "resolves chr WITHOUT the CHROMIA_CHR_BIN override and actually runs it. It used to skip when " +
                "chr was present but unlaunchable, which silently excused the exact box (a scoop .cmd shim) " +
                "the check exists for; with CHROMIA_REQUIRE_CHR set that is now a failure."
        ),
        Row(
            "LiveChromia.kt", "requireLive", "requireLiveNetwork", Resource.CHROMIA_TESTNET,
            "the single gate for the whole live surface. Every test that used to answer from a fixture - the " +
                "explorer's GraphQL API, a chain node behind postchain-client, the Economy Chain - now asks the " +
                "real network through this one call, so there is one row here instead of one per test. Nothing " +
                "behind it signs or spends; it is all read-only queries against public infrastructure, and the " +
                "only thing the JVM cannot conjure is the network itself."
        ),
        Row(
            "TestnetProvisioningLiveTest.kt", "liveDryRunPricesLeaseAndResolvesEverythingWithoutSpending",
            "requireLiveNetwork", Resource.CHROMIA_TESTNET,
            "prices a container lease against the live Economy Chain with a throwaway keypair; nothing is " +
                "signed and nothing is spent, but the price and the resolution are the live chain's."
        ),
        Row(
            "TestnetProvisioningLiveTest.kt", "liveClaimDryRunReportsFaucetTerms", "requireLiveNetwork",
            Resource.CHROMIA_TESTNET,
            "reads the live faucet's terms; a stale copy of them in a fixture is how the tool would start " +
                "telling agents to do something the faucet no longer accepts."
        ),
        Row(
            "TestnetProvisioningLiveTest.kt", "liveKnownAccountIsRegisteredOnTheEconomyChain",
            "requireLiveNetwork", Resource.CHROMIA_TESTNET,
            "the ONLY positive registration assertion in the suite: get_balance for a public, " +
                "known-registered account id. No key material is involved."
        ),
        Row(
            "TestnetProvisioningLiveTest.kt", "liveSignedTransactionIsAcceptedOnWireAndRejectedByFt4Auth",
            "requireLiveNetwork", Resource.CHROMIA_TESTNET,
            "posts a real signed transaction from a throwaway key and requires the chain to REJECT it - " +
                "proof of the whole signing pipeline against the live node with no possibility of spending."
        )
    )

    // ---- 1. these are the only assumptions in the suite ---------------------

    private val rawAssumption = Regex("""\b(assumeTrue|assumeFalse|assumingThat|Assumptions\.)""")
    private val liveEnvCall = Regex("""LiveEnv\.(requireDatabaseUrl|requireLiveNetwork|requireChrOnPath|requireChrRan)\s*\(""")
    private val functionDeclaration = Regex("""^\s*(?:private\s+|internal\s+|suspend\s+)*fun\s+([A-Za-z0-9_]+)""")

    @Test
    fun onlyLiveEnvMayDeclineToRunATest() {
        val offenders = RepoFiles.testSources()
            .filter { RepoFiles.className(it) != "LiveEnv" && RepoFiles.className(it) != "AssumptionLedgerTest" }
            .flatMap { file ->
                file.toFile().readLines().withIndex()
                    .filter { (_, line) -> rawAssumption.containsMatchIn(line) }
                    .map { (i, line) -> "${file.fileName}:${i + 1}: ${line.trim()}" }
            }
        assertTrue(
            offenders.isEmpty(),
            "a skip is a test that did not run, and only LiveEnv may cause one - it escalates to a " +
                "FAILURE when the environment is enabled, which a raw assumeTrue does not:\n  " +
                offenders.joinToString("\n  ")
        )
    }

    @Test
    fun everyAssumptionSiteIsInTheLedgerAndEveryLedgerRowExists() {
        val found = mutableSetOf<Triple<String, String, String>>()
        for (file in RepoFiles.testSources()) {
            if (RepoFiles.className(file) == "LiveEnv") continue
            var currentFunction = "<top level>"
            for (line in file.toFile().readLines()) {
                functionDeclaration.find(line)?.let { currentFunction = it.groupValues[1] }
                liveEnvCall.find(line)?.let {
                    found += Triple(file.fileName.toString(), currentFunction, it.groupValues[1])
                }
            }
        }
        val pinned = ledger.map { Triple(it.file, it.function, it.call) }.toSet()
        assertEquals(
            pinned, found,
            "the assumption ledger and the test sources disagree. Every test that can decline to run must " +
                "be listed here with the third-party resource that gates it and the reason it cannot be " +
                "made unconditional; a new one appearing unlisted is how the zero-skip claim rots."
        )
        assertEquals(ledger.size, ledger.map { it.file to it.function }.distinct().size, "duplicate ledger rows")
    }

    // ---- 2. every assumption is on a third party ----------------------------

    @Test
    fun everyAssumptionIsOnAThirdPartyAndCarriesItsReason() {
        ledger.forEach { row ->
            // The Resource enum is the closed list of things outside this repo.
            // There is deliberately no value for "our own code" or "a condition
            // the repo controls": such a condition is not gated, it is fixed.
            assertTrue(row.resource in Resource.entries, "${row.function}: unknown resource")
            assertTrue(
                row.reason.length > 60,
                "${row.function}: the ledger's reason must say why the resource cannot be replaced by " +
                    "something in-process, not just name it: '${row.reason}'"
            )
        }
        assertEquals(
            setOf(Resource.POSTGRES, Resource.CHR_CLI, Resource.CHROMIA_TESTNET),
            ledger.map { it.resource }.toSet(),
            "the three external dependencies are the only reasons a test here may abstain"
        )
    }

    // ---- 3. enabled in CI, and required by the gate -------------------------

    @Test
    fun ciEnablesEveryEnablingVariable() {
        val ci = RepoFiles.text(".github/workflows/ci.yml")
        Resource.entries.forEach { resource ->
            assertTrue(
                Regex("""^\s*${Regex.escape(resource.envVar)}\s*:""", RegexOption.MULTILINE).containsMatchIn(ci),
                "${resource.envVar} is not set in .github/workflows/ci.yml, so ${resource.what} is not " +
                    "enabled in CI and the tests that need it skip there - which is the state this repo " +
                    "found itself in on 2026-09-02 with eight silent skips."
            )
        }
        assertTrue(
            ci.contains("ALLOWED = set()"),
            "CI's skip check must keep an EMPTY allowlist - a named exception there is the same bypass " +
                "--allow-skip was in the local gate"
        )
    }

    @Test
    fun theMergeGateRefusesToRunWithoutTheseVariables() {
        val gate = RepoFiles.text("scripts/loop-gate.mjs")
        Resource.entries.forEach { resource ->
            assertTrue(
                gate.contains(resource.envVar),
                "scripts/loop-gate.mjs must refuse to run unless ${resource.envVar} is set: a local gate " +
                    "that starts with the live environment disabled certifies a suite that quietly did " +
                    "less than the one CI runs"
            )
        }
        assertTrue(
            gate.contains("--allow-skip was removed"),
            "the gate must keep refusing --allow-skip explicitly rather than ignoring it - a stale " +
                "invocation that silently loses its allowlist should say so"
        )
        // The tally - and with it the skip refusal - moved to
        // scripts/gate-tally.mjs on 2026-09-08, so that the merge gate and CI
        // run ONE implementation rather than two that can drift. The pin
        // follows the implementation to its new home, and pins the import as
        // well: a move that loses its caller is a deletion with extra steps.
        assertTrue(
            gate.contains("from './gate-tally.mjs'"),
            "scripts/loop-gate.mjs must import the shared tally. Two gates with different " +
                "definitions of green is one gate and one bypass - which is what --allow-skip was."
        )
        val tallyScript = RepoFiles.text("scripts/gate-tally.mjs")
        assertTrue(
            Regex("""skippedNames\.length\)\s*\{""").containsMatchIn(tallyScript) ||
                tallyScript.contains("there is no allowlist"),
            "the gate must fail on ANY skip"
        )
        // And the third status must not have become a way to stop counting one:
        // a skip is red in the same file that decides an upstream warning.
        assertTrue(
            tallyScript.contains("every skip") || tallyScript.contains("every skip, every failure"),
            "gate-tally.mjs must say, where it classifies, that a skip is red - the third status " +
                "is for a proven THIRD-PARTY outage and never for a test that did not run"
        )
    }

    // ---- 4. no test may end early without having asserted anything ---------

    /**
     * The shape this section exists for, found in the tree on 2026-09-07:
     *
     *     val doc = ...
     *     if (!doc.exists()) {
     *         System.err.println("live nested fetch skipped (path missing)")
     *         return
     *     }
     *
     * That is strictly worse than a skip. A skip is COUNTED - the gate fails on
     * any non-zero skip count, CI's allowlist is empty, and [LiveEnv] escalates
     * a skip to a failure the moment the environment is enabled. An early
     * `return` is counted as a PASS: JUnit sees a method that completed
     * normally, the XML says 1 test ran, and the claim in the method name is
     * reported as verified by a body that verified nothing. Two of these were
     * in the suite (GitRepositoryFetcherLiveTest, SitemapDocsFetcherTest); both
     * now assert instead, and the scans below stop a third appearing.
     */
    private val testAnnotation = Regex("""^\s*@(Test|ParameterizedTest|RepeatedTest|TestFactory)\b""")

    /**
     * A return that LEAVES THE TEST. A bare `return`, or `return@runBlocking`
     * for the `= runBlocking { }` bodies. Labelled returns into a collection
     * builder (`return@forEach`) are continues, not exits, and are not matched.
     */
    private val testExitingReturn = Regex("""(?<![\w.@])return(@runBlocking)?\s*(?:\}\s*)?$""")
    private val announcement = Regex("""\b(println|System\.(out|err)\.print(ln)?|logger\.|log\.(info|warn|debug|error))""")
    private val anAssertion = Regex("""\b(assert[A-Z]\w*|assertThrows|fail)\s*[(<]""")

    /** Every (file, testName, lineNumber, previousCodeLines) for a test-exiting return. */
    private fun testExitingReturns(): List<TestReturn> {
        val found = mutableListOf<TestReturn>()
        for (file in RepoFiles.testSources()) {
            if (RepoFiles.className(file) == "AssumptionLedgerTest") continue
            val lines = stripKotlinComments(file.toFile().readText()).lines()
            var pendingTest = false
            var testName: String? = null
            var entryDepth = 0
            var depth = 0
            val recent = ArrayDeque<String>()
            lines.forEachIndexed { index, line ->
                if (testAnnotation.containsMatchIn(line)) pendingTest = true
                val declaration = functionDeclaration.find(line)
                if (declaration != null && pendingTest) {
                    testName = declaration.groupValues[1]
                    entryDepth = depth
                    pendingTest = false
                    recent.clear()
                }
                val before = depth
                depth += line.count { it == '{' } - line.count { it == '}' }
                val name = testName
                if (name != null) {
                    if (testExitingReturn.containsMatchIn(line)) {
                        // The window INCLUDES the current line: `val x = assertThing(..) ?: return`
                        // asserted before it left, and `println("skipped"); return` announced on the
                        // way out. Both judgements are about the line the return is on.
                        found += TestReturn(
                            file.fileName.toString(), name, index + 1, line.trim(),
                            recent.toList() + line.trim()
                        )
                    }
                    if (line.isNotBlank()) {
                        recent.addLast(line.trim())
                        while (recent.size > 5) recent.removeFirst()
                    }
                    if (before > entryDepth && depth <= entryDepth) testName = null
                }
            }
        }
        return found
    }

    data class TestReturn(
        val file: String,
        val test: String,
        val line: Int,
        val text: String,
        val precedingCode: List<String>
    )

    /** The same comment stripper the double scan uses; a `return` in prose is not code. */
    private fun stripKotlinComments(source: String): String {
        val withoutBlocks = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
            .replace(source) { m -> "\n".repeat(m.value.count { it == '\n' }) }
        return withoutBlocks.lineSequence().joinToString("\n") { line ->
            val guarded = line.replace("://", "\u0000\u0000\u0000")
            val at = guarded.indexOf("//")
            if (at >= 0) guarded.substring(0, at).replace("\u0000\u0000\u0000", "://") else line
        }
    }

    @Test
    fun noTestAnnouncesThatItIsNotTestingAndThenPasses() {
        val offenders = testExitingReturns().filter { site ->
            site.precedingCode.takeLast(3).any { announcement.containsMatchIn(it) }
        }
        assertTrue(
            offenders.isEmpty(),
            "a test that logs why it is not testing and then returns reports a PASS for work it did " +
                "not do, and unlike a skip nothing counts it - not the gate, not CI's skip check, not " +
                "the XML. Assert instead: a third party that is missing or has moved is a red with " +
                "retry as the remedy.\n  " +
                offenders.joinToString("\n  ") { "${it.file}:${it.line} [${it.test}] ${it.text}" }
        )
    }

    @Test
    fun anEarlyReturnFromATestMustFollowAnAssertion() {
        val offenders = testExitingReturns().filter { site ->
            site.precedingCode.none { anAssertion.containsMatchIn(it) }
        }
        assertTrue(
            offenders.isEmpty(),
            "an early `return` out of a test body is only honest once the test has actually asserted " +
                "something - otherwise the method name makes a claim and the body proves nothing. " +
                "Either assert the condition you were about to return on, or delete the claim:\n  " +
                offenders.joinToString("\n  ") { "${it.file}:${it.line} [${it.test}] ${it.text}" }
        )
    }

    // ---- 5. a live test may not leave on an upstream refusal ---------------

    /**
     * The shape adversary round 18 found (section 4), and the reason this
     * section exists:
     *
     *     val holders = assertLiveExplorerTool("get_asset_top_holders", result, "getAssetTopHolders")
     *         ?: return@runBlocking
     *
     * The helper returned NULL whenever the tool's error text matched an
     * upstream marker, and `internal_error` was the first entry in that list.
     * `get_asset_top_holders` answered eight consecutive live INTERNAL_ERRORs
     * across three real asset ids and the example id in its own description -
     * and `liveGetAssetTopHoldersAnswersForARealAsset` PASSED through every one
     * of them, because the body left before it asserted anything. That is the
     * same silent non-result section 4 above forbids, wearing the clothes of an
     * upstream courtesy.
     *
     * README "Testing Layers" says an upstream outage makes this suite RED and
     * that the remedy is to re-run. So there is no "skip on upstream" path in a
     * unit test at all: the live helpers FAIL with the third party's own words
     * and the retry advice, and they cannot hand back a null for a caller to
     * leave on. Only the e2e sweep may tag WARN-UPSTREAM, under its own
     * guardrail (all-live-warn is a FAIL, and so is more than
     * SWEEP_MAX_UPSTREAM_WARNS of them).
     *
     * Two scans keep it that way.
     */
    private val liveAssertionCall = Regex("""\bassertLive[A-Za-z0-9_]*\s*\(""")

    /** A `?:` that leaves, and a `return` standing on its own line. */
    private val leavesOnTheValue = Regex("""\?:\s*return\b|^\s*return(@\w+)?\s*$""")

    /** A live helper declared to return something nullable, on one line or two. */
    private val nullableHelperOnOneLine =
        Regex("""\bfun\s+assertLive\w*\s*\([^)]*\)\s*:\s*[\w.<>, ]+\?""")
    private val nullableHelperContinued = Regex("""^\s*\)\s*:\s*[\w.<>, ]+\?\s*[={]""")
    private val returnsNull = Regex("""\breturn\s+null\b""")

    /** Every live-helper CALL site, with the three lines that follow it. */
    private fun liveAssertionWindows(): List<Pair<String, List<Pair<Int, String>>>> {
        val windows = mutableListOf<Pair<String, List<Pair<Int, String>>>>()
        for (file in RepoFiles.testSources()) {
            if (RepoFiles.className(file) == "AssumptionLedgerTest") continue
            val lines = stripKotlinComments(file.toFile().readText()).lines()
            lines.forEachIndexed { index, line ->
                if (!liveAssertionCall.containsMatchIn(line)) return@forEachIndexed
                // The declaration of a helper is not a call of one.
                if (functionDeclaration.containsMatchIn(line)) return@forEachIndexed
                windows += file.fileName.toString() to
                    (index..minOf(index + 3, lines.lastIndex)).map { (it + 1) to lines[it] }
            }
        }
        return windows
    }

    @Test
    fun noLiveAssertionIsFollowedByAnEarlyReturn() {
        val offenders = liveAssertionWindows().flatMap { (file, window) ->
            window.filter { (_, line) -> leavesOnTheValue.containsMatchIn(line) }
                .map { (number, line) -> "$file:$number: ${line.trim()}" }
        }
        assertTrue(
            offenders.isEmpty(),
            "a live test left on the value a live assertion handed back. An upstream refusal is a " +
                "RED in a unit test - the remedy is to fix or wait for the third party and re-run - " +
                "and a body that returns instead reports a PASS for a call that answered nothing. " +
                "That is how get_asset_top_holders stayed green through eight consecutive live " +
                "INTERNAL_ERRORs. Make the helper fail; then there is nothing to return on:\n  " +
                offenders.joinToString("\n  ")
        )
    }

    @Test
    fun noLiveAssertionHelperCanHandBackNull() {
        val offenders = mutableListOf<String>()
        for (file in RepoFiles.testSources()) {
            if (RepoFiles.className(file) == "AssumptionLedgerTest") continue
            val lines = stripKotlinComments(file.toFile().readText()).lines()
            var helper: String? = null
            var depth = 0
            var entered = false
            lines.forEachIndexed { index, line ->
                fun offend() { offenders += "${file.fileName}:${index + 1} [$helper]: ${line.trim()}" }
                val declaration = functionDeclaration.find(line)
                if (declaration != null) {
                    val name = declaration.groupValues[1]
                    helper = if (name.startsWith("assertLive")) name else null
                    entered = false
                    depth = 0
                    if (helper != null && nullableHelperOnOneLine.containsMatchIn(line)) offend()
                }
                if (helper == null) return@forEachIndexed
                if (!entered && nullableHelperContinued.containsMatchIn(line)) offend()
                if (returnsNull.containsMatchIn(line)) offend()
                val opens = line.count { it == '{' }
                depth += opens - line.count { it == '}' }
                if (opens > 0) entered = true
                if (entered && depth <= 0) helper = null
            }
        }
        assertTrue(
            offenders.isEmpty(),
            "a live assertion helper can hand back null, which is the doorway every `?: return` " +
                "above came through. A helper that cannot answer must FAIL with the third party's " +
                "own words and the retry advice, so that no caller has the option:\n  " +
                offenders.joinToString("\n  ")
        )
    }

    /** These scans must SEE the shapes they forbid, or their silence is empty. */
    @Test
    fun theUpstreamEscapeScansFindTheShapesTheyLookFor() {
        assertTrue(
            liveAssertionCall.containsMatchIn("""        val h = assertLiveExplorerTool("t", r, "f")"""),
            "the live-helper call is not matched"
        )
        assertTrue(
            liveAssertionCall.containsMatchIn("        val a = assertLiveChrAggregatesTool(result)"),
            "a second live helper spelling is not matched"
        )
        assertTrue(
            leavesOnTheValue.containsMatchIn("""        val h = assertLiveExplorerTool("t", r, "f") ?: return@runBlocking"""),
            "the inline elvis exit is not matched"
        )
        assertTrue(leavesOnTheValue.containsMatchIn("            ?: return@runBlocking"), "continued elvis")
        assertTrue(leavesOnTheValue.containsMatchIn("            ?: return null"), "elvis returning null")
        assertTrue(
            !leavesOnTheValue.containsMatchIn("        val rows = holders.jsonArray"),
            "a plain use of the value must NOT match"
        )
        assertTrue(
            nullableHelperOnOneLine.containsMatchIn("    private fun assertLiveThing(r: R): JsonObject? {"),
            "a one-line nullable helper is not matched"
        )
        assertTrue(nullableHelperContinued.containsMatchIn("    ): JsonElement? {"), "continued nullable")
        assertTrue(
            !nullableHelperContinued.containsMatchIn("    ): JsonElement {"),
            "a non-nullable helper must NOT match"
        )
        assertTrue(returnsNull.containsMatchIn("            return null"), "return null not matched")
        // And the call-site scan is reading a real corpus.
        assertTrue(
            liveAssertionWindows().size > 20,
            "the live-assertion scan found ${liveAssertionWindows().size} call sites; the suite has " +
                "more than that, so the scan stopped reading rather than the tree being clean"
        )
    }

    /** The scan must be able to SEE the shape, or its silence means nothing. */
    @Test
    fun theSilentReturnScanFindsTheShapeItLooksFor() {
        val exits = testExitingReturns()
        // Present-tense proof the regexes match real code: these are the exact
        // three forms, checked against literal source lines rather than a file.
        assertTrue(testExitingReturn.containsMatchIn("            return"), "bare return not matched")
        assertTrue(testExitingReturn.containsMatchIn("        return@runBlocking"), "runBlocking exit not matched")
        assertTrue(!testExitingReturn.containsMatchIn("        return@forEach"), "loop continue must NOT match")
        assertTrue(!testExitingReturn.containsMatchIn("        return amount;"), "return of a value must NOT match")
        assertTrue(announcement.containsMatchIn("""System.err.println("skipped")"""), "announcement not matched")
        assertTrue(anAssertion.containsMatchIn("assertTrue(x, \"y\")"), "assertion not matched")
        // And it is looking at a real, non-empty corpus.
        assertTrue(
            exits.isNotEmpty(),
            "the scan found no test-exiting return anywhere, which means it stopped parsing test " +
                "bodies rather than that the tree is clean"
        )
    }

    // ---- 6. the third status may only be reached with its proof -------------

    /**
     * THE UPSTREAM WARNING, AND WHY IT NEEDS PINNING AT ALL.
     *
     * Section 5 above closed the door on "the upstream refused, so we are done".
     * Or's decision of 2026-09-08 opens a NARROWER one: a live test whose third
     * party is PROVEN down is neither a pass nor a red but an UPSTREAM WARNING -
     * still a failure in the XML, counted and printed separately by the gate.
     *
     * That door has to stay narrow, because it is the same door. What separates
     * it from the round-18 hole is not the wording, it is the PROOF: an
     * allowlisted signature only the third party can produce, PLUS a canary or a
     * DATED docs/UPSTREAM.md entry saying it really is not serving that query.
     * Take either half away and it is a marker being trusted again.
     *
     * Three properties are pinned, none of them a spelling:
     *
     *  1. only the explorer live helpers may reach it - a @Test body that called
     *     it directly could excuse any failure it liked;
     *  2. an unproven failure comes back a PLAIN red and writes NO evidence file,
     *     proved by calling the real function and reading the real directory;
     *  3. a ledger entry that carries no date, or does not name the query it is
     *     being used to excuse, is not an entry.
     */
    private val upstreamOutageCall = Regex("""\bupstreamOutage\s*\(""")

    @Test
    fun onlyTheLiveExplorerHelpersMayReportAnUpstreamWarning() {
        val offenders = mutableListOf<String>()
        for (file in RepoFiles.testSources()) {
            val name = RepoFiles.className(file)
            // LiveEnv declares it; this file drives it directly to pin the
            // refusal branch, which is the one place a non-helper may.
            if (name == "LiveEnv" || name == "AssumptionLedgerTest") continue
            var enclosing = "<top level>"
            stripKotlinComments(file.toFile().readText()).lines().forEachIndexed { index, line ->
                functionDeclaration.find(line)?.let { enclosing = it.groupValues[1] }
                if (upstreamOutageCall.containsMatchIn(line) && !enclosing.startsWith("assertLive")) {
                    offenders += "${file.fileName}:${index + 1} [$enclosing]: ${line.trim()}"
                }
            }
        }
        assertTrue(
            offenders.isEmpty(),
            "LiveEnv.upstreamOutage was reached from outside a live assertion helper. The third " +
                "status exists so a PROVEN third-party outage is not read as our red; a test body " +
                "that calls it directly has decided its own verdict, which is the round-18 shape " +
                "wearing a new name. Route it through the helper that already classifies the " +
                "error:\n  " + offenders.joinToString("\n  ")
        )
        // And the scan is reading a real corpus: the three helpers wire it.
        val wired = RepoFiles.testSources().filter { file ->
            RepoFiles.className(file) !in setOf("LiveEnv", "AssumptionLedgerTest") &&
                upstreamOutageCall.containsMatchIn(stripKotlinComments(file.toFile().readText()))
        }.map { RepoFiles.className(it) }.sorted()
        assertEquals(
            listOf("ProbeImprovementsRegressionTest", "ToolExecutorRemainingToolsTest", "ToolExecutorStrategiesTest"),
            wired,
            "the three live explorer helpers must each classify through LiveEnv.upstreamOutage. A " +
                "helper left unwired reports a proven outage as our red, and the gate cannot tell " +
                "the difference - which is the whole point of the third status."
        )
    }

    @Test
    fun anUpstreamFailureWithNoAllowlistedSignatureStaysAPlainRedAndLeavesNoEvidence() {
        val evidence = LiveEnv.upstreamDir.resolve("warnings").resolve(
            "AssumptionLedgerTest.anUpstreamFailureWithNoAllowlistedSignatureStaysAPlainRedAndLeavesNoEvidence.json"
        )
        java.nio.file.Files.deleteIfExists(evidence)

        // A real failure text with no allowlisted signature. "Connection
        // refused" is deliberately NOT on the downgrade allowlist even though
        // the helpers' wording lists mention it: a refused socket says nothing
        // about whose fault it is, and several tests here produce one on purpose.
        val thrown = org.junit.jupiter.api.Assertions.assertThrows(AssertionError::class.java) {
            LiveEnv.upstreamOutage(
                "filter_blockchains",
                LiveEnv.UpstreamEvidence(
                    query = "allBlockchains",
                    errorText = "Request failed: Connection refused: no further information",
                    ledgerEntry = "3b"
                )
            )
        }
        assertTrue(
            !thrown.message.orEmpty().startsWith(LiveEnv.UPSTREAM_WARNING_PREFIX),
            "a failure with no allowlisted signature must stay a PLAIN red - the signature is the " +
                "half of the proof that says the words are the third party's: ${thrown.message}"
        )
        assertTrue(
            thrown.message.orEmpty().contains("allowlisted upstream signature"),
            "the refusal must name the guardrail that refused: ${thrown.message}"
        )
        assertTrue(
            !java.nio.file.Files.exists(evidence),
            "an unproven failure wrote an evidence file at $evidence. The gate counts a warning only " +
                "when a file backs it, so a file written on the refusal path is a warning " +
                "manufactured out of a red."
        )
    }

    @Test
    fun aLedgerEntryOnlyExcusesTheQueryItNamesAndOnlyWhenItIsDated() {
        assertTrue(
            LiveEnv.datedLedgerEntry("3b", "allBlockchains(state:)") != null,
            "docs/UPSTREAM.md #3b must record `allBlockchains(state:)` with a date - it is the " +
                "guardrail that lets the live `state` test warn instead of redding while the " +
                "explorer as a whole is up"
        )
        assertTrue(
            LiveEnv.datedLedgerEntry("11", "blockchainAnalytics") != null,
            "docs/UPSTREAM.md #11 must record `blockchainAnalytics` with a date - without it a " +
                "60 s connection drop on that query is a plain red while the canary answers"
        )
        assertTrue(
            LiveEnv.datedLedgerEntry("3b", "blockchainAnalytics") == null,
            "an entry may only excuse the query it NAMES. #3b is about allBlockchains(state:); " +
                "letting it cover a different broken field is how one outage would excuse the next."
        )
        assertTrue(
            LiveEnv.datedLedgerEntry("this-entry-does-not-exist", "allBlockchains(state:)") == null,
            "a ledger entry that is not in docs/UPSTREAM.md excuses nothing"
        )
    }

    /**
     * ROUND 19'S DEEP FINDING, MEASURED END TO END.
     *
     * Until 2026-09-09 `LiveEnv.explorerCanary()` built `ChromiaConfig()` and
     * `HttpClientService(config)` and asked `config.explorerUrl` - the same
     * config, the same client, the same bounds and the same endpoint as the tool
     * that had just failed. A fault on OUR side of the wire therefore satisfied
     * both guardrails at once: `HttpTimeouts.requestTimeout` is ours, ktor's
     * text for it is `Request timeout has expired`, and that text is an
     * allowlisted signature. Set the bound low enough and our own bug reads as
     * ChromaWay being down - `upstream=1`, exit 0.
     *
     * Round 19 analysed that and wrote it down as unmeasured rather than
     * claiming it. This is the measurement, all the way through:
     *
     *  1. the production request timeout is tightened to 1 ms **through the real
     *     seam** - `ChromiaConfig` and `HttpTimeouts`, the production data
     *     classes `App` and [LiveChromia] construct. There is no double
     *     anywhere here: the client, the repository, the strategy and the
     *     explorer are the real ones, and the failure comes off the real wire;
     *  2. the live call really fails, and its real ktor message really carries
     *     an allowlisted signature - so guardrail 1 passes and the failure LOOKS
     *     upstream, which is the whole hole;
     *  3. `upstreamOutage` classifies it and must answer RED, because the
     *     INDEPENDENT canary - a plain `java.net.http` client with its OWN 20 s
     *     bound, which our 1 ms cannot reach - does not agree;
     *  4. and no evidence file is left behind, because a file the gate could
     *     count is a warning manufactured out of a red.
     *
     * It lives in this class because [onlyTheLiveExplorerHelpersMayReportAnUpstreamWarning]
     * forbids `upstreamOutage` outside the live helpers and exempts this file
     * exactly so the refusal branches can be driven directly.
     */
    @Test
    fun ourOwnRequestTimeoutThroughTheRealSeamStaysARed() {
        LiveChromia.requireLive(
            "tightens the production request timeout to 1 ms through the real ChromiaConfig seam and " +
                "proves our own bound expiring is still OUR red"
        )
        val evidenceFile = LiveEnv.upstreamDir.resolve("warnings").resolve(
            "AssumptionLedgerTest.ourOwnRequestTimeoutThroughTheRealSeamStaysARed.json"
        )
        Files.deleteIfExists(evidenceFile)

        // THE REAL SEAM: production's own config, with production's own timeouts
        // type, holding a bound the explorer cannot possibly answer inside.
        // Everything else is the default - this is `LiveChromia.repository()`
        // with one field changed.
        val production = ChromiaConfig()
        val tightened = production.copy(
            httpTimeouts = production.httpTimeouts.copy(requestTimeout = 1.milliseconds)
        )
        val repository = ChromiaRepositoryImpl(
            config = tightened,
            httpClientService = HttpClientService(tightened),
            postchainClientService = PostchainClientService(tightened)
        )
        val refused = runBlocking {
            FilterBlockchainsStrategy().execute(
                callToolRequest(
                    name = "filter_blockchains",
                    arguments = buildJsonObject {
                        put("network", LiveChromia.EXPLORER_NETWORK)
                        put("limit", 1)
                    }
                ),
                repository
            )
        }
        val text = (refused.content.first() as TextContent).text.orEmpty()
        assertEquals(
            true, refused.isError,
            "a 1 ms request timeout must make the call FAIL - if the explorer answered inside 1 ms " +
                "this measurement is about nothing: $text"
        )
        val signature = LiveEnv.upstreamSignature(text)
        assertEquals(
            "explorer-request-timeout", signature,
            "OUR OWN outbound bound expiring still matches an allowlisted signature - that is the " +
                "hole, and a test that could not reproduce it would prove nothing: $text"
        )

        // The canary is the ONE measurement of this JVM, and it is independent:
        // its bound is its own, so our 1 ms never reached it.
        val canary = LiveEnv.explorerCanary()
        assertTrue(
            LiveEnv.CANARY_REQUEST_TIMEOUT.toMillis() !=
                tightened.httpTimeouts.requestTimeout.inWholeMilliseconds &&
                LiveEnv.CANARY_REQUEST_TIMEOUT.toMillis() !=
                production.httpTimeouts.requestTimeout.inWholeMilliseconds,
            "the canary's bound (${LiveEnv.CANARY_REQUEST_TIMEOUT.toMillis()} ms) must be its own - " +
                "not the production ${production.httpTimeouts.requestTimeout} and not the " +
                "${tightened.httpTimeouts.requestTimeout} under test. Sharing that field is exactly " +
                "how one fault of ours satisfied the signature guard and the canary guard at once."
        )
        assertEquals(
            "true", LiveEnv.canaryJson(canary)["independent"].toString(),
            "the evidence the gate reads must say the canary was measured on the independent path"
        )

        val canaryAgreed = canary.state == LiveEnv.CanaryState.FAILED_SIGNATURE &&
            canary.signature == signature
        val thrown = assertThrows(AssertionError::class.java) {
            LiveEnv.upstreamOutage(
                "filter_blockchains",
                LiveEnv.UpstreamEvidence(query = "allBlockchains", errorText = text, ledgerEntry = null)
            )
        }
        val classified =
            if (thrown.message.orEmpty().startsWith(LiveEnv.UPSTREAM_WARNING_PREFIX)) "UPSTREAM WARNING" else "RED"
        val measured = buildJsonObject {
            put("probe", "a5_our_own_request_timeout_through_the_real_seam")
            put("seam", "ChromiaConfig(httpTimeouts = HttpTimeouts(requestTimeout = 1ms))")
            put("tool", "filter_blockchains")
            put("toolFailureSignature", signature)
            put("canaryIsIndependent", true)
            put("canaryAgreedWithTheToolFailure", canaryAgreed)
            put("classified", classified)
            put("evidenceFileWritten", Files.exists(evidenceFile))
            put("truth", "RED - our own outbound bound expiring is not the third party being down")
        }
        Round19Evidence.record("upstream/our-own-timeout.json", measured)

        assertTrue(
            classified == "RED",
            "OUR OWN 1 ms request timeout was reported as a PROVEN upstream outage. The signature " +
                "guard cannot tell our bound from theirs - it is the same ktor sentence - so the " +
                "canary is the only thing standing here, and a canary that shares our timeout falls " +
                "over at the same moment we do: ${thrown.message}"
        )
        assertTrue(
            thrown.message.orEmpty().contains("independent canary"),
            "the refusal must name the guardrail that refused, and say it was measured on the " +
                "independent path: ${thrown.message}"
        )
        assertTrue(
            !Files.exists(evidenceFile),
            "an unproven failure wrote an evidence file at $evidenceFile - the gate counts a warning " +
                "only when a file backs it, so a file on the refusal path is a warning manufactured " +
                "out of a red"
        )
        Round19Evidence.assertFrozen("upstream/our-own-timeout.json", measured)
    }
}
