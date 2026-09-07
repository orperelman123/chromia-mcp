package org.chromia.tools

/**
 * The long form of a tool's guidance, moved out of the advertised `tools/list`
 * description by audit finding F9 (measured: 115,908 B / ~29k tokens before an
 * agent's first useful call). Nothing here was deleted - `describe_tool{tool}`
 * and `chromia_help{topic}` return it verbatim, and
 * ToolDescriptionBudgetTest pins that every sentence that left a description is
 * still reachable by name.
 */
object ToolDocs {

    /** Full advertised description of `check_dapp_project` before F9 moved it here. */
    private val CHECK_DAPP_PROJECT: String = """
        One-call project gate. Takes a chromia.yml string plus one or more .rell file contents and runs
        the FULL check: validate_chromia_yml + check_ft4_imports + rell_check (real compilation, FT4
        imports included) + rell_security_check when it compiles. Returns combined {ok, errors, warnings, notes};
        ok=true means the project parses, compiles, and has no CRITICAL/HIGH security findings.
        `yaml` is optional: when omitted, a minimal default chromia.yml at the current pins
        (rellVersion ${DappScaffold.RELL_VERSION}) is used and noted in the output.
        allowAdminModules:true (default false) downgrades banned admin-module findings from errors
        to warnings - for admin/ops tooling only, never for production dApps.
        When the yaml declares multiple blockchains whose modules match submitted files, each chain's
        module set is compiled separately (like chr build) so per-chain alternative modules do not
        false-red with mount-name conflicts; notes says so.
        Submitted lib/ft4/ or lib/iccf/ files identical to the vendored sources compile but are exempt
        from the import/security scanners (FT4's own sources legitimately contain e.g. ras_open); a
        file differing from the vendored copy is scanned like app code and noted. Other lib/** files
        (lib/ft3, lib/icmf, ...) are skipped as third-party library code and noted. @test modules are
        exempt from the forbidden-module scan (test code legitimately exercises admin modules).
        Use this as the single pre-deploy gate instead of calling the four tools separately.
        Read-only: does not write files, run chr, generate keys, or send signed transactions.
    """.trimIndent()

    /** Full advertised description of `chr_deploy_help` before F9 moved it here. */
    private val CHR_DEPLOY_HELP: String = """
        Official Chromia CLI 0.33.x chr deployment flag help: create / update / inspect plus
        read-only info / proposal list|info / voterset info|list (no key-pair flags).
        Includes -y, --key-id (reference only; does not generate a key), schema-compare DROP warning,
        and that create writes deployments.<net>.chains back. Optional container: field after a
        Vault/PMC lease — does not invent a lease id or BRID.
        Also returns official chromia.yml database / test section snippets (Java 21+, Postgres 16+). Official BUILD vault-listing read-only find_dapp_details query (skip chr tx writes and sample 64-hex). Official BUILD testnet list-dapp-vault (200) checkmark / setUpMocks.ts / hardcoded vs db names. Official intro/installation/postchain-clients is 404; /build/clients/overview wins. Official BUILD testnet deploy-dapp / getting-started (200): create write-back wins; getting-started mainnet wording on TESTNET page is stale. Official BUILD get-tchr-binance (200) BSC vs Chromia tCHR differences; deploy-dapp explorer verify explorer.chromia.com. Official BUILD connect-client started (mainnet creatClient typo). Official BUILD deploy-frontend-dapp webStatic (200).
        Does not shell out to chr and does not send signed transactions.
        Skips vote/propose/pause/resume/remove and hidden lease-info / remove-container.
    """.trimIndent()

