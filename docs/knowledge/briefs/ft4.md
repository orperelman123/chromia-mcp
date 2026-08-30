# FT4 ready-brief (Chromia)

**As-of:** 2026-08-26 (Asia/Jerusalem).
**Rule:** Names and shapes below are copied from official docs + GitLab `chromaway/ft4-lib` tag `v1.1.0r`. Silent = said so. Nothing invented.

**Sources:** `code-ft4.md` (GitLab ft4-lib, primary tag `v1.1.0r` commit `1dd829052f422c7a78c18aaebcf72967b7002212`), `study-ft4.md` (official docs crawl), `ft4-clients.md`, `SKILL.md` (`/home/box/agent-data/workflows/chromia-ft4/SKILL.md`). Raw copies: `/workspace/chromia-knowledge/raw-ft4-src/v1.1.0r/`.

Repo: https://gitlab.com/chromaway/ft4-lib (project 59553401). Rell path `rell/src/lib/ft4`. JS path `client/lib/ft4`. Package `@chromia/ft4`.

---

## 1. Pin vs GitLab tags

Official setup/imports pin (`study-ft4.md` §9, `SKILL.md`, `ft4-clients.md` §4):

```yaml
libs:
  ft4:
    registry: https://gitlab.com/chromaway/ft4-lib.git
    path: rell/src/lib/ft4
    tagOrBranch: v1.1.0r
    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"
    insecure: false
```

ICCF on the same setup page (not an FT4 tag): `tagOrBranch: 1.87.0`, path `src/lib/iccf`, RID `x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"`.

At `v1.1.0r`, `version.rell` (`code-ft4.md` §0):

- `get_version()` → `"1.1.0"`
- `get_api_version()` → `1`
- JS `package.json`: `@chromia/ft4` `1.1.0`

GitLab tags observed 2026-08-26 (`code-ft4.md` §13, `ft4-clients.md` header): `v1.1.0r`, `v1.1.1r`, `v1.2.0r`, `v2.1.1`, `v2.1.2-beta.0`. Official changelog (`/build/ft4/releases/ft4`) **stops at 1.1.0r (2025-02-25)** and still pins `v1.1.0r`. **No RID is printed in these files for any tag except `v1.1.0r`.** Do not invent one. Confirm the tag/RID pair you actually `chr install`.

`code-ft4.md` §13: “2.x is the TS client; Rell at v2.1.1 is still 1.2.0.” Official pages in the crawl do not document 1.2 / 2.x.

---

## 2. NEVER ship `lib.ft4.admin` / `ras_open` / `ras_transfer_open`

### `lib.ft4.admin` — exact source reason

`require_admin()` in `rell/src/lib/ft4/core/admin/module.rell` (`v1.1.0r`, raw + `code-ft4.md` §3/§11):

> Not intended for use in production. If you need admin functionality, e.g. for authorizing certain operations that the users should never be allowed to call, building a different framework for that is strongly recommended, as it will allow **multisigs and rotating the admin key**.

Enforcement is only `op_context.is_signer(chain_context.args.admin_pubkey)`. Single key. No rotation. No multisig. Throws `"ADMIN REQUIRED"`. `admin_pubkey` type is `pubkey` (source), not `byte_array` (narrative / generated pages).

Import `lib.ft4.admin` mounts (`code-ft4.md` §11): `ft4.admin.register_account`, `register_asset`, `register_asset_with_type`, `mint`, `add_rate_limit_points`. Import `lib.ft4.admin.crosschain` mounts `ft4.admin.register_crosschain_asset` (9 args including `id`, `type`, `uniqueness_resolver`). Official imports page: “Admin must not be in production.” (`study-ft4.md` §2)

### `ras_open` — exact source reason

`rell/src/lib/ft4/core/accounts/strategies/open/module.rell`:

> The open strategy will allow anybody to register an account by simply sending a transaction that uses it. **In most cases, this is not something that is desirable for production dapps, as it will expose the dapp to spam account creation.**

Mounted: `ft4.ras_open(main, disposable?)`. Official docs: “Not recommended (dev/test)”. (`study-ft4.md` §8)

### `ras_transfer_open` — exact source reason

`rell/src/lib/ft4/core/accounts/strategies/transfer/open/module.rell`:

