package org.chromia

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.lang.reflect.Modifier
import java.nio.file.Path

/**
 * ROUND 19, THE ZERO-DOUBLES PROOF: what the structural scan is ANCHORED ON.
 *
 * [NoTestDoublesTest] reads compiled classes and finds a double three ways: a
 * SEAM in the transitive supertype closure, a JDK factory that manufactures an
 * implementation, and an invokedynamic whose target interface is substitutable.
 * Every one of round 18's eight probes is a substitute for a type production
 * OWNS, which is why all eight were caught.
 *
 * Round 19 substituted the collaborators production does NOT own, and all four
 * walked past the scan as it stood on 2026-09-08. Each consequence followed from
 * one of the scan's own definitions being a literal rather than a derivation:
 *
 *  - `productionSupertypeOf` returned the first supertype found in `mainClasses`
 *    and ENDED any branch that left the scanned tree without reaching one, so a
 *    named class over a THIRD-PARTY interface had no finding at all - and
 *    `ContentRetriever` is a real injection point, the RAG seam `RagStore` builds
 *    (`r19d1`).
 *  - `samTargets` was derived from what production DECLARES or ACCEPTS AS A
 *    PARAMETER. `ContentRetriever` is only ever a LOCAL in production, so it was
 *    in no method descriptor, in no `samTargets`, and a SAM lambda over it was
 *    invisible where the same lambda over `EmbeddingModel` was caught (`r19d2`).
 *  - `proxyTypes` was exactly two names, so `MethodHandleProxies`, which builds
 *    the same runtime substitute without naming `Proxy`, was not in it (`r19d4`).
 *  - a class DEFINED AT RUNTIME leaves no class file, and the two directories its
 *    bytes could be checked into - `app/build/classes/java/test` and
 *    `app/build/resources/test` - were outside `classesRoot` (`r19d5`).
 *
 * `r19d6` is the CONTROL: the identical named class over a PRODUCTION type,
 * which must be caught as SUPERTYPE, so each probe above is one substitution
 * away and not a different experiment.
 *
 * WHAT BECAME OF THEM. All four are CLOSED, by widening the shipped scan and not
 * by touching these pins: the seam set is now every interface or abstract class
 * production's bytecode NAMES (constant pool, method and field descriptors, local
 * variable types), `samTargets` follows that same set, the manufacturing entry
 * points are read off the constant pool as CALLS (`Proxy.newProxyInstance`,
 * `MethodHandleProxies.asInterfaceInstance`, `Lookup.defineClass` /
 * `defineHiddenClass` / `defineHiddenClassWithClassData`,
 * `ClassLoader.defineClass`, `Unsafe.defineAnonymousClass`), and the scanned
 * trees are the `test` task's own runtime classpath minus the dependency jars and
 * minus production's output. The frozen evidence beside this file records
 * `caught: true` for all five rows now; re-freezing was the deliberate act the
 * recorder demands, and `closed_by` on each row says what did it.
 *
 * WHY THE DETECTORS ARE RE-IMPLEMENTED HERE. [NoTestDoublesTest]'s scan is
 * private, and running a second scoreboard through it would mean opening it up.
 * So [structuralSites] below is a faithful copy, and
 * [theCopiedScanAgreesWithTheShippedOneOnAllEightRound18Probes] proves it is
 * faithful the only way that means anything: it must reproduce the shipped
 * scan's own expected verdict for all eight round-18 probes, by name and by
 * kind. A copy that has drifted fails there first. It is kept faithful to the
 * WIDER scan for the same reason it was written - and because a copy can only
 * ever confirm the shipped scan and never replace it (a copy would narrow along
 * with it), `NoTestDoublesTest` carries its own `everyRound19EvasionProbeIsCaught`
 * against the real detectors.
 */
class Round19DoubleEvasionTest {

    private data class Site(val where: String, val kind: String, val what: String) {
        override fun toString() = "$where  $kind  $what"
    }

    private data class Probe(val name: String, val kind: String, val why: String, val closedBy: String)