    /** Full advertised description of `chromia_cookbook_help` before F9 moved it here. */
    private val CHROMIA_COOKBOOK_HELP: String = """
        Official Chromia BUILD cookbook help for building a dapp: queries, client reads, and tests.
        Official pages only, including /rell/tests builders, asserts, and @disabled.
        Official cookbook run-queries is HELP ONLY (skip sample BRID hex).
        Official cookbook run-tests is HELP ONLY (skip sample BRID hex; chr test --sql-log removed).
        Official cookbook create-rell-dapp is HELP ONLY (skip sample BRID hex; --local skipped).
        Official cookbook overview is HELP ONLY (Welcome to the Chromia Cookbook; skip sample BRID hex).
        Official cookbook CLI is HELP ONLY (CLI; skip sample BRID hex; run-operations this signs).
        Official cookbook query-creation is HELP ONLY (Create queries; skip sample BRID hex; get-account-balance EVM key pair).
        Official cookbook get-account-balance is HELP ONLY (How to get account balance; EVM key pair).
        Official cookbook account-creation is HELP ONLY (Account creation; this signs).
        Official cookbook transaction-creation is HELP ONLY (Create & manage transactions; this signs).
        Official cookbook run-operations is HELP ONLY (How to run operations; this signs).
        Skips recipes that sign a live tx, cookbook-only flags, non-schema keys, and printed sample keys.
        Does not run chr, generate a key, or send signed transactions.
    """.trimIndent()

    /** Full advertised description of `chromia_dapp_query` before F9 moved it here. */
    private val CHROMIA_DAPP_QUERY: String = """
        **WORKFLOW FOR AI AGENTS:**
        1. First, obtain the blockchain RID using filter_blockchains tool
        2. Second run "rell.get_app_structure" query using chromia_dapp_query tool which returns dApp structure of the blockchain (queries, modules, entities)
        3. Third, look for the query from response of step 2 that user is looking for
            - When looking for a follow-up query from the structure result (step 2):
                - Use mount name + '.' + the query name to execute the follow-up query. e.g. "module1.query_name"
                - Fill in the required arguments first based on the parameter definitions from the structure result
        4. Use TODO to track the progress of verifying if query exists and executing it
        5. When getting a response as json file, do to write scripts in Python/Javascript etc,
             to parse it, use bash or jq
        6. Always cache the result of the previous query in case follow-up questions are asked  

        **SECURITY RULES:**
        - NEVER read the contents of secret files, private keys, or generated keypairs
        - NEVER expose or display private keys or sensitive cryptographic data

        **RETURNS:**
        - Query results from the specified dApp in JSON format
        - For default query: Complete dApp structure with all available queries/operations entities... with their parameter names and types
        - For custom queries: Results based on the specific query executed

        **USE CASES:**
        - Discover available queries/operations by using default rell.get_app_structure query
        - Execute custom dApp queries with specific parameters
        - Analyze dApp architecture and data models
        - Get real-time data from blockchain applications

        The whole query is bounded by an overall deadline (default 20s, env
        CHROMIA_MCP_QUERY_DEADLINE_MS, capped at 45s) so it can never hang: a chain the queried
        nodes do not serve returns an actionable error (usually: pass the dapp's own node URL
        as network) instead of crawling every endpoint for minutes.
    """.trimIndent()

    /** Full advertised description of `deploy_testnet_chain` before F9 moved it here. */
    private val DEPLOY_TESTNET_CHAIN: String = """
        Deploy dapp sources to a leased TESTNET container with no human involved. Order is fixed and
        gated: (1) the security gate (rell_security_check) - CRITICAL/HIGH findings refuse the deploy
        even on testnet; (2) the compile + config gate (deployment_preflight with the sources) - any
        blocker refuses, including a module whose `struct module_args` has no default and no entry
        under blockchains.<name>.moduleArgs (the oracle/vault/lending/stablecoin templates leave
        main.oracle_pubkey deliberately unset - set it before deploying); (3) only then `chr install` (when chromia.yml declares libs - a fresh project
        has no src/lib and the build fails without it) followed by `chr deployment create|update
        --settings chromia.yml --network testnet --blockchain <name>` run headlessly, signed via POSTCHAIN_CLIENT_PRIVKEY from the
        server-held deploy key for the container (stored by provision_testnet_container, or env
        CHROMIA_TESTNET_DEPLOY_PRIVKEY); (4) the new chain BRID is read back and a live height probe
        verifies it (a fresh chain can take minutes to start - that is reported honestly, with
        verify_deployment as the follow-up). Provide chromiaYml or let the tool generate one from the
        scaffold pins plus the container name; a generated config probes `chr --version` and pins
        compile.rellVersion to the Rell the INSTALLED CLI actually bundles (falling back to the
        scaffold default when chr cannot be probed - the choice and its source are reported), and
        omits deployments.testnet.chains, which a first create must not carry. dryRun defaults to
        TRUE: gates run, nothing deploys. chr is resolved via CHROMIA_CHR_BIN, then a PATH search
        honoring PATHEXT on Windows (.cmd/.bat shims run via `cmd /c`); the resolution used is
        reported. If chr is missing or no deploy key is held, the tool names the exact blocked step
        instead of pretending. Key material never appears in any output.
    """.trimIndent()

