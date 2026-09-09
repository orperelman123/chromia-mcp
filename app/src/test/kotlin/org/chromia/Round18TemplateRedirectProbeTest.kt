package org.chromia

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ROUND 18, THE REDIRECT AFTER THE FIX - the laundered mixed ask, and the matrix that
 * makes it impossible to launder any of the others.
 *
 * WHAT ROUND 18 MEASURED. `adversary-round18/redirect/raw.json` records fifty-four asks
 * through the real `scaffold_dapp`: 23 scaffolded, 31 declined with a reason and no
 * files. Three of the four two-class asks were declined. The fourth,
 *
 *     "a lending pool that also runs a weekly raffle for depositors"
 *
 * was scaffolded onto `lending` - three files, ok:true, and a 1517-byte note about lazy
 * interest accrual and share pricing with NOT ONE WORD about the raffle, the class that
 * asked on its own this server declines by name. That is not a near miss: naming a
 * covered class in the same sentence as an uncovered one LAUNDERS the uncovered one, and
 * the agent gets files, a green gate and silence about the half of its ask that has no
 * guards at all. The raffle half built from that answer drained on a real chain
 * (`fixtures/raffle`, `chr test`, 3 of 3 green): trudy staked 90 of 1890 - about one week
 * in twenty-one - and won FIVE OF FIVE weekly draws, turning 100 into 400 while alice and
 * bob each put in 1500 and ended at 1350, with `rell_security_check` ok:true and zero
 * findings, and every drained draw a legal draw.
 *
 * The three that were declined were declined by ACCIDENT OF ORDER - the declined branch
 * happened to sit earlier in the `when` than the covered one. The fix removes the accident:
 * the uncovered classes are a LIST consulted before the routing, so an ask that carries
 * one of them is answered by it first whatever else it names, and the covered half is
 * named by template with nothing scaffolded.
 *
 * This class is the AFTER, frozen. The adversary's own recording is the BEFORE and this
 * lane does not touch it. A route that changes here fails with the committed value beside
 * the fresh one; re-freezing is deliberate - copy
 * `app/build/round18-template-fix/redirect/raw.json` over the committed file and commit
 * it with the reason.
 */
class Round18TemplateRedirectProbeTest {

    /** The fifty-four asks `adversary-round18/redirect/raw.json` measured, in its order. */
    private val measured = listOf(
        "a raffle with on-chain randomness",
        "a lottery that pays a random winner from the prize pool",
        "a weekly lottery with rewards for ticket holders",
        "an insurance pool with claims",
        "an insurance pool funded by premium payments",
        "a parametric insurance payout on an oracle trigger",
        "order books",
        "limit-orders",
        "cross-chain bridges",
        "escrows between two parties",
        "OTCs desk",
        "de-fi lending",
        "a de-fi yield aggregator",
        "a swap-and-escrow for two parties",
        "an NFT marketplace with escrowed offers",
        "a cross-chain DEX",
        "a liquidity mining program that pays rewards",
        "a staking vault",
        "a stablecoin with a governance token",
        "a lending market with a DAO treasury",
        "una boveda con oraculo",
        "ein Tresor mit Orakel",
        "un echange de jetons",
        "borsa di scambio",
        "a payment channel",
        "a loyalty programme with points",
        "a prediction market",
        "a gaming item shop",
        "a charity donation pool",
        "a crowdfunding campaign with refunds",
        "a vesting schedule for founders",
        "a multisig wallet",
        "a token airdrop with a claim window",
        "a fee splitter contract",
        "insurance policies",
        "payment channels",
        "multisig wallets",
        "raffles",
        "airdrops",
        "multi-sig wallet",
        "multi sig wallet",
        "pay-per-use metering",
        "yield-farming rewards",
        "on-chain lottery",
        "a lending pool that also runs a weekly raffle for depositors",
        "an insurance pool whose premiums are streamed by the second",
        "a marketplace with an escrow and a multisig treasury",
        "a staking contract with a payment channel for the rewards",
        "an amm that pays a lottery jackpot from the swap fee",
        "un contrato de seguro con reclamaciones",
        "ein Zahlungskanal zwischen zwei Parteien",
        "une loterie hebdomadaire avec tirage au sort",
        "portfel multisig",
        "\u5e26\u7d22\u8d54\u7684\u4fdd\u9669\u6c60"
    )

