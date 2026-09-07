package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * THE MOCK LEDGER: every test double in this suite, what it stands in for, and
 * the REAL check that exercises the same path.
 *
 * The rule an external review applied to this repository, and the one GOAL.md
 * already implies ("every claim testable or deleted"):
 *
 *  - a double may stand in for a **third party** - the explorer's HTTP API, a
 *    chain node, the docs site, the published embeddings index, the `chr` CLI -
 *    only if a NAMED real check covers the same path, so that the third party
 *    changing underneath the fixture is something we find out from a red build
 *    rather than from a user;
 *  - a double may **never** stand in for our own code. A strategy, a validator,
 *    the rule engine, the scaffold, the test runner or the docs retrieval
 *    replaced by a canned answer is a green that proves nothing about the thing
 *    it names. Two such doubles existed when this ledger was written (an
 *    override of `RagStore.query` in ten places and a `PromptManager` subclass
 *    that threw on command); both were rewritten to drive the real code, which
 *    is why [StandsFor.OUR_OWN_CODE] exists here with no rows.
 *
 * The set of doubles is SCANNED from the test sources, not typed out, so a new
 * double cannot appear unlisted: [doubleSitesInSources] finds named doubles,
 * anonymous `object : T` substitutes, Ktor `MockEngine`s, SAM lambdas for our
 * production `fun interface` seams and the PostchainClientService client seams,
 * and [ledgerMatchesTheSources] fails if that set differs from [LEDGER] in
 * either direction.
 */
class MockLedgerTest {

    /** What the double replaces. Every value but [OUR_OWN_CODE] is outside this repo. */
    enum class StandsFor(val what: String) {
        EXPLORER_HTTP_API("the explorer's GraphQL/HTTP API (a third-party service)"),
        CHAIN_NODE("a Chromia chain node reached through postchain-client (a third party)"),
        LOCAL_CHAIN_ENGINE("the embedded Postchain node behind LocalChainRestBridge (a third-party library's engine)"),
        ECONOMY_CHAIN("the testnet Economy Chain: pricing, account lookup, the faucet, tx submission"),
        CHR_CLI("the third-party `chr` command-line tool as an OS process"),
        DOCS_SITE("docs.chromia.com - its sitemap and pages"),
        RAG_INDEX_DOWNLOAD("the published embeddings index asset (GitHub release / GitLab package)"),
        EMBEDDING_MODEL("langchain4j's embedding model (production: the bundled quantized BGE-small ONNX)"),
        OUR_OWN_CODE("code in this repository - NEVER acceptable; rewrite the test to drive the real thing")
    }

    /** How the real counterpart is identified, and how this test proves it exists. */
    enum class CounterpartKind { SWEEP_CHECK, LIVE_TEST, CI_STEP }

    data class Counterpart(val kind: CounterpartKind, val id: String)

    data class Double(
        val file: String,
        val kind: String,
        val target: String,
        val standsFor: StandsFor,
        val counterparts: List<Counterpart>
    )

    private fun sweep(label: String) = Counterpart(CounterpartKind.SWEEP_CHECK, label)
    private fun liveTest(classAndMethod: String) = Counterpart(CounterpartKind.LIVE_TEST, classAndMethod)
    private fun ciStep(command: String) = Counterpart(CounterpartKind.CI_STEP, command)

    // Counterparts used often enough to name once.
    private val liveExplorer = listOf(sweep("get_network_stats"), sweep("get_all_assets"), sweep("blockchain_details"))
    private val liveChain = listOf(sweep("dapp_query live on-chain"), sweep("verify_deployment: live mainnet chain + bogus brid"))
    private val liveChr = listOf(
        liveTest("DappBuildToolsTest.chrVersionLiveProbe"),
        liveTest("ProvisioningToolsTest.realChrResolvesWithoutOverrideAndRuns"),
        liveTest("RealProcessRunnerTest.timeoutKillsAHangingChildInsteadOfWaitingForItsOutput")
    )
    private val liveEconomyChain = listOf(
        liveTest("TestnetProvisioningLiveTest.liveDryRunPricesLeaseAndResolvesEverythingWithoutSpending"),
        liveTest("TestnetProvisioningLiveTest.liveKnownAccountIsRegisteredOnTheEconomyChain"),
        liveTest("TestnetProvisioningLiveTest.liveClaimDryRunReportsFaucetTerms")
    )
    private val liveTxSubmission = listOf(
        liveTest("TestnetProvisioningLiveTest.liveSignedTransactionIsAcceptedOnWireAndRejectedByFt4Auth")
    )
    private val liveLocalChain = listOf(
        liveTest("LocalChainIntegrationTest.chainStartsAnswersQueriesAndStops"),
        sweep("local_chain_up: up -> HTTP answers -> status -> down -> port closed")
    )
    private val liveDocsIndex = listOf(
        sweep("search (ChatGPT)"), sweep("fetch (ChatGPT)"), sweep("fetch_docs live+search"),
        ciStep("scripts/rag-eval.mjs --production-shaped")
    )
    /** Both HTTP transports of a REAL server, plus the stdio surface. */
    private val liveServerSurface = listOf(
        ciStep("scripts/e2e-sweep.mjs http://127.0.0.1:3001 --transport sse"),
        ciStep("scripts/e2e-sweep.mjs http://127.0.0.1:3001 --transport http"),
        ciStep("scripts/stdio-smoke.mjs app/build/libs/chromia-mcp-server.jar"),
        sweep("coverage: every advertised tool responds")
    )

