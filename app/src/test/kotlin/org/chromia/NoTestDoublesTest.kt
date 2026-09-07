package org.chromia

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * THERE ARE NO TEST DOUBLES IN THIS SUITE. This is the scan that says so.
 *
 * Until 2026-09-07 this file was `MockLedgerTest`, and it held a table: 41 rows,
 * each naming a double, the third party it stood in for, and a live check said
 * to cover the same path. The rule it enforced was "a double may stand in for a
 * THIRD PARTY, never for our own code". That rule was defensible and it was
 * still wrong, and the conversion proved it within the hour: when the sixteen
 * explorer tools were pointed at the real explorer, FOUR of them turned out not
 * to work at all - `dashboardData` and `groupedTransactionsByBlockchain`
 * answering INTERNAL_ERROR, `getNodeUnavailability` behind a reCAPTCHA header
 * this client does not send. Every one had a green unit test. The fixtures had
 * been answering on the explorer's behalf, and the named live counterparts had
 * not caught it, because a counterpart that covers "the same path" in the
 * abstract is not the same as the assertion itself running against the thing.
 * All four tools are retired now (docs/UPSTREAM.md #3a, #7a).
 *
 * So the table is gone and the assertion is zero. What replaces a double is one
 * of three things, decided per site and written down where the site was:
 *
 *  - the REAL thing in process - the `chr` CLI on PATH, the embedded Postchain
 *    node the suite already knows how to start, the production embedding model,
 *    a real embedded HTTP server serving real bytes over a real socket, a real
 *    CLOSED loopback port when the honest outcome is a connection error;
 *  - the REAL thing live - [LiveChromia] wires the production services with the
 *    production config against the public explorer and the testnet Economy
 *    Chain, read-only, no key material, nothing signed or spent;
 *  - DELETION, when no real input can produce the shape - and then the comment
 *    left in place names the claim that went with it, and what (if anything)
 *    covers it now.
 *
 * A literal VALUE handed to real production code is not a double: a
 * `NetworkResult.Error("...")` fed to the real error translator, a GraphQL
 * envelope fed to the real parser, a `TextSegment` fed to the real embedder are
 * all data. What is forbidden is a substitute IMPLEMENTATION - something that
 * takes the place of a collaborator and answers on its behalf.
 *
 * ONE BOUNDARY CASE, judged and named rather than left for a reader to find.
 * `resolveLocalEmbeddingsPath` / `resolveRuntimeEmbeddingsPath` take
 * `exists: (Path) -> Boolean` and `directoryExists`, and
 * `RagStoreCwdIndependenceTest` passes both (four call sites). That is not a
 * collaborator being impersonated: those functions are PURE over (candidate
 * paths, an existence predicate) - the predicate is the input, the way the env
 * map beside it is. It is also the only way to ask the question the tests ask,
 * which is what the answer would be from a DIFFERENT working directory: a JVM
 * cannot change its own cwd, so the alternative is not a more honest test, it is
 * no test. The detectors deliberately do not flag it, and this paragraph is why.
 *
 * The detectors below are the ones the ledger used, kept and widened. They are
 * deliberately close to `grep`: a double is where substitute BEHAVIOUR is
 * defined, so this finds the declaration rather than each use.
 */
class NoTestDoublesTest {

    /** `FakeX`, `MockY`, `StubZ`, `RecordingW`, `ScriptedV`... by name. */
    private val namedDouble = Regex(
        """^\s*(?:private\s+|internal\s+|open\s+|abstract\s+|inner\s+)*(?:class|object)\s+(\w*(?:Mock|Fake|Stub|Recording|Scripted|Dummy|Canned|Noop|NoOp|Spy|Double)\w*)\b"""
    )

    /** `object : SomeProductionType { ... }` - a substitute with no name at all. */
    private val anonymousObject = Regex("""\bobject\s*:\s*([\w.]+)\s*[({]""")

    /** Ktor's mock engine, in any form. */
    private val mockEngine = Regex("""\bMockEngine\b""")

    /** SAM-converted lambdas for our own `fun interface` seams. */
    private val samLambda =
        Regex("""\b(TxPoster|ProcessRunner|ChainGateway|BlockchainQueryClient|BlockchainHeightClient)\s*\{""")

    /**
     * Named-argument seams that take a lambda: the client cache, the index
     * loaders, the embedding-model SPI lookup - and any `*OverrideForTests`
     * hook at all. A production field whose NAME says it exists for tests is an
     * admission; the assertion is that none is left to assign to.
     */
    private val clientSeam = Regex(
        """\b(queryClient|heightClient|clientFactory|registryLoader|storeLoader|""" +
            """embeddingModelSpiLoader|\w*OverrideForTests)\s*="""
    )

    /** The trailing-lambda form of the query-client seam. */
    private val trailingQueryClient = Regex("""\bPostchainClientService\s*\([^\n]*\)\s*\{""")

    /**
     * The production types a double can be BUILT on. A class implementing one of
     * these is a substitute however innocently it is named -
     * `BagOfWordsEmbeddingModel` was not called Fake or Mock and was still an
     * embedding model standing in for the real one.
     */
    private val seamTypes =
        "(EmbeddingModel|ChromiaRepository|TxPoster|ProcessRunner|ChainGateway|PostchainQuery|" +
            "RagStore|PromptManager|HttpHandler|HttpClientEngine|ContentRetriever|" +
            "BlockchainQueryClient|BlockchainHeightClient)"
    private val implementsSeamInline =
        Regex("""\b(?:class|object)\s+\w+\s*(?:\([^()]*\))?\s*:\s*(?:[\w.]+\.)?$seamTypes\b""")

    /** The `) : TxPoster {` line closing a multi-line constructor. */
    private val implementsSeamContinued = Regex("""^\s*\)\s*:\s*(?:[\w.]+\.)?$seamTypes\b""")

    /**
     * Comments are stripped first. Prose ABOUT a double - this file is full of
     * it, and so is every test that explains why its double went - is not a
     * double, and a scan that reacted to its own explanation would be useless.
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

    data class Site(val file: String, val line: Int, val kind: String, val target: String) {
        override fun toString() = "$file:$line  $kind $target"
    }

    private fun doubleSites(): List<Site> {
        val sites = mutableListOf<Site>()
        for (file in RepoFiles.testSources()) {
            // This scan and its own regexes must not scan themselves.
            if (RepoFiles.className(file) == "NoTestDoublesTest") continue
            val name = file.fileName.toString()
            stripComments(Files.readString(file)).lineSequence().forEachIndexed { index, line ->
                val n = index + 1
                namedDouble.find(line)?.let { sites += Site(name, n, "NAMED", it.groupValues[1]) }
                anonymousObject.findAll(line).forEach {
                    sites += Site(name, n, "ANONYMOUS", it.groupValues[1].substringAfterLast('.'))
                }
                if (mockEngine.containsMatchIn(line)) sites += Site(name, n, "MOCK_ENGINE", "MockEngine")
                samLambda.findAll(line).forEach { sites += Site(name, n, "SAM_LAMBDA", it.groupValues[1]) }
                clientSeam.findAll(line).forEach { sites += Site(name, n, "SEAM", it.groupValues[1]) }
                if (trailingQueryClient.containsMatchIn(line)) {
                    sites += Site(name, n, "SEAM", "queryClient")
                }
                implementsSeamInline.findAll(line).forEach {
                    sites += Site(name, n, "IMPLEMENTS", it.groupValues[1])
                }
                implementsSeamContinued.findAll(line).forEach {
                    sites += Site(name, n, "IMPLEMENTS", it.groupValues[1])
                }
            }
        }
        return sites
    }

    // ---- the assertion ----------------------------------------------------

    @Test
    fun thereAreNoTestDoubles() {
        val sites = doubleSites()
        assertTrue(
            sites.isEmpty(),
            "a test double appeared. There is no ledger to add it to any more and no counterpart that " +
                "excuses it: a recorded answer cannot notice that the thing it stands in for changed, " +
                "which is the only reason to test an integration at all. Drive the real thing (in " +
                "process or live), or delete the test together with the claim it carries and say so " +
                "where it stood:\n  " + sites.joinToString("\n  ")
        )
    }

    /**
     * A scan whose regexes have quietly stopped matching would be silent for the
     * wrong reason, and silence is exactly this file's output. Each detector is
     * exercised against a literal line so its silence means something.
     */
    @Test
    fun everyDetectorStillMatchesTheShapeItLooksFor() {
        assertTrue(namedDouble.containsMatchIn("    private class FakeChain(val x: Int) {"), "NAMED")
        assertTrue(namedDouble.containsMatchIn("object MockThing {"), "NAMED object")
        assertTrue(namedDouble.containsMatchIn("private class RecordingRepository : X {"), "NAMED recording")
        assertTrue(anonymousObject.containsMatchIn("val g = object : ChainGateway {"), "ANONYMOUS")
        assertTrue(mockEngine.containsMatchIn("val engine = MockEngine { respond(\"\") }"), "MOCK_ENGINE")
        assertTrue(samLambda.containsMatchIn("txPoster = TxPoster { _, _ -> outcome }"), "SAM_LAMBDA")
        assertTrue(clientSeam.containsMatchIn("queryClient = { _, _, _ -> gtv }"), "SEAM queryClient")
        assertTrue(clientSeam.containsMatchIn("registryLoader = { null }"), "SEAM registryLoader")
        assertTrue(
            clientSeam.containsMatchIn("LocalChain.starterOverrideForTests = { plan -> running }"),
            "SEAM OverrideForTests"
        )
        assertTrue(
            clientSeam.containsMatchIn("RunRellTests.runnerOverrideForTests = null"),
            "SEAM OverrideForTests reset - the reset goes with the hook"
        )
        assertTrue(
            trailingQueryClient.containsMatchIn("val s = PostchainClientService(ChromiaConfig()) { a, b, c ->"),
            "trailing SEAM"
        )
        assertTrue(
            implementsSeamInline.containsMatchIn("private class Whatever(val n: Int) : EmbeddingModel {"),
            "IMPLEMENTS inline"
        )
        assertTrue(implementsSeamContinued.containsMatchIn("    ) : TxPoster {"), "IMPLEMENTS continued")
        // And prose about a double is not a double.
        assertTrue(
            stripComments("// this used to be a MockEngine\nval x = 1").let { !mockEngine.containsMatchIn(it) },
            "comments must be stripped before the scan"
        )
        assertTrue(
            stripComments("/**\n * registryLoader = { null }\n */\nval x = 1")
                .let { !clientSeam.containsMatchIn(it) },
            "KDoc about a removed seam must be stripped before the scan"
        )
    }

    /** The scan must actually be reading a corpus, not an empty list. */
    @Test
    fun theScanReadsTheWholeTestTree() {
        val sources = RepoFiles.testSources()
        assertTrue(sources.size > 100, "expected the whole test tree, found ${sources.size} files")
        assertTrue(
            sources.any { RepoFiles.className(it) == "LiveChromia" },
            "LiveChromia is what replaced the doubles; if the scan cannot see it, it is not reading " +
                "the tree the doubles would live in"
        )
    }

    /**
     * No mocking FRAMEWORK may be on the classpath either. A scan of the sources
     * cannot see `every { x } returns y` if someone adds mockk tomorrow, so the
     * dependency itself is refused. `ktor-client-mock` is in the list because it
     * is what `MockEngine` comes from: the last one left this repository on
     * 2026-09-07 and the dependency went with it.
     */
    @Test
    fun noMockingFrameworkIsOnTheBuildClasspath() {
        val build = RepoFiles.text("app/build.gradle.kts")
        listOf("mockk", "mockito", "easymock", "jmock", "powermock", "wiremock", "client-mock").forEach { framework ->
            assertTrue(
                !build.contains(framework, ignoreCase = true),
                "$framework is declared in app/build.gradle.kts. There are no doubles in this suite " +
                    "and there is no framework for making them."
            )
        }
    }
}
