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
 * THE PROOF IS STRUCTURAL, AND THE SOURCE REGEXES ARE A SECOND LAYER.
 *
 * Until 2026-09-08 the eight detectors below were the whole scan, and their KDoc
 * said they were "deliberately close to grep". Adversary round 18 (section 6)
 * ported all eight verbatim and ran them over eight doubles written the way a
 * person in a hurry writes one: SIX WERE INVISIBLE. An anonymous object with
 * `by` delegation (the detector wants the supertype followed by `{`), a nested
 * subclass of a production CLASS rather than a listed interface, an import
 * alias on the seam type (every detector is keyed on the spelling), a supertype
 * on the next line over an unlisted seam, a `java.lang.reflect.Proxy` whose
 * substitute type is not in the source at all, and a SAM lambda on a seam the
 * five-name lambda list omitted. The suite really did contain no double these
 * regexes could see, which is exactly what that claim was worth.
 *
 * Or's rule is ZERO doubles and this scan is the proof, so the proof cannot be
 * lexical. [structuralSites] reads the COMPILED test classes
 * (`app/build/classes/kotlin/test`) and asks what each class IS: `by`
 * delegation, nesting, aliases and multi-line supertypes all compile to the same
 * supertype list, a Proxy has to name `java/lang/reflect/Proxy` in its constant
 * pool to call it, and a SAM lambda - which compiles to an `invokedynamic` and
 * to no class at all - leaves the interface it implements in the call site's own
 * descriptor. Three structural detectors, none of them keyed on a spelling.
 *
 * The eight probes are kept as a Gradle source set that is COMPILED AND NEVER
 * RUN (`app/src/doubleProbes/kotlin`), and [everyRound18EvasionProbeIsCaught]
 * asserts the scan catches each shape. A double compiled into the test tree
 * would still be a double in the suite; the assertion over the test tree is
 * still zero.
 *
 * THE SEAMS. The lexical `samLambda` detector named five `fun interface`s by
 * hand, which is how it missed the sixth. Production now declares NO
 * `fun interface` and no single-abstract-method type at all - the zero-doubles
 * pass removed `TxPoster`, `ProcessRunner` and `ChainGateway`, and `RagStore`
 * stopped being `open` on 2026-09-08 (nothing subclassed it in production or in
 * the tests; `open` was a doorway with no room behind it). What remains
 * extendable is `ChromiaRepository`, the domain port every tool strategy is
 * written against, and `ToolStrategy` / `BaseToolStrategy`, which some seventy
 * production strategies implement: those are the program's own structure, not
 * injection points, and they are DETECTED rather than removed. [samTargets] is
 * derived from the compiled production API - single-method interfaces production
 * declares or accepts as a parameter - so a new seam is covered the day it
 * appears instead of the day someone remembers to add it to a list.
 *
 * The source detectors below are kept as they were. They are the second layer:
 * they see a double in a file that has not been compiled yet, and they cost
 * nothing.
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

    // ---- the structural scan -----------------------------------------------

    /** Where Kotlin puts each source set's classes. */
    private val classesRoot = RepoFiles.root.resolve("app/build/classes/kotlin")
    private val mainClasses: Map<String, ClassFacts> by lazy { ClassFiles.readTree(classesRoot.resolve("main")) }
    private val testClasses: Map<String, ClassFacts> by lazy { ClassFiles.readTree(classesRoot.resolve("test")) }
    private val probeClasses: Map<String, ClassFacts> by lazy {
        ClassFiles.readTree(classesRoot.resolve("doubleProbes"))
    }

    /**
     * A structural finding: a class file, not a line. The file name is the
     * `SourceFile` attribute where there is one, so a finding still points at a
     * .kt even when the class it names was never written down (an anonymous
     * object, a lambda).
     */
    data class Structural(val where: String, val kind: String, val what: String) {
        override fun toString() = "$where  $kind  $what"
    }

    /** The language's and the harness's own types are never a collaborator. */
    private val notACollaborator =
        listOf("java/", "javax/", "jdk/", "sun/", "kotlin/", "kotlinx/", "org/junit/", "org/opentest4j/")

    private fun isLanguageOrHarness(internalName: String) =
        notACollaborator.any { internalName.startsWith(it) }

    /** Reflection is only ever a substitute here; both halves of it are named. */
    private val proxyTypes = setOf("java/lang/reflect/Proxy", "java/lang/reflect/InvocationHandler")

    /**
     * The types a SAM lambda could substitute, DERIVED from the compiled
     * production API rather than listed:
     *
     *  - every interface production DECLARES with exactly one abstract instance
     *    method (there are none today, and that is the point - the seams were
     *    removed; if one comes back this covers it the same day);
     *  - every single-method interface production ACCEPTS AS A PARAMETER, which
     *    is what a test would have to implement to answer in a collaborator's
     *    place. `EmbeddingModel` - the type `BagOfWordsEmbeddingModel` stood in
     *    for before the zero-doubles pass deleted it - is here because
     *    `RagStore` takes one.
     *
     * The value is the reason, so a finding can say why the type is one.
     */
    private val samTargets: Map<String, String> by lazy {
        val targets = mutableMapOf<String, String>()
        mainClasses.values
            .filter { it.isInterface && it.abstractInstanceMethods == 1 }
            .forEach { targets[it.internalName] = "a single-method interface production declares" }
        mainClasses.values.asSequence()
            .flatMap { it.methodDescriptors.asSequence() }
            .flatMap { ClassFiles.parameterTypes(it).asSequence() }
            .distinct()
            .filter { it !in mainClasses && !isLanguageOrHarness(it) }
            .filter { isFunctionalInterface(it) }
            .forEach { targets[it] = "a single-method interface production accepts as a parameter" }
        targets
    }

    /**
     * Loads a THIRD-PARTY type to count its abstract methods. Nothing being
     * scanned is loaded - only the library types production's own descriptors
     * name, which are on this test's classpath by construction.
     */
    private fun isFunctionalInterface(internalName: String): Boolean = runCatching {
        val type = Class.forName(internalName.replace('/', '.'), false, javaClass.classLoader)
        type.isInterface && type.methods.count { method ->
            java.lang.reflect.Modifier.isAbstract(method.modifiers) &&
                !java.lang.reflect.Modifier.isStatic(method.modifiers)
        } == 1
    }.getOrDefault(false)

    /**
     * The first production type in [facts]'s transitive supertype closure, or
     * null. A supertype inside the scanned tree is followed through, so a test
     * class extending a test base class that extends production code is found;
     * anything else (JUnit, the Kotlin runtime, a library) ends that branch.
     */
    private fun productionSupertypeOf(facts: ClassFacts, scanned: Map<String, ClassFacts>): String? {
        val seen = HashSet<String>()
        val queue = ArrayDeque(listOfNotNull(facts.superName) + facts.interfaces)
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (!seen.add(next)) continue
            if (next in mainClasses) return next
            val known = scanned[next] ?: continue
            queue += listOfNotNull(known.superName) + known.interfaces
        }
        return null
    }

    private fun structuralSites(scanned: Map<String, ClassFacts>): List<Structural> {
        val sites = mutableListOf<Structural>()
        scanned.values.sortedBy { it.internalName }.forEach { facts ->
            val where = "${facts.sourceFile ?: "?"} [${facts.simpleName}]"
            productionSupertypeOf(facts, scanned)?.let { production ->
                sites += Structural(where, "SUPERTYPE", "stands in for $production")
            }
            val reflective = facts.referencedTypes.intersect(proxyTypes) +
                facts.invokeDynamicTargets.filter { it in proxyTypes }
            if (reflective.isNotEmpty()) {
                sites += Structural(where, "REFLECTION_PROXY", "builds a substitute at runtime: $reflective")
            }
            facts.invokeDynamicTargets.distinct().filter { it in samTargets }.forEach { target ->
                sites += Structural(where, "SAM_CONVERSION", "$target - ${samTargets[target]}")
            }
        }
        return sites
    }

    /** The scan is worthless if it is reading an empty or missing directory. */
    private fun requireCompiledTree() {
        assertTrue(
            testClasses.size > 500,
            "the structural scan read ${testClasses.size} compiled test classes from " +
                "${classesRoot.resolve("test")}. The suite has far more than that, so the scan is " +
                "looking at the wrong place or at a stale build - run it through `:app:test`, which " +
                "compiles the tree first."
        )
        assertTrue(
            mainClasses.size > 200,
            "the structural scan read ${mainClasses.size} compiled production classes; it cannot " +
                "tell a substitute from anything else without them"
        )
    }

    /**
     * ZERO, STRUCTURALLY. Six of round 18's eight doubles were invisible to the
     * regexes above and every one of them is a class whose supertype list, whose
     * constant pool or whose invokedynamic descriptor gives it away.
     */
    @Test
    fun thereAreNoTestDoublesInTheCompiledTestClasses() {
        requireCompiledTree()
        val sites = structuralSites(testClasses)
        assertTrue(
            sites.isEmpty(),
            "a test double appeared in the COMPILED test classes. This scan does not read spellings: " +
                "`by` delegation, a nested subclass, an import alias and a supertype on the next line " +
                "are all the same supertype list, a java.lang.reflect.Proxy has to name itself in the " +
                "constant pool, and a SAM lambda leaves the interface it implements in the call " +
                "site's descriptor. Drive the real thing (in process or live), or delete the test " +
                "together with the claim it carries and say so where it stood:\n  " +
                sites.joinToString("\n  ")
        )
    }

    /**
     * EVERY ONE OF THE EIGHT ROUND-18 PROBES IS CAUGHT, by name and by kind.
     *
     * The probes live in `app/src/doubleProbes/kotlin`: compiled by Gradle,
     * never run, never on the test classpath. Six of them were invisible to the
     * source regexes; the assertion here is what makes the zero above mean
     * something.
     */
    @Test
    fun everyRound18EvasionProbeIsCaught() {
        assertTrue(
            probeClasses.size >= 8,
            "the round-18 evasion probes are not compiled (found ${probeClasses.size} classes under " +
                "${classesRoot.resolve("doubleProbes")}). Without them the scan's silence over the " +
                "test tree is unproven - `:app:test` depends on compileDoubleProbesKotlin for exactly " +
                "this reason."
        )
        val sites = structuralSites(probeClasses)
        val expected = mapOf(
            "d1AnonymousObjectByDelegation" to "SUPERTYPE",
            "D2NestedSubclassOfAProductionClass" to "SUPERTYPE",
            "D3ImportAliasOnTheSeamType" to "SUPERTYPE",
            "D4SupertypeOnTheNextLine" to "SUPERTYPE",
            "D4bSupertypeOnTheNextLineUnlistedSeam" to "SUPERTYPE",
            "D5ReflectionProxy" to "REFLECTION_PROXY",
            "D6SamLambdaOnAnUnlistedSeam" to "SAM_CONVERSION",
            "d7ControlAPlainAnonymousObject" to "SUPERTYPE"
        )
        val missed = expected.filter { (probe, kind) ->
            sites.none { it.where.contains(probe) && it.kind == kind }
        }
        assertTrue(
            missed.isEmpty(),
            "the structural scan does not see ${missed.keys.sorted()} as ${missed.values.toSet()}. " +
                "These are the shapes adversary round 18 used to walk past the scan; a probe that " +
                "stops being caught is the scan going blind again, not the probe being wrong.\n" +
                "what the scan DID see:\n  " + sites.joinToString("\n  ")
        )
    }

    /**
     * AND THE SOURCE LAYER IS EXACTLY AS BLIND AS THE ROUND SAID.
     *
     * The control (`d7`) is caught by the `ANONYMOUS` regex; `d1`, one token
     * away, is not. This is the measurement that made the structural scan
     * necessary, asserted rather than remembered - so "the regexes are enough"
     * can never be believed again by accident.
     */
    @Test
    fun theSourceRegexesStillMissTheProbesTheStructuralScanCatches() {
        val probeSource = RepoFiles.root
            .resolve("app/src/doubleProbes/kotlin/org/chromia/doubleprobes/Probes.kt")
        assertTrue(Files.isRegularFile(probeSource), "the probe source is missing: $probeSource")
        val lines = stripComments(Files.readString(probeSource)).lines()
        val control = lines.single { it.contains("object : ToolStrategy {") }
        val evader = lines.single { it.contains("object : ChromiaRepository by real {") }
        assertTrue(anonymousObject.containsMatchIn(control), "the control must be caught lexically: $control")
        assertTrue(
            !anonymousObject.containsMatchIn(evader),
            "the lexical ANONYMOUS detector now matches `by` delegation. That is an improvement, not " +
                "a failure - update this test and say so; the structural scan is still the proof."
        )
        // The nested subclass of a production class carries none of the eleven
        // trigger words and is not one of the thirteen listed seam types.
        val nested = lines.single { it.contains(": BaseToolStrategy()") }
        assertTrue(!namedDouble.containsMatchIn(nested), "NAMED must miss the nested subclass: $nested")
        assertTrue(!implementsSeamInline.containsMatchIn(nested), "IMPLEMENTS must miss it: $nested")
    }

    /**
     * The derived seam list, and what it says about production today: there is no
     * `fun interface` left to SAM-convert, so [samTargets] is entirely made of
     * library types production ACCEPTS - and the embedding model is one of them.
     */
    @Test
    fun theSamTargetsAreDerivedFromProductionAndCoverTheEmbeddingModel() {
        requireCompiledTree()
        assertTrue(
            mainClasses.values.none { it.isInterface && it.abstractInstanceMethods == 1 },
            "production declares a single-method interface again: " +
                mainClasses.values.filter { it.isInterface && it.abstractInstanceMethods == 1 }
                    .map { it.internalName } +
                ". That is a seam a test can implement with a lambda. Either it is a real " +
                "configuration point - say so here - or it goes, the way TxPoster, ProcessRunner " +
                "and ChainGateway went."
        )
        assertTrue(
            "dev/langchain4j/model/embedding/EmbeddingModel" in samTargets,
            "the embedding model is a constructor parameter of RagStore and a single-method " +
                "interface, so a lambda can answer in its place - the scan must know that without " +
                "being told: ${samTargets.keys.sorted()}"
        )
        println("derived SAM targets (${samTargets.size}): ${samTargets.keys.sorted()}")
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