    /** Full advertised description of `deployment_preflight` before F9 moved it here. */
    private val DEPLOYMENT_PREFLIGHT: String = """
        Catch every deployment problem BEFORE a human burns a lease step or signs anything.
        Given chromia.yml text and a deployment target name (e.g. "testnet" / "mainnet"), checks:
        (1) the deployments.<target> block - brid present/well-formed, url valid, container a real
        lease id (not a placeholder), chains matching declared blockchains; (2) reachability - a
        read-only height probe of the block's Directory Chain BRID against its own URL(s), with
        classified failure hints, bounded by one overall deadline shared by all probed URLs
        (default 20s, env CHROMIA_MCP_PREFLIGHT_PROBE_DEADLINE_MS, capped at 45s) so an
        unserved chain can never hang the tool; (3) network sanity - a testnet/mainnet target whose brid or url
        points at the OTHER network is a HIGH blocker (wrong-network deploys are unrecoverable);
        (4) the source gate when `rell` is supplied - code must compile, and for MAINNET targets
        CRITICAL/HIGH security findings are blockers (warnings for testnet); (5) production pins
        (rellVersion the CLI accepts, merkle_hash_version) - blockers for mainnet, warnings otherwise
        (`strict` overrides). Returns {ready, target, network, findings, blockers, nextAction, notes};
        ready=true only with zero blockers - a MAINNET target without `rell` stays blocked until the
        source gate runs, while other targets can be ready with the skipped source gate explicitly
        called out in notes (nothing skipped is silently vouched for). When ready, nextAction is the
        exact `chr deployment create|update --settings chromia.yml --network <target> --blockchain
        <name>` command. Read-only: no keys, no signing, no network writes.
    """.trimIndent()

    /** Full advertised description of `filter_blockchains` before F9 moved it here. */
    private val FILTER_BLOCKCHAINS: String = """
        - Get a comprehensive list of all blockchains with advanced filtering capabilities
        - **Primary use case: Finding blockchains by name** - this is the main tool for blockchain name lookups
        - Returns detailed information about each blockchain including:
            - Unique RID for each blockchain
            - Names/aliases associated with each blockchain
            - The cluster each blockchain belongs to
            - Container information for each blockchain
            - Current operational state of each blockchain (RUNNING, REMOVED, PAUSED)
            - Whether each blockchain is a system chain or user application
        - Supports comprehensive filtering options:
            - **Filter by name**: Find blockchains by exact or partial name match (main feature)
            - Filter by RID: Find specific blockchain by its RID
            - Filter by cluster: Find blockchains in specific clusters (e.g., 'pink', 'system')
            - Filter by container: Find blockchains in specific containers
            - Filter by state: Find blockchains by operational state (RUNNING, REMOVED, PAUSED).
              UPSTREAM LIMITATION, verified live 2026-09-07: the explorer answers INTERNAL_ERROR
              whenever this argument is supplied, with or without the others, so this one filter
              is unusable today through any client (docs/UPSTREAM.md #3b). Filter by
              cluster/container/name and read `state` off the rows instead.
            - Filter by system status: Find system chains vs user applications
            - Pagination support: limit and offset for large result sets
            - Sorting options: sortBy and sortDirection for ordered results
        - This tool is essential for:
            - **Finding blockchains by name** (primary use case)
            - Getting an overview of all blockchains in the system
            - Comparing deployment environments across blockchains
            - Identifying system chains vs user applications
            - Checking the operational status of blockchains
            - Discovering blockchains in specific clusters or containers
    """.trimIndent()

