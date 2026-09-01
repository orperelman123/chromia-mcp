package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Offline translator for cryptic errors from anywhere in the Chromia stack:
 * Rell compiler diagnostics, chr CLI, postchain runtime, the postgres under
 * postchain, explorer/GraphQL, FT4, and this MCP server's own messages.
 *
 * Engine: a curated ORDERED rule table (first match wins) - substring/regex
 * pattern -> {meaning, likelyCause, nextAction, relatedTools}. No LLM calls,
 * no network. Rules are mined from real material: docs/UPSTREAM.md (all 11
 * verified upstream gotchas), postchain engine texts documented in
 * docs/knowledge/briefs/postchain.md, compiler diagnostics exercised by this
 * repo's tests, and the validation errors this server itself emits.
 *
 * When nothing matches the tool says so (matched=false) and returns generic
 * triage guidance plus docs-search terms extracted from the error text -
 * it never pretends to know.
 */
object ErrorTranslator {

    /** ~8 KB cap on the pasted error text. */
    const val MAX_ERROR_CHARS = 8192

    /** Cap on the optional free-text context. */
    const val MAX_CONTEXT_CHARS = 2048

    data class Rule(
        val id: String,
        val family: String,
        val pattern: Regex,
        val meaning: String,
        val likelyCause: String,
        val nextAction: String,
        val relatedTools: List<String> = emptyList()
    )

    private fun rule(
        id: String,
        family: String,
        pattern: String,
        meaning: String,
        likelyCause: String,
        nextAction: String,
        relatedTools: List<String> = emptyList()
    ) = Rule(
        id = id,
        family = family,
        pattern = Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        meaning = meaning,
        likelyCause = likelyCause,
        nextAction = nextAction,
        relatedTools = relatedTools
    )