    val LEDGER: List<Double> = listOf(
        // ---- the chain node, behind postchain-client -----------------------
        Double("AuditConcurrencyRegressionTest.kt", "ANONYMOUS", "PostchainQuery", StandsFor.CHAIN_NODE, liveChain),
        Double("AuditConcurrencyRegressionTest.kt", "CLIENT_SEAM", "clientFactory", StandsFor.CHAIN_NODE, liveChain),
        Double("AuditGtvAndFt4TreeRegressionTest.kt", "CLIENT_SEAM", "queryClient", StandsFor.CHAIN_NODE, liveChain),
        Double("AuditRound4RegressionTest.kt", "ANONYMOUS", "PostchainQuery", StandsFor.CHAIN_NODE, liveChain),
        Double("AuditRound4RegressionTest.kt", "CLIENT_SEAM", "clientFactory", StandsFor.CHAIN_NODE, liveChain),
        Double("AuditRound4RegressionTest.kt", "CLIENT_SEAM", "queryClient", StandsFor.CHAIN_NODE, liveChain),
        Double("PostchainClientServiceTest.kt", "CLIENT_SEAM", "queryClient", StandsFor.CHAIN_NODE, liveChain),
        Double("ToolExecutorStrategiesTest.kt", "CLIENT_SEAM", "queryClient", StandsFor.CHAIN_NODE, liveChain),
        Double("VerifyDeploymentToolTest.kt", "CLIENT_SEAM", "heightClient", StandsFor.CHAIN_NODE, liveChain),
        Double("McpJsonRpcSessionTest.kt", "CLIENT_SEAM", "queryClient", StandsFor.CHAIN_NODE, liveChain),
        // McpTestSupport's seam is a REFUSING double: it errors on any call, so
        // an in-process session that reaches for the network fails loudly. Its
        // counterpart is the same tool surface answered by a real server.
        Double("McpTestSupport.kt", "CLIENT_SEAM", "queryClient", StandsFor.CHAIN_NODE, liveServerSurface + liveChain),

        // ---- the explorer's HTTP API, behind Ktor ---------------------------
        Double("HttpClientServiceTest.kt", "MOCK_ENGINE", "MockEngine", StandsFor.EXPLORER_HTTP_API, liveExplorer),
        Double("ToolExecutorStrategiesTest.kt", "MOCK_ENGINE", "MockEngine", StandsFor.EXPLORER_HTTP_API, liveExplorer),
        Double("McpJsonRpcSessionTest.kt", "MOCK_ENGINE", "MockEngine", StandsFor.EXPLORER_HTTP_API, liveExplorer + liveServerSurface),
        Double("McpSseSessionTest.kt", "MOCK_ENGINE", "MockEngine", StandsFor.EXPLORER_HTTP_API, liveExplorer + liveServerSurface),
        Double("McpStreamableHttpSessionTest.kt", "MOCK_ENGINE", "MockEngine", StandsFor.EXPLORER_HTTP_API, liveExplorer + liveServerSurface),
        Double("McpTestSupport.kt", "MOCK_ENGINE", "MockEngine", StandsFor.EXPLORER_HTTP_API, liveExplorer + liveServerSurface),
        Double("RecordingRepository.kt", "NAMED", "RecordingRepository", StandsFor.EXPLORER_HTTP_API, liveExplorer + liveChain),
        Double("RecordingRepository.kt", "IMPLEMENTS", "ChromiaRepository", StandsFor.EXPLORER_HTTP_API, liveExplorer + liveChain),

        // ---- the embedded Postchain node behind the local chain -------------
        Double("LocalChainRestBridgeTest.kt", "ANONYMOUS", "ChainGateway", StandsFor.LOCAL_CHAIN_ENGINE, liveLocalChain),
        Double("LocalChainToolTest.kt", "ANONYMOUS", "ChainGateway", StandsFor.LOCAL_CHAIN_ENGINE, liveLocalChain),

        // ---- the testnet Economy Chain and tx submission --------------------
        Double("ProvisioningToolsTest.kt", "NAMED", "FakeChain", StandsFor.ECONOMY_CHAIN, liveEconomyChain),
        Double("ProvisioningToolsTest.kt", "CLIENT_SEAM", "queryClient", StandsFor.ECONOMY_CHAIN, liveEconomyChain),
        Double("ProvisioningToolsTest.kt", "CLIENT_SEAM", "heightClient", StandsFor.ECONOMY_CHAIN, liveEconomyChain),
        Double("ProvisioningRobustnessTest.kt", "CLIENT_SEAM", "queryClient", StandsFor.ECONOMY_CHAIN, liveEconomyChain),
        Double("ProvisioningRobustnessTest.kt", "CLIENT_SEAM", "heightClient", StandsFor.ECONOMY_CHAIN, liveEconomyChain),
        Double("ProvisioningToolsTest.kt", "NAMED", "FakePoster", StandsFor.ECONOMY_CHAIN, liveTxSubmission),
        Double("ProvisioningToolsTest.kt", "IMPLEMENTS", "TxPoster", StandsFor.ECONOMY_CHAIN, liveTxSubmission),
        Double("ProvisioningToolsTest.kt", "SAM_LAMBDA", "TxPoster", StandsFor.ECONOMY_CHAIN, liveTxSubmission),
        Double("ProvisioningRobustnessTest.kt", "SAM_LAMBDA", "TxPoster", StandsFor.ECONOMY_CHAIN, liveTxSubmission),
        // ScriptedNode is an http4k HttpHandler standing in for a node's REST
        // API: the POST /tx endpoint and the status polls RealTxPoster drives.
        Double("RealTxPosterTest.kt", "NAMED", "ScriptedNode", StandsFor.CHAIN_NODE, liveTxSubmission),
        Double("RealTxPosterTest.kt", "IMPLEMENTS", "HttpHandler", StandsFor.CHAIN_NODE, liveTxSubmission),

        // ---- the chr CLI as an OS process -----------------------------------
        Double("ProvisioningToolsTest.kt", "NAMED", "FakeRunner", StandsFor.CHR_CLI, liveChr),
        Double("ProvisioningToolsTest.kt", "IMPLEMENTS", "ProcessRunner", StandsFor.CHR_CLI, liveChr),
        Double("ProvisioningToolsTest.kt", "SAM_LAMBDA", "ProcessRunner", StandsFor.CHR_CLI, liveChr),
        Double("ProvisioningRobustnessTest.kt", "SAM_LAMBDA", "ProcessRunner", StandsFor.CHR_CLI, liveChr),

        // ---- the docs site --------------------------------------------------
        // Added 2026-09-07: this was the one double in the suite with NO real
        // counterpart. The whole sitemap ingest ran against MockEngine and
        // nothing else, so docs.chromia.com changing shape was invisible here.
        Double(
            "SitemapDocsFetcherTest.kt", "MOCK_ENGINE", "MockEngine", StandsFor.DOCS_SITE,
            listOf(
                sweep("docs site sitemap shape (live)"),
                liveTest("GitRepositoryFetcherLiveTest.sparseFetchesNestedPostchainClientDocWhenNetworkAvailable")
            )
        ),

        // ---- the published embeddings index ----------------------------------
        Double(
            "RagStoreRegistryDownloadTest.kt", "MOCK_ENGINE", "MockEngine", StandsFor.RAG_INDEX_DOWNLOAD,
            listOf(
                ciStep("scripts/rag-eval.mjs --production-shaped"),
                ciStep("scripts/stdio-smoke.mjs --launcher-download")
            )
        ),

        // ---- langchain4j's embedding model ------------------------------------
        Double("RagStoreLexicalBoostTest.kt", "ANONYMOUS", "EmbeddingModel", StandsFor.EMBEDDING_MODEL, liveDocsIndex),
        Double("SearchFetchToolsTest.kt", "ANONYMOUS", "EmbeddingModel", StandsFor.EMBEDDING_MODEL, liveDocsIndex),
        // TestDocsIndex.BagOfWordsEmbeddingModel: a real, deterministic hashed
        // bag-of-words embedder standing in for production's quantized BGE-small
        // ONNX. It is what let the RagStore.query() overrides go away, and it is
        // still a double of a third party, so it is listed like one.
        Double("TestDocsIndex.kt", "IMPLEMENTS", "EmbeddingModel", StandsFor.EMBEDDING_MODEL, liveDocsIndex)
    )