    /** Full advertised description of `get_chr_aggregates` before F9 moved it here. */
    private val GET_CHR_AGGREGATES: String = """
        - Get CHR token deposit and withdrawal aggregates with detailed breakdown
        - Returns comprehensive CHR token flow information including:
            - Grouped deposits by address and network ID with totals
            - Grouped withdrawals by address and network ID with totals
            - Overall totals for deposits and withdrawals
        - RESPONSE SIZE: by default the response is summarized - the grouped
          deposit/withdrawal arrays are capped at the first 50 entries each, with a
          `note` field saying how many entries were omitted. Pass full:true for the
          complete, uncapped response (can be hundreds of KB).
        - Supports flexible data inclusion options:
            - Include/exclude total summaries (depositsTotal, withdrawalsTotal)
            - Include/exclude grouped deposit details by address and network
            - Include/exclude grouped withdrawal details by address and network
        - This tool is useful for:
            - Analyzing CHR token flow patterns across networks
            - Monitoring deposit and withdrawal activities
            - Understanding CHR distribution across different addresses
            - Tracking cross-network CHR movements
            - Financial analysis and reporting of CHR token usage
            - Identifying major CHR holders and their activity patterns
            - Compliance and auditing of CHR token movements
    """.trimIndent()

    /** Full advertised description of `local_chain_up` before F9 moved it here. */
    private val LOCAL_CHAIN_UP: String = """
        Stand up a REAL local Chromia chain from Rell sources - in-process, zero keys, zero
        funds, zero human steps. Compiles the sources into a blockchain configuration and runs
        it on the embedded Postchain engine (the same engine `chr node start` wraps) against
        the server's PostgreSQL, then serves a subset of the Postchain REST API on 127.0.0.1:
        GET /brid/iid_0, GET+POST /query/{brid}, POST /query_gtv/{brid}, POST /tx/{brid}, and
        GET /tx/{brid}/{txRid}/status - block and confirmation-proof endpoints are NOT served.
        This is the last step of the agent loop: rell_check (compiles) -> rell_security_check
        (secure) -> run_rell_tests (tests pass) -> verify_guards (the guards those tests
        claim to prove are load-bearing) -> local_chain_up (runs against a live chain).
        Returns the BRID and apiUrl; then query with
        POST {apiUrl}/query/{brid} {"type":"<query>", ...args} (start with type=rell.get_app_structure),
        and submit transactions with any postchain client against apiUrl + BRID, signed with the
        public Chromia CLI dev key (privkey 42 repeated 32 times - local only, never a secret).
        actions: "up" (default; requires `files`), "status", "down".
        Bounded by design: one chain at a time, auto-stops after ttlSeconds (default 1800, max
        7200), runs in a dedicated PostgreSQL schema that is wiped on every start. Calling up
        again with identical inputs returns the running chain (TTL refreshed); a change to the
        sources, moduleArgs, or databaseUrl restarts it. Needs PostgreSQL via
        CHROMIA_TEST_DATABASE_URL on the server (or
        `databaseUrl`); @test modules are excluded from the chain like `chr build`.
    """.trimIndent()

