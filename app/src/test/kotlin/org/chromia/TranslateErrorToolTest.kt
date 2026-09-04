package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.ErrorTranslator
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * translate_error: curated offline rule table over real Chromia-stack error
 * shapes. One test per rule (each uses realistic error text and asserts the
 * rule id, so ordering regressions - a broad rule shadowing a specific one -
 * fail loudly), plus the unmatched fallback, the ~8 KB input cap, context
 * matching, and registration/schema parity.
 */
class TranslateErrorToolTest {

    // ---- unit-level helpers ------------------------------------------------

    private fun assertRule(expectedRuleId: String, error: String, context: String? = null) {
        val t = ErrorTranslator.translate(error, context)
        assertTrue(t.matched, "expected a match for: $error")
        assertEquals(expectedRuleId, t.ruleId, "wrong rule for: $error")
        assertTrue(t.meaning.isNotBlank())
        assertTrue(t.likelyCause.isNotBlank())
        assertTrue(t.nextAction.isNotBlank())
    }

    // ---- family: this server's own messages --------------------------------

    @Test
    fun ownSwallowedNullError() =
        assertRule("own_tool_failed_null", "Tool execution failed: null")

    @Test
    fun ownUnknownTool() =
        assertRule("own_unknown_tool", "Unknown tool: get_dashboard_data")

    @Test
    fun ownUnknownToolSdkShapedText() =
        // The gated session (App.callToolGated) answers a bogus tool name with
        // the SDK-shaped "Tool X not found" - the text a LIVE server actually
        // returns. It went unmatched until the e2e sweep fed it back through
        // translate_error (found 2026-09-02); this pins the fix.
        assertRule("own_unknown_tool", "Tool definitely_no_such_tool not found")

    @Test
    fun ownMissingParameter() =
        assertRule(
            "own_missing_parameter",
            "Tool execution failed: Missing required parameter: blockchainRid"
        )

    @Test
    fun ownDocumentationNotFound() =
        assertRule("own_doc_not_found", "Documentation not found for id: rell-compiler.md#3")

    @Test
    fun ownPaginationLimit() =
        assertRule(
            "own_pagination_limit",
            "Tool execution failed: limit must not exceed 1000 (got 5000)"
        )

    @Test
    fun ownSourceTooLarge() =
        assertRule(
            "own_source_too_large",
            "Total Rell source size (3145728 chars across 12 file(s)) exceeds the 2097152-char (~2 MB) limit - submit a smaller project."
        )

    // ---- family: explorer / GraphQL ----------------------------------------

    @Test
    fun explorerRecaptcha() =
        assertRule(
            "explorer_recaptcha",
            "GraphQL Error: reCAPTCHA verification failed: token is required"
        )

    @Test
    fun explorerTestnet400() =
        assertRule(
            "explorer_testnet_400",
            "Failed to get network stats: explorer returned HTTP 400 for network=testnet"
        )

    @Test
    fun explorerTestnet400ViaContext() =
        // the 400 alone is opaque; the context supplies the testnet clue
        assertRule(
            "explorer_testnet_400",
            "HTTP error: 400 Bad Request",
            context = "calling get_network_stats with network=testnet"
        )

    @Test
    fun graphqlUnknownArgument() =
        assertRule(
            "graphql_unknown_argument",
            "Validation error (UnknownArgument@[getAssetTopHolders]) : Unknown field argument excludeAccounts"
        )

    @Test
    fun graphqlFieldUndefined() =
        assertRule(
            "graphql_field_undefined",
            "Validation error (FieldUndefined@[groupedTransactionsByCluster]) : Field 'groupedTransactionsByCluster' in type 'Query' is undefined"
        )

    @Test
    fun graphqlWrongTypeVariable() =
        assertRule(
            "graphql_wrong_type_variable",
            "Validation error (WrongType@[getAllTransactions]) : argument 'accountTypes' with value 'StringValue{value='[FT4_USER]'}' has an invalid value"
        )

    @Test
    fun graphqlInternalErrorWithRequestId() {
        // Real incident text (live explorer outage 2026-09-02): several
        // explorer-backed tools failed with exactly this shape at once while
        // non-explorer tools kept working - the translation must say upstream
        // incident, not bad arguments, and point at retry / chromia_dapp_query.
        val t = ErrorTranslator.translate(
            "GraphQL Error: INTERNAL_ERROR for 6f6a1b2c-3d4e-4f50-8182-93a4b5c6d7e8"
        )
        assertTrue(t.matched)
        assertEquals("graphql_internal_error", t.ruleId)
        assertTrue(t.meaning.contains("upstream incident"), t.meaning)
        assertTrue(t.meaning.contains("not a fault in your call"), t.meaning)
        assertTrue(t.nextAction.contains("retry later"), t.nextAction)
        assertTrue(t.nextAction.contains("chromia_dapp_query"), t.nextAction)
        assertTrue("chromia_dapp_query" in t.relatedTools)
    }

