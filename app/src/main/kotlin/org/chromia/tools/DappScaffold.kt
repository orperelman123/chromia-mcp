package org.chromia.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Production-correct new-dapp skeleton. Pins match AGENTS.md / official source:
 * Rell source tag 0.16.7, chromia.yml rellVersion 0.16.1, merkle_hash_version 2,
 * FT4 v1.1.0r API 1.
 * Never ships lib.ft4.admin, admin.crosschain, ras_open, or ras_transfer_open.
 * Does not send signed transactions.
 */
object DappScaffold {

    /**
     * The FT4 module_args the ft4 template writes into chromia.yml - the
     * production `moduleArgs` block plus the test-scoped `test.moduleArgs`
     * block - in the shape run_rell_tests takes. FT4 will not initialize
     * without them: the tool accepts module_args as a PARAMETER and does not
     * read the generated yml, so running the shipped tests without these fails
     * every case with an opaque "Unable to create GTX module". Kept beside the
     * yml it mirrors so the two cannot drift.
     */
    fun ft4TestModuleArgs(): Map<String, Map<String, kotlinx.serialization.json.JsonElement>> = mapOf(
        "lib.ft4" to mapOf("query_max_page_size" to JsonPrimitive(100)),
        "lib.ft4.core.accounts" to mapOf(
            "rate_limit" to buildJsonObject {
                put("active", JsonPrimitive(true))
                put("max_points", JsonPrimitive(10))
                put("recovery_time", JsonPrimitive(5000))
                put("points_at_account_creation", JsonPrimitive(1))
            },
            "auth_descriptor" to buildJsonObject {
                put("max_rules", JsonPrimitive(8))
                put("max_number_per_account", JsonPrimitive(10))
            },
            "auth_flags" to buildJsonObject {
                put("mandatory", buildJsonArray { add(JsonPrimitive("A")); add(JsonPrimitive("T")) })
            }
        ),
        // TEST-ONLY admin wiring. lib.ft4.test.core (register_alice & co) transitively
        // imports lib.ft4.admin, whose core.admin module_args has no default - the
        // block runner refuses to build the chain without an admin_pubkey ("No
        // moduleArgs for module 'lib.ft4.core.admin'"). These are the WELL-KNOWN
        // FT4 repo test keys (public in ft4-lib's own chromia.yml) - they gate
        // nothing in production because the scaffold's main module never imports
        // admin, so no admin operation is mounted on a deployed chain.
        "lib.ft4.core.admin" to mapOf(
            "admin_pubkey" to JsonPrimitive(TEST_ADMIN_PUBKEY)
        ),
        "lib.ft4.test.core.auth" to mapOf(
            "admin_priv_key" to JsonPrimitive(TEST_ADMIN_PRIVKEY)
        )
    )

    /**
     * FT4's own published test admin keypair (ft4-lib chromia.yml, tag v1.1.0r).
     * Test configuration only - never a production credential.
     */
    const val TEST_ADMIN_PUBKEY = "02C4049F9550DCFF6003347BB3944DF2AA2D6EF5202C22834284B085C56DE8C6DD"
    const val TEST_ADMIN_PRIVKEY = "00CED79962D1150BF844CACB76310D4746C4426558A7FD9C827B30203DACC4CE"
    // Git tag / source revision of the Rell language sources and docs we reference.
    // NOT what goes into generated chromia.yml.
    const val RELL_SOURCE_TAG = "0.16.7"
    // Value written into generated chromia.yml `compile.rellVersion`.
    // Chromia CLI 0.33.x bundles Rell 0.16.1, whose SUPPORTED_VERSIONS list stops at
    // 0.16.1 - a project pinned to anything newer (e.g. the 0.16.7 source tag) fails
    // `chr build` with an "Unknown version" error. Keep this at the newest version
    // the installed CLI actually accepts.
    const val RELL_VERSION = "0.16.1"
    const val FT4_VERSION = "v1.1.0r"
    const val FT4_API = "1"
    const val MERKLE_HASH_VERSION = 2
    const val CLI_SERIES = "0.33.x"
    const val DEFAULT_NAME = "hello"
    const val FT4_REGISTRY = "https://gitlab.com/chromaway/ft4-lib.git"
    const val FT4_PATH = "rell/src/lib/ft4"
    const val FT4_RID = "x\"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E\""

    val forbiddenModules = listOf(
        "lib.ft4.admin",
        "lib.ft4.core.admin",
        "admin.crosschain",
        "ras_open",
        "ras_transfer_open",
        "lib.ft4.core.accounts.strategies.open",
        "lib.ft4.accounts.strategies.open"
    )

    private val namePattern = Regex("^[a-z][a-z0-9_]{0,31}$")

    fun normalizeName(raw: String?): String {
        val trimmed = raw?.trim()?.lowercase().orEmpty()
        return if (namePattern.matches(trimmed)) trimmed else DEFAULT_NAME
    }

    /**
     * Like [normalizeName] for absent names (default), but a PRESENT invalid
     * name is a validation error instead of a silent '$DEFAULT_NAME' fallback.
     * write_deployment_config must never key a deployments block under a name
     * the caller did not ask for (audit round 4 minor); scaffold_dapp keeps the
     * warning+fallback behavior in [toJson].
     */
    fun requireValidName(raw: String?): String {
        val requested = raw?.trim().orEmpty()
        if (requested.isEmpty()) return DEFAULT_NAME
        val normalized = requested.lowercase()
        require(namePattern.matches(normalized)) {
            "'$requested' is not a valid chain name: it must start with a lowercase letter and " +
                "contain only lowercase letters, digits, or underscores, at most 32 characters " +
                "([a-z][a-z0-9_]{0,31})."
        }
        return normalized
    }

    /**
     * Minimal chromia.yml at the current pins, used by check_dapp_project when
     * the caller omits `yaml` - same content the hello scaffold ships.
     */
    fun defaultChromiaYml(): String = chromiaYml(DEFAULT_NAME)

    /** Every template scaffold_dapp accepts; anything else falls back to hello with a warning. */
    val templates = listOf("hello", "ft4", "governance", "vault", "staking", "marketplace", "lending")

    fun files(name: String, template: String = "hello"): Map<String, String> {
        val chain = normalizeName(name)
        return when (template) {
            "ft4" -> linkedMapOf(
                "chromia.yml" to ft4ChromiaYml(chain),
                "src/main.rell" to ft4MainRell(),
                "src/test/main_test.rell" to ft4TestRell(),
                "client/example.ts" to ft4ClientTs(chain)
            )
            "governance" -> linkedMapOf(
                "chromia.yml" to ft4ChromiaYml(chain),
                "src/main.rell" to governanceMainRell(),
                "src/test/main_test.rell" to governanceTestRell()
            )
            "vault" -> linkedMapOf(
                "chromia.yml" to vaultChromiaYml(chain),
                "src/main.rell" to vaultMainRell(),
                "src/test/main_test.rell" to vaultTestRell()
            )
            "staking" -> linkedMapOf(
                "chromia.yml" to ft4ChromiaYml(chain),
                "src/main.rell" to stakingMainRell(),
                "src/test/main_test.rell" to stakingTestRell()
            )
            "marketplace" -> linkedMapOf(
                "chromia.yml" to ft4ChromiaYml(chain),
                "src/main.rell" to marketplaceMainRell(),
                "src/test/main_test.rell" to marketplaceTestRell()
            )
            "lending" -> linkedMapOf(
                "chromia.yml" to lendingChromiaYml(chain),
                "src/main.rell" to lendingMainRell(),
                "src/test/main_test.rell" to lendingTestRell()
            )
            else -> linkedMapOf(
                "chromia.yml" to chromiaYml(chain),
                "src/main.rell" to mainRell(),
                "src/test/main_test.rell" to mainTestRell()
            )
        }
    }

    fun notes(name: String): String {
        val chain = normalizeName(name)
        return """
            New Chromia dapp skeleton for `$chain`.
            Chromia CLI $CLI_SERIES (Java 21+, Postgres 16+): `chr install` then `chr build` then `chr test`.
            Official first-run (run-dapp-cli, query-only): `chr create-rell-dapp` → `cd my-rell-dapp` →
            `chr node start` → `chr query hello_world` → `"Hello World!"`.
            There is no top-level `chr compile` in 0.33.x; use `chr build` or `chr code check`.
            merkle_hash_version must stay 2. Do not ship merkle_hash_version 1.
            compile.rellVersion pin $RELL_VERSION — the newest Rell the CLI $CLI_SERIES bundle accepts
            (SUPPORTED_VERSIONS stops at $RELL_VERSION; a newer pin fails `chr build` with "Unknown version").
            Rell language source tag $RELL_SOURCE_TAG (docs may still say 0.16.4 — source wins for language docs).
            FT4 pin $FT4_VERSION API $FT4_API. Add FT4 by importing lib.ft4.accounts / lib.ft4.assets after reading fetch_docs; configure module_args from official FT4 setup.
            The ft4 template ships runnable INVARIANT tests (conservation, no-negative-balance,
            non-owner-must-fail) in src/test/main_test.rell - run them with run_rell_tests (they
            register FT4 test accounts, so the server needs CHROMIA_TEST_DATABASE_URL, and you must
            pass module_args = chromia.yml's moduleArgs PLUS its test.moduleArgs block - FT4's test
            helpers need the test-only lib.ft4.core.admin/lib.ft4.test.core.auth keys) or `chr test`
            (green as scaffolded; needs `chr install` and a C.UTF-8 PostgreSQL in `database:`),
            and copy test_transfer_conserves_total_points for your own app's economic invariant.
            HOW TO PASS module_args to run_rell_tests: one JSON object keyed by module name that
            merges blockchains.<name>.moduleArgs with test.moduleArgs, e.g.
            {"lib.ft4": {...}, "lib.ft4.core.accounts": {...}, "lib.ft4.core.admin": {"admin_pubkey": "x\"02C4...\""},
            "lib.ft4.test.core.auth": {"admin_priv_key": "x\"00CE...\""}} - byte_array values may be
            pasted as the yml's x"..." literal, as 0x..., or as bare hex; all three decode to bytes.
            test.moduleArgs admin keys are FT4's published TEST keys, scoped to `chr test` only -
            never move them under blockchains.<name> and never import admin modules in code.
            A passing security check is NOT economic soundness: missing authorization, unbacked
            minting, missing quorum/timeouts, and value with no withdrawal path all pass static
            analysis - only an invariant test you write catches them.
            Building a DAO / treasury: start from template=governance (quorum, a fixed voting
            window, stake-weighted votes and execute-once are structural, and the shipped tests
            replay the single-account drain and require it to fail). Building an exchange, vault
            or anything priced by an oracle: start from template=vault (every credit is paid out
            of a reserve row in the same operation, price moves are bounded and rate-limited,
            stale prices halt trading; the shipped tests replay the 100 -> 200,000,000 mint and
            require it to fail). The vault's oracle key is a module arg: its tests need
            main.oracle_pubkey from chromia.yml test.moduleArgs in the module_args you pass to
            run_rell_tests, and you must set main.oracle_pubkey under blockchains.<name>.moduleArgs
            before `chr build` - it is deliberately absent so no placeholder key can ship.
            Building staking, yield, rewards or vesting - anything that pays out over time: start
            from template=staking (rewards come only from a sponsor-funded pool, the clock releases
            at most what the pool holds, every credit is a pool debit in the same operation, unstaking
            has a cooldown; the shipped tests replay the round-4 stake-times-elapsed-times-rate mint
            from an empty pool and require it to fail).
            Building an NFT marketplace, a listing/auction board, or anything with a buy button and
            creator royalties: start from template=marketplace (a buy names the EXACT price it agreed
            to and the listing row is immutable, so the round-5 max_price sandwich - seller reprices
            to the buyer's ceiling, 200 extracted - cannot be written; offers escrow the bidder's
            points and settle atomically with the split asserted; the royalty is fixed at mint, and
            the template DOCUMENTS in its header that a gift plus a side payment bypasses it, with a
            shipped test asserting the bypass still works rather than pretending otherwise).
            AUCTIONS ARE IN THAT TEMPLATE, not something to write freehand: it ships a timed
            ascending auction with NO mutable bid field - the standing bid is its own immutable
            escrow row, raising is delete-and-recreate, and settlement is permissionless after the
            deadline - plus the encumbrance helper every token-moving path consults, because a
            transfer that walks a token out from under an escrowed bid strands it and nothing static
            can see that. Its header has an EXTENDING THIS TEMPLATE section: new market states must
            be mutually exclusive, new token-moving paths must call the same encumbrance helper, and
            new escrow rows must be added to points_in_circulation.
            Building a lending pool, a credit line, a money market - anything where depositors hold
            a SHARE of a pool whose value moves: start from template=lending. Round 6 drained a
            competent hand-built one for 1500 without minting anything, because interest accrued
            only on the paths a borrower signs, so the price of a lender share was stale between
            touches. The template's answer is not "remember to accrue": NO CASH-DENOMINATED DEBT IS
            STORED ANYWHERE. Positions and the pool both carry scaled_debt in index units, the cash
            figures exist only inside a pool_state, pool_now() is the only function that makes one,
            and every pricing helper TAKES one - so a new operation cannot price an entry or an exit
            without a fresh state. It keeps the vault's bounded oracle, over-collateralisation, a
            liquidation threshold with a close factor and bonus, and the minimum-first-deposit guard
            that kills ERC-4626 share inflation; the shipped tests replay the round-6 drain
            (10000 in, 11500 out) and require the attacker to come out no better than they went in.
            Its oracle key is a module arg exactly like the vault's - same note as above applies.
            Its EXTENDING section names the seam: any new operation that moves pool value in a STEP
            rather than with the clock (a fee, a bad-debt write-off, a donation) re-opens the
            just-in-time window, and then you need an entry/exit fee or a minimum holding period.
            NEVER import ${forbiddenModules.joinToString(", ")}.
            require_mandatory_flags only on the main auth descriptor.
            Since CLI 0.30.0, `chr deployment create` writes deployments.<net>.chains into chromia.yml.
            This tool does not send signed transactions and does not run chr.
            Confirm APIs with fetch_docs / search / fetch before inventing module_args keys.
        """.trimIndent()
    }

    /**
     * What to do INSTEAD, for a template name we do not ship. The notes already
     * route a DAO to `governance` and an oracle to `vault`; the unknown-template
     * fallback only listed names and scaffolded `hello`, so
     * `scaffold_dapp(template="lending")` sent an agent off to write the whole
     * value class freehand - and the un-templated class is where BOTH adversary
     * rounds' drains landed. Where the honest answer is "no template covers
     * this", say so and name the hole rather than implying the nearest one does.
     */
    internal fun closestTemplateNote(requested: String): String {
        val t = requested.lowercase()
        fun has(vararg keys: String) = keys.any { it in t }
        return when {
            has("lend", "borrow", "credit", "loan", "debt", "money_market", "moneymarket", "interest", "yield_farm") ->
                "Use `template=lending`: it is the template for this class, and this class is what " +
                    "adversary round 6 drained. A hand-built pool accrued interest LAZILY (only " +
                    "inside the operations a borrower signs), so the price of a lender share was " +
                    "stale between touches while the pending interest was already public on the " +
                    "loan row: a depositor bought in at the stale price, waited one block for the " +
                    "borrower's next touch and withdrew at the fresh one - 10000 in, 11500 out, " +
                    "taken from the other lenders, nothing minted, every conservation invariant " +
                    "green and the gate reporting zero findings. The template does not ask you to " +
                    "remember to accrue: it stores NO cash-denominated debt at all (positions and " +
                    "the pool both carry scaled_debt in index units, the cash figures exist only " +
                    "inside a pool_state, and every pricing helper takes one), so a new operation " +
                    "cannot price an entry or an exit without a fresh state. It also ships the " +
                    "vault's bounded oracle, over-collateralisation, a liquidation threshold with " +
                    "a close factor and bonus, and the minimum-first-deposit guard that kills " +
                    "ERC-4626 share inflation - with the round-6 drain as a must-fail test."
            has("auction", "bid", "nft", "marketplace", "listing", "royalt", "collectible") ->
                "Use `template=marketplace`: it ships listings with exact-price buys, escrowed " +
                    "offers, AND a timed ascending auction with no mutable bid field (the standing " +
                    "bid is its own immutable escrow row), plus the encumbrance helper every " +
                    "token-moving path consults."
            has("dao", "govern", "vot", "treasury", "proposal", "quorum") ->
                "Use `template=governance`: quorum, a fixed voting window, stake-weighted votes and " +
                    "execute-once are structural there, and it ships the single-account drain as a " +
                    "must-fail test."
            has("oracle", "exchange", "vault", "swap", "redeem", "redemption", "price", "amm", "dex", "stablecoin") ->
                "Use `template=vault`: every credit is paid out of a reserve row in the same " +
                    "operation, price posts are bounded, rate-limited and staleness-checked, and it " +
                    "ships the 100 -> 200,000,000 oracle mint as a must-fail test. An AMM's own " +
                    "invariant is yours to prove - the template covers the reserve and the price " +
                    "feed, not the curve."
            has("stak", "reward", "vest", "emission", "farm", "airdrop") ->
                "Use `template=staking`: rewards come only from a sponsor-funded pool, the clock " +
                    "releases at most what the pool holds, every credit is a pool debit in the same " +
                    "operation, and unstaking has a cooldown."
            has("token", "ft4", "asset", "coin", "transfer", "wallet", "payment") ->
                "Use `template=ft4`: it ships the conservation, no-negative-balance and " +
                    "non-owner-must-fail invariant tests to copy for your own economics."
            else ->
                "No shipped template covers that name. The five hardened ones are `governance` " +
                    "(DAO/treasury/voting), `vault` (oracle-priced value, reserves, redemption), " +
                    "`staking` (anything paid out over time), `marketplace` (listings, escrowed " +
                    "offers, auctions, royalties) and `lending` (a pool whose SHARES have a price " +
                    "that moves); `ft4` is the plain token skeleton with runnable " +
                    "invariant tests. Pick the one whose EXPLOIT class matches yours - the value " +
                    "class with no template is where every drain in this project has landed - and " +
                    "if none does, write the economic invariant test FIRST: a passing security " +
                    "check is not economic soundness."
        }
    }

    fun toJson(name: String?, template: String = "hello"): JsonObject {
        val chain = normalizeName(name)
        val effectiveTemplate = if (template in templates) template else "hello"
        val fileMap = files(chain, template)
        // Never silently substitute what the agent asked for (QA finding):
        // surface every fallback as an explicit warning.
        val warnings = mutableListOf<String>()
        val requested = name?.trim().orEmpty()
        if (requested.isNotEmpty() && requested.lowercase() != chain) {
            warnings.add(
                "Requested name '$requested' is not a valid chain name (must match [a-z][a-z0-9_]{0,31}); " +
                    "scaffolded as '$chain' instead - pass a valid name to use it."
            )
        }
        if (template != effectiveTemplate) {
            warnings.add(
                "Unknown template '$template' (valid: ${templates.joinToString(", ")}); scaffolded the " +
                    "'$effectiveTemplate' template. " + closestTemplateNote(template)
            )
        }
        return buildJsonObject {
            put("name", chain)
            put("template", effectiveTemplate)
            put("warnings", buildJsonArray { warnings.forEach { add(JsonPrimitive(it)) } })
            put("rellVersion", RELL_VERSION)
            put("ft4Version", FT4_VERSION)
            put("ft4Api", FT4_API)
            put("merkleHashVersion", MERKLE_HASH_VERSION)
            put("cli", CLI_SERIES)
            put(
                "pins",
                buildJsonObject {
                    put("rell", RELL_VERSION)
                    put("ft4", FT4_VERSION)
                    put("ft4Api", FT4_API)
                    put("merkle_hash_version", MERKLE_HASH_VERSION)
                    put("cli", CLI_SERIES)
                }
            )
            put(
                "forbidden",
                buildJsonArray {
                    forbiddenModules.forEach { add(JsonPrimitive(it)) }
                }
            )
            put(
                "files",
                buildJsonObject {
                    fileMap.forEach { (path, content) -> put(path, content) }
                }
            )
            put("notes", notes(chain))
        }
    }

    private fun chromiaYml(name: String): String = """
        blockchains:
          $name:
            module: main
            config:
              features:
                merkle_hash_version: $MERKLE_HASH_VERSION

        test:
          modules:
            - test.main_test
          failOnError: true

        compile:
          rellVersion: $RELL_VERSION

        libs:
          ft4:
            registry: $FT4_REGISTRY
            path: $FT4_PATH
            tagOrBranch: $FT4_VERSION
            rid: $FT4_RID
            insecure: false
    """.trimIndent() + "\n"

    private fun mainRell(): String = """
        module;

        // Official Hello World (run-dapp-cli + JS/TS hello-world-quickstart).
        // Confirm FT4 imports with fetch_docs.
        // NEVER import lib.ft4.admin, lib.ft4.core.admin, admin.crosschain,
        // ras_open, ras_transfer_open, or lib.ft4.core.accounts.strategies.open.

        object my_name {
          mutable name = "World";
        }

        operation set_name(name) {
          my_name.name = name;
        }

        query hello_world() = "Hello %s!".format(my_name.name);
    """.trimIndent() + "\n"

    private fun mainTestRell(): String = """
        @test module;

        import main;

        function test_hello_world() {
            assert_equals(main.hello_world(), "Hello World!");
        }
    """.trimIndent() + "\n"

    // ---- Golden FT4 template: accounts + authenticated, validated operation ----

    /**
     * The FT4-based chromia.yml every account-holding template ships.
     * [productionModuleArgsNote] is appended inside the production moduleArgs
     * block (4-space indented lines; comments only - a template never ships
     * a production key), [extraTestModuleArgs] inside test.moduleArgs.
     */
    private fun ft4ChromiaYml(
        name: String,
        productionModuleArgsNote: String = "",
        extraTestModuleArgs: String = ""
    ): String = buildString {
        append("blockchains:\n")
        append("  $name:\n")
        append("    module: main\n")
        append("    config:\n")
        append("      features:\n")
        append("        merkle_hash_version: $MERKLE_HASH_VERSION\n")
        Ft4ModuleArgs.moduleArgsYaml().lineSequence().forEach { line ->
            if (line.isBlank()) append('\n') else append("    ").append(line).append('\n')
        }
        append(productionModuleArgsNote)
        append('\n')
        // chr test discovers tests through this section; without it the shipped
        // invariant tests would silently not run under `chr test`.
        append("test:\n")
        append("  modules:\n")
        append("    - test.main_test\n")
        append("  failOnError: true\n")
        // TEST-SCOPED module_args - never under the production blockchain.
        // lib.ft4.test.core (register_alice & co) transitively imports
        // lib.ft4.admin, whose core.admin module_args has no default: without an
        // admin_pubkey here, `chr test` refuses to start ("Missing module_args
        // for module(s): lib.ft4.core.admin") and run_rell_tests fails every tx
        // with "Unable to create GTX module". These are FT4's own PUBLISHED test
        // keys (ft4-lib chromia.yml, tag v1.1.0r) - they gate nothing on a
        // deployed chain because main never imports an admin module, so no admin
        // operation is mounted. Verified green with chr test on 2026-09-02.
        append("  # Test-only FT4 admin wiring (FT4's published test keys - not credentials).\n")
        append("  # Required by lib.ft4.test.core; never move these under blockchains.<name>.\n")
        append("  moduleArgs:\n")
        append("    lib.ft4.core.admin:\n")
        append("      admin_pubkey: x\"$TEST_ADMIN_PUBKEY\"\n")
        append("    lib.ft4.test.core.auth:\n")
        append("      admin_priv_key: x\"$TEST_ADMIN_PRIVKEY\"\n")
        append(extraTestModuleArgs)
        append('\n')
        append("compile:\n")
        append("  rellVersion: $RELL_VERSION\n")
        append('\n')
        append(Ft4ModuleArgs.libsYaml(includeIccf = false))
        // FT4's test helpers import lib.ft4.admin.crosschain, which imports
        // lib.iccf - without this lib `chr test` fails compilation with
        // "Module 'lib.iccf' not found". Official FT4-setup git pin (verified
        // installable + green with chr install / chr test on 2026-09-02).
        // The production app never imports it; run_rell_tests vendors its own.
        append("  # Required only by the shipped tests (FT4 test helpers import lib.iccf).\n")
        append(
            Ft4ModuleArgs.gitIccfYaml()
                .removePrefix("libs:\n")
        )
    }