    /** Full advertised description of `rell_security_check` before F9 moved it here. */
    private val RELL_SECURITY_CHECK: String = """
        Static security review of Rell code, run after a successful compile (compiles first via
        the embedded Rell compiler; uncompilable code returns the compile errors instead).
        Checks the production security rules for Chromia dApps:
        - CRITICAL: banned admin modules (lib.ft4.admin, admin.crosschain) and open
          registration/transfer strategies (ras_open, ras_transfer_open)
        - HIGH: operations that create/update/delete state without any auth check
          (ft4 auth.authenticate, op_context.is_signer, signer require)
        - HIGH: authenticated operations that debit/delete rows selected by a caller-supplied
          account parameter never bound to the authenticated identity (confused deputy:
          anyone drains anyone)
        - HIGH: is_signer(<param>) gates where the caller supplies the very key being checked
          and the parameter is used nowhere else (a phantom admin gate)
        - HIGH: update/delete @* {} with an empty where-clause (hits every row)
        - HIGH: hardcoded 64+ char hex literals that look like key material
        - MEDIUM: value-moving operations when every registered auth handler has flags = []
          (FT4 contains_all([]) is always true, so limited session keys can spend)
        - MEDIUM: hardcoded 64-hex constants named like BRIDs/hashes (public identifiers)
        - MEDIUM: operations with parameters but no require(...) input validation
          (validation inside called helper functions counts)
        HIGH findings in test-only code (@test modules, files under test/ or tests/, and
        modules imported only from @test modules) are reported as MEDIUM with a
        "-test-surface" rule suffix. @test modules are fully exempt from the banned-module/
        open-strategy rules (test code legitimately exercises admin modules and registration
        strategies); in non-test code CRITICAL findings never downgrade. Submitted lib/** files
        are library code: vendored-identical lib/ft4 and lib/iccf files are exempt, differing
        ones are scanned and noted, and other lib/* trees are skipped as third-party code.
        allowAdminModules:true (default false) downgrades banned-module findings from
        CRITICAL to MEDIUM - for admin/ops tooling only, never for production dApps.
        Returns line-anchored findings with a concrete fix per finding. ok=true means no
        CRITICAL/HIGH findings. Heuristic static analysis - it does not replace an audit.
        ok=true is NOT economic soundness. Static rules structurally cannot see: missing
        AUTHORIZATION (an authenticated caller touching a row it does not own - key writes
        off the authenticated id, or require(row.owner == account.id)); unbacked minting
        (crediting value no reserve covers); missing quorum/stake/timelock in governance;
        funds with no withdrawal or timeout path; i64 overflow aborting large legitimate
        amounts; whether an outcome meant to be UNPREDICTABLE actually is (only the
        block-clock-as-selector shape is caught - no chain value is secret, so a hash, a
        counter or a seed mixed from on-chain state is still public before the transaction
        is signed, and a clean report is not proof of fair randomness); or TRANSACTION
        ORDERING / MEV (front-running, sandwiching, a price or listing that can change
        under a pending transaction - the order operations land in a block is invisible
        to every static rule). Prove those with invariant tests via run_rell_tests - scaffold_dapp
        template=ft4 ships runnable conservation/overdraft/non-owner-must-fail examples,
        and for a DAO or an oracle-priced vault start from template=governance / template=vault,
        where quorum, voting window, reserve-backing and price bounds are structural.
        Use with rell_check as the loop: compile clean, then security clean, then present.
    """.trimIndent()

    /** Full advertised description of `run_rell_tests` before F9 moved it here. */
    private val RUN_RELL_TESTS: String = """
        Execute Rell tests in-process with the embedded Rell test runner (same engine the
        Chromia CLI wraps) and return per-case pass/fail results - no chr installation needed.
        This is step 3 of the agent verification loop:
        1. rell_check - the code compiles
        2. rell_security_check - the code is secure
        3. run_rell_tests - the code behaves correctly
        4. verify_guards - every guard you rely on is LOAD-BEARING: its must-fail test
           goes red without it, because the attack lands. A must-fail test that passes
           with the guard and still passes without it is a fake green; step 4 is what
           catches that, and it is what this server's own templates are held to.
        Pass `files` including at least one file starting with `@test module;` whose test
        functions are named test_*. Tests that touch entities/database need PostgreSQL via the
        CHROMIA_TEST_DATABASE_URL env var on the server; pure-logic tests run without it.
        Happy-path tests are not enough: for any dapp that holds value, also ship INVARIANT
        tests - conservation (a transfer never changes the total), no-negative-balance
        (overdraft must abort), and authorization (a NON-owner's attempt must fail via
        rell.test.tx()...run_must_fail("message")). scaffold_dapp template=ft4 ships runnable
        examples of all three to copy; template=governance and template=vault ship the
        adversary's DAO drain and oracle mint as must-fail tests - copy those for your own
        exploit-must-fail cases.
        Chasing one red case? Pass `tests` (same as `chr test --tests`) to run only the
        matching functions instead of the whole suite.
        Nothing is deployed; sources run in a temp directory and are deleted afterwards.
    """.trimIndent()

