# FT4 official-docs study

**As-of:** 2026-08-26 (Asia/Jerusalem).
**Rule:** Only what official Chromia docs state. Nothing invented. Where official pages disagree with each other or with `/workspace/chromia-knowledge/ft4-clients.md`, that is called out.
**Crawl method:** sitemap.xml then every URL under `/build/ft4/`; WebFetch plus curl of those pages plus `/get-started/about/protocols/ft4`, `/reference/ft4/`, and generated FT4 Rell API (`/pages/ft4-rell/`) for the index and key modules (accounts, auth, assets, crosschain, admin, strategies.transfer, lib.ft4 module_args). `/build/ft4/` itself is 404 (no section landing page).

**Official trees crawled**

| Tree | Result |
| --- | --- |
| `https://docs.chromia.com/build/ft4/` | No index (404). 46 child URLs from sitemap, all HTTP 200 |
| `https://docs.chromia.com/get-started/about/protocols/ft4` | 200 |
| `https://docs.chromia.com/reference/ft4/` | 200 — pointer only (Rell API + TS client API) |
| `https://docs.chromia.com/pages/ft4-rell/` | 200. Module indexes: `lib.ft4`, `lib.ft4.core.accounts`, `.auth`, `.assets`, `.crosschain`, `.admin`, `.accounts.strategies.transfer`, `lib.ft4.external.{accounts,assets,auth,crosschain}`. `lib.ft4.accounts` / `.assets` / `.auth` / `.admin` / `.crosschain` (non-core) 404 on the generated site |

Generated API pages stamp **Rell 0.16.1** (confirmed on `lib.ft4.core.accounts` `module_args`). Some cached search snippets still show 0.16.0.

---

## 1. What FT4 is

Official intro (`/build/ft4/intro`): Chromia FT4 (Flexible Token 4) is a token standard for Chromia. Similar role to ERC-20 for token create / transfer / ownership tracking, plus flexible account access, multi-signature, configurable rules, rate limiting, and cross-chain transfers inside Chromia. Backend is Rell (`lib.ft4.*`), configured through `chromia.yml`. Client is TypeScript (`@chromia/ft4`).

Protocol page (`/get-started/about/protocols/ft4`): token registration, token transfers, account creation. Accounts (not bare addresses) are the unit of balance and operations. Accounts are registered on demand and cannot receive transfers until they exist (unlike EVM addresses). FT4 accounts can bind native Chromia keys or EVM addresses (MetaMask / Chromia Vault). Developers can also bridge assets to/from Ethereum or Binance Chain using Chromia's bridging framework (not FT4 ICCF).

Use FT4 when the dapp needs (intro):

- User accounts with authentication
- Token / asset management
- Multi-sig or role-based access
- Transfers between accounts
- Cross-chain asset transfers
- Built-in rate limiting and spam prevention

Do not need FT4 for simple Rell dapps (read-only store, voting without tokens). FT4 is optional.

Backend features listed on intro: Account management, Asset management, Authorization, Transfers, Cross-chain transfers, Pagination.

Reference page (`/reference/ft4/`): two external indexes — FT4 Rell library API (`/pages/ft4-rell/index.html`) and FT4 TypeScript client API (`/pages/ft4-ts-client/index.html`).

Generated library intro (`/pages/ft4-rell/`): out-of-the-box account creation and access management, external signature solutions, asset issuance / allocation / transfers / tracing, on-chain and across Chromia chains.

---

## 2. Import surface

Official modules (`/build/ft4/setup/imports`):

| Module | Role |
| --- | --- |
| `accounts` | Accounts, auth descriptors, rate limiting |
| `admin` | Dev / chain init. Mint, register assets/accounts, grant rate-limit points |
| `admin.crosschain` | Register cross-chain assets |
| `assets` | Assets, transfers, transfer history |
| `auth` | Account authentication; used by the login function |
| `cross-chain` | Transactions across chains in one Chromia network |
| `prioritization` | Transaction prioritization during congestion |
| `test` | Tests only; exposes no external functions |
| `utils` | Pagination utilities; exposes no external functions |

Import split:

- `import lib.ft4.<module>` — entities plus mounted queries/ops users can call
- `import lib.ft4.core.<module>` — implementation without exposing those external functions

Admin must not be in production. Cross-chain requires three pieces: `import lib.ft4.crosschain`, GTX module `net.postchain.d1.iccf.IccfGTXModule`, and `libs.iccf`.

Generated site has no `lib.ft4.accounts` (etc.) pages. External ops/queries live under `lib.ft4.external.*`. Narrative `import lib.ft4.accounts` pulls those in.


## 3. Accounts

### Entity (account-management overview + accounts-and-auth-descriptors)

```rell
entity account {
  key id: byte_array;
  index type: text;
}
```

Account types (glossary `/build/ft4/terms`):

- **User accounts** — only type the user controls by default; have an auth descriptor. Type constant on API: `ACCOUNT_TYPE_USER`.
- **Lock accounts** — associated with a user account; assets not directly accessible by the user. Dapp implements unlock rules. Type constant on locking page: `ACCOUNT_TYPE_LOCK` = `"FT4_LOCK"`. Any number of lock accounts per user.
- **System accounts** — not linked to a user; dapp-administrative (e.g. blockchain account tracking cross-chain balances). API: `ACCOUNT_TYPE_BLOCKCHAIN` = `"FT4_BLOCKCHAIN"` for downstream-chain balances.

Dapp-controlled accounts (registration overview): `create_account_without_auth(account_id, type)` — no auth descriptors; cannot be accessed by external signers. Use for pools, treasuries, locked funds, protocol fees, automated rewards.

### Account ID

Intro:

```
Account ID = hash(public_key)   // native Chromia
Account ID = hash(evm_address)  // EVM-compatible
```

Auth page (`/build/ft4/backend/authentication/auth`):

- FT (Postchain) signer: `hash(pubkey)`
- EVM signer: `hash(evm_address without "0x")`. Official formula: `hash(hash(pubkey).sub(0, 40))` — first 40 hex chars of the hashed pubkey.
- Same underlying key produces different IDs for FT vs EVM representation.

Accounts-and-auth-descriptors: `create_account_with_auth(auth_descriptor, account_id? = null)` derives ID from signers if `account_id` is omitted (`hash(pubkey)` or `hash(evm_address)`). Then sets that descriptor as main and creates `rl_state`.

