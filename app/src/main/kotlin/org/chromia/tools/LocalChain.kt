package org.chromia.tools

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.postchain.PostchainNode
import net.postchain.StorageInitializer
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.base.runStorageCommand
import net.postchain.base.withReadWriteConnection
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.crypto.secp256k1_derivePubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.toObject
import net.postchain.rell.api.base.PrinterRellCliEnv
import net.postchain.rell.api.base.RellCliException
import net.postchain.rell.api.base.RellConfigGen
import net.postchain.rell.base.compiler.base.utils.C_SourceDir
import net.postchain.rell.base.model.ModuleName
import net.postchain.rell.module.RellPostchainModuleEnvironment
import org.apache.commons.configuration2.BaseConfiguration
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * In-process local Chromia chain for the `local_chain_up` tool: compiles the
 * given Rell sources into a real blockchain configuration and runs it on an
 * embedded Postchain node (the same engine `chr node start` wraps) against
 * PostgreSQL, exposing a subset of the Postchain REST API on localhost
 * (see [LocalChainRestBridge] for the served endpoints).
 *
 * This closes the last gap in the agent loop: after rell_check (compiles),
 * rell_security_check (secure), and run_rell_tests (tests pass), an agent can
 * now exercise its dapp against a RUNNING chain - submit real transactions,
 * run real queries over REST - with zero keys, zero funds, zero human steps.
 *
 * Design notes (mirrors chromia-cli StartCommand, verified against CLI source):
 * - Node key is the Chromia CLI's own well-known dev key (privkey 4242...42,
 *   NodeConfig.getDefaultNodeConfig). It is PUBLIC and for local dev only -
 *   nothing here touches real networks or funds.
 * - The chain runs in its own PostgreSQL schema ([SCHEMA]), wiped on every
 *   `up`, so runs are reproducible and never collide with run_rell_tests
 *   (which uses the URL's own schema).
 * - Bounded lifetime: at most [MAX_CHAINS] chain at a time, auto-stopped after
 *   `ttlSeconds` (default [DEFAULT_TTL_SECONDS], max [MAX_TTL_SECONDS]), plus
 *   a JVM shutdown hook - no orphan node can outlive the MCP server.
 * - Idempotent-ish: calling `up` again with identical sources returns the
 *   running chain's info (TTL refreshed); different sources restart the chain.
 */
object LocalChain {

    const val DATABASE_URL_ENV = RunRellTests.DATABASE_URL_ENV

    const val DEFAULT_TTL_SECONDS = 1800L
    const val MIN_TTL_SECONDS = 30L
    const val MAX_TTL_SECONDS = 7200L
    const val START_TIMEOUT_SECONDS = 120L
    const val MAX_CHAINS = 1
    const val CHAIN_IID = 0L

    /** Dedicated schema so the node never touches run_rell_tests state. */
    const val SCHEMA = "chromia_mcp_local_chain"

    /**
     * The Chromia CLI's default dev node key (chromia-cli NodeConfig.kt,
     * getDefaultNodeConfig). Deliberately well-known; never valid on any real
     * network. Do NOT replace with a generated key - a stable pubkey keeps
     * restarts reproducible, exactly like `chr node start`.
     */
    const val DEV_PRIV_KEY_HEX = "4242424242424242424242424242424242424242424242424242424242424242"

    data class UpResult(
        val ok: Boolean,
        val status: String,
        val brid: String? = null,
        val apiUrl: String? = null,
        val chainId: Long? = null,
        val nodePubkey: String? = null,
        val expiresInSeconds: Long? = null,
        val notes: String
    )

    internal class Running(
        /** Null only in tests (starterOverrideForTests) - a real chain always has a node. */
        val node: PostchainNode?,
        val brid: String,
        val apiPort: Int,
        val fingerprint: String,
        val nodePubkey: String,
        @Volatile var expiresAtMillis: Long,
        @Volatile var ttlTask: ScheduledFuture<*>?,
        val bridge: LocalChainRestBridge? = null
    )

    // Single scheduler thread for TTL expiry; daemon so it never blocks shutdown.
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "local-chain-ttl").apply { isDaemon = true }
    }

    // Node start runs on its own thread so a hung DB connection cannot pin the
    // caller forever; the thread is daemon and the start is bounded by
    // START_TIMEOUT_SECONDS.
    private fun newStartExecutor() = Executors.newSingleThreadExecutor { r ->
        Thread(r, "local-chain-start").apply { isDaemon = true }
    }

    internal val lock = Any()
    internal var running: Running? = null
    private var shutdownHookInstalled = false

    /** Test seam: replaces the real node bring-up when set. */
    internal var starterOverrideForTests: ((StartPlan) -> Running)? = null

    /** Test seam: replaces [startNode] INSIDE the bounded start executor when set. */
    internal var nodeStarterOverrideForTests: ((StartPlan) -> Running)? = null

    /** Test seam: shrinks [START_TIMEOUT_SECONDS] so the timeout path is testable. */
    internal var startTimeoutSecondsOverrideForTests: Long? = null

    // ------------------------------------------------------------------
    // Public entry points
    // ------------------------------------------------------------------

    fun up(
        files: Map<String, String>,
        databaseUrl: String? = System.getenv(DATABASE_URL_ENV),
        moduleArgs: Map<String, Map<String, JsonElement>> = emptyMap(),
        ttlSeconds: Long? = null,
        apiPort: Int? = null
    ): UpResult {
        val ttl = boundedTtl(ttlSeconds)
        if (databaseUrl.isNullOrBlank()) {
            return UpResult(
                ok = false,
                status = "error",
                notes = "No PostgreSQL configured: a real local chain needs a database. " +
                    "Set $DATABASE_URL_ENV (jdbc:postgresql://host:port/db?user=...&password=...) " +
                    "or pass `databaseUrl`. Pure-logic verification without a database is available " +
                    "via run_rell_tests."
            )
        }
        require(apiPort == null || apiPort in 1..65535) { "apiPort must be in 1..65535" }

        val plan = prepare(files, databaseUrl, moduleArgs, apiPort)

        synchronized(lock) {
            val current = running
            if (current != null && current.fingerprint == plan.fingerprint && isAlive(current)) {
                // Same sources, chain already up: refresh the TTL and return it.
                reschedule(current, ttl)
                return describe(current, "already_running", refreshedTtl = ttl)
            }
            if (current != null) {
                stopLocked("restarting with different sources")
            }
            val started = try {
                startBounded(plan)
            } catch (e: Exception) {
                return UpResult(ok = false, status = "error", notes = diagnose(e, plan))
            }
            running = started
            installShutdownHook()
            reschedule(started, ttl)
            return describe(started, "started", refreshedTtl = ttl)
        }
    }

    fun down(): UpResult = synchronized(lock) {
        val current = running
        if (current == null) {
            UpResult(ok = true, status = "not_running", notes = "No local chain is running.")
        } else {
            stopLocked("stopped by local_chain_up action=down")
            UpResult(
                ok = true,
                status = "stopped",
                brid = current.brid,
                notes = "Local chain ${current.brid} stopped and its node shut down."
            )
        }
    }

    fun status(): UpResult = synchronized(lock) {
        val current = running
        if (current == null || !isAlive(current)) {
            UpResult(ok = true, status = "not_running", notes = "No local chain is running. Call local_chain_up with `files` to start one.")
        } else {
            describe(current, "running", refreshedTtl = null)
        }
    }

    // ------------------------------------------------------------------
    // Planning (pure - unit-testable without a database)
    // ------------------------------------------------------------------

    internal class StartPlan(
        val configWithSigners: Gtv,
        val brid: String,
        val databaseUrl: String,
        val apiPort: Int,
        val messagingPort: Int,
        val fingerprint: String,
        val privKeyHex: String,
        val pubKeyHex: String,
        val compilerWarnings: List<String>
    )

    /**
     * Compiles sources into a full blockchain configuration Gtv and computes
     * everything needed to start the node. Throws IllegalArgumentException with
     * agent-actionable diagnostics when the sources do not compile.
     */
    internal fun prepare(
        files: Map<String, String>,
        databaseUrl: String,
        moduleArgs: Map<String, Map<String, JsonElement>>,
        requestedApiPort: Int?
    ): StartPlan {
        require(files.isNotEmpty()) { "Provide a non-empty `files` map" }
        RellCheck.requireTotalSizeWithinCap(files)
        files.keys.forEach { relPath ->
            require(!relPath.contains("..") && !Path.of(relPath).isAbsolute) { "Path must be relative without '..': $relPath" }
            require(relPath.endsWith(".rell")) { "Only .rell files are supported: $relPath" }
        }
        val collisions = files.keys.groupBy { it.lowercase().replace('\\', '/') }.filterValues { it.size > 1 }
        require(collisions.isEmpty()) {
            "Case-insensitive path collision: ${collisions.values.first()} - most file systems treat these as the same file; rename one."
        }
        val sources = RellCheck.normalizeSourceRoots(files)

        // The chain runs APP modules only - @test modules are excluded, exactly
        // like `chr build` (tests never ship in a blockchain configuration).
        val testModules = sources
            .filterValues { RunRellTests.isTestModuleSource(it) }
            .map { (path, content) -> RunRellTests.moduleNameForPath(path, content) }
            .toSet()
        val tempDir = Files.createTempDirectory("local-chain")
        try {
            sources.forEach { (relPath, content) ->
                val target = tempDir.resolve(relPath).normalize()
                require(target.startsWith(tempDir)) { "Path escapes source root: $relPath" }
                Files.createDirectories(target.parent)
                Files.writeString(target, RellCheck.stripBom(content))
            }
            // Skip provisioning when the caller submitted their own vendored
            // library tree (lib/ft4, lib/iccf), same policy as rell_check.
            val submittedVendored = RellLibs.submittedVendoredLibFileCount(sources)
            if (submittedVendored == 0 && RellLibs.needsFt4(sources)) {
                RellLibs.provisionFt4(tempDir)
            }
            val appModules = (RellLibs.userAppModules(sources) - testModules).distinct()
            require(appModules.isNotEmpty()) {
                "No app modules found - a chain needs at least one non-test module (e.g. \"main.rell\": \"module; ...\"). " +
                    "@test modules are excluded from the blockchain configuration."
            }

            val messages = java.util.concurrent.CopyOnWriteArrayList<String>()
            val cliEnv = PrinterRellCliEnv({ messages.add(it) }, { messages.add(it) })
            val configGen = try {
                RellConfigGen.create(
                    cliEnv,
                    C_SourceDir.diskDir(tempDir.toFile()),
                    appModules.map { ModuleName.of(it) }
                )
            } catch (e: RellCliException) {
                val diagnostics = messages
                    .filter { it.isNotBlank() && !RunRellTests.isVendoredFt4Warning(it) }
                    .joinToString("\n")
                    .ifBlank { e.message ?: "no compiler diagnostics captured" }
                throw IllegalArgumentException(
                    "Rell sources do not compile:\n$diagnostics\nFix with rell_check first, then retry local_chain_up.".trimEnd()
                )
            }
            val config = configGen.makeConfig(configTemplate(moduleArgs))
            val privKey = DEV_PRIV_KEY_HEX.hexStringToByteArray()
            val pubKey = secp256k1_derivePubKey(privKey)
            val configWithSigners = withSigners(config, pubKey)
            val brid = GtvToBlockchainRidFactory.calculateBlockchainRid(configWithSigners.toObject()).toHex()
            val apiPort = requestedApiPort ?: freePortIn(API_PORT_RANGE)
            return StartPlan(
                configWithSigners = configWithSigners,
                brid = brid,
                databaseUrl = databaseUrl,
                apiPort = apiPort,
                messagingPort = freePortIn(MESSAGING_PORT_RANGE),
                fingerprint = fingerprint(sources, moduleArgs, databaseUrl),
                privKeyHex = privKey.toHex(),
                pubKeyHex = pubKey.toHex(),
                compilerWarnings = messages.filter { it.isNotBlank() && !RunRellTests.isVendoredFt4Warning(it) }
            )
        } finally {
            runCatching { tempDir.toFile().deleteRecursively() }
        }
    }

    /**
     * Blockchain config template matching what `chr build` emits for a plain
     * dapp: base block strategy, GTX configuration factory, the Rell module
     * plus standard ops, and merkle_hash_version 2 (production pin - never 1).
     */
    internal fun configTemplate(moduleArgs: Map<String, Map<String, JsonElement>>): Gtv {
        val rell = mutableMapOf<String, Gtv>()
        if (moduleArgs.isNotEmpty()) {
            rell["moduleArgs"] = gtv(
                moduleArgs.mapValues { (_, args) -> gtv(args.mapValues { (_, v) -> RunRellTests.jsonToGtv(v) }) }
            )
        }
        return gtv(
            mapOf(
                "blockstrategy" to gtv(mapOf("name" to gtv("net.postchain.base.BaseBlockBuildingStrategy"))),
                "configurationfactory" to gtv("net.postchain.gtx.GTXBlockchainConfigurationFactory"),
                "gtx" to gtv(
                    mapOf(
                        "modules" to gtv(
                            listOf(
                                gtv("net.postchain.rell.module.RellPostchainModuleFactory"),
                                gtv("net.postchain.gtx.StandardOpsGTXModule")
                            )
                        ),
                        "rell" to gtv(rell)
                    )
                ),
                "merkle_hash_version" to gtv(2L)
            )
        )
    }

    internal fun withSigners(config: Gtv, pubKey: ByteArray): Gtv =
        gtv(config.asDict() + mapOf("signers" to gtv(listOf(gtv(pubKey)))))

    /** postchain AppConfig properties, mirroring chr's getDefaultNodeConfig. */
    internal fun nodeAppConfig(plan: StartPlan): AppConfig {
        val (user, password) = credentialsFromJdbcUrl(plan.databaseUrl)
        val config = BaseConfiguration().apply {
            // postchain's own http4k RestApi is ABI-incompatible with the http4k
            // version postchain-client pins (see build.gradle.kts) - disable it;
            // LocalChainRestBridge serves the REST subset on plan.apiPort instead.
            setProperty("api.port", -1)
            setProperty("debug.port", -1) // DebugApi is http4k too (defaults to 7750)
            setProperty("messaging.privkey", plan.privKeyHex)
            setProperty("messaging.pubkey", plan.pubKeyHex)
            setProperty("messaging.port", plan.messagingPort)
            setProperty("database.driverclass", "org.postgresql.Driver")
            setProperty("database.url", plan.databaseUrl)
            setProperty("database.schema", SCHEMA)
            setProperty("database.username", user)
            setProperty("database.password", password)
            setProperty("configuration.provider.node", "manual")
            setProperty("fastsync.exit_delay", 0)
        }
        return AppConfig(config)
    }

    /**
     * user/password from JDBC URL query params - the repo's
     * CHROMIA_TEST_DATABASE_URL convention carries credentials in the URL
     * (jdbc:postgresql://host/db?user=x&password=y). Defaults to postgres/postgres
     * (the stock local install) when absent.
     */
    internal fun credentialsFromJdbcUrl(jdbcUrl: String): Pair<String, String> {
        val query = jdbcUrl.substringAfter('?', "")
        val params = query.split('&').mapNotNull {
            val idx = it.indexOf('=')
            if (idx <= 0) null else it.substring(0, idx) to java.net.URLDecoder.decode(it.substring(idx + 1), Charsets.UTF_8)
        }.toMap()
        return (params["user"] ?: "postgres") to (params["password"] ?: "postgres")
    }

    internal fun fingerprint(
        sources: Map<String, String>,
        moduleArgs: Map<String, Map<String, JsonElement>>,
        databaseUrl: String
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        sources.toSortedMap().forEach { (path, content) ->
            digest.update(path.toByteArray())
            digest.update(0)
            digest.update(content.toByteArray())
            digest.update(0)
        }
        moduleArgs.toSortedMap().forEach { (module, args) ->
            digest.update(module.toByteArray())
            digest.update(args.toSortedMap().toString().toByteArray())
        }
        digest.update(databaseUrl.toByteArray())
        return digest.digest().toHex()
    }

    internal fun boundedTtl(requested: Long?): Long = when {
        requested == null -> DEFAULT_TTL_SECONDS
        else -> requested.coerceIn(MIN_TTL_SECONDS, MAX_TTL_SECONDS)
    }

    /**
     * Postchain rejects ports above 49151 (ephemeral range), so ServerSocket(0)
     * is unusable - probe the chr-adjacent ranges for a free port instead.
     * The api range starts at 7741 to leave 7740 (chr's default) alone.
     */
    internal val API_PORT_RANGE = 7741..7999
    internal val MESSAGING_PORT_RANGE = 9871..9999

    internal fun freePortIn(range: IntRange): Int {
        for (candidate in range) {
            try {
                ServerSocket(candidate).use { return candidate }
            } catch (_: java.io.IOException) {
                // busy - try the next one
            }
        }
        throw IllegalStateException("No free port in $range - stop something or pass `apiPort` explicitly.")
    }

    // ------------------------------------------------------------------
    // Node lifecycle
    // ------------------------------------------------------------------

    private fun startBounded(plan: StartPlan): Running {
        starterOverrideForTests?.let { return it(plan) }
        val timeoutSeconds = startTimeoutSecondsOverrideForTests ?: START_TIMEOUT_SECONDS
        val executor = newStartExecutor()
        // Read by the timeout path below: cancel(true) makes future.get() throw
        // CancellationException even when the callable completed, so the future
        // alone cannot hand a late-started node to cleanup.
        val lateResult = java.util.concurrent.atomic.AtomicReference<Running?>()
        val future = executor.submit<Running> {
            (nodeStarterOverrideForTests ?: ::startNode)(plan).also { lateResult.set(it) }
        }
        return try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.TimeoutException) {
            future.cancel(true)
            // The interrupt cannot stop a non-interruptible JDBC connect. If the
            // start still COMPLETES later, the node and its REST bridge used to
            // leak forever: never registered in `running`, invisible to the
            // shutdown hook, port held, and sharing the schema with the next
            // `up` (QA lens 2026-09-02). Queue the shutdown BEHIND the start
            // task on its own single thread - it runs only once the start ends.
            executor.submit {
                lateResult.get()?.let { late ->
                    runCatching { late.bridge?.close() }
                    runCatching { late.node?.shutdown() }
                    org.chromia.App.logger.info(
                        "local-chain start completed after the ${timeoutSeconds}s timeout - late node shut down"
                    )
                }
            }
            throw IllegalStateException(
                "Node start exceeded ${timeoutSeconds}s - the database at the configured URL " +
                    "may be unreachable or overloaded. Verify PostgreSQL is running and $DATABASE_URL_ENV is correct."
            )
        } catch (e: java.util.concurrent.ExecutionException) {
            throw (e.cause as? Exception ?: e)
        } finally {
            executor.shutdown()
        }
    }

    private fun startNode(plan: StartPlan): Running {
        val appConfig = nodeAppConfig(plan)
        // Rell print()/log() from operations/queries must never hit System.out -
        // in --stdio mode that is the JSON-RPC stream (same hazard run_rell_tests
        // fixed). Route to a bounded no-op printer.
        val silent = object : net.postchain.rell.base.runtime.Rt_Printer {
            override fun print(str: String) { /* discard - never System.out */ }
        }
        val env = RellPostchainModuleEnvironment(
            outPrinter = silent,
            logPrinter = silent,
            wrapCtErrors = false,
            wrapRtErrors = false
        )
        var node: PostchainNode? = null
        try {
            var started: Running? = null
            RellPostchainModuleEnvironment.set(env) {
                // wipeDb=true: every `up` starts the chain from height 0 in the
                // dedicated schema - reproducible, and immune to stale configs.
                val n = PostchainNode(appConfig, wipeDb = true)
                node = n
                val brid = net.postchain.common.BlockchainRid.buildFromHex(plan.brid)
                withReadWriteConnection(n.postchainContext.sharedStorage, CHAIN_IID) { ctx ->
                    BlockchainApi.initializeBlockchain(ctx, brid, true, plan.configWithSigners)
                }
                runStorageCommand(appConfig) { StorageInitializer.setupInitialPeers(appConfig, it) }
                val startedBrid = n.startBlockchain(CHAIN_IID)
                val engine = n.processManager.retrieveBlockchain(CHAIN_IID)?.blockchainEngine
                    ?: throw IllegalStateException("Chain started but its process is not retrievable")
                val bridge = LocalChainRestBridge(engine, startedBrid.toHex(), plan.apiPort)
                started = Running(
                    node = n,
                    brid = startedBrid.toHex(),
                    apiPort = plan.apiPort,
                    fingerprint = plan.fingerprint,
                    nodePubkey = plan.pubKeyHex,
                    expiresAtMillis = Long.MAX_VALUE,
                    ttlTask = null,
                    bridge = bridge
                )
            }
            return started!!
        } catch (e: Exception) {
            runCatching { node?.shutdown() }
            throw e
        }
    }

    private fun isAlive(chain: Running): Boolean {
        val node = chain.node ?: return true // test stub: treated as alive until stopped
        return runCatching { node.isBlockchainRunning(CHAIN_IID) }.getOrDefault(false)
    }

    private fun stopLocked(reason: String) {
        val current = running ?: return
        running = null
        current.ttlTask?.cancel(false)
        runCatching { current.bridge?.close() }
        runCatching { current.node?.shutdown() }
        org.chromia.App.logger.info("local-chain stopped ({})", reason)
    }

    private fun reschedule(chain: Running, ttlSeconds: Long) {
        chain.ttlTask?.cancel(false)
        chain.expiresAtMillis = System.currentTimeMillis() + ttlSeconds * 1000
        chain.ttlTask = scheduler.schedule(
            {
                synchronized(lock) {
                    if (running === chain) stopLocked("TTL of ${ttlSeconds}s expired")
                }
            },
            ttlSeconds,
            TimeUnit.SECONDS
        )
    }

    private fun installShutdownHook() {
        if (shutdownHookInstalled) return
        shutdownHookInstalled = true
        Runtime.getRuntime().addShutdownHook(
            Thread({ synchronized(lock) { stopLocked("JVM shutdown") } }, "local-chain-shutdown")
        )
    }

    fun stopAll() = synchronized(lock) { stopLocked("stopAll") }

    // ------------------------------------------------------------------
    // Reporting
    // ------------------------------------------------------------------

    private fun describe(chain: Running, status: String, refreshedTtl: Long?): UpResult {
        val apiUrl = "http://127.0.0.1:${chain.apiPort}"
        val expiresIn = ((chain.expiresAtMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        return UpResult(
            ok = true,
            status = status,
            brid = chain.brid,
            apiUrl = apiUrl,
            chainId = CHAIN_IID,
            nodePubkey = chain.nodePubkey,
            expiresInSeconds = expiresIn,
            notes = buildString {
                append("Local chain is up at $apiUrl (BRID ${chain.brid}). ")
                append("Query it: GET $apiUrl/query/${chain.brid}?type=<query_name>&<arg>=<value> ")
                append("or POST $apiUrl/query/${chain.brid} with {\"type\":\"<query_name>\", ...args}; ")
                append("discover the app structure with type=rell.get_app_structure. ")
                append("Submit transactions with any postchain client against $apiUrl and BRID ${chain.brid} ")
                append("(single dev signer; the node key is the public Chromia CLI dev key - local only, never for real networks). ")
                append("Auto-stops in ${expiresIn}s")
                if (refreshedTtl != null) append(" (TTL refreshed)")
                append("; call local_chain_up again to extend, or action=down to stop now.")
            }
        )
    }

    /** Actionable diagnostics for the common failure classes. */
    internal fun diagnose(e: Exception, plan: StartPlan?): String {
        val message = e.message ?: e::class.simpleName ?: "unknown error"
        val lower = message.lowercase()
        return when {
            e is IllegalArgumentException -> message // compile errors etc. - already actionable
            lower.contains("collation") ->
                "PostgreSQL database collation is incompatible: $message. Create the database with " +
                    "CREATE DATABASE ... TEMPLATE template0 ENCODING 'UTF8' LC_COLLATE 'C.UTF-8' LC_CTYPE 'C.UTF-8' " +
                    "(on Windows: LC_COLLATE 'C' LC_CTYPE 'en-US')."
            lower.contains("connection") && (lower.contains("refused") || lower.contains("timed out")) ->
                "Cannot reach PostgreSQL: $message. Verify the database in $DATABASE_URL_ENV is running and reachable."
            lower.contains("password") || lower.contains("authentication") ->
                "PostgreSQL rejected the credentials: $message. Put user/password in the JDBC URL " +
                    "(jdbc:postgresql://host:port/db?user=...&password=...)."
            lower.contains("database") && lower.contains("does not exist") ->
                "The database in the JDBC URL does not exist: $message. Create it first (CREATE DATABASE ...)."
            lower.contains("address already in use") || lower.contains("bind") ->
                "Port ${plan?.apiPort} is busy: $message. Retry without `apiPort` to pick a free port automatically."
            else -> "local_chain_up failed: $message"
        }
    }

    fun UpResult.toJson(): JsonObject = buildJsonObject {
        put("ok", ok)
        put("status", status)
        brid?.let { put("brid", it) }
        apiUrl?.let { put("apiUrl", it) }
        chainId?.let { put("chainId", it) }
        nodePubkey?.let { put("nodePubkey", it) }
        expiresInSeconds?.let { put("expiresInSeconds", it) }
        put("notes", notes)
    }
}