    /** Full advertised description of `scaffold_dapp` before F9 moved it here. */
    private val SCAFFOLD_DAPP: String = """
        Return a production-correct new Chromia dapp skeleton (chromia.yml, src/main.rell, test).
        Pins: Rell ${DappScaffold.RELL_VERSION}, merkle_hash_version 2, FT4 v1.1.0r API 1, Chromia CLI 0.33.x.
        Templates: 'hello' (default, query-only quickstart) or 'ft4' - the golden FT4 template:
        accounts + auth handlers, operations showing the full authenticate -> authorize ->
        validate -> check-invariants pattern (including an explicit ownership check and a
        Transfer-flag-scoped value move), plus RUNNABLE invariant tests (conservation,
        no-negative-balance, non-owner-must-fail) that execute via run_rell_tests - copy
        them for the app's own invariants. Also module_args, libs block, and a TypeScript
        client example. FT4 imports compile after `chr install`.
        Building a DAO / treasury: use 'governance' - quorum, a fixed voting window,
        stake-weighted votes and execute-once are structural, and its shipped tests replay
        the single-account treasury drain and require it to fail. Building an exchange,
        vault or anything priced by an oracle: use 'vault' - every credit is a reserve
        debit in the same operation, price posts are bounded and rate-limited, a stale
        price halts trading, and its tests replay the 100 -> 200,000,000 mint and require
        it to fail (its oracle key is a module arg: see the notes). Building staking, yield,
        rewards or farming emissions - a share of a REWARD POOL that many stakers split: use
        'staking' - rewards come only from a sponsor-funded pool, the clock releases at most
        what the pool holds, every credit is a pool debit in the same operation, unstaking has
        a cooldown, and its tests replay the round-4 stake-times-elapsed mint from an empty
        pool and require it to fail.
        Building an NFT marketplace, a listing board or anything with a buy button and creator
        royalties: use 'marketplace' - a buy names the EXACT price it agreed to and the listing
        row is immutable (so the round-5 max_price sandwich cannot be written), offers escrow the
        bidder's points and settle atomically, and the royalty's off-market bypass is DOCUMENTED
        in the template header with a shipped test asserting it still works.
        Building a lending pool, a credit line or a money market - anything where depositors hold
        a SHARE of a pool whose value moves: use 'lending' - it stores NO cash-denominated debt
        anywhere (positions and the pool carry scaled_debt in index units, the cash figures exist
        only inside a pool_state, pool_now() is the only function that makes one, and every
        pricing helper takes one), so the round-6 just-in-time interest capture - deposit at a
        share price stale between a borrower's touches, exit one block later, 10000 in and 11500
        out with nothing minted - cannot be written. It keeps the vault's bounded oracle (its
        key is a module arg: see the notes), over-collateralisation, a liquidation threshold with
        a close factor and bonus, and the minimum-first-deposit guard that kills ERC-4626 share
        inflation, and its tests replay the round-6 drain and require it to be refused.
        Building a payment stream, payroll, a subscription, a vesting grant or a drip - any
        payout METERED BY THE CLOCK to one named beneficiary: use 'streaming' - NO OPERATION IN
        IT WRITES A TIMESTAMP, so the round-7 grief cannot be written: started_at is written
        once by the create and is not mutable, the entitlement is a pure function of that
        immutable start and an immutable rate less a MONOTONE released total, and every other
        term is immutable too, so a stranger settling faster than one whole unit of entitlement
        (which released ZERO and still advanced the anchor in round 7, grinding the payee's
        income to nothing while the payer kept 100% of the escrow) is now a no-op. The stream
        is PREPAID, cancellation pays the payee everything accrued BEFORE refunding the payer
        the unearned remainder, and `cancellable` is fixed at creation so a vesting grant
        cannot be clawed back. Its tests replay the round-7 grind and require the payee to be
        paid what the clock says anyway.
        NEVER includes lib.ft4.admin, admin.crosschain, ras_open, or ras_transfer_open.
        Does not send signed transactions and does not run chr. Confirm APIs with fetch_docs.
    """.trimIndent()

