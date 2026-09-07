package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
            "starts a REAL Postchain node and drives its REST surface; the in-process ChainGateway double " +
                "in LocalChainRestBridgeTest is the thing this exists to be the counterpart of."
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
            "ProvisioningToolsTest.kt", "realChrResolvesWithoutOverrideAndRuns", "requireChrRan",
            Resource.CHR_CLI,
            "resolves chr WITHOUT the CHROMIA_CHR_BIN override and actually runs it. It used to skip when " +
                "chr was present but unlaunchable, which silently excused the exact box (a scoop .cmd shim) " +
                "the check exists for; with CHROMIA_REQUIRE_CHR set that is now a failure."
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
        assertTrue(
            Regex("""skippedNames\.length\)\s*\{""").containsMatchIn(gate) || gate.contains("there is no allowlist"),
            "the gate must fail on ANY skip"
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
                        found += TestReturn(
                            file.fileName.toString(), name, index + 1, line.trim(), recent.toList()
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
}
