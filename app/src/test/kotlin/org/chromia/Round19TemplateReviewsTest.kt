package org.chromia

import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.ToolDocs
import org.chromia.tools.propertiesOrEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ROUND 19, THE TWO REVIEWS THE ADVERSARY DID NOT REACH (brief items 3 and 8), done as
 * MEASUREMENTS rather than as opinions.
 *
 * (a) EVERY GUARD THE FIFTEEN EXTENDING SECTIONS NAME MUST EXIST. Round 18 measured that
 *     only four of fifteen templates carried an EXTENDING section at all, and every
 *     template ships one now - but "it exists and is 400 characters long" is not the same
 *     claim as "everything it tells an extender to hold on to is really in the module".
 *     `DappScaffoldSecureTemplatesTest.everyTemplateShipsAnExtendingSectionNamingItsOwnSeam`
 *     already checks that a section names AT LEAST ONE identifier the module has. This
 *     closes the gap between "at least one" and "all of them": every identifier a section
 *     names is classified, and the taxonomy is CLOSED - anything that is not in the
 *     module, in the template's own shipped suite, a tool of this server, a corpus
 *     fixture on disk, a Rell standard-library call, or a name the section explicitly
 *     tells the extender to WRITE, fails this test with its name and its template.
 *
 *     The measurement, run before this test was written: sixteen `//` blocks carry the
 *     phrase, fifteen of them are section headers, and ten identifiers in them resolve
 *     outside the module - five adversary fixtures that exist on disk, one corpus test
 *     case, one Rell standard-library call, and three names the sections PRESCRIBE
 *     (`exit_requested_at`, `cliff_ms`, `last_paid_at`). Nothing was wrong; what was
 *     missing was the test that says so, and would say so again after an edit.
 *
 * (b) EVERY `describe_tool` LONG FORM'S CLAIMS. The 71 tools' full descriptions are the
 *     text an agent reads before it calls anything, and round 17's stale-roster defect -
 *     a HAND-WRITTEN count of "the ELEVEN hardened ones" that had been false for five
 *     weeks - is the class of defect they are exposed to. Measured here: every tool has a
 *     full description; every tool is named by at least one test source file; every
 *     cross-reference a description makes (`describe_tool{tool:"X"}`,
 *     `chromia_help{topic:"X"}`) resolves to a tool that exists and a topic that exists;
 *     and every "Rell pin X" a description states equals the DERIVED source tag rather
 *     than a number somebody typed.
 *
 *     WHAT THIS DOES NOT DO, written down rather than implied: it does not audit each
 *     description SENTENCE BY SENTENCE against a test that measures it. That is a lane of
 *     its own, and `docs/TEMPLATE-GAPS.md` carries it as the next item rather than this
 *     test carrying a claim it does not measure.
 */
class Round19TemplateReviewsTest {

    // ---- (a) the fifteen EXTENDING sections ---------------------------------------

    /**
     * A NAME THE SECTION TELLS THE EXTENDER TO WRITE, which is therefore correctly ABSENT
     * from the module. Each entry carries the sentence that prescribes it, and the test
     * asserts that sentence is really in the section - a prescription that loses its
     * sentence stops being a prescription and becomes a stale reference.
     */
    private val prescribed: Map<String, String> = linkedMapOf(
        "exit_requested_at" to "give `policy` an immutable",
        "cliff_ms" to "is safe: the discontinuity",
        "last_paid_at" to "never `now - s.last_paid_at`"
    )

    /**
     * A NAME THAT LIVES IN THE EXPLOIT CORPUS RATHER THAN IN THE MODULE: the adversary
     * dapps the seam sentences cite by name. Each path is asserted to exist, so a fixture
     * that is renamed or deleted reddens this test rather than leaving a seam sentence
     * pointing at nothing.
     */
    private val citedInTheCorpus: Map<String, String> = linkedMapOf(
        "dapp_a_feepool" to "app/src/test/resources/exploit-corpus/realworld/adversary-round7/dapp_a_feepool",
        "dapp_b_ratecurve" to "app/src/test/resources/exploit-corpus/realworld/adversary-round8/dapp_b_ratecurve",
        "dapp_a_pause" to "app/src/test/resources/exploit-corpus/realworld/adversary-round8/dapp_a_pause",
        "dapp_a2_pause_variant" to
            "app/src/test/resources/exploit-corpus/realworld/adversary-round8/dapp_a2_pause_variant",
        "dapp_a3_pause_as_cancel" to
            "app/src/test/resources/exploit-corpus/realworld/adversary-round8/dapp_a3_pause_as_cancel",
        "test_b2" to
            "app/src/test/resources/exploit-corpus/realworld/adversary-round8/dapp_b_ratecurve/src/test/attack_test.rell"
    )

