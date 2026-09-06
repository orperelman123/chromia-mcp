package org.chromia.tools

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.EmptyJsonObject
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject

/**
 * Shims for the two shape changes kotlin-sdk 0.15 made to `tools/call`.
 *
 * The upgrade from 0.7.7 (2026-09-06) moved every protocol type into
 * `io.modelcontextprotocol.kotlin.sdk.types` and, at the same time:
 *
 *  - `CallToolRequest` no longer carries `name`/`arguments` directly - they moved
 *    into a nested [CallToolRequestParams]. [callToolRequest] keeps the flat
 *    shape so hundreds of call sites (and every test) read the same as before.
 *  - `CallToolRequest.arguments` became nullable: a request that omits `params.
 *    arguments` now yields `null` where 0.7.7 substituted an empty object.
 *    [argumentsOrEmpty] restores the old value, so a tool called with no
 *    arguments still reaches its strategy as an empty map rather than an NPE.
 */
fun callToolRequest(name: String, arguments: JsonObject? = null): CallToolRequest =
    CallToolRequest(CallToolRequestParams(name = name, arguments = arguments))

/** [CallToolRequest.arguments], with 0.7.7's empty-object default for an absent value. */
val CallToolRequest.argumentsOrEmpty: JsonObject
    get() = arguments ?: EmptyJsonObject

/**
 * [ToolSchema.properties], with 0.7.7's empty-object default for an absent value.
 *
 * 0.15 replaced `Tool.Input` / `Tool.Output` with one [ToolSchema] whose
 * `properties` is nullable. Every schema this server builds sets it, so this only
 * exists so callers keep reading a plain map instead of threading `!!` through
 * argument validation and thirty schema assertions.
 */
val ToolSchema.propertiesOrEmpty: JsonObject
    get() = properties ?: EmptyJsonObject