    /**
     * Ordered rule table - first match wins, so specific patterns (this
     * server's own messages, explorer schema errors) come before broad ones
     * (generic Rell diagnostics, timeouts).
     */
    val rules: List<Rule> = listOf(

        // ---- this MCP server's own messages -------------------------------
        rule(
            id = "own_tool_failed_null",
            family = "mcp-server",
            pattern = "tool execution failed: null",
            meaning = "A tool threw an exception but its message was lost - you got the generic wrapper with no reason.",
            likelyCause = "Upstream chromia-mcp builds the error result inside Result.onFailure and discards it (docs/UPSTREAM.md #4), or the exception genuinely had a null message.",
            nextAction = "Retry against this fork (it preserves e.message), check the server's stderr/file log for the underlying stack trace, and re-check the call's arguments against the tool schema.",
        ),
        rule(
            id = "own_unknown_tool",
            family = "mcp-server",
            pattern = "unknown tool:",
            meaning = "The MCP server has no tool registered under that name.",
            likelyCause = "Typo in the tool name, a *_help tool hidden by compact mode (CHROMIA_MCP_COMPACT_TOOLS=true), or the tool was dropped via CHROMIA_MCP_DISABLE_TOOLS.",
            nextAction = "List the advertised tools; for help topics call chromia_help without a topic to get the index, then chromia_help(topic).",
            relatedTools = listOf("chromia_help")
        ),
        rule(
            id = "own_missing_parameter",
            family = "mcp-server",
            pattern = "missing required parameter",
            meaning = "A required argument was omitted or blank in the tool call.",
            likelyCause = "The call left out the parameter named in the message, or passed it as an empty/whitespace string.",
            nextAction = "Re-issue the call with the named parameter set to a non-blank value, per the tool's inputSchema.",
        ),
        rule(
            id = "own_doc_not_found",
            family = "mcp-server",
            pattern = "documentation not found",
            meaning = "The docs id passed to fetch/fetch_docs does not exist in the RAG index.",
            likelyCause = "A guessed or stale document id - ids are only stable within what `search` returned.",
            nextAction = "Call `search` with your query first and pass one of the returned result ids to `fetch` verbatim.",
            relatedTools = listOf("search", "fetch")
        ),
        rule(
            id = "own_pagination_limit",
            family = "mcp-server",
            pattern = "limit must not exceed",
            meaning = "The requested page size is above this server's pagination ceiling (1000).",
            likelyCause = "An attempt to fetch everything in one call instead of paginating.",
            nextAction = "Lower `limit` (schemas default to 10-50) and page with `offset`/timestamps instead.",
        ),
        // Validator/security findings this server itself emits (the tool
        // advertises coverage of its own messages - real-world round 2 D6b).
        // Must precede ft4_open_registration: a finding text quoting ras_open
        // would otherwise match that broader rule first.
        rule(
            id = "own_forbidden_module",
            family = "mcp-server",
            pattern = "forbidden ft4 production module|banned-module",
            meaning = "This server's validator flagged an FT4 admin module or open registration strategy that must never ship in a production dApp.",
            likelyCause = "App code imports lib.ft4.admin / admin.crosschain (or references ras_open / an open strategy module), or chromia.yml pulls an admin module via libs. moduleArgs KEYS naming admin modules (admin_pubkey configuration) are legitimate and are not flagged; @test modules are exempt.",
            nextAction = "Remove the admin/open-strategy import from production modules (use a gated registration strategy). Deliberately building admin/ops tooling? Pass allowAdminModules:true to check_dapp_project / rell_security_check / check_ft4_imports to downgrade these findings.",
            relatedTools = listOf("check_ft4_imports", "check_dapp_project", "rell_security_check")
        ),
        rule(
            id = "own_unauthenticated_mutation",
            family = "mcp-server",
            pattern = "unauthenticated-mutation",
            meaning = "This server's security scan found an operation that creates/updates/deletes state without any authentication check.",
            likelyCause = "The operation body (and its helper call chain) contains no auth marker - no ft4 auth.authenticate(), op_context.is_signer, require_signer, or ICCF require_valid_proof.",
            nextAction = "Authenticate the caller before mutating (ft4 auth.authenticate() or an explicit signer check). If auth lives in a helper the scan missed, submit the helper's file too - the call graph spans all submitted files. Test-only code is downgraded automatically (-test-surface).",
            relatedTools = listOf("rell_security_check", "chromia_rell_practices_help")
        ),
        rule(
            id = "own_hardcoded_key_material",
            family = "mcp-server",
            pattern = "hardcoded-key-material",
            meaning = "This server's security scan found a 64+ char hex literal that looks like key material or a chain RID embedded in source.",
            likelyCause = "A private/public key, BRID, or similar secret pasted into a .rell file instead of arriving via module args or configuration.",
            nextAction = "Move the value to chromia.yml moduleArgs (or test setup) and read it from module_args; never commit key material in source. If it is a well-known public constant, keep it but expect the finding.",
            relatedTools = listOf("rell_security_check", "ft4_module_args")
        ),
        rule(
            id = "own_source_too_large",
            family = "mcp-server",
            pattern = "total rell source size",
            meaning = "The submitted Rell sources exceed the in-process compiler tools' ~2 MB cap.",
            likelyCause = "A whole vendored tree (e.g. lib/ft4 plus generated files) was pasted along with the app code.",
            nextAction = "Submit only your app sources - the FT4 v1.1.0r library is already vendored server-side, so drop lib/ft4 from the files map.",
            relatedTools = listOf("rell_check", "check_dapp_project")
        ),

        // ---- explorer / GraphQL -------------------------------------------
        rule(
            id = "explorer_recaptcha",
            family = "explorer",
            pattern = "recaptcha",
            meaning = "The explorer rejected the call because it now requires a browser reCAPTCHA token for this query.",
            likelyCause = "explorer.chromia.com gates getNodeUnavailability (and possibly other queries) behind reCAPTCHA (docs/UPSTREAM.md #7a) - every programmatic client is blocked, not just yours.",
            nextAction = "There is no programmatic path until the explorer offers one - view node unavailability in the explorer web UI, and use the other analytics tools for what they still serve.",
            relatedTools = listOf("get_network_stats")
        ),
        rule(
            id = "explorer_testnet_400",
            family = "explorer",
            pattern = "(?=.*testnet)(?=.*\\b400\\b)",
            meaning = "The explorer API returned HTTP 400 for a testnet request that would succeed on mainnet.",
            likelyCause = "explorer.chromia.com currently rejects network=testnet outright (docs/UPSTREAM.md #9) even though tools advertise it.",
            nextAction = "Retry the same call with network=mainnet; treat testnet analytics as unavailable until the explorer serves it again.",
            relatedTools = listOf("get_network_stats")
        ),
        rule(
            id = "graphql_unknown_argument",
            family = "explorer",
            pattern = "unknownargument",
            meaning = "The GraphQL query passes an argument the explorer schema no longer declares.",
            likelyCause = "Explorer schema drift - e.g. getAssetTopHolders renamed excludeAccounts to excludedAccounts while getAssetDistribution kept the old name (docs/UPSTREAM.md #2). Outdated clients hit this on every call.",
            nextAction = "Introspect the live schema for the current argument name and update the query; if this came from an official chromia-mcp build, use a build with the rename applied.",
        ),
        rule(
            id = "graphql_field_undefined",
            family = "explorer",
            pattern = "fieldundefined",
            meaning = "The GraphQL query selects a field that no longer exists at that position in the explorer schema.",
            likelyCause = "Explorer schema drift - e.g. top-level groupedTransactionsByCluster was removed and now lives only under dashboardData (docs/UPSTREAM.md #3).",
            nextAction = "Introspect the schema to find where the field moved and requery it at the new path (dashboardData { ... } for cluster transaction groups).",
            relatedTools = listOf("get_transactions_by_cluster")
        ),
        rule(
            id = "graphql_wrong_type_variable",
            family = "explorer",
            pattern = "wrongtype|has an invalid value",
            meaning = "A GraphQL variable was sent with the wrong JSON shape for its declared type.",
            likelyCause = "Classic upstream bug: list variables serialized via toString(), sending the string \"[FT4_USER]\" instead of the array [\"FT4_USER\"] (docs/UPSTREAM.md #1). The explorer may also answer HTTP 200 with silently EMPTY results for this.",
            nextAction = "Encode list variables as real JSON arrays; if a list filter returns suspiciously empty data with no error, assume the same serialization bug.",
        ),
        rule(
            id = "http_rate_limited",
            family = "explorer",
            pattern = "too many requests|http 429|status(?: code)?[ :=]{1,3}429",
            meaning = "The remote service is rate-limiting you (HTTP 429).",
            likelyCause = "Too many calls in a short window - common when an agent loops over analytics queries.",
            nextAction = "Back off and retry with exponential delay; batch or cache results instead of re-querying per item.",
        ),
        rule(
            id = "http_unavailable",
            family = "explorer",
            pattern = "service unavailable|bad gateway|http 50[23]|status(?: code)?[ :=]{1,3}50[23]",
            meaning = "The remote endpoint (explorer or node) is temporarily down or overloaded (HTTP 502/503).",
            likelyCause = "Transient outage or restart on the server side - not a problem with your request.",
            nextAction = "Retry after a short delay; if it persists, check get_network_stats / the Chromia status page rather than changing your call.",
            relatedTools = listOf("get_network_stats")
        ),

        // ---- GTV / serialization ------------------------------------------
        rule(
            id = "gtv_big_integer_json",
            family = "gtv",
            pattern = "big_integer cannot be serialized",
            meaning = "The chain answered successfully, but the client failed to render a big_integer value as JSON.",
            likelyCause = "postchain's default make_gtv_gson() registers a BIGINTEGER branch that throws (docs/UPSTREAM.md #8) - and every FT4 balance/total_supply/amount is a big_integer, so the obvious builder fails on the most common data.",
            nextAction = "Serialize responses with makeStrictGtvGson() (encodes big integers as JSON strings) instead of make_gtv_gson(); the query itself needs no change.",
            relatedTools = listOf("chromia_dapp_query")
        ),
        rule(
            id = "gtv_double",
            family = "gtv",
            pattern = "cannot convert object of type double to gtv",
            meaning = "A JSON number with a decimal point cannot be converted to a GTV argument.",
            likelyCause = "GTV has no double type - query/operation arguments were passed as JSON floats (e.g. an amount like 1.5 or an integer that your JSON library rendered as 10.0).",
            nextAction = "Pass integers as plain integers and token amounts in raw units as integers or decimal STRINGS; re-check argument types with rell.get_app_structure.",
            relatedTools = listOf("chromia_dapp_query")
        ),

        // ---- postchain runtime / postgres ---------------------------------
        rule(
            id = "postchain_missing_metadata",
            family = "postchain",
            pattern = "missing metadata entities",
            meaning = "postchain found tables from a previous/other chain in its database that its metadata does not describe.",
            likelyCause = "Two node instances or successive test runs sharing one postgres database/schema - the second run collides with the first run's chain tables.",
            nextAction = "Give each node/test instance its own database (or schema), or wipe the database before rerunning (chr node start --wipe for local dev).",
            relatedTools = listOf("chr_node_help")
        ),
        rule(
            id = "postgres_collation",
            family = "postgres",
            pattern = "collation|lc_collate",
            meaning = "postchain's startup collation check failed - the postgres database was created with a locale postchain rejects.",
            likelyCause = "The database was initialized with a default locale (e.g. en_US.UTF-8 on many distros); the engine check demands C-collation ordering ('A'<'a').",
            nextAction = "Recreate the database with LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8' (the engine's own error text); database.suppressCollationCheck only downgrades it to a warning.",
            relatedTools = listOf("chr_node_help")
        ),
        rule(
            id = "postgres_conn_refused",
            family = "postgres",
            pattern = "(?=.*(?:postgres|postgresql|5432))(?=.*(?:connection refused|could not connect|connection to .{0,80}refused))",
            meaning = "Nothing is listening where the node/tests expect postgres.",
            likelyCause = "postgres is not running, is on a different host/port than configured (database.url / CHROMIA_TEST_DATABASE_URL), or a container port is not mapped.",
            nextAction = "Start postgres (e.g. the Chromia docker-compose postgres) and verify the connection URL host/port; then rerun.",
            relatedTools = listOf("run_rell_tests", "chr_node_help")
        ),
        rule(
            id = "postgres_auth_failed",
            family = "postgres",
            pattern = "password authentication failed",
            meaning = "postgres rejected the node's credentials.",
            likelyCause = "Wrong user/password in database.url (node config) or CHROMIA_TEST_DATABASE_URL, or the postgres user was never created.",
            nextAction = "Fix the credentials in the connection URL (postgresql://user:password@host:5432/db) or create the user/database in postgres, then rerun.",
            relatedTools = listOf("run_rell_tests")
        ),
        rule(
            id = "postgres_missing_relation",
            family = "postgres",
            pattern = "relation \"?[\\w.]+\"? does not exist",
            meaning = "A query hit a postgres table that does not exist in this database.",
            likelyCause = "The database is fresh/wiped and postchain has not initialized its schema yet, the connection points at the wrong database/schema, or something dropped tables mid-run.",
            nextAction = "Verify the connection URL targets the node's database, and (re)start the node so postchain creates its schema; for tests, let the test runner own the database.",
            relatedTools = listOf("chr_node_help")
        ),
        rule(
            id = "jvm_oom",
            family = "runtime",
            pattern = "outofmemoryerror|gc overhead limit|java heap space",
            meaning = "The JVM ran out of heap memory.",
            likelyCause = "A memory-constrained deployment (e.g. a 512 MB hosted instance) running the heavy in-process Rell compiler tools or loading the embeddings index.",
            nextAction = "Raise the heap (-Xmx) or, on small hosts, disable the heavy tools: CHROMIA_MCP_DISABLE_TOOLS=rell_check,rell_security_check,run_rell_tests (docs tools disabled also skip the embeddings warmup).",
        ),
        rule(
            id = "http_gzip",
            family = "runtime",
            pattern = "not in gzip format|zipexception",
            meaning = "Something tried to gunzip bytes that are not (or are no longer) gzip data.",
            likelyCause = "GZIP double-decode: the HTTP client already decompressed the response transparently and application code gunzips it again - or a Content-Encoding header lies about the body.",
            nextAction = "Decompress in exactly one place: either let the HTTP client handle Content-Encoding (and drop manual GZIPInputStream), or disable transparent decompression and decode manually.",
        ),
        rule(
            id = "net_timeout",
            family = "runtime",
            pattern = "sockettimeoutexception|connect timed out|read timed out|request timed out|timed out (?:after|waiting)",
            meaning = "The remote side did not answer within the client's time budget.",
            likelyCause = "A slow or unreachable node/explorer, a cold-starting hosted service, or a long-running query.",
            nextAction = "Retry once; if it persists, check the target URL is reachable, prefer a lighter query (pagination, narrower time range), and only then raise the client timeout.",
            relatedTools = listOf("get_network_stats")
        ),

        // ---- MCP transport ------------------------------------------------
        rule(
            id = "stdio_corrupt_json",
            family = "mcp-transport",
            pattern = "is not valid json|not valid json|failed to parse json|invalid json-?rpc|error parsing json",
            meaning = "The MCP client received bytes on stdout that are not JSON-RPC - the stdio protocol stream is polluted.",
            likelyCause = "The server (or a library like Ktor's default Logging plugin) wrote log lines to stdout, the same stream MCP JSON-RPC uses in stdio mode (docs/UPSTREAM.md #5).",
            nextAction = "Route ALL server logging to stderr or a file in stdio mode (this fork does); never println from tool code.",
        ),

        // ---- chr CLI / chromia.yml ----------------------------------------
        rule(
            id = "chr_unknown_version",
            family = "chr-cli",
            pattern = "unknown version",
            meaning = "chr does not recognize a version pinned in chromia.yml (typically rellVersion).",
            likelyCause = "chromia.yml pins a rellVersion newer than the installed chr supports - e.g. pinning past 0.16.1 breaks `chr build` on current CLIs.",
            nextAction = "Pin rellVersion: 0.16.1 (the newest broadly-supported pin) or upgrade the Chromia CLI; validate the file with validate_chromia_yml.",
            relatedTools = listOf("validate_chromia_yml", "chr_build_help")
        ),
        rule(
            id = "chr_no_project",
            family = "chr-cli",
            pattern = "project settings file not found|chromia\\.ya?ml (?:was )?not found|no chromia\\.ya?ml",
            meaning = "The command ran outside a Chromia project - no chromia.yml was found.",
            likelyCause = "Wrong working directory, or the project was never scaffolded.",
            nextAction = "cd to the directory containing chromia.yml (the project root) and rerun; to start a new project use scaffold_dapp.",
            relatedTools = listOf("scaffold_dapp", "chromia_project_structure_help")
        ),
        rule(
            id = "chr_not_installed",
            family = "chr-cli",
            pattern = "chr\\W{0,3}is not recognized|chr: command not found|command not found: chr",
            meaning = "The chr CLI is not installed or not on PATH.",
            likelyCause = "The Chromia CLI was never installed on this machine, or the shell PATH does not include it.",
            nextAction = "Install the Chromia CLI (docs.chromia.com setup) - meanwhile compile, security-scan and test Rell fully in-process with rell_check / rell_security_check / run_rell_tests (no chr needed).",
            relatedTools = listOf("rell_check", "rell_security_check", "run_rell_tests")
        ),

        // ---- Rell compiler ------------------------------------------------
        rule(
            id = "rell_syntax",
            family = "rell-compiler",
            pattern = "syntax error",
            meaning = "The Rell parser could not parse the source at the reported file(line:column).",
            likelyCause = "A typo near that position - missing semicolon/brace/paren, a keyword misuse, or non-Rell syntax pasted in.",
            nextAction = "Fix the code at the reported position and iterate with rell_check until it compiles; see chromia_rell_statements_help for statement syntax.",
            relatedTools = listOf("rell_check", "chromia_rell_statements_help")
        ),
        // Before rell_unknown_module: a lib.* module is an external LIBRARY
        // dependency, not a mislaid file - the path-vs-import advice sent
        // agents chasing a nonexistent bug in their own code (round 2 D6a).
        rule(
            id = "rell_lib_module_not_found",
            family = "rell-compiler",
            pattern = "module ['\"‘“]?lib\\.[\\w.]+['\"’”]? not found",
            meaning = "The compiler cannot resolve a lib.* module - a missing external library dependency, not a typo in your own module paths.",
            likelyCause = "The library was never installed: real projects declare it under chromia.yml `libs:` and run `chr install`, which vendors it under src/lib/. This server vendors FT4 v1.1.0r plus its lib.iccf sibling in-process; any OTHER library (lib.icmf, lib.ft3, ...) must be submitted in the files map under lib/. For lib.ft4.* specifically, a module that exists in one FT4 version may not exist in another - check your tagOrBranch pin.",
            nextAction = "Locally: add the library to chromia.yml libs and run `chr install`. Through this server: include the library's sources in the files map (lib/<name>/...), or for lib.ft4.*/lib.iccf just import them - they are vendored. If a lib.ft4 module is 'not found' although FT4 is vendored, suspect an FT4 version mismatch.",
            relatedTools = listOf("rell_check", "check_dapp_project", "chr_library_help", "validate_chromia_yml")
        ),
        rule(
            id = "rell_unknown_module",
            family = "rell-compiler",
            pattern = "unknown module|module ['\"‘“]?[\\w.]+['\"’”]? not found|main module .{0,60}not found",
            meaning = "The compiler cannot resolve a module - an import path or the configured main module points nowhere.",
            likelyCause = "Module paths must mirror the directory layout under the source root (module a.b lives in a/b/), the module name in chromia.yml does not match, or a library (e.g. lib.ft4) is not vendored where expected.",
            nextAction = "Make the import path match the file path from the src root and the chromia.yml module list; check the whole project in one shot with check_dapp_project.",
            relatedTools = listOf("check_dapp_project", "chromia_project_structure_help", "validate_chromia_yml")
        ),
        rule(
            id = "rell_unknown_name",
            family = "rell-compiler",
            pattern = "unknown name",
            meaning = "The compiler found an identifier that is not defined in scope.",
            likelyCause = "A typo, a symbol from a module that was never imported, or use before declaration.",
            nextAction = "Fix the spelling or add the missing import; iterate with rell_check.",
            relatedTools = listOf("rell_check", "chromia_rell_language_help")
        ),
        rule(
            id = "rell_unknown_member",
            family = "rell-compiler",
            pattern = "unknown member",
            meaning = "The type at that position has no attribute/function with that name.",
            likelyCause = "A misspelled attribute, or calling a function that exists on a different type (e.g. list vs map operations).",
            nextAction = "Check the type's actual members in chromia_rell_types_help / chromia_rell_systemlib_help and fix the access; iterate with rell_check.",
            relatedTools = listOf("rell_check", "chromia_rell_types_help", "chromia_rell_systemlib_help")
        ),
        rule(
            id = "rell_type_mismatch",
            family = "rell-compiler",
            pattern = "type mismatch|wrong argument type|incompatible type",
            meaning = "An expression's type does not match what the context requires.",
            likelyCause = "Passing/assigning the wrong type (text vs integer, nullable vs non-null - Rell nullable types need explicit handling with ?:, ?., or require()).",
            nextAction = "Convert or handle the value explicitly at the reported position; chromia_rell_types_help covers conversions and nullability; iterate with rell_check.",
            relatedTools = listOf("rell_check", "chromia_rell_types_help", "chromia_rell_expressions_help")
        ),
        rule(
            id = "rell_unknown_entity",
            family = "rell-compiler",
            pattern = "unknown entity",
            meaning = "An @-expression or create/update references an entity the compiler does not know.",
            likelyCause = "The entity is defined in a module that is not imported, or the name is misspelled.",
            nextAction = "Import the defining module (import determines visibility) or fix the name; see chromia_rell_database_help for entity/at-expression rules.",
            relatedTools = listOf("rell_check", "chromia_rell_database_help")
        ),
        rule(
            id = "rell_mount_conflict",
            family = "rell-compiler",
            pattern = "mount.{0,60}(?:conflict|duplicate|already)|mnt_conflict",
            meaning = "Two definitions resolve to the same mount name, so the chain cannot expose both.",
            likelyCause = "Two operations/queries/entities with the same name in modules mounted at the same point, or clashing @mount annotations (vendored libraries can collide with app code).",
            nextAction = "Rename one definition or give its module/definition a distinct @mount annotation; recompile with rell_check.",
            relatedTools = listOf("rell_check", "chromia_rell_database_help")
        ),
        rule(
            id = "runtime_op_not_found",
            family = "postchain",
            pattern = "(?:operation|query) ['\"‘]?[\\w.]+['\"’]? not found|unknown (?:operation|query)",
            meaning = "The chain rejected the call because no operation/query is mounted under that name.",
            likelyCause = "The call used the definition name instead of the full MOUNT name (module mount + '.' + name), or the dapp version on-chain does not have it yet.",
            nextAction = "Run rell.get_app_structure via chromia_dapp_query to list the real mounted names and call the exact mount path.",
            relatedTools = listOf("chromia_dapp_query")
        ),

        // ---- FT4 ----------------------------------------------------------
        rule(
            id = "ft4_account_not_found",
            family = "ft4",
            pattern = "account (?:not found|does not exist)|no account (?:found|registered|with)",
            meaning = "FT4 has no registered account for that id/signer on this chain.",
            likelyCause = "The account was never registered on THIS blockchain (FT4 accounts are per-chain), or the id is a pubkey where an account id is expected.",
            nextAction = "Register the account via the dapp's registration strategy first; check which chains a signer has accounts on with get_account_blockchains / get_signer_blockchains.",
            relatedTools = listOf("get_account_blockchains", "get_signer_blockchains", "chromia_ft4_queries_help")
        ),
        rule(
            id = "ft4_auth_descriptor",
            family = "ft4",
            pattern = "auth descriptor|mandatory flags|require_mandatory_flags",
            meaning = "FT4 authentication failed at the auth-descriptor level - the descriptor is missing, expired, or lacks required flags.",
            likelyCause = "Signing with a key that has no auth descriptor on the account, a rate-limited/expired descriptor, or flag rules misapplied (require_mandatory_flags belongs on the MAIN descriptor only).",
            nextAction = "Query the account's auth descriptors and use one whose flags satisfy the operation; see chromia_ft4_queries_help for the auth queries.",
            relatedTools = listOf("chromia_ft4_queries_help", "chromia_dapp_query")
        ),
        rule(
            id = "ft4_open_registration",
            family = "ft4",
            pattern = "ras_open|ras_transfer_open",
            meaning = "The open registration strategy (anyone can register/claim) is referenced - a security finding in app code.",
            likelyCause = "App code imports/declares the open strategy, OR a scanner flagged the vendored FT4 library itself: FT4 v1.1.0r legitimately declares operation ras_open inside lib/ft4 (docs/UPSTREAM.md #10), which is a false alarm for code you cannot change.",
            nextAction = "In YOUR modules, remove open registration for production (use a gated strategy); findings pointing INSIDE an unmodified lib/ft4 tree are the library's own declaration - verify with check_ft4_imports, which hash-exempts the pristine vendored copy.",
            relatedTools = listOf("check_ft4_imports", "rell_security_check")
        ),
    )