    private fun ft4MainRell(): String = """
        module;

        import lib.ft4.auth;
        import lib.ft4.accounts;

        // Golden FT4 pattern. Every state-mutating operation must:
        //   1. AUTHENTICATE - resolve which FT4 account signed the call.
        //   2. AUTHORIZE    - prove that account may touch THIS row. Authenticating
        //                     only says who is calling; it does not say the caller
        //                     owns what they are changing. Key writes off the
        //                     authenticated id (as add_note and transfer do below),
        //                     or check ownership explicitly, as delete_note does:
        //                     require(row.owner == account.id).
        //                     Never trust an account id passed in as a parameter.
        //   3. VALIDATE     - require(...) every input, each parameter separately.
        //   4. CHECK INVARIANTS - conservation (value created must be backed),
        //                     bounds, quorum. Nothing below enforces these for you -
        //                     state each one as a test the way src/test/main_test.rell
        //                     does, and keep those tests passing as this file grows.
        // Never import FT4 admin modules or open registration/transfer strategies
        // (see the project security pins).

        entity note {
            index owner: byte_array;
            body: text;
            created_at: timestamp;
        }

        // A tiny point ledger the shipped invariant tests exercise. The one-time
        // welcome grant in register_wallet is the ONLY place points are created;
        // transfer moves them and must never mint or destroy them. If you replace
        // this ledger with your own economics, carry the same discipline: every
        // unit of value credited must be debited from somewhere real.
        entity wallet {
            key owner: byte_array;
            mutable balance: integer = 0;
        }

        val WELCOME_POINTS = 100;

        // DEFAULT: every operation requires the Transfer flag unless a scoped
        // handler below loosens it. FT4 resolves flags with contains_all(), and
        // contains_all([]) is ALWAYS true - an empty default would let limited
        // session/login descriptors call ANY operation, including value-moving
        // ones you add later. Defaulting to ["T"] means a new operation is safe
        // before you think about it; grant exceptions per operation, never by
        // weakening this default.
        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            flags = ["T"]
        );

        // EXCEPTION, scoped by operation name: the note demo moves no value, so
        // limited session descriptors may call it. flags = [] requires nothing -
        // use it ONLY for operations that cannot move value, and only scoped
        // like this. transfer has no exception: it moves value, so it stays on
        // the ["T"] default - a session key the user believed could not spend
        // their funds must not be able to call it.
        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            scope = "add_note",
            flags = []
        );

        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            scope = "delete_note",
            flags = []
        );

        operation add_note(body: text) {
            // 1. Authenticate: resolves the FT4 account that signed this call.
            val account = auth.authenticate();
            // 2. Authorize: the write below is keyed off the authenticated id,
            //    so the caller can only ever create notes it owns.
            // 3. Validate inputs before touching state.
            require(body.size() > 0, "note must not be empty");
            require(body.size() <= 1000, "note too long (max 1000 chars)");
            // 4. Mutate state only after auth + validation.
            create note(owner = account.id, body, created_at = op_context.last_block_time);
        }

        operation delete_note(note_id: rowid) {
            val account = auth.authenticate();
            val n = require(note @? { .rowid == note_id }, "no such note");
            // AUTHORIZE: authentication says who is calling - this require says
            // the caller owns the row it names. Without it, any account could
            // delete any note. The shipped test proves a non-owner is refused.
            require(n.owner == account.id, "not your note");
            delete n;
        }

        operation register_wallet() {
            val account = auth.authenticate();
            require(wallet @? { .owner == account.id } == null, "wallet already registered");
            create wallet(owner = account.id, balance = WELCOME_POINTS);
        }

        operation transfer(to: byte_array, amount: integer) {
            // 1. AUTHENTICATE - who signed this call.
            val account = auth.authenticate();
            // 2. AUTHORIZE - spend only from the CALLER's wallet. Never accept a
            //    `from: byte_array` parameter here: an operation that debits an
            //    account named by a parameter lets anyone drain anyone, and a
            //    static check cannot tell you that.
            val from_wallet = require(wallet @? { .owner == account.id }, "register a wallet first");
            // 3. VALIDATE - every input separately. Rell integers are 64-bit
            //    and arithmetic overflow ABORTS the operation, so bound any
            //    input you will multiply (e.g. by a price or scale factor)
            //    BEFORE the arithmetic, or large legitimate amounts abort.
            require(amount > 0, "amount must be positive");
            require(to != account.id, "cannot transfer to yourself");
            val to_wallet = require(wallet @? { .owner == to }, "recipient has no wallet");
            // 4. CHECK INVARIANTS - no negative balances, and conservation: the
            //    debit and credit below are the same amount, so transfers can
            //    never change the total in circulation. src/test/main_test.rell
            //    asserts both.
            require(from_wallet.balance >= amount, "insufficient balance");
            update from_wallet ( .balance -= amount );
            update to_wallet ( .balance += amount );
        }

        query get_notes(owner: byte_array) = note @* { .owner == owner } (
            .body,
            .created_at
        );

        query get_balance(owner: byte_array): integer {
            val w = wallet @? { .owner == owner };
            return if (w != null) w.balance else 0;
        }

        // INVARIANT: every point in circulation came from a welcome grant -
        // transfers move value, they never create it. The shipped conservation
        // test compares this to registered_wallets() * WELCOME_POINTS.
        query points_in_circulation(): integer {
            var total = 0;
            for (b in wallet @* {} ( .balance )) total += b;
            return total;
        }

        query registered_wallets(): integer = wallet @* {} ( .owner ).size();
    """.trimIndent() + "\n"

    private fun ft4TestRell(): String = """
        @test module;

        // Invariant tests the scaffold ships. They are real: they register FT4
        // test accounts, sign operations, and run against PostgreSQL - via
        // run_rell_tests (server needs CHROMIA_TEST_DATABASE_URL; pass the
        // module_args from chromia.yml INCLUDING the test.moduleArgs block) or
        // `chr test` (works as scaffolded: the yml carries the required
        // test-only FT4 admin args and the iccf lib the test helpers import).
        // Keep them passing as main.rell grows, and add one such test for every
        // property your app's economics depend on:
        //   - CONSERVATION: a transfer never changes the total in circulation.
        //   - NO NEGATIVE BALANCE: an overdraft aborts instead of going negative.
        //   - AUTHORIZATION: a non-owner's attempt MUST fail - a passing "happy
        //     path" test proves nothing about who else can call the operation.
        // test_transfer_conserves_total_points is the template to copy for your
        // own invariant: state the property in one line, then try to violate it.

        import main;
        import lib.ft4.test.core.{ register_alice, register_bob, ft_auth_operation_for };

        function signed(keypair: rell.test.keypair, op: rell.test.op) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .sign(keypair)
                .run();
        }

        function signed_must_fail(keypair: rell.test.keypair, op: rell.test.op, expected: text) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .sign(keypair)
                .run_must_fail(expected);
        }

        // CONSERVATION: transfers move points; only register_wallet creates them.
        function test_transfer_conserves_total_points() {
            val alice = register_alice();
            val bob = register_bob();
            signed(alice.keypair, main.register_wallet());
            signed(bob.keypair, main.register_wallet());
            assert_equals(main.points_in_circulation(), main.registered_wallets() * main.WELCOME_POINTS);

            signed(alice.keypair, main.transfer(bob.account.id, 30));

            assert_equals(main.get_balance(alice.account.id), main.WELCOME_POINTS - 30);
            assert_equals(main.get_balance(bob.account.id), main.WELCOME_POINTS + 30);
            // The invariant: no sequence of transfers changes the total.
            assert_equals(main.points_in_circulation(), main.registered_wallets() * main.WELCOME_POINTS);
        }

        // NO NEGATIVE BALANCE: overdrafts abort with the exact message - and
        // nothing moves.
        function test_overdraft_must_fail() {
            val alice = register_alice();
            val bob = register_bob();
            signed(alice.keypair, main.register_wallet());
            signed(bob.keypair, main.register_wallet());

            signed_must_fail(
                alice.keypair,
                main.transfer(bob.account.id, main.WELCOME_POINTS + 1),
                "insufficient balance"
            );

            assert_equals(main.get_balance(alice.account.id), main.WELCOME_POINTS);
            assert_equals(main.get_balance(bob.account.id), main.WELCOME_POINTS);
        }

        // AUTHORIZATION: bob authenticates as himself, then tries to touch
        // alice's row. The operation must refuse - this is the test that
        // catches a missing require(row.owner == account.id).
        function test_non_owner_cannot_delete_note() {
            val alice = register_alice();
            val bob = register_bob();
            signed(alice.keypair, main.add_note("alice's private note"));
            val note_id = (main.note @ { .owner == alice.account.id }).rowid;

            signed_must_fail(bob.keypair, main.delete_note(note_id), "not your note");

            assert_equals(main.get_notes(alice.account.id).size(), 1);
        }
    """.trimIndent() + "\n"

    private fun ft4ClientTs(name: String): String = """
        // TypeScript client for the `$name` dapp (postchain-client + FT4).
        // npm i postchain-client @chromia/ft4
        import { createClient } from "postchain-client";
        import { createKeyStoreInteractor, createInMemoryFtKeyStore } from "@chromia/ft4";

        const client = await createClient({
            nodeUrlPool: ["http://localhost:7740"], // chr node start; use network nodes for testnet/mainnet
            blockchainIid: 0,
        });

        // Sign with an FT4 session (see FT4 docs for account registration flows).
        const keyStore = createInMemoryFtKeyStore(/* your key pair */);
        const { getSession } = createKeyStoreInteractor(client, keyStore);
        const session = await getSession(/* your account id */);

        await session.call({ name: "add_note", args: ["hello from ts"] });
        const notes = await client.query("get_notes", { owner: /* account id bytes */ });
        console.log(notes);
    """.trimIndent() + "\n"

    // ---- governance template: quorum, fixed window, stake weight, execute-once are structural ----
    //
    // Adversary round 1 (exploit-corpus/realworld/adversary-1/dapp_c_dao) drained a
    // DAO the gate certified ok:true: one account with zero contribution proposed
    // paying itself the treasury, cast its single vote, and executed after a 1 ms
    // window. Static analysis can only advise here - a stake-weighted execute is
    // textually identical to an unweighted one - so this template makes each step
    // of that drain unwritable: the window is a constant (no parameter to shrink),
    // votes weigh stake (no stake, no proposal, no weight), execution needs a quorum
    // snapshotted at proposal time, and the executed flag flips in the paying op.

    private fun governanceMainRell(): String = """
        module;

        import lib.ft4.auth;
        import lib.ft4.accounts;

        // Governance template: a member-funded treasury that pays out only by
        // stake-weighted vote. Four guards are STRUCTURAL - they live in the
        // entities and constants, not in a require() a future operation can forget:
        //   QUORUM        - execute_proposal needs yes_weight + no_weight >= quorum_weight,
        //                   snapshotted from the total stake when the proposal was created,
        //                   so joining late cannot shrink the bar.
        //   VOTING WINDOW - VOTING_PERIOD_MS is a constant, not a parameter. Nothing a
        //                   proposer sends can close a vote before others can act. If you
        //                   make it configurable, floor it: require(period >= VOTING_PERIOD_MS).
        //   STAKE WEIGHT  - a vote weighs what the voter has locked in the treasury. A
        //                   member with nothing at stake cannot propose and weighs zero.
        //   EXECUTED ONCE - `executed` flips in the same operation that pays.
        // The single-account drain (zero-stake proposer pays itself, votes 1-0,
        // executes after a 1 ms window) is refused at every step - it cannot propose,
        // cannot end the window early, and cannot reach quorum alone.
        // src/test/main_test.rell replays that exact attack and REQUIRES it to fail;
        // keep that test passing as this file grows.
        // What no template can fix: a member holding more than QUORUM_BPS of all stake
        // owns the DAO by construction. That is what stake weighting means - choose
        // QUORUM_BPS and a member cap for your own economics.

        entity member {
            key owner: byte_array;
            mutable balance: integer = 0;   // spendable points
            mutable stake: integer = 0;     // points locked in the treasury = voting weight
        }

        // The treasury and the total stake behind it: one row, updated in the same
        // operations that move member points, so the conservation queries below
        // hold at every block.
        object dao {
            mutable treasury_balance: integer = 0;
            mutable total_stake: integer = 0;
        }

        entity proposal {
            index proposer: byte_array;
            title: text;
            beneficiary: byte_array;
            amount: integer;
            created_at: timestamp;
            deadline: timestamp;
            quorum_weight: integer;         // snapshot: total_stake * QUORUM_BPS / 10000 at creation
            mutable yes_weight: integer = 0;
            mutable no_weight: integer = 0;
            mutable executed: boolean = false;
        }

        // One vote per member per proposal: the key aborts a second vote.
        entity vote {
            key proposal, voter: byte_array;
            weight: integer;
            support: boolean;
        }

        // The one-time welcome grant is the ONLY place points are created (a
        // stand-in for a real deposit - replace with an FT4 asset transfer and
        // keep the same discipline: every credit is debited from somewhere real).
        val WELCOME_POINTS = 1000;
        // Three days. A constant: there is no parameter that shortens it.
        val VOTING_PERIOD_MS = 3 * 24 * 60 * 60 * 1000;
        // Half of all stake must vote for a proposal to be decidable at all.
        val QUORUM_BPS = 5000;
        val MAX_TITLE_LENGTH = 200;

        // DEFAULT: every operation requires the Transfer flag. FT4 resolves flags
        // with contains_all(), and contains_all([]) is always true - never weaken
        // this default; grant flags = [] only per operation, scoped, for
        // operations that cannot move value.
        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            flags = ["T"]
        );

        function member_of(owner: byte_array): member =
            require(member @? { .owner == owner }, "register as a member first");

        function quorum_for(total_stake: integer): integer =
            max(1, total_stake * QUORUM_BPS / 10000);

        operation register_member() {
            val account = auth.authenticate();
            require(member @? { .owner == account.id } == null, "already a member");
            create member(owner = account.id, balance = WELCOME_POINTS);
        }

        // Lock points in the treasury. Stake is voting weight: what you can lose
        // is what you may decide over.
        operation fund_treasury(amount: integer) {
            val account = auth.authenticate();
            val m = member_of(account.id);
            require(amount > 0, "amount must be positive");
            require(m.balance >= amount, "insufficient balance");
            update m ( .balance -= amount, .stake += amount );
            dao.treasury_balance += amount;
            dao.total_stake += amount;
        }

        operation create_proposal(title: text, beneficiary: byte_array, amount: integer) {
            // 1. AUTHENTICATE
            val account = auth.authenticate();
            // 2. AUTHORIZE - only a member with stake may put the treasury to a vote.
            val proposer = member_of(account.id);
            require(proposer.stake > 0, "only members with stake may propose");
            // 3. VALIDATE - each input separately.
            require(title.size() > 0 and title.size() <= MAX_TITLE_LENGTH, "invalid title");
            require(member @? { .owner == beneficiary } != null, "beneficiary is not a member");
            require(amount > 0, "amount must be positive");
            require(amount <= dao.treasury_balance, "amount exceeds treasury");
            // 4. INVARIANTS - the window and the quorum are fixed HERE, from
            //    constants and the current total stake; the proposer chooses neither.
            val now = op_context.last_block_time;
            create proposal(
                proposer = account.id,
                title = title,
                beneficiary = beneficiary,
                amount = amount,
                created_at = now,
                deadline = now + VOTING_PERIOD_MS,
                quorum_weight = quorum_for(dao.total_stake)
            );
        }

        operation cast_vote(proposal_id: rowid, support: boolean) {
            val account = auth.authenticate();
            val voter = member_of(account.id);
            val p = require(proposal @? { .rowid == proposal_id }, "proposal not found");
            require(not p.executed, "proposal already executed");
            require(op_context.last_block_time < p.deadline, "voting has ended");
            // A vote weighs the voter's stake. Zero stake is zero weight, and is
            // refused outright so a spam of empty votes cannot count toward quorum.
            val weight = voter.stake;
            require(weight > 0, "no voting weight: fund the treasury first");
            create vote(proposal = p, voter = account.id, weight = weight, support = support);
            if (support) {
                update p ( .yes_weight += weight );
            } else {
                update p ( .no_weight += weight );
            }
        }

        // Any member may trigger execution: the outcome is fixed by the votes.
        operation execute_proposal(proposal_id: rowid) {
            auth.authenticate();
            val p = require(proposal @? { .rowid == proposal_id }, "proposal not found");
            require(not p.executed, "proposal already executed");
            require(op_context.last_block_time >= p.deadline, "voting is still open");
            require(p.yes_weight + p.no_weight >= p.quorum_weight, "quorum not reached");
            require(p.yes_weight > p.no_weight, "proposal was not approved");
            require(dao.treasury_balance >= p.amount, "treasury cannot cover the proposal");
            val b = member_of(p.beneficiary);
            // Flip the flag and move the value in the same operation: Rell
            // operations are atomic, so there is no state where one happened
            // without the other.
            update p ( .executed = true );
            dao.treasury_balance -= p.amount;
            update b ( .balance += p.amount );
        }

        query get_proposal(proposal_id: rowid) {
            val p = require(proposal @? { .rowid == proposal_id }, "proposal not found");
            return (
                title = p.title,
                beneficiary = p.beneficiary,
                amount = p.amount,
                deadline = p.deadline,
                quorum_weight = p.quorum_weight,
                yes_weight = p.yes_weight,
                no_weight = p.no_weight,
                executed = p.executed
            );
        }

        query get_balance(owner: byte_array): integer {
            val m = member @? { .owner == owner };
            return if (m != null) m.balance else 0;
        }

        query get_stake(owner: byte_array): integer {
            val m = member @? { .owner == owner };
            return if (m != null) m.stake else 0;
        }

        query treasury_balance(): integer = dao.treasury_balance;

        query total_stake(): integer = dao.total_stake;

        query member_count(): integer = member @* {} ( .owner ).size();

        // INVARIANT: every point in circulation came from a welcome grant. Points
        // are either spendable (member.balance) or in the treasury; funding and
        // paying out move them, they never create or destroy them. The shipped
        // conservation test compares this to member_count() * WELCOME_POINTS.
        query points_in_circulation(): integer {
            var total = dao.treasury_balance;
            for (b in member @* {} ( .balance )) total += b;
            return total;
        }

        // INVARIANT: the stake ledger and the dao's total agree.
        query staked_points(): integer {
            var total = 0;
            for (s in member @* {} ( .stake )) total += s;
            return total;
        }
    """.trimIndent() + "\n"

    private fun governanceTestRell(): String = """
        @test module;

        // The governance template's invariant tests. They are real: FT4 test
        // accounts, signed operations, PostgreSQL - run via run_rell_tests (pass
        // chromia.yml's moduleArgs PLUS its test.moduleArgs block) or `chr test`.
        //
        // test_round1_single_account_drain_must_fail replays the adversary's DAO
        // drain step by step against this template and REQUIRES each step to be
        // refused. That test is the proof the bug is unwritable here: it can only
        // pass while the guards stand, so if you ever delete one, this goes red
        // before an attacker finds out.

        import main;
        import lib.ft4.test.core.{ register_alice, register_bob, register_trudy, ft_auth_operation_for };

        function signed(keypair: rell.test.keypair, op: rell.test.op) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run();
        }

        function signed_must_fail(keypair: rell.test.keypair, op: rell.test.op, expected: text) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run_must_fail(expected);
        }

        // Advance the chain past the voting window: the next block is stamped
        // VOTING_PERIOD_MS + 1 after the last one.
        function close_voting_window() {
            rell.test.set_next_block_time_delta(main.VOTING_PERIOD_MS + 1);
            rell.test.block().run();
        }

        function assert_conserved() {
            assert_equals(main.points_in_circulation(), main.member_count() * main.WELCOME_POINTS);
            assert_equals(main.staked_points(), main.total_stake());
        }

        function proposal_by(proposer: byte_array): rowid =
            (main.proposal @ { .proposer == proposer }).rowid;

        // EXPLOIT MUST FAIL. Round 1: two honest members fund 600 + 400; a third
        // account with nothing at stake proposes paying itself the treasury, casts
        // one vote, and executes after a 1 ms window. Here every step is refused.
        function test_round1_single_account_drain_must_fail() {
            val alice = register_alice();
            val bob = register_bob();
            val trudy = register_trudy();
            signed(alice.keypair, main.register_member());
            signed(bob.keypair, main.register_member());
            signed(trudy.keypair, main.register_member());
            signed(alice.keypair, main.fund_treasury(600));
            signed(bob.keypair, main.fund_treasury(400));
            assert_equals(main.treasury_balance(), 1000);
            assert_equals(main.get_stake(trudy.account.id), 0);

            // Step 1 - a zero-stake account cannot even propose.
            signed_must_fail(
                trudy.keypair,
                main.create_proposal("pay me", trudy.account.id, 1000),
                "only members with stake may propose"
            );

            // Step 2 - with the smallest possible stake the proposal exists, but the
            // attacker's single vote weighs 1 against a quorum of 500.
            signed(trudy.keypair, main.fund_treasury(1));
            signed(trudy.keypair, main.create_proposal("pay me", trudy.account.id, 1001));
            val pid = proposal_by(trudy.account.id);
            signed(trudy.keypair, main.cast_vote(pid, true));

            // Step 3 - the window is a constant: executing in the next block is refused.
            signed_must_fail(trudy.keypair, main.execute_proposal(pid), "voting is still open");

            // Step 4 - even after the window closes, quorum was never reached.
            close_voting_window();
            signed_must_fail(trudy.keypair, main.execute_proposal(pid), "quorum not reached");

            assert_equals(main.treasury_balance(), 1001);
            assert_equals(main.get_balance(trudy.account.id), main.WELCOME_POINTS - 1);
            assert_conserved();
        }

        // HAPPY PATH + EXECUTE ONCE + CONSERVATION: enough stake votes yes, the
        // payout lands exactly once, and no point is created or lost on the way.
        function test_stake_weighted_proposal_pays_once_and_conserves_points() {
            val alice = register_alice();
            val bob = register_bob();
            signed(alice.keypair, main.register_member());
            signed(bob.keypair, main.register_member());
            signed(alice.keypair, main.fund_treasury(600));
            signed(bob.keypair, main.fund_treasury(400));

            signed(alice.keypair, main.create_proposal("pay bob for the audit", bob.account.id, 300));
            val pid = proposal_by(alice.account.id);
            assert_equals(main.get_proposal(pid).quorum_weight, 500);
            signed(alice.keypair, main.cast_vote(pid, true));
            signed(bob.keypair, main.cast_vote(pid, true));
            // One vote per member: the (proposal, voter) key refuses a second one.
            rell.test.tx()
                .op(ft_auth_operation_for(bob.keypair.pub))
                .op(main.cast_vote(pid, true))
                .sign(bob.keypair)
                .run_must_fail();
            assert_equals(main.get_proposal(pid).yes_weight, 1000);

            signed_must_fail(bob.keypair, main.execute_proposal(pid), "voting is still open");
            close_voting_window();
            signed(bob.keypair, main.execute_proposal(pid));

            assert_equals(main.get_balance(bob.account.id), main.WELCOME_POINTS - 400 + 300);
            assert_equals(main.treasury_balance(), 700);
            // Executed once: a replay is refused and moves nothing.
            signed_must_fail(bob.keypair, main.execute_proposal(pid), "proposal already executed");
            assert_equals(main.treasury_balance(), 700);
            assert_conserved();
        }

        // STAKE WEIGHT BEATS HEAD COUNT: two yes votes worth 301 lose to one no
        // vote worth 600, and a member with no stake has no vote at all.
        function test_majority_of_stake_can_reject() {
            val alice = register_alice();
            val bob = register_bob();
            val trudy = register_trudy();
            signed(alice.keypair, main.register_member());
            signed(bob.keypair, main.register_member());
            signed(trudy.keypair, main.register_member());
            signed(alice.keypair, main.fund_treasury(600));
            signed(bob.keypair, main.fund_treasury(300));

            signed(bob.keypair, main.create_proposal("pay bob", bob.account.id, 500));
            val pid = proposal_by(bob.account.id);
            signed_must_fail(trudy.keypair, main.cast_vote(pid, true), "no voting weight");
            signed(trudy.keypair, main.fund_treasury(1));
            signed(trudy.keypair, main.cast_vote(pid, true));
            signed(bob.keypair, main.cast_vote(pid, true));
            signed(alice.keypair, main.cast_vote(pid, false));

            close_voting_window();
            signed_must_fail(bob.keypair, main.execute_proposal(pid), "proposal was not approved");
            assert_equals(main.treasury_balance(), 901);
            assert_conserved();
        }
    """.trimIndent() + "\n"

    // ---- vault template: every credit is a reserve debit; prices are bounded and time-checked ----
    //
    // Adversary round 1 (exploit-corpus/realworld/adversary-1/dapp_d_oracle) turned
    // 100 USD into 200,000,000 through a gate that said ok:true: sell_tokens credited
    // USD that no reserve ever held, and set_price accepted any positive number at any
    // time. This template keeps the vault's own holdings as ordinary rows of the same
    // entities, so a trade is a transfer between rows (the credit cannot exist without
    // its debit), bounds and rate-limits every price update, and refuses to trade on
    // a stale price. The oracle key is a module arg the production yml deliberately
    // leaves unset: the chain cannot build with a placeholder key.

    /**
     * Module args for running the vault template's shipped tests: FT4's test
     * wiring plus the oracle key the vault reads from `main`. The key is FT4's
     * published TEST admin key, used here only so the tests can sign price posts
     * - it mirrors chromia.yml's test.moduleArgs and, like them, never belongs
     * under blockchains.<name>.
     */
    fun vaultTestModuleArgs(): Map<String, Map<String, kotlinx.serialization.json.JsonElement>> =
        oracleTestModuleArgs()

    private fun vaultChromiaYml(name: String): String = oracleChromiaYml(name, "vault")

    /**
     * chromia.yml for a template whose main module reads an oracle key from
     * configuration. The production block deliberately leaves the key UNSET -
     * commented out with instructions - so the chain cannot be built with a
     * placeholder; test.moduleArgs wires FT4's published test key so the
     * shipped tests can sign price posts.
     */
    private fun oracleChromiaYml(name: String, owner: String): String = ft4ChromiaYml(
        name,
        productionModuleArgsNote = buildString {
            append("      # REQUIRED before `chr build` / deploy - the $owner's oracle key. It is\n")
            append("      # deliberately NOT set here so the chain cannot be built with a\n")
            append("      # placeholder: put your oracle's 33-byte compressed public key here and\n")
            append("      # nowhere in source. Never copy the test key from test.moduleArgs.\n")
            append("      # main:\n")
            append("      #   oracle_pubkey: x\"<your oracle public key>\"\n")
        },
        extraTestModuleArgs = buildString {
            append("    # The shipped tests sign price posts with FT4's published test key.\n")
            append("    main:\n")
            append("      oracle_pubkey: x\"$TEST_ADMIN_PUBKEY\"\n")
        }
    )

