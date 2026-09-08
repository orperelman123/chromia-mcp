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

/**
 * ROUND 19, THE ZERO-DOUBLES PROOF: what the structural scan is ANCHORED ON.
 *
 * [NoTestDoublesTest] reads compiled classes and finds a double three ways: a
 * PRODUCTION type in the transitive supertype closure, `java.lang.reflect.Proxy`
 * (or `InvocationHandler`) in the constant pool, and an invokedynamic whose
 * target interface production declares or accepts as a parameter. Every one of
 * round 18's eight probes is a substitute for a type production OWNS, which is
 * why all eight are caught.
 *
 * Round 19 substitutes the collaborators production does NOT own. The
 * consequences follow from the scan's own definitions, not from a spelling:
 *
 *  - `productionSupertypeOf` returns the first supertype found in `mainClasses`
 *    and ENDS any branch that leaves the scanned tree without reaching one, so a
 *    named class over a THIRD-PARTY interface has no finding at all - and
 *    `ContentRetriever` is a real injection point, the RAG seam `RagStore` builds
 *    (`r19d1`).
 *  - `samTargets` is derived from what production DECLARES or ACCEPTS AS A
 *    PARAMETER. `ContentRetriever` is only ever a LOCAL in production, so it is
 *    in no method descriptor, in no `samTargets`, and a SAM lambda over it is
 *    invisible where the same lambda over `EmbeddingModel` is caught (`r19d2`).
 *  - `proxyTypes` is exactly two names, so `MethodHandleProxies`, which builds
 *    the same runtime substitute without naming `Proxy`, is not in it (`r19d4`).
 *  - a class DEFINED AT RUNTIME has no class file in any scanned directory
 *    (`r19d5`).
 *
 * `r19d6` is the CONTROL: the identical named class over a PRODUCTION type,
 * which must be caught as SUPERTYPE, so each probe above is one substitution
 * away and not a different experiment.
 *
 * WHY THE DETECTORS ARE RE-IMPLEMENTED HERE. [NoTestDoublesTest]'s scan is
 * private, and widening it to run a second scoreboard would be editing the test
 * this round is attacking. So [structuralSites] below is a faithful copy, and
 * [theCopiedScanAgreesWithTheShippedOneOnAllEightRound18Probes] proves it is
 * faithful the only way that means anything: it must reproduce the shipped
 * scan's own expected verdict for all eight round-18 probes, by name and by
 * kind. A copy that has drifted fails there first.
 *
 * NOTHING HERE IS FIXED. The evasions are pinned as probes the scan must catch,
 * exactly as round 18 pinned its eight.
 */
class Round19DoubleEvasionTest {

    private data class Site(val where: String, val kind: String, val what: String) {
        override fun toString() = "$where  $kind  $what"
    }

    private val classesRoot = RepoFiles.root.resolve("app/build/classes/kotlin")
    private val mainClasses: Map<String, ClassFacts> by lazy { ClassFiles.readTree(classesRoot.resolve("main")) }
    private val probeClasses: Map<String, ClassFacts> by lazy {
        ClassFiles.readTree(classesRoot.resolve("doubleProbes"))
    }

    private val notACollaborator =
        listOf("java/", "javax/", "jdk/", "sun/", "kotlin/", "kotlinx/", "org/junit/", "org/opentest4j/")

    private fun isLanguageOrHarness(internalName: String) = notACollaborator.any { internalName.startsWith(it) }

    private val proxyTypes = setOf("java/lang/reflect/Proxy", "java/lang/reflect/InvocationHandler")

    private fun isFunctionalInterface(internalName: String): Boolean = runCatching {
        val type = Class.forName(internalName.replace('/', '.'), false, javaClass.classLoader)
        type.isInterface && type.methods.count { method ->
            java.lang.reflect.Modifier.isAbstract(method.modifiers) &&
                !java.lang.reflect.Modifier.isStatic(method.modifiers)
        } == 1
    }.getOrDefault(false)

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