    /** Full advertised description of `validate_chromia_yml` before F9 moved it here. */
    private val VALIDATE_CHROMIA_YML: String = """
        Validate a chromia.yml string against production pins.
        Checks compile.rellVersion (semver N.N.N), blockchains.*.module (module name, not a file path),
        merkle_hash_version == 2, blockchain key webStatic is accepted, and forbids FT4 admin / ras_open modules
        in libs and code; moduleArgs KEYS naming admin modules (e.g. lib.ft4.core.admin for admin_pubkey) are
        legitimate configuration and are NOT flagged.
        MISSING production pins (compile.rellVersion, merkle_hash_version) are warnings by default - chr builds
        official configs that omit them; pass strict:true to make missing pins errors. A rellVersion newer than
        the CLI-bundled compiler or a present-but-wrong merkle value is always an error.
        Deployments: reserved names mainnet / testnet auto-fill Directory brid + url; custom names require both;
        a Directory Chain BRID that is not 64 hex is an error; official reserved BRIDs must match.
        require_mandatory_flags as a YAML / moduleArgs key is an error (main auth descriptor only).
        Warns if a chain config lacks merkle_hash_version while others set it, deployments.*.container is missing,
        or libs.*.insecure is true (skips RID check; not for production).
        Returns structured {ok, errors[], warnings[]}. Does not run chr or send signed transactions.
    """.trimIndent()

    /** Full advertised description of `verify_guards` before F9 moved it here. */
    private val VERIFY_GUARDS: String = """
        Prove that a guard in YOUR dapp is load-bearing - the discipline every shipped template
        is held to, run on your own code. A must-fail test is only evidence if it goes red when
        the guard it depends on is removed, and goes red BECAUSE THE ATTACK LANDED. A test that
        passes with the guard and still passes without it is a fake green with a security label
        on it - one was written by this server's own maintainers and only a mutant caught it.
        For each {guard, test} you name: (1) the test must PASS on your files as submitted
        (otherwise `baseline_red`: it proves nothing in either state); (2) the guard line is
        replaced (default: deleted) and ONLY that test is run; (3) the verdict is read from WHY
        it failed. Verdicts per guard:
          load_bearing           - the test failed because the attack landed (run_must_fail
                                   reports the transaction "did not fail", or the test's own
                                   conservation / payout assertion tripped)
          vacuous                - the test stayed green with the guard gone: it does not
                                   exercise the guard. Make the test drive the attack.
          still_refused          - something else refused the attack (another require, a
                                   second guard, a module_args bound). Name it in alsoRemove
                                   if it is defence in depth, else the test measures a
                                   different guard. A refusal is the GUARD'S OWN when the
                                   runner's frame - [module:declaration(file:line)] - names
                                   the declaration the guard sits in, MODULE INCLUDED (module
                                   names are derived relative to the Rell source root, as
                                   the runner derives them), or (guard in a function) any
                                   operation or query that reaches it through your own call
                                   graph including @extend/@extendable.
          ambiguous_refusal      - the mutant went red and the tool WILL NOT GUESS whether that
                                   red is the attack being refused or the damage being noticed.
                                   Never proven, and never a reason to weaken the test: the
                                   evidence names the shape to write.
        WRITE THE TEST IN ONE OF TWO SHAPES - these are what the tool can prove, and both
        require the test to invoke the guard's declaration (the declaration it sits in, or,
        for a guard in a function, any operation or query that reaches it through your call
        graph including @extend) in EXACTLY ONE top-level statement, directly or through
        test-module helpers. A HELPER MAY LIVE IN ANY TEST MODULE of the submission, not only
        in the test's own file, and a call is resolved through the CALLING module's own
        imports: h(...) in its own module or through `import mod.*;`, mod.h(...) through
        `import a.b.mod;`, and alias.h(...) through `import alias: a.b.mod;`. The chain is
        followed up to 16 calls deep; past that, or through a helper that calls itself, the
        tool says the chain is deeper than 16 calls and counts no call sites at all rather
        than reporting a number. The operations of the transaction and the run_must_fail are
        read over the statement's WHOLE call closure - every helper it calls, not only the
        ones that reach the guard - so a helper that quietly adds a second operation makes the
        statement ambiguous_refusal instead of a single-operation SHAPE A.
        SHAPE A, the must-fail test: that statement is a single-operation
        rell.test.tx().op(<that declaration>(...)).run_must_fail(...), and the guard is proven
        only when removing it makes the transaction SUCCEED ("did not fail") - any other red
        is the attack still being refused. SHAPE B, the must-hold test: that statement expects
        the call to SUCCEED (a single-operation .run(), or a direct call to the query) and the
        damage is measured AFTER it - a rell.test assert_* failure, or a later transaction
        refusing, is the guard being load-bearing, while a refusal from the guard's own
        declaration is the attack being refused. Anything else - a loop or a table, several
        invoking statements, a transaction carrying more than that one operation, a helper
        with several call sites - is ambiguous_refusal, because "which invocation refused"
        is then a question nothing in the run answers.
        stillRefused and attackLanded are read in SHAPE B ONLY. That is the shape where the
        tool must decide whether a red is a refusal or the damage being noticed: a red
        containing stillRefused is still_refused, and a red missing a CUSTOM attackLanded is
        red_for_another_reason. They change no SHAPE A verdict and cannot. A shape A statement
        runs ONE operation, that operation is a declaration this guard runs in, and an
        operation refusal always carries its own [module:declaration(file:line)] frame - so a
        must-fail red either says "did not fail" (the transaction went through: the attack
        landed, whatever you pinned) or names the guard's own declaration as having refused it,
        and there is no third red for a fragment to decide. A caller-supplied substring must
        never be able to read the guard's OWN refusal as the attack succeeding; round 11 got
        four false ok:true exactly that way.
          environmental          - the mutant is not a running dapp (compile error, missing
                                   module_args). A failure for that reason proves nothing.
          red_for_another_reason - red, but not the attack; read the error before counting it.
          baseline_red / guard_not_found / guard_ambiguous / test_not_found - the inputs
                                   cannot be verified yet.
          replacement_rejected   - the replacement would change code OUTSIDE the guard's span
                                   (it opens or closes a comment or a string). A mutation may
                                   alter only the line it names.
          also_remove_overlaps_guard - an alsoRemove entry contains (or is contained by) the
                                   guard; the two must be disjoint or the control run strips
                                   the guard itself.
        A replacement's own require() messages count as refusals exactly like the guard's. The
        string literals a frame-less error is attributed to are read off the MUTANT SOURCES
        THAT RAN, so a message the replacement introduced belongs to the declaration the guard
        sits in, and a replacement that refuses the attack in a query is still_refused rather
        than ambiguous_refusal.
        ok=true only when EVERY named guard is load_bearing. Pass the same moduleArgs you pass
        to run_rell_tests. Nothing is deployed; sources run in a temp directory and are
        deleted afterwards. This does not replace an audit and says nothing about guards you
        did not name - it makes the guards you DID name real evidence instead of a claim.
    """.trimIndent()