API `get_account_id_from_signers`: "Default calculation for new account IDs." Narrative does not reprint the multi-signer formula on the pages crawled here.

One account can be controlled by many keypairs via auth descriptors. One keypair can sit on many accounts.

### Account link (`/build/ft4/backend/accounts/account-linking`)

```rell
entity account_link {
    key account, secondary;
    index secondary, type;
    accounts.account;
    secondary: accounts.account;
    type: text;   // e.g. "stake", "bid"
}
```

Used for lock accounts, auction bids, escrow, system-controlled holdings.

### Queries / ops (generated `lib.ft4.external.accounts`)

Operations: `add_auth_descriptor`, `delete_auth_descriptor`, `delete_all_auth_descriptors_except_main`, `delete_auth_descriptors_for_signer`, `update_main_auth_descriptor`.

Queries include: `get_account_by_id` (returns type as well as id since 1.1.0r), `get_account_auth_descriptors`, `get_account_auth_descriptors_by_signer`, `get_account_main_auth_descriptor`, `get_account_auth_descriptor_by_id`, `get_accounts_by_signer`, `get_accounts_by_type`, `get_accounts_by_auth_descriptor_id`, `get_auth_descriptor_counter`, `get_config`, `is_auth_descriptor_valid`, plus filtered/paginated variants added in 1.1.0r (`get_accounts_filtered`, `get_account_auth_descriptors_filtered`, `get_main_auth_descriptors_filtered`, `get_auth_descriptor_signers_filtered`, `get_rl_states_filtered`).

---

## 4. Auth descriptors, flags, rules

### Structs (accounts-and-auth-descriptors)

```rell
struct auth_descriptor {
    auth_type;
    args: list<gtv>;
    rules: gtv;
}

entity account_auth_descriptor {
    id: byte_array;
    key account, id;
    index id;
    auth_type;          // S single-sig or M multi-sig
    args: byte_array;   // encoded single_sig_args or multi_sig_args
    rules: byte_array;  // null -> GTV_NULL_BYTES; simple rule_expression; or list starting with "and"
    mutable ctr: integer;
    created: timestamp;
}

entity main_auth_descriptor {
    key account;
    key auth_descriptor: account_auth_descriptor;
}

entity auth_descriptor_signer {
    id: byte_array;   // pubkey, or EVM address without 0x
    key account_auth_descriptor, id;
}
```

Rules count must be `<= auth_descriptor_config.max_rules`. Main descriptor rules must be `GTV_NULL_BYTES`. Expired descriptors are deleted automatically when the owning account next sends an operation.

### Main vs other

- Every user account has exactly one main descriptor. It can be substituted, not deleted via the public update path.
- `update_main_auth_descriptor(account, auth_descriptor)` — operation-only; previous main is deleted; new one needs mandatory flags and no rules.
- `add_auth_descriptor` checks descriptor cap, args shape (`single_sig_args` / `multi_sig_args`), and that rules are initially valid; then `add_signers`.
- `delete_auth_descriptor` is for non-main descriptors.
- `delete_all_auth_descriptors_except_main` is the documented cleanup for leftover login/disposable descriptors.
- API also lists `delete_main_auth_descriptor` (leaves the account without a main — glossary/narrative warn this is unusable).

Multisig guide GTV template for a descriptor:

```
[ 0 -> single-sig OR 1 -> multi-sig,
  [ ["A","T"],               // flags
    5,                       // signatures_required if multi-sig; omit if single-sig
    x"pubkey" OR [x"pk1", x"pk2"]
  ],
  null                       // rules; must be null for main
]
```

Admin register-account example uses `0` for single-sig: `[0, [["A","T"], x"<pubkey>"], null]`.

### Flags

Built-in (glossary + account-management/auth-descriptors):

- `A` (Account): add/remove auth descriptors / edit account details. Super access — holder can add descriptors with any flag.
- `T` (Transfer): transfer and burn.

Dapps may define more flags. Narrative: "any string"; account-management page recommends shorter flags. API `require_valid_auth_flags`: letters and/or underscores.

`ft4.add_auth_descriptor` / `ft4.delete_auth_descriptor` fail without `A`. `ft4.transfer` requires `T`.

### Rules (`/build/ft4/backend/authentication/auth-descriptors-and-rules`)

Operators: `>`, `>=`, `=`, `<`, `<=`.

Variables:

- `operation_count` — only `<` and `<=`
- `block_time`, `block_height`
- `relative_block_height`, `relative_block_time` (relative to the block that registered the descriptor)

Statuses: Active, Inactive, Expired. Expired descriptors are cleaned on next account interaction.

Client helpers (`/build/ft4/client/client-auth-descriptors`): `lessThan(opCount(3))`, `lessOrEqual(blockHeight(100))`, `and(greaterOrEqual(blockTime(...)), lessThan(blockTime(...)))`, `ttlLoginRule(minutes(30))`.

Client TS types: `AuthType` `"S"` / `"M"`; `SingleSig { flags, signer }`; `MultiSig { flags, signaturesRequired, signers }`. Factories: `createSingleSigAuthDescriptorRegistration`, `createMultiSigAuthDescriptorRegistration`.

### Limits

- `auth_descriptor.max_number_per_account` default 10, hard-capped at 200 (`AUTH_DESCRIPTORS_PER_ACCOUNT_UPPER_BOUND` on API).
- `auth_descriptor.max_rules` default 8 (generated struct). Narrative pages also document a sibling key `max_auth_descriptor_rules` default 8 — see contradictions.


---

## 5. Auth handlers and auth operations

### `authenticate` (`/build/ft4/backend/authentication/auth`)

```rell
operation foo() {
    val account = auth.authenticate();
}
```

Missing handler: `Cannot find auth handler for operation <foo>`.

```rell
@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  scope = rell.meta(foo).mount_name,
  flags = []
);
```

Scopes:

- Operation-scope: `scope = rell.meta(foo).mount_name`
- Mount-scope: `scope = "mid.inner"`
- App-scope: omit `scope` — applies to ops without a more specific handler. Multiple app-scope handlers cause a runtime error at auth time.

`add_overridable_auth_handler` lets a library ship a default that a dapp can replace once.

Optional `message` (EVM UX) and `resolver`. Resolver args: `(args: gtv, account_id: byte_array, auth_descriptor_ids: list<byte_array>): byte_array?`. Returning `null` means none of the candidates is authorized. Official example: `ft4.delete_auth_descriptor` requires `A` unless the descriptor being deleted is the one authorizing the op.