    // ---------------------------------------------------------------------
    // The scanner. Kept deliberately close to `grep`: a double is where the
    // substitute BEHAVIOUR is defined, so a shared double (RecordingRepository)
    // is one row at its declaration, not one row per use.
    // ---------------------------------------------------------------------

    private val namedDouble = Regex(
        """^\s*(?:private\s+|internal\s+|open\s+|abstract\s+|inner\s+)*(?:class|object)\s+(\w*(?:Mock|Fake|Stub|Recording|Scripted|Dummy|Canned|Noop|NoOp)\w*)\b"""
    )
    private val anonymousObject = Regex("""\bobject\s*:\s*([\w.]+)\s*[({]""")
    private val mockEngine = Regex("""\bMockEngine\b""")
    private val samLambda = Regex("""\b(TxPoster|ProcessRunner)\s*\{""")
    private val clientSeam = Regex("""\b(queryClient|heightClient|clientFactory)\s*=\s*\{""")
    private val trailingQueryClient = Regex("""\bPostchainClientService\s*\([^\n]*\)\s*\{""")

    /**
     * The production types a double can be BUILT on. A named class that
     * implements one of these is a substitute however innocently it is named -
     * `TestDocsIndex.BagOfWordsEmbeddingModel` is not called Fake or Mock and is
     * still an embedding model standing in for the real one.
     */
    private val seamTypes =
        "(EmbeddingModel|ChromiaRepository|TxPoster|ProcessRunner|ChainGateway|PostchainQuery|" +
            "RagStore|PromptManager|HttpHandler|HttpClientEngine|ContentRetriever)"
    private val implementsSeamInline =
        Regex("""\b(?:class|object)\s+\w+\s*(?:\([^()]*\))?\s*:\s*(?:[\w.]+\.)?$seamTypes\b""")
    /** The `) : TxPoster {` line closing a multi-line constructor. */
    private val implementsSeamContinued = Regex("""^\s*\)\s*:\s*(?:[\w.]+\.)?$seamTypes\b""")