> In most cases, this is not something that is desirable for production dapps, as it will expose the dapp to **spam account creation, since no costs are associated**.

Mounted: `ft4.ras_transfer_open(main, disposable?)`. Official docs soften this to “Safe with safeguards (same-address / extra limits)” — **source is stricter**. Prefer `ras_transfer_fee` / `ras_transfer_subscription` (official: “Recommended”).

---

## 3. `require_mandatory_flags` is main-only

Call sites in `core/accounts/module.rell` (`v1.1.0r`):

| Function | Calls `require_mandatory_flags`? |
| --- | --- |
| `create_account_with_auth` | yes (and requires `rules == GTV_NULL`) |
| `update_main_auth_descriptor` | yes (and requires `rules == GTV_NULL`) |
| `add_auth_descriptor` | **no** — cap, `validate_auth_descriptor_args`, `validate_auth_descriptor_rules` only |

Struct comment on `auth_flags_config.mandatory` (`auth_flags_config.rell`): “All auth descriptors must have these flags to exist”. **Code does not enforce that on `add_auth_descriptor`.** Login/disposable descriptors with flags `[]` are allowed. Default `mandatory` is `[A, T]`. Missing → `"MISSING MANDATORY FLAGS"`.

Docs that say “all descriptors” or “`ft4.add_auth_descriptor` fails without `A`” are describing **handler flags / narrative**, not this function. Treat mandatory as **main-descriptor policy**.

---

## 4. Import footgun

Official split (`/build/ft4/setup/imports`, `SKILL.md`, `study-ft4.md` §2):

| Import | What it does |
| --- | --- |
| `lib.ft4.accounts` / `assets` / `auth` / `crosschain` | entities **plus mounted user-facing ops/queries** |
| `lib.ft4.core.<same>` | implementation **without** exposing those externals |
| `lib.ft4.admin` / `lib.ft4.admin.crosschain` | **dev/init only** |
| `lib.ft4.test` | tests only; exposes no external functions |
| `lib.ft4.utils` | pagination utilities; exposes no external functions |
| `lib.ft4.core.prioritization` / `.default` | tx-queue priority; not a user-facing token API |

Public wrappers (`code-ft4.md` §1) re-export `core.*` **and** `external.*`. Generated site has **no** pages for `lib.ft4.accounts` / `.assets` / `.auth` / `.admin` / `.crosschain` (404). Docs live under `lib.ft4.core.*` and `lib.ft4.external.*`. Narrative `import lib.ft4.accounts` still pulls the externals in.

Cross-chain also needs GTX module `net.postchain.d1.iccf.IccfGTXModule` + `libs.iccf`. Import only what you intend to expose.

---

## 5. Accounts, descriptors, login, assets, transfers, moduleArgs

Only names that appear in the files.

### Accounts

- Entity: `account { key id: byte_array; index type: text }`
- Types named: `ACCOUNT_TYPE_USER` = `"FT4_USER"`; `ACCOUNT_TYPE_LOCK` = `"FT4_LOCK"`; `ACCOUNT_TYPE_BLOCKCHAIN` = `"FT4_BLOCKCHAIN"`; `ACCOUNT_TYPE_POOL` = `"FT4_POOL"`; `ACCOUNT_TYPE_FEE` = `"FT4_FEE"`
- Create: `create_account_with_auth(auth_descriptor, account_id? = null)` (derives id via `get_account_id_from_signers` if omitted; sets main; creates `rl_state`); `create_account_without_auth(account_id, type)` / `ensure_account_without_auth` — no descriptors, no external signers (pools, treasuries, fees, locks)
- ID: FT = `hash(pubkey)`; EVM = `hash(evm_address)` without `0x` (official formula `hash(hash(pubkey).sub(0, 40))`). Source multi-signer: one signer → that hash; many → `hash(sorted signers)`. Official pages **do not reprint** the multi-signer formula.
- Same key as FT vs EVM → two different account IDs.
- Linking: `entity account_link { key account, secondary; type }`
- Protocol: accounts must exist before they can receive transfers — **except** the transfer-registration strategies.