    private fun vaultMainRell(): String = """
        module;

        import lib.ft4.auth;
        import lib.ft4.accounts;

        // Vault template: an oracle-priced exchange between a cash balance and a
        // token balance. Three guards are STRUCTURAL:
        //   RESERVE-BACKED  - the vault's holdings are rows of the SAME entities as
        //                     users' balances, keyed by the chain's own id. Every trade
        //                     debits one row and credits another in the same operation;
        //                     there is no code path that credits without a debit, and a
        //                     sale the reserve cannot cover aborts. Value is conserved
        //                     by construction - the shipped tests assert it after every
        //                     trade, including the adversary's replayed drain.
        //   BOUNDED PRICE   - set_price refuses a move beyond MAX_PRICE_MOVE_BPS of the
        //                     previous price and a second post inside
        //                     MIN_PRICE_UPDATE_INTERVAL_MS, so a wrong or hostile post is
        //                     capped per hour instead of arbitrary per block.
        //   TIME-CHECKED    - a price older than MAX_PRICE_AGE_MS halts trading until
        //                     the oracle posts again; an uninitialised feed never trades.
        // The oracle is the ONE key in chain_context.args.oracle_pubkey - configured,
        // never a parameter, never in source.
        // What no template can fix: an honest-but-wrong oracle still moves price
        // within the bound, and a trader can capture that move - but only out of what
        // the reserve holds, never out of thin air. Size MAX_PRICE_MOVE_BPS and the
        // interval for your asset, and keep the conservation test green.

        struct module_args {
            oracle_pubkey: pubkey;
        }

        entity cash_account {
            key owner: byte_array;
            mutable balance: integer = 0;
        }

        entity token_account {
            key owner: byte_array;
            mutable balance: integer = 0;
        }

        // price == 0 means "never posted": trading refuses until the oracle posts.
        object price_feed {
            mutable price: integer = 0;
            mutable updated_at: timestamp = 0;
        }

        // Cash per token, scaled: PRICE_SCALE == 1.00.
        val PRICE_SCALE = 1000000;
        val MAX_PRICE = 1000 * PRICE_SCALE;
        // A post may move the price at most 20% from the previous post...
        val MAX_PRICE_MOVE_BPS = 2000;
        // ...and at most once an hour, so the worst case is bounded per hour.
        val MIN_PRICE_UPDATE_INTERVAL_MS = 60 * 60 * 1000;
        // A price older than a day is not a price.
        val MAX_PRICE_AGE_MS = 24 * 60 * 60 * 1000;
        // Bound every amount BEFORE multiplying by a price: Rell integers are
        // 64-bit and overflow aborts. MAX_TRADE_AMOUNT * MAX_PRICE fits.
        val MAX_TRADE_AMOUNT = 1000000000;
        // The one-time welcome grant is the ONLY place cash is created (a
        // stand-in for a real deposit - replace with an FT4 asset transfer and keep
        // the same discipline). Tokens exist only as the vault's initial reserve.
        val WELCOME_CASH = 1000;
        val INITIAL_TOKEN_SUPPLY = 1000000;

        // DEFAULT: every operation requires the Transfer flag. FT4 resolves flags
        // with contains_all(), and contains_all([]) is always true - never weaken
        // this default; grant flags = [] only per operation, scoped, for
        // operations that cannot move value.
        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            flags = ["T"]
        );

        // The vault's own rows are keyed by the chain's id: not a key, not a
        // constant anyone can register, and never a parameter.
        function vault_id(): byte_array = chain_context.blockchain_rid;

        function cash_of(owner: byte_array): cash_account =
            require(cash_account @? { .owner == owner }, "register an account first");

        function tokens_of(owner: byte_array): token_account =
            require(token_account @? { .owner == owner }, "register an account first");

        function cash_reserve(): cash_account {
            val vault = vault_id();
            val r = cash_account @? { .owner == vault };
            if (r != null) return r;
            return create cash_account(owner = vault, balance = 0);
        }

        function token_reserve(): token_account {
            val vault = vault_id();
            val r = token_account @? { .owner == vault };
            if (r != null) return r;
            return create token_account(owner = vault, balance = INITIAL_TOKEN_SUPPLY);
        }

        // The price a trade may use: posted, and not stale.
        function current_price(): integer {
            require(price_feed.price > 0, "price feed not initialised");
            require(
                op_context.last_block_time - price_feed.updated_at <= MAX_PRICE_AGE_MS,
                "price feed is stale"
            );
            return price_feed.price;
        }

        operation set_price(price: integer) {
            // 1+2. AUTHENTICATE + AUTHORIZE: the configured oracle key, never a parameter.
            require(op_context.is_signer(chain_context.args.oracle_pubkey), "oracle only");
            // 3. VALIDATE the post itself...
            require(price > 0 and price <= MAX_PRICE, "price out of range");
            // 4. ...and bound it against the previous post, in size and in time. The
            //    first post initialises the feed and is exempt from the move bound.
            val prev = price_feed.price;
            val now = op_context.last_block_time;
            if (prev > 0) {
                require(price * 10000 <= prev * (10000 + MAX_PRICE_MOVE_BPS), "price move exceeds bound");
                require(price * 10000 >= prev * (10000 - MAX_PRICE_MOVE_BPS), "price move exceeds bound");
                require(now - price_feed.updated_at >= MIN_PRICE_UPDATE_INTERVAL_MS, "price update too soon");
            }
            price_feed.price = price;
            price_feed.updated_at = now;
        }

        operation register_account() {
            val account = auth.authenticate();
            require(cash_account @? { .owner == account.id } == null, "account already registered");
            create cash_account(owner = account.id, balance = WELCOME_CASH);
            create token_account(owner = account.id, balance = 0);
            // The vault's rows exist from the first registration, so the
            // conservation queries are meaningful before the first trade.
            cash_reserve();
            token_reserve();
        }

        operation buy_tokens(cash_in: integer) {
            // 1. AUTHENTICATE  2. AUTHORIZE - only the caller's own rows are debited.
            val account = auth.authenticate();
            val buyer_cash = cash_of(account.id);
            val buyer_tokens = tokens_of(account.id);
            // 3. VALIDATE - bound before the multiplication below.
            require(cash_in > 0, "amount must be positive");
            require(cash_in <= MAX_TRADE_AMOUNT, "amount too large");
            require(buyer_cash.balance >= cash_in, "insufficient cash");
            val price = current_price();
            val tokens_out = cash_in * PRICE_SCALE / price;
            require(tokens_out > 0, "amount too small");
            // 4. INVARIANTS - the tokens come out of the reserve or not at all.
            val reserve_cash = cash_reserve();
            val reserve_tokens = token_reserve();
            require(reserve_tokens.balance >= tokens_out, "vault cannot cover the trade");
            update buyer_cash ( .balance -= cash_in );
            update reserve_cash ( .balance += cash_in );
            update reserve_tokens ( .balance -= tokens_out );
            update buyer_tokens ( .balance += tokens_out );
        }

        operation sell_tokens(tokens_in: integer) {
            val account = auth.authenticate();
            val seller_cash = cash_of(account.id);
            val seller_tokens = tokens_of(account.id);
            require(tokens_in > 0, "amount must be positive");
            require(tokens_in <= MAX_TRADE_AMOUNT, "amount too large");
            require(seller_tokens.balance >= tokens_in, "insufficient tokens");
            val price = current_price();
            val cash_out = tokens_in * price / PRICE_SCALE;
            require(cash_out > 0, "amount too small");
            // The cash comes out of the reserve or not at all: this is the line the
            // adversary's sell_tokens did not have, and it is what keeps a price
            // swing from minting balance nobody deposited.
            val reserve_cash = cash_reserve();
            val reserve_tokens = token_reserve();
            require(reserve_cash.balance >= cash_out, "vault cannot cover the trade");
            update seller_tokens ( .balance -= tokens_in );
            update reserve_tokens ( .balance += tokens_in );
            update reserve_cash ( .balance -= cash_out );
            update seller_cash ( .balance += cash_out );
        }

        query get_token_price(): integer = price_feed.price;

        query get_price_updated_at(): timestamp = price_feed.updated_at;

        query oracle_pubkey(): pubkey = chain_context.args.oracle_pubkey;

        query get_cash_balance(owner: byte_array): integer {
            val a = cash_account @? { .owner == owner };
            return if (a != null) a.balance else 0;
        }

        query get_token_balance(owner: byte_array): integer {
            val a = token_account @? { .owner == owner };
            return if (a != null) a.balance else 0;
        }

        query get_vault_cash(): integer {
            val vault = vault_id();
            val a = cash_account @? { .owner == vault };
            return if (a != null) a.balance else 0;
        }

        query get_vault_tokens(): integer {
            val vault = vault_id();
            val a = token_account @? { .owner == vault };
            return if (a != null) a.balance else 0;
        }

        query registered_accounts(): integer {
            val vault = vault_id();
            return cash_account @* { .owner != vault } ( .owner ).size();
        }

        // INVARIANT: cash is only ever created by the welcome grant; trades move it
        // between a user row and the vault row. The shipped tests compare this to
        // registered_accounts() * WELCOME_CASH after every trade.
        query cash_in_circulation(): integer {
            var total = 0;
            for (b in cash_account @* {} ( .balance )) total += b;
            return total;
        }

        // INVARIANT: tokens are only ever the vault's initial reserve, moved around.
        query tokens_in_circulation(): integer {
            var total = 0;
            for (b in token_account @* {} ( .balance )) total += b;
            return total;
        }
    """.trimIndent() + "\n"

    private fun vaultTestRell(): String = """
        @test module;

        // The vault template's invariant tests. They are real: FT4 test accounts,
        // signed operations, PostgreSQL - run via run_rell_tests (pass chromia.yml's
        // moduleArgs PLUS its test.moduleArgs block, which carries the oracle key the
        // tests sign with) or `chr test`.
        //
        // The two test_round1_* functions replay the adversary's oracle drain against
        // this template and REQUIRE it to be refused, then assert that nothing was
        // created: cash_in_circulation() and tokens_in_circulation() are unchanged by
        // any trade. They can only pass while the guards stand.

        import main;
        import lib.ft4.test.core.{ register_alice, ft_auth_operation_for };
        // admin_priv_key() is defined in test.core.auth; importing it from the parent
        // module is ambiguous (FT4's own assets.rell imports it from ^.auth too).
        import lib.ft4.test.core.auth.{ admin_priv_key };

        // The oracle keypair: FT4's published test key, wired through
        // test.moduleArgs (lib.ft4.test.core.auth.admin_priv_key + main.oracle_pubkey).
        function oracle(): rell.test.keypair =
            rell.test.keypair(priv = admin_priv_key(), pub = main.oracle_pubkey());

        function post_price(price: integer) {
            rell.test.tx().op(main.set_price(price)).nop().sign(oracle()).run();
        }

        function post_price_must_fail(price: integer, expected: text) {
            rell.test.tx().op(main.set_price(price)).nop().sign(oracle()).run_must_fail(expected);
        }

        function signed(keypair: rell.test.keypair, op: rell.test.op) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run();
        }

        function signed_must_fail(keypair: rell.test.keypair, op: rell.test.op, expected: text) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run_must_fail(expected);
        }

        // Stamp the next block `ms` after the last one.
        function after(ms: integer) {
            rell.test.set_next_block_time_delta(ms);
            rell.test.block().run();
        }

        function assert_conserved() {
            assert_equals(main.cash_in_circulation(), main.registered_accounts() * main.WELCOME_CASH);
            assert_equals(main.tokens_in_circulation(), main.INITIAL_TOKEN_SUPPLY);
        }

        // EXPLOIT MUST FAIL. Round 1, step 1: the fair price is 2.00 and the oracle
        // momentarily posts 0.000001. Here the post is refused - and so is a legal
        // move that comes too soon, and any post from a key that is not the oracle.
        function test_round1_price_crash_must_fail() {
            val alice = register_alice();
            signed(alice.keypair, main.register_account());
            post_price(2 * main.PRICE_SCALE);

            post_price_must_fail(1, "price move exceeds bound");
            post_price_must_fail(main.PRICE_SCALE * 8 / 5, "price update too soon");
            rell.test.tx().op(main.set_price(2 * main.PRICE_SCALE)).sign(alice.keypair).run_must_fail("oracle only");
            assert_equals(main.get_token_price(), 2 * main.PRICE_SCALE);

            // The widest legal move, after the interval, lands - 20% down, not 99.99995%.
            after(main.MIN_PRICE_UPDATE_INTERVAL_MS);
            post_price(main.PRICE_SCALE * 8 / 5);
            assert_equals(main.get_token_price(), 1600000);
        }

        // EXPLOIT MUST FAIL. Round 1, step 2: buy at one price, sell everything at a
        // higher one for more cash than was ever deposited. Here the sale the vault
        // cannot cover aborts, what it can cover moves, and the totals never change.
        function test_round1_unbacked_sell_must_fail() {
            val alice = register_alice();
            signed(alice.keypair, main.register_account());
            assert_conserved();
            post_price(main.PRICE_SCALE);

            signed(alice.keypair, main.buy_tokens(1000));
            assert_equals(main.get_token_balance(alice.account.id), 1000);
            assert_equals(main.get_cash_balance(alice.account.id), 0);
            assert_equals(main.get_vault_cash(), 1000);
            assert_conserved();

            after(main.MIN_PRICE_UPDATE_INTERVAL_MS);
            post_price(main.PRICE_SCALE * 12 / 10);
            // 1000 tokens at 1.20 would be 1200 cash; the vault holds 1000.
            signed_must_fail(alice.keypair, main.sell_tokens(1000), "vault cannot cover the trade");
            assert_equals(main.get_cash_balance(alice.account.id), 0);
            assert_conserved();

            // What the vault holds is what can leave it.
            signed(alice.keypair, main.sell_tokens(800));
            assert_equals(main.get_cash_balance(alice.account.id), 960);
            assert_equals(main.get_token_balance(alice.account.id), 200);
            assert_equals(main.get_vault_cash(), 40);
            assert_conserved();
        }

        // TIME-CHECKED: no post, no trade; an old post, no trade; a fresh post
        // reopens trading.
        function test_stale_or_missing_price_halts_trading() {
            val alice = register_alice();
            signed(alice.keypair, main.register_account());
            signed_must_fail(alice.keypair, main.buy_tokens(10), "price feed not initialised");

            post_price(main.PRICE_SCALE);
            signed(alice.keypair, main.buy_tokens(10));
            assert_equals(main.get_token_balance(alice.account.id), 10);

            after(main.MAX_PRICE_AGE_MS + 1);
            signed_must_fail(alice.keypair, main.buy_tokens(10), "price feed is stale");

            post_price(main.PRICE_SCALE);
            signed(alice.keypair, main.buy_tokens(10));
            assert_equals(main.get_token_balance(alice.account.id), 20);
            assert_conserved();
        }
    """.trimIndent() + "\n"

    // ---- staking template: rewards are released from a sponsor-funded pool, never computed from a rate ----
    //
    // Adversary round 4 (exploit-corpus/realworld/adversary-round4/dapp_c_staking V8,
    // corpus row r4-staking-unbacked-mint) drained a staking dapp the gate certified
    // with zero findings: claim_rewards credited `staked * elapsed * REWARD_PER_SECOND`
    // straight into the balance with no pool debit, so a staker who simply waited
    // minted from an empty pool. No static rule can see that - a rate constant times
    // elapsed time is ordinary arithmetic - so this template makes the formula
    // unwritable: there is no reward expression at all, only a RELEASE from
    // pool.undistributed (funded by sponsors, debited in the same operation) that the
    // clock can never make larger than what the pool holds, and a per-share
    // accumulator that splits what was released among the stakers of the moment.

    private fun stakingMainRell(): String = """
        module;

        import lib.ft4.auth;
        import lib.ft4.accounts;

        // Staking template: stake points, earn a share of a reward pool, unstake
        // through a cooldown. Four guards are STRUCTURAL - they live in the pool
        // object and the settlement helpers, not in a require() a future operation
        // can forget:
        //   SPONSOR-FUNDED  - pool.undistributed has exactly one inflow: fund_rewards,
        //                     which debits the sponsor's own balance in the same
        //                     operation. There is no rate that creates points.
        //   RELEASE-CAPPED  - update_pool releases min(elapsed * REWARD_PER_SECOND,
        //                     pool.undistributed). The clock decides WHEN, the sponsors
        //                     decided HOW MUCH: an empty pool releases nothing however
        //                     long a staker waits, so the round-4 mint (stake * elapsed
        //                     * rate credited from nothing) has no line to live on.
        //   PAIRED CREDIT   - claim_rewards debits pool.unclaimed and credits the
        //                     member in the same operation, and refuses a claim the
        //                     pool cannot cover. Every credit in this file is a debit
        //                     of a real row or pool field in the same operation.
        //   COOLDOWN        - unstaked points stop earning at once and leave only
        //                     after COOLDOWN_MS, so nobody can stake for one block,
        //                     take the reward and run.
        // The per-share accumulator (acc_reward_per_share, big_integer) splits each
        // release across the stakers of that moment, so claiming often, staking late
        // or a large stake cannot inflate a claim. src/test/main_test.rell replays the
        // round-4 mint and REQUIRES it to fail, and asserts after every step that
        // points in circulation equal the welcome grants: rewards are sponsor points
        // moved, never created. Keep those tests passing as this file grows.
        // What no template can fix: REWARD_PER_SECOND and COOLDOWN_MS are your
        // economics - a rate the sponsors cannot keep funded simply stops paying.

        entity member {
            key owner: byte_array;
            mutable balance: integer = 0;       // spendable points
            mutable staked: integer = 0;        // points locked in the pool, earning
            // The accumulator value at the member's last settlement: the pending
            // reward is staked * (pool.acc_reward_per_share - reward_snapshot) / ACC_SCALE.
            mutable reward_snapshot: big_integer = 0L;
            mutable pending_reward: integer = 0;
        }

        // One cooldown per member at a time: the key refuses a second request.
        entity unstake_request {
            key member;
            amount: integer;
            ready_at: timestamp;
        }

        object pool {
            mutable total_staked: integer = 0;
            // Sponsored, not yet released to any staker. The ONLY source of rewards.
            mutable undistributed: integer = 0;
            // Released to stakers by the accumulator but not yet claimed. Rounding
            // dust stays here; it is never paid twice.
            mutable unclaimed: integer = 0;
            mutable acc_reward_per_share: big_integer = 0L;
            mutable last_update: timestamp = 0;
        }

        // The one-time welcome grant is the ONLY place points are created (a
        // stand-in for a real deposit - replace with an FT4 asset transfer and keep
        // the same discipline: every credit is debited from somewhere real).
        val WELCOME_POINTS = 1000;
        // Points released from the pool to all stakers per second - while the pool
        // has them. This is a release schedule, not a mint: see update_pool.
        val REWARD_PER_SECOND = 1;
        // One day. A constant: there is no parameter that shortens it.
        val COOLDOWN_MS = 24 * 60 * 60 * 1000;
        // Bound every stake BEFORE the accumulator multiplies it (i64 overflow aborts).
        val MAX_STAKE = 1000000000;
        val ACC_SCALE = 1000000000000L;

        // DEFAULT: every operation requires the Transfer flag. FT4 resolves flags
        // with contains_all(), and contains_all([]) is always true - never weaken
        // this default; grant flags = [] only per operation, scoped, for
        // operations that cannot move value.
        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            flags = ["T"]
        );

        function member_of(owner: byte_array): member =
            require(member @? { .owner == owner }, "register as a member first");

        // Release what the clock has earned since the last update - out of
        // pool.undistributed and into the accumulator. Never more than the pool
        // holds, nothing while nobody is staked, and the clock is consumed even
        // when nothing is released so a later sponsor cannot fund the past.
        function update_pool() {
            val now = op_context.last_block_time;
            if (pool.last_update == 0) {
                pool.last_update = now;
                return;
            }
            val elapsed_ms = now - pool.last_update;
            if (elapsed_ms <= 0) return;
            pool.last_update = now;
            if (pool.total_staked == 0 or pool.undistributed == 0) return;
            // THE guard against the round-4 mint: the schedule is capped by the
            // pool. Delete the min() and a staker mints from nothing again.
            val earned = min(pool.undistributed, elapsed_ms / 1000 * REWARD_PER_SECOND);
            if (earned <= 0) return;
            pool.undistributed -= earned;
            pool.unclaimed += earned;
            pool.acc_reward_per_share += earned.to_big_integer() * ACC_SCALE / pool.total_staked.to_big_integer();
        }

        // Settle a member against the accumulator before its stake changes.
        function settle(m: member) {
            update_pool();
            val owed = (m.staked.to_big_integer() * (pool.acc_reward_per_share - m.reward_snapshot) / ACC_SCALE).to_integer();
            update m ( .pending_reward += owed, .reward_snapshot = pool.acc_reward_per_share );
        }

        operation register_member() {
            val account = auth.authenticate();
            require(member @? { .owner == account.id } == null, "already a member");
            create member(owner = account.id, balance = WELCOME_POINTS);
        }

        operation transfer_points(to: byte_array, amount: integer) {
            // 1. AUTHENTICATE  2. AUTHORIZE - spend only from the CALLER's row.
            val account = auth.authenticate();
            val from = member_of(account.id);
            // 3. VALIDATE - each input separately.
            require(to != account.id, "cannot transfer to yourself");
            val recipient = member_of(to);
            require(amount > 0, "amount must be positive");
            require(from.balance >= amount, "insufficient balance");
            // 4. INVARIANTS - the same amount leaves one row and lands in another.
            update from ( .balance -= amount );
            update recipient ( .balance += amount );
        }

        // Sponsor the reward pool from your own balance. This is the pool's only
        // inflow, and it is paid for in the same operation.
        operation fund_rewards(amount: integer) {
            val account = auth.authenticate();
            val m = member_of(account.id);
            require(amount > 0, "amount must be positive");
            require(m.balance >= amount, "insufficient balance");
            // Consume the clock first so the new funding is released from now on,
            // not retroactively over the time before it arrived.
            update_pool();
            update m ( .balance -= amount );
            pool.undistributed += amount;
        }

        operation stake(amount: integer) {
            val account = auth.authenticate();
            val m = member_of(account.id);
            require(amount > 0, "amount must be positive");
            require(amount <= MAX_STAKE, "amount too large");
            require(m.balance >= amount, "insufficient balance");
            require(m.staked + amount <= MAX_STAKE, "stake cap exceeded");
            // Settle BEFORE the stake grows: the new stake starts at the current
            // accumulator and earns nothing for the past.
            settle(m);
            update m ( .balance -= amount, .staked += amount );
            pool.total_staked += amount;
        }

        // Start the cooldown for part of the stake. The amount stops earning now
        // and leaves the stake; it can be withdrawn after COOLDOWN_MS.
        operation request_unstake(amount: integer) {
            val account = auth.authenticate();
            val m = member_of(account.id);
            require(amount > 0, "amount must be positive");
            require(m.staked >= amount, "insufficient stake");
            require(unstake_request @? { .member == m } == null, "an unstake is already pending");
            settle(m);
            update m ( .staked -= amount );
            pool.total_staked -= amount;
            create unstake_request(member = m, amount = amount, ready_at = op_context.last_block_time + COOLDOWN_MS);
        }

        operation withdraw_unstaked() {
            val account = auth.authenticate();
            val m = member_of(account.id);
            val r = require(unstake_request @? { .member == m }, "no pending unstake");
            require(op_context.last_block_time >= r.ready_at, "cooldown not over");
            val amount = r.amount;
            // The cooling row is the debit; deleting it and crediting the balance
            // happen in the same operation.
            delete r;
            update m ( .balance += amount );
        }

        operation claim_rewards() {
            val account = auth.authenticate();
            val m = member_of(account.id);
            settle(m);
            val reward = m.pending_reward;
            require(reward > 0, "nothing to claim");
            // Paid out of what the accumulator released, never more than the pool
            // holds: the credit below cannot exist without this debit.
            require(pool.unclaimed >= reward, "pool cannot cover the claim");
            pool.unclaimed -= reward;
            update m ( .pending_reward = 0, .balance += reward );
        }

        query get_balance(owner: byte_array): integer {
            val m = member @? { .owner == owner };
            return if (m != null) m.balance else 0;
        }

        query get_staked(owner: byte_array): integer {
            val m = member @? { .owner == owner };
            return if (m != null) m.staked else 0;
        }

        // What the accumulator will be once the next operation settles the clock
        // up to the latest block. Read-only: the same arithmetic as update_pool.
        function projected_acc(): big_integer {
            val last_block = block @? {} ( @max .timestamp );
            if (last_block == null or pool.last_update == 0) return pool.acc_reward_per_share;
            val elapsed_ms = last_block - pool.last_update;
            if (elapsed_ms <= 0 or pool.total_staked == 0 or pool.undistributed == 0) return pool.acc_reward_per_share;
            val earned = min(pool.undistributed, elapsed_ms / 1000 * REWARD_PER_SECOND);
            if (earned <= 0) return pool.acc_reward_per_share;
            return pool.acc_reward_per_share + earned.to_big_integer() * ACC_SCALE / pool.total_staked.to_big_integer();
        }

        // Pending reward as of the latest block (read-only: does not settle).
        query get_pending_reward(owner: byte_array): integer {
            val m = member @? { .owner == owner };
            if (m == null) return 0;
            val owed = (m.staked.to_big_integer() * (projected_acc() - m.reward_snapshot) / ACC_SCALE).to_integer();
            return m.pending_reward + owed;
        }

        query get_unstake_request(owner: byte_array) {
            val r = unstake_request @? { .member.owner == owner };
            return if (r != null) (amount = r.amount, ready_at = r.ready_at) else null;
        }

        query pool_state() = (
            total_staked = pool.total_staked,
            undistributed = pool.undistributed,
            unclaimed = pool.unclaimed,
            last_update = pool.last_update
        );

        query member_count(): integer = member @* {} ( .owner ).size();

        // INVARIANT: every point in circulation came from a welcome grant. Points
        // are spendable, staked, cooling, or in the pool (undistributed or released
        // but unclaimed); staking, funding, releasing and claiming move them, they
        // never create or destroy them. The shipped tests compare this to
        // member_count() * WELCOME_POINTS after every step.
        query points_in_circulation(): integer {
            var total = pool.undistributed + pool.unclaimed;
            for (m in member @* {} ( .balance, .staked )) total += m.balance + m.staked;
            for (a in unstake_request @* {} ( .amount )) total += a;
            return total;
        }

        // INVARIANT: the stake ledger and the pool's total agree.
        query staked_points(): integer {
            var total = 0;
            for (s in member @* {} ( .staked )) total += s;
            return total;
        }
    """.trimIndent() + "\n"