Placeholders in custom messages: `{account_id}`, `{auth_descriptor_id}`. Message is decorated with blockchain RID and a nonce.

### Auth operations (generated `lib.ft4.external.auth`)

- `ft_auth` — FT signer. Must precede the authorized op. Whole tx is signed by required signers.
- `evm_auth` — EVM EIP-191 message for an existing descriptor.
- `evm_signatures` — EVM signer not yet on an account descriptor (registration).

API constants: `FT_AUTH_OP`, `EVM_AUTH_OP`, `EVM_ADDRESS_SIZE`, `FT_PUBKEY_SIZE`, placeholders `BLOCKCHAIN_RID_PLACEHOLDER`, `ACCOUNT_ID_PLACEHOLDER`, `AUTH_DESCRIPTOR_ID_PLACEHOLDER`, `NONCE_PLACEHOLDER`, `DEFAULT_LOGIN_CONFIG_NAME`, `APP_SCOPE`, `OVERRIDE_PREFIX`.

Queries: `get_all_auth_handlers`, `get_auth_flags`, `get_auth_handler_for_operation`, `get_auth_message_template`, `get_login_config`.

`login_config` is extendable via `add_login_config`. `_login_config` is documented as default auth-descriptor configurations the client can use.

`require_regular_next_operation`: the authorized op must be immediately next. Official clients insert `ft_auth` / `evm_auth` for you.

1.1.0r added `auth_op_blacklisted_operations` (ops that must not follow an auth op) and `evm_signatures_authorized_operations` (whitelist for `evm_signatures`). Both live under `lib.ft4.core.auth` moduleArgs (generated struct). The generated page's YAML example for the blacklist field is copy-pasted as `evm_signatures_authorized_operations` — see contradictions.

CLI: `--ft-auth` inserts `ft4.ft_auth`. Same page later says CLI only supports `--ft4-auth` (client-transfer-assets). Multisig CLI uses `--ft-auth` and `--auth-descriptor-id` / `-id`.

FT vs EVM signing:

- FT: signer on tx `signers`; signature of the whole transaction.
- EVM: not a tx-signer; signs a human-readable message (account id, auth descriptor id, nonce, blockchain RID).

### Login / disposable keys (`/build/ft4/client/client-login`)

```js
const { session, logout } = await login({
  accountId,
  config: { flags: ["T"], rules: ttlLoginRule(minutes(30)) }, // optional
  loginKeyStore: createSessionStorageLoginKeyStore(),         // optional
});
await session.call(op("foo"), op("bar", "...", 123));
await logout();
```

Defaults if `config` omitted: flags `[]`, timeout one day.

Login keystores: `createSessionStorageLoginKeyStore`, `createLocalStorageLoginKeyStore`, plus any `LoginKeyStore`. In-memory is the default if omitted — cleared on reload; each reload adds another descriptor and can hit the cap.

Official warning: never put flags on a disposable key that could compromise assets if the key leaks. Do not put `A` on login keys.

`logout` removes the disposable descriptor. Always logout: leak risk plus descriptor cap.

Code-examples also show `keyStoreInteractor.login({ accountId, flags: ["0"] })` (flags at top level, not under `config`) for a custom `"0"` flag. That is a second official calling shape.

---

## 6. Rate limiting

Account-management overview:

- Accumulate one operation point every `recovery_time` ms, up to `max_points`.
- Spend one point per authenticated operation.

`lib.ft4.core.accounts` YAML:

```yaml
rate_limit:
  active: true          # default true (API: active: boolean = 1)
  max_points: 10        # default 10
  recovery_time: 5000   # ms per recovered point
  points_at_account_creation: 1  # min 0
```

`chr query ft4.get_config` sample on setup page returns only the rate_limit block (active=1, max_points=10, points_at_account_creation=1, recovery_time=5000).

Per-account override (registration overview):

```rell
@extend(get_rate_limit_config_for_account)
function get_rate_limit_config_for_account(account: account): rate_limit_config? {
    // custom config, or null for global default
}
```

API also: `add_rate_limit_points`, `current_rate_limit_points`, `rate_limit(...)`, `create_rate_limiter_state_for_account`.

Wording on registration-overview vs configuration-values vs API disagrees slightly — see contradictions.

---

## 7. Assets

### Entity (asset-management/asset)

```rell
entity asset {
   key id: byte_array;
   name;
   key symbol: text;          // changelog 0.8.0r removed the key constraint — see contradictions
   decimals: integer;
   issuing_blockchain_rid: byte_array;
   icon_url: text;
   type: text = ASSET_TYPE_FT4;   // query output shows "ft4"
   mutable total_supply: big_integer;
}
```

Changelog 0.8.0r also added `uniqueness_resolver` and made `icon_url` mutable. 0.6.0r added `type`.

```rell
entity balance {
  key accounts.account, asset;
  mutable amount: big_integer;
}
```

Validation (asset-amounts + generated assets API):

- `id`: exactly 32 bytes
- `name` / `symbol`: at most 1024 characters
- `decimals`: `[0, 78]` inclusive
- `type`: non-empty, at most 1024
- `uniqueness_resolver`: at most 1024 bytes
- Amounts: interval `(0, 2^256)` exclusive (`require_zero_exclusive_asset_amount_limits`); `max_asset_amount` for EVM int256 compatibility
- `format_amount_with_decimals(11, 2)` -> `"0.11"`

Balance helpers: `increase_balance` (creates row if needed, does **not** update total supply), `deduct_balance` (deletes row at zero; throws `INSUFFICIENT BALANCE` / `INVALID AMOUNT`). Mint/burn on a non-issuing chain: `UNAUTHORIZED MINTING` / `UNAUTHORIZED BURNING`.

Glossary vs ERC-20: users can always burn (reduces supply); no empty-address burn; minting is only native on admin (do not ship admin); no native max-supply.

### Registration

Admin (dev only):

```
chr tx ft4.admin.register_asset TestAsset TST 6 https://url-to-asset-icon --secret .chromia/ft4-admin.keypair --await
```

Custom (production path):

```rell
val asset_id = (asset_name, chain_context.blockchain_rid).hash();
assets.Unsafe.register_asset(asset_name, symbol, decimals, chain_context.blockchain_rid, icon_url);
```