    // ---- the trees, as the shipped scan derives them ------------------------

    private fun isDependencyArchive(path: Path): Boolean =
        path.toString().let { it.endsWith(".jar", ignoreCase = true) || it.endsWith(".zip", ignoreCase = true) }

    private fun required(paths: List<Path>?, property: String): List<Path> =
        paths ?: error(
            "`$property` is missing; the `test` task in app/build.gradle.kts writes those paths from " +
                "its own classpath into app/build/zero-doubles/scan-paths.tsv, so run this through " +
                "`:app:test`"
        )

    private val testTrees: List<Path> by lazy {
        val classpath = required(RepoFiles.testRuntimeClasspath, "chromia.test.scanpaths[classpath]")
        val production = required(RepoFiles.productionOutput, "chromia.test.scanpaths[production.output]").toSet()
        classpath.filterNot { isDependencyArchive(it) }
            .filterNot { it in production }
            .distinct()
            .sortedBy { it.toString() }
    }

    private val mainClasses: Map<String, ClassFacts> by lazy {
        ClassFiles.readTrees(required(RepoFiles.productionClasses, "chromia.test.scanpaths[production.classes]"))
    }
    private val probeClasses: Map<String, ClassFacts> by lazy {
        ClassFiles.readTrees(required(RepoFiles.doubleProbeClasses, "chromia.test.scanpaths[doubleprobes.classes]"))
    }

    // ---- the detectors, copied ---------------------------------------------

    private val notACollaborator =
        listOf("java/", "javax/", "jdk/", "sun/", "kotlin/", "kotlinx/", "org/junit/", "org/opentest4j/")

    private fun isLanguageOrHarness(internalName: String) = notACollaborator.any { internalName.startsWith(it) }

    private val proxyTypes = setOf("java/lang/reflect/Proxy", "java/lang/reflect/InvocationHandler")

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

    private fun extensionPointKind(internalName: String): String? = runCatching {
        val type = Class.forName(internalName.replace('/', '.'), false, javaClass.classLoader)
        when {
            type.isAnnotation || type.isEnum -> null
            type.isInterface -> "an interface production depends on"
            Modifier.isAbstract(type.modifiers) -> "an abstract class production depends on"
            else -> null
        }
    }.getOrNull()

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

    private fun isFunctionalInterface(internalName: String): Boolean = runCatching {
        val type = Class.forName(internalName.replace('/', '.'), false, javaClass.classLoader)
        type.isInterface && type.methods.count { method ->
            Modifier.isAbstract(method.modifiers) && !Modifier.isStatic(method.modifiers)
        } == 1
    }.getOrDefault(false)

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

    private val librarySupertypes = mutableMapOf<String, List<String>>()

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

    private fun structuralSites(scanned: Map<String, ClassFacts>): List<Site> {
        val sites = mutableListOf<Site>()
        scanned.values.sortedBy { it.internalName }.forEach { facts ->
            val where = "${facts.sourceFile ?: "?"} [${facts.simpleName}]"
            seamSupertypeOf(facts, scanned)?.let { (seam, why) ->
                sites += Site(where, "SUPERTYPE", "stands in for $seam - $why")
            }
            val reflective = (
                facts.referencedTypes.intersect(proxyTypes) +
                    facts.invokeDynamicTargets.filter { it in proxyTypes } +
                    facts.methodReferences.filter { it in manufacturingCalls }
                ).sorted()
            if (reflective.isNotEmpty()) {
                sites += Site(where, "REFLECTION_PROXY", "builds a substitute at runtime: $reflective")
            }
            facts.invokeDynamicTargets.distinct().filter { it in samTargets }.forEach { target ->
                sites += Site(where, "SAM_CONVERSION", "$target - ${samTargets[target]}")
            }
        }
        return sites
    }

    private fun requireProbes() {
        assertTrue(
            probeClasses.size >= 12,
            "the evasion probes are not compiled (found ${probeClasses.size} classes); :app:test " +
                "depends on compileDoubleProbesKotlin, and round 18 contributes eight with round 19 four"
        )
        assertTrue(
            mainClasses.size > 200,
            "production classes are not compiled (found ${mainClasses.size}) - every detector here is " +
                "anchored on them"
        )
    }