    @Test
    fun graphqlInternalErrorWithoutRequestId() =
        assertRule("graphql_internal_error", "GraphQL Error: INTERNAL_ERROR")

    @Test
    fun httpRateLimited() =
        assertRule("http_rate_limited", "Request failed: HTTP 429 Too Many Requests")

    @Test
    fun httpUnavailable() =
        assertRule("http_unavailable", "Failed to get network stats: explorer HTTP 503")

    // ---- family: GTV / serialization ---------------------------------------

    @Test
    fun gtvBigIntegerJson() =
        assertRule(
            "gtv_big_integer_json",
            "java.lang.IllegalArgumentException: big_integer cannot be serialized as JSON"
        )

    @Test
    fun gtvDouble() =
        assertRule("gtv_double", "Cannot convert object of type Double to GTV")

    @Test
    fun gtvByteArrayFromWrappedHexLiteral() =
        // Adversary round 4: the scaffold's yml writes admin_pubkey: x"02C4...",
        // the agent pasted it into run_rell_tests moduleArgs, every case failed
        // with this, and translate_error answered matched:false.
        assertRule(
            "gtv_bytearray_from_string",
            "net.postchain.common.exception.UserMistake: Can't create ByteArray from string 'x\"02C4049F9550DCFF6003347BB3944DF2AA2D6EF5202C22834284B085C56DE8C6DD\"'",
            context = "calling run_rell_tests with moduleArgs from chromia.yml"
        )

    @Test
    fun gtxModuleCreationWithoutModuleArgs() {
        // DX audit 2026-09-04 (T15): the most common stall on the test path
        // answered matched:false although run_rell_tests' own notes explain it.
        assertRule("gtx_module_missing_module_args", "Unable to create GTX module")
        assertRule(
            "gtx_module_missing_module_args",
            "Bad module_args for module 'lib.ft4': Wrong key in Gtv dictionary for type 'lib.ft4:module_args': 'rate_limit'"
        )
        val t = ErrorTranslator.translate("Unable to create GTX module", null)
        assertTrue(t.nextAction.contains("keyed by module name"), t.nextAction)
        assertTrue("run_rell_tests" in t.relatedTools && "scaffold_dapp" in t.relatedTools, t.relatedTools.toString())
    }

    // ---- family: postchain runtime / postgres ------------------------------

    @Test
    fun postchainMissingMetadata() =
        assertRule(
            "postchain_missing_metadata",
            "org.postchain.base.data.DatabaseAccess: Missing metadata entities for existing tables: c0.accounts, c0.assets"
        )

    @Test
    fun postgresCollation() =
        assertRule(
            "postgres_collation",
            "Database collation check failed: please initialize Postgres with LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8'"
        )

    @Test
    fun postgresConnectionRefused() =
        assertRule(
            "postgres_conn_refused",
            "org.postgresql.util.PSQLException: Connection to localhost:5432 refused. Check that the hostname and port are correct."
        )

    @Test
    fun postgresAuthFailed() =
        assertRule(
            "postgres_auth_failed",
            "org.postgresql.util.PSQLException: FATAL: password authentication failed for user \"postchain\""
        )

    @Test
    fun postgresMissingRelation() =
        assertRule(
            "postgres_missing_relation",
            "ERROR: relation \"blockchains\" does not exist"
        )

    @Test
    fun jvmOutOfMemory() =
        assertRule("jvm_oom", "java.lang.OutOfMemoryError: Java heap space")

    @Test
    fun gzipDoubleDecode() =
        assertRule("http_gzip", "java.util.zip.ZipException: Not in GZIP format")

    @Test
    fun networkTimeout() =
        assertRule("net_timeout", "java.net.SocketTimeoutException: connect timed out")

    // ---- family: MCP transport ---------------------------------------------

    @Test
    fun stdioCorruptJson() =
        assertRule(
            "stdio_corrupt_json",
            "SyntaxError: Unexpected token 'T', \"[TRACE] (re\"... is not valid JSON"
        )

    // ---- family: chr CLI / chromia.yml -------------------------------------

    @Test
    fun chrUnknownVersion() =
        assertRule("chr_unknown_version", "chr build failed: Unknown version: 0.16.4")

    @Test
    fun chrNoProject() =
        assertRule("chr_no_project", "Project settings file not found")