    /**
     * Comments are stripped first. Prose about a double - this file is full of
     * it, and so are the tests that explain why a double was removed - is not a
     * double, and a ledger that reacted to its own explanation would be useless.
     */
    private fun stripComments(source: String): String {
        val withoutBlocks = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
            .replace(source) { m -> "\n".repeat(m.value.count { it == '\n' }) }
        return withoutBlocks.lineSequence().joinToString("\n") { line ->
            // `://` inside a URL literal is not the start of a comment.
            val guarded = line.replace("://", "\u0000\u0000\u0000")
            val at = guarded.indexOf("//")
            if (at >= 0) guarded.substring(0, at).replace("\u0000\u0000\u0000", "://") else line
        }
    }

    private fun doubleSitesInSources(): Set<Triple<String, String, String>> {
        val sites = mutableSetOf<Triple<String, String, String>>()
        for (file in RepoFiles.testSources()) {
            // This ledger and the scanner's own regexes must not scan themselves.
            if (RepoFiles.className(file) == "MockLedgerTest") continue
            val name = file.fileName.toString()
            for (line in stripComments(Files.readString(file)).lineSequence()) {
                namedDouble.find(line)?.let { sites += Triple(name, "NAMED", it.groupValues[1]) }
                anonymousObject.findAll(line).forEach {
                    sites += Triple(name, "ANONYMOUS", it.groupValues[1].substringAfterLast('.'))
                }
                if (mockEngine.containsMatchIn(line)) sites += Triple(name, "MOCK_ENGINE", "MockEngine")
                samLambda.findAll(line).forEach { sites += Triple(name, "SAM_LAMBDA", it.groupValues[1]) }
                clientSeam.findAll(line).forEach { sites += Triple(name, "CLIENT_SEAM", it.groupValues[1]) }
                if (trailingQueryClient.containsMatchIn(line)) sites += Triple(name, "CLIENT_SEAM", "queryClient")
                implementsSeamInline.findAll(line).forEach {
                    sites += Triple(name, "IMPLEMENTS", it.groupValues[1])
                }
                implementsSeamContinued.findAll(line).forEach {
                    sites += Triple(name, "IMPLEMENTS", it.groupValues[1])
                }
            }
        }
        return sites
    }