    private fun structuralSites(scanned: Map<String, ClassFacts>): List<Site> {
        val sites = mutableListOf<Site>()
        scanned.values.sortedBy { it.internalName }.forEach { facts ->
            val where = "${facts.sourceFile ?: "?"} [${facts.simpleName}]"
            productionSupertypeOf(facts, scanned)?.let { sites += Site(where, "SUPERTYPE", "stands in for $it") }
            val reflective = facts.referencedTypes.intersect(proxyTypes) +
                facts.invokeDynamicTargets.filter { it in proxyTypes }
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
            probeClasses.size >= 8,
            "the evasion probes are not compiled (found ${probeClasses.size} classes under " +
                "${classesRoot.resolve("doubleProbes")}); :app:test depends on compileDoubleProbesKotlin"
        )
        assertTrue(
            mainClasses.size > 200,
            "production classes are not compiled (found ${mainClasses.size} under " +
                "${classesRoot.resolve("main")}) - every detector here is anchored on them"
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
        // (probe, the kind that would have to name it, why it is a double)
        val expected = listOf(
            Triple(
                "R19d1NamedClassOverAThirdPartySeam", "SUPERTYPE",
                "a named class implementing dev.langchain4j.rag.content.retriever.ContentRetriever, " +
                    "the RAG seam RagStore builds - productionSupertypeOf ends the branch because " +
                    "the supertype is not one of production's own types"
            ),
            Triple(
                "R19d2SamLambdaOnANonParameterSeam", "SAM_CONVERSION",
                "the same seam as a SAM lambda - ContentRetriever is only ever a LOCAL in " +
                    "production, so it is in no method descriptor and so in no samTargets, while " +
                    "the identical lambda over EmbeddingModel (D6) is caught"
            ),
            Triple(
                "R19d4MethodHandleProxy", "REFLECTION_PROXY",
                "MethodHandleProxies.asInterfaceInstance builds the same runtime substitute for " +
                    "EmbeddingModel without naming Proxy, and proxyTypes is two names"
            ),
            Triple(
                "R19d5RuntimeDefinedClass", "REFLECTION_PROXY",
                "MethodHandles.Lookup.defineHiddenClass defines the substitute from bytes - there " +
                    "is no class file in any scanned directory for it"
            ),
            Triple(
                "R19d6ControlNamedClassOverAProductionSeam", "SUPERTYPE",
                "THE CONTROL: the identical named class over a PRODUCTION type, which must be caught"
            )
        )
        val rows: JsonArray = buildJsonArray {
            expected.forEach { (probe, kind, why) ->
                val hit = sites.firstOrNull { it.where.contains(probe) && it.kind == kind }
                add(
                    buildJsonObject {
                        put("probe", probe)
                        put("expected_kind", kind)
                        put("caught", hit != null)
                        put("evidence", hit?.toString() ?: "")
                        put("why", why)
                    }
                )
            }
        }
        Round19Evidence.record("doubles/structural.json", rows)
        val missed = expected.filterNot { (probe, kind, _) ->
            sites.any { it.where.contains(probe) && it.kind == kind }
        }.map { "${it.first} (expected ${it.second}): ${it.third}" }
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
     * THE TREES THE SCAN NEVER OPENS. `classesRoot` is
     * `app/build/classes/kotlin`, so `app/build/classes/java/test` - where any
     * `.java` file under `app/src/test/java` compiles to, by the java plugin's
     * own convention, with no `srcDirs` override anywhere in `app/build.gradle.kts` -
     * is never read; and `RepoFiles.testSources()` walks `app/src/test/kotlin`
     * filtering `.kt`, so the source layer cannot see it either. A double
     * written in Java is outside BOTH layers, and neither the scan nor any other
     * test in the repository asserts that those directories are empty.
     */
    @Test
    fun theScanReadsEveryDirectoryTheTestRuntimeLoadsClassesFrom() {
        val kotlinTest = "app/build/classes/kotlin/test"
        val javaTest = "app/build/classes/java/test"
        // NoTestDoublesTest's classesRoot is `app/build/classes/kotlin`, so the
        // set of trees it reads is exactly the kotlin one.
        val scanned = setOf(kotlinTest)
        val rows = buildJsonArray {
            listOf(kotlinTest, javaTest).forEach { dir ->
                add(
                    buildJsonObject {
                        put("directory", dir)
                        put("scanned_by_NoTestDoublesTest", dir in scanned)
                    }
                )
            }
        }
        Round19Evidence.record("doubles/scanned-trees.json", rows)
        assertAll(
            Executable {
                assertTrue(
                    javaTest in scanned,
                    "NoTestDoublesTest reads app/build/classes/kotlin only. The test source set " +
                        "compiles Java too - the java plugin's conventional app/src/test/java, which " +
                        "app/build.gradle.kts never redefines - and that output lands in " +
                        "$javaTest, on the test runtime classpath and outside the scan. " +
                        "RepoFiles.testSources() walks app/src/test/kotlin and filters .kt, so the " +
                        "source layer is blind to it as well: a double written in Java is proven " +
                        "absent by nothing."
                )
            },
            Executable { Round19Evidence.assertFrozen("doubles/scanned-trees.json", rows) }
        )
    }
}