Verify: `chr query ft4.get_all_assets page_size=10 page_cursor=null`. Sample fields: `blockchain_rid`, `decimals`, `icon_url`, `id`, `name`, `supply`, `symbol`, `type: "ft4"`.

### Transfer (on-chain)

Mounted when `lib.ft4.assets` is imported: `ft4.transfer`. Requires `T`. Amount in `(0, 2^256)` exclusive. If recipient exists: normal transfer. If not: starts account-creation-on-transfer when that strategy is enabled; otherwise error. `recall_unclaimed_transfer` recalls a transfer to a never-claimed non-existing account.

Extensible: `before_transfer` / `after_transfer`, plus `before_mint` / `after_mint` / `before_burn` / `after_burn`. `is_create_on_internal_transfer_enabled` / `create_on_internal_transfer` / `recall_on_internal_transfer`.

CLI: `chr tx ft4.transfer RECIPIENT ASSET AMOUNT --ft-auth --await --secret ...`

TS: `session.account.transfer(recipientId, assetId, amountToSend)` with `createAmount(10, 6)`. Also `session.account.burn(assetId, amount)`.

Mint (admin): `chr tx ft4.admin.mint SENDER_ACCOUNT_ID ASSET_ID AMOUNT_WITH_DECIMALS --await --secret ...`

### Locking (`/build/ft4/backend/assets/locking-assets`)

`ACCOUNT_TYPE_LOCK = "FT4_LOCK"`. Functions: `ensure_lock_account(type, account)`, `get_lock_accounts`, `get_lock_accounts_with_non_zero_balances`, `lock_asset`, `unlock_asset`, plus paginated `get_locked_asset_balance`, `get_locked_asset_aggregated_balance`, `get_locked_asset_balances`, `get_locked_asset_aggregated_balances`.

### External assets API (`lib.ft4.external.assets`)

Ops: `transfer`, `burn`, `recall_unclaimed_transfer`.

Queries: `get_all_assets`, `get_asset_by_id`, `get_asset_balance`, `get_asset_balances`, `get_assets_by_name`, `get_assets_by_symbol`, `get_assets_by_type`, `get_assets_filtered`, `get_balances_filtered`, `get_transfer_history`, `get_transfer_history_entry`, `get_transfer_history_from_height`, `get_transfer_history_entries_filtered`, `get_transfer_details`, `get_transfer_details_by_asset`, `get_crosschain_transfer_history_entries_filtered`, `get_asset_details_for_crosschain_registration`.


---

## 8. Account registration strategies

Enable by import plus `moduleArgs`. Official four:

| Strategy | Module | Fee | Official production note |
| --- | --- | --- | --- |
| Open | `lib.ft4.core.accounts.strategies.open` | None | Not recommended (dev/test) |
| Transfer open | `...strategies.transfer.open` | Transfer required, no fee | Safe with safeguards (same-address / extra limits) |
| Transfer fee | `...strategies.transfer.fee` | One-time to `fee_account` | Recommended |
| Transfer subscription | `...strategies.transfer.subscription` | Recurring | Recommended |

Open: anyone can call `register_account()`.

Transfer family: send assets to a not-yet-existing account ID, then claim via `register_account`. After `timeout_days` the sender can recall.

### Transfer rules (`lib.ft4.core.accounts.strategies.transfer`)

```yaml
lib.ft4.core.accounts.strategies.transfer:
  rules:
    - sender_blockchain: ...
      sender: ...
      recipient: ...
      asset:
        - name: CHR
          min_amount: 5L
      timeout_days: 30
      strategy:
        - "fee"
        - "open"
```

Special values (narrative): `$` current chain BRID; `*` any; `X` sender = recipient (must be used together). Generated API names: `CURRENT_CHAIN_REF`, `ANY_REF`, `CURRENT_ACCOUNT_REF`.

`sender_blockchain` formats (API): current-chain ref; a specific RID; any; or a list.

`sender` / `recipient`: a specific account ID; any; current-account ref (same ID across chains; only works if the other side matches and cross-chain is allowed); or a list.

`asset`: one `asset_limit`, a list, or any.

`strategy`: one name or a list of names.

Fee amount must be lower than `min_amount` or users can send too little and wait for timeout to recall.

### Fee moduleArgs

```yaml
lib.ft4.core.accounts.strategies.transfer.fee:
  asset:
    - id: x"..."          # xor name / name+issuing_blockchain_rid
      amount: 2L
    - name: test1         # issued by this chain
      amount: 1L
    - issuing_blockchain_rid: x"..."
      name: test1
      amount: 3L
  fee_account: x"..."
```

`id` cannot be accompanied by name or issuing chain RID.

CHR example (registration overview): `name: "CHR"`, `issuing_blockchain_rid: x"15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA"` (Economy chain RID as printed), `amount: 1L` with comment "10 CHR (6 decimals)".

One overview example imports `lib.ft4.accounts.strategies.transfer.fee` (no `core`) — see contradictions.

### Subscription moduleArgs

```yaml
lib.ft4.core.accounts.strategies.transfer.subscription:
  asset:
    - name: "MyTestAsset"
      amount: 10L
  subscription_period_days: 30
  free_operations:
    - some_free_operation
  subscription_account: x"..."
```

When the period ends the account becomes inactive. Renew by paying again. `free_operations` remain callable without an active subscription.

EVM registration order: `evm_signatures` then `ras_transfer_subscription` then `register_account`. Non-EVM: `ras_transfer_subscription` then `register_account`.

The open-strategy narrative page does not reprint a mounted op name. The subscription page names `ras_transfer_subscription`. Official client registration page uses `registrationStrategy.open(...)`.

### Client registration (`/build/ft4/client/client-account-registration`)

Requires `import lib.ft4.external.accounts.strategies` plus the concrete strategy module.

```ts
registerAccount(client, keyStore, registrationStrategy.open(authDescriptor))
```

Multisig guide also uses `registrationStrategy.transferFee(tchr, ad)`. Code-examples use `registerAccountAdmin(client, adminSigProv, authDesc)` for admin path.

Account ID derivation example in multisig guide: `pcl.gtv.gtvHash(kp.pubKey)`.

Economy Chain Testnet example BRID: `090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874`. Directory Chain testnet BRID in that guide: `6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92`. Fee on Economy Chain Mainnet stated as 10 CHR at time of writing; Testnet example pays 10 tCHR.