    /**
     * THE COPY IS FAITHFUL. The eight round-18 probes and their kinds are
     * [NoTestDoublesTest]'s own table; if this file's detectors have drifted from
     * the shipped ones, this test fails before any round-19 verdict is read.
     */
    @Test
    fun theCopiedScanAgreesWithTheShippedOneOnAllEightRound18Probes() {
        requireProbes()
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
        val missed = expected.filterNot { (probe, kind) ->
            sites.any { it.where.contains(probe) && it.kind == kind }
        }
        assertEquals(
            emptyMap<String, String>(), missed,
            "the detectors in THIS file no longer agree with NoTestDoublesTest's own table, so " +
                "nothing it says about the round-19 probes can be trusted. Re-copy them.\n" +
                "what this scan saw:\n  " + sites.joinToString("\n  ")
        )
    }

    /**
     * EVERY ROUND-19 EVASION PROBE MUST BE CAUGHT. The control is, and it is one
     * substitution away from each of the others.
     */
    @Test
    fun everyRound19EvasionProbeIsCaught() {
        requireProbes()
        val sites = structuralSites(probeClasses)
        val expected = listOf(
            Probe(
                "R19d1NamedClassOverAThirdPartySeam", "SUPERTYPE",
                "a named class implementing dev.langchain4j.rag.content.retriever.ContentRetriever, " +
                    "the RAG seam RagStore builds - productionSupertypeOf ends the branch because " +
                    "the supertype is not one of production's own types",
                "the seam set is every interface or abstract class production's bytecode NAMES - " +
                    "constant pool, method and field descriptors, local variable types - so a " +
                    "collaborator production DEPENDS ON is a seam whoever declared it"
            ),
            Probe(
                "R19d2SamLambdaOnANonParameterSeam", "SAM_CONVERSION",
                "the same seam as a SAM lambda - ContentRetriever is only ever a LOCAL in " +
                    "production, so it is in no method descriptor and so in no samTargets, while " +
                    "the identical lambda over EmbeddingModel (D6) is caught",
                "samTargets follows the same dependency seam set, so every single-method interface " +
                    "production depends on is a target - not only the ones it declares or accepts"
            ),
            Probe(
                "R19d4MethodHandleProxy", "REFLECTION_PROXY",
                "MethodHandleProxies.asInterfaceInstance builds the same runtime substitute for " +
                    "EmbeddingModel without naming Proxy, and proxyTypes is two names",
                "every JDK entry point that manufactures an implementation is read off the constant " +
                    "pool as a CALL - Proxy.newProxyInstance, MethodHandleProxies.asInterfaceInstance, " +
                    "Lookup.defineClass/defineHiddenClass/defineHiddenClassWithClassData, " +
                    "ClassLoader.defineClass, Unsafe.defineAnonymousClass"
            ),
            Probe(
                "R19d5RuntimeDefinedClass", "REFLECTION_PROXY",
                "MethodHandles.Lookup.defineHiddenClass defines the substitute from bytes - there " +
                    "is no class file in any scanned directory for it",
                "the defining CALL is read the way Proxy always was, and the scanned trees are the " +
                    "test task's own runtime classpath minus the jars and production's output, so " +
                    "app/build/classes/java/test and app/build/resources/test are read too - by " +
                    "class-file MAGIC, not by a .class suffix"
            ),
            Probe(
                "R19d6ControlNamedClassOverAProductionSeam", "SUPERTYPE",
                "THE CONTROL: the identical named class over a PRODUCTION type, which must be caught",
                "unchanged - the control was caught before the widening and is caught after it"
            )
        )
        val rows: JsonArray = buildJsonArray {
            expected.forEach { probe ->
                val hit = sites.firstOrNull { it.where.contains(probe.name) && it.kind == probe.kind }
                add(
                    buildJsonObject {
                        put("probe", probe.name)
                        put("expected_kind", probe.kind)
                        put("caught", hit != null)
                        put("evidence", hit?.toString() ?: "")
                        put("why", probe.why)
                        put("closed_by", probe.closedBy)
                    }
                )
            }
        }
        Round19Evidence.record("doubles/structural.json", rows)
        val missed = expected.filterNot { probe ->
            sites.any { it.where.contains(probe.name) && it.kind == probe.kind }
        }.map { "${it.name} (expected ${it.kind}): ${it.why}" }
        assertAll(
            Executable {
                assertEquals(
                    emptyList<String>(), missed,
                    "the structural scan walked past ${missed.size} substitute(s) in the compiled " +
                        "probe tree. A scan that proves ZERO doubles has to see a substitute for a " +
                        "LIBRARY collaborator too - production's seams are typed with library " +
                        "interfaces, and a test that answers in one of their places is a double " +
                        "whoever declared the type.\n  " + missed.joinToString("\n  ") +
                        "\nwhat the scan DID see:\n  " + sites.joinToString("\n  ")
                )
            },
            Executable { Round19Evidence.assertFrozen("doubles/structural.json", rows) }
        )
    }