    private fun stakingTestRell(): String = """
        @test module;

        // The staking template's invariant tests. They are real: FT4 test accounts,
        // signed operations, PostgreSQL - run via run_rell_tests (pass chromia.yml's
        // moduleArgs PLUS its test.moduleArgs block) or `chr test`.
        //
        // test_round4_unbacked_reward_must_fail replays the adversary's staking
        // drain against this template and REQUIRES it to be refused: nobody funds
        // the pool, a staker waits a year, and the claim that minted 31 million
        // points in round 4 must find nothing to claim here. It can only pass while
        // the release cap stands, so deleting it goes red before an attacker finds
        // out. test_rewards_come_only_from_sponsor_funding is the conservation
        // proof: what stakers receive plus the dust left in the pool is exactly
        // what sponsors paid in, and points in circulation never change.
        // Test blocks are DEFAULT_BLOCK_INTERVAL (10 s) apart, so amounts that
        // depend on how many blocks a setup took are asserted as bounds and the
        // totals exactly.

        import main;
        import lib.ft4.test.core.{ register_alice, register_bob, register_trudy, ft_auth_operation_for };

        function signed(keypair: rell.test.keypair, op: rell.test.op) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run();
        }

        function signed_must_fail(keypair: rell.test.keypair, op: rell.test.op, expected: text) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run_must_fail(expected);
        }

        // Stamp the next block `ms` after the last one.
        function after(ms: integer) {
            rell.test.set_next_block_time_delta(ms);
            rell.test.block().run();
        }

        val ONE_YEAR_MS = 365 * 24 * 60 * 60 * 1000;

        function assert_conserved() {
            assert_equals(main.points_in_circulation(), main.member_count() * main.WELCOME_POINTS);
            assert_equals(main.staked_points(), main.pool_state().total_staked);
        }

        function reward_of(owner: byte_array, staked: integer): integer =
            main.get_balance(owner) - (main.WELCOME_POINTS - staked);

        // EXPLOIT MUST FAIL. Round 4: nobody funds the pool, alice stakes everything
        // and waits a year. The adversary's claim_rewards credited
        // stake * seconds * REWARD_PER_SECOND from nothing; here the pool released
        // nothing, so there is nothing to claim and nothing was created. Then a
        // sponsor funds 50 and a second year passes: exactly 50 is claimable, and
        // not a point more.
        function test_round4_unbacked_reward_must_fail() {
            val alice = register_alice();
            signed(alice.keypair, main.register_member());
            assert_equals(main.pool_state().undistributed, 0);
            signed(alice.keypair, main.stake(main.WELCOME_POINTS));
            assert_conserved();

            after(ONE_YEAR_MS);
            // The exploit step: in round 4 this claim credited 31,536,000 points.
            signed_must_fail(alice.keypair, main.claim_rewards(), "nothing to claim");
            assert_equals(main.get_balance(alice.account.id), 0);
            assert_equals(main.get_pending_reward(alice.account.id), 0);
            assert_equals(main.pool_state().unclaimed, 0);
            assert_conserved();

            val bob = register_bob();
            signed(bob.keypair, main.register_member());
            signed(bob.keypair, main.fund_rewards(50));
            after(ONE_YEAR_MS);
            signed(alice.keypair, main.claim_rewards());
            assert_equals(main.get_balance(alice.account.id), 50);
            assert_equals(main.get_balance(bob.account.id), main.WELCOME_POINTS - 50);
            assert_equals(main.pool_state().undistributed, 0);
            assert_equals(main.pool_state().unclaimed, 0);
            after(ONE_YEAR_MS);
            signed_must_fail(alice.keypair, main.claim_rewards(), "nothing to claim");
            assert_conserved();
        }

        // CONSERVATION: total value = welcome grants; rewards are sponsor points
        // moved. trudy sponsors 300; alice and bob stake 600 / 200; after an hour the
        // pool is out and what they received plus the dust left unclaimed is exactly
        // 300 - and another hour releases nothing, because the clock alone creates
        // nothing.
        function test_rewards_come_only_from_sponsor_funding() {
            val alice = register_alice();
            val bob = register_bob();
            val trudy = register_trudy();
            signed(alice.keypair, main.register_member());
            signed(bob.keypair, main.register_member());
            signed(trudy.keypair, main.register_member());
            signed(trudy.keypair, main.fund_rewards(300));
            assert_equals(main.pool_state().undistributed, 300);
            assert_conserved();

            signed(alice.keypair, main.stake(600));
            signed(bob.keypair, main.stake(200));
            assert_conserved();

            after(60 * 60 * 1000);
            signed(alice.keypair, main.claim_rewards());
            signed(bob.keypair, main.claim_rewards());
            assert_equals(main.pool_state().undistributed, 0);
            val alice_reward = reward_of(alice.account.id, 600);
            val bob_reward = reward_of(bob.account.id, 200);
            // alice staked a block before bob and earned those seconds alone, so she
            // gets at least her 3/4 share; together they got the 300 sponsored, less
            // rounding dust that stays in the pool and is never paid to anyone.
            assert_true(alice_reward >= 225 and bob_reward <= 75);
            assert_true(alice_reward + bob_reward >= 298);
            assert_equals(alice_reward + bob_reward + main.pool_state().unclaimed, 300);
            assert_equals(main.get_balance(trudy.account.id), main.WELCOME_POINTS - 300);
            assert_conserved();

            after(60 * 60 * 1000);
            signed_must_fail(alice.keypair, main.claim_rewards(), "nothing to claim");
            signed_must_fail(bob.keypair, main.claim_rewards(), "nothing to claim");
            assert_conserved();
        }

        // ACCUMULATOR + COOLDOWN: staking just before a claim earns nothing for the
        // past; unstaked points stop earning at once and only leave after the
        // cooldown - withdrawing early must fail.
        function test_late_staker_earns_nothing_for_the_past_and_cooldown_holds() {
            val alice = register_alice();
            val bob = register_bob();
            signed(alice.keypair, main.register_member());
            signed(bob.keypair, main.register_member());
            signed(bob.keypair, main.fund_rewards(500));
            signed(alice.keypair, main.stake(100));
            after(50 * 1000);
            // bob stakes after ~50 seconds of accrual: none of it is his. (An
            // operation sees the PREVIOUS block's time, so a new stake is credited
            // from one block interval before its inclusion - 10 s in tests, at most
            // 8 of the 400/500 share here - never the 50 s before it.)
            signed(bob.keypair, main.stake(400));
            assert_true(main.get_pending_reward(bob.account.id) <= 10);
            assert_true(main.get_pending_reward(alice.account.id) >= 50);
            signed(alice.keypair, main.claim_rewards());
            assert_true(main.get_balance(alice.account.id) >= main.WELCOME_POINTS - 100 + 50);
            assert_conserved();

            // alice starts a cooldown for all of it and settles what was pending;
            // from here on she earns nothing, and cannot withdraw early.
            signed_must_fail(alice.keypair, main.request_unstake(101), "insufficient stake");
            signed(alice.keypair, main.request_unstake(100));
            signed_must_fail(alice.keypair, main.request_unstake(1), "insufficient stake");
            signed_must_fail(alice.keypair, main.withdraw_unstaked(), "cooldown not over");
            val pending = main.get_pending_reward(alice.account.id);
            if (pending > 0) signed(alice.keypair, main.claim_rewards());
            val settled = main.get_balance(alice.account.id);
            after(50 * 1000);
            assert_equals(main.get_pending_reward(alice.account.id), 0);
            signed_must_fail(alice.keypair, main.claim_rewards(), "nothing to claim");
            assert_true(main.get_pending_reward(bob.account.id) >= 50);
            assert_conserved();

            after(main.COOLDOWN_MS);
            signed(alice.keypair, main.withdraw_unstaked());
            signed_must_fail(alice.keypair, main.withdraw_unstaked(), "no pending unstake");
            assert_equals(main.get_balance(alice.account.id), settled + 100);
            assert_equals(main.get_staked(alice.account.id), 0);
            assert_conserved();
        }

        // INPUT BOUNDS + OWNERSHIP: negative or oversized amounts abort, a
        // non-member cannot stake, and transfers move only spendable points.
        function test_bounds_and_ownership() {
            val alice = register_alice();
            val bob = register_bob();
            signed(alice.keypair, main.register_member());
            signed_must_fail(bob.keypair, main.stake(1), "register as a member first");
            signed_must_fail(alice.keypair, main.stake(-1), "amount must be positive");
            signed_must_fail(alice.keypair, main.stake(main.WELCOME_POINTS + 1), "insufficient balance");
            signed_must_fail(alice.keypair, main.fund_rewards(0), "amount must be positive");
            signed(bob.keypair, main.register_member());
            signed_must_fail(alice.keypair, main.transfer_points(alice.account.id, 1), "cannot transfer to yourself");
            signed_must_fail(alice.keypair, main.transfer_points(bob.account.id, main.WELCOME_POINTS + 1), "insufficient balance");
            signed(alice.keypair, main.stake(main.WELCOME_POINTS));
            signed_must_fail(alice.keypair, main.transfer_points(bob.account.id, 1), "insufficient balance");
            assert_conserved();
        }
    """.trimIndent() + "\n"

    // ---- marketplace template: the price a buy names is the price it pays; offers are escrowed ----
    //
    // Adversary round 5 (exploit-corpus/realworld/adversary-round5/dapp_b_marketplace)
    // drained a hand-built marketplace the gate certified with zero findings, twice:
    //   * corpus row r5-listing-price-race-under-max - buy(listing, max_price) took a
    //     caller-supplied CEILING while the seller could reprice at will, so the seller
    //     front-ran the pending buy up to the ceiling and took the buffer (100 listed,
    //     300 paid). That row stays a GAP on purpose: "a caller-supplied bound is
    //     compared and a counterparty can move the bounded value" is the shape of every
    //     orderbook, so a rule for it fires on all of them.
    //   * royalty bypass, two ways - transfer_nft + transfer_points as a pair, or a
    //     listing at 1 with the rest paid off-book. A gift path exists in every real
    //     marketplace, so this is not ruleable either.
    // Both are DESIGN holes, so the answer is a template (north-star principle 4): an
    // exact-price buy and an immutable listing make the sandwich unwritable, and the
    // royalty limitation is stated as a limitation instead of being faked.

    private fun marketplaceMainRell(): String = """
        module;

        import lib.ft4.auth;
        import lib.ft4.accounts;

        // Marketplace template: list an NFT, buy it at the price you were shown, or
        // bid with escrowed points. Four guards are STRUCTURAL - they live in the
        // entity declarations and the single settlement helper, not in a require()
        // a future operation can forget:
        //   EXACT PRICE       - buy_nft names the price it agreed to and aborts unless
        //                       the listing is still at exactly that price. A
        //                       caller-supplied CEILING (the max_price / slippage
        //                       shape) is a sandwich: the seller front-runs the pending
        //                       buy up to the ceiling and pockets the buffer - adversary
        //                       round 5 took 200 out of a 100-point listing that way.
        //                       An equality can still be raced, but racing it only
        //                       ABORTS the buy; it can never move what the buyer pays.
        //   IMMUTABLE LISTING - `listing.price` and `listing.seller` are not mutable and
        //                       there is no update_listing_price operation. REPRICING IS
        //                       cancel_listing + list_nft, chosen over a timelock because
        //                       a timelock leaves the mutable field (and therefore the
        //                       race) in place and only narrows the window: here a seller
        //                       who reprices destroys their own listing, so a buy that
        //                       lands after it aborts with "not listed" instead of paying
        //                       more. Adding a mutable price is what re-creates the
        //                       sandwich - it is the one edit to this file to refuse.
        //   ESCROWED OFFERS   - make_offer debits the bidder NOW and the points live in
        //                       the offer row until it settles, is cancelled, or expires.
        //                       accept_offer names the amount it agreed to (the same
        //                       guard in the other direction - a bidder can cancel and
        //                       re-offer lower, so the seller must name what it accepts),
        //                       deletes the escrow row and pays it out in the same
        //                       operation, and asserts proceeds + royalty == amount.
        //   TIMED AUCTION     - an ascending auction with NO mutable bid field. The
        //                       standing bid IS the escrow row, keyed by the auction and
        //                       immutable, so raising a bid is delete-and-recreate and the
        //                       refund happens in the same operation as the new debit.
        //                       The auction row carries only immutable terms (seller,
        //                       reserve, deadline), so the seller has nothing to move
        //                       under a bid that already stands; settle_auction is
        //                       PERMISSIONLESS once the deadline passes, so a seller who
        //                       walks away cannot hold the escrow hostage. A mutable
        //                       `highest_bid` is the round-5 sandwich wearing an auction's
        //                       clothes - that is the one edit to this file to refuse.
        //   PAIRED SETTLEMENT - settle_sale is the ONLY place a seller or a creator is
        //                       credited, it asserts the split is exact, and each of its
        //                       three callers debits exactly `price` in the same
        //                       operation: the buyer's balance in buy_nft, the escrow row
        //                       it just deleted in accept_offer, the bid row it just
        //                       deleted in settle_auction. Nothing in this file creates a
        //                       point outside the one-time welcome grant.
        //   ONE ENCUMBRANCE   - require_unencumbered is the single question every path
        //                       that MOVES a token asks (buy_nft, transfer_nft,
        //                       accept_offer, list_nft): is somebody's money already
        //                       promised to this token? Without it a plain gift walks the
        //                       token out from under an escrowed bid and strands the
        //                       escrow, and no static rule can see that - the gate stays
        //                       silent because nothing is minted.
        //
        // EXTENDING THIS TEMPLATE - the three seams a static rule cannot see, and where
        // every extension of it has gone wrong so far:
        //   1. EVERY NEW MARKET STATE MUST BE MUTUALLY EXCLUSIVE WITH THE OTHERS. A
        //      listing and an auction on the same token are two settlement paths for one
        //      item; whichever settles second finds the token gone. list_nft refuses a
        //      token in an auction and start_auction refuses a listed one - do the same
        //      for anything you add (a rental, a bundle, a swap).
        //   2. EVERY TOKEN-MOVING PATH MUST CONSULT THE SAME ENCUMBRANCE HELPER. Add the
        //      check to require_unencumbered, never a fresh require() in your new
        //      operation: the guard has to be asked by the paths that already exist, and
        //      a second copy is one somebody will forget.
        //   3. EVERY NEW ESCROW MUST BE ADDED TO THE CONSERVATION TOTAL. Points live in a
        //      balance, an offer row or a bid row; points_in_circulation sums all three
        //      and the shipped tests compare it to member_count() * WELCOME_POINTS after
        //      every step. A new row that holds points and is not summed there makes the
        //      invariant test pass while points go missing.
        //
        // ROYALTY - AN HONEST BOUNDARY, NOT A GUARD. The royalty is fixed at mint (no
        // operation changes it, so no creator can front-run a pending sale by raising
        // it), capped at MAX_ROYALTY_BPS, and taken on every sale THIS MODULE RECORDS -
        // including a 1-point listing, where it is floored at one point rather than
        // rounding away to zero. It is NOT enforced on the trade, and no template can
        // enforce it: transfer_nft gives a token away and transfer_points sends points,
        // and two willing parties can pair them off-market at any price. The module
        // cannot tell that apart from a genuine gift and an unrelated payment - which is
        // why off-marketplace sales exist on every chain that has tried this.
        // src/test/main_test.rell asserts the bypass WORKS
        // (test_round5_royalty_bypass_is_documented_not_enforced) so that nobody reading
        // this template mistakes a documented limit for a guard. If your economics
        // REQUIRE royalties, the transfer path itself has to change - no free transfer
        // at all, or a transfer that also pays the creator - and that is a product
        // decision with real costs, not something a template should decide for you.
        //
        // What no template can fix: the price is whatever two parties agree to, so
        // under-pricing and wash trades stay judgment calls; mint_nft costs nothing, so
        // token ids can be squatted until you add a fee, a cap or a name registry; and
        // MAX_ROYALTY_BPS is your economics.

        entity member {
            key owner: byte_array;
            mutable balance: integer = 0;
        }

        entity nft {
            key token_id: text;
            mutable owner: byte_array;
            // Both fixed at mint. A royalty that could be reassigned is a royalty
            // anyone could steal; a royalty that could be RAISED is one the creator
            // front-runs a pending sale with, so there is no operation that writes it.
            creator: byte_array;
            royalty_bps: integer;
        }

        // IMMUTABLE BY DECLARATION: nothing here is mutable, so there is no value a
        // counterparty can move under a buy that is already in flight. Recorded with
        // the seller so a listing can never outlive the ownership it was made under.
        entity listing {
            key nft;
            seller: byte_array;
            price: integer;
        }

        // The escrow row: these points have already left the bidder. `amount` is
        // immutable for the same reason a listing's price is - changing a bid means
        // cancel_offer + make_offer, which refunds first.
        entity offer {
            key nft, bidder: byte_array;
            amount: integer;
            expires_at: timestamp;
        }

        // IMMUTABLE BY DECLARATION, like a listing: reserve, deadline and seller are
        // fixed when the auction opens, so there is no term a seller can move under a
        // bid that already stands. Cancelling is only possible while nobody has bid.
        entity auction {
            key nft;
            seller: byte_array;
            reserve_price: integer;
            ends_at: timestamp;
        }

        // THE STANDING BID IS THE ESCROW, and it is immutable. An ascending auction
        // "needs" a mutable highest_bid; it does not, and a mutable number the
        // counterparty can move under an in-flight settlement is exactly the round-5
        // sandwich. Raising a bid means refunding this row and creating a new one -
        // the same delete-and-recreate the template already uses for repricing a
        // listing and for changing an offer.
        entity bid {
            key auction;
            bidder: byte_array;
            amount: integer;
        }

        // The one-time welcome grant is the ONLY place points are created (a stand-in
        // for a real deposit - replace it with an FT4 asset transfer and keep the same
        // discipline: every credit is debited from somewhere real).
        val WELCOME_POINTS = 1000;
        val BPS = 10000;
        // A royalty can never take the whole sale price.
        val MAX_ROYALTY_BPS = 1000;
        // Bound every price BEFORE it is multiplied by the rate (i64 overflow aborts).
        val MAX_PRICE = 1000000000;
        val MAX_OFFER_MS = 30 * 24 * 60 * 60 * 1000;
        // An auction nobody can reach is a seller bidding against themselves; one that
        // never ends holds the escrow forever. Both bounds are checked separately.
        val MIN_AUCTION_MS = 60 * 1000;
        val MAX_AUCTION_MS = 30 * 24 * 60 * 60 * 1000;
        // A minimum raise, floored at one point so a 1-point bid can still be beaten.
        val BID_INCREMENT_BPS = 500;

        // DEFAULT: every operation requires the Transfer flag. FT4 resolves flags
        // with contains_all(), and contains_all([]) is always true - never weaken
        // this default; grant flags = [] only per operation, scoped, for
        // operations that cannot move value.
        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            flags = ["T"]
        );

        function member_of(owner: byte_array): member =
            require(member @? { .owner == owner }, "register as a member first");

        function nft_of(token_id: text): nft =
            require(nft @? { .token_id == token_id }, "no such token");

        // Clear anything that referred to the OLD owner. Called by every path that
        // moves a token, so a stale listing can never sell a token its lister no
        // longer owns.
        function clear_listing(token: nft) {
            val l = listing @? { .nft == token };
            if (l != null) delete l;
        }

        // THE ENCUMBRANCE QUESTION, asked in ONE place. Every path that moves a token
        // or opens a second market state on it calls this; when you add a path, add
        // the call here rather than a fresh require() of your own. An escrowed bid has
        // no owner who can take it back - only settle_auction can pay it out - so a
        // token that leaves while a bid stands strands somebody's points forever, and
        // nothing is minted, so no static rule will tell you.
        function require_unencumbered(token: nft) {
            require(auction @? { .nft == token } == null, "token is in an auction");
        }

        function min_next_bid(standing: integer): integer {
            val step = standing * BID_INCREMENT_BPS / BPS;
            return standing + (if (step > 0) step else 1);
        }

        // Floored at one point when a royalty is owed at all: the round-5 "list at 1,
        // take the rest off-book" trade paid the creator ZERO because the cut
        // floor-divided away. This does not stop the bypass (see the header) - it stops
        // the RECORDED price from paying nothing.
        function royalty_for(token: nft, price: integer): integer {
            if (token.royalty_bps == 0) return 0;
            val exact = price * token.royalty_bps / BPS;
            val royalty = if (exact > 0) exact else 1;
            require(royalty <= price, "royalty cannot exceed the price");
            return royalty;
        }

        // THE settlement path: the only place in this file that credits a seller or a
        // creator. Its caller must have debited exactly `price` in the same operation.
        // The assertion below is the conservation proof for the split itself - delete
        // it and rounding could quietly create or destroy points.
        function settle_sale(seller_id: byte_array, token: nft, price: integer) {
            val royalty = royalty_for(token, price);
            val proceeds = price - royalty;
            require(royalty >= 0 and proceeds >= 0, "bad split");
            require(royalty + proceeds == price, "the split must pay out exactly the price");
            val seller = member_of(seller_id);
            update seller ( .balance += proceeds );
            if (royalty > 0) {
                val creator = member_of(token.creator);
                update creator ( .balance += royalty );
            }
        }

        operation register_member() {
            val account = auth.authenticate();
            require(member @? { .owner == account.id } == null, "already a member");
            create member(owner = account.id, balance = WELCOME_POINTS);
        }

        operation mint_nft(token_id: text, royalty_bps: integer) {
            // 1. AUTHENTICATE  2. AUTHORIZE  3. VALIDATE every input separately.
            val account = auth.authenticate();
            member_of(account.id);
            require(token_id.matches("^[a-z0-9_]{1,64}${'$'}"), "invalid token id");
            require(nft @? { .token_id == token_id } == null, "token already exists");
            require(royalty_bps >= 0, "royalty must not be negative");
            require(royalty_bps <= MAX_ROYALTY_BPS, "royalty too high");
            create nft(token_id = token_id, owner = account.id, creator = account.id, royalty_bps = royalty_bps);
        }

        operation list_nft(token_id: text, price: integer) {
            val account = auth.authenticate();
            member_of(account.id);
            val token = nft_of(token_id);
            // AUTHORIZE: only the owner of THIS token, never a caller-named seller.
            require(token.owner == account.id, "not the owner");
            // MUTUALLY EXCLUSIVE MARKET STATES: a listing and an auction are two
            // settlement paths for one token, and whichever settles second finds it gone.
            require_unencumbered(token);
            require(price > 0, "price must be positive");
            require(price <= MAX_PRICE, "price too high");
            require(listing @? { .nft == token } == null, "already listed");
            create listing(nft = token, seller = account.id, price = price);
        }

        // Repricing is cancel + list: there is deliberately no operation that edits a
        // live listing. See the header - a mutable price is the sandwich.
        operation cancel_listing(token_id: text) {
            val account = auth.authenticate();
            val token = nft_of(token_id);
            val l = require(listing @? { .nft == token }, "not listed");
            require(l.seller == account.id, "not the seller");
            delete l;
        }

        // EXACT PRICE, not a ceiling: the buyer names the price they were shown, and a
        // seller who moves it can only make this abort. Deleting the equality below is
        // exactly the round-5 sandwich.
        operation buy_nft(token_id: text, expected_price: integer) {
            val account = auth.authenticate();
            val buyer = member_of(account.id);
            val token = nft_of(token_id);
            val l = require(listing @? { .nft == token }, "not listed");
            require(l.seller != account.id, "cannot buy your own listing");
            // A listing is only good while its lister still owns the token.
            require(token.owner == l.seller, "listing is stale");
            require_unencumbered(token);
            require(expected_price > 0, "expected_price must be positive");
            require(l.price == expected_price, "listing price changed - buy at the price you were shown");
            require(buyer.balance >= l.price, "insufficient balance");
            val price = l.price;
            val seller_id = l.seller;
            // The listing is consumed exactly once, and the buyer's debit is the
            // money settle_sale pays out.
            delete l;
            update buyer ( .balance -= price );
            settle_sale(seller_id, token, price);
            update token ( .owner = account.id );
        }

        // A plain gift. It moves no points, so no royalty is due - and it clears the
        // listing so the token cannot be sold out from under its new owner. This
        // operation is half of the documented royalty bypass; see the header.
        operation transfer_nft(token_id: text, to: byte_array) {
            val account = auth.authenticate();
            val token = nft_of(token_id);
            require(token.owner == account.id, "not the owner");
            // A gift is still a token move: without this, transfer_nft walks the token
            // out from under an escrowed bid and strands it.
            require_unencumbered(token);
            require(to != account.id, "cannot transfer to yourself");
            member_of(to);
            clear_listing(token);
            update token ( .owner = to );
        }

        operation transfer_points(to: byte_array, amount: integer) {
            val account = auth.authenticate();
            val from = member_of(account.id);
            require(to != account.id, "cannot transfer to yourself");
            val recipient = member_of(to);
            require(amount > 0, "amount must be positive");
            require(from.balance >= amount, "insufficient balance");
            // The same amount leaves one row and lands in another.
            update from ( .balance -= amount );
            update recipient ( .balance += amount );
        }

        // Escrowed bid: the points leave the bidder NOW, so an accepted offer can
        // never bounce and the bidder cannot spend them twice while it is open.
        operation make_offer(token_id: text, amount: integer, valid_ms: integer) {
            val account = auth.authenticate();
            val bidder = member_of(account.id);
            val token = nft_of(token_id);
            require(token.owner != account.id, "cannot bid on your own token");
            require(amount > 0, "amount must be positive");
            require(amount <= MAX_PRICE, "amount too high");
            require(valid_ms > 0, "validity must be positive");
            require(valid_ms <= MAX_OFFER_MS, "validity too long");
            require(bidder.balance >= amount, "insufficient balance");
            require(offer @? { .nft == token, .bidder == account.id } == null, "offer already open");
            update bidder ( .balance -= amount );
            create offer(
                nft = token,
                bidder = account.id,
                amount = amount,
                expires_at = op_context.last_block_time + valid_ms
            );
        }

        // Only the bidder can take their own escrow back - at any time, expired or
        // not, so escrowed points can never be stranded.
        operation cancel_offer(token_id: text) {
            val account = auth.authenticate();
            val bidder = member_of(account.id);
            val token = nft_of(token_id);
            val o = require(offer @? { .nft == token, .bidder == account.id }, "no open offer");
            val amount = o.amount;
            // The escrow row is the debit: deleting it and crediting the bidder
            // happen in the same operation.
            delete o;
            update bidder ( .balance += amount );
        }

        // The owner accepts an escrowed bid, naming the amount it agreed to: a bidder
        // can cancel and re-offer lower, which is the sandwich pointed the other way.
        // The escrow row is destroyed exactly once and what it held is paid out
        // exactly once, in this operation.
        operation accept_offer(token_id: text, bidder: byte_array, expected_amount: integer) {
            val account = auth.authenticate();
            member_of(account.id);
            val token = nft_of(token_id);
            require(token.owner == account.id, "not the owner");
            require_unencumbered(token);
            require(bidder != account.id, "cannot accept your own offer");
            val o = require(offer @? { .nft == token, .bidder == bidder }, "no such offer");
            require(expected_amount > 0, "expected_amount must be positive");
            require(o.amount == expected_amount, "offer amount changed - accept the amount you were shown");
            require(op_context.last_block_time < o.expires_at, "offer expired");
            val amount = o.amount;
            delete o;
            clear_listing(token);
            settle_sale(account.id, token, amount);
            update token ( .owner = bidder );
        }

        // Opens an auction on a token the caller owns. Every term is fixed here and
        // there is deliberately no operation that edits one - the seller's only move
        // once a bid stands is to wait for the deadline.
        operation start_auction(token_id: text, reserve_price: integer, duration_ms: integer) {
            val account = auth.authenticate();
            member_of(account.id);
            val token = nft_of(token_id);
            require(token.owner == account.id, "not the owner");
            require_unencumbered(token);
            require(listing @? { .nft == token } == null, "token is listed");
            require(reserve_price > 0, "reserve must be positive");
            require(reserve_price <= MAX_PRICE, "reserve too high");
            require(duration_ms >= MIN_AUCTION_MS, "auction too short");
            require(duration_ms <= MAX_AUCTION_MS, "auction too long");
            create auction(
                nft = token,
                seller = account.id,
                reserve_price = reserve_price,
                ends_at = op_context.last_block_time + duration_ms
            );
        }

        // The escrow leaves the bidder NOW. The previous standing bid is deleted and
        // paid back in this same operation, so exactly one bid is ever held and the
        // outbid bidder never has to come and ask for their points back.
        operation place_bid(token_id: text, amount: integer) {
            val account = auth.authenticate();
            val bidder = member_of(account.id);
            val token = nft_of(token_id);
            val a = require(auction @? { .nft == token }, "no auction");
            require(a.seller != account.id, "cannot bid on your own auction");
            require(token.owner == a.seller, "auction is stale");
            require(op_context.last_block_time < a.ends_at, "auction has ended");
            require(amount > 0, "amount must be positive");
            require(amount <= MAX_PRICE, "amount too high");
            require(amount >= a.reserve_price, "bid below reserve");
            require(bidder.balance >= amount, "insufficient balance");
            val standing = bid @? { .auction == a };
            if (standing != null) {
                require(standing.bidder != account.id, "you already hold the standing bid");
                require(amount >= min_next_bid(standing.amount), "bid does not beat the standing bid");
                val refund_to = standing.bidder;
                val refund = standing.amount;
                // The row that held the points is destroyed in the operation that
                // pays them back, so it can never pay twice.
                delete standing;
                val previous = member_of(refund_to);
                update previous ( .balance += refund );
            }
            update bidder ( .balance -= amount );
            create bid(auction = a, bidder = account.id, amount = amount);
        }

        // Only while nobody has bid: once points are escrowed the seller is committed.
        operation cancel_auction(token_id: text) {
            val account = auth.authenticate();
            val token = nft_of(token_id);
            val a = require(auction @? { .nft == token }, "no auction");
            require(a.seller == account.id, "not the seller");
            require(bid @? { .auction == a } == null, "auction has a bid");
            delete a;
        }

        // PERMISSIONLESS once the deadline has passed: the outcome is already fixed by
        // then, so who calls it cannot change it - and a seller who dislikes the price
        // cannot strand the winner's escrow by refusing to show up.
        operation settle_auction(token_id: text) {
            auth.authenticate();
            val token = nft_of(token_id);
            val a = require(auction @? { .nft == token }, "no auction");
            require(op_context.last_block_time >= a.ends_at, "auction has not ended");
            val winning = bid @? { .auction == a };
            val seller_id = a.seller;
            if (winning == null) {
                delete a;
                return;
            }
            val winner = winning.bidder;
            val amount = winning.amount;
            require(token.owner == seller_id, "auction is stale");
            // The escrow row is the debit settle_sale pays out, consumed exactly once.
            delete winning;
            delete a;
            settle_sale(seller_id, token, amount);
            update token ( .owner = winner );
        }

        query get_balance(owner: byte_array): integer {
            val m = member @? { .owner == owner };
            return if (m != null) m.balance else 0;
        }

        query get_owner(token_id: text): byte_array? {
            val t = nft @? { .token_id == token_id };
            return if (t != null) t.owner else null;
        }

        query get_token(token_id: text) {
            val t = nft @? { .token_id == token_id };
            return if (t != null)
                (owner = t.owner, creator = t.creator, royalty_bps = t.royalty_bps)
                else null;
        }

        query get_listing(token_id: text) {
            val l = listing @? { .nft.token_id == token_id };
            return if (l != null) (seller = l.seller, price = l.price) else null;
        }

        query get_offer(token_id: text, bidder: byte_array) {
            val o = offer @? { .nft.token_id == token_id, .bidder == bidder };
            return if (o != null) (amount = o.amount, expires_at = o.expires_at) else null;
        }

        query get_auction(token_id: text) {
            val a = auction @? { .nft.token_id == token_id };
            return if (a != null)
                (seller = a.seller, reserve_price = a.reserve_price, ends_at = a.ends_at)
                else null;
        }

        query get_bid(token_id: text) {
            val b = bid @? { .auction.nft.token_id == token_id };
            return if (b != null) (bidder = b.bidder, amount = b.amount) else null;
        }

        query member_count(): integer = member @* {} ( .owner ).size();

        query token_count(): integer = nft @* {} ( .token_id ).size();

        // INVARIANT: every point in circulation came from a welcome grant. Points are
        // spendable, escrowed in an open offer, or escrowed in a standing bid - never
        // anywhere else; a sale MOVES them and never creates them. The shipped tests
        // compare this to member_count() * WELCOME_POINTS after every step.
        // EXTENDING: a new row that holds points MUST be summed here, or the invariant
        // test goes on passing while the points it holds go missing.
        query points_in_circulation(): integer {
            var total = 0;
            for (b in member @* {} ( .balance )) total += b;
            for (a in offer @* {} ( .amount )) total += a;
            for (a in bid @* {} ( .amount )) total += a;
            return total;
        }

        // INVARIANT: every token has exactly one owner and that owner is a member.
        query tokens_owned_by_members(): integer =
            nft @* { .owner in member @* {} ( .owner ) } ( .token_id ).size();
    """.trimIndent() + "\n"