    data class Translation(
        val matched: Boolean,
        val ruleId: String?,
        val family: String?,
        val meaning: String,
        val likelyCause: String,
        val nextAction: String,
        val relatedTools: List<String>,
        val searchTerms: List<String>,
        val notes: String
    ) {
        fun toJson() = buildJsonObject {
            put("matched", matched)
            if (ruleId != null) put("ruleId", ruleId)
            if (family != null) put("family", family)
            put("meaning", meaning)
            put("likelyCause", likelyCause)
            put("nextAction", nextAction)
            put("relatedTools", buildJsonArray { relatedTools.forEach { add(JsonPrimitive(it)) } })
            put("searchTerms", buildJsonArray { searchTerms.forEach { add(JsonPrimitive(it)) } })
            put("notes", notes)
        }
    }

    private val identifierRegex = Regex("[A-Za-z_][A-Za-z0-9_.]{3,}")

    /**
     * Docs-search seeds for the unmatched fallback: identifier-shaped tokens
     * (dotted/underscored names, long words) from the error text, most
     * distinctive first, capped at 5.
     */
    internal fun searchTerms(error: String): List<String> =
        identifierRegex.findAll(error)
            .map { it.value.trim('.') }
            .filter { it.contains('_') || it.contains('.') || it.length >= 8 }
            .distinct()
            .take(5)
            .toList()