    @Test
    fun chrNotInstalled() =
        assertRule(
            "chr_not_installed",
            "'chr' is not recognized as an internal or external command, operable program or batch file."
        )

    @Test
    fun chrNotInstalledUnixVariant() =
        assertRule("chr_not_installed", "bash: chr: command not found")

    // ---- family: Rell compiler ---------------------------------------------

    @Test
    fun rellSyntaxError() =
        assertRule("rell_syntax", "main.rell(3:14) syntax error")

    @Test
    fun rellUnknownModule() =
        assertRule("rell_unknown_module", "main.rell(1:8) unknown module: 'foo.bar'")

    @Test
    fun rellModuleNotFoundVariant() =
        assertRule("rell_unknown_module", "Module 'my_dapp' not found in source directory")

    @Test
    fun rellUnknownName() =
        assertRule("rell_unknown_name", "main.rell(5:9) unknown name: 'balanse'")

    @Test
    fun rellUnknownMember() =
        assertRule(
            "rell_unknown_member",
            "main.rell(7:22) unknown member: 'ammount' (type 'asset')"
        )

    @Test
    fun rellTypeMismatch() =
        assertRule(
            "rell_type_mismatch",
            "main.rell(4:18) type mismatch: cannot assign 'text' to 'integer'"
        )

    @Test
    fun rellUnknownEntity() =
        assertRule("rell_unknown_entity", "main.rell(9:30) unknown entity: 'user_account'")

    @Test
    fun rellMountConflict() =
        assertRule(
            "rell_mount_conflict",
            "main.rell(2:1) mount conflict: operation 'transfer' is already mounted at 'foo.transfer'"
        )

    @Test
    fun runtimeOperationNotFound() =
        assertRule("runtime_op_not_found", "operation 'accounts.register' not found")

    // ---- family: FT4 --------------------------------------------------------

    @Test
    fun ft4AccountNotFound() =
        assertRule("ft4_account_not_found", "Account not found: 02badbeefcafe")

    @Test
    fun ft4AuthDescriptor() =
        assertRule(
            "ft4_auth_descriptor",
            "Auth descriptor not found for signer 02aabbcc on account"
        )

    @Test
    fun ft4OpenRegistration() =
        assertRule(
            "ft4_open_registration",
            "CRITICAL: open registration strategy ras_open declared at lib/ft4/registration.rell:12"
        )

    // ---- ordering guards ----------------------------------------------------

    @Test
    fun moduleNamedAccountHitsModuleRuleNotFt4AccountRule() {
        // "module 'account' not found" must be a compiler-module diagnosis,
        // not an FT4 account-registration one.
        val t = ErrorTranslator.translate("main.rell(1:8) module 'account' not found")
        assertEquals("rell_unknown_module", t.ruleId)
    }

    @Test
    fun serverOwnMessagesWinOverBroadRules() {
        // Contains "not found" too, but the server's own doc message is more specific.
        val t = ErrorTranslator.translate("Documentation not found for id: module_x.md")
        assertEquals("own_doc_not_found", t.ruleId)
    }

    // ---- unmatched fallback -------------------------------------------------

    @Test
    fun unmatchedErrorReturnsTriageNotGuesses() {
        val t = ErrorTranslator.translate("flurble gronk failed spectacularly in zorp_module.quux")
        assertFalse(t.matched)
        assertEquals(null, t.ruleId)
        assertTrue(t.searchTerms.contains("zorp_module.quux"), t.searchTerms.toString())
        assertTrue(t.nextAction.contains("rell_check"))
        assertTrue(t.nextAction.contains("search"))
        assertTrue(t.relatedTools.containsAll(listOf("rell_check", "search", "chromia_help")))
        assertTrue(t.notes.contains("unknown"))
    }

    @Test
    fun searchTermsExtractIdentifierShapedTokens() {
        val terms = ErrorTranslator.searchTerms(
            "boom at my_module.thing due to WeirdException in short ok"
        )
        assertTrue(terms.contains("my_module.thing"), terms.toString())
        assertTrue(terms.contains("WeirdException"), terms.toString())
        assertFalse(terms.contains("short"))
        assertTrue(terms.size <= 5)
    }

    // ---- input cap ----------------------------------------------------------

