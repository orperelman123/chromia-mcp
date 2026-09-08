package org.chromia

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path

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
 * lexical. [structuralSites] reads the COMPILED test classes and asks what each
 * class IS: `by` delegation, nesting, aliases and multi-line supertypes all
 * compile to the same supertype list, a runtime substitute has to CALL a JDK
 * factory that manufactures one, and a SAM lambda - which compiles to an
 * `invokedynamic` and to no class at all - leaves the interface it implements in
 * the call site's own descriptor. Three structural detectors, none of them keyed
 * on a spelling.
 *
 * WHAT IS SCANNED, WHAT A SEAM IS, AND WHAT MANUFACTURES ONE - the three answers
 * adversary round 19 (section 3) took apart, each of which had been a literal.
 *
 *  - SCANNED: [testTrees] is the `test` task's own resolved runtime classpath
 *    minus the dependency jars and minus production's output - not
 *    `app/build/classes/kotlin`, which left `app/build/classes/java/test` (the
 *    java plugin's convention, and the test source set compiles Java) and
 *    `app/build/resources/test` outside the proof entirely (r19d5). A class file
 *    is recognised by its MAGIC and not by a `.class` suffix, so bytes checked in
 *    under any name are read too, and
 *    [theStructuralScanReadsEveryDirectoryOnTheTestRuntimeClasspath] asserts the
 *    two sets are the same one, so a source set added tomorrow cannot appear
 *    unscanned. `RepoFiles.testSources()` walks `.java` as well as `.kt` for the
 *    same reason.
 *  - A SEAM: [dependencySeams] is every interface or abstract class production's
 *    own bytecode NAMES - constant pool, method and field descriptors, and the
 *    types its locals are declared as - not only the types production DECLARES.
 *    A named class over `ContentRetriever`, langchain4j's retrieval interface and
 *    the RAG seam `RagStore` builds into a local, answers on a collaborator's
 *    behalf exactly as `BagOfWordsEmbeddingModel` did, and who wrote the
 *    `interface` keyword has nothing to do with it (r19d1). [samTargets] follows
 *    the same set (r19d2), so the identical lambda is a finding over
 *    `ContentRetriever` and over `EmbeddingModel` alike.
 *  - MANUFACTURING: [manufacturingCalls] is every JDK entry point that turns
 *    something which is not a class into a live instance -
 *    `Proxy.newProxyInstance`, `MethodHandleProxies.asInterfaceInstance`,
 *    `Lookup.defineClass` / `defineHiddenClass` /
 *    `defineHiddenClassWithClassData`, `ClassLoader.defineClass`,
 *    `Unsafe.defineAnonymousClass` - read off the constant pool as CALLS, the way
 *    `Proxy` alone was read before (r19d4, r19d5). `MethodHandles.lookup()` on
 *    its own manufactures nothing and is not one.
 *
 * The twelve probes - round 18's eight and round 19's four with their control -
 * are kept as a Gradle source set that is COMPILED AND NEVER RUN
 * (`app/src/doubleProbes/kotlin`), and [everyRound18EvasionProbeIsCaught] and
 * [everyRound19EvasionProbeIsCaught] assert the scan catches each shape. A double
 * compiled into the test tree would still be a double in the suite; the assertion
 * over the test tree is still zero.
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
 * derived from the compiled production API - every single-method interface
 * production declares, accepts as a parameter, or otherwise DEPENDS ON - so a new
 * seam is covered the day it appears instead of the day someone remembers to add
 * it to a list. The third clause is round 19's: "declares or accepts as a
 * parameter" made the identical lambda a finding over `EmbeddingModel` and
 * invisible over `ContentRetriever`, on a distinction that says nothing about
 * whether it answers in a collaborator's place.
 *
 * The two INJECTION POINTS that are left are named rather than removed, because
 * both hand back the real thing. `ToolExecutor(ragStore =, ragStoreFactory =)`
 * is how a caller supplies an already-built store instead of having one lazily
 * downloaded, and the tests pass [TestDocsIndex]'s REAL `RagStore` over a
 * two-segment index through it; `RagStore(initialStore =, localEmbeddingsPath =,
 * remoteUrls =, embeddingModel =)` are the same kind of thing, and the KDoc on
 * `remoteUrls` argues its case where the parameter is. Neither can be handed a
 * substitute any more: `RagStore` is final, so the only way to answer in its
 * place is to implement or extend something - which is what the structural scan
 * reads.
 *
 * The source detectors below are kept as they were. They are the second layer:
 * they see a double in a file that has not been compiled yet, and they cost
 * nothing.
 */
class NoTestDoublesTest {

    /**
     * A JUnit TEST class is not a double, whatever the attack it pins is called.
     *
     * Round 19 shipped `Round19FakeUpstreamWarningTest` (a fake upstream warning
     * is what it ATTACKS) and `Round19DoubleEvasionTest` (test doubles are what it
     * is ABOUT), and [namedDouble] flagged both on the words `Fake` and `Double`
     * in their names - a red that says nothing about the suite. It is the same
     * mistake as reacting to prose about a double, which is why comments are
     * stripped: the detector reads a name for what the class IS, and a class whose
     * name ends in `Test` is the test, not the substitute. Both are silent under
     * the structural scan, which is the proof.
     *
     * The carve-out is on the NAME only and it costs nothing: a substitute called
     * `FakeChainTest` would still have to implement or extend something to answer
     * in a collaborator's place, and that is a supertype list
     * [thereAreNoTestDoublesInTheCompiledTestClasses] reads.
     * [everyDetectorStillMatchesTheShapeItLooksFor] pins its exact width so it
     * cannot quietly grow.
     */
    private fun isTestClassName(declared: String) = declared.endsWith("Test")

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
                namedDouble.find(line)?.let { match ->
                    val declared = match.groupValues[1]
                    if (!isTestClassName(declared)) sites += Site(name, n, "NAMED", declared)
                }
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

    /**
     * A classpath entry that is a dependency ARTIFACT rather than one of this
     * build's own output directories.
     *
     * A regular file is one whatever it is called - and on this build that is not
     * only jars: `net.postchain:directory-chain`'s `.pom` is on the test runtime
     * classpath (measured 2026-09-09), and a first cut that named `.jar` and
     * `.zip` recorded it as a tree. The suffixes are kept for the other half of
     * the rule: an output directory a source set has not written to yet is not a
     * regular file either, and it must stay in the list rather than vanish from
     * it, so anything that is neither a file on disk nor an artifact spelling is a
     * tree.
     */
    private fun isDependencyArtifact(path: Path): Boolean =
        Files.isRegularFile(path) ||
            path.toString().lowercase().let { name ->
                listOf(".jar", ".zip", ".pom", ".module", ".klib", ".aar").any { name.endsWith(it) }
            }

    private fun required(paths: List<Path>?, property: String, what: String): List<Path> =
        paths ?: error(
            "`$property` is missing, so this scan does not know $what. The `test` task in " +
                "app/build.gradle.kts writes those paths from its OWN classpath into " +
                "app/build/zero-doubles/scan-paths.tsv - run the scan through `:app:test`. It may not " +
                "guess a directory instead: a guessed one is exactly what adversary round 19 (r19d5) " +
                "walked past."
        )

    /**
     * EVERY DIRECTORY THE TEST JVM CAN LOAD A CLASS FROM, except production's own
     * output. Not a literal: it is the `test` task's resolved runtime classpath
     * with the dependency jars and the `main` source set's output removed, so
     * `app/build/classes/java/test` and `app/build/resources/test` are in it
     * because GRADLE puts them there, and a source set added tomorrow is in it
     * the day it is added.
     */
    private val testTrees: List<Path> by lazy {
        val classpath = required(
            RepoFiles.testRuntimeClasspath, "chromia.test.scanpaths[classpath]", "where a test class can come from"
        )
        val production = required(
            RepoFiles.productionOutput, "chromia.test.scanpaths[production.output]", "which of those trees is production's"
        ).toSet()
        classpath.filterNot { isDependencyArtifact(it) }
            .filterNot { it in production }
            .distinct()
            .sortedBy { it.toString() }
    }

    /** The compiled production classes - what a substitute stands in FOR. */
    private val productionTrees: List<Path> by lazy {
        required(
            RepoFiles.productionClasses, "chromia.test.scanpaths[production.classes]", "what a substitute would stand in for"
        )
    }

    /** The evasion probes: compiled, never run, never on the test classpath. */
    private val probeTrees: List<Path> by lazy {
        required(
            RepoFiles.doubleProbeClasses, "chromia.test.scanpaths[doubleprobes.classes]", "where the evasion probes compiled to"
        )
    }

    private val mainClasses: Map<String, ClassFacts> by lazy { ClassFiles.readTrees(productionTrees) }
    private val testClasses: Map<String, ClassFacts> by lazy { ClassFiles.readTrees(testTrees) }
    private val probeClasses: Map<String, ClassFacts> by lazy { ClassFiles.readTrees(probeTrees) }

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
     * THE JDK'S OWN FACTORIES FOR AN IMPLEMENTATION THAT HAS NO SOURCE, as
     * `owner#name` in the constant pool - the same way `Proxy` has always been
     * read, one level down.
     *
     * `java.lang.reflect.Proxy` is not the only way to manufacture one. Adversary
     * round 19 (r19d4) built a substitute for `EmbeddingModel` with
     * `MethodHandleProxies.asInterfaceInstance` and never mentioned `Proxy`, and
     * (r19d5) defined one from BYTES with `Lookup.defineHiddenClass`, which leaves
     * no class file anywhere for the supertype detector to read. Every entry point
     * that turns something which is not a class into a live instance is listed
     * here, and the list is of CALLS, not of types: `MethodHandles.lookup()` on
     * its own manufactures nothing, and `javaClass.classLoader` is not
     * `ClassLoader.defineClass`.
     */
    private val manufacturingCalls = setOf(
        "java/lang/reflect/Proxy#newProxyInstance",
        "java/lang/invoke/MethodHandleProxies#asInterfaceInstance",
        "java/lang/invoke/MethodHandles\$Lookup#defineClass",
        "java/lang/invoke/MethodHandles\$Lookup#defineHiddenClass",
        "java/lang/invoke/MethodHandles\$Lookup#defineHiddenClassWithClassData",
        "java/lang/ClassLoader#defineClass",
        "sun/misc/Unsafe#defineAnonymousClass",
        "jdk/internal/misc/Unsafe#defineAnonymousClass"
    )

    /**
     * EVERY EXTENSION POINT PRODUCTION DEPENDS ON - not only the ones it owns.
     *
     * Until 2026-09-09 a finding needed a supertype in `mainClasses`, and
     * adversary round 19 (r19d1) walked past that with a named class over
     * `ContentRetriever`: a langchain4j interface `RagStore` builds into a LOCAL
     * and the RAG seam of the whole docs path. A substitute for it answers on a
     * collaborator's behalf exactly as `BagOfWordsEmbeddingModel` did, and who
     * DECLARED the interface has nothing to do with it.
     *
     * So the seam set is derived from what production's own bytecode NAMES -
     * every `CONSTANT_Class` in the pool, every type in a method or field
     * descriptor, and every type a local was declared as (which is where a seam
     * production merely holds appears under its own name) - kept where the type
     * is an interface or an abstract class, which are the two shapes something can
     * be substituted THROUGH. Annotations and enums are neither. The language's
     * and the harness's own types are excluded the way they always were, so
     * `java.io.Closeable` on a real TCP endpoint is not a finding.
     *
     * The real library implementation is never in the scan: only the test trees
     * are scanned, and a class that comes out of a dependency jar is not in one.
     */
    private val dependencySeams: Map<String, String> by lazy {
        val referenced = mutableSetOf<String>()
        mainClasses.values.forEach { facts ->
            referenced += facts.referencedTypes
            referenced += facts.interfaces
            facts.superName?.let { referenced += it }
            facts.methodDescriptors.forEach { referenced += ClassFiles.objectTypes(it) }
            facts.fieldDescriptors.forEach { referenced += ClassFiles.objectTypes(it) }
            referenced += facts.localVariableTypes
        }
        referenced.asSequence()
            .filterNot { it in mainClasses || isLanguageOrHarness(it) }
            .mapNotNull { name -> extensionPointKind(name)?.let { name to it } }
            .toMap()
    }

    /**
     * Loads a THIRD-PARTY type to ask whether anything can be substituted through
     * it. Nothing being scanned is loaded - only the library types production's
     * own bytecode names, which are on this test's classpath by construction.
     */
    private fun extensionPointKind(internalName: String): String? = runCatching {
        val type = Class.forName(internalName.replace('/', '.'), false, javaClass.classLoader)
        when {
            type.isAnnotation || type.isEnum -> null
            type.isInterface -> "an interface production depends on"
            Modifier.isAbstract(type.modifiers) -> "an abstract class production depends on"
            else -> null
        }
    }.getOrNull()

    private val librarySupertypes = mutableMapOf<String, List<String>>()

    /** A library type's own supertypes, so a closure does not stop at the jar boundary. */
    private fun librarySupertypesOf(internalName: String): List<String> =
        librarySupertypes.getOrPut(internalName) {
            runCatching {
                val type = Class.forName(internalName.replace('/', '.'), false, javaClass.classLoader)
                val supertypes = mutableListOf<String>()
                type.superclass?.let { supertypes += it.name.replace('.', '/') }
                type.interfaces.forEach { supertypes += it.name.replace('.', '/') }
                supertypes.toList()
            }.getOrDefault(emptyList())
        }

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
     *
     * ROUND 19 (r19d2) widened the first clause. "Declares or accepts as a
     * parameter" caught `EmbeddingModel` and missed `ContentRetriever`, which
     * production only ever holds in a LOCAL - so the identical lambda was a
     * finding over one seam and invisible over the other, on a difference that
     * says nothing about whether it is a substitute. Every single-method
     * interface in [dependencySeams] is a target now, and the two narrower
     * reasons are kept because they are more specific and read better in a
     * finding.
     */
    private val samTargets: Map<String, String> by lazy {
        val targets = mutableMapOf<String, String>()
        dependencySeams.keys
            .filter { isFunctionalInterface(it) }
            .forEach { targets[it] = "a single-method interface production depends on" }
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
     * The first SEAM in [facts]'s transitive supertype closure, with the reason it
     * is one, or null. A supertype inside the scanned tree is followed through, so
     * a test class extending a test base class that extends a seam is found, and a
     * supertype that comes out of a dependency jar is followed through the JAR's
     * own hierarchy - a class three library levels above the one that was written
     * down is still the type being answered for. The language's and the harness's
     * types end a branch.
     */
    private fun seamSupertypeOf(facts: ClassFacts, scanned: Map<String, ClassFacts>): Pair<String, String>? {
        val seen = HashSet<String>()
        val queue = ArrayDeque(listOfNotNull(facts.superName) + facts.interfaces)
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (!seen.add(next)) continue
            if (isLanguageOrHarness(next)) continue
            if (next in mainClasses) return next to "a type production owns"
            dependencySeams[next]?.let { return next to it }
            val known = scanned[next]
            if (known != null) {
                queue += listOfNotNull(known.superName) + known.interfaces
            } else {
                queue += librarySupertypesOf(next)
            }
        }
        return null
    }

    private fun structuralSites(scanned: Map<String, ClassFacts>): List<Structural> {
        val sites = mutableListOf<Structural>()
        scanned.values.sortedBy { it.internalName }.forEach { facts ->
            val where = "${facts.sourceFile ?: "?"} [${facts.simpleName}]"
            seamSupertypeOf(facts, scanned)?.let { (seam, why) ->
                sites += Structural(where, "SUPERTYPE", "stands in for $seam - $why")
            }
            val reflective = (
                facts.referencedTypes.intersect(proxyTypes) +
                    facts.invokeDynamicTargets.filter { it in proxyTypes } +
                    facts.methodReferences.filter { it in manufacturingCalls }
                ).sorted()
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
                "${testTrees.map { RepoFiles.relative(it) }}. The suite has far more than that, so the " +
                "scan is looking at the wrong place or at a stale build - run it through `:app:test`, " +
                "which compiles the tree first."
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
                "${probeTrees.map { RepoFiles.relative(it) }}). Without them the scan's silence over the " +
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

    /**
     * AND THE FOUR ROUND-19 PROBES ARE CAUGHT BY THE SHIPPED SCAN ITSELF.
     *
     * [Round19DoubleEvasionTest] pins these too, through a faithful copy of the
     * detectors - but a copy cannot notice the shipped scan narrowing back, since
     * the copy would narrow with it and the round-18 table would still pass. This
     * assertion is against the real thing, in the file the widening lives in.
     *
     * Each one substitutes a collaborator production does NOT own: a named class
     * and a SAM lambda over `ContentRetriever` (a langchain4j interface `RagStore`
     * only ever holds in a local), a `MethodHandleProxies` instance of
     * `EmbeddingModel`, and a class defined from bytes with
     * `Lookup.defineHiddenClass`. `r19d6` is the control - the same named class
     * over a production type - so each is one substitution away and not a
     * different experiment.
     */
    @Test
    fun everyRound19EvasionProbeIsCaught() {
        assertTrue(
            probeClasses.size >= 12,
            "the round-19 evasion probes are not compiled (found ${probeClasses.size} classes under " +
                "${probeTrees.map { RepoFiles.relative(it) }}); round 18 contributes eight and round " +
                "19 four more"
        )
        val sites = structuralSites(probeClasses)
        val expected = mapOf(
            "R19d1NamedClassOverAThirdPartySeam" to "SUPERTYPE",
            "R19d2SamLambdaOnANonParameterSeam" to "SAM_CONVERSION",
            "R19d4MethodHandleProxy" to "REFLECTION_PROXY",
            "R19d5RuntimeDefinedClass" to "REFLECTION_PROXY",
            "R19d6ControlNamedClassOverAProductionSeam" to "SUPERTYPE"
        )
        val missed = expected.filter { (probe, kind) ->
            sites.none { it.where.contains(probe) && it.kind == kind }
        }
        assertTrue(
            missed.isEmpty(),
            "the structural scan does not see ${missed.keys.sorted()} as ${missed.values.toSet()}. A " +
                "scan that proves ZERO doubles has to see a substitute for a LIBRARY collaborator too: " +
                "production's seams are typed with library interfaces, and a test that answers in one " +
                "of their places is a double whoever declared the type.\n" +
                "what the scan DID see:\n  " + sites.joinToString("\n  ")
        )
    }

    /**
     * THE SCAN READS EVERY DIRECTORY THE TEST RUNTIME LOADS A CLASS FROM.
     *
     * `classesRoot` used to be the literal `app/build/classes/kotlin`, and
     * adversary round 19 (r19d5) pointed out what that leaves outside: the test
     * source set compiles Java too, into `app/build/classes/java/test`, and
     * `app/build/resources/test` is on the classpath as well, so a double written
     * in the other language of the same source set - or simply checked in as
     * bytes - was proven absent by nothing.
     *
     * The fix is not a longer list. [testTrees] is the `test` task's own resolved
     * runtime classpath minus the dependency jars and minus production's output,
     * and this test asserts the two sets are the same one: every directory the
     * JVM can load from is either production or scanned. A source set added
     * tomorrow appears on that classpath and is therefore scanned, without anyone
     * remembering to say so.
     */
    @Test
    fun theStructuralScanReadsEveryDirectoryOnTheTestRuntimeClasspath() {
        val classpath = required(
            RepoFiles.testRuntimeClasspath, "chromia.test.scanpaths[classpath]", "where a test class can come from"
        )
        val production = required(
            RepoFiles.productionOutput, "chromia.test.scanpaths[production.output]", "which of those trees is production's"
        ).toSet()
        val directories = classpath.filterNot { isDependencyArtifact(it) }.distinct()
        val unaccounted = directories.filterNot { it in production || it in testTrees }
        assertTrue(
            unaccounted.isEmpty(),
            "the test JVM can load a class from ${unaccounted.map { RepoFiles.relative(it) }} and the " +
                "zero-doubles scan does not read it. Every classpath directory is either production's " +
                "own output or a tree this scan searches; there is no third kind."
        )
        assertTrue(
            testTrees.none { it in production },
            "the scan is reading production's own output as a tree to search for substitutes in: " +
                "${testTrees.filter { it in production }.map { RepoFiles.relative(it) }}. Production " +
                "is what a substitute stands in FOR."
        )
        val relative = testTrees.map { RepoFiles.relative(it) }
        listOf(
            "app/build/classes/kotlin/test",
            "app/build/classes/java/test",
            "app/build/resources/test"
        ).forEach { tree ->
            assertTrue(
                tree in relative,
                "$tree is not among the trees the scan reads ($relative). It is on the test runtime " +
                    "classpath - the java plugin's conventional output and the test resources - and " +
                    "round 19 (r19d5) hid a double in exactly the two that were missing."
            )
        }
        println("scanned test trees (${relative.size}): $relative")
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
        // The carve-out, at exactly the width the KDoc claims: the name says what
        // the class IS, and a class ending in `Test` is the test.
        assertTrue(!isTestClassName("FakeChain"), "a double is not exempted by the carve-out")
        assertTrue(!isTestClassName("MockLedger"), "a double is not exempted by the carve-out")
        assertTrue(isTestClassName("Round19FakeUpstreamWarningTest"), "a JUnit test class is exempted")
        assertTrue(isTestClassName("Round19DoubleEvasionTest"), "a JUnit test class is exempted")
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
