package org.chromia

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import io.modelcontextprotocol.kotlin.sdk.types.Method
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.server.*
import io.ktor.server.request.path
import io.ktor.server.request.httpMethod
import io.ktor.server.response.header
import io.ktor.server.application.pluginOrNull
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import org.chromia.App.Companion.logger
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.domain.ChromiaRepository
import org.chromia.tools.McpResources
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor
import org.chromia.tools.ToolProfiles
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class App(
    private val repository: ChromiaRepository = ChromiaRepositoryImpl(),
    private val promptManager: PromptManager = PromptManager(),
    private val toolExecutor: ToolExecutor = ToolExecutor(repository, promptManager)
) {
    companion object {
        const val SERVER_NAME = "chromia-mcp-server"
        /**
         * MCP Implementation.version and /health "version".
         * Sourced from Gradle `project.version` via generated [BuildInfo]
         * (`generateBuildInfo` in `app/build.gradle.kts`).
         * Default is gradle.properties `version` (this fork's release
         * version). CI and release jobs override with `-Pversion`.
         */
        val SERVER_VERSION: String = BuildInfo.VERSION
        val logger: Logger = LoggerFactory.getLogger(App::class.java)

        /**
         * Active tool profile ([ToolProfiles]). Read from CHROMIA_MCP_PROFILE at
         * class init and overridden by `--profile <name>` in [runMain] before
         * anything is served, so /health, the chromia://server/health resource and
         * the MCP serverInfo all report the same thing to a connector.
         */
        @Volatile
        var profile: String = runCatching { ToolProfiles.resolve() }.getOrElse { e ->
            // A bad CHROMIA_MCP_PROFILE must not silently serve the full toolset;
            // runMain re-resolves and exits non-zero with this message.
            logger.error("Invalid ${ToolProfiles.PROFILE_ENV}: ${e.message}")
            ToolProfiles.FULL
        }
            private set

        internal fun applyProfile(name: String) {
            profile = name
        }

        /**
         * Tools this deployment refuses: the operator's CHROMIA_MCP_DISABLE_TOOLS
         * plus whatever the active profile removes. A profile can only ever add to
         * the disabled set, never re-enable a tool the operator turned off.
         */
        fun effectiveDisabledTools(
            env: Map<String, String> = System.getenv(),
            profileName: String = profile
        ): Set<String> = McpTools.disabledTools(env) + ToolProfiles.disabledTools(profileName)

        /**
         * MCP `serverInfo`. The title carries the active profile so a connector
         * (and a human reading ChatGPT's connector panel) can see whether it is
         * talking to the full local toolset or the public subset.
         */
        fun serverImplementation(profileName: String = profile): Implementation = Implementation(
            name = SERVER_NAME,
            version = SERVER_VERSION,
            title = "Chromia MCP Server (profile: $profileName)"
        )

        /** Streamable HTTP endpoint path, beside the root SSE endpoints. */
        const val STREAMABLE_HTTP_PATH = "/mcp"

        /** Header the Streamable HTTP transport carries its session id in. */
        const val MCP_SESSION_ID_HEADER = "mcp-session-id"

        /** Comma-separated extra `Host` values accepted by the MCP endpoints. */
        const val ALLOWED_HOSTS_ENV = "CHROMIA_MCP_ALLOWED_HOSTS"

        /**
         * `Host` values always accepted: loopback, plus a Cloudflare quick tunnel.
         *
         * kotlin-sdk 0.13 turned DNS-rebinding protection on by default for HTTP
         * transports and its allow-list is exact hostnames only - which cannot
         * express the random `<words>.trycloudflare.com` name a quick tunnel mints
         * on every start, so serve-public.ps1 could never name it in advance. This
         * list is the same defence with suffix wildcards, applied by
         * [installHostAllowList] to BOTH transports (the SSE endpoints never had
         * any Host validation at all).
         *
         * A tunnel that terminates on the client side - the OpenAI Secure MCP
         * Tunnel, `cloudflared access`, an SSH forward - needs no entry here: the
         * request reaches this process from localhost with a loopback `Host`.
         * A custom domain does: set CHROMIA_MCP_ALLOWED_HOSTS=mcp.example.com.
         */
        val DEFAULT_ALLOWED_HOSTS: List<String> = listOf(
            "localhost", "127.0.0.1", "[::1]", "*.trycloudflare.com"
        )

        /**
         * The effective `Host` allow-list: [DEFAULT_ALLOWED_HOSTS], plus whatever
         * CHROMIA_MCP_ALLOWED_HOSTS names, plus the interface the server was told
         * to bind when that is a concrete address (so `--host 192.168.1.20` works
         * without a second setting). A list containing `*` disables the check.
         */
        fun hostAllowList(configured: String?, bindHost: String? = null): List<String> {
            val extra = configured?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
            val bind = bindHost?.takeIf { it !in setOf("0.0.0.0", "::", "[::]") }
            return (DEFAULT_ALLOWED_HOSTS + extra + listOfNotNull(bind)).distinct()
        }

        /**
         * Host-header match, port-insensitive and case-insensitive. `*` matches
         * everything; a leading `*.` matches that suffix one or more labels deep
         * (`*.trycloudflare.com` matches `wide-fox-42.trycloudflare.com` but not
         * `trycloudflare.com.evil.net`, because the comparison is anchored at the
         * END of the host).
         */
        fun hostAllowed(hostHeader: String?, allowed: List<String>): Boolean {
            if ("*" in allowed) return true
            val host = hostHeader?.substringBefore('/')?.trim()?.lowercase() ?: return false
            // Strip the port: "[::1]:3001" -> "[::1]", "localhost:3001" -> "localhost".
            val name = if (host.startsWith("[")) {
                host.substringBefore(']', "").ifEmpty { return false } + "]"
            } else {
                host.substringBefore(':')
            }
            if (name.isEmpty()) return false
            return allowed.any { pattern ->
                val p = pattern.trim().lowercase()
                if (p.startsWith("*.")) name.endsWith(p.substring(1)) && name.length > p.length - 1
                else name == p
            }
        }

        /**
         * Advertise only capabilities this server actually implements.
         * Tools are registered and static (no listChanged notifications).
         * Resources are the three static snapshots below
         * (no subscribe / listChanged notifications). Prompts are not advertised:
         * the catalog is the get_prompts tool plus chromia://config/prompt-catalog.
         */
        val SERVER_CAPABILITIES = ServerCapabilities(
            tools = ServerCapabilities.Tools(listChanged = false),
            resources = ServerCapabilities.Resources(
                subscribe = false,
                listChanged = false
            )
        )

        fun healthJson(profileName: String = profile): String = """
                    {
                        "status": "healthy",
                        "server": "$SERVER_NAME",
                        "version": "$SERVER_VERSION",
                        "profile": "$profileName"
                    }
                    """.trimIndent()
    }

    internal fun createMcpServer(
        compact: Boolean = McpTools.compactToolsMode(),
        disabled: Set<String> = effectiveDisabledTools()
    ): Server {
        return Server(
            serverInfo = serverImplementation(),
            options = ServerOptions(
                capabilities = SERVER_CAPABILITIES
            )
        ).apply {
            registerTools(compact, disabled)
            registerResources()
        }
    }

    private fun Server.registerTools(compact: Boolean, disabled: Set<String>) {
        val tools = McpTools.allTools(compact = compact, disabled = disabled)

        val registeredTools = tools.map { tool ->
            RegisteredTool(tool) { request ->
                logger.debug("Request: {}", request)
                val response = toolExecutor.executeTool(request)
                logger.debug("Response: {}", response.content)

                response
            }
        }

        addTools(registeredTools)
    }

    /**
     * Connects [server] to [transport] and replaces the session's tools/call
     * handler with a deployment-aware one. The SDK keeps ONE registry that feeds
     * both tools/list and tools/call, so a registered stub for a disabled tool
     * would also be advertised - defeating the point of disabling it. Overriding
     * the per-session call handler keeps tools/list exactly the registered set
     * while a call to a disabled-but-real tool gets an actionable refusal
     * ([McpTools.disabledToolRefusal]) instead of the SDK's bare
     * "Tool X not found" (hosted probe 2026-09-01). Compact-hidden help tools
     * intentionally get NO such refusal - chromia_help covers their content and
     * they are hidden for schema savings, not disabled.
     */
    internal suspend fun createGatedSession(
        server: Server,
        transport: io.modelcontextprotocol.kotlin.sdk.shared.Transport,
        disabled: Set<String>
    ): ServerSession {
        val session = server.createSession(transport)
        // kotlin-sdk 0.15 gave RegisteredTool.handler a ClientConnection receiver
        // (a tool can ping/sample/elicit back at the client). The session's own
        // connection is internal to the SDK, but the server hands it out by id.
        val connection = server.clientConnection(session.sessionId)
        session.setRequestHandler<CallToolRequest>(Method.Defined.ToolsCall) { request, _ ->
            callToolGated(server, connection, request, disabled)
        }
        return session
    }

    internal suspend fun callToolGated(
        server: Server,
        connection: io.modelcontextprotocol.kotlin.sdk.server.ClientConnection,
        request: CallToolRequest,
        disabled: Set<String>
    ): CallToolResult {
        val registered = server.tools[request.name]
            ?: return McpTools.disabledToolRefusal(request.name, disabled)
                ?.let { org.chromia.tools.toolErrorResult(it) }
            // Same shape and text as the SDK's unknown-tool answer.
            ?: CallToolResult(
                content = listOf(TextContent("Tool ${request.name} not found")),
                isError = true
            )
        return try {
            registered.handler(connection, request)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Mirrors the SDK's handleCallTool error wrapping.
            logger.error("Error executing tool ${request.name}", e)
            CallToolResult(
                content = listOf(
                    TextContent("Error executing tool ${request.name}: ${e.message}")
                ),
                isError = true
            )
        }
    }

    private fun Server.registerResources() {
        McpResources.all(healthJson = ::healthJson).forEach { resource ->
            addResource(
                uri = resource.uri,
                name = resource.name,
                description = resource.description,
                mimeType = resource.mimeType
            ) { request ->
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = resource.readText(),
                            uri = request.uri,
                            mimeType = resource.mimeType
                        )
                    )
                )
            }
        }
    }

    /**
     * CORS for browser-based MCP clients. Without any allowed host the ktor
     * plugin rejects every cross-origin request with 403 (QA finding), so no
     * browser client could ever connect. CHROMIA_MCP_ALLOWED_ORIGINS (comma
     * separated, e.g. "https://app.example.com,http://localhost:5173") narrows
     * access to specific origins; unset (or "*") allows any origin.
     * Credentials are never allowed - the wildcard-origin-with-credentials
     * combination is unsafe, and bearer auth uses the Authorization header,
     * which browsers only send when explicitly set by the client code.
     */
    internal fun Application.installCors(allowedOrigins: String?) {
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
            allowNonSimpleContentTypes = true
            allowHeader(HttpHeaders.Authorization)
            // Streamable HTTP carries its session on a custom header, and a browser
            // client cannot continue a session it is not allowed to read back.
            allowHeader(MCP_SESSION_ID_HEADER)
            allowHeader("mcp-protocol-version")
            allowHeader("last-event-id")
            exposeHeader(MCP_SESSION_ID_HEADER)
            val origins = allowedOrigins?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
            if (origins.isEmpty() || "*" in origins) {
                anyHost()
            } else {
                origins.forEach { origin ->
                    val parts = origin.removeSuffix("/").split("://", limit = 2)
                    if (parts.size == 2) {
                        allowHost(parts[1], schemes = listOf(parts[0]))
                    } else {
                        allowHost(parts[0], schemes = listOf("http", "https"))
                    }
                }
            }
        }
    }

    /**
     * Rejects a request whose `Host` header is not in [allowed] with 403, before
     * any MCP handler sees it. This is DNS-rebinding protection: a browser page on
     * evil.com that resolves the name to 127.0.0.1 still sends `Host: evil.com`,
     * so the request never reaches a tool. /health is exempt so a load balancer or
     * `docker healthcheck` probing by IP keeps working; it exposes no tools.
     */
    internal fun Application.installHostAllowList(allowed: List<String>) {
        if ("*" in allowed) {
            logger.warn("$ALLOWED_HOSTS_ENV allows any Host - DNS-rebinding protection is off")
            return
        }
        intercept(ApplicationCallPipeline.Plugins) {
            if (call.request.path() != "/health" &&
                !hostAllowed(call.request.headers[HttpHeaders.Host], allowed)
            ) {
                call.respondText(
                    text = """{"error":"forbidden host","allowed":"$allowed"}""",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.Forbidden
                )
                finish()
            }
        }
    }

    /**
     * Runs the stdio transport and returns once the session ends. The SDK's
     * `Server.onClose` only fires on an explicit `Server.close()`; when the
     * client goes away and stdin reaches EOF only the *transport* closes, so
     * waiting on the server callback alone parked the process forever - one
     * zombie JVM per disconnected client (QA finding). Hooking the transport
     * close signal makes EOF end this call; requests in flight during normal
     * operation are unaffected because the transport only closes on EOF,
     * read error, or explicit shutdown.
     */
    fun runStdioMcpServer(
        inputStream: Source = System.`in`.asSource().buffered(),
        outputStream: Sink = System.out.asSink().buffered(),
        compact: Boolean = McpTools.compactToolsMode(),
        disabled: Set<String> = effectiveDisabledTools(),
    ) = runBlocking {
        val server = createMcpServer(compact, disabled)
        val transport = StdioServerTransport(
            input = inputStream,
            output = outputStream,
        )

        runBlocking {
            val done = Job()
            transport.onClose {
                done.complete()
            }
            server.onClose {
                done.complete()
            }
            createGatedSession(server, transport, disabled)
            done.join()
        }
    }

    /**
     * Startup docs-index warmup. A no-op when every RAG-backed docs tool
     * (search/fetch_docs/fetch) is disabled via CHROMIA_MCP_DISABLE_TOOLS:
     * the index can never be queried, so a lite hosted config must not pay
     * the embeddings load memory spike at boot (512MB instances crash-looped
     * on it). RagStore stays lazy for the enabled case - this only decides
     * whether to trigger the existing warmup, never how the store loads.
     */
    suspend fun warmUpDocs(docsToolsDisabled: Boolean = McpTools.docsToolsDisabled(effectiveDisabledTools())) {
        if (docsToolsDisabled) {
            logger.info("docs tools disabled - skipping index warmup")
            return
        }
        toolExecutor.warmUpDocs()
    }

    /** The most recent [warmDocsInBackground] job, for tests and diagnostics. */
    @Volatile
    var docsWarmup: Job? = null
        private set

    /**
     * [warmUpDocs] on a scope of its own, detached from the caller's
     * `runBlocking`: the stdio server returns on stdin EOF and `main` must exit
     * right then, not after a download that a client which already left will
     * never read. The IO thread dies with the process (`exitProcess`).
     *
     * Why stdio warms at all (2026-09-05): Claude Code starts this process at
     * session start and the first `fetch_docs` comes seconds to minutes later.
     * Loading lazily on that call made the agent wait for the whole index load
     * (3.7 s from the binary cache, ~15 s with the download); loading from
     * spawn makes the same call answer from a loaded index.
     */
    fun warmDocsInBackground(docsToolsDisabled: Boolean = McpTools.docsToolsDisabled(effectiveDisabledTools())): Job =
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch { warmUpDocs(docsToolsDisabled) }.also { docsWarmup = it }

    fun runSseMcpServer(
        host: String,
        port: Int,
        wait: Boolean = true,
        authToken: String? = System.getenv("CHROMIA_MCP_AUTH_TOKEN")?.takeIf { it.isNotBlank() },
        allowedOrigins: String? = System.getenv("CHROMIA_MCP_ALLOWED_ORIGINS")?.takeIf { it.isNotBlank() },
        compact: Boolean = McpTools.compactToolsMode(),
        disabled: Set<String> = effectiveDisabledTools(),
        allowedHosts: String? = System.getenv(ALLOWED_HOSTS_ENV)?.takeIf { it.isNotBlank() }
    ): EmbeddedServer<*, *> {
        val server = embeddedServer(CIO, host = host, port = port) {
            installCors(allowedOrigins)
            // Optional bearer auth for hosted deployments (CHROMIA_MCP_AUTH_TOKEN).
            // /health stays open for load-balancer checks. Off by default so
            // no-auth connectors (e.g. ChatGPT) keep working unless opted in.
            // Match on the PATH, not the raw URI: "/health?x=1" is still the
            // health endpoint, but the raw-URI comparison 401'd any query
            // string (QA finding). /health is the only public endpoint.
            if (authToken != null) {
                // Constant-time compare: a plain != leaks match-length timing.
                val expectedAuth = "Bearer $authToken".toByteArray()
                intercept(io.ktor.server.application.ApplicationCallPipeline.Plugins) {
                    val presented = call.request.headers[io.ktor.http.HttpHeaders.Authorization]
                        ?.toByteArray() ?: ByteArray(0)
                    if (call.request.path() != "/health" &&
                        !java.security.MessageDigest.isEqual(expectedAuth, presented)
                    ) {
                        call.respondText(
                            text = """{"error":"unauthorized"}""",
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.Unauthorized
                        )
                        finish()
                    }
                }
            }
            installHealthEndpoint()
            installMcpJson()
            installHostAllowList(hostAllowList(allowedHosts, host))
            installMcpSse(compact, disabled)
            installMcpStreamableHttp(compact, disabled)
        }.start(wait = wait)
        return server
    }

    /**
     * ktor's SSE plugin, installed at most once. Both transports need it (the
     * Streamable HTTP GET stream is an SSE stream too) and ktor throws
     * DuplicatePluginException on a second install.
     */
    internal fun Application.installSseOnce() {
        if (pluginOrNull(SSE) == null) install(SSE)
    }

    /**
     * ContentNegotiation configured with the SDK's [McpJson]. The Streamable HTTP
     * transport answers a POST through `call.respond(<JSONRPCMessage>)`, which
     * needs a serializer registered for the call; and it must be McpJson, not a
     * default Json - MCP requires `explicitNulls = false` / `encodeDefaults = true`,
     * and a plain `json()` emits nulls that clients reject. SSE is unaffected (it
     * writes its own frames), so this is additive.
     */
    internal fun Application.installMcpJson() {
        if (pluginOrNull(ContentNegotiation) == null) {
            install(ContentNegotiation) { json(McpJson) }
        }
    }

    /**
     * Streamable HTTP (MCP spec 2025-03-26 and later) at [STREAMABLE_HTTP_PATH],
     * beside the root SSE endpoints - same process, same server, same auth
     * interceptor, same CORS rule, same /health.
     *
     * Stateful: the first POST (an `initialize`) mints an `Mcp-Session-Id`, and
     * every later POST/GET/DELETE carrying that header lands on the same transport
     * and the same gated MCP session. `enableJsonResponse = true` matches the SDK's
     * own `Application.mcpStreamableHttp` default, so a plain request/response pair
     * comes back as JSON and a client that never opens the GET stream still works.
     *
     * Hand-wired for the same reason [installMcpSse] is: the SDK's DSL calls
     * `server.createSession(transport)` itself, which would skip
     * [createGatedSession] and lose the disabled-tool refusal. The pieces are the
     * SDK's own - only the session creation and the transport map are ours.
     *
     * Keepalive: [heartbeatMillis] drives ktor's SSE heartbeat on the standalone
     * GET stream, so an idle stream keeps proxies (a Cloudflare tunnel, a corporate
     * load balancer) from cutting it, exactly as the SSE endpoint's comment loop
     * does. A POST-only client never opens that stream and needs no keepalive.
     */
    internal fun Application.installMcpStreamableHttp(
        compact: Boolean,
        disabled: Set<String>,
        path: String = STREAMABLE_HTTP_PATH,
        transports: java.util.concurrent.ConcurrentHashMap<String, StreamableHttpServerTransport> =
            java.util.concurrent.ConcurrentHashMap(),
        heartbeatMillis: Long = 15_000
    ) {
        installSseOnce()
        installMcpJson()
        routing {
            route(path) {
                // Two things ktor's `sse {}` cannot do from inside the handler,
                // because it has already committed the response by then: answer a
                // GET whose session does not exist with a status, and set the
                // Mcp-Session-Id response header the spec asks for on the stream.
                // Both belong before the handler. (The SDK's own DSL does the same.)
                intercept(ApplicationCallPipeline.Plugins) {
                    if (call.request.httpMethod == HttpMethod.Get) {
                        val sessionId = call.request.headers[MCP_SESSION_ID_HEADER]
                        val known = sessionId?.let { transports[it] } != null
                        if (!known) {
                            call.respondText(
                                text = """{"error":"no such MCP session","header":"$MCP_SESSION_ID_HEADER"}""",
                                contentType = ContentType.Application.Json,
                                status = if (sessionId == null) HttpStatusCode.BadRequest else HttpStatusCode.NotFound
                            )
                            finish()
                        } else {
                            call.response.header(MCP_SESSION_ID_HEADER, sessionId!!)
                        }
                    }
                }
                post {
                    val transport = streamableTransport(transports, compact, disabled) ?: return@post
                    transport.handleRequest(null, call)
                }
                sse {
                    // The interceptor above guarantees a live session here.
                    val sessionId = call.request.headers[MCP_SESSION_ID_HEADER]
                        ?: error("the interceptor above rejects a GET without a session id")
                    val transport = transports[sessionId]
                        ?: error("the interceptor above rejects a GET whose session is unknown")
                    heartbeat {
                        period = heartbeatMillis.milliseconds
                        event = io.ktor.sse.ServerSentEvent(comments = "keepalive")
                    }
                    transport.handleRequest(this, call)
                }
                delete {
                    val sessionId = call.request.headers[MCP_SESSION_ID_HEADER]
                    val transport = sessionId?.let { transports[it] }
                    if (transport == null) {
                        call.respond(HttpStatusCode.NotFound, "Session not found")
                        return@delete
                    }
                    transport.handleRequest(null, call)
                    transports.remove(sessionId)
                }
            }
        }
    }

    /**
     * The transport a Streamable HTTP POST belongs to: an existing session when the
     * request carries a known `Mcp-Session-Id`, otherwise a fresh transport wired to
     * a fresh gated MCP session (the `initialize` that mints the id). Answers the
     * call and returns null when the header names a session this process does not
     * have - the client must start a new one.
     */
    private suspend fun io.ktor.server.routing.RoutingContext.streamableTransport(
        transports: java.util.concurrent.ConcurrentHashMap<String, StreamableHttpServerTransport>,
        compact: Boolean,
        disabled: Set<String>
    ): StreamableHttpServerTransport? {
        val sessionId = call.request.headers[MCP_SESSION_ID_HEADER]
        if (sessionId != null) {
            return transports[sessionId] ?: run {
                call.respond(HttpStatusCode.NotFound, "Session not found")
                null
            }
        }
        val transport = StreamableHttpServerTransport(
            StreamableHttpServerTransport.Configuration(enableJsonResponse = true)
        )
        transport.setOnSessionInitialized { id -> transports[id] = transport }
        transport.setOnSessionClosed { id -> transports.remove(id) }
        val server = createMcpServer(compact, disabled)
        server.onClose { transport.sessionId?.let { transports.remove(it) } }
        createGatedSession(server, transport, disabled)
        return transport
    }

    /**
     * SSE transport wiring, functionally identical to the SDK's
     * `Application.mcp {}` plugin (same root SSE + POST endpoints, same status
     * codes) except each session is connected through [createGatedSession] so
     * disabled-but-real tools refuse with guidance. The SDK plugin never exposes
     * the session it creates, and the SDK's tools/list and tools/call share one
     * registry, so this is the only way to gate calls without also advertising
     * stubs in tools/list.
     *
     * One deliberate divergence: the SDK plugin removes a transport from its
     * map only via `server.onClose`, which fires only on an explicit
     * `server.close()` - never when the client disconnects - so it leaks one
     * transport (plus the Server closure it holds) per connection for the
     * lifetime of the process (e2e transport probe 2026-09-01). Two pieces fix
     * that here:
     *  - the keepalive loop: CIO only notices a dead client when it WRITES
     *    (the SDK's `awaitCancellation` alone never ends - TCP half-close is
     *    invisible to a silent server), so the handler sends an SSE comment
     *    every [heartbeatMillis]; the send to a dead socket throws, ending the
     *    handler. Bonus: the comments keep proxy idle timeouts (e.g. hosted
     *    load balancers) from silently killing quiet sessions.
     *  - the `finally`: removes the map entry however the handler ends, so a
     *    POST to a dead session 404s like any unknown session.
     *
     * [transports] and [heartbeatMillis] are injectable so tests can observe
     * that cleanup quickly.
     */
    internal fun Application.installMcpSse(
        compact: Boolean,
        disabled: Set<String>,
        transports: java.util.concurrent.ConcurrentHashMap<String, SseServerTransport> =
            java.util.concurrent.ConcurrentHashMap(),
        heartbeatMillis: Long = 15_000
    ) {
        installSseOnce()
        routing {
            sse {
                val transport = SseServerTransport("", this)
                transports[transport.sessionId] = transport
                try {
                    val server = createMcpServer(compact, disabled)
                    server.onClose { transports.remove(transport.sessionId) }
                    createGatedSession(server, transport, disabled)
                    while (true) {
                        kotlinx.coroutines.delay(heartbeatMillis)
                        send(io.ktor.sse.ServerSentEvent(comments = "keepalive"))
                    }
                } catch (e: java.io.IOException) {
                    logger.debug("SSE session ${transport.sessionId} ended: ${e.message}")
                } finally {
                    transports.remove(transport.sessionId)
                }
            }
            post {
                val sessionId = call.request.queryParameters["sessionId"]
                if (sessionId == null) {
                    call.respond(HttpStatusCode.BadRequest, "sessionId query parameter is not provided")
                    return@post
                }
                val transport = transports[sessionId]
                if (transport == null) {
                    call.respond(HttpStatusCode.NotFound, "Session not found")
                    return@post
                }
                transport.handlePostMessage(call)
            }
        }
    }

}