    private fun marketplaceTestRell(): String = """
        @test module;

        // The marketplace template's invariant tests. They are real: FT4 test accounts,
        // signed operations, PostgreSQL - run via run_rell_tests (pass chromia.yml's
        // moduleArgs PLUS its test.moduleArgs block) or `chr test`.
        //
        // test_round5_price_sandwich_must_fail replays the adversary's sandwich against
        // this template and REQUIRES it to be refused, in both directions: the seller
        // repricing under a pending buy, and the bidder re-offering lower under a
        // pending accept. It can only pass while the two equalities stand, so deleting
        // one goes red before an attacker finds out.
        // test_round5_royalty_bypass_is_documented_not_enforced asserts the OPPOSITE:
        // the bypass still works, exactly as round 5 found it, because a gift plus a
        // side payment is indistinguishable from a gift and an unrelated payment. It is
        // a test of an honest boundary, not of a guard - if it ever starts failing
        // because the bypass was closed, read the header before celebrating.
        // The two round-6 tests cover the auction's extension seams: terms that cannot
        // move under a standing bid, and an escrow that cannot be stranded by walking
        // the token out from under it. Both go red the moment their guard is deleted.

        import main;
        import lib.ft4.test.core.{ register_alice, register_bob, register_trudy, ft_auth_operation_for };

        function signed(keypair: rell.test.keypair, op: rell.test.op) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run();
        }

        function signed_must_fail(keypair: rell.test.keypair, op: rell.test.op, expected: text) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run_must_fail(expected);
        }

        // Stamp the next block `ms` after the last one.
        function after(ms: integer) {
            rell.test.set_next_block_time_delta(ms);
            rell.test.block().run();
        }

        // Points are spendable or escrowed, never anywhere else, and every token has
        // an owner who is a member. Asserted after every step of every test below.
        function assert_conserved() {
            assert_equals(main.points_in_circulation(), main.member_count() * main.WELCOME_POINTS);
            assert_equals(main.tokens_owned_by_members(), main.token_count());
        }

        // EXPLOIT MUST FAIL. Round 5: the buyer signed buy(listing, max_price = 300) as
        // a routine 3x slippage buffer on a 100-point listing, the seller repriced to
        // the ceiling first, and 200 points were extracted. Here the buy names the
        // price it agreed to, so the same front-run only ABORTS it - and the seller had
        // to destroy the listing to reprice at all, because there is no operation that
        // edits one. Then the same sandwich pointed the other way: the bidder cancels
        // and re-offers lower under the seller's pending accept.
        function test_round5_price_sandwich_must_fail() {
            val seller = register_alice();
            val buyer = register_bob();
            signed(seller.keypair, main.register_member());
            signed(buyer.keypair, main.register_member());
            signed(seller.keypair, main.mint_nft("sandwich", 0));
            signed(seller.keypair, main.list_nft("sandwich", 100));
            assert_conserved();

            // The buyer signs buy_nft("sandwich", 100) - the price on the listing. The
            // seller sees it and gets a repricing in first. Repricing is cancel+relist,
            // so this is the strongest front-run the template allows.
            signed(seller.keypair, main.cancel_listing("sandwich"));
            signed(seller.keypair, main.list_nft("sandwich", 300));
            // THE EXPLOIT STEP: in round 5 this paid 300. Here it pays nothing.
            signed_must_fail(buyer.keypair, main.buy_nft("sandwich", 100), "listing price changed");
            assert_equals(main.get_balance(buyer.account.id), main.WELCOME_POINTS);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS);
            assert_equals(main.get_owner("sandwich"), seller.account.id);
            assert_conserved();

            // 300 leaves the buyer only when the buyer names 300.
            signed(buyer.keypair, main.buy_nft("sandwich", 300));
            assert_equals(main.get_owner("sandwich"), buyer.account.id);
            assert_equals(main.get_balance(buyer.account.id), main.WELCOME_POINTS - 300);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 300);
            assert_conserved();

            // THE SAME SANDWICH, OTHER SIDE. The owner is about to accept a 400 bid;
            // the bidder cancels and re-offers 100 first. The accept names 400, so it
            // aborts instead of settling at 100.
            signed(seller.keypair, main.mint_nft("bidsand", 0));
            signed(buyer.keypair, main.make_offer("bidsand", 400, main.MAX_OFFER_MS));
            signed(buyer.keypair, main.cancel_offer("bidsand"));
            signed(buyer.keypair, main.make_offer("bidsand", 100, main.MAX_OFFER_MS));
            signed_must_fail(seller.keypair, main.accept_offer("bidsand", buyer.account.id, 400), "offer amount changed");
            assert_equals(main.get_owner("bidsand"), seller.account.id);
            assert_conserved();

            signed(seller.keypair, main.accept_offer("bidsand", buyer.account.id, 100));
            assert_equals(main.get_owner("bidsand"), buyer.account.id);
            assert_equals(main.get_balance(buyer.account.id), main.WELCOME_POINTS - 300 - 100);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 300 + 100);
            assert_conserved();
        }

        // DOCUMENTED, NOT ENFORCED. This test asserts that the royalty bypass STILL
        // WORKS, because it does and no template can close it: transfer_nft is a gift
        // and transfer_points is a payment, and two willing parties pairing them is
        // indistinguishable from two unrelated favours. What the template does fix is
        // the arithmetic: a recorded sale always pays a royalty, floored at one point
        // instead of rounding to zero as the round-5 dapp did. Read the header of
        // src/main.rell before changing any assertion here.
        function test_round5_royalty_bypass_is_documented_not_enforced() {
            val creator = register_alice();
            val seller = register_bob();
            val buyer = register_trudy();
            signed(creator.keypair, main.register_member());
            signed(seller.keypair, main.register_member());
            signed(buyer.keypair, main.register_member());

            // The honest path first, so the number the bypass avoids is on the record:
            // 10% of 200 is 20, and the creator gets it whether they like the sale or not.
            signed(creator.keypair, main.mint_nft("honest", main.MAX_ROYALTY_BPS));
            signed(creator.keypair, main.transfer_nft("honest", seller.account.id));
            signed(seller.keypair, main.list_nft("honest", 200));
            signed(buyer.keypair, main.buy_nft("honest", 200));
            assert_equals(main.get_balance(creator.account.id), main.WELCOME_POINTS + 20);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 180);
            assert_equals(main.get_balance(buyer.account.id), main.WELCOME_POINTS - 200);
            assert_conserved();

            // BYPASS 1 - a gift plus a side payment. IT WORKS, and it always will.
            signed(creator.keypair, main.mint_nft("bypass", main.MAX_ROYALTY_BPS));
            signed(creator.keypair, main.transfer_nft("bypass", seller.account.id));
            val creator_before = main.get_balance(creator.account.id);
            signed(buyer.keypair, main.transfer_points(seller.account.id, 100));
            signed(seller.keypair, main.transfer_nft("bypass", buyer.account.id));
            assert_equals(main.get_owner("bypass"), buyer.account.id);
            // NOT ENFORCED: a 100-point trade paid the creator nothing.
            assert_equals(main.get_balance(creator.account.id), creator_before);
            assert_conserved();

            // BYPASS 2 - the same trade routed through the marketplace: list at 1 and
            // take the rest off-book. The round-5 dapp paid ZERO here because the cut
            // floor-divided away; this template floors it at one point, so the recorded
            // price always pays something. "Something" is 1 of the 100 that changed
            // hands - the under-pricing is the bypass, and no arithmetic can see it.
            signed(creator.keypair, main.mint_nft("wash", main.MAX_ROYALTY_BPS));
            signed(creator.keypair, main.transfer_nft("wash", seller.account.id));
            val before_wash = main.get_balance(creator.account.id);
            signed(seller.keypair, main.list_nft("wash", 1));
            signed(buyer.keypair, main.transfer_points(seller.account.id, 99));
            signed(buyer.keypair, main.buy_nft("wash", 1));
            assert_equals(main.get_owner("wash"), buyer.account.id);
            assert_equals(main.get_balance(creator.account.id), before_wash + 1);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 180 + 100 + 99);
            assert_equals(main.get_balance(buyer.account.id), main.WELCOME_POINTS - 200 - 100 - 99 - 1);
            assert_conserved();
        }

        // CONSERVATION: both sale paths move points and never create them, and every
        // credit is somebody's debit in the same operation - the buyer's balance on the
        // listing path, the escrow row on the offer path. Totals are checked after
        // every step and the exact split after every sale.
        function test_sale_and_escrow_conserve_points() {
            val creator = register_alice();
            val seller = register_bob();
            val buyer = register_trudy();
            signed(creator.keypair, main.register_member());
            signed(seller.keypair, main.register_member());
            signed(buyer.keypair, main.register_member());
            assert_conserved();

            // Listing path: 400 at 5% is 20 to the creator and 380 to the seller.
            signed(creator.keypair, main.mint_nft("art", 500));
            signed(creator.keypair, main.transfer_nft("art", seller.account.id));
            signed(seller.keypair, main.list_nft("art", 400));
            signed(buyer.keypair, main.buy_nft("art", 400));
            assert_equals(main.get_balance(creator.account.id), main.WELCOME_POINTS + 20);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 380);
            assert_equals(main.get_balance(buyer.account.id), main.WELCOME_POINTS - 400);
            assert_conserved();

            // Offer path: the escrow leaves the bidder the moment the offer is made,
            // and an offer that is cancelled returns exactly what it held.
            signed(creator.keypair, main.make_offer("art", 300, main.MAX_OFFER_MS));
            assert_equals(main.get_balance(creator.account.id), main.WELCOME_POINTS + 20 - 300);
            assert_equals(main.get_offer("art", creator.account.id)!!.amount, 300);
            assert_conserved();
            signed(seller.keypair, main.make_offer("art", 50, main.MAX_OFFER_MS));
            assert_conserved();
            signed(seller.keypair, main.cancel_offer("art"));
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 380);
            assert_conserved();

            // Accepting settles from the escrow: 300 at 5% is 15 to the creator and
            // 285 to the accepting owner, and the escrow row is consumed exactly once.
            signed(buyer.keypair, main.accept_offer("art", creator.account.id, 300));
            assert_equals(main.get_owner("art"), creator.account.id);
            assert_equals(main.get_offer("art", creator.account.id), null);
            assert_equals(main.get_balance(creator.account.id), main.WELCOME_POINTS + 20 - 300 + 15);
            assert_equals(main.get_balance(buyer.account.id), main.WELCOME_POINTS - 400 + 285);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 380);
            assert_conserved();

            // The same escrow cannot be spent twice.
            signed_must_fail(buyer.keypair, main.accept_offer("art", creator.account.id, 300), "not the owner");
            signed_must_fail(creator.keypair, main.cancel_offer("art"), "no open offer");
            assert_conserved();
        }

        // ESCROW + OWNERSHIP + BOUNDS: no free tokens, no double-spending escrowed
        // points, no taking somebody else's escrow, no accepting an expired offer, no
        // selling a token the listing no longer covers, and every input bounded.
        function test_escrow_and_ownership_hold() {
            val alice = register_alice();
            val bob = register_bob();
            val trudy = register_trudy();
            signed(alice.keypair, main.register_member());
            signed(bob.keypair, main.register_member());
            signed(trudy.keypair, main.register_member());
            signed(alice.keypair, main.mint_nft("held", 0));

            // No free tokens: every path that moves one checks THIS caller owns it.
            signed_must_fail(bob.keypair, main.list_nft("held", 1), "not the owner");
            signed_must_fail(bob.keypair, main.transfer_nft("held", trudy.account.id), "not the owner");
            signed_must_fail(bob.keypair, main.accept_offer("held", trudy.account.id, 1), "not the owner");
            signed_must_fail(bob.keypair, main.buy_nft("held", 1), "not listed");
            assert_conserved();

            // Escrowed points are gone from the balance and cannot be spent again -
            // not on a transfer, not on a purchase - and one bidder gets one offer per
            // token, so the same points cannot be pledged twice on the same row.
            signed(bob.keypair, main.make_offer("held", 900, 60000));
            assert_equals(main.get_balance(bob.account.id), main.WELCOME_POINTS - 900);
            signed_must_fail(bob.keypair, main.transfer_points(trudy.account.id, 101), "insufficient balance");
            signed_must_fail(bob.keypair, main.make_offer("held", 1, 60000), "offer already open");
            signed(alice.keypair, main.mint_nft("second", 0));
            signed(alice.keypair, main.list_nft("second", 200));
            signed_must_fail(bob.keypair, main.buy_nft("second", 200), "insufficient balance");
            // And only the bidder can take them back.
            signed_must_fail(alice.keypair, main.cancel_offer("held"), "no open offer");
            signed_must_fail(trudy.keypair, main.cancel_offer("held"), "no open offer");
            assert_conserved();
            signed(bob.keypair, main.cancel_offer("held"));
            signed_must_fail(bob.keypair, main.cancel_offer("held"), "no open offer");
            assert_equals(main.get_balance(bob.account.id), main.WELCOME_POINTS);
            assert_conserved();

            // An expired offer can no longer be accepted - and the bidder still gets
            // the escrow back, so points are never stranded.
            signed(bob.keypair, main.make_offer("held", 200, 60000));
            after(main.MAX_OFFER_MS);
            signed_must_fail(alice.keypair, main.accept_offer("held", bob.account.id, 200), "offer expired");
            signed(bob.keypair, main.cancel_offer("held"));
            assert_equals(main.get_balance(bob.account.id), main.WELCOME_POINTS);
            assert_conserved();

            // A token that moved cannot be sold by the listing it left behind.
            signed(alice.keypair, main.list_nft("held", 100));
            signed(alice.keypair, main.transfer_nft("held", trudy.account.id));
            signed_must_fail(bob.keypair, main.buy_nft("held", 100), "not listed");
            signed_must_fail(alice.keypair, main.cancel_listing("held"), "not listed");
            assert_equals(main.get_owner("held"), trudy.account.id);
            assert_conserved();

            // Every input is bounded, separately.
            signed_must_fail(alice.keypair, main.mint_nft("bad_royalty", main.MAX_ROYALTY_BPS + 1), "royalty too high");
            signed_must_fail(alice.keypair, main.mint_nft("Bad Id", 0), "invalid token id");
            signed_must_fail(alice.keypair, main.mint_nft("held", 0), "token already exists");
            signed_must_fail(trudy.keypair, main.list_nft("held", 0), "price must be positive");
            signed_must_fail(trudy.keypair, main.list_nft("held", -1), "price must be positive");
            signed_must_fail(bob.keypair, main.make_offer("held", 0, 60000), "amount must be positive");
            signed_must_fail(bob.keypair, main.make_offer("held", 10, main.MAX_OFFER_MS + 1), "validity too long");
            signed_must_fail(trudy.keypair, main.transfer_nft("held", trudy.account.id), "cannot transfer to yourself");
            assert_conserved();
        }

        // EXPLOIT MUST FAIL. An ascending auction is where a mutable price comes back:
        // it "needs" a highest_bid field, and a mutable number a counterparty can move
        // under an in-flight settlement is the round-5 sandwich again. Here the terms
        // are immutable and the standing bid is its own immutable escrow row, so the
        // seller's only moves are to cancel (refused once a bid stands), to reopen
        // (refused - the token is still in an auction), or to settle early (refused -
        // the deadline the bidders were shown is a fixed term). Delete any one of
        // those and this test goes red because the seller GOT to move the terms.
        function test_round6_auction_terms_cannot_move_under_a_standing_bid() {
            val seller = register_alice();
            val bidder = register_bob();
            val rival = register_trudy();
            signed(seller.keypair, main.register_member());
            signed(bidder.keypair, main.register_member());
            signed(rival.keypair, main.register_member());
            signed(seller.keypair, main.mint_nft("lot", 0));
            signed_must_fail(seller.keypair, main.start_auction("lot", 100, main.MIN_AUCTION_MS - 1), "auction too short");
            signed_must_fail(seller.keypair, main.start_auction("lot", 100, main.MAX_AUCTION_MS + 1), "auction too long");
            signed_must_fail(seller.keypair, main.start_auction("lot", 0, main.MAX_AUCTION_MS), "reserve must be positive");
            signed(seller.keypair, main.start_auction("lot", 100, main.MAX_AUCTION_MS));
            assert_conserved();

            // The escrow leaves the bidder the moment the bid is placed.
            signed(bidder.keypair, main.place_bid("lot", 100));
            assert_equals(main.get_balance(bidder.account.id), main.WELCOME_POINTS - 100);
            assert_equals(main.get_bid("lot")!!.amount, 100);
            assert_conserved();

            // THE EXPLOIT STEPS: the seller wants better terms now that a bid stands.
            signed_must_fail(seller.keypair, main.cancel_auction("lot"), "auction has a bid");
            signed_must_fail(seller.keypair, main.start_auction("lot", 500, main.MAX_AUCTION_MS), "token is in an auction");
            signed_must_fail(seller.keypair, main.settle_auction("lot"), "auction has not ended");
            assert_equals(main.get_auction("lot")!!.reserve_price, 100);
            assert_conserved();

            // Raising a bid is delete-and-recreate: the outbid escrow comes back in
            // the same operation the new one leaves.
            signed(rival.keypair, main.place_bid("lot", 200));
            assert_equals(main.get_balance(bidder.account.id), main.WELCOME_POINTS);
            assert_equals(main.get_balance(rival.account.id), main.WELCOME_POINTS - 200);
            signed_must_fail(bidder.keypair, main.place_bid("lot", 200), "bid does not beat the standing bid");
            signed_must_fail(rival.keypair, main.place_bid("lot", 500), "you already hold the standing bid");
            signed_must_fail(seller.keypair, main.place_bid("lot", 500), "cannot bid on your own auction");
            assert_conserved();

            // Settlement pays exactly what the winning escrow held.
            after(main.MAX_AUCTION_MS);
            signed(bidder.keypair, main.settle_auction("lot"));
            assert_equals(main.get_owner("lot"), rival.account.id);
            assert_equals(main.get_bid("lot"), null);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 200);
            assert_equals(main.get_balance(rival.account.id), main.WELCOME_POINTS - 200);
            assert_equals(main.get_balance(bidder.account.id), main.WELCOME_POINTS);
            assert_conserved();
        }

        // EXPLOIT MUST FAIL. An escrowed bid has no owner who can take it back - only
        // settle_auction pays it out - so a token that LEAVES while a bid stands
        // strands somebody's points forever. Nothing is minted, every conservation
        // total stays green and the security gate says nothing: this is a design hole
        // a static rule cannot see, which is why require_unencumbered is asked by
        // every token-moving path instead of by the auction operations alone.
        function test_round6_auction_escrow_cannot_be_stranded() {
            val seller = register_alice();
            val bidder = register_bob();
            val stranger = register_trudy();
            signed(seller.keypair, main.register_member());
            signed(bidder.keypair, main.register_member());
            signed(stranger.keypair, main.register_member());
            signed(seller.keypair, main.mint_nft("strand", 0));

            // An escrowed offer is already sitting on the token when the auction opens.
            signed(stranger.keypair, main.make_offer("strand", 300, main.MAX_OFFER_MS));
            signed(seller.keypair, main.start_auction("strand", 100, main.MAX_AUCTION_MS));
            signed(bidder.keypair, main.place_bid("strand", 150));
            assert_conserved();

            // THE EXPLOIT STEPS: every way to walk the token out from under the
            // standing bid. All three are refused by the one encumbrance helper.
            signed_must_fail(seller.keypair, main.transfer_nft("strand", stranger.account.id), "token is in an auction");
            signed_must_fail(seller.keypair, main.list_nft("strand", 1), "token is in an auction");
            signed_must_fail(seller.keypair, main.accept_offer("strand", stranger.account.id, 300), "token is in an auction");
            assert_equals(main.get_owner("strand"), seller.account.id);
            assert_equals(main.get_bid("strand")!!.amount, 150);
            assert_conserved();

            // And the seller cannot hold the escrow hostage by walking away: once the
            // deadline passes ANY member can settle.
            after(main.MAX_AUCTION_MS);
            signed(stranger.keypair, main.settle_auction("strand"));
            assert_equals(main.get_owner("strand"), bidder.account.id);
            assert_equals(main.get_balance(seller.account.id), main.WELCOME_POINTS + 150);
            assert_equals(main.get_balance(bidder.account.id), main.WELCOME_POINTS - 150);
            assert_conserved();

            // The offer escrow was never touched and is still the stranger's to take.
            signed(stranger.keypair, main.cancel_offer("strand"));
            assert_equals(main.get_balance(stranger.account.id), main.WELCOME_POINTS);
            assert_conserved();
        }
    """.trimIndent() + "\n"
    // ---- lending template: nothing stores a cash-denominated debt, so no share price can go stale ----
    //
    // Adversary round 6 (exploit-corpus/realworld/adversary-round6/dapp_b_creditline,
    // corpus row r6-lending-jit-interest-capture) drained a competent hand-built
    // lending pool the gate certified with ZERO findings: 10000 cash in, 11500 out,
    // after one block in the pool. Interest accrued LAZILY - only inside the
    // operations a BORROWER signs - so pool_value(), the price of a lender share,
    // was stale between a borrower's touches while the pending interest was already
    // public on the loan row. The attacker deposited at the stale price, waited one
    // block for the borrower's next touch to land the accrual, and withdrew at the
    // fresh one. Nothing was minted, so every conservation invariant stayed green
    // and the gate reported nothing - the theft was from one lender to another.
    //
    // "A value refreshed on some paths and not others" has no rule that is both
    // un-evadable and quiet: every formulation either misses the laundered form or
    // fires on any lazily-updated cache. So the answer is a template (north-star
    // principle 4), and the template's answer is not "remember to accrue" - it is
    // that a cash-denominated debt is not stored ANYWHERE. Positions and the pool
    // both carry `scaled_debt` in index units; the cash figures exist only inside a
    // `pool_state`; pool_now() is the only function that makes one; and every
    // pricing helper TAKES one. The round-6 bug is not something an extender can
    // forget their way into - it takes hand-building a price out of the raw field.