### Custom registration

Call `accounts.create_account_with_auth(auth_descriptor, account_id?)` from your own op after your own anti-spam. Official voucher example: admin `add_voucher(hash)`, then `register_account(auth_descriptor, voucher_code)` checks signer, unused voucher, then `create_account_with_auth`.

### Admin registration

```
chr tx ft4.admin.register_account \
  '[0, [["A","T"], x"<pubkey>"], null]' \
  --await --secret .chromia/ft4-admin.keypair
```

---

## 9. moduleArgs (exact official shapes)

### `lib.ft4` (generated)

```rell
struct module_args {
    query_max_page_size: integer = 100;
}
```

### `lib.ft4.core.accounts` (generated, Rell 0.16.1)

```rell
struct module_args {
    rate_limit: rate_limit_config;
    auth_descriptor: auth_descriptor_config;
    auth_flags: auth_flags_config;
}

struct rate_limit_config {
    active: boolean = 1;
    max_points: integer = 10;
    recovery_time: integer = 5000;
    points_at_account_creation: integer = 1;
}

struct auth_descriptor_config {
    max_rules: integer = 8;
    max_number_per_account: integer = 10;  // clamped to 200
}

struct auth_flags_config {
    mandatory: gtv;            // comment: "All auth descriptors must have these flags to exist"
    default: gtv? = null;      // if null, copies mandatory
}
```

Narrative `/build/ft4/configuration-values` does **not** document `auth_flags`. It documents `max_auth_descriptor_rules` as a sibling of `auth_descriptor`, not `auth_descriptor.max_rules`. Example YAML on that page:

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

`auth-descriptors` page also uses the sibling key `max_auth_descriptor_rules: 4`.

Changelog 0.7.0r: "Added `auth_flags` config to define mandatory and default authentication flags." Changelog 0.6.0r: `get_config` gained `max_rules` and `max_number_per_account`. Changelog 0.2.0r originally added `max_auth_descriptor_rules` on `lib.ft4.accounts` (pre-`core` path). 0.7.0r moved all submodule moduleArgs under `lib.ft4.core.*`.

### `lib.ft4.core.auth` (generated)

```rell
struct module_args {
    evm_signatures_authorized_operations: gtv? = null;
    auth_op_blacklisted_operations: gtv? = null;
}
```

YAML as documented for the first field:

```yaml
evm_signatures_authorized_operations:
  - op_1
  - op_2
```

Parsed to a set; duplicates fail. Standard FT4 ops that already require `evm_signatures` need not be listed. If a tx contains `evm_signatures` but does not authorize an op on this list, it fails (anti-weight).

Blacklist: ops that must never follow an auth op (typically ops that do not call `authenticate` and are not rate-limited). 1.1.0r changelog: defined as `core.auth.auth_op_blacklisted_operations`.

### `lib.ft4.core.admin` (generated + narrative)

```rell
struct module_args {
    admin_pubkey: byte_array;  # required if admin imported; chr keygen
}
```

### `lib.ft4.core.accounts.strategies.transfer` (generated)

```rell
struct module_args {
    rules: gtv;  // GTV-encoded list<module_args_list_element>
}
```

Fields of `module_args_list_element`: `sender_blockchain`, `sender`, `recipient`, `asset`, `timeout_days`, `strategy` (all as above).

### `lib.ft4.core.accounts.strategies.transfer.fee` / `.subscription`

Documented only on narrative strategy pages (not re-fetched as generated structs in this crawl). Fee: `asset[]` plus `fee_account`. Subscription: `asset[]` plus `subscription_period_days` plus `free_operations` plus `subscription_account`.

### Library pins printed on official setup pages

```yaml
libs:
  ft4:
    registry: https://gitlab.com/chromaway/ft4-lib.git
    path: rell/src/lib/ft4
    tagOrBranch: v1.1.0r
    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"
    insecure: false
  iccf:
    registry: https://gitlab.com/chromaway/core/directory-chain
    path: src/lib/iccf
    tagOrBranch: 1.87.0
    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"
    insecure: false

compile:
  rellVersion: 0.14.9  # setup page: "Or your rell version if yours is newer"
```

Strategy cookbook examples still show ICCF `tagOrBranch: 1.32.2`, `path: src/iccf`, RID `x"1D567580C717B91D2F188A4D786DB1D41501086B155A68303661D25364314A4D"`, and `rellVersion: 0.13.14`. Transfer-open cross-chain example still cites Bitbucket `chromawallet/ft3-lib` `v0.8.0r` RID `x"B6AE6AC82AC735BFB9E4E412FFB76BF95380E94F371F5F6A14E71A3AA7D5FEF6"` and Rell 0.14.5.


---

## 10. Client (@chromia/ft4)

Install the TypeScript package named at-chromia/ft4.

Official setup: createClient from postchain-client with nodeUrlPool http://localhost:7740 and blockchainIid 0, then createConnection(client). getAllAssets returns data plus nextCursor (null when empty).

Orchestrator and automate-registration also use directoryNodeUrlPool plus blockchainRid, and createClientToBlockchain(client, originBlockchainRid).