    /** Rell standard-library calls a seam sentence names. Not module code, and not ours. */
    private val rellStandardLibrary = setOf("contains_all")

    private val identifier = Regex("[a-z][a-z0-9]*(?:_[a-z0-9]+)+")

    private fun extendingSection(main: String): String {
        val lines = main.lines()
        val first = lines.indexOfFirst { it.trimStart().startsWith("// EXTENDING THIS TEMPLATE") }
        check(first >= 0) { "no EXTENDING THIS TEMPLATE section" }
        var last = first
        while (last + 1 < lines.size && lines[last + 1].trimStart().startsWith("//")) last++
        return lines.subList(first, last + 1).joinToString("\n")
    }

    @Test
    fun `every identifier the fifteen EXTENDING sections name resolves, and the taxonomy is closed`() {
        val toolNames = McpTools.allTools().map { it.name }.toSet()
        val seenPrescribed = mutableSetOf<String>()
        val seenCited = mutableSetOf<String>()
        val seenStdlib = mutableSetOf<String>()
        val unresolved = mutableListOf<String>()
        val tally = mutableListOf<String>()

        DappScaffold.templates.forEach { template ->
            val fileMap = DappScaffold.files("t", template = template)
            val main = fileMap.getValue("src/main.rell")
            val section = extendingSection(main)
            // The module WITHOUT its comments, so a seam sentence cannot satisfy itself.
            val moduleCode = main.lines().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")
            val suite = fileMap.filterKeys { it != "src/main.rell" }.values.joinToString("\n")
            var inModule = 0
            var elsewhere = 0
            identifier.findAll(section).map { it.value }.toSet().sorted().forEach { name ->
                when {
                    moduleCode.contains(name) -> inModule++
                    suite.contains(name) -> elsewhere++
                    name in toolNames -> elsewhere++
                    name in prescribed -> { seenPrescribed += name; elsewhere++ }
                    name in citedInTheCorpus -> { seenCited += name; elsewhere++ }
                    name in rellStandardLibrary -> { seenStdlib += name; elsewhere++ }
                    else -> unresolved += "$template names `$name`, which is in neither the module, its " +
                        "shipped suite, the tool registry, the corpus nor the prescribed list"
                }
            }
            tally += "%-12s section=%-6d in-module=%-3d classified-elsewhere=%d".format(
                template, section.length, inModule, elsewhere
            )
            assertTrue(
                inModule > 0,
                "$template's EXTENDING section names no identifier of its own module - that is advice " +
                    "about a different dapp"
            )
        }
        println("ROUND19-EXTENDING-SEAM-AUDIT\n" + tally.joinToString("\n"))
        assertTrue(
            unresolved.isEmpty(),
            "an EXTENDING section must be about guards that EXIST - fix the section or the template, " +
                "whichever is wrong:\n" + unresolved.joinToString("\n")
        )

        // ...and no entry of the three escape hatches may go dead: an allowance nothing
        // uses is an allowance that will be used by accident.
        assertEquals(prescribed.keys, seenPrescribed, "a prescribed name no section names any more")
        assertEquals(citedInTheCorpus.keys, seenCited, "a corpus citation no section makes any more")
        assertEquals(rellStandardLibrary, seenStdlib, "a stdlib allowance no section needs any more")
    }

    @Test
    fun `every prescribed name carries the sentence that prescribes it, and every citation exists`() {
        val sections = DappScaffold.templates.associateWith {
            extendingSection(DappScaffold.files("t", template = it).getValue("src/main.rell"))
        }
        prescribed.forEach { (name, sentence) ->
            val owner = sections.entries.singleOrNull { it.value.contains(name) }
            assertTrue(owner != null, "`$name` is prescribed by no section")
            assertTrue(
                owner!!.value.contains(sentence),
                "${owner.key}'s section names `$name` but no longer carries the sentence that tells an " +
                    "extender to write it: a prescription without its sentence is a stale reference"
            )
            // ...and it really is absent from the module, which is the whole reason it is here.
            val code = DappScaffold.files("t", template = owner.key).getValue("src/main.rell")
                .lines().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")
            assertTrue(
                !code.contains(name),
                "`$name` is now IN ${owner.key} - it is no longer prescribed and belongs out of this list"
            )
        }
        citedInTheCorpus.forEach { (name, path) ->
            assertTrue(RepoFiles.exists(path), "`$name` cites $path, which does not exist")
            if (path.endsWith(".rell")) {
                assertTrue(
                    RepoFiles.text(path).contains(name),
                    "`$name` cites $path, which does not contain it"
                )
            }
        }
    }