    @Test
    fun errorOverCapIsRejectedWithHelpfulMessage() {
        val big = "x".repeat(ErrorTranslator.MAX_ERROR_CHARS + 1)
        val ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            ErrorTranslator.translate(big)
        }
        assertTrue(ex.message!!.contains(ErrorTranslator.MAX_ERROR_CHARS.toString()))
    }

    @Test
    fun errorAtCapIsAccepted() {
        val atCap = "y".repeat(ErrorTranslator.MAX_ERROR_CHARS)
        val t = ErrorTranslator.translate(atCap)
        assertFalse(t.matched)
    }

    @Test
    fun oversizeContextIsTruncatedNotFatal() {
        val t = ErrorTranslator.translate(
            "HTTP error: 400 Bad Request",
            context = "z".repeat(ErrorTranslator.MAX_CONTEXT_CHARS + 500) + " testnet"
        )
        // the testnet clue sits beyond the context cap, so it must NOT match testnet rule
        assertFalse(t.matched)
    }

    // ---- rule table hygiene -------------------------------------------------

    @Test
    fun ruleTableHasAtLeast25RulesWithUniqueIdsAndCompleteFields() {
        val rules = ErrorTranslator.rules
        assertTrue(rules.size >= 25, "expected >= 25 curated rules, got ${rules.size}")
        assertEquals(rules.size, rules.map { it.id }.toSet().size, "duplicate rule ids")
        rules.forEach { r ->
            assertTrue(r.meaning.isNotBlank(), "${r.id} meaning blank")
            assertTrue(r.likelyCause.isNotBlank(), "${r.id} likelyCause blank")
            assertTrue(r.nextAction.isNotBlank(), "${r.id} nextAction blank")
            assertTrue(r.family.isNotBlank(), "${r.id} family blank")
        }
    }

    @Test
    fun relatedToolsOnlyReferenceRegisteredToolsOrHelpTopics() {
        val registered = McpTools.allTools().map { it.name }.toSet()
        ErrorTranslator.rules.flatMap { it.relatedTools }.forEach { tool ->
            assertTrue(tool in registered, "rule references unknown tool: $tool")
        }
    }

    // ---- MCP wiring: strategy + schema + compact mode ------------------------

    private fun executor(): ToolExecutor =
        ToolExecutor(
            RecordingRepository(),
            PromptManager(),
            CompletableDeferred(RagStore(loadFromRegistry = false))
        )

    @Test
    fun executeToolReturnsStructuredTranslation() = runBlocking {
        val result = executor().executeTool(
            CallToolRequest(
                name = "translate_error",
                arguments = buildJsonObject {
                    put("error", "java.lang.IllegalArgumentException: big_integer cannot be serialized as JSON")
                    put("context", "calling chromia_dapp_query for an FT4 balance")
                }
            )
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        assertTrue(structured["matched"]!!.jsonPrimitive.boolean)
        assertEquals("gtv_big_integer_json", structured["ruleId"]!!.jsonPrimitive.content)
        assertEquals("gtv", structured["family"]!!.jsonPrimitive.content)
        assertTrue(structured["nextAction"]!!.jsonPrimitive.content.contains("makeStrictGtvGson"))
        assertTrue(
            structured["relatedTools"]!!.jsonArray.any {
                it.jsonPrimitive.content == "chromia_dapp_query"
            }
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject)
    }

    @Test
    fun executeToolMissingErrorParameterIsError() = runBlocking {
        val result = executor().executeTool(
            CallToolRequest(name = "translate_error", arguments = buildJsonObject { })
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Missing required parameter: error"))
    }

    @Test
    fun executeToolOverCapIsErrorMentioningLimit() = runBlocking {
        val result = executor().executeTool(
            CallToolRequest(
                name = "translate_error",
                arguments = buildJsonObject { put("error", "q".repeat(9000)) }
            )
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("8192"), text)
    }

    @Test
    fun translateErrorAdvertisedInFullAndCompactMode() {
        val full = McpTools.allTools(compact = false).map { it.name }
        val compact = McpTools.allTools(compact = true).map { it.name }
        assertTrue("translate_error" in full)
        assertTrue("translate_error" in compact, "translate_error is cheap+high-value: compact mode must keep it")
    }

    @Test
    fun toolSchemaDeclaresErrorRequiredAndOutputShape() {
        val tool = McpTools.translateErrorTool()
        assertEquals("translate_error", tool.name)
        assertEquals(listOf("error"), tool.inputSchema.required)
        assertNotNull(tool.inputSchema.properties["error"])
        assertNotNull(tool.inputSchema.properties["context"])
        val out = tool.outputSchema!!
        listOf("matched", "meaning", "likelyCause", "nextAction", "relatedTools", "searchTerms", "notes")
            .forEach { assertNotNull(out.properties[it], "outputSchema missing $it") }
        assertTrue(out.required!!.containsAll(listOf("matched", "meaning", "likelyCause", "nextAction")))
    }
}
