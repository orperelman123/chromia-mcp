# Report for the Chromia Explorer service (explorer.chromia.com)

**Venue uncertain - do not guess.** We found no public repository for the
explorer service in the `chromaway` GitLab group (checked 2026-09-02).
Options, in order of preference:

1. Ask in the chromia-mcp MR (the maintainers own the client of this API and
   can route it internally) - the MR body in `mr-chromia-mcp.md` already
   summarizes these points.
2. Chromia developer support / community channels linked from
   docs.chromia.com (Telegram, Discord).

Both findings are OBSERVED live behavior (re-verified 2026-09-02); we cannot
fix them client-side.

---

## Title

Explorer API: testnet returns 400 for valid queries; getNodeUnavailability requires reCAPTCHA (breaks all API clients)

## Body (ready to paste)

Two behaviors of `https://explorer.chromia.com/api/explorer-service` break
programmatic clients, including Chromaway's own `chromia-mcp` server. Both
reproduced on 2026-09-02.

**1. `network=testnet` returns HTTP 400 for queries that succeed on mainnet.**

Same request body, only the query parameter differs:

    curl -s -o /dev/null -w '%{http_code}\n' -X POST \
      'https://explorer.chromia.com/api/explorer-service?network=mainnet' \
      -H 'Content-Type: application/json' -d '{"query":"query { totalRewardsPaid }"}'
    # -> 200

    curl -s -w '\n%{http_code}\n' -X POST \
      'https://explorer.chromia.com/api/explorer-service?network=testnet' \
      -H 'Content-Type: application/json' -d '{"query":"query { totalRewardsPaid }"}'
    # -> {"message":{}}  400

If testnet analytics are intentionally discontinued, an explicit error
message (or documentation) would let clients degrade gracefully instead of
surfacing an opaque 400; chromia-mcp currently advertises `'testnet'` as a
valid `network` argument on all analytics tools.

**2. `getNodeUnavailability` requires a reCAPTCHA token.**

    curl -s -X POST 'https://explorer.chromia.com/api/explorer-service?network=mainnet' \
      -H 'Content-Type: application/json' \
      -d '{"query":"query getNodeUnavailability($pubkey: String!, $startTimestamp: String!) { getNodeUnavailability(pubkey: $pubkey, startTimestamp: $startTimestamp) { blockchainRid, intervals { start, end } } }","variables":{"pubkey":"03e5c53e1f3a11d939ffab54f1d883e0d9b47e2ba1bbd52a55d61e4bd2e109e79b","startTimestamp":"1735689600000"}}'

    # -> 200 with:
    # "reCAPTCHA verification failed: token is required.
    #  Add X-reCAPTCHA-Token header with a valid token."

Every non-browser client of this query is broken (chromia-mcp's
`get_node_unavailability` tool among them). Is there - or could there be - a
non-browser API path for this data?

**3. Minor schema-consistency note.** `getAssetTopHolders` now takes
`excludedAccounts` while `getAssetDistribution` still takes
`excludeAccounts` (live introspection). The rename broke API clients built
against the older name; aligning the two (with a deprecation alias) would
avoid the next round of breakage. Likewise, top-level
`groupedTransactionsByCluster` was removed (now only under `dashboardData`),
which silently broke existing consumers.