    /**
     * THE INSURANCE EXIT QUEUE, WHICH IS THE SEAM ROUND 18 WALKED INTO, read against the
     * section that describes it. The section's promise is specific - `leave_policy()` is
     * the only place a policy leaves, an in-round exit JOINS the round instead of racing
     * it, and the queue is drained INSIDE the settlement after the claims are paid - so
     * the promise is checked against the module rather than taken on trust.
     */
    @Test
    fun `the insurance section's exit queue is the exit queue the template ships`() {
        val main = DappScaffold.files("t", template = "insurance").getValue("src/main.rell")
        val section = extendingSection(main)
        val code = main.lines().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")

        assertTrue(section.contains("EVERY PATH OUT OF A POLICY IS A SECOND EXIT PATH"), section.take(200))
        assertTrue(section.contains("leave_policy(p)"), "the section must name the one door: $section")

        // ONE door out, and it is the one the section names.
        assertTrue(code.contains("function leave_policy("), "leave_policy must be a helper, not advice")
        assertTrue(code.contains("function retire_policy("), "retire_policy must be a helper")
        // An in-round exit JOINS the round: it is marked, nothing is paid, and the
        // settlement drains it. `exiting` is the mark the section's promise rests on.
        assertTrue(code.contains("exiting"), "the exit queue's mark must exist")
        assertTrue(code.contains("operation settle_claim_round()"), "the template must ship settle_claim_round")
        // The settlement body: everything from the operation's header to the next
        // top-level declaration of the module, whatever it happens to be.
        val settle = code.substringAfter("operation settle_claim_round()")
            .split(Regex("\\n\\s{0,8}(operation|query|function|entity|struct)\\s"))
            .first()
        assertTrue(
            settle.contains("retire_policy("),
            "the exit queue must be drained INSIDE the settlement, through the same refund helper - " +
                "that is the whole of round 18's finding"
        )
        // ...and the two refusals that make the race unwritable rather than checked.
        assertTrue(
            code.contains("this policy is already leaving with the round") ||
                code.contains("is already leaving"),
            "a second exit request must be refused"
        )
        assertTrue(
            code.contains("this policy is leaving with the round"),
            "a claim from a policy that is leaving must be refused - the symmetric half"
        )
    }

    // ---- (b) the describe_tool long forms ------------------------------------------

    @Test
    fun `every tool has a full description, and every tool is named by at least one test`() {
        val tools = McpTools.allTools()
        assertTrue(tools.size >= 71, "round 19 measured 71 tools; got ${tools.size}")
        val sources = RepoFiles.testSources().associate { RepoFiles.className(it) to it.toFile().readText() }
        val unnamed = mutableListOf<String>()
        val blank = mutableListOf<String>()
        tools.forEach { tool ->
            val full = ToolDocs.full(tool.name, tool.description)
            if (full.isNullOrBlank()) blank += tool.name
            if (sources.none { (_, text) -> text.contains(tool.name) }) unnamed += tool.name
        }
        assertTrue(blank.isEmpty(), "a tool with no full description tells an agent nothing: $blank")
        assertTrue(
            unnamed.isEmpty(),
            "a tool no test source names is a long form no test measures - the round-17 stale-roster " +
                "defect one level up: " + unnamed.joinToString(", ")
        )
    }