    /**
     * Module args for the templates whose main module reads an oracle key from
     * configuration - `vault` and `lending`. FT4's test wiring plus that key,
     * which is FT4's published TEST admin key, used here only so the shipped
     * tests can sign price posts. Mirrors chromia.yml's test.moduleArgs and,
     * like them, never belongs under blockchains.<name>.
     */
    fun oracleTestModuleArgs(): Map<String, Map<String, kotlinx.serialization.json.JsonElement>> =
        ft4TestModuleArgs() + mapOf(
            "main" to mapOf("oracle_pubkey" to JsonPrimitive(TEST_ADMIN_PUBKEY))
        )

    /**
     * The lending template reads TWO keys from configuration: the oracle that posts
     * the price, and the protocol key that may collect the accrued fee. Both are
     * deliberately unset in the production block.
     */
    fun lendingTestModuleArgs(): Map<String, Map<String, kotlinx.serialization.json.JsonElement>> =
        ft4TestModuleArgs() + mapOf(
            "main" to mapOf(
                "oracle_pubkey" to JsonPrimitive(TEST_ADMIN_PUBKEY),
                "treasury_pubkey" to JsonPrimitive(TEST_ADMIN_PUBKEY)
            )
        )

    private fun lendingChromiaYml(name: String): String = ft4ChromiaYml(
        name,
        productionModuleArgsNote = buildString {
            append("      # REQUIRED before `chr build` / deploy - the lending pool's oracle key and\n")
            append("      # the protocol key that may collect the accrued fee. They are deliberately\n")
            append("      # NOT set here so the chain cannot be built with a placeholder: put the\n")
            append("      # 33-byte compressed public keys here and nowhere in source, and make them\n")
            append("      # DIFFERENT keys held by different parties. Never copy a test key.\n")
            append("      # main:\n")
            append("      #   oracle_pubkey: x\"<your oracle public key>\"\n")
            append("      #   treasury_pubkey: x\"<your protocol fee key>\"\n")
        },
        extraTestModuleArgs = buildString {
            append("    # The shipped tests sign price posts and fee collections with FT4's published\n")
            append("    # test key. One key in BOTH roles is a test convenience, never a deployment.\n")
            append("    main:\n")
            append("      oracle_pubkey: x\"$TEST_ADMIN_PUBKEY\"\n")
            append("      treasury_pubkey: x\"$TEST_ADMIN_PUBKEY\"\n")
        }
    )