fun Application.installHealthEndpoint() {
    routing {
        get("/health") {
            call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
                text = App.healthJson()
            )
        }
    }
}

/**
 * Exits with [runMain]'s code. The explicit [exitProcess] both surfaces
 * startup failures to supervisors/CI (failures used to fall out of main and
 * exit 0, QA finding) and guarantees the JVM dies after stdin EOF instead of
 * being kept alive by lingering non-daemon worker threads (zombie JVM,
 * QA finding).
 */
fun main(args: Array<String>): Unit = exitProcess(runMain(args))

/**
 * Argument dispatch, separated from [main] so tests can assert exit codes
 * without killing the test JVM. Returns 0 on success, non-zero on any
 * startup failure (bad arguments, unknown command, bind failure, ...).
 */
internal fun runMain(args: Array<String>, appFactory: () -> App = { App() }): Int = runBlocking {
    val arg = args.firstOrNull() ?: "--stdio"
    if (arg == "--generate-embeddings" || arg == "--generate-embeddings-no-upload") {
        val upload = arg == "--generate-embeddings"
        logger.info(if (upload) "Starting embeddings generation" else "Starting embeddings generation (no upload)")
        RagStore(loadFromRegistry = false).createAndUploadEmbeddings(upload = upload)
        logger.info("Embeddings generation finished")
        return@runBlocking 0
    }
    val app = appFactory()
    when (arg) {
        // --http is an alias: the same server has served BOTH transports since the
        // 0.15 SDK bump, and "sse" is the older of the two names. Nothing a client
        // already points at changes.
        "--sse", "--http" -> {
            val options = try {
                parseSseArgs(args.drop(1))
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to start SSE server --> ${e.message}")
                logger.error(USAGE_HELP)
                return@runBlocking 1
            }
            // Apply the profile BEFORE anything is created: createMcpServer,
            // /health and serverInfo all read App.profile.
            App.applyProfile(options.profile)
            logger.info(
                "profile={} disabled={}",
                options.profile,
                App.effectiveDisabledTools().sorted().joinToString(",").ifEmpty { "-" }
            )
            // Warm the RAG store/model in the background: production telemetry
            // showed ~15s first-search latency per fresh instance without it.
            val warmup = launch { app.warmUpDocs() }
            try {
                app.runSseMcpServer(options.host, options.port)
                0
            } catch (e: Exception) {
                // Bind/startup failures (port in use, bad address, ...) must
                // exit non-zero so supervisors and CI notice (QA finding).
                warmup.cancel()
                logger.error("Failed to start SSE server --> ${e.message}")
                1
            }
        }
        "--stdio" -> {
            // Detached on purpose: this runBlocking must return on stdin EOF even
            // mid-download (see App.warmDocsInBackground).
            app.warmDocsInBackground()
            app.runStdioMcpServer()
            0
        }
        else -> {
            logger.error("""
                Unknown command argument: $arg

                $USAGE_HELP
            """.trimMargin())
            1
        }
    }
}
