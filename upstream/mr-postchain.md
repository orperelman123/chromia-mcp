# Issue for gitlab.com/chromaway/core/postchain

This is an issue (a design/documentation suggestion), not an MR - the
behavior is documented, so changing it is a maintainer decision. If issues
are not open to external users on this project, the alternative is to
mention it in the chromia-mcp MR (where the consumer-side fix lands) and let
the team route it.

---

## Title

make_gtv_gson() throws on big_integer - the default-named builder breaks on a core Rell type

## Body (ready to paste)

`postchain-gtv/src/main/kotlin/net/postchain/gtv/gtvjson.kt` defines three
Gson builders. The one with the obvious default name, `make_gtv_gson()`,
registers `GtvAdapter(strict = true, supportBigInteger = false)`, whose
BIGINTEGER branch throws:

    GtvType.BIGINTEGER -> ... throw IllegalStateException("big_integer cannot be serialized as JSON")

`big_integer` is a first-class Rell type and is what FT4 uses for balances,
supplies and amounts - i.e. it appears in essentially every real dapp query
response. Any client that picks the default-named builder to serialize query
results therefore fails on the most common data in the ecosystem, and the
failure surfaces far from its cause: the query looks like it failed even
though the chain answered successfully.

This is documented ("Does not support BigInteger" in the KDoc), so we are
not claiming a bug - but it is a trap in practice. Chromaway's own official
`chromia-mcp` server shipped with `make_gtv_gson()` in
`PostchainClientService.kt` and its `chromia_dapp_query` tool failed on every
FT4 balance/supply query until we switched it to `makeStrictGtvGson()`
(see the chromia-mcp MR; downstream fix commit `a54408d` in
github.com/orperelman123/chromia-mcp). We verified in postchain-gtv 3.49.18
that the serializers of `make_gtv_gson()` and `makeStrictGtvGson()` differ
only in the BIGINTEGER branch.

Suggestions, in decreasing order of impact:

1. Make the default builder strict (serialize big_integer as a JSON string),
   or
2. Deprecate `make_gtv_gson()` in favor of explicitly-named variants so the
   choice is conscious, or
3. At minimum, add a prominent KDoc warning that FT4/typical dapp responses
   will throw with this builder, pointing at `makeStrictGtvGson()`.

Verified against `dev` on 2026-09-02.