    private fun lendingMainRell(): String = """
        module;

        import lib.ft4.auth;
        import lib.ft4.accounts;

        // Lending template: lenders deposit cash for pool shares, borrowers lock
        // collateral and draw cash against it at an oracle price, debt accrues
        // interest, and an under-water position is liquidated at a bonus.
        //
        // Adversary round 6 (exploit-corpus/realworld/adversary-round6/dapp_b_creditline,
        // corpus row r6-lending-jit-interest-capture) drained a competent hand-built
        // lending pool the gate certified with ZERO findings: 10000 cash in, 11500 out,
        // after one block in the pool. Interest accrued LAZILY - inside the operations a
        // BORROWER signs - so the pool's cash-denominated debt counter, and with it the
        // price of a lender share, was stale between a borrower's touches while the
        // pending interest was already public on the loan row. The attacker deposited at
        // the stale price, waited for the borrower's next touch to land the accrual, and
        // withdrew at the fresh one. Nothing was minted, so every conservation invariant
        // stayed green: the theft was from one lender to another.
        //
        // That shape - "a value refreshed on some paths and not others" - has no static
        // rule that is both un-evadable and quiet, so the answer is a template
        // (north-star principle 4).
        //
        // THE GUARD THAT MAKES IT UNWRITABLE: NOTHING HERE STORES A CASH-DENOMINATED DEBT.
        //   * A position stores `scaled_debt`, denominated in INDEX units, never in cash.
        //     The pool stores `scaled_debt` too - the exact sum of the positions' - in the
        //     same units. Neither is a snapshot of anything, so neither can go stale.
        //   * The cash a position owes, and the value a share is a claim on, exist ONLY
        //     inside a `pool_state`, and `pool_now()` is the ONLY function that makes one.
        //     It reads the block clock every time it is called.
        //   * `debt_of`, `shares_for`, `cash_for`, `payment_for` and `is_liquidatable`
        //     each TAKE a `pool_state`. A new operation therefore cannot price an entry
        //     or an exit without holding one, and cannot hold one without having called
        //     pool_now() in this operation. Forgetting is not possible; the only way back
        //     to the round-6 bug is to hand-build a price out of the raw `scaled_debt`
        //     field - a deliberate act a reviewer can see, not an omission.
        //   * The index is a PURE FUNCTION OF THE BLOCK CLOCK - simple interest since the
        //     pool's first priced block - so there is no "index at last accrual" to
        //     refresh and no accrue() to forget to call. `opened_at` is written once, by
        //     the first operation that ever prices anything, and never again.
        //
        // THE SECOND GUARD, added after adversary round 7 drained the TEMPLATE ITSELF:
        // DEBT IS WORTH WHAT THE COLLATERAL CAN REPAY, NOT WHAT THE LOAN SAYS.
        //   * pool_now() values the pool's debt as min(face, what pool.total_collateral
        //     is worth at a fresh price). A position past any liquidator's reach stops
        //     being counted at face the moment it passes its own collateral, so the
        //     share price falls as the loss happens rather than staying flat until the
        //     cash runs out.
        //   * That bound is a PURE FUNCTION of the clock and the price - not a write-off
        //     somebody triggers - so there is no block to be on the right side of.
        //   * Round 7 (realworld/adversary-round7/dapp_c_lending_base) drained 13920 of
        //     14000 from an honest lender on this template EXACTLY AS SHIPPED, with
        //     nothing minted and every conservation invariant green: bad debt left the
        //     share price untouched, the pool merely became illiquid at an unchanged
        //     price, and withdraw_cash is first-come-first-served.
        //   * PRICING A SHARE THEREFORE NEEDS A PRICE, once any cash is out on loan. An
        //     unlent pool needs no oracle; a lent one cannot price an exit without
        //     pricing the collateral behind the debt. Same deliberate trade as halting
        //     borrowing while the oracle is silent, extended to entries and exits.
        //
        // THE THIRD GUARD: THE PROTOCOL FEE ACCRUES WITH THE CLOCK, NEVER IN A STEP.
        //   * accrued_fee() is a cut of ALL the interest the pool has earned - the part
        //     already paid (pool.interest_realised, a cumulative record of past events)
        //     plus the part still outstanding (a pure function of the clock) - less what
        //     has been collected. pool_now() nets it out of `value`.
        //   * So a repayment moves no pool value: the interest inside it just moves from
        //     the outstanding side of that sum to the realised side. And a collection
        //     moves no pool value either: cash out and fee_collected up by the same
        //     amount. Nothing to straddle, in either direction.
        //   * Round 7 (realworld/adversary-round7/dapp_a_feepool) took the fee as a step
        //     at the repayment block, exactly as this header used to advise, and 300
        //     moved from an honest lender to an attacker for one block of timing. Set
        //     PROTOCOL_FEE_BPS to 0 if your protocol takes no cut; do not move WHEN it
        //     is taken.
        //
        // THE OTHER FIVE GUARDS, all of which round 6's build already refused with and
        // which are kept here unchanged in substance:
        //   OVER-COLLATERALISED - a borrow is capped at MAX_LTV_BPS of what the collateral
        //                     is worth at a FRESH price, checked against the position's
        //                     whole debt every time, so slicing it into pieces gains
        //                     nothing; remove_collateral re-checks the same limit.
        //   BOUNDED ORACLE  - the vault template's price feed: one configured key, a move
        //                     capped at MAX_PRICE_MOVE_BPS, at most one post per
        //                     MIN_PRICE_UPDATE_INTERVAL_MS, and a price older than
        //                     MAX_PRICE_AGE_MS is not a price - everything that needs one
        //                     refuses rather than using the last number it saw.
        //   LIQUIDATION     - past LIQUIDATION_THRESHOLD_BPS anyone but the borrower may
        //                     repay at most CLOSE_FACTOR_BPS of the debt and seize
        //                     collateral worth that plus LIQUIDATION_BONUS_BPS, priced at
        //                     the same fresh price the health check used. The bonus comes
        //                     out of the liquidated position's own collateral, so a pair
        //                     of accounts under one hand nets exactly zero.
        //   FIRST DEPOSIT   - the ERC-4626 first-depositor inflation steal starts by
        //                     seeding the pool with one unit. A first deposit must be at
        //                     least MIN_INITIAL_DEPOSIT, and any deposit that would mint
        //                     zero shares is REFUSED rather than swallowed, so the victim
        //                     keeps their cash instead of handing it to the seed.
        //   SATURATING      - every derived cash figure saturates instead of overflowing.
        //                     An aborting arithmetic would make a position un-priceable
        //                     and therefore un-liquidatable, which is worse than a debt
        //                     that stops growing.
        //
        // EXTENDING THIS TEMPLATE - the seams a static rule cannot see, and where round 6
        // went wrong:
        //   1. EVERY PATH THAT READS OR WRITES THE SHARE PRICE MUST PRICE THROUGH
        //      pool_now(). Not "must remember to accrue first" - there is nothing to
        //      accrue. The rule is: never read `pool.total_scaled_debt`, `loan.scaled_debt` or
        //      `pool.total_shares` to build a cash number yourself. Pass a `pool_state`
        //      to debt_of / shares_for / cash_for / payment_for and let them do it. Those
        //      raw fields are in INDEX units; treating one as cash IS the round-6 drain,
        //      and it is the one edit to this file to refuse.
        //   2. ANY NEW OPERATION THAT MOVES POOL VALUE IN A STEP RE-OPENS THE JIT WINDOW,
        //      AND THE FIX IS TO NET IT INTO pool_state, NOT TO CHARGE A TOLL.
        //      Here value moves only with the clock, continuously, so a deposit held for
        //      one block earns one block of interest and a one-block round trip cannot
        //      come out ahead. Add a protocol fee, a bad-debt write-off, a donation or a
        //      rewards drop naively and value moves in a JUMP, which somebody will
        //      straddle.
        //
        //      THIS HEADER USED TO PRESCRIBE "an entry/exit fee or a minimum holding
        //      period". THAT ADVICE WAS WRONG and it is worth knowing why, because both
        //      mistakes are easy to repeat. Adversary round 7 implemented it faithfully
        //      - a 20% cut of interest, taken at the repayment block, plus a 24-hour
        //      minimum holding period - and the pool still drained, honest 10901 against
        //      attacker 11500:
        //        * A HOLDING PERIOD GATES A ROUND TRIP. The attack has no deposit in it.
        //          It is an EXIT, by a lender who has been in the pool since the first
        //          block and is years past any period. The only caller such a rule ever
        //          catches is an honest short-term depositor.
        //        * A FIXED ENTRY/EXIT FEE IS SIZED BY THE ATTACKER. The step is a
        //          percentage of interest on a position they chose the size of, so they
        //          can always make the step exceed any fixed percentage of their stake.
        //      THE RULE THAT ACTUALLY HOLDS is the one this template already uses for
        //      interest: A STEP IN POOL VALUE MUST BE NETTED OUT OF pool_state SO IT
        //      ACCRUES CONTINUOUSLY. A toll on the round trip does not stop an exit.
        //      Both extensions below are shipped, worked, and covered by must-fail tests:
        //        * PROTOCOL FEE - accrued_fee() is a cut of realised PLUS outstanding
        //          interest, netted out of `value`; repay only moves interest from one
        //          side of that sum to the other, and collect_fees lowers cash and
        //          raises fee_collected by the same amount. Neither moves `value`.
        //          (test_round7_fee_step_jit_capture_must_fail)
        //        * BAD-DEBT WRITE-OFF - recoverable_debt() caps the pool's debt at what
        //          its collateral can repay, continuously, instead of writing a position
        //          off in one operation.
        //          (test_round7_bad_debt_exit_race_must_fail)
        //      A DONATION or a rewards drop is the same problem in the other direction:
        //      do not credit it to cash_available in one block. Give it a start block and
        //      a rate and let pool_now() release it with the clock, exactly as the index
        //      does - that is the shape, and it is the same one the streaming lesson in
        //      chromia_rell_practices_help names.
        //      WHAT IS STILL A STEP HERE, stated rather than implied: while
        //      recoverable_debt()'s cap is ACTIVE - only in a pool whose debt already
        //      exceeds its collateral - a price post and an add_collateral/liquidate both
        //      move `value` in a jump. That window exists only in an already-insolvent
        //      pool, and closing it would mean valuing the collateral continuously too,
        //      which no oracle can do.
        //   3. EVERY NEW ROW THAT HOLDS CASH MUST BE ADDED TO cash_in_circulation(),
        //      EVERY NEW ROW THAT HOLDS DEBT TO scaled_debt_matches_positions(), AND
        //      EVERY NEW ROW THAT HOLDS COLLATERAL TO collateral_matches_positions().
        //      The shipped tests compare those to fixed totals after every step; a row
        //      they do not sum makes the invariant pass while value goes missing - and
        //      collateral now sets the ceiling on what the pool's debt is worth, so a
        //      collateral row the aggregate misses is a share price that is too high.
        //   4. A SEAM-2 MITIGATION WILL TURN SHIPPED TESTS RED. NAMED, so that three
        //      unexplained failures do not push you into weakening the tests that
        //      matter. If you add a deposit lock-up, a minimum holding period, or any
        //      other gate on a round trip, these three stop compiling as written:
        //        * test_round6_jit_interest_capture_must_fail - the attacker's one-block
        //          round trip is now refused BEFORE the value assertion is reached, so
        //          the test proves nothing about the price. Adapt it, do not delete it:
        //          assert the refusal, wait out the period, and then keep the original
        //          "no more than 10000 comes out" assertion (it still holds, plus one
        //          period of honest interest).
        //        * test_first_depositor_inflation_refuses_instead_of_swallowing - the
        //          victim's withdrawal is inside the lock-up. Same adaptation.
        //        * test_interest_moves_only_from_borrower_to_lender - a fee changes the
        //          split, not the conservation: assert the protocol's cut explicitly and
        //          subtract it from the lender's, so the total still reconciles.
        //      Round 7 committed working adapted versions of all three in
        //      exploit-corpus/realworld/adversary-round7/dapp_a_feepool/src/test/main_test.rell,
        //      marked ADAPTED FOR THE EXTENSION.
        //
        // WHAT NO TEMPLATE CAN FIX, stated rather than implied:
        //   - One oracle key posts the price. An honest-but-wrong post still moves who is
        //     liquidatable; the bound and the interval cap how fast, not whether.
        //   - While the oracle is silent NOBODY is liquidatable - the freshness check
        //     halts liquidation exactly as it halts borrowing. That is a deliberate
        //     trade: no liquidations at a stale price, at the cost of bad debt in an
        //     outage.
        //   - Liquidation is a race between liquidators and the bonus is what pays for
        //     it; a borrower watching the chain can always add collateral first.
        //   - A position that goes under water faster than liquidators arrive leaves BAD
        //     DEBT. The share price now falls as that happens - recoverable_debt() stops
        //     valuing debt above what its collateral can repay - so the exit ORDER no
        //     longer decides who eats the loss. What remains is ILLIQUIDITY: the pool
        //     can be worth more than the cash it holds, and withdraw_cash is
        //     first-come-first-served for that cash, so a late exit may have to wait for
        //     a repayment or a liquidation. That is the honest residual; a lender who
        //     waits is owed the same as one who did not.
        //     (An earlier version of this header said the loss "sits in the share price
        //     as debt that will never be repaid". IT DID NOT, and the inversion cost an
        //     honest lender 13920 of 14000 in adversary round 7: pool_now() valued every
        //     index unit at face, so the price was UNCHANGED and the whole loss landed on
        //     whoever exited last while the attacker who created it exited first.)
        //   - The write-off's bound is at the POOL level, so a heavily over-collateralised
        //     position's surplus can mask another's shortfall. Making it exact costs one
        //     pass over every loan on every pricing call; recoverable_debt() says where
        //     to make that change.
        //   - The interest RATE is your economics, and so is PROTOCOL_FEE_BPS (0 turns
        //     the fee off entirely; only WHEN it is taken is structural). So are
        //     MAX_LTV_BPS, the threshold, the
        //     bonus and the close factor: they decide whether liquidators show up before
        //     a position goes under water, and no template can size them for your asset.
        //   - Every rounding is in the pool's favour by at most one unit: a borrow records
        //     at least what it took out, a payment retires only the units it covers and
        //     is charged the rounded-up price of exactly those, and what the pool is
        //     worth is rounded down. Dust accumulates to the lenders and never leaves.
        //   - Amounts here are WHOLE units. Because a position's debt is stored in index
        //     units, a borrow smaller than the index ratio rounds against the borrower -
        //     harmless at a token's smallest denomination, which is what FT4 assets use,
        //     and visible if you denominate in whole tokens.

        struct module_args {
            oracle_pubkey: pubkey;
            // The protocol key that may collect the accrued fee. Configured, never a
            // parameter and never in source - and a DIFFERENT key from the oracle.
            treasury_pubkey: pubkey;
        }

        entity account {
            key owner: byte_array;
            mutable cash: integer = 0;
            mutable tokens: integer = 0;
        }

        // A lender's claim on the pool.
        entity lender {
            key owner: byte_array;
            mutable shares: integer = 0;
        }

        // One position per borrower. `scaled_debt` is denominated in INDEX units, NOT in
        // cash: what is owed is scaled_debt times the CURRENT index, and the current
        // index comes from pool_now(). There is deliberately no cash `principal` field
        // and no per-loan `accrued_at` - those are the two fields that go stale, and
        // their staleness is the round-6 drain.
        entity loan {
            key borrower: byte_array;
            mutable scaled_debt: integer = 0;
            mutable collateral: integer = 0;
        }

        object pool {
            mutable cash_available: integer = 0;
            // The exact sum of every position's scaled_debt, in the SAME index units, so
            // it cannot drift out of date. Never read this as a cash amount.
            mutable total_scaled_debt: integer = 0;
            mutable total_shares: integer = 0;
            // The exact sum of every position's collateral, in TOKENS. What the pool's
            // debt is worth is capped by what this is worth at a fresh price - see
            // pool_now(). Not a cash figure and not a snapshot: it changes only in the
            // same operation that moves a position's collateral.
            mutable total_collateral: integer = 0;
            // CUMULATIVE, MONOTONE, AND NEVER A SNAPSHOT: the total interest borrowers
            // have actually PAID, in cash. It records past events only, so it cannot go
            // stale the way a "current value" counter can. Together with the interest
            // still outstanding (a pure function of the clock) it is what the protocol's
            // fee is a cut of.
            mutable interest_realised: integer = 0;
            // Cash the protocol has already taken out of the pool as its fee. The
            // uncollected fee is (the fee earned so far) minus this, and collect_fees
            // moves cash and raises this by the SAME amount, so collecting changes no
            // share price.
            mutable fee_collected: integer = 0;
            // The interest clock's anchor: written once, by the first operation that ever
            // prices anything, and never again. Nothing else in this module writes them.
            mutable opened: boolean = false;
            mutable opened_at: timestamp = 0;
        }

        // price == 0 means "never posted": nothing that needs a price may run.
        object price_feed {
            mutable price: integer = 0;
            mutable updated_at: timestamp = 0;
        }

        val BPS = 10000;
        // Cash per token, scaled: PRICE_SCALE == 1.00.
        val PRICE_SCALE = 1000000;
        val MAX_PRICE = 1000 * PRICE_SCALE;
        // A post may move the price at most 20% from the previous one...
        val MAX_PRICE_MOVE_BPS = 2000;
        // ...and at most once an hour.
        val MIN_PRICE_UPDATE_INTERVAL_MS = 60 * 60 * 1000;
        // A price older than a day is not a price.
        val MAX_PRICE_AGE_MS = 24 * 60 * 60 * 1000;

        // Bound every amount BEFORE it is multiplied by a price: Rell integers are
        // 64-bit and an overflow aborts.
        val MAX_AMOUNT = 1000000000;

        // INDEX_SCALE is the index's 1.00: a position's debt in cash is
        // scaled_debt * index / INDEX_SCALE.
        val INDEX_SCALE = 1000000000;
        // The index stops growing at 100x - about two thousand years at the default
        // rate - so the arithmetic can never abort.
        val MAX_INDEX = 100 * INDEX_SCALE;
        // The same ceiling as a plain multiplier: 100.
        val MAX_INDEX_GROWTH = MAX_INDEX / INDEX_SCALE;

        // BOTH SATURATION CEILINGS ARE DERIVED FROM THE SAME NUMBER, so the pool can
        // never value debt above what the borrowers behind it will actually be charged.
        // (Adversary round 7, defect G5: MAX_DEBT was MAX_AMOUNT and MAX_POOL_DEBT was
        // a million times larger, so past MAX_AMOUNT one position was charged a debt
        // that saturated while the share price still counted the unsaturated figure.
        // Unreachable in the welcome-grant economy below, reachable the moment you
        // follow this header's own instruction and swap in a real FT4 asset.)
        // The most a single position's debt can ever be worth: the largest borrow the
        // limit allows, grown by the index's own ceiling.
        val MAX_DEBT = MAX_AMOUNT * MAX_INDEX_GROWTH;
        // The number of such positions the pool's aggregate stays exact for. Raising
        // this without raising MAX_DEBT is what re-opens the divergence.
        val MAX_POSITIONS_PRICED = 10000;
        val MAX_POOL_DEBT = MAX_DEBT * MAX_POSITIONS_PRICED;

        // Borrow up to 60% of collateral value...
        val MAX_LTV_BPS = 6000;
        // ...liquidatable once debt passes 75% of it...
        val LIQUIDATION_THRESHOLD_BPS = 7500;
        // ...and the liquidator seizes 10% more collateral than they repaid.
        val LIQUIDATION_BONUS_BPS = 1000;
        // At most half a position may be closed in one liquidation.
        val CLOSE_FACTOR_BPS = 5000;

        val YEAR_MS = 365 * 24 * 60 * 60 * 1000;
        val INTEREST_RATE_BPS_PER_YEAR = 500;

        // The protocol's cut of the interest the pool earns - a "reserve factor". The
        // RATE is your economics (set it to 0 and every line below goes inert); the
        // SHAPE is not. It accrues CONTINUOUSLY out of pool_state, exactly as the
        // interest it is a cut of does, and is never taken as a step. See seam 2.
        val PROTOCOL_FEE_BPS = 2000;

        // The first deposit into an empty pool must be large enough that no later
        // deposit can be rounded away against it (the ERC-4626 inflation steal seeds
        // the pool with one unit).
        val MIN_INITIAL_DEPOSIT = 1000;

        // The one-time welcome grant is the ONLY place cash or tokens are created
        // (a stand-in for a real deposit - replace it with an FT4 asset transfer and
        // keep the same discipline).
        val WELCOME_CASH = 10000;
        val WELCOME_TOKENS = 100;

        // DEFAULT: every operation requires the Transfer flag. FT4 resolves flags
        // with contains_all(), and contains_all([]) is always true - never weaken
        // this default.
        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            flags = ["T"]
        );

        // Exposed so the shipped tests can sign price posts with the configured key.
        function oracle_pubkey(): pubkey = chain_context.args.oracle_pubkey;
        function treasury_pubkey(): pubkey = chain_context.args.treasury_pubkey;

        // -------------------------- THE ONE PRICING HELPER --------------------------

        // Everything a path needs in order to price an entry or an exit, computed from
        // the block clock at the moment it is asked for. Nothing in this module stores a
        // cash-denominated debt or a share price, so this struct is the only place
        // either one exists.
        struct pool_state {
            debt_index: integer;
            // The fresh price the debt was valued at. 0 ONLY when the pool has no debt
            // at all, in which case there is nothing to value and no price is needed.
            price: integer;
            // What the outstanding debt is WORTH: its face value, capped at what the
            // collateral behind it can actually repay.
            debt: integer;
            // The protocol's accrued, uncollected cut of the interest. Netted out of
            // `value` below - it is not part of what a share is a claim on.
            fee: integer;
            value: integer;
            shares: integer;
        }

        // The interest index: simple interest since the pool's first priced block, times
        // INDEX_SCALE. A PURE FUNCTION OF THE BLOCK CLOCK - it is not stored, so there is
        // no cached copy to refresh and no accrual to forget.
        function current_index(): integer {
            if (not pool.opened) return INDEX_SCALE;
            val elapsed = op_context.last_block_time - pool.opened_at;
            if (elapsed <= 0) return INDEX_SCALE;
            val growth =
                INDEX_SCALE.to_big_integer() * INTEREST_RATE_BPS_PER_YEAR.to_big_integer() * elapsed.to_big_integer()
                / (BPS.to_big_integer() * YEAR_MS.to_big_integer());
            val grown = INDEX_SCALE.to_big_integer() + growth;
            val cap = MAX_INDEX.to_big_integer();
            return (if (grown > cap) cap else grown).to_integer();
        }

        // THE ONE HELPER. Every path that reads or writes the share price gets its
        // numbers from here, because debt_of / shares_for / cash_for / payment_for /
        // is_liquidatable all take the pool_state it returns and there is no other way
        // to make one.
        function pool_now(): pool_state {
            if (not pool.opened) {
                pool.opened = true;
                pool.opened_at = op_context.last_block_time;
            }
            val now_index = current_index();
            val face = to_cash_down(pool.total_scaled_debt, now_index, MAX_POOL_DEBT);
            // NO DEBT, NOTHING TO VALUE: an unlent pool is worth its cash and needs no
            // oracle. The moment cash is out on loan, pricing a share means pricing the
            // collateral behind that loan, so a share cannot be priced without a fresh
            // price either. That is the same deliberate trade as halting borrowing.
            val price = if (face > 0) fresh_price() else 0;
            val debt = recoverable_debt(face, price);
            val fee = accrued_fee(pool.total_scaled_debt, debt);
            val net = pool.cash_available + debt - fee;
            return pool_state(
                debt_index = now_index,
                price = price,
                debt = debt,
                fee = fee,
                // Never negative: a write-down deep enough to swallow the whole pool
                // leaves shares worth nothing, not worth less than nothing.
                value = if (net < 0) 0 else net,
                shares = pool.total_shares
            );
        }

        // WHAT THE POOL'S DEBT IS WORTH, not what it says on the loans. Every index unit
        // valued at face is what let adversary round 7 pay the first lender out of the
        // second lender's capital: a position ten years past any liquidator's reach
        // still counted at 306000 against 10000 of collateral, the share price never
        // moved, and withdraw_cash is first-come-first-served. Debt is worth at most
        // what the collateral behind it can repay, and that bound is a PURE FUNCTION of
        // the clock and the price - it moves continuously, it is not a write-off
        // somebody has to trigger, and there is no block to be on the right side of.
        //
        // HONEST LIMIT, because the residual list is where an auditor trusts this file:
        // the bound is at the POOL level, so one heavily over-collateralised position's
        // surplus can mask another's shortfall. Making it exact means valuing every
        // position on every pricing call - O(number of loans) per operation. If your
        // pool has few, large positions, do that instead; the shape to keep is that the
        // bound lives HERE, inside pool_now(), and not in an operation someone calls.
        function recoverable_debt(face: integer, price: integer): integer {
            if (face <= 0) return 0;
            val backing = pool.total_collateral.to_big_integer() * price.to_big_integer()
                / PRICE_SCALE.to_big_integer();
            val f = face.to_big_integer();
            return (if (backing < f) backing else f).to_integer();
        }

        // THE PROTOCOL'S CUT, ACCRUED CONTINUOUSLY. A share of ALL the interest this
        // pool has ever earned - the part borrowers have already paid (pool.interest_realised,
        // a cumulative record of past events, which is why it cannot go stale) plus the
        // part still outstanding (debt_value minus the same units at face, a pure
        // function of the clock) - minus what the protocol has already taken out.
        //
        // Nothing here happens in a STEP. That is the whole point: see seam 2.
        function accrued_fee(scaled_total: integer, debt_value: integer): integer {
            if (PROTOCOL_FEE_BPS <= 0) return 0;
            // One index unit was one unit of cash when the pool opened, so what the debt
            // is worth ABOVE its unit count is the interest inside it.
            val outstanding = debt_value - scaled_total;
            val interest = pool.interest_realised + max(0, outstanding);
            val earned = interest.to_big_integer() * PROTOCOL_FEE_BPS.to_big_integer() / BPS.to_big_integer();
            val uncollected = earned - pool.fee_collected.to_big_integer();
            // A write-down can retract interest the protocol was already paid for. The
            // lenders keep that, rather than the pool carrying a negative liability.
            return (if (uncollected < 0) 0 else uncollected).to_integer();
        }

        // Index units -> cash, rounded DOWN and saturated at `ceiling`. This is what a
        // position OWES and what the pool is WORTH: never overstated, so the share price
        // never counts value the pool has not earned and a borrow limit is never widened
        // by rounding.
        function to_cash_down(scaled: integer, debt_index: integer, ceiling: integer): integer {
            if (scaled <= 0) return 0;
            val whole = scaled.to_big_integer() * debt_index.to_big_integer() / INDEX_SCALE.to_big_integer();
            val cap = ceiling.to_big_integer();
            return (if (whole > cap) cap else whole).to_integer();
        }

        // Index units -> cash, rounded UP. This is what a payment is CHARGED for the
        // units it retires, so a repayment can never buy more debt relief than it paid
        // for. Saturating for the same reason: an aborting arithmetic would make a
        // position un-priceable and therefore un-liquidatable.
        function to_cash_up(scaled: integer, debt_index: integer, ceiling: integer): integer {
            if (scaled <= 0) return 0;
            val d = INDEX_SCALE.to_big_integer();
            val n = scaled.to_big_integer() * debt_index.to_big_integer();
            val whole = n / d;
            // The remainder is smaller than INDEX_SCALE, so it always fits an integer.
            val remainder = (n - whole * d).to_integer();
            val cap = ceiling.to_big_integer();
            val cash = (if (whole > cap) cap else whole).to_integer();
            return if (remainder > 0 and cash < ceiling) cash + 1 else cash;
        }

        // Cash -> index units, rounded UP: a borrow records at least what it took out,
        // so no amount of slicing walks cash out of the pool that nobody owes.
        function to_scaled_up(cash: integer, debt_index: integer): integer {
            if (cash <= 0) return 0;
            val d = debt_index.to_big_integer();
            val n = cash.to_big_integer() * INDEX_SCALE.to_big_integer();
            val whole = n / d;
            // The remainder is smaller than the index, which is bounded by MAX_INDEX.
            val remainder = (n - whole * d).to_integer();
            val scaled = whole.to_integer();
            return if (remainder > 0) scaled + 1 else scaled;
        }

        // Cash -> index units, rounded DOWN: a payment retires only what it covers.
        function to_scaled_down(cash: integer, debt_index: integer): integer {
            if (cash <= 0) return 0;
            return (cash.to_big_integer() * INDEX_SCALE.to_big_integer() / debt_index.to_big_integer()).to_integer();
        }

        // A payment against a position, priced in BOTH directions out of the same state:
        // it retires as many index units as the offered cash covers, and charges for
        // exactly those. Never more relief than was paid for, never more cash than was
        // offered, and offering at least the whole debt clears the whole position.
        struct payment {
            scaled: integer;
            cash: integer;
        }

        function payment_for(l: loan, offered: integer, st: pool_state): payment {
            val scaled = min(l.scaled_debt, to_scaled_down(offered, st.debt_index));
            require(scaled > 0, "payment too small to retire any debt");
            return payment(scaled = scaled, cash = to_cash_up(scaled, st.debt_index, MAX_DEBT));
        }

        // A payment's interest, banked. One index unit was one unit of cash when the
        // pool opened, so the cash charged ABOVE the units retired IS the interest on
        // them. This counter only ever records what has already happened, so - unlike a
        // "current debt" counter - there is no version of it that can be out of date.
        function record_interest(p: payment) {
            val interest = p.cash - p.scaled;
            if (interest > 0) pool.interest_realised += interest;
        }

        // What this position owes, in cash, right now.
        function debt_of(l: loan, st: pool_state): integer =
            to_cash_down(l.scaled_debt, st.debt_index, MAX_DEBT);

        // The price of an entry. An empty pool mints one share per unit of cash.
        function shares_for(cash: integer, st: pool_state): integer {
            if (st.shares <= 0 or st.value <= 0) return cash;
            return (cash.to_big_integer() * st.shares.to_big_integer() / st.value.to_big_integer()).to_integer();
        }

        // The price of an exit, out of the same state an entry is priced from.
        function cash_for(shares: integer, st: pool_state): integer {
            if (st.shares <= 0) return 0;
            return (shares.to_big_integer() * st.value.to_big_integer() / st.shares.to_big_integer()).to_integer();
        }

        // ------------------------------- LOOKUPS ------------------------------------

        function account_of(owner: byte_array): account =
            require(account @? { .owner == owner }, "register an account first");

        function loan_of(borrower: byte_array): loan {
            val l = loan @? { .borrower == borrower };
            if (l != null) return l;
            return create loan(borrower = borrower);
        }

        function lender_of(owner: byte_array): lender {
            val l = lender @? { .owner == owner };
            if (l != null) return l;
            return create lender(owner = owner, shares = 0);
        }

        // A price that is missing or stale is not a price: everything that depends on
        // one refuses rather than using the last number it saw.
        function fresh_price(): integer {
            require(price_feed.price > 0, "no price posted yet");
            require(op_context.last_block_time - price_feed.updated_at <= MAX_PRICE_AGE_MS, "price is stale");
            return price_feed.price;
        }

        function collateral_value(tokens: integer, price: integer): integer {
            require(tokens >= 0 and tokens <= MAX_AMOUNT, "collateral out of range");
            return tokens * price / PRICE_SCALE;
        }

        // A position is liquidatable when its debt has passed LIQUIDATION_THRESHOLD_BPS
        // of what its collateral is worth right now. It takes the pool_state, so it
        // cannot be asked about a stale debt.
        function is_liquidatable(l: loan, st: pool_state, price: integer): boolean {
            val owed = debt_of(l, st);
            if (owed <= 0) return false;
            return owed * BPS > collateral_value(l.collateral, price) * LIQUIDATION_THRESHOLD_BPS;
        }

        // ------------------------------- ORACLE ------------------------------------

        // The oracle key is configured, never a parameter and never in source.
        operation set_price(new_price: integer) {
            require(op_context.is_signer(chain_context.args.oracle_pubkey), "not the oracle");
            require(new_price > 0, "price must be positive");
            require(new_price <= MAX_PRICE, "price too high");
            val now = op_context.last_block_time;
            if (price_feed.price > 0) {
                require(
                    now - price_feed.updated_at >= MIN_PRICE_UPDATE_INTERVAL_MS,
                    "price posted too soon"
                );
                val previous = price_feed.price;
                val move = if (new_price > previous) new_price - previous else previous - new_price;
                require(move * BPS <= previous * MAX_PRICE_MOVE_BPS, "price move too large");
            }
            price_feed.price = new_price;
            price_feed.updated_at = now;
        }

        // ------------------------------ ACCOUNTS -----------------------------------

        operation register_account() {
            val acc = auth.authenticate();
            require(account @? { .owner == acc.id } == null, "already registered");
            create account(owner = acc.id, cash = WELCOME_CASH, tokens = WELCOME_TOKENS);
        }

        // ------------------------------- LENDING -----------------------------------

        operation deposit_cash(amount: integer) {
            val acc = auth.authenticate();
            val me = account_of(acc.id);
            require(amount > 0, "amount must be positive");
            require(amount <= MAX_AMOUNT, "amount too large");
            require(me.cash >= amount, "insufficient cash");
            val st = pool_now();
            require(st.shares > 0 or amount >= MIN_INITIAL_DEPOSIT, "the first deposit is too small to seed the pool");
            val minted = shares_for(amount, st);
            require(minted > 0, "deposit too small to mint a share");
            val position = lender_of(acc.id);
            // The depositor's cash is the pool's credit, in the same operation.
            update me ( .cash -= amount );
            pool.cash_available += amount;
            pool.total_shares += minted;
            update position ( .shares += minted );
        }

        operation withdraw_cash(shares: integer) {
            val acc = auth.authenticate();
            val me = account_of(acc.id);
            val position = lender_of(acc.id);
            require(shares > 0, "shares must be positive");
            require(position.shares >= shares, "not enough shares");
            val st = pool_now();
            require(st.shares > 0, "pool is empty");
            val amount = cash_for(shares, st);
            require(amount > 0, "nothing to withdraw");
            // Only cash the pool actually holds can leave it - what is out on loan is
            // not withdrawable until it is repaid. This is FIRST-COME-FIRST-SERVED, and
            // it is only fair because `amount` is priced out of a pool_state that has
            // already written unrecoverable debt down: waiting costs you time, not
            // value. Remove recoverable_debt()'s cap and this line becomes the round-7
            // drain - the first caller paid in full at a price that counts debt nobody
            // will ever repay.
            require(pool.cash_available >= amount, "pool is illiquid - wait for repayments");
            update position ( .shares -= shares );
            pool.total_shares -= shares;
            pool.cash_available -= amount;
            update me ( .cash += amount );
        }

        // ------------------------------ BORROWING ----------------------------------

        operation add_collateral(amount: integer) {
            val acc = auth.authenticate();
            val me = account_of(acc.id);
            require(amount > 0, "amount must be positive");
            require(amount <= MAX_AMOUNT, "amount too large");
            require(me.tokens >= amount, "insufficient tokens");
            val l = loan_of(acc.id);
            require(l.collateral + amount <= MAX_AMOUNT, "collateral too large");
            update me ( .tokens -= amount );
            update l ( .collateral += amount );
            // The pool's aggregate moves in the SAME operation as the position's.
            pool.total_collateral += amount;
        }

        operation remove_collateral(amount: integer) {
            val acc = auth.authenticate();
            val me = account_of(acc.id);
            val l = loan_of(acc.id);
            require(amount > 0, "amount must be positive");
            require(l.collateral >= amount, "not that much collateral");
            val st = pool_now();
            val owed = debt_of(l, st);
            if (owed > 0) {
                // Withdrawing collateral must leave the position within the borrow limit
                // at a FRESH price, never the last one seen.
                val price = fresh_price();
                val remaining = collateral_value(l.collateral - amount, price);
                require(owed * BPS <= remaining * MAX_LTV_BPS, "that would put the position under water");
            }
            update l ( .collateral -= amount );
            update me ( .tokens += amount );
            pool.total_collateral -= amount;
        }

        operation borrow(amount: integer) {
            val acc = auth.authenticate();
            val me = account_of(acc.id);
            require(amount > 0, "amount must be positive");
            require(amount <= MAX_AMOUNT, "amount too large");
            val l = loan_of(acc.id);
            val st = pool_now();
            val price = fresh_price();
            val owed = debt_of(l, st);
            val borrow_limit = collateral_value(l.collateral, price) * MAX_LTV_BPS / BPS;
            require(owed + amount <= borrow_limit, "over the borrow limit");
            require(owed + amount <= MAX_AMOUNT, "debt too large");
            require(pool.cash_available >= amount, "pool is illiquid");
            // What the position records is at least what left the pool.
            val added = to_scaled_up(amount, st.debt_index);
            // Cash leaves the pool and lands on the borrower; the debt records it.
            pool.cash_available -= amount;
            pool.total_scaled_debt += added;
            update l ( .scaled_debt += added );
            update me ( .cash += amount );
        }

        operation repay(amount: integer) {
            val acc = auth.authenticate();
            val me = account_of(acc.id);
            val l = loan_of(acc.id);
            require(amount > 0, "amount must be positive");
            require(l.scaled_debt > 0, "nothing owed");
            val st = pool_now();
            // Offering more than is owed pays what is owed: payment_for clamps to the
            // position and charges for exactly the units it retires.
            val p = payment_for(l, amount, st);
            require(me.cash >= p.cash, "insufficient cash");
            update me ( .cash -= p.cash );
            pool.cash_available += p.cash;
            // The interest inside this payment moves from OUTSTANDING to REALISED. Both
            // sides feed accrued_fee(), so the protocol's cut is exactly the same before
            // and after: a repayment moves NO pool value. That is why this template
            // needs no holding period and no exit fee - see seam 2.
            record_interest(p);
            pool.total_scaled_debt -= p.scaled;
            update l ( .scaled_debt -= p.scaled );
        }

        // ----------------------------- LIQUIDATION ---------------------------------

        operation liquidate(borrower: byte_array, repay_amount: integer) {
            val acc = auth.authenticate();
            val liquidator = account_of(acc.id);
            require(borrower != acc.id, "cannot liquidate your own position");
            val l = require(loan @? { .borrower == borrower }, "no such position");
            val st = pool_now();
            val price = fresh_price();
            require(is_liquidatable(l, st, price), "position is healthy");
            require(repay_amount > 0, "amount must be positive");
            val max_close = debt_of(l, st) * CLOSE_FACTOR_BPS / BPS;
            require(repay_amount <= max_close, "over the close factor");
            val p = payment_for(l, repay_amount, st);
            require(liquidator.cash >= p.cash, "insufficient cash");
            // Collateral worth the repayment plus the bonus, priced at the same fresh
            // price the health check used.
            // In big_integer and capped: MAX_DEBT is the index's ceiling times the
            // largest allowed borrow, so this product leaves 64 bits. An aborting
            // arithmetic here would make a position un-liquidatable, which is worse
            // than a seizure the collateral check refuses on the next line.
            val seize_value = p.cash.to_big_integer() * (BPS + LIQUIDATION_BONUS_BPS).to_big_integer() / BPS.to_big_integer();
            val seize_big = seize_value * PRICE_SCALE.to_big_integer() / price.to_big_integer();
            val seize_cap = MAX_AMOUNT.to_big_integer();
            val seize = (if (seize_big > seize_cap) seize_cap else seize_big).to_integer();
            require(seize > 0, "nothing to seize");
            require(seize <= l.collateral, "not enough collateral to cover the bonus");
            // Cash goes to the pool, collateral to the liquidator; the debt and the
            // pool's record of it fall by the same amount, all in this operation.
            update liquidator ( .cash -= p.cash, .tokens += seize );
            pool.cash_available += p.cash;
            record_interest(p);
            pool.total_scaled_debt -= p.scaled;
            update l ( .scaled_debt -= p.scaled, .collateral -= seize );
            pool.total_collateral -= seize;
        }

        // ------------------------------ PROTOCOL FEE --------------------------------

        // Only the configured protocol key may collect, and collecting moves cash that
        // pool_now() ALREADY excluded from what a share is a claim on: the cash leaves
        // and fee_collected rises by the SAME amount, so `value` does not move. A
        // collection is therefore not a step and cannot be straddled - the property
        // seam 2 is about, written out in two lines.
        operation collect_fees(amount: integer) {
            val acc = auth.authenticate();
            require(op_context.is_signer(chain_context.args.treasury_pubkey), "not the protocol");
            val me = account_of(acc.id);
            require(amount > 0, "amount must be positive");
            val st = pool_now();
            require(amount <= st.fee, "more than the fee accrued so far");
            require(pool.cash_available >= amount, "pool is illiquid - wait for repayments");
            pool.cash_available -= amount;
            pool.fee_collected += amount;
            update me ( .cash += amount );
        }

        // ------------------------------- QUERIES -----------------------------------
        // A query has no block clock, so nothing here can report a cash-denominated debt
        // or a share price: those exist only inside an operation's pool_state. What a
        // client needs to compute them itself - the scaled totals, the rate and the
        // pool's anchor - is all public below.

        query get_account(owner: byte_array) {
            val a = account @? { .owner == owner };
            return if (a != null) (cash = a.cash, tokens = a.tokens) else null;
        }

        query get_loan(borrower: byte_array) {
            val l = loan @? { .borrower == borrower };
            return if (l != null) (scaled_debt = l.scaled_debt, collateral = l.collateral) else null;
        }

        query get_shares(owner: byte_array): integer {
            val l = lender @? { .owner == owner };
            return if (l != null) l.shares else 0;
        }

        query get_pool() = (
            cash_available = pool.cash_available,
            total_scaled_debt = pool.total_scaled_debt,
            total_shares = pool.total_shares,
            total_collateral = pool.total_collateral,
            interest_realised = pool.interest_realised,
            fee_collected = pool.fee_collected,
            opened_at = pool.opened_at,
            index_scale = INDEX_SCALE,
            rate_bps_per_year = INTEREST_RATE_BPS_PER_YEAR,
            protocol_fee_bps = PROTOCOL_FEE_BPS
        );

        query get_price() = (price = price_feed.price, updated_at = price_feed.updated_at);

        query account_count(): integer = account @* {} ( .owner ).size();

        // INVARIANT: cash is never created. Every unit is either on an account or in the
        // pool's own balance; a loan MOVES cash and interest only creates a CLAIM.
        query cash_in_circulation(): integer {
            var total = pool.cash_available;
            for (c in account @* {} ( .cash )) total += c;
            return total;
        }

        // INVARIANT: collateral is never created either - it is on an account or locked
        // in a position.
        query tokens_in_circulation(): integer {
            var total = 0;
            for (t in account @* {} ( .tokens )) total += t;
            for (c in loan @* {} ( .collateral )) total += c;
            return total;
        }

        // INVARIANT: what the pool records as lent out is EXACTLY what the positions say
        // they owe - exactly, because both are in the same index units. A
        // cash-denominated version of this counter is what went stale in round 6.
        query scaled_debt_matches_positions(): boolean {
            var total = 0;
            for (s in loan @* {} ( .scaled_debt )) total += s;
            return total == pool.total_scaled_debt;
        }

        // INVARIANT: the aggregate the share price is bounded by is EXACTLY the sum of
        // the positions' collateral. A row this does not sum would let the pool value
        // debt that nothing backs - which is the round-7 exit race by another door.
        query collateral_matches_positions(): boolean {
            var total = 0;
            for (c in loan @* {} ( .collateral )) total += c;
            return total == pool.total_collateral;
        }
    """.trimIndent() + "\n"