    /**
     * THE TREES THE SCAN NEVER OPENED - and now does.
     *
     * `classesRoot` was `app/build/classes/kotlin`, so `app/build/classes/java/test`
     * - where any `.java` under `app/src/test/java` compiles to, by the java
     * plugin's own convention with no `srcDirs` override anywhere in
     * `app/build.gradle.kts` - was never read, and `app/build/resources/test` was
     * not read either, so a `defineClass`-style evasion could simply check its
     * bytes in. `RepoFiles.testSources()` walked `app/src/test/kotlin` filtering
     * `.kt`, so the source layer was blind to both as well.
     *
     * The scan holds no list of directories at all now: it reads the `test` task's
     * OWN runtime classpath, drops the dependency jars and production's output,
     * and searches what is left. This test asserts the two round-19 directories
     * are in that set - and, more importantly, that NOTHING on the classpath is
     * outside it, which is the property a literal could never have.
     */
    @Test
    fun theScanReadsEveryDirectoryTheTestRuntimeLoadsClassesFrom() {
        val classpath = required(RepoFiles.testRuntimeClasspath, "chromia.test.scanpaths[classpath]")
        val production = required(RepoFiles.productionOutput, "chromia.test.scanpaths[production.output]").toSet()
        val directories = classpath.filterNot { isDependencyArchive(it) }.distinct()
        val scanned = testTrees.map { RepoFiles.relative(it) }.sorted()
        val unaccounted = directories
            .filterNot { it in production || it in testTrees }
            .map { RepoFiles.relative(it) }
            .sorted()
        val rows = buildJsonArray {
            scanned.forEach { directory ->
                add(
                    buildJsonObject {
                        put("directory", directory)
                        put("scanned_by_NoTestDoublesTest", true)
                    }
                )
            }
        }
        Round19Evidence.record("doubles/scanned-trees.json", rows)
        assertAll(
            Executable {
                assertTrue(
                    "app/build/classes/java/test" in scanned,
                    "the java plugin's conventional test output is not scanned ($scanned). The test " +
                        "source set compiles Java, that output is on the test runtime classpath, and " +
                        "a double written in Java would be proven absent by nothing."
                )
            },
            Executable {
                assertTrue(
                    "app/build/resources/test" in scanned,
                    "the test resources directory is not scanned ($scanned). It is on the test " +
                        "runtime classpath, so a class file can be CHECKED IN there and defined at " +
                        "runtime - which is r19d5's second door."
                )
            },
            Executable {
                assertEquals(
                    emptyList<String>(), unaccounted,
                    "the test JVM can load a class from a directory the scan does not read: " +
                        "$unaccounted. A list of directories is exactly what round 19 walked past; " +
                        "the set has to be the classpath's own."
                )
            },
            Executable { Round19Evidence.assertFrozen("doubles/scanned-trees.json", rows) }
        )
    }
}