    // ---- the assertions ---------------------------------------------------

    @Test
    fun ledgerMatchesTheSources() {
        val scanned = doubleSitesInSources()
        val pinned = LEDGER.map { Triple(it.file, it.kind, it.target) }.toSet()
        val unlisted = (scanned - pinned).sortedBy { it.toString() }
        val stale = (pinned - scanned).sortedBy { it.toString() }
        assertTrue(
            unlisted.isEmpty(),
            "test double(s) not in the mock ledger. Every double must say WHAT it stands in for and " +
                "WHICH real check covers the same path - a double nobody has to justify is how a suite " +
                "drifts into testing its own fixtures:\n  " +
                unlisted.joinToString("\n  ") { "${it.first}: ${it.second} ${it.third}" }
        )
        assertTrue(
            stale.isEmpty(),
            "mock ledger row(s) with no double left in the sources - delete the row (or the scan no " +
                "longer sees a double it used to):\n  " +
                stale.joinToString("\n  ") { "${it.first}: ${it.second} ${it.third}" }
        )
        assertEquals(LEDGER.size, pinned.size, "duplicate rows in the ledger")
    }

    @Test
    fun noDoubleStandsInForOurOwnCode() {
        val ours = LEDGER.filter { it.standsFor == StandsFor.OUR_OWN_CODE }
        assertTrue(
            ours.isEmpty(),
            "a double of our own code is a fake green: the test names a behaviour and then supplies the " +
                "answer itself. Rewrite it to drive the real thing (an in-process server, the real " +
                "ToolExecutor, the real RagStore, the real database):\n  " +
                ours.joinToString("\n  ") { "${it.file}: ${it.kind} ${it.target}" }
        )
    }

    @Test
    fun everyDoubleHasARealCounterpartThatExists() {
        val sweepSource = RepoFiles.text("scripts/e2e-sweep.mjs")
        val ci = RepoFiles.text(".github/workflows/ci.yml")
        val missing = mutableListOf<String>()
        LEDGER.forEach { row ->
            if (row.counterparts.isEmpty()) {
                missing += "${row.file}: ${row.kind} ${row.target} has NO counterpart at all"
                return@forEach
            }
            row.counterparts.forEach { counterpart ->
                val present = when (counterpart.kind) {
                    // The sweep declares checks as check('<label>', ...).
                    CounterpartKind.SWEEP_CHECK ->
                        sweepSource.contains("check('${counterpart.id}'")
                    // A live test is Class.method; both must exist.
                    CounterpartKind.LIVE_TEST -> {
                        val className = counterpart.id.substringBefore('.')
                        val method = counterpart.id.substringAfter('.')
                        val source = RepoFiles.testSources().firstOrNull { RepoFiles.className(it) == className }
                        source != null && Regex("""\bfun\s+${Regex.escape(method)}\s*\(""")
                            .containsMatchIn(Files.readString(source))
                    }
                    // A CI step is a command line CI actually runs.
                    CounterpartKind.CI_STEP -> ci.contains(counterpart.id)
                }
                if (!present) {
                    missing += "${row.file}: ${row.kind} ${row.target} -> " +
                        "${counterpart.kind} '${counterpart.id}' does not exist"
                }
            }
        }
        assertTrue(
            missing.isEmpty(),
            "the mock ledger names counterparts that are not there. A counterpart that has been renamed " +
                "or deleted leaves the double standing alone, which is the same as never having had one:\n  " +
                missing.joinToString("\n  ")
        )
    }

    @Test
    fun everyDoubleStandsInForSomethingOutsideThisRepository() {
        val bad = LEDGER.filterNot { it.standsFor in EXTERNAL }
        assertTrue(bad.isEmpty(), "not an external dependency: $bad")
        // Named so the report can quote them, and so that adding a category is
        // a deliberate act rather than a typo.
        assertEquals(
            setOf(
                StandsFor.EXPLORER_HTTP_API, StandsFor.CHAIN_NODE, StandsFor.LOCAL_CHAIN_ENGINE,
                StandsFor.ECONOMY_CHAIN, StandsFor.CHR_CLI, StandsFor.DOCS_SITE,
                StandsFor.RAG_INDEX_DOWNLOAD, StandsFor.EMBEDDING_MODEL
            ),
            LEDGER.map { it.standsFor }.toSet(),
            "every external dependency the suite fakes must be represented; a category with no rows " +
                "means a double was deleted without its ledger row, or the scan stopped seeing it"
        )
    }

    private val EXTERNAL = StandsFor.entries.toSet() - StandsFor.OUR_OWN_CODE
}