    /**
     * EVERY CROSS-REFERENCE A LONG FORM MAKES RESOLVES. `describe_tool{tool:"X"}` and
     * `chromia_help{topic:"X"}` are the two forms the descriptions use to send an agent
     * somewhere else, and a dead one is a claim that is simply false.
     */
    @Test
    fun `every describe_tool and chromia_help cross-reference in a long form resolves`() {
        val toolNames = McpTools.allTools().map { it.name }.toSet()
        val describeRef = Regex("""describe_tool\{\s*tool\s*:\s*"([^"]+)"""")
        val helpRef = Regex("""chromia_help\{\s*topic\s*:\s*"([^"]+)"""")
        val dead = mutableListOf<String>()
        var checked = 0
        McpTools.allTools().forEach { tool ->
            val full = ToolDocs.full(tool.name, tool.description).orEmpty()
            describeRef.findAll(full).forEach { m ->
                val target = m.groupValues[1]
                checked++
                if (!target.startsWith("<") && target !in toolNames) {
                    dead += "${tool.name} sends an agent to describe_tool{tool:\"$target\"}, which is not a tool"
                }
            }
            helpRef.findAll(full).forEach { m ->
                val target = m.groupValues[1]
                checked++
                if (!target.startsWith("<") && target !in ToolDocs.TOPICS) {
                    dead += "${tool.name} sends an agent to chromia_help{topic:\"$target\"}, which is not a topic"
                }
            }
        }
        println("ROUND19-DESCRIBE-CROSSREFS checked=$checked dead=${dead.size}")
        assertTrue(dead.isEmpty(), dead.joinToString("\n"))
    }

    /**
     * THE ONE NUMBER IN THE LONG FORMS THAT IS TYPED RATHER THAN DERIVED, and the trap
     * underneath it. Seven descriptions state "Rell pin 0.16.7" - the Rell SOURCE TAG the
     * help tools quote - while `chromia.yml`'s `compile.rellVersion` is 0.16.1, because
     * CLI 0.33.x's SUPPORTED_VERSIONS list stops there and a project pinned to the source
     * tag fails to build. Both numbers are right in their own place and NOTHING tied
     * either of them to a constant, which is the round-17 stale-roster defect exactly:
     * a hand-written number that no test compares to the thing it describes.
     *
     * So the source tag is a constant now, and every "Rell pin X" in every description is
     * measured against it.
     */
    @Test
    fun `every Rell pin a long form states is the derived source tag, not a typed number`() {
        val pin = Regex("""Rell pin (\d+\.\d+\.\d+)""")
        val wrong = mutableListOf<String>()
        var stated = 0
        McpTools.allTools().forEach { tool ->
            val full = ToolDocs.full(tool.name, tool.description).orEmpty()
            pin.findAll(full).forEach { m ->
                stated++
                if (m.groupValues[1] != DappScaffold.RELL_SOURCE_TAG) {
                    wrong += "${tool.name} states `${m.value}`; the source tag is ${DappScaffold.RELL_SOURCE_TAG}"
                }
            }
        }
        println("ROUND19-RELL-PIN-CLAIMS stated=$stated wrong=${wrong.size}")
        assertTrue(stated >= 7, "round 19 measured seven descriptions stating a Rell pin; got $stated")
        assertTrue(wrong.isEmpty(), wrong.joinToString("\n"))
        // ...and the two pins are DIFFERENT on purpose. If they ever become equal, the
        // sentence in DappScaffold's header that explains the difference is stale.
        assertNotEquals(
            DappScaffold.RELL_SOURCE_TAG,
            DappScaffold.RELL_VERSION,
            "the source tag and chromia.yml's compile.rellVersion are two different pins; if they have " +
                "converged, the comment that explains why they differ must go with them"
        )
    }

    /**
     * AND THE SCHEMA-LEVEL HALF: a long form that names a parameter must name one the
     * schema declares. An agent that copies the example and gets "unknown parameter" has
     * been told something false by the tool's own description.
     */
    @Test
    fun `every parameter a long form's own call example names is declared by that tool`() {
        val call = Regex("""\{\s*([a-zA-Z][a-zA-Z0-9_]*)\s*:""")
        val bad = mutableListOf<String>()
        McpTools.allTools().forEach { tool ->
            val declared = tool.inputSchema.propertiesOrEmpty.keys
            val full = ToolDocs.full(tool.name, tool.description).orEmpty()
            Regex("""\b${Regex.escape(tool.name)}\{([^{}]{0,200})\}""").findAll(full).forEach { m ->
                call.findAll("{" + m.groupValues[1]).forEach { p ->
                    val param = p.groupValues[1]
                    if (param !in declared) {
                        bad += "${tool.name}'s own example passes `$param`, which its schema does not declare " +
                            "(declared: ${declared.joinToString(", ")})"
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), bad.joinToString("\n"))
    }
}