    /**
     * Where each of them landed BEFORE this lane, from that same recording - the 23 that
     * scaffolded. Round 18's rule for this lane is that nothing routed honestly may
     * regress, so every one of these must still route exactly there, and the ONE
     * exception is the laundering itself.
     */
    /**
     * ROUND 20 moved exactly ONE of these, and it is the laundered ask itself: `a lending
     * pool that also runs a weekly raffle for depositors` was recorded landing on
     * `lending` BEFORE round 18's fix, round 18 turned it into a refusal, and round 20
     * gave its other half a template - so it is now a two-covered-class refusal rather
     * than a covered-plus-a-hole one. It is dropped from this map, which is the record of
     * what must not REGRESS, and asserted by name in the laundered-ask test instead.
     */
    private val measuredTargets: Map<String, String> = mapOf(
        "an insurance pool with claims" to "insurance",
        "an insurance pool funded by premium payments" to "insurance",
        "a parametric insurance payout on an oracle trigger" to "insurance",
        "order books" to "exchange",
        "limit-orders" to "exchange",
        "cross-chain bridges" to "bridge",
        "escrows between two parties" to "escrow",
        "OTCs desk" to "escrow",
        "de-fi lending" to "lending",
        "a swap-and-escrow for two parties" to "escrow",
        "an NFT marketplace with escrowed offers" to "marketplace",
        "a cross-chain DEX" to "bridge",
        "a liquidity mining program that pays rewards" to "staking",
        "a staking vault" to "vault",
        "a stablecoin with a governance token" to "stablecoin",
        "a lending market with a DAO treasury" to "lending",
        "a vesting schedule for founders" to "streaming",
        "a token airdrop with a claim window" to "staking",
        "insurance policies" to "insurance",
        "airdrops" to "staking",
        "yield-farming rewards" to "lending",
        "an insurance pool whose premiums are streamed by the second" to "insurance"
    )

    private val laundered = "a lending pool that also runs a weekly raffle for depositors"

    private fun probeToken(key: String) = key.removeSuffix("*")