    /**
     * Tool name -> the long form that used to be the advertised description.
     * Absent for tools whose advertised description is already the full text.
     */
    val LONG: Map<String, String> = mapOf(
        "check_dapp_project" to CHECK_DAPP_PROJECT,
        "chr_deploy_help" to CHR_DEPLOY_HELP,
        "chromia_cookbook_help" to CHROMIA_COOKBOOK_HELP,
        "chromia_dapp_query" to CHROMIA_DAPP_QUERY,
        "deploy_testnet_chain" to DEPLOY_TESTNET_CHAIN,
        "deployment_preflight" to DEPLOYMENT_PREFLIGHT,
        "filter_blockchains" to FILTER_BLOCKCHAINS,
        "get_chr_aggregates" to GET_CHR_AGGREGATES,
        "local_chain_up" to LOCAL_CHAIN_UP,
        "rell_security_check" to RELL_SECURITY_CHECK,
        "run_rell_tests" to RUN_RELL_TESTS,
        "scaffold_dapp" to SCAFFOLD_DAPP,
        "validate_chromia_yml" to VALIDATE_CHROMIA_YML,
        "verify_guards" to VERIFY_GUARDS
    )

    /** The long form when one was moved out, else [advertised] (already full). */
    fun full(name: String, advertised: String?): String? = LONG[name] ?: advertised

    /** Tools whose long form is reachable as a `chromia_help` topic. */
    val TOPICS: Set<String> = LONG.keys
}
