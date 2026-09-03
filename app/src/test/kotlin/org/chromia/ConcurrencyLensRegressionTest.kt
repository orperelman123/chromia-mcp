package org.chromia

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.chromia.tools.LocalChain
import org.chromia.tools.ProbeBudget
import org.chromia.tools.VerifyDeployment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * QA concurrency and resource-lifecycle lens (2026-09-02): races between
 * tool calls and the background work they leave behind. Each test failed on
 * the code it pins.
 */
class ConcurrencyLensRegressionTest {

    private val dbUrl = "jdbc:postgresql://localhost:5432/db?user=u&password=p"
    private val files = mapOf(
        "main.rell" to "module;\nentity item { key name; }\nquery item_count() = (item @* {}).size();"
    )

    @AfterEach
    fun tearDown() {
        LocalChain.stopAll()
        LocalChain.starterOverrideForTests = null
    }

    private fun installFakeStarter() {
        LocalChain.starterOverrideForTests = { plan ->
            LocalChain.Running(
                node = null,
                brid = plan.brid,
                apiPort = plan.apiPort,
                fingerprint = plan.fingerprint,
                nodePubkey = plan.pubKeyHex,
                expiresAtMillis = Long.MAX_VALUE,
                ttlTask = null
            )
        }
    }

    // ------------------------------------------------------------------
    // local_chain_up: TTL expiry racing an `up` for the same sources
    // ------------------------------------------------------------------

    /**
     * A TTL task that has already fired and is waiting for the registry lock
     * cannot be cancelled (cancel(false) only stops tasks that have not
     * started). When an `up` for the same sources holds the lock at that
     * moment it refreshes the TTL and answers "already_running, auto-stops in
     * N s"; the stale task then acquired the lock, saw `running === chain`
     * and stopped the chain the agent had just been promised. The interleaving
     * is reproduced exactly: arm a due TTL while holding the lock, refresh
     * under the same lock, release, and the chain must survive.
     */
    @Test
    fun ttlTaskAlreadyWaitingForTheLockMustNotStopAJustRefreshedChain() {
        installFakeStarter()
        val started = LocalChain.up(files, databaseUrl = dbUrl, ttlSeconds = 120)
        assertTrue(started.ok, started.notes)
        assertEquals("started", started.status)
        val chain = LocalChain.running!!

        val refreshed: LocalChain.UpResult
        synchronized(LocalChain.lock) {
            // The TTL is due NOW; its task fires on the scheduler thread and
            // blocks on the lock this thread holds - a TTL that woke up while
            // an `up` call was in progress.
            LocalChain.reschedule(chain, 0)
            Thread.sleep(300)
            // Same sources: the registry refreshes the TTL and promises the chain.
            refreshed = LocalChain.up(files, databaseUrl = dbUrl, ttlSeconds = 120)
        }
        assertEquals("already_running", refreshed.status, refreshed.notes)
        assertTrue(refreshed.expiresInSeconds!! > 60, refreshed.expiresInSeconds.toString())

        // The woken task runs as soon as the lock is free. Whatever it does
        // must be visible within a second; poll so a fast kill is caught early.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < deadline) {
            assertEquals(
                "running",
                LocalChain.status().status,
                "a stale TTL task stopped the chain right after `up` promised it for ${refreshed.expiresInSeconds}s"
            )
            Thread.sleep(50)
        }
        assertTrue(LocalChain.running === chain, "the same chain must still be registered")

        // A TTL that is genuinely current still stops the chain.
        LocalChain.reschedule(chain, 0)
        val stopDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (LocalChain.status().status == "running" && System.nanoTime() < stopDeadline) Thread.sleep(25)
        assertEquals("not_running", LocalChain.status().status, "a current TTL must still expire the chain")
    }

    // ------------------------------------------------------------------
    // ProbeBudget: abandoned probes must not starve the shared IO pool
    // ------------------------------------------------------------------

    /**
     * Every probe that outlives its deadline keeps blocking in the postchain
     * client (cancel cannot interrupt it). They used to do so on
     * Dispatchers.IO - the pool every other tool's blocking work is dispatched
     * to (rell_check, run_rell_tests, local_chain_up, docs) and which has 64
     * threads. Enough abandoned probes and an unrelated IO dispatch never got
     * a thread: the whole server hung with no message. Reproduced with more
     * stuck probes than the pool has threads, then an unrelated IO dispatch.
     */
    @Test
    fun abandonedProbesMustNotStarveTheSharedIoDispatcher() = runBlocking {
        val gate = CountDownLatch(1)
        val stuckProbes = Runtime.getRuntime().availableProcessors().coerceAtLeast(64) + 8
        assertEquals(0, ProbeBudget.abandonedCount(), "test precondition: nothing abandoned yet")
        assertEquals("", ProbeBudget.abandonedNote())
        try {
            val outcomes = (1..stuckProbes).map {
                async(Dispatchers.Default) {
                    ProbeBudget.withBudget(20) {
                        gate.await() // a non-interruptible client crawl that never answers in time
                        "late"
                    }
                }
            }.awaitAll()
            assertTrue(outcomes.all { it == null }, "every probe must have hit its deadline: $outcomes")

            // What rell_check / run_rell_tests / local_chain_up do next.
            val unrelated = withTimeoutOrNull(3_000) { withContext(Dispatchers.IO) { "ran" } }
            assertEquals(
                "ran",
                unrelated,
                "abandoned probes exhausted Dispatchers.IO - every IO-dispatched tool call would hang silently"
            )

            // Honest reporting: the backlog is counted and named in the hints.
            val abandoned = ProbeBudget.abandonedCount()
            assertTrue(abandoned >= 1, "abandoned probes must be accounted for, got $abandoned")
            // The count is exactly the probes blocked in the client - every one
            // of them is parked on `gate`, so it cannot move until we open it.
            // It used to include probes whose body never started (cancelled
            // while still queued for a pool thread) which then completed a
            // moment later: the count read here and the count in the hints
            // below disagreed on a loaded machine (gate red twice, 2026-09-03).
            Thread.sleep(250)
            assertEquals(abandoned, ProbeBudget.abandonedCount(), "abandoned count drifted while every real probe was still blocked")
            for (hint in listOf(
                ProbeBudget.queryTimeoutHint("mainnet", 20),
                ProbeBudget.preflightProbeTimeoutMessage(20),
                VerifyDeployment.timeoutHint("mainnet", 20)
            )) {
                assertTrue(hint.contains("$abandoned earlier probe(s) abandoned"), hint)
            }
        } finally {
            gate.countDown()
        }

        // Once the blocked calls return, the accounting is released - it is a
        // live count, never a ratchet.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (ProbeBudget.abandonedCount() > 0 && System.nanoTime() < deadline) Thread.sleep(10)
        assertEquals(0, ProbeBudget.abandonedCount(), "abandoned count must drop back once the probes return")
        assertEquals("", ProbeBudget.abandonedNote())
        assertFalse(VerifyDeployment.timeoutHint("mainnet", 20).contains("abandoned"))
    }

    @Test
    fun probeThatAnswersInTimeIsNeitherAbandonedNorCounted() = runBlocking {
        val result = ProbeBudget.withBudget(5_000) { "fast" }
        assertEquals("fast", result)
        assertEquals(0, ProbeBudget.abandonedCount())
        assertNull(ProbeBudget.withBudget(0) { "never runs" })
        assertEquals(0, ProbeBudget.abandonedCount())
    }
}