    @Test
    fun `the fifty-four round 18 asks, re-measured after the fix`() {
        val lines = mutableListOf<String>()
        val rows = buildJsonArray {
            measured.forEach { ask ->
                val target = DappScaffold.closestTemplate(ask)
                val note = DappScaffold.closestTemplateNote(ask)
                val files = if (target == null) listOf() else DappScaffold.files("probe", target).keys.toList()
                lines += "%-58s -> %-12s files=%d".format(ask, target ?: "(none)", files.size)
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("closestTemplate", target ?: "")
                        put("declinedClasses", buildJsonArray {
                            DappScaffold.untemplatedClasses.filter { note.startsWith(it.note) || note.contains(it.note) }
                                .forEach { add(it.id) }
                        })
                        put("noteFirstSentence", note.substringBefore(". ").take(400))
                        put("scaffoldedFiles", buildJsonArray { files.forEach { add(it) } })
                    }
                )
            }
        }
        Round18TemplateEvidence.record("redirect/raw.json", rows)
        println("ROUND18-TEMPLATE-REDIRECT-AFTER\n" + lines.joinToString("\n"))
        Round18TemplateEvidence.assertFrozen("redirect/raw.json", rows)
    }

    /**
     * NOTHING ROUTED HONESTLY MAY REGRESS, and the one route that changes is the one
     * round 18 named. This reads the adversary's own targets rather than a fresh opinion
     * about where an ask ought to go.
     */
    @Test
    fun `every honest route of the fifty-four survives, and only the laundered one changes`() {
        measuredTargets.forEach { (ask, before) ->
            if (ask == laundered) {
                assertNull(
                    DappScaffold.closestTemplate(ask),
                    "the laundered ask must stop scaffolding `$before` with no word about the raffle"
                )
            } else {
                assertEquals(before, DappScaffold.closestTemplate(ask), ask)
            }
        }
        // ...and the ones that were declined are still declined - except the SIX that
        // round 20 gave a template to. They are the round-18 finding's own class: they
        // were declined because nothing here covered a draw, and now something does.
        // Each is asserted by name, not filtered out, so a seventh cannot join them
        // quietly.
        val nowRaffle = setOf(
            "a raffle with on-chain randomness",
            "a lottery that pays a random winner from the prize pool",
            "a weekly lottery with rewards for ticket holders",
            "raffles",
            "on-chain lottery",
            "une loterie hebdomadaire avec tirage au sort"
        )
        nowRaffle.forEach {
            assertEquals(
                "raffle",
                DappScaffold.closestTemplate(it),
                "'$it' is the class round 20 built the sixteenth template for"
            )
        }
        measured.filterNot { measuredTargets.containsKey(it) || it in nowRaffle }.forEach {
            assertNull(DappScaffold.closestTemplate(it), "'$it' was declined before the fix and must still be")
        }
        // The two TEMPLATE-GAPS rows that had NO named refusal are still declined - what
        // changed for them is that they are now declined BY NAME rather than by falling
        // to the roster, which Round20TemplateVocabularyProbeTest asserts.
        listOf("a loyalty programme with points", "a charity donation pool", "a fee splitter contract").forEach {
            assertNull(DappScaffold.closestTemplate(it), "'$it' still has no template")
        }
    }

    /**
     * THE LAUNDERED ASK, answered. The declined class is named FIRST, the covered half is
     * named by template, the missing guard is spelled out, and nothing is scaffolded.
     */
    @Test
    fun `the laundered mixed ask names both halves and scaffolds nothing`() {
        val note = DappScaffold.closestTemplateNote(laundered)
        // ROUND 18'S FINDING IS UNCHANGED AND SO IS ITS ANSWER: the ask that was
        // scaffolded onto `lending` with not one word about the raffle still scaffolds
        // NOTHING, and still says why. What round 20 changed is the OTHER half - the
        // raffle has a template now, so this names two templates instead of naming one
        // and a hole.
        assertNull(DappScaffold.closestTemplate(laundered))
        assertFalse(note.startsWith("Use `template="), "the covered half must not be the first thing said: $note")
        assertTrue(note.startsWith("THIS ASK NAMES MORE THAN ONE COVERED CLASS"), note.take(200))
        // ...both halves, by template and each with its exploit class.
        assertTrue(note.contains("`template=lending`"), "the lending half must be named: $note")
        assertTrue(note.contains("`template=raffle`"), "the raffle half must be named: $note")
        assertTrue(
            note.contains(DappScaffold.templateClasses.getValue("lending")) &&
                note.contains(DappScaffold.templateClasses.getValue("raffle")),
            "a template NAME without its exploit class is the round-8 hazard: $note"
        )
        // ...and the measurement that says why this is a NO rather than a best-effort
        // scaffold of the half we happened to read first.
        assertTrue(note.contains("trudy staked 90 of 1890"), note)
        assertTrue(note.contains("FIVE OF FIVE"), note)
        assertTrue(note.contains("NOTHING WAS SCAFFOLDED"), note)
        // The two-class asks whose OTHER half still has no template are declined the way
        // they always were, by the uncovered class, first.
        listOf(
            "a marketplace with an escrow and a multisig treasury",
            "a staking contract with a payment channel for the rewards"
        ).forEach {
            val n = DappScaffold.closestTemplateNote(it)
            assertNull(DappScaffold.closestTemplate(it), it)
            assertTrue(n.startsWith("No shipped template covers that name"), n.take(160))
            assertTrue(n.contains("NOTHING WAS SCAFFOLDED"), "$it must name the covered half it is refusing: $n")
        }
        // ...and the one whose other half is the raffle is answered like the laundered
        // ask above: two covered classes, one at a time, nothing scaffolded.
        val jackpot = "an amm that pays a lottery jackpot from the swap fee"
        val jn = DappScaffold.closestTemplateNote(jackpot)
        assertNull(DappScaffold.closestTemplate(jackpot), jackpot)
        assertTrue(jn.startsWith("THIS ASK NAMES MORE THAN ONE COVERED CLASS"), jn.take(160))
        assertTrue(jn.contains("`template=amm`") && jn.contains("`template=raffle`"), jn)
        assertTrue(jn.contains("NOTHING WAS SCAFFOLDED"), jn)
    }

    /**
     * EVERY DECLINED CLASS TOKEN, COMBINED WITH EVERY TEMPLATE CLASS TOKEN. Both sides are
     * DERIVED - the tokens are the first key of each class's own key list, and the key
     * lists are the ones the routing matches on - so a class added to either side is in
     * this matrix in the same commit, and a token that stops routing where it says fails
     * here rather than quietly dropping out of the probe.
     */
    @Test
    fun `every declined class token combined with every template class token`() {
        val templateTokens = DappScaffold.templateKeys.map { (name, keys) -> name to probeToken(keys.first()) }
        val declinedTokens = DappScaffold.untemplatedClasses.map { it to probeToken(it.keys.first()) }
        // ROUND 20: sixteen routing branches (the raffle joined them) and six classes
        // declined by name (the raffle LEFT them, and loyalty-points and fee-splitter -
        // the two TEMPLATE-GAPS rows that had no named refusal at all - joined them).
        assertEquals(16, templateTokens.size, "one entry per routing branch")
        assertEquals(6, declinedTokens.size, "one entry per class this server declines by name")

        val lines = mutableListOf<String>()
        val rows = buildJsonArray {
            // EACH TOKEN ALONE FIRST: a matrix built on a token that does not route where
            // it claims would prove nothing about the pair.
            templateTokens.forEach { (name, token) ->
                assertEquals(name, DappScaffold.closestTemplate(token), "the derived token '$token' must reach $name")
            }
            declinedTokens.forEach { (klass, token) ->
                assertNull(DappScaffold.closestTemplate(token), "'$token' must be declined on its own")
                assertTrue(
                    DappScaffold.closestTemplateNote(token).startsWith(klass.note),
                    "'$token' must be answered by ${klass.id}"
                )
            }
            // ...AND THEN EVERY PAIR.
            templateTokens.forEach { (name, templateToken) ->
                declinedTokens.forEach { (klass, declinedToken) ->
                    val ask = "$templateToken and a $declinedToken"
                    val note = DappScaffold.closestTemplateNote(ask)
                    val target = DappScaffold.closestTemplate(ask)
                    assertNull(target, "'$ask' names an uncovered class and must scaffold nothing: ${note.take(160)}")
                    assertTrue(note.startsWith(klass.note), "'$ask' must name ${klass.id} FIRST: ${note.take(200)}")
                    assertTrue(note.contains("`template=$name`"), "'$ask' must name the covered half: $note")
                    assertTrue(note.contains(klass.missingGuard), "'$ask' must name the missing guard: $note")
                    assertTrue(note.contains("NOTHING WAS SCAFFOLDED"), "'$ask': $note")
                    lines += "%-40s -> declined[%s] + covered[%s]".format(ask, klass.id, name)
                    add(
                        buildJsonObject {
                            put("ask", ask)
                            put("declinedClass", klass.id)
                            put("coveredTemplate", name)
                            put("closestTemplate", target ?: "")
                            put("noteFirstSentence", note.substringBefore(". ").take(400))
                            put("noteLength", note.length)
                        }
                    )
                }
            }
        }
        Round18TemplateEvidence.record("redirect/mixed-matrix.json", rows)
        println("ROUND18-TEMPLATE-MIXED-MATRIX\n" + lines.joinToString("\n"))
        Round18TemplateEvidence.assertFrozen("redirect/mixed-matrix.json", rows)
    }

    /**
     * THE KEYS ARE THE ROUTING'S OWN, and there is no second copy. A keyword that only
     * exists inside the `when` would be invisible to the mixed-ask answer, which is the
     * defect one level up from the one round 18 found.
     */
    @Test
    fun `the covered roster is derived from the routing's own key lists`() {
        DappScaffold.templateKeys.forEach { (name, keys) ->
            assertTrue(name in DappScaffold.templates, "$name is not a shipped template")
            assertTrue(keys.isNotEmpty(), "$name has no keys")
        }
        // Every hardened template is reachable by name through this map.
        DappScaffold.hardenedTemplates().forEach { t ->
            assertTrue(
                DappScaffold.templateKeys.any { it.first == t },
                "$t ships but no key list routes to it - that is the round-17 stale-roster defect"
            )
        }
        // Every declined class has a label, a missing guard and a note that says NO.
        DappScaffold.untemplatedClasses.forEach {
            assertTrue(it.note.startsWith("No shipped template covers that name"), it.id)
            assertTrue(it.missingGuard.length > 80, "${it.id}'s missing guard is too short to be a shape")
            assertTrue(it.label.isNotBlank(), it.id)
            assertTrue(it.keys.isNotEmpty(), it.id)
        }
        // ...and no declined class's key is also a template key: a token that is both
        // would make the answer depend on which list was read first.
        val templateKeySet = DappScaffold.templateKeys.flatMap { it.second }.toSet()
        DappScaffold.untemplatedClasses.forEach { klass ->
            klass.keys.forEach { key ->
                assertFalse(key in templateKeySet, "'$key' is both a covered and an uncovered class's key")
            }
        }
    }
}