    fun translate(error: String, context: String? = null): Translation {
        val trimmed = error.trim()
        require(trimmed.length <= MAX_ERROR_CHARS) {
            "error is ${trimmed.length} chars - exceeds the $MAX_ERROR_CHARS-char (~8 KB) limit. " +
                "Paste only the relevant part: the first compiler diagnostic, or the final 'Caused by' of a stack trace."
        }
        val ctx = context?.trim()?.take(MAX_CONTEXT_CHARS).orEmpty()
        val haystack = if (ctx.isEmpty()) trimmed else trimmed + "\n" + ctx

        val hit = rules.firstOrNull { it.pattern.containsMatchIn(haystack) }
        if (hit != null) {
            return Translation(
                matched = true,
                ruleId = hit.id,
                family = hit.family,
                meaning = hit.meaning,
                likelyCause = hit.likelyCause,
                nextAction = hit.nextAction,
                relatedTools = hit.relatedTools,
                searchTerms = emptyList(),
                notes = "Matched curated rule '${hit.id}' (family: ${hit.family}). Rules are offline heuristics " +
                    "mined from verified Chromia-stack failures - no LLM, no network. Verify against your setup."
            )
        }
        val terms = searchTerms(haystack)
        return Translation(
            matched = false,
            ruleId = null,
            family = null,
            meaning = "No curated rule matched this error text - this tool does not guess.",
            likelyCause = "An error shape not (yet) in the rule table, or the distinctive part of the message was not included.",
            nextAction = "Triage: (1) if it involves your own Rell code, run rell_check on the sources first; " +
                "(2) call `search` with the extracted terms" +
                (if (terms.isEmpty()) "" else " (${terms.joinToString(", ")})") +
                "; (3) call chromia_help without a topic for the help index; " +
                "(4) retry translate_error with more of the raw error and a `context` note saying what you were doing.",
            relatedTools = listOf("rell_check", "search", "chromia_help"),
            searchTerms = terms,
            notes = "No LLM, no network - matching is against ${rules.size} curated rules. " +
                "An unmatched result means unknown, not impossible."
        )
    }
}