    private fun lendingTestRell(): String = """
        @test module;

        // The lending template's invariant tests. They are real: FT4 test accounts,
        // signed operations, PostgreSQL - run via run_rell_tests (pass chromia.yml's
        // moduleArgs PLUS its test.moduleArgs block, which carries the oracle key the
        // tests sign with) or `chr test`.
        //
        // test_round6_jit_interest_capture_must_fail replays the adversary's drain
        // against this template step for step - the same 10000 cash, the same ten-year
        // wait, the same one-block round trip straddling the borrower's touch - and
        // requires the attacker to come out no better than they went in. It can only
        // pass while every entry and exit is priced through pool_now().

        import main;
        import lib.ft4.test.core.{ register_alice, register_bob, register_trudy, register_account_open, ft_auth_operation_for };
        // admin_priv_key() is defined in test.core.auth; importing it from the parent
        // module is ambiguous (FT4's own assets.rell imports it from ^.auth too).
        import lib.ft4.test.core.auth.{ admin_priv_key };

        // The oracle keypair: FT4's published test key, wired through test.moduleArgs
        // (lib.ft4.test.core.auth.admin_priv_key + main.oracle_pubkey). test.moduleArgs
        // puts the SAME key in the treasury role, which is a test convenience only -
        // chromia.yml's production block says to configure two different keys.
        function oracle(): rell.test.keypair =
            rell.test.keypair(priv = admin_priv_key(), pub = main.oracle_pubkey());

        function post_price(price: integer) {
            rell.test.tx().op(main.set_price(price)).nop().sign(oracle()).run();
        }

        function post_price_must_fail(price: integer, expected: text) {
            rell.test.tx().op(main.set_price(price)).nop().sign(oracle()).run_must_fail(expected);
        }

        function signed(keypair: rell.test.keypair, op: rell.test.op) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run();
        }

        function signed_must_fail(keypair: rell.test.keypair, op: rell.test.op, expected: text) {
            rell.test.tx()
                .op(ft_auth_operation_for(keypair.pub))
                .op(op)
                .nop()
                .sign(keypair)
                .run_must_fail(expected);
        }

        // Stamp the next block `ms` after the last one.
        //
        // A long jump makes the price STALE, and once any cash is out on loan a share
        // cannot be priced without pricing the collateral behind that debt - so the
        // tests below re-post the same number after a jump. Re-posting an unchanged
        // price is a 0% move and is always accepted once the interval has passed.
        function after(ms: integer) {
            rell.test.set_next_block_time_delta(ms);
            rell.test.block().run();
        }

        // Cash and collateral are never created: every unit is on an account or in the
        // pool, and what the pool records as lent out is EXACTLY what the positions say
        // they owe - exactly, because both are counted in the same index units. The
        // collateral aggregate is checked too: it is the ceiling on what the pool's debt
        // is worth, so a drift there is a wrong share price.
        function assert_conserved() {
            assert_equals(main.cash_in_circulation(), main.account_count() * main.WELCOME_CASH);
            assert_equals(main.tokens_in_circulation(), main.account_count() * main.WELCOME_TOKENS);
            assert_true(main.scaled_debt_matches_positions());
            assert_true(main.collateral_matches_positions());
        }

        val HOUR = 60 * 60 * 1000;

        // EXPLOIT MUST FAIL. Adversary round 6, corpus row r6-lending-jit-interest-capture:
        // the attacker deposits at a share price that has not seen ten years of pending
        // interest, waits one block for the borrower's next touch to land the accrual,
        // and withdraws at the fresh price - 10000 in, 11500 out, taken from the honest
        // lender. Here the entry is priced out of the SAME pool_state the exit will be,
        // so the round trip earns one block of interest, which is nothing.
        function test_round6_jit_interest_capture_must_fail() {
            val honest = register_alice();
            val borrower = register_bob();
            val attacker = register_trudy();
            signed(honest.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            signed(attacker.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);

            // The honest lender funds the pool and a borrower draws against collateral.
            signed(honest.keypair, main.deposit_cash(10000));
            signed(borrower.keypair, main.add_collateral(100));
            signed(borrower.keypair, main.borrow(6000));
            assert_equals(main.get_shares(honest.account.id), 10000);
            assert_equals(main.get_pool().total_scaled_debt, 6000);
            assert_conserved();

            // Ten years pass. 6000 at 5% simple is 3000 of interest the honest lender's
            // capital earned. Nobody has touched the position - and it does not matter,
            // because the index is a function of the clock, not of the touches.
            after(10 * main.YEAR_MS);
            post_price(100 * main.PRICE_SCALE);
            assert_equals(main.get_pool().total_scaled_debt, 6000);

            // The attacker buys in. The pool is already worth 4000 cash + 9000 debt,
            // less the 600 of that interest the protocol's accrued fee has taken: 12400.
            signed(attacker.keypair, main.deposit_cash(10000));
            val bought = main.get_shares(attacker.account.id);
            assert_conserved();

            // The borrower touches the position - in round 6 this is the block that
            // landed the accrual and made the attacker's shares jump.
            signed(borrower.keypair, main.repay(2));

            // The attacker exits after one block in the pool.
            signed(attacker.keypair, main.withdraw_cash(bought));
            val out = main.get_account(attacker.account.id)!!.cash;
            // THE DRAIN, REFUSED: 10000 went in and no more than 10000 comes out.
            assert_equals(out > main.WELCOME_CASH, false);
            assert_equals(out, 9999);
            assert_equals(main.get_shares(attacker.account.id), 0);
            // ...and the reason is the entry price: 10000 cash bought 8064 shares, not
            // 10000, because the pool was already worth 12400 when they deposited.
            assert_equals(bought, 8064);
            assert_conserved();

            // The honest lender's yield is still theirs: 30% of their position comes
            // back as 3720, not 3000. (3000 of interest less the protocol's 600, on a
            // pool the attacker's round trip left exactly as they found it.)
            signed(honest.keypair, main.withdraw_cash(3000));
            assert_equals(main.get_account(honest.account.id)!!.cash, 3720);
            assert_conserved();
        }

        // EXPLOIT MUST FAIL. Adversary round 7, corpus row r7-lending-fee-step-jit-capture:
        // this template's own header used to say a step in pool value needs "an entry/exit
        // fee or a minimum holding period". Round 7 built exactly that - a 20% cut of
        // interest taken AT THE REPAYMENT BLOCK, plus a 24-hour holding period - and the
        // pool drained anyway, honest 10901 against attacker 11500, because the attack is
        // an EXIT by a lender years past any period. Here the fee accrues with the clock
        // and is netted out of pool_state, so the block the repayment lands in is worth
        // nothing to anybody. It can only pass while accrued_fee() counts the interest
        // still OUTSTANDING and not just the interest already paid.
        function test_round7_fee_step_jit_capture_must_fail() {
            val honest = register_alice();
            val attacker = register_bob();
            // The attacker also controls the borrower, so they choose the block the
            // repayment - and in round 7 the fee step - lands in.
            val borrower = register_trudy();
            signed(honest.keypair, main.register_account());
            signed(attacker.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);

            // Two equal lenders, one borrower drawing the full limit against collateral.
            signed(honest.keypair, main.deposit_cash(10000));
            signed(attacker.keypair, main.deposit_cash(10000));
            signed(borrower.keypair, main.add_collateral(100));
            signed(borrower.keypair, main.borrow(6000));
            assert_equals(main.get_shares(honest.account.id), 10000);
            assert_equals(main.get_shares(attacker.account.id), 10000);
            assert_conserved();

            // Ten years of interest: 3000 on 6000, of which 600 is the protocol's.
            after(10 * main.YEAR_MS);
            post_price(100 * main.PRICE_SCALE);

            // THE ATTACK: exit in the block BEFORE the fee-bearing repayment, so the
            // step falls entirely on the lender still in the pool.
            signed(attacker.keypair, main.withdraw_cash(10000));
            val attacker_out = main.get_account(attacker.account.id)!!.cash;
            assert_conserved();

            // The repayment the attacker was dodging.
            signed(borrower.keypair, main.repay(20000));
            assert_equals(main.get_loan(borrower.account.id)!!.scaled_debt, 0);
            assert_equals(main.get_pool().interest_realised, 3001);

            // The honest lender exits afterwards.
            signed(honest.keypair, main.withdraw_cash(10000));
            val honest_out = main.get_account(honest.account.id)!!.cash;

            // THE DRAIN, REFUSED: the early exit is worth nothing. Both lenders put in
            // 10000 and both take out the same, because the fee was already priced into
            // the share the attacker sold.
            assert_equals(attacker_out > honest_out, false);
            assert_equals(attacker_out, 11200);
            assert_equals(honest_out, 11201);
            assert_conserved();

            // ...and the protocol's 600 is there to be collected - by the configured key
            // and nobody else. Collecting moves cash the share price already excluded,
            // so it is not a step either.
            val protocol = register_account_open(oracle());
            signed(oracle(), main.register_account());
            signed_must_fail(honest.keypair, main.collect_fees(1), "not the protocol");
            signed_must_fail(oracle(), main.collect_fees(601), "more than the fee accrued so far");
            signed(oracle(), main.collect_fees(600));
            assert_equals(main.get_account(protocol.account.id)!!.cash, main.WELCOME_CASH + 600);
            assert_equals(main.get_pool().cash_available, 0);
            assert_conserved();
        }

        // EXPLOIT MUST FAIL. Adversary round 7, corpus row r7-lending-bad-debt-exit-race:
        // 13920 of 14000 taken from an honest lender on this template EXACTLY AS SHIPPED,
        // with nothing minted and every conservation invariant green. pool_now() valued
        // every index unit of debt at face, so a position a thousand years past any
        // liquidator's reach left the share price UNCHANGED; the pool was merely illiquid
        // at a price that counted 306000 of unrecoverable debt as good, and withdraw_cash
        // is first-come-first-served. Here recoverable_debt() caps the pool's debt at what
        // its collateral can repay, so the two lenders are priced identically no matter
        // who moves first.
        function test_round7_bad_debt_exit_race_must_fail() {
            val honest = register_alice();
            val attacker = register_bob();
            val borrower = register_trudy();
            signed(honest.keypair, main.register_account());
            signed(attacker.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);

            signed(honest.keypair, main.deposit_cash(10000));
            signed(attacker.keypair, main.deposit_cash(10000));
            // The attacker's own second account posts the minimum it can and draws the
            // full 60% against it, leaving 14000 of real cash in the pool.
            signed(borrower.keypair, main.add_collateral(100));
            signed(borrower.keypair, main.borrow(6000));
            assert_equals(main.get_pool().cash_available, 14000);
            assert_conserved();

            // A thousand years takes the position to 306000 of debt against collateral
            // worth 10000 - beyond any liquidator, because the 10% bonus has to come out
            // of the borrower's own collateral.
            after(1000 * main.YEAR_MS);
            post_price(100 * main.PRICE_SCALE);
            assert_equals(main.get_pool().total_scaled_debt, 6000);

            // THE ATTACK: 1000 shares, which at round 7's face-value price were worth
            // most of the 14000 the pool held and left the honest lender 80.
            signed(attacker.keypair, main.withdraw_cash(1000));
            val attacker_out = main.get_account(attacker.account.id)!!.cash - (main.WELCOME_CASH - 10000);
            // THE DRAIN, REFUSED: 1000 shares are worth 1160, not 13000, because the
            // price now counts the debt at what its collateral can repay.
            assert_equals(attacker_out, 1160);

            // ...and the honest lender's identical 1000 shares are still worth the same.
            // The exit ORDER decides nothing.
            signed(honest.keypair, main.withdraw_cash(1000));
            val honest_out = main.get_account(honest.account.id)!!.cash - (main.WELCOME_CASH - 10000);
            assert_equals(honest_out, 1160);
            assert_equals(attacker_out > honest_out, false);
            assert_conserved();
        }

        // REFUSED. Liquidating a healthy position, tried with every argument the caller
        // controls: an absurd repay amount, a zero, a negative, a position with no debt,
        // the caller's own position, and one that a bounded price post has merely moved
        // close to the line.
        function test_healthy_position_cannot_be_liquidated() {
            val lender = register_alice();
            val victim = register_bob();
            val attacker = register_trudy();
            signed(lender.keypair, main.register_account());
            signed(victim.keypair, main.register_account());
            signed(attacker.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);
            signed(lender.keypair, main.deposit_cash(10000));
            signed(victim.keypair, main.add_collateral(100));
            signed(victim.keypair, main.borrow(6000));

            // A repayment size that WOULD be accepted on a position that really was
            // under water - so this refusal is the health check and nothing else.
            signed_must_fail(attacker.keypair, main.liquidate(victim.account.id, 2000), "position is healthy");
            signed_must_fail(attacker.keypair, main.liquidate(victim.account.id, 1), "position is healthy");
            signed_must_fail(attacker.keypair, main.liquidate(victim.account.id, 999999999), "position is healthy");
            signed_must_fail(attacker.keypair, main.liquidate(victim.account.id, 0), "position is healthy");
            signed_must_fail(attacker.keypair, main.liquidate(victim.account.id, -1), "position is healthy");
            // A lender who never borrowed has no position at all.
            signed_must_fail(attacker.keypair, main.liquidate(lender.account.id, 1), "no such position");
            // Nor may a borrower liquidate themselves for the bonus.
            signed_must_fail(victim.keypair, main.liquidate(victim.account.id, 1), "cannot liquidate your own position");

            // One bounded price post is not enough to cross the threshold either.
            after(HOUR);
            post_price(80 * main.PRICE_SCALE);
            signed_must_fail(attacker.keypair, main.liquidate(victim.account.id, 1), "position is healthy");
            assert_conserved();
        }

        // REFUSED. The borrower tries to make an under-water position untouchable: by
        // walking the collateral out, by borrowing more against it, and by leaving it
        // alone in the hope the interest arithmetic overflows and makes liquidate()
        // abort.
        function test_under_water_position_cannot_hide() {
            val lender = register_alice();
            val borrower = register_bob();
            val liquidator = register_trudy();
            signed(lender.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            signed(liquidator.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);
            signed(lender.keypair, main.deposit_cash(10000));
            signed(borrower.keypair, main.add_collateral(100));
            signed(borrower.keypair, main.borrow(6000));
            after(HOUR);
            post_price(80 * main.PRICE_SCALE);
            after(HOUR);
            post_price(70 * main.PRICE_SCALE);

            // Neither escape route is open.
            signed_must_fail(borrower.keypair, main.remove_collateral(1), "that would put the position under water");
            signed_must_fail(borrower.keypair, main.borrow(1), "over the borrow limit");
            // Nor does waiting: the index saturates instead of aborting, so a very old
            // position is still priceable and therefore still liquidatable. (While the
            // oracle is silent NOBODY is liquidatable - the freshness check halts
            // liquidation too, which is a deliberate trade and is documented as one.)
            after(1000 * main.YEAR_MS);
            signed_must_fail(liquidator.keypair, main.liquidate(borrower.account.id, 1000), "price is stale");
            post_price(70 * main.PRICE_SCALE);
            signed(liquidator.keypair, main.liquidate(borrower.account.id, 1000));
            assert_true(main.get_loan(borrower.account.id)!!.scaled_debt < 6000);
            assert_true(main.get_loan(borrower.account.id)!!.collateral < 100);
            assert_conserved();
        }

        // REFUSED. The 10% bonus looks like free money to a pair of accounts under one
        // hand. It is not: the bonus is paid out of the liquidated position's OWN
        // collateral, so every token stays inside the pair and the only cash that moves
        // is the repayment the pair made.
        function test_self_liquidation_nets_nothing() {
            val lender = register_alice();
            val self_a = register_bob();
            val self_b = register_trudy();
            signed(lender.keypair, main.register_account());
            signed(self_a.keypair, main.register_account());
            signed(self_b.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);
            signed(lender.keypair, main.deposit_cash(10000));
            signed(self_a.keypair, main.add_collateral(100));
            signed(self_a.keypair, main.borrow(6000));
            after(HOUR);
            post_price(80 * main.PRICE_SCALE);
            after(HOUR);
            post_price(70 * main.PRICE_SCALE);

            val cash_before = main.get_account(self_a.account.id)!!.cash + main.get_account(self_b.account.id)!!.cash;
            val tokens_before = main.get_account(self_a.account.id)!!.tokens
                + main.get_account(self_b.account.id)!!.tokens
                + main.get_loan(self_a.account.id)!!.collateral;
            val scaled_before = main.get_loan(self_a.account.id)!!.scaled_debt;

            signed(self_b.keypair, main.liquidate(self_a.account.id, 3000));

            val cash_after = main.get_account(self_a.account.id)!!.cash + main.get_account(self_b.account.id)!!.cash;
            val tokens_after = main.get_account(self_a.account.id)!!.tokens
                + main.get_account(self_b.account.id)!!.tokens
                + main.get_loan(self_a.account.id)!!.collateral;
            val scaled_after = main.get_loan(self_a.account.id)!!.scaled_debt;

            // Every seized token, bonus included, stayed inside the pair...
            assert_equals(tokens_before, tokens_after);
            // ...the pair paid out exactly the repayment and got nothing else back...
            assert_equals(cash_before - cash_after, 3000);
            // ...and it bought at most 3000 of debt relief. The bonus is not income.
            assert_true(scaled_before - scaled_after <= 3000);
            // Half the position is the most one liquidation may close.
            signed_must_fail(self_b.keypair, main.liquidate(self_a.account.id, 3000), "over the close factor");
            assert_conserved();
        }

        // REFUSED. The ERC-4626 first-depositor steal: seed the pool with one unit,
        // inflate its value so a later depositor's shares round to zero, and keep their
        // cash. The seed itself is refused, and so is any deposit that would mint zero
        // shares - the victim keeps their cash instead of handing it over.
        function test_first_depositor_inflation_refuses_instead_of_swallowing() {
            val attacker = register_alice();
            val victim = register_bob();
            val borrower = register_trudy();
            signed(attacker.keypair, main.register_account());
            signed(victim.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);

            // The one-unit seed the attack starts from is not allowed to exist.
            signed_must_fail(attacker.keypair, main.deposit_cash(1), "the first deposit is too small to seed the pool");

            // So the attacker seeds legitimately and grows the pool with real interest.
            signed(attacker.keypair, main.deposit_cash(1000));
            assert_equals(main.get_shares(attacker.account.id), 1000);
            signed(attacker.keypair, main.deposit_cash(5000));
            signed(borrower.keypair, main.add_collateral(100));
            signed(borrower.keypair, main.borrow(6000));
            after(100 * main.YEAR_MS);
            post_price(100 * main.PRICE_SCALE);
            assert_equals(main.get_shares(attacker.account.id), 6000);

            // A deposit that would mint zero shares aborts - it is not swallowed.
            signed_must_fail(victim.keypair, main.deposit_cash(1), "deposit too small to mint a share");
            // ...and a deposit large enough to mint at least one share is priced at the
            // pool's REAL value, so the round trip returns what went in bar rounding
            // dust: the attacker's seed captures none of it.
            signed(victim.keypair, main.deposit_cash(10000));
            val minted = main.get_shares(victim.account.id);
            assert_true(minted > 0);
            signed(victim.keypair, main.withdraw_cash(minted));
            val back = main.get_account(victim.account.id)!!.cash;
            assert_true(back >= 9950 and back <= 10000);
            assert_equals(main.get_shares(attacker.account.id), 6000);
            assert_conserved();
        }

        // REFUSED. The borrow limit is checked against the position's WHOLE debt every
        // time, so slicing it into pieces gains nothing.
        function test_borrow_limit_cannot_be_sliced() {
            val lender = register_alice();
            val borrower = register_bob();
            signed(lender.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);
            signed(lender.keypair, main.deposit_cash(10000));
            signed(borrower.keypair, main.add_collateral(100));

            var i = 0;
            while (i < 6) {
                signed(borrower.keypair, main.borrow(1000));
                i += 1;
            }
            assert_equals(main.get_loan(borrower.account.id)!!.scaled_debt, 6000);
            signed_must_fail(borrower.keypair, main.borrow(1), "over the borrow limit");
            // Nor can collateral be walked out from under the debt.
            signed_must_fail(borrower.keypair, main.remove_collateral(1), "that would put the position under water");
            assert_conserved();
        }

        // REFUSED. No price, no lending; a price only the oracle may post; a post that
        // jumps or comes too soon; and a price older than a day halts everything that
        // needs one rather than falling back on the last number it saw.
        function test_stale_or_missing_price_halts_lending() {
            val lender = register_alice();
            val borrower = register_bob();
            signed(lender.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            signed(lender.keypair, main.deposit_cash(10000));
            signed(borrower.keypair, main.add_collateral(100));
            signed_must_fail(borrower.keypair, main.borrow(1), "no price posted yet");

            // The oracle is the configured key and nobody else.
            rell.test.tx().op(main.set_price(100 * main.PRICE_SCALE)).sign(lender.keypair).run_must_fail("not the oracle");
            post_price(100 * main.PRICE_SCALE);

            // A second post inside the interval is refused, and after the interval a
            // move beyond the bound still is.
            post_price_must_fail(90 * main.PRICE_SCALE, "price posted too soon");
            after(HOUR);
            post_price_must_fail(1, "price move too large");
            post_price(80 * main.PRICE_SCALE);
            assert_equals(main.get_price().price, 80 * main.PRICE_SCALE);

            signed(borrower.keypair, main.borrow(1000));
            after(main.MAX_PRICE_AGE_MS + 1);
            signed_must_fail(borrower.keypair, main.borrow(1), "price is stale");
            signed_must_fail(borrower.keypair, main.remove_collateral(1), "price is stale");
            assert_conserved();
        }

        // CONSERVATION. A full cycle - deposit, borrow, two years, repay in full,
        // withdraw - moves the interest from the borrower to the lender and creates
        // nothing: 501 out of one pocket and into the other (5000 at 5% for two years,
        // plus the thirty seconds of block time the setup took, rounded up in the
        // pool's favour), with the pool empty at the end.
        function test_interest_moves_only_from_borrower_to_lender() {
            val lender = register_alice();
            val borrower = register_bob();
            signed(lender.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            post_price(100 * main.PRICE_SCALE);
            assert_conserved();

            signed(lender.keypair, main.deposit_cash(10000));
            signed(borrower.keypair, main.add_collateral(100));
            signed(borrower.keypair, main.borrow(5000));
            assert_conserved();

            after(2 * main.YEAR_MS);
            post_price(100 * main.PRICE_SCALE);
            // Offering more than is owed pays what is owed and clears the position.
            signed(borrower.keypair, main.repay(10000));
            assert_equals(main.get_loan(borrower.account.id)!!.scaled_debt, 0);
            assert_equals(main.get_pool().total_scaled_debt, 0);
            assert_equals(main.get_account(borrower.account.id)!!.cash, 9499);
            assert_equals(main.get_pool().interest_realised, 501);
            signed_must_fail(borrower.keypair, main.repay(1), "nothing owed");
            assert_conserved();

            // 501 of interest, of which PROTOCOL_FEE_BPS (20%) is the protocol's: the
            // lender's share is 401, and the 100 left in the pool is not theirs.
            signed(lender.keypair, main.withdraw_cash(10000));
            assert_equals(main.get_account(lender.account.id)!!.cash, 10401);
            assert_equals(main.get_pool().total_shares, 0);
            assert_equals(main.get_pool().cash_available, 100);
            // Nothing was created: the borrower's 501 is the lender's 401 plus the
            // protocol's 100, and the protocol's 100 is all that is collectable.
            val protocol = register_account_open(oracle());
            signed(oracle(), main.register_account());
            signed_must_fail(oracle(), main.collect_fees(101), "more than the fee accrued so far");
            signed(oracle(), main.collect_fees(100));
            assert_equals(main.get_account(protocol.account.id)!!.cash, main.WELCOME_CASH + 100);
            assert_equals(main.get_pool().cash_available, 0);
            assert_conserved();

            // The borrower's collateral comes back once the debt is gone.
            signed(borrower.keypair, main.remove_collateral(100));
            assert_equals(main.get_account(borrower.account.id)!!.tokens, 100);
            assert_conserved();
        }
    """.trimIndent() + "\n"
}
