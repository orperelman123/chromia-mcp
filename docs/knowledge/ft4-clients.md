# FT4 and official Chromia clients — production knowledge brief

**As-of date:** 2026-08-26 (Asia/Jerusalem).
**Sources:** official Chromia docs (docs.chromia.com), generated FT4 Rell API (docs.chromia.com/pages/ft4-rell/), GitLab chromaway/ft4-lib source, and official client pages.
**Rule:** APIs below are only those stated in those sources. Where sources disagree or omit a field, that is called out. Nothing here is inferred as a stable public API.

---

## Rell version on FT4 pages (0.16.0 vs current)

- The generated FT4 Library pages (https://docs.chromia.com/pages/ft4-rell/) now stamp **Rell 0.16.1** (confirmed on module_args and other struct pages). The earlier **0.16.0** stamp on those pages is no longer current.
- Language-level Rell is newer than that stamp. Official Rell release notes list **0.16.4** (2026-08-02) as the latest release as of this brief, with 0.16.3 / 0.16.2 / 0.16.1 dated 2026-07-30 / 2026-07-28 / 2026-07-15.
- Do not treat 0.16.1 as the Rell version you must compile against. FT4 narrative setup pages still show older pins (e.g. compile.rellVersion: 0.14.9 or 0.13.14 in strategy examples). Those are examples, not a current platform requirement.
- GitLab ft4-lib tags observed on 2026-08-26 include v2.1.1, v2.1.2-beta.0, v1.2.0r, v1.1.1r, v1.1.0r. Official setup/import pages still pin **tagOrBranch: v1.1.0r** with RID x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E". Which tag a production dapp should use is not stated as a single current version on the FT4 intro pages; confirm against the tag/RID pair you actually install via `chr install`.

---

## 1. What FT4 is, and when you must use it vs raw Rell entities

FT4 (Flexible Token 4) is Chromia's official library for accounts plus fungible assets. Official intro (/build/ft4/intro):

- Similar role to ERC-20 for token create / transfer / ownership, plus flexible account access, multi-sig, configurable rules, rate limiting, and cross-chain transfers inside the Chromia network.
- Backend is Rell (lib.ft4.*), configured through chromia.yml. Frontend/client is TypeScript (@chromia/ft4) on top of postchain-client.
- Protocol-level page (/get-started/about/protocols/ft4): token registration, token transfers, account creation; accounts (not bare addresses) are the unit of balance and operations; accounts are registered on demand and cannot receive transfers until they exist (unlike EVM addresses). FT4 accounts can bind native Chromia keys or EVM addresses (MetaMask / Vault).

Use FT4 when the dapp needs (quoted from intro):

- User accounts with authentication
- Token / asset management
- Multi-sig or role-based access
- Transfers between accounts
- Cross-chain asset transfers (Chromia-to-Chromia)
- Built-in rate limiting / spam prevention

Do not need FT4 for simple Rell dapps (read-only store, voting without tokens, etc.). You can use raw Rell entities, operations, and op_context signers. FT4 is optional but recommended once you have user accounts and assets.

Practical implication: rolling your own entity account / entity balance duplicates what FT4 already standardizes (auth descriptors, flags, rate limits, transfer history, cross-chain hops, client libraries). Official docs also warn that importing lib.ft4.* (especially lib.ft4.assets / lib.ft4.accounts, not lib.ft4.core.*) exposes user-facing operations and queries. Production dapps should import only the modules they intend to expose.

Import split (official):

- lib.ft4.accounts / lib.ft4.assets / lib.ft4.auth / lib.ft4.crosschain: entities plus mounted queries/ops users can call
- lib.ft4.core.<same>: implementation without exposing those external functions
- lib.ft4.admin: dev/init admin ops — must not be in production
- lib.ft4.test: tests only

Modules listed in /build/ft4/setup/imports: accounts, admin, admin.crosschain, assets, auth, cross-chain, prioritization, test, utils.

---

## 2. Accounts, auth descriptors, auth flags, login sessions

### Account

Rell entity (account-management overview + module.rell):

```rell
entity account {
  key id: byte_array;
  index type: text;
}
```

- User accounts created via create_account_with_auth get type = ACCOUNT_TYPE_USER ("FT4_USER").
- Account ID (intro + auth docs):
  - Native FT / Postchain signer: hash(pubkey)
  - EVM signer: hash(evm_address) where the address is without 0x, 20 bytes. Official formula: hash(hash(pubkey).sub(0, 40)) (first 40 hex chars of the hashed pubkey).
  - Multi-sig (source get_account_id_from_signers): if one signer then signers[0].hash(); if many then hash(sorted signers).
- Same underlying key produces different account IDs for FT vs EVM representation.
- Multiple users / keypairs can control one account via auth descriptors. One keypair can also sit on multiple accounts.
- Dapp-controlled accounts (no auth descriptors) exist via create_account_without_auth(account_id, type) — for pools, fee, lock, treasuries. They cannot be accessed by external signers.

### Auth descriptors

An account_auth_descriptor binds who (signers), what (flags), and when (rules) to an account.

Fields (Rell API + source):

- id: unique id (hash of the descriptor)
- account: account it authorizes
- auth_type: S (single-sig) or M (multi-sig)
- args: encoded single_sig_args or multi_sig_args (flags + signer(s) +, for multi-sig, signatures_required)
- rules: expiration / usability rules as GTV bytes; null becomes GTV_NULL_BYTES
- ctr: operation-count used by op_count / operation_count rules
- created: registration timestamp

Main auth descriptor (main_auth_descriptor entity):

- Every user account must have exactly one.
- It can be substituted, not deleted via the public update path.
- Its rules must be GTV_NULL (RESTRICTED MAIN AUTH otherwise).
- create_account_with_auth and update_main_auth_descriptor call require_mandatory_flags.
- delete_auth_descriptor throws DELETE MAIN UNAUTHORIZED if you pass the main one.
- delete_all_auth_descriptors_except_main is the documented cleanup for leftover login/disposable descriptors.

Limits (source + docs):

- auth_descriptor.max_number_per_account default 10, hard-capped at 200 (AUTH_DESCRIPTORS_PER_ACCOUNT_UPPER_BOUND). Exceeding throws TOO MANY AUTH DESCRIPTORS.
- auth_descriptor.max_rules default 8.

Signer sizes (source validate_auth_descriptor_args): EVM address 20 bytes, FT pubkey 33 bytes. Multi-sig signatures_required must be > 0 and <= number of signers.

### Auth flags (A, T, mandatory / default)

Built-in constants (lib.ft4.core.accounts.auth_flags):

- ACCOUNT = "A": add/remove auth descriptors. Official text: super access — holder can add new descriptors with any flag, so they can unlock any operation.
- TRANSFER = "T": move funds (transfer, burn, etc.).

Dapps may define additional flags (any string matching /[a-z_A-Z]+/). Short flags are recommended.

Enforcement:

- ft4.add_auth_descriptor / ft4.delete_auth_descriptor fail unless the authorizing descriptor has A.
- ft4.transfer must be signed by an auth descriptor with accounts.auth_flags.TRANSFER.
- Custom ops opt in via @extend(auth.auth_handler) + auth.add_auth_handler(scope, flags, message?, resolver?). Missing handler: Cannot find auth handler for operation <name>.
- Operation-scope, mount-scope (scope = "mid.inner"), and app-scope (omit scope) handlers exist. Multiple app-scope handlers cause a runtime error.
- add_overridable_auth_handler lets a library ship a default that a dapp can replace once.
- Resolvers (e.g. delete-own-descriptor without A) can override static flags. Returning null means none of the candidate descriptors is authorized.

moduleArgs -> lib.ft4.core.accounts -> auth_flags (source auth_flags_config.rell; not shown on the narrative configuration-values page):

```rell
struct auth_flags_config {
    mandatory: gtv = [auth_flags.ACCOUNT, auth_flags.TRANSFER].to_gtv(); // ["A","T"]
    default:   gtv? = null;  // if null, treated as mandatory
}
```

- mandatory: flags main descriptors must have (require_mandatory_flags on create/update main). Missing: MISSING MANDATORY FLAGS.
- default: flags applied on creation when not specified; if null, equals mandatory.
- YAML values may be a list of texts or a comma-separated string. Invalid: INVALID FLAGS.

Important nuance: the struct comment says all auth descriptors must have these flags to exist, but source only calls require_mandatory_flags from create_account_with_auth and update_main_auth_descriptor. add_auth_descriptor (used for login/disposable keys) does not call it. That matches the client login docs, which create descriptors with flags: []. Treat mandatory as main-descriptor policy, not a global filter on every descriptor.

### Login sessions (disposable keys)

Backend (lib.ft4.core.auth):

- Dapp extends login_config(): map<text, _login_config> via add_login_config.
- _login_config { flags: list<text>; rules: gtv? = null }.
- A named config is what the client uses to mint a login auth descriptor whose signer is stored so the client can sign non-sensitive ops without prompting. Official warning: configure this so login keys cannot call sensitive ops.
- DEFAULT_LOGIN_CONFIG_NAME exists; exact string is not restated on the pages fetched for this brief.

Client (/build/ft4/client/client-login, @chromia/ft4):

```js
const { session, logout } = await login({
  accountId,
  config: { flags: ["T"], rules: ttlLoginRule(minutes(30)) }, // optional
  loginKeyStore: createSessionStorageLoginKeyStore(),         // optional
});
await session.call(op("foo"), op("bar", "...", 123));
await logout();
```

Documented defaults if config is omitted: flags [], timeout one day.

Login keystores in the official TS client: createInMemoryLoginKeyStore, createSessionStorageLoginKeyStore, createLocalStorageLoginKeyStore, plus any LoginKeyStore implementation.

hasActiveLogin: if an unexpired login key exists, login will reuse it and not prompt. Official caveat: rules are not re-validated except expiry — a descriptor expiring in 2 seconds can be reused even if you asked for 5 minutes.

Auth operations that precede the real op (nothing may sit between them):

- ft4.ft_auth(account_id, auth_descriptor_id): native FT signer. Whole tx must be signed by required signers.
- ft4.evm_auth(account_id, auth_descriptor_id, signatures): EVM EIP-191 message (placeholders: account id, auth descriptor id, nonce, blockchain RID). Nonce from utils.derive_nonce + descriptor counter; counter increments per evm_auth on the same descriptor. Unused multi-sig slots are null.
- evm_signatures: EVM signer not yet on an account descriptor (Kotlin docs: addEvmSignaturesOp).

Official clients (@chromia/ft4 TransactionBuilder / session.call, Kotlin addFtAuthenticationOp / addEvmAuthenticationOp) insert these for you.

Expiration rules (auth-descriptors-and-rules):

- Operators: >, >=, =, <, <=
- Variables: operation_count (only < and <=), block_time, block_height, relative_block_height, relative_block_time
- Simple rule or complex rule: list starting with "and" plus rule_expression items
- Statuses: Active, Inactive, Expired. Expired descriptors are deleted automatically when the account next sends an operation.
- Main descriptor rules must be GTV_NULL_BYTES.

---

## 3. Assets, balances, transfers, cross-chain / EIF

### Asset and balance

```rell
entity asset {
  key id: byte_array;
  name;
  key symbol: text;
  decimals: integer;
  issuing_blockchain_rid: byte_array;
  icon_url: text;
  type: text = ASSET_TYPE_FT4;   // shown as "ft4" in query output
  mutable total_supply: big_integer;
}

entity balance {
  key accounts.account, asset;
  mutable amount: big_integer;
}
```

Registration (production: custom op, not admin):

- Admin (dev only): chr tx ft4.admin.register_asset <name> <symbol> <decimals> <icon_url>
- Custom: assets.Unsafe.register_asset(name, symbol, decimals, chain_context.blockchain_rid, icon_url). Example derives asset_id = (asset_name, chain_context.blockchain_rid).hash().
- Cross-chain: child chain registers the same asset with an origin (parent) chain. Admin: ft4.admin.register_crosschain_asset. Client: registerCrosschainAsset(childClient, adminSignatureProvider, asset, parentBrid).

Queries (mounted when lib.ft4.assets is imported; names from lib.ft4.external.assets): ft4.get_all_assets, ft4.get_asset_by_id, ft4.get_asset_balance, ft4.get_asset_balances, filtered/paginated variants, transfer-history queries.

### On-chain transfer

```rell
@mount("ft4.transfer")
operation transfer(recipient_id: byte_array, asset_id: byte_array, amount: big_integer)
```

- Requires T.
- Amount must be in (0, 2^256) exclusive. Insufficient balance fails.
- If recipient exists: normal transfer. If not: starts account-creation-on-transfer when that strategy is enabled; otherwise error.
- recall_unclaimed_transfer recalls a transfer sent to a non-existing account that was never claimed.
- Extensible via before_transfer / after_transfer.
- TS helper: transfer(receiverId, assetId, amount) builds an ft4.transfer operation object. Session.account.crosschainTransfer is documented for cross-chain. Do not invent extra wrapper names beyond those pages.

### Cross-chain (Chromia to Chromia) — ICCF, not EIF

FT4 cross-chain is inside the Chromia network. It needs:

1. import lib.ft4.crosschain;
2. GTX module net.postchain.d1.iccf.IccfGTXModule
3. libs.iccf from https://gitlab.com/chromaway/core/directory-chain (setup page example: tagOrBranch 1.87.0, RID x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D")

Origin tree: issuing chain is the mint root. Each other chain registers the asset with an origin (parent). Transfers walk the tree (A to B direct; B to C may go B to A to C). Wrong or malicious origin can lose funds. Origin registration is not validated on the origin chain.

Hops (official):

- ft4.crosschain.init_transfer — source — lock/burn on source; record hops + deadline
- ft4.crosschain.apply_transfer — each hop / target — needs iccf_proof of previous hop
- ft4.crosschain.complete_transfer — source — finalize after last apply
- ft4.crosschain.cancel_transfer — next hop after expiry — start rollback (anyone with proof)
- ft4.crosschain.unapply_transfer — hops already applied — walk funds back
- ft4.crosschain.revert_transfer — source — return funds to sender after rollback
- ft4.crosschain.recall_unclaimed_transfer — target — recipient account never created

Direction of the hop decides lock / mint / unlock / burn. TS orchestrator: session.account.crosschainTransfer(targetChainId, recipientId, assetId, amount) emits signed / init / hop. Also initTransfer, applyTransfer, resumeCrosschainTransfer, revertCrosschainTransfer.

initTransfer TS args (official): recipientId, assetId, amount, hops (BRIDs from source to target including target), deadline (days until revert if unclaimed). Rell init_transfer docs also mention destination and hops. Use the published signatures; do not invent extra fields.

### EIF (Ethereum Interoperability Framework)

EIF is not an FT4 transfer API. Official FT4 pages do not document EIF as the way to move FT4 balances.

What is documented, separately:

- EVM auth inside FT4: EVM address as signer; ft4.evm_auth; MetaMask via createWeb3ProviderEvmKeyStore. This is authentication, not bridging.
- Chromia-EVM asset bridge: /build/ft4/asset-management/transfer-assets points at the EVM Bridge docs (/ecosystem/bridge/...) and the bridge-client. That is a different product (deposit/withdraw, leases, mass-exit).
- EIF itself: chr eif generate-events-config (/build/cli/commands/eif) — generate Solidity event configs for a Postchain extension that listens to Ethereum events. Governance starter-kit has eif-configuration. Rell op_context.emit_event notes EIF as an event-sink consumer, same class as ICMF.
- No official page fetched for this brief defines an FT4 eif_* operation or says call this FT4 op to mint from an EVM lock.

If a dapp needs Chromia to Ethereum token movement, follow the bridge docs, not FT4 cross-chain (init_transfer / ICCF). If it needs Chromia to Ethereum event/state integration, follow EIF / governance-extension docs. Do not conflate the three.

---

## 4. moduleArgs for lib.ft4.core.accounts — exact shape

### Authoritative Rell struct (GitLab development + generated API, Rell 0.16.1)

Source: rell/src/lib/ft4/core/accounts/module.rell + auth_flags_config.rell.

```rell
struct rate_limit_config {
    active: boolean = true;
    max_points: integer = 10;
    recovery_time: integer = 5000;   // ms per recovered point
    points_at_account_creation: integer = 1;  // min 0
}

struct auth_descriptor_config {
    max_rules: integer = 8;
    max_number_per_account: integer = 10;  // clamped to 200
}

struct auth_flags_config {
    mandatory: gtv = [auth_flags.ACCOUNT, auth_flags.TRANSFER].to_gtv();
    default: gtv? = null;
}

struct module_args {
    rate_limit: rate_limit_config = rate_limit_config();
    auth_descriptor: auth_descriptor_config = auth_descriptor_config();
    auth_flags: auth_flags_config = auth_flags_config();
}
```

YAML that matches the struct (all keys optional because of defaults):

```yaml
blockchains:
  <chain>:
    module: main
    moduleArgs:
      lib.ft4.core.accounts:
        rate_limit:
          active: true
          max_points: 10
          recovery_time: 5000
          points_at_account_creation: 1
        auth_descriptor:
          max_rules: 8
          max_number_per_account: 10
        auth_flags:
          mandatory: ["A", "T"]   # or "A,T"
          default: ["A", "T"]     # omit / null copies mandatory
```

chr query ft4.get_config (setup page) returns the rate-limit block; that page sample does not show auth_descriptor / auth_flags.

### Discrepancy vs /build/ft4/configuration-values

That narrative page:

- Documents rate_limit the same way.
- Documents auth_descriptor.max_number_per_account (default 10).
- Puts max_auth_descriptor_rules as a sibling of auth_descriptor, default 8, example:

```yaml
lib.ft4.core.accounts:
  rate_limit:
    active: true
    max_points: 20
    recovery_time: 5000
    points_at_account_creation: 1
  auth_descriptor:
    max_number_per_account: 4
  max_auth_descriptor_rules: 4
```

- Does not document auth_flags at all.
- Account-management overview also nests only rate_limit and auth_descriptor.max_number_per_account.

Production choice: the Rell module_args struct is what the compiler binds. Prefer:

```yaml
auth_descriptor:
  max_rules: 4
  max_number_per_account: 4
```

not a top-level max_auth_descriptor_rules. If a deployed chain was configured with the narrative key, verify against rell.get_module_args / get_auth_descriptor_config() rather than assuming the YAML key was applied.

### Related moduleArgs (not on lib.ft4.core.accounts)

- lib.ft4.core.admin: admin_pubkey (required if admin imported; chr keygen)
- lib.ft4.core.accounts.strategies.transfer: rules[] with sender_blockchain, sender, recipient, asset (name / id / issuing_blockchain_rid + min_amount), timeout_days, strategy ("open" / "fee" / ...)
- lib.ft4.core.accounts.strategies.transfer.fee: asset[] (id xor name[+issuing chain], amount), fee_account
- Transfer specials: "*" any; "$" current chain; "X" sender=recipient

libs (setup/imports):

```yaml
libs:
  ft4:
    registry: https://gitlab.com/chromaway/ft4-lib.git
    path: rell/src/lib/ft4
    tagOrBranch: v1.1.0r   # as printed on official setup pages
    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"
    insecure: false
  iccf:
    registry: https://gitlab.com/chromaway/core/directory-chain
    path: src/lib/iccf
    tagOrBranch: 1.87.0    # setup/imports; some older examples still show 1.32.2
    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"
    insecure: false
```

---

## 5. Account registration strategies

Official strategies (enable by import + moduleArgs):

- Open — lib.ft4.core.accounts.strategies.open — anyone can register — not recommended (spam)
- Transfer open — ...strategies.transfer.open — transfer to not-yet-existing account, then claim all — usable if same-address / extra limits
- Transfer fee — ...strategies.transfer.fee — same, plus one-time fee to fee_account — recommended
- Transfer subscription — ...strategies.transfer.subscription — recurring fee to keep the account usable — recommended

Open op:

```rell
@mount("ft4.ras_open")
operation ras_open(main: auth_descriptor, disposable: auth_descriptor?)
```

Next op in the same tx must pass require_register_account_next_operation (typically register_account).

Client (@chromia/ft4 registrationStrategy):

```ts
registerAccount(client, keyStore, registrationStrategy.open(authDescriptor, loginConfig?))
registrationStrategy.transferOpen(authDescriptor, loginConfig?)
registrationStrategy.transferFee(feeAsset, authDescriptor, loginConfig?)
registrationStrategy.transferSubscription(subscriptionAsset, authDescriptor, loginConfig?)
registrationStrategy.fee(senderBlockchainRid, feeAsset, authDescriptor, loginConfig?)
registrationStrategy.subscription(senderBlockchainRid, subscriptionAsset, authDescriptor, loginConfig?)
```

Client guide also requires import lib.ft4.external.accounts.strategies; plus the concrete strategy module. Example main descriptor: createSingleSigAuthDescriptorRegistration(["A", "T"], keyStore.id).

Admin path (dev only): ft4.admin.register_account with GTV [0, [["A","T"], x"<pubkey>"], null] (0 = single-sig).

Custom path: call accounts.create_account_with_auth(auth_descriptor, account_id?) from your own op after your own anti-spam (voucher example is in the docs).

Transfer-strategy rules (spam control): which origin chains, senders, recipients, assets, min_amount, timeout_days, which sub-strategies. Fee amount must be lower than min_amount or users can send too little and wait for timeout to recall.

---


## 6. Official clients — how they talk to a chain

Chromia clients are Postchain REST clients.

They identify a chain by BRID (and/or local IID), talk to one or more node URLs, and separate query (read) from operation/transaction (write, signed). FT4 sits on top and adds account/session/auth-descriptor/asset helpers.

### 6.1 postchain-client JS/TS
- Official JS library for queries and transactions.
- Name: postchain-client
- Docs path: /build/clients/postchain-clients/javascript-typescript/

createClient settings from the JS reference page:
- nodeUrlPool: URL or list of node URLs
- directoryNodeUrlPool: system-cluster URLs; client discovers dapp nodes via Directory Chain
- blockchainRid: target chain RID
- blockchainIid: instance id; Directory Chain is 0
- statusPollInterval and statusPollCount
- failOverConfig: strategy, attemptsPerEndpoint (default 3), attemptInterval (default 5000 ms), unreachableDuration (default 30000 ms)
- useStickyNode: keep using a successful node; requires directoryNodeUrlPool

Queries (no signatures): client.query("hello_world") or client.query("get_foobar", { foo: 1, bar: 2 }) or client.query({ name, args }).

Transactions: newSignatureProvider({ privKey }), then signAndSendUniqueTransaction({ operations, signers }, sig). Also signTransaction, sendTransaction, addNop. PromiEvent on "sent". Statuses: Waiting, Rejected, Confirmed, Unknown.

Failover strategies: Abort on error, Try next on error, Single endpoint, Query majority. createIccfProofTx builds ICCF proofs. signAndSendUniqueTransaction injects a nop so the tx is unique.

### 6.2 @chromia/ft4 (JavaScript / TypeScript FT4 client)

- Dependency: @chromia/ft4 (docs pages labeled v2.0.0)
- Official pages: /build/ft4/client/*, /build/clients/ft4-client, /pages/ft4-ts-client/client/
- Wraps an IClient from postchain-client.

Typical flow from official setup: createClient from postchain-client with nodeUrlPool and blockchainIid (0 locally), then createConnection(client). createKeyStoreInteractor(client, keyStore) yields getAccounts, login, getSession. login({ accountId }) returns { session, logout }. session.call(op(...), ...) builds ft_auth or evm_auth plus ops plus nop.

Documented factories: createConnection, createConnectionToBlockchainRid, createClientToBlockchain, createSession, createKeyStoreInteractor, createAuthDataService.

Session fields/methods from the TS reference: account (authenticated), blockchainRid, client, call, callWithoutNop, sign, signAndSend, plus filtered getters also on Connection (assets, accounts, transfers, rate-limit states).

TransactionBuilder: add(operation, config?), addSigners(...keyStores), build, buildAndSend, buildAndSendWithAnchoring (wait for cluster and system anchoring).

Keystores/handlers documented: createInMemoryFtKeyStore, createInMemoryEvmKeyStore, createWeb3ProviderEvmKeyStore, createGenericEvmKeyStore, createFtKeyHandler, createEvmKeyHandler, ftAuth, ftSigner.

### 6.3 Kotlin / Java

Official page /build/clients/postchain-clients/kotlin-client (marked being updated). Artifacts from GitLab Maven, not Maven Central:

- net.postchain.client:postchain-client — queries and txs against a node URL plus BRID
- net.postchain.client:chromia-client — looks up system nodes; awaitAnchoredTx
- net.postchain.client:ft4-client — FT4 auth ops, descriptors, balances

Maven repos: gitlab.com/api/v4/projects/50818999/packages/maven, projects/32294340, projects/46288950. Upstream README also at github.com/ChromiaProject/postchain-client.

Init: BlockchainRid.buildFromHex, EndpointPool.singleUrl("http://127.0.0.1:7740"), PostchainClientProviderImpl().createClient(PostchainClientConfig(bcRid, endpointPool, listOf(keyPair))). Query: psClient.query("hello_world", GtvFactory.gtv(mapOf())). Tx: transactionBuilder(), addOperation, addNop, sign(sigMaker), post(). Statuses: WAITING, REJECTED, CONFIRMED, UNKNOWN.

FT4 helpers documented: addFtAuthenticationOp(psClient, txBuilder, operationName, pubKey, accountId), addEvmAuthenticationOp(...), addEvmSignaturesOp(...), getAccountAuthDescriptorsBySigner, getAssetBalance(psClient, accountId, assetId).

StandardChromiaClient(url).awaitAnchoredTx(brid, txRid). The Gradle snippet on the same page still lists postchain-client under the chromia-client heading; treat artifact ids as printed in the Maven XML (chromia-client) when in doubt.

### 6.4 Python — postchain-client-py

Official page /build/clients/postchain-clients/python-client (marked being updated; points at the postchain-client-py repository). Async via aiohttp. No official Python FT4 client is documented.

NetworkSettings(node_url_pool=["http://localhost:7740"], blockchain_rid=...). BlockchainClient.create(settings). client.query(name) or client.query(name, args_dict). Transaction(operations=[Operation(op_name, args)], signers, signatures=None, blockchain_rid=...). sign_transaction(tx, private_bytes) then send_transaction(signed, do_status_polling=True). secp256k1 via coincurve.PrivateKey.

This is raw Postchain, same as JS postchain-client: add ft4.ft_auth yourself if you call FT4 ops.

### Shared model (all official clients)

- Node URL: REST API, default local http://localhost:7740
- blockchainRid / BRID: chain identity (hex). Required on public networks
- blockchainIid: local / directory instance id; 0 on a single local chain
- Directory pool: discover nodes from the Directory Chain
- Query: read-only; no fee, no signature (unless the dapp checks something else)
- Operation: state change; packed into a GTX tx with signers
- Signers: FT uses 33-byte pubkeys on the tx plus signatures. EVM is not a tx-signer; evm_auth carries EIP-191 sigs
- nop: makes an otherwise-identical tx unique (signAndSendUniqueTransaction / addNop / FT4 call)
- Anchoring: optional wait until cluster and system anchoring (buildAndSendWithAnchoring, Kotlin awaitAnchoredTx)

## 7. Production auth pitfalls

1. A is superuser. A descriptor with A can add another descriptor with T (or any custom flag) and empty the account. Never put A on a login/disposable key. Official login page: never add auth flags that could lead to asset compromise if the disposable key pair is compromised.

2. T is money. Default login flags are empty; transfers still need a T-bearing descriptor (prompt or explicit config.flags: ["T"]). Do not give session keys T unless the product accepts hot-wallet risk.

3. Mandatory flags vs session keys. Default auth_flags.mandatory is ["A","T"] and applies to the main descriptor. If you raise mandatory to include custom flags, every new account main descriptor must include them. Disposable/login descriptors are not checked against mandatory in source.

4. Main descriptor is permanent access. No expiration rules. Losing those keys without a pre-added recovery descriptor (also typically A+T) loses the account. Use update_main_auth_descriptor, not delete-then-hope. delete_main_auth_descriptor leaves the account unusable.

5. Descriptor cap. Default 10, max 200. Each login without a durable loginKeyStore and without logout adds a descriptor. Hot reload in dev hits the cap until rules expire. Mitigations: session/localStorage login keystore, always logout, delete_all_auth_descriptors_except_main (needs A).

6. hasActiveLogin reuses almost-expired keys. Do not assume requested TTL is the remaining TTL.

7. Rate limits. Default: on, 10 points, 5000 ms/point, 1 point at creation. Each authenticated operation spends a point. Bursts die with empty points; points_at_account_creation: 0 means the new account cannot do anything until one recovery interval. Per-account override: extend get_rate_limit_config_for_account (return null for global default). Docs disagree slightly on wording (max_points as max txs at once vs points before limiting); source behavior is accumulate up to max_points, spend one per authenticated op, recover one every recovery_time ms.

8. Admin module. lib.ft4.admin can mint, register assets/accounts, grant rate-limit points. Official: never in production. Use a custom multi-sig admin surface.

9. Open registration. ft4.ras_open is free account creation. Docs: dev/test only. Prefer transfer-fee / subscription; if transfer-open, constrain with X / $ and a real min_amount.

10. Auth handlers. Every FT4-authenticated op needs one. Empty flags: [] means any descriptor of that account — usually too open for money or admin. App-scope handler applies to every unmatched op; two of them crash at auth time.

11. ft_auth / evm_auth adjacency. The authorized op must be immediately next. Extra ops in between fail. EVM nonce must track ctr or verification fails. Official clients handle this; raw Python/Kotlin/JS Postchain must not invent their own pairing.

12. Import surface. import lib.ft4.assets exposes ft4.transfer to every user with T. Use lib.ft4.core.assets if you want balances without the public transfer op.

13. Cross-chain origin tree. A wrong origin BRID can black-hole tokens. ICCF GTX module must be on every hop chain. Deadline / recall / cancel / unapply / revert are the only official recovery path.

14. EVM vs FT account IDs. hash(pubkey) is not hash(evm_address). Registering the same key both ways creates two accounts.

15. Example pins in docs are stale. Setup still shows FT4 v1.1.0r and Rell 0.14.9 while GitLab has newer v1.2.0r / v2.1.1 tags and Rell is at 0.16.x. Copy RID+tag from the release you actually use, not from a random cookbook snippet (one transfer-open example still cites Bitbucket ft3-lib v0.8.0r).

16. max_auth_descriptor_rules YAML key. Narrative docs put it at the top level; the Rell struct expects auth_descriptor.max_rules. Mis-keyed YAML may silently use the default 8.

## 8. Canonical URLs

### Intro / setup / config

- https://docs.chromia.com/build/ft4/intro
- https://docs.chromia.com/get-started/about/protocols/ft4
- https://docs.chromia.com/build/ft4/setup/ft4-setup
- https://docs.chromia.com/build/ft4/setup/imports
- https://docs.chromia.com/build/ft4/configuration-values
- https://docs.chromia.com/build/ft4/account-management/overview
- https://docs.chromia.com/build/ft4/account-management/auth-descriptors
- https://docs.chromia.com/build/ft4/terms

### Backend accounts / auth / assets / cross-chain

- https://docs.chromia.com/build/ft4/backend/accounts/overview
- https://docs.chromia.com/build/ft4/backend/accounts/accounts-and-auth-descriptors
- https://docs.chromia.com/build/ft4/backend/accounts/open
- https://docs.chromia.com/build/ft4/backend/accounts/fixed
- https://docs.chromia.com/build/ft4/backend/accounts/subscription
- https://docs.chromia.com/build/ft4/backend/authentication/auth
- https://docs.chromia.com/build/ft4/backend/authentication/auth-descriptors-and-rules
- https://docs.chromia.com/build/ft4/backend/authentication/multi-sig
- https://docs.chromia.com/build/ft4/backend/assets/register-assets
- https://docs.chromia.com/build/ft4/backend/assets/asset-amounts
- https://docs.chromia.com/build/ft4/backend/assets/locking-assets
- https://docs.chromia.com/build/ft4/asset-management/asset
- https://docs.chromia.com/build/ft4/asset-management/transfer-assets
- https://docs.chromia.com/build/ft4/backend/cross-chain/introduction
- https://docs.chromia.com/build/ft4/backend/cross-chain/cross-chain-transfers
- https://docs.chromia.com/build/ft4/backend/cross-chain/cross-chain-assets

### FT4 client (TS) guides

- https://docs.chromia.com/build/ft4/client/client-setup
- https://docs.chromia.com/build/ft4/client/client-login
- https://docs.chromia.com/build/ft4/client/client-auth-descriptors
- https://docs.chromia.com/build/ft4/client/client-account-registration
- https://docs.chromia.com/build/ft4/client/client-orchestrator
- https://docs.chromia.com/build/ft4/client/client-key-store
- https://docs.chromia.com/build/ft4/client/client-transfer-assets
- https://docs.chromia.com/build/clients/ft4-client
- https://docs.chromia.com/pages/ft4-ts-client/client/index.html

### Generated FT4 Rell API (stamp: Rell 0.16.1)

- https://docs.chromia.com/pages/ft4-rell/
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.accounts/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.accounts/module_args/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.accounts/rate_limit_config/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.accounts/auth_descriptor_config/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.accounts/auth_flags_config/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.accounts.auth_flags/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.auth/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.external.assets/transfer.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.external.auth/ft_auth.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.external.auth/evm_auth.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.external.crosschain/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.accounts.strategies.open/index.html

### GitLab source

- https://gitlab.com/chromaway/ft4-lib
- https://gitlab.com/chromaway/ft4-lib/-/blob/development/rell/src/lib/ft4/core/accounts/module.rell
- https://gitlab.com/chromaway/ft4-lib/-/blob/development/rell/src/lib/ft4/core/accounts/auth_flags_config.rell
- https://gitlab.com/chromaway/core/directory-chain (ICCF)

### Official clients

- https://docs.chromia.com/build/clients/overview
- https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/
- https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart
- https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference
- https://docs.chromia.com/build/clients/postchain-clients/kotlin-client
- https://docs.chromia.com/build/clients/postchain-clients/python-client
- https://github.com/ChromiaProject/postchain-client (Kotlin README; FT4 section)
- https://www.npmjs.com/package/postchain-client
- https://www.npmjs.com/package/@chromia/ft4

### EIF / EVM bridge (adjacent, not FT4 transfer)

- https://docs.chromia.com/build/cli/commands/eif
- https://docs.chromia.com/ecosystem/bridge/overview
- https://docs.chromia.com/build/clients/bridge-client

### Rell releases

- https://docs.chromia.com/rell/releases

## Gaps / do-not-invent

- Exact string of DEFAULT_LOGIN_CONFIG_NAME and the built-in default login_config map: not copied from a page in this pass.
- Full parameter lists for every ft4.crosschain.* Rell op beyond what /build/ft4/backend/cross-chain/cross-chain-transfers and the TS initTransfer page state.
- Whether narrative max_auth_descriptor_rules is accepted as a legacy alias: not stated. Use auth_descriptor.max_rules.
- A single current FT4 library tag: official setup still prints v1.1.0r; GitLab also has v1.2.0r / v2.1.1.
- Python/Kotlin FT4 high-level session/login (only Kotlin helper ops are documented).
- Any FT4-to-EIF transfer API: not documented.