Factories documented on /build/ft4/client/* and /build/ft4/code-examples: createConnection, createClientToBlockchain, createKeyStoreInteractor, createInMemoryFtKeyStore, createInMemoryEvmKeyStore, createWeb3ProviderEvmKeyStore, createGenericEvmKeyStore, createSessionStorageLoginKeyStore, createLocalStorageLoginKeyStore, createSingleSigAuthDescriptorRegistration, createMultiSigAuthDescriptorRegistration, registerAccount, registrationStrategy.open and transferFee, registerAccountAdmin, registerCrosschainAsset, getAssetDetailsForCrosschainRegistration, createAmount, ftSigner, addAuthDescriptor.

createKeyStoreInteractor(client, keyStore) yields getAccounts, login, getSession.

Session: session.account (authenticated: transfer, burn, addAuthDescriptor, crosschainTransfer, getBalanceByAssetId, getBalances), session.call(op(...)), session.sign(tx), session.transactionBuilder().

TransactionBuilder: add(operation, config), build(). EVM signers must sign before FT signers.

Paginated client: getAllAssets(), getAllAssets(3), getAllAssets(3, previous.nextCursor). Default page 100.

Connection is read-only (no key). Session is authenticated.

KeyStore: id (Buffer), isInteractive (boolean). EvmKeyStore: address without 0x, signMessage. FtKeyStore: pubKey, sign(transaction).

CLI --ft-auth for native; EVM auth is client-lib only. Same transfer page also says CLI only supports --ft4-auth (contradiction).

---

## 11. Cross-chain (Chromia-to-Chromia, ICCF)

Not EIF. Not the EVM bridge.

Setup: (1) import lib.ft4.crosschain; (2) config.gtx.modules includes net.postchain.d1.iccf.IccfGTXModule; (3) libs.iccf as on the setup page.

Origin tree: issuing chain is the mint root. Each other chain registers the asset with an origin_blockchain_rid (parent). Transfers walk the tree. Sibling chains cannot send directly; they go through the parent. Children do not track parent balance; parent tracks children via blockchain accounts (ACCOUNT_TYPE_BLOCKCHAIN). Origin registration is not validated on the origin chain. Wrong or malicious origin can lose funds.

asset_origin entity: asset plus origin_blockchain_rid. ensure_blockchain_account creates the downstream-chain account.

Register cross-chain asset params (cross-chain-assets page): asset_id, name, symbol, decimals (0-78), issuing_blockchain_rid, origin_blockchain_rid, icon_url, type (typically ASSET_TYPE_FT4), uniqueness_resolver. Origin and issuing RID must not be the current chain. ID 32 bytes.

Admin CLI (orchestrator page): chr tx --blockchain-rid CHILD ft4.admin.register_crosschain_asset TestAsset TST 6 ASSET_BRID icon-url PARENT_BRID --await --secret .chromia/ft4-admin.keypair

TS registerCrosschainAsset: orchestrator page uses (childClient, adminSignatureProvider, asset, parentBrid). Automate page uses (chromiaClient, adminSignatureProvider, assetId, originBlockchainRid). See contradictions.

Automate page also: getAssetDetailsForCrosschainRegistration(originClient, assetId) then optionally require asset.blockchainRid == originBlockchainRid.

0.8.0r: register_crosschain_asset gained asset_id, asset_type, uniqueness_resolver.

Transfer hops (official):

Happy path: (1) ft4.crosschain.init_transfer on source (lock/burn; record hops plus deadline); (2) ft4.crosschain.apply_transfer on each hop/target (needs iccf_proof of previous hop); (3) ft4.crosschain.complete_transfer on source after last apply.

Expiry/failure: (4) cancel_transfer on next hop after expiry (anyone with proof); (5) unapply_transfer on hops already applied; (6) revert_transfer on source to return funds; (7) recall_unclaimed_transfer on target if recipient account never created (only if create-on-crosschain-transfer is enabled).

Hop direction decides lock / mint / unlock / burn. Next chain verifies previous tx inclusion via ICCF / anchoring.

init_transfer params (transfers page): asset_id, amount, destination, hops, deadline. Throws on insufficient balance, invalid destination, invalid hops. Transfers applied after deadline fail and can only be reverted.

apply_transfer params: init_transfer_tx, init_tx_op_index, previous_hop_tx, op_index, hop_index.

TS orchestrator: session.account.crosschainTransfer(targetChainId, recipientId, assetId, amount) emits signed / init / hop. Errors are descendants of OrchestratorError.

Extendable (changelog): before/after_init_transfer, before/after_apply_transfer; 1.1.0r before/after_crosschain_balance_change inside Unsafe.update_balances_if_needed. is_create_on_crosschain_transfer_enabled.

External crosschain queries: get_pending_transfers_for_account, is_transfer_applied, get_asset_origin_by_id, get_init_transfer_details, get_apply_transfer_details, get_apply_transfer_tx, plus filtered variants for applied / canceled / unapplied / recalled / pending / reverted / asset_origin.

EVM bridge vs EIF: /build/ft4/asset-management/transfer-assets documents on-chain, Chromia-to-Chromia, and Chromia-to-EVM via the EVM Bridge (/ecosystem/bridge/...). Protocol page also mentions bridging to Ethereum / BSC. No official FT4 page in this crawl defines an FT4 eif_* operation. EVM inside FT4 is authentication (evm_auth, MetaMask keystore), not bridging.

---

## 12. Pagination

/build/ft4/pagination: every FT4 query is paginated by default. Returns data plus cursor.

Utils components: page_cursor, pagination_result (data plus rowid), paged_result (list plus next cursor), encode_cursor / decode_cursor (Base64), fetch_data_size, make_page, before_rowid.

lib.ft4.query_max_page_size default 100 (0.4.0r: page_size optional; default from this field).

Client: nextCursor; getAllAssets(pageSize, cursor).

---

## 13. Prioritization

Import lib.ft4.core.prioritization.default to apply rate-limit rules to the tx queue:

- More points means higher priority
- Rate-limit disabled / exempt gets highest priority (no_account_priority_state(1.0))
- Would-be-rejected txs rejected on submit
- Full queue: lower-priority txs can be dropped

Custom: import lib.ft4.core.prioritization and extend priority_check(tx_body: gtx_transaction_body, tx_size: integer, tx_enter_timestamp: timestamp, current_timestamp: timestamp): priority_state_v1?

Default logic: extract auth ops, then account plus descriptor; if rate-limited, priority = current_points / max_points capped to [0, 1]; else high priority. Multiple accounts: first valid account. No valid auth op: no_op_priority_state() priority 0.0.

priority_state_v1: account_id, account_points, tx_cost_points, priority (zero or positive decimal; higher = first). Query priority_check_v1 calls priority_check. Example: 50/100 points = 0.5. Tx cost 20 in that example.

---

## 14. Releases (official changelog /build/ft4/releases/ft4)

The official changelog ends at 1.1.0r (2025-02-25). No 1.2.x or 2.x entry on this page.

| Version | Date | Notes that matter |
| --- | --- | --- |
| 1.1.0r | 2025-02-25 | Multiple smaller transfers to a non-existing account for the same asset; multi-sender; register_account for crosschain fee/open/subscription does not require a signature when transferring to the same account; get_account_by_id also returns type; auth-op blacklist; get_api_version (integer, starts at 1 for 1.1.0); get_block_height; many *_filtered queries; before/after_crosschain_balance_change; authenticate no longer deletes the descriptor that just expired during auth |
| 1.0.0r | 2024-07-04 | Filter on get_pending_transfer_strategies; get_auth_message_template null-args fix |
| 0.8.0r | 2024-05-29 | Removed key on asset.symbol; get_asset_by_symbol becomes get_assets_by_symbol; register_crosschain_asset adds asset_id/asset_type/uniqueness_resolver; icon_url mutable; get_transfer_rules; multisig signatures_required > 0; update_main_auth_descriptor checks mandatory flags |
| 0.7.0r | 2024-04-23 | deadline on init_transfer; revert/recall; moduleArgs path gains core (lib.ft4.core.accounts); auth_flags; lock accounts; delete_all_auth_descriptors_except_main; get_enabled_registration_strategies; create_account_with_auth no longer verifies signatures (use verify_signers); default account id from signers hash; requires Rell 0.13.10 |
| 0.6.0r | 2024-03-22 | Asset type; get_config includes auth-descriptor limits; default login expiry 1 day; delete_auth_descriptors_for_signer |
| 0.5.0r | 2024-02-29 | Mount ft becomes ft4; subscription strategy; configurable fee account; cross-chain before/after hooks |
| 0.4.0r | 2024-02-15 | Account-creation framework (open / fee / transfer-open); query_max_page_size; transfer can create accounts; create_account_with_auth requires A by default |
| 0.3.0r | 2024-01-17 | Removed auth types ES/EM; overridable handlers; resolver; get_first_allowed_auth_descriptor |
| 0.2.0r | 2023-12-22 | Pagination; rules rewrite; brid becomes blockchain_rid; participant becomes signer; max_auth_descriptor_rules; transaction priority |
| 0.1.5r | 2023-10-19 | Cross-chain plus admin.crosschain |
| 0.1.0r | 2023-07-12 | Initial |

get_api_version: integer bumped when queries/ops change; 1 at version 1.1.0.

---

## 15. Multi-sig (practical official path)

There is no distinct multisig account type. A descriptor is S or M. An account can have both.

Guide /build/ft4/account-management/multisig: start singlesig, convert via ft4.update_main_auth_descriptor. Do not require all signers (one lost key locks the account). Signers file plus chr multi-signature create / sign / send / view. --ft-auth plus -id AUTH_DESCRIPTOR_ID prepends ft4.ft_auth. View output shows ft4.ft_auth, the real op, and nop.

Rell helper in backend multi-sig page: multi_sig_auth_descriptor(signers, 3, set(["A", "T"])).

---

## 16. Contradictions vs /workspace/chromia-knowledge/ft4-clients.md

ft4-clients.md is a prior production brief that mixed official docs with GitLab ft4-lib source. This study is official-docs only. Differences:

1. FT4 tag beyond 1.1.0r. ft4-clients.md reports GitLab tags v1.2.0r, v2.1.1, v2.1.2-beta.0. Official pages in this crawl still pin v1.1.0r and the changelog stops at 1.1.0r. No official narrative or generated page fetched here documents 1.2 / 2.x.

2. Rell stamp. ft4-clients.md correctly notes generated pages now stamp Rell 0.16.1. Confirmed on lib.ft4.core.accounts module_args. Official setup examples still print compile.rellVersion 0.14.9 (or 0.13.14 / 0.14.5 in cookbooks). Official changelog last required Rell is 0.13.10 (0.7.0r). ft4-clients.md claim that language-level Rell is 0.16.4 is not on the FT4 pages crawled here.

3. auth_flags. Both agree: generated module_args includes auth_flags; narrative configuration-values omits it. API comment says every descriptor must have mandatory flags; login docs create descriptors with flags []. ft4-clients.md explains from source that require_mandatory_flags is only called from create_account_with_auth and update_main_auth_descriptor. Official docs do not say that. Official docs only: main descriptors need mandatory flags; login default flags are [].

4. max_auth_descriptor_rules vs auth_descriptor.max_rules. Both agree this is a real official-docs vs generated-struct mismatch. Prefer the generated struct (auth_descriptor.max_rules) as what the compiler binds. ft4-clients.md additionally cites GitLab source defaults, not restated here.

5. ICCF pin. Both agree setup/imports = 1.87.0 plus RID 9C35...575D plus path src/lib/iccf, while some strategy pages still show 1.32.2 plus a different RID plus path src/iccf.

6. Bitbucket ft3-lib v0.8.0r in the transfer-open cross-chain example. Both flag it as stale. Official page is still live with that pin.

7. EIF. ft4-clients.md section 3 EIF write-up is outside the FT4 official trees (CLI chr eif, governance starter-kit). This study: official FT4 pages do not document EIF as an FT4 transfer API. They document EVM auth and point at the separate EVM Bridge for Chromia-to-EVM assets. Protocol page mentions Chromia bridging framework without naming EIF.

8. DEFAULT_LOGIN_CONFIG_NAME. Generated auth module lists the constant. Exact string is still not printed on narrative pages. ft4-clients.md already said so.

9. get_account_id_from_signers multi-signer formula (hash of sorted signers if many). ft4-clients.md attributed this to source. Official API blurb is only Default calculation for new account IDs. Official narrative: single FT = hash(pubkey), single EVM = hash(evm_address). Multi-signer ID formula is not on the official pages crawled here.

10. Signer sizes 20 / 33 and AUTH_DESCRIPTORS_PER_ACCOUNT_UPPER_BOUND 200. Generated API states EVM address size, FT pubkey size, and the 200 cap on auth_descriptor_config. ft4-clients.md also cited validate_auth_descriptor_args from source. Official generated pages confirm the sizes as constants and the 200 clamp; they do not reprint validator throw strings on the indexes fetched.

11. Client extra factories. ft4-clients.md lists createConnectionToBlockchainRid, createSession, createAuthDataService, createFtKeyHandler, createEvmKeyHandler, ftAuth, callWithoutNop, signAndSend, buildAndSendWithAnchoring from /pages/ft4-ts-client/ and /build/clients/ft4-client. Those URLs are outside the four trees this study was asked to crawl. Not confirmed here. What is on /build/ft4/client/* and /build/ft4/code-examples is listed in section 10.

12. hasActiveLogin reuse-almost-expired caveat. Stated in ft4-clients.md from the login page. The login page crawled here documents keystore reuse for UX and the descriptor-cap problem; it does not restate the rules are not re-validated except expiry sentence in the extracted text. Treat that specific caveat as not confirmed on this crawl.

13. Open op name ft4.ras_open. ft4-clients.md printed the mounted signature. Official open-strategy page in this crawl describes the strategy and YAML but does not reprint that mount. Subscription page does name ras_transfer_subscription. Do not treat ras_open as confirmed by this crawl.

14. Rate-limit source behavior (accumulate up to max_points, spend one per authenticated op). Official account-management overview matches that. Registration-overview table text (points consumed when creating an account, max points before rate limiting is triggered) does not. ft4-clients.md used source to pick the first reading. Official docs internally disagree.

---

## 17. Official-page-internal contradictions (this crawl)

1. max_auth_descriptor_rules (sibling) vs auth_descriptor.max_rules (generated struct). configuration-values plus auth-descriptors pages vs /pages/ft4-rell/.../auth_descriptor_config.

2. auth_flags missing from configuration-values; present on generated module_args and in changelog 0.7.0r.

3. asset.symbol still marked key on /build/ft4/asset-management/asset; changelog 0.8.0r removed the key constraint.

4. CLI flag name: client-transfer-assets command uses --ft-auth; a note on the same page says CLI only supports --ft4-auth. Multisig pages use --ft-auth.

5. registerCrosschainAsset signature: orchestrator (client, adminSig, asset, parentBrid) vs automate-registration (client, adminSig, assetId, originBrid).

6. Fee import path: registration overview once shows import lib.ft4.accounts.strategies.transfer.fee (no core); enable-list uses lib.ft4.core.accounts.strategies.transfer.fee. After 0.7.0r, moduleArgs are under core; the public import name without core is still how you expose ops.

7. ICCF / FT4 pins differ across setup vs strategy cookbooks (1.87.0 vs 1.32.2; gitlab ft4-lib v1.1.0r vs bitbucket ft3-lib v0.8.0r).

8. lib.ft4.core.auth module_args page documents auth_op_blacklisted_operations but the YAML example under that field is titled evm_signatures_authorized_operations.

9. Subscription option 2 shows id plus min_amount (transfer-rule field) under a subscription asset block that elsewhere uses amount.

10. Rate-limit wording (see section 6 / contradiction 14).

11. Protocol page cannot receive transfers until the account exists vs transfer strategies that require a transfer to a non-existing account. The exception is the transfer-registration framework.

12. Login API shape: login({ accountId, config: { flags, rules }, loginKeyStore }) on client-login vs login({ accountId, flags: ["0"] }) on code-examples.

13. createClient field casing in code-examples: nodeURLPool / blockchainIID / blockchainIiD vs nodeUrlPool / blockchainIid on setup/client-setup. Official setup page uses nodeUrlPool plus blockchainIid.

---

## 18. Coverage and gaps

### Covered (official pages in the four trees)

- What FT4 is; when to use it; protocol-level account model
- Import vs core import; admin warning; ICCF wiring
- Account entity, types (user / lock / system), linking, dapp-controlled accounts
- Auth descriptors (main / disposable), flags A/T, rules, statuses, GTV template
- Auth handlers (scopes, overridable, resolver, messages), ft_auth / evm_auth / evm_signatures
- Login / disposable keys / login keystores / logout / descriptor cap
- Rate limit moduleArgs plus per-account extend
- Assets, amounts, burn/mint/transfer, locking
- Four registration strategies plus transfer rules plus fee/subscription YAML
- Client connection, session, transfer, registration, keystores, orchestrator, transaction builder, admin client helper
- Cross-chain origin tree, hops, cancel/unapply/revert/recall, ICCF, automate registration example
- Pagination primitives and query_max_page_size
- Prioritization default plus priority_check
- Official changelog through 1.1.0r
- Generated module indexes for accounts / auth / assets / crosschain / admin / transfer strategies / lib.ft4 module_args
- Multisig guide (convert main descriptor, CLI multi-signature)

### Gaps (not on official pages in this crawl, or not fetched)

- /build/ft4/ index does not exist (404).
- Generated pages for the public import names lib.ft4.accounts / .assets / .auth / .admin / .crosschain 404; docs live under core.* and external.* only.
- Exact string of DEFAULT_LOGIN_CONFIG_NAME, CURRENT_CHAIN_REF, ANY_REF, CURRENT_ACCOUNT_REF: named, not printed.
- Full field lists for every generated function/op (only indexes plus key structs were fetched, not every function page).
- lib.ft4.external.admin index fetch timed out; admin ops known from narrative: register_account, register_asset, register_crosschain_asset, mint, plus rate-limit points (imports page).
- Mounted names ras_open / ras_transfer_open / ras_transfer_fee: only ras_transfer_subscription is printed on a narrative page in this crawl.
- Multi-signer account-id formula (sorted-hash) not on official pages fetched.
- TS client generated API (/pages/ft4-ts-client/) and /build/clients/ft4-client were outside the requested trees.
- Official changelog has nothing after 1.1.0r (2025-02-25). Whether 1.2 / 2.x exist is not stated on these official pages.
- hasActiveLogin semantics beyond durable keystore reuses the key.
- EIF / EVM-bridge operational detail (pointed at, not specified here).
- Cookbook pages under /build/cookbook/** and /build/integrations/exchange-guide/** mention FT4 but are outside the requested trees.

### Pages fetched (narrative, all 200)

intro, terms, configuration-values, code-examples, pagination, prioritization, releases/ft4, setup/, setup/ft4-setup, setup/imports, account-management/, account-management/auth-descriptors, account-management/multisig, account-management/overview, asset-management/, asset-management/asset, asset-management/transfer-assets, backend/, backend/accounts/, backend/accounts/account-linking, backend/accounts/accounts-and-auth-descriptors, backend/accounts/fixed, backend/accounts/open, backend/accounts/overview, backend/accounts/subscription, backend/assets/, backend/assets/asset-amounts, backend/assets/locking-assets, backend/assets/register-assets, backend/authentication/, backend/authentication/auth, backend/authentication/auth-descriptors-and-rules, backend/authentication/multi-sig, backend/cross-chain/, backend/cross-chain/automate-cross-chain-asset-registration, backend/cross-chain/cross-chain-assets, backend/cross-chain/cross-chain-transfers, backend/cross-chain/introduction, client/, client/client-account-registration, client/client-auth-descriptors, client/client-key-store, client/client-login, client/client-orchestrator, client/client-setup, client/client-transfer-assets, plus get-started/about/protocols/ft4 and reference/ft4/.
