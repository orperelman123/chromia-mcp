// Shared upstream-failure classifier for the live-network harnesses
// (e2e-sweep.mjs, synthetic-agent.mjs). stress-soak.mjs never imports it:
// every stress/soak task is local-only against our own server, so there a
// failure is always ours.
//
// CONTRACT - when may a harness downgrade a failing check to WARN-UPSTREAM
// instead of FAIL?
//   1. OUR server answered the MCP call cleanly: transport intact, JSON-RPC
//      intact, a well-formed tool result carrying an error TEXT (isError) -
//      the server behaved correctly by surfacing a clean error; AND
//   2. the check is a LIVE-NETWORK check (it exercises a third-party
//      dependency: the explorer backend, mainnet/testnet chain nodes, the
//      docs site); AND
//   3. the error text matches one of the allowlisted signatures below, all of
//      which describe failures of the third party or of the hop TO it.
// Callers enforce (1) and (2) structurally - they classify only clean
// tool-level errors, and only on checks explicitly declared live. This module
// owns only (3): the signature allowlist. Non-network checks (rell_check,
// security, local chain, preflight gates, onboarding, translate_error,
// scaffold, validators) must never consult this module.
//
// Everything else stays a hard FAIL: our own 5xx or crash, protocol/transport
// errors on OUR SSE connection, malformed or wrong-shaped responses, a wrong
// tool answering, assertion mismatches about OUR behavior, a tool that hangs
// (client-side rpc timeout), a missing/undeclared tool.
//
// Signatures are anchored to the exact clean-error formats our server emits
// (see app/src/main/kotlin/org/chromia/domain/exceptions/ChromiaExceptions.kt
// and the postchain-client/ktor messages it wraps) - not free-form substrings.
export const UPSTREAM_SIGNATURES = [
  // GraphQLException: "GraphQL Error: INTERNAL_ERROR for <uuid>" - the
  // explorer's backend failing internally (live incident 2026-09-01/02
  // observed across get_asset_top_holders, get_all_transactions,
  // filter_blockchains, get_blockchain_details and others at once).
  { name: 'explorer-graphql-internal-error', re: /GraphQL Error:.*\bINTERNAL_ERROR\b/i },
  // HttpRequestException("HTTP 5xx: ...") / raw gateway texts: the third
  // party's server-side failure, surfaced cleanly by our HTTP client.
  { name: 'upstream-http-5xx', re: /\bHTTP 50[0-9]\b|\bBad Gateway\b|\bService Unavailable\b|\bGateway Time-?out\b/i },
  // HTTP 429: the third party is rate-limiting us.
  { name: 'upstream-http-429', re: /\bHTTP 429\b|\bToo Many Requests\b/i },
  // Upstream timeouts surfaced cleanly: the explorer's own "Request timeout
  // has expired", ktor's connect-timeout text, JVM socket read/connect
  // timeouts on the server's OUTBOUND hop to the third party.
  { name: 'upstream-timeout', re: /Request timeout has expired|Connect timeout has expired|connect(?:ion)? timed out|Read timed out|SocketTimeoutException/i },
  // DNS/socket failures reaching the third party: JVM (UnknownHostException,
  // Connection refused/reset...) and Node ("fetch failed", ECONNRESET...)
  // spellings - the latter for any direct third-party fetch a harness makes.
  { name: 'upstream-unreachable', re: /UnknownHostException|Unknown host|UnresolvedAddress|Connection refused|Connection reset|No route to host|failed to connect|fetch failed|ECONNRESET|ECONNREFUSED|ETIMEDOUT|EAI_AGAIN|ENOTFOUND/i },
];

/** The matched signature name for a clean tool-error text, or null (= ours). */
export function upstreamSignature(text) {
  const t = String(text ?? '');
  for (const { name, re } of UPSTREAM_SIGNATURES) if (re.test(t)) return name;
  return null;
}

/**
 * Thrown by a LIVE check (or the liveText helper) when a failure is
 * demonstrably the upstream dependency's. The harness turns it into a
 * WARN-UPSTREAM tag instead of FAIL - bounded by the harness's guardrails.
 */
export class UpstreamError extends Error {
  constructor(signature, detail) {
    super(`${signature}: ${String(detail ?? '').slice(0, 200)}`);
    this.signature = signature;
  }
}