External accounts ops (`lib.ft4.external.accounts`): `add_auth_descriptor`, `delete_auth_descriptor`, `delete_all_auth_descriptors_except_main`, `delete_auth_descriptors_for_signer`, `update_main_auth_descriptor`.

Queries named: `get_account_by_id` (returns type since 1.1.0r), `get_account_auth_descriptors`, `get_account_auth_descriptors_by_signer`, `get_account_main_auth_descriptor`, `get_account_auth_descriptor_by_id`, `get_accounts_by_signer`, `get_accounts_by_type`, `get_accounts_by_auth_descriptor_id`, `get_auth_descriptor_counter`, `get_config`, `is_auth_descriptor_valid`, plus 1.1.0r filtered: `get_accounts_filtered`, `get_account_auth_descriptors_filtered`, `get_main_auth_descriptors_filtered`, `get_auth_descriptor_signers_filtered`, `get_rl_states_filtered`.

`delete_main_auth_descriptor` **exists as a function** (leaves account unusable). **No mounted op** (`code-ft4.md` §13 #19).

### Auth descriptors

- `struct auth_descriptor { auth_type; args: list<gtv>; rules: gtv }`
- `entity account_auth_descriptor` — `id`, `auth_type` (`S`/`M`), `args`, `rules`, mutable `ctr`, `created`
- `entity main_auth_descriptor` — exactly one per user account; substitute, do not delete via public update
- `entity auth_descriptor_signer` — pubkey or EVM address without `0x`
- Flags: `auth_flags.ACCOUNT` = `"A"` (super: can add descriptors with any flag); `auth_flags.TRANSFER` = `"T"` (transfer/burn). Custom: `/[a-z_A-Z]+/` (`require_valid_auth_flags`)
- Helpers: `single_sig_auth_descriptor(signer, flags)`, `multi_sig_auth_descriptor(signers, signatures_required, flags)`
- Main rules must be `GTV_NULL` / `GTV_NULL_BYTES` (`"RESTRICTED MAIN AUTH"`)
- Limits: `auth_descriptor.max_number_per_account` default 10, clamp `AUTH_DESCRIPTORS_PER_ACCOUNT_UPPER_BOUND` = `min(200, config)` (`"TOO MANY AUTH DESCRIPTORS"`); `auth_descriptor.max_rules` default 8
- Signer sizes: `EVM_ADDRESS_SIZE` = 20, `FT_PUBKEY_SIZE` = 33
- GTV template (docs): `[0|1, [["A","T"], (signatures_required if M), signer|signers], null]`
- Rules vars: `operation_count` (only `<`/`<=`), `block_time`, `block_height`, `relative_block_height`, `relative_block_time`. Ops: `>`, `>=`, `=`, `<`, `<=`. Complex = list starting with `"and"`. Statuses: Active, Inactive, Expired (cleaned on next account op)

### Auth ops / handlers

- `authenticate()` / `authenticate_and_return_context()` — previous op must be `ft4.ft_auth` or `ft4.evm_auth` (`is_auth_op`). `ft4.evm_signatures` is **not** an auth op.
- Then: fetch account+descriptor; resolver; `before_authenticate`; `rate_limit` (spend 1 point); reject expired; check handler flags; FT/EVM sig; increment `ctr`; delete expired except used; `after_authenticate`.
- Mounted: `ft_auth(account_id, auth_descriptor_id)`, `evm_auth(...)`, `evm_signatures(...)`
- Constants: `FT_AUTH_OP` = `"ft4.ft_auth"`, `EVM_AUTH_OP` = `"ft4.evm_auth"`, placeholders `{blockchain_rid}` `{account_id}` `{auth_descriptor_id}` `{nonce}`, `APP_SCOPE` = `"app"`, `OVERRIDE_PREFIX` = `"__override__"`, `DEFAULT_LOGIN_CONFIG_NAME` = `"default"` (source; narrative does not print the string)
- Register handler: `@extend(auth.auth_handler)` + `add_auth_handler` / `add_overridable_auth_handler`. Scopes: operation (`rell.meta(foo).mount_name`), mount (`"mid.inner"`), app (omit). Two app-scope handlers → runtime error. Missing → `Cannot find auth handler for operation <name>`.
- Handler flags on shipped ops (`code-ft4.md` §10): `transfer` / `burn` / `recall_unclaimed_transfer` / `init_transfer` / `renew_subscription` require `T`. `add_auth_descriptor` / `update_main_auth_descriptor` / `delete_all_auth_descriptors_except_main` require `A`. `delete_auth_descriptor` / `delete_auth_descriptors_for_signer` use **empty flags + resolver** (self or `A`). Cross-chain apply/complete/cancel/unapply/revert/recall do **not** call `authenticate`.
- Built-in `evm_signatures` whitelist: `ft4.register_account`, `ft4.add_auth_descriptor`, `ft4.update_main_auth_descriptor`. Blacklist: `nop`, `timeb`, `iccf_proof`, `ft4.ft_auth`, `ft4.evm_auth`, `ft4.evm_signatures`.
- Queries: `get_all_auth_handlers`, `get_auth_flags`, `get_auth_handler_for_operation`, `get_auth_message_template`, `get_login_config`, `get_first_allowed_auth_descriptor`, `get_first_allowed_auth_descriptor_by_signers`

### Login sessions

- Backend: extend `login_config()` via `add_login_config`. `_login_config { flags; rules? }`. Helpers: `ttl(millis)`, `login_simple_rule` / `login_rules`, `block_height` / `block_time` / `op_count` / `relative_*`.
- Missing named config → flags `[]` and `ttl(1 day)` (`MILLISECONDS_PER_DAY` = 86400000).
- Client (`/build/ft4/client/client-login`): `login({ accountId, config?: { flags, rules }, loginKeyStore? })` → `{ session, logout }`. Defaults if `config` omitted: flags `[]`, timeout one day.
- Keystores named: `createSessionStorageLoginKeyStore`, `createLocalStorageLoginKeyStore`, `createInMemoryLoginKeyStore`. In-memory default — each reload adds another descriptor.
- Official: never put flags on a disposable key that could compromise assets; **do not put `A` on login keys**. Always `logout` (leak + descriptor cap).
- Second official shape on code-examples: `keyStoreInteractor.login({ accountId, flags: ["0"] })` (flags at top level). JS `LoginOptions` in source is `config` **or** `configName`, not top-level flags (`code-ft4.md` §13 #16).
- `hasActiveLogin` reuse-almost-expired caveat: stated in `ft4-clients.md`; **not confirmed** on the `study-ft4.md` crawl.

### Assets / transfers

- `entity asset` — `id` (32 bytes), `name`, `symbol` (not a lone key since 0.8.0r), `decimals` `[0,78]`, `issuing_blockchain_rid`, `icon_url` (mutable), `type` default `ASSET_TYPE_FT4` = `"ft4"`, `uniqueness_resolver`, mutable `total_supply`
- `entity balance { key account, asset; mutable amount }`
- Amounts: `(0, 2^256)` exclusive; `max_asset_amount` = 32 `ff` bytes
- Production register: `assets.Unsafe.register_asset(...)`; id = `(name, blockchain_rid).hash()`. Dev: `ft4.admin.register_asset`.
- On-chain ops (`lib.ft4.external.assets`): `transfer(recipient_id, asset_id, amount)` requires `T`; `burn(asset_id, amount)` requires `T`; `recall_unclaimed_transfer(transfer_tx_rid, transfer_op_index)`
- Missing recipient: `"INVALID RECIPIENT"` unless `is_create_on_internal_transfer_enabled` (default `false`)
- Mint/burn non-issuing: `"UNAUTHORIZED MINTING"` / burn string is **`Assets can only be burned on issuing chain`** — **not** `"UNAUTHORIZED BURNING"` (source vs docs)
- Locking: `ensure_lock_account`, `lock_asset`, `unlock_asset`, `get_lock_accounts`, `get_locked_asset_*`
- Hooks named: `before_transfer` / `after_transfer`, `before_mint` / `after_mint`, `before_burn` / `after_burn`

Queries named: `get_all_assets`, `get_asset_by_id`, `get_asset_balance`, `get_asset_balances`, `get_assets_by_name`, `get_assets_by_symbol`, `get_assets_by_type`, `get_assets_filtered`, `get_balances_filtered`, `get_transfer_history`, `get_transfer_history_entry`, `get_transfer_history_from_height`, `get_transfer_history_entries_filtered`, `get_transfer_details`, `get_transfer_details_by_asset`, `get_crosschain_transfer_history_entries_filtered`, `get_asset_details_for_crosschain_registration`.

### Cross-chain (ICCF, not EIF)

Happy: `ft4.crosschain.init_transfer` (source args: `recipient_id, asset_id, amount, hops, deadline`; requires `T`; `MAX_PATH_LENGTH` = 100) → `apply_transfer` (needs `iccf_proof`; no flags) → `complete_transfer` on source.

Failure: `cancel_transfer`, `unapply_transfer`, `revert_transfer`, `recall_unclaimed_transfer` (target; only if create-on-crosschain enabled).

Origin tree: wrong/malicious `origin_blockchain_rid` can lose funds. Origin is **not** validated on the origin chain. No `eif_*` ops in this library.

TS: `session.account.crosschainTransfer(targetChainId, recipientId, assetId, amount)`; also `initTransfer`, `applyTransfer`, `completeTransfer`, `cancelTransfer`, `unapplyTransfer`, `revertTransfer`, `recallUnclaimedTransfer`. JSDoc on `applyTransfer` incorrectly says `complete_transfer` (`code-ft4.md` §13 #20).

### Registration strategies (enable by import + moduleArgs)

| Strategy | Module | Op | Official note |
| --- | --- | --- | --- |
| Open | `lib.ft4.core.accounts.strategies.open` | `ras_open` | not recommended |
| Transfer open | `...strategies.transfer.open` | `ras_transfer_open` | docs: safeguards; source: spam |
| Transfer fee | `...strategies.transfer.fee` | `ras_transfer_fee` | recommended |
| Transfer subscription | `...strategies.transfer.subscription` | `ras_transfer_subscription` + `renew_subscription` | recommended |

Claim via `ft4.register_account()` (must be next op: `require_register_account_next_operation`). Client also needs `import lib.ft4.external.accounts.strategies`. Custom: own op → `create_account_with_auth` after own anti-spam.

Transfer-rule specials (source constants): `CURRENT_CHAIN_REF` = `"$"`; `ANY_REF` = `"*"`; `CURRENT_ACCOUNT_REF` = `"X"`. Narrative names them, does not always print the strings.

### moduleArgs (compiler binds the Rell struct)

`lib.ft4`: `query_max_page_size: integer = 100`

`lib.ft4.core.accounts`:

```rell
struct rate_limit_config {
    active: boolean = true;          // generated page writes = 1
    max_points: integer = 10;
    recovery_time: integer = 5000;
    points_at_account_creation: integer = 1;
}
struct auth_descriptor_config {
    max_rules: integer = 8;
    max_number_per_account: integer = 10;  // clamped to 200
}
struct auth_flags_config {
    mandatory: gtv;   // default [A,T]
    default: gtv? = null;  // copies mandatory
}
```

`lib.ft4.core.auth`: `evm_signatures_authorized_operations: gtv? = null`; `auth_op_blacklisted_operations: gtv? = null`

`lib.ft4.core.admin`: `admin_pubkey: pubkey` (source)

`lib.ft4.core.accounts.strategies.transfer`: `rules: gtv` (`sender_blockchain`, `sender`, `recipient`, `asset`, `timeout_days`, `strategy`)

`.transfer.fee`: `asset` + `fee_account`. `.transfer.subscription`: `asset` + `subscription_period_days` + `free_operations` + `subscription_account`.

Rate limit: accumulate up to `max_points`, spend 1 per authenticated op, recover 1 every `recovery_time` ms. Per-account: `@extend(get_rate_limit_config_for_account)` / source name `account_rate_limit_config`. `points_at_account_creation: 0` → new account cannot act until first recovery.

`chr query ft4.get_config` sample on setup returns **only** `rate_limit`. Source query also returns `auth_descriptor` (not `auth_flags`).

---

## 6. `@chromia/ft4` vs `postchain-client`

`postchain-client` is the raw Postchain REST client. `@chromia/ft4` wraps an `IClient` and adds accounts / session / descriptors / assets.

**postchain-client** (named settings): `createClient({ nodeUrlPool, directoryNodeUrlPool, blockchainRid, blockchainIid, statusPollInterval, statusPollCount, failOverConfig, useStickyNode })`. Query unsigned. Tx: `newSignatureProvider`, `signAndSendUniqueTransaction` (injects `nop`), `signTransaction`, `sendTransaction`, `addNop`. Statuses: Waiting, Rejected, Confirmed, Unknown. `createIccfProofTx` for ICCF.

**@chromia/ft4** typical official flow: `createClient` → `createConnection(client)` (read-only) → `createKeyStoreInteractor(client, keyStore)` → `getAccounts` / `login` / `getSession`. `login` returns `{ session, logout }`. `session.call(op(...))` inserts `ft_auth` / `evm_auth` + ops + `nop`.

Factories **named** on `/build/ft4/client/*` + code-examples (`study-ft4.md` §10): `createConnection`, `createClientToBlockchain`, `createKeyStoreInteractor`, `createInMemoryFtKeyStore`, `createInMemoryEvmKeyStore`, `createWeb3ProviderEvmKeyStore`, `createGenericEvmKeyStore`, `createSessionStorageLoginKeyStore`, `createLocalStorageLoginKeyStore`, `createSingleSigAuthDescriptorRegistration`, `createMultiSigAuthDescriptorRegistration`, `registerAccount`, `registrationStrategy.open` / `transferFee`, `registerAccountAdmin`, `registerCrosschainAsset`, `getAssetDetailsForCrosschainRegistration`, `createAmount`, `ftSigner`, `addAuthDescriptor`.

Session: `session.account` (`transfer`, `burn`, `addAuthDescriptor`, `crosschainTransfer`, `getBalanceByAssetId`, `getBalances`), `session.call`, `session.sign`, `session.transactionBuilder()`. EVM signers must sign before FT signers. Default page 100.

`ft4-clients.md` also lists from `/pages/ft4-ts-client/` and `/build/clients/ft4-client` (outside the `study-ft4.md` trees): `createConnectionToBlockchainRid`, `createSession`, `createAuthDataService`, `createFtKeyHandler`, `createEvmKeyHandler`, `ftAuth`, `callWithoutNop`, `signAndSend`, `buildAndSendWithAnchoring`. Treat those as **that file only**, not the FT4-tree crawl.

Identify a chain by **dapp Blockchain RID** + **system-node URL pool** (`directoryNodeUrlPool`). Do not hardcode dapp-node URLs. Do not ship admin keys in the frontend. CLI `--ft-auth` inserts `ft4.ft_auth`; EVM auth is client-lib only. Python has no official FT4 client — add `ft4.ft_auth` yourself.

JS client ops (`v1.1.0r` `client/lib/ft4`): `burn`, `transfer`, `updateMainAuthDescriptor`, `recallUnclaimedTransfer`, `addAuthDescriptor`, `deleteAuthDescriptor`, `deleteAuthDescriptorsForSigner`, `deleteAllAuthDescriptorsExceptMain`; admin: `registerAccount`, `addRateLimitPoints`, `registerAsset`, `mint`, `registerCrosschainAsset`; registration: `registerAccount()` → `ft4.register_account`; crosschain: `initTransfer`, `applyTransfer`, `completeTransfer`, `cancelTransfer`, `unapplyTransfer`, `revertTransfer`, `recallUnclaimedTransfer`.

Auth flags on TS: `AuthFlag.Account = "A"`, `AuthFlag.Transfer = "T"`; `AuthType.SingleSig = "S"`, `MultiSig = "M"`.

---

## 7. Docs vs generated `/pages/ft4-rell/` vs source

1. **Tags.** Docs + changelog pin `v1.1.0r` + RID `FEEB…2A5E`. GitLab also has `v1.1.1r`, `v1.2.0r`, `v2.1.1`, `v2.1.2-beta.0`. No other RIDs in these files.
2. **Rell stamp.** Generated `/pages/ft4-rell/` stamps **Rell 0.16.1**. Cached snippets still show 0.16.0. Language-level Rell is newer (`ft4-clients.md`: 0.16.4 as of 2026-08-02). Setup examples still print `compile.rellVersion: 0.14.9` (cookbooks: 0.13.14 / 0.14.5). Those are examples, not a current requirement. Changelog last required Rell is 0.13.10 (0.7.0r).
3. **Public import pages 404.** Generated site has `lib.ft4.core.*` and `lib.ft4.external.*`. `lib.ft4.accounts` / `.assets` / `.auth` / `.admin` / `.crosschain` (non-core) **404**. `/build/ft4/` index itself is 404.
4. **`max_auth_descriptor_rules`.** Narrative `/build/ft4/configuration-values` and auth-descriptors put this as a **sibling** of `auth_descriptor`. Generated + source: only `auth_descriptor.max_rules`. Prefer the struct. Sibling key may silently no-op (default 8). Files do **not** say it is a legacy alias.
5. **`auth_flags`.** In generated `module_args` and changelog 0.7.0r. **Omitted** from configuration-values. Comment says all descriptors; code is main-only.
6. **`get_config`.** Setup sample shows only `rate_limit`. Source returns `rate_limit` + `auth_descriptor`. Not `auth_flags`.
7. **`asset.symbol`.** Narrative entity still marks `key symbol`. Changelog 0.8.0r removed the key; source uses `uniqueness_resolver`; `icon_url` is mutable.
8. **`init_transfer` args.** Source / JS: `recipient_id, asset_id, amount, hops, deadline`. Docs transfers page: `asset_id, amount, destination, hops, deadline`.
9. **`admin.register_crosschain_asset`.** Source/0.8.0r: 9 args including `id`, `type`, `uniqueness_resolver`. Orchestrator CLI example is the pre-0.8.0r shape (`TestAsset TST 6 ASSET_BRID icon-url PARENT_BRID`).
10. **`admin_pubkey`.** Source type `pubkey`. Narrative/generated: `byte_array`.
11. **Burn string.** Docs: `UNAUTHORIZED BURNING`. Source: `Assets can only be burned on issuing chain`. Mint uses `UNAUTHORIZED MINTING`.
12. **`delete_auth_descriptor` flags.** Docs short form: fail without `A`. Source: empty handler flags + resolver (self or `A`).
13. **Open op names.** Source mounts `ras_open`, `ras_transfer_open`, `ras_transfer_fee`. Docs crawl printed only `ras_transfer_subscription` on a narrative page.
14. **Fee `id` pairing.** Docs: `id` cannot be accompanied by name / issuing RID. Source: if `id` is present, name and issuing RID are **ignored**.
15. **Fee import path.** One overview example: `import lib.ft4.accounts.strategies.transfer.fee` (no `core`). Enable-list uses `lib.ft4.core.accounts.strategies.transfer.fee`. After 0.7.0r, moduleArgs live under `core`; public import without `core` is how you expose ops.
16. **ICCF / FT4 pins.** Setup: ICCF `1.87.0` + RID `9C35…575D` + path `src/lib/iccf`. Strategy cookbooks still show ICCF `1.32.2` + different RID + path `src/iccf`, and one transfer-open example still cites Bitbucket `chromawallet/ft3-lib` `v0.8.0r` RID `x"B6AE…FEF6"`.
17. **`lib.ft4.core.auth` YAML.** Blacklist field documented; the YAML example under it is titled `evm_signatures_authorized_operations`.
18. **CLI flag.** Same transfer page: command uses `--ft-auth`; a note says CLI only supports `--ft4-auth`. Multisig pages use `--ft-auth`.
19. **`registerCrosschainAsset` TS.** Orchestrator: `(client, adminSig, asset, parentBrid)`. Automate: `(client, adminSig, assetId, originBrid)`.
20. **Login API shape.** `client-login`: `config: { flags, rules }`. Code-examples: top-level `flags`. JS types: `config` or `configName`.
21. **`createClient` casing.** Setup: `nodeUrlPool` / `blockchainIid`. Code-examples: `nodeURLPool` / `blockchainIID` / `blockchainIiD`.
22. **Rate-limit wording.** Account-management: accumulate / spend 1. Registration-overview table: “points consumed when creating” / “max points before rate limiting is triggered”. Source = first reading.
23. **Protocol vs transfer-registration.** Protocol: cannot receive transfers until account exists. Transfer strategies require a transfer **to** a non-existing ID. The exception is the transfer-registration framework.
24. **JS `applyTransfer` JSDoc** says `complete_transfer`. Op name is `apply_transfer`.

---

## 8. Production rules that bite

1. **Never import `lib.ft4.admin` in production.** Single `admin_pubkey`. No rotation, no multisig. Source: build a different framework.
2. **Never ship `ras_open` / `ras_transfer_open` without accepting spam.** Source: “not desirable for production… spam account creation.” Prefer `ras_transfer_fee` / `ras_transfer_subscription`. If transfer-open, constrain with `X` / `$` and a real `min_amount`. Fee amount must be **lower** than `min_amount` or users send too little and wait to recall.
3. **`A` is superuser.** Holder can add a `T` (or any custom) descriptor and empty the account. Never put `A` on a login/disposable key.
4. **`T` is money.** Default login flags are `[]`. Transfers still need a `T`-bearing descriptor. Do not give session keys `T` unless you accept hot-wallet risk.
5. **Mandatory flags are main-only.** Raising `auth_flags.mandatory` only constrains `create_account_with_auth` / `update_main_auth_descriptor`. Login descriptors are not checked.
6. **Main descriptor is permanent access.** Rules must be `GTV_NULL`. Losing those keys without a recovery descriptor loses the account. Use `update_main_auth_descriptor`. `delete_main_auth_descriptor` (function, no public op) leaves the account unusable.
7. **Descriptor cap.** Default 10, hard 200. Login without a durable `loginKeyStore` and without `logout` adds a descriptor every time. Mitigate: session/localStorage store, always `logout`, `delete_all_auth_descriptors_except_main` (needs `A`).
8. **Rate limits.** Default on, 10 points, 5000 ms/point, 1 at creation. Each authenticated op spends 1. `points_at_account_creation: 0` freezes the new account until one recovery interval.
9. **Auth handlers required.** Empty `flags: []` = any descriptor of that account — too open for money/admin. Two app-scope handlers crash at auth time. Authorized op must be **immediately next** after `ft_auth` / `evm_auth`. Official clients insert this; raw Python/Kotlin/JS Postchain must not invent pairing. EVM nonce must track `ctr`.
10. **Import surface.** `import lib.ft4.assets` mounts `ft4.transfer` for every user with `T`. Use `lib.ft4.core.assets` if you want balances without the public transfer op. Same for accounts/auth/crosschain.
11. **Cross-chain origin tree.** Wrong origin BRID can black-hole tokens. ICCF GTX module must be on every hop. Recovery is only cancel / unapply / revert / recall. Not EIF. Not the EVM bridge. EVM inside FT4 is authentication (`evm_auth`).
12. **FT vs EVM account IDs differ.** Registering the same key both ways creates two accounts.
13. **YAML key `max_auth_descriptor_rules`.** Narrative sibling is not the struct field. Compiler binds `auth_descriptor.max_rules`. Verify with `get_auth_descriptor_config()` / `rell.get_module_args`.
14. **Example pins are stale.** Copy RID+tag from the release you `chr install`, not from a cookbook (Bitbucket `ft3-lib` `v0.8.0r` is still live on one page). Never `insecure: true` in production (`SKILL.md`).
15. **Do not ship admin keys in the frontend.** `registerAccountAdmin` / `ft4.admin.*` are init/dev.
16. **Pagination.** Every FT4 query is paginated; `query_max_page_size` default 100.
17. **No invented APIs.** Files are silent on: RIDs for tags other than `v1.1.0r`; whether `max_auth_descriptor_rules` is accepted as an alias; a single “current” FT4 tag after 1.1.0r; Python FT4 session/login; any FT4 `eif_*` transfer op; full field lists for every generated function page not fetched.

---

## Canonical URLs (official)

- https://docs.chromia.com/build/ft4/intro
- https://docs.chromia.com/build/ft4/setup/imports
- https://docs.chromia.com/build/ft4/setup/ft4-setup
- https://docs.chromia.com/build/ft4/configuration-values
- https://docs.chromia.com/build/ft4/releases/ft4
- https://docs.chromia.com/pages/ft4-rell/
- https://docs.chromia.com/reference/ft4/
- https://gitlab.com/chromaway/ft4-lib
