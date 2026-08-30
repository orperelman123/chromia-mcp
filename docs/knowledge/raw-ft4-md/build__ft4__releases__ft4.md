On this page
# FT4 changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a changelog, and this project adheres to Semantic versioning.

## [1.1.0r] - 2025-02-25​

### Changed 🪙​

- Allow multiple smaller transfers to a non-existing account for the same asset type. 
- The logic takes multiple senders for one recipient into consideration 
- `register_account` used for crosschain transfers with the strategies `fee`, `open` and `subscription` does not require a signature, when transfering to the same account 
- Updated the response of the query `get_account_by_id` to also return type along with id 
### Added ✅​

- 
Added a blacklist for operations that may not be used after an auth operation (`evm_auth` and `ft_auth`). Defined in the `chromia.yml` as `core.auth.auth_op_blacklisted_operations`, it works the same way as `evm_signatures_authorized_operations`, except the former is a list of operations that are not allowed, while the latter includes only allowed operations.

- 
Added RellDocs for everything

- 
Added `get_api_version`. While version looks like "1.0.3", and it's difficult to parse, api version is an integer that is increased by one every time the API (queries and operations) changes. It will start at 1 for version "1.1.0".

- 
Added `get_block_height`, analogous to `latest_time`. The main difference is that the block height returned is the height of the next block to be produced, if in a query. This is also analogous to the difference between `op_context.last_block_time` and `op_context.block_height`, where the height refers to the current block and the time to the last block.

- 
Added multiple queries:

- `get_assets_filtered`
- `get_balances_filtered`
- `get_transfer_history_entries_filtered`
- `get_crosschain_transfer_history_entries_filtered`
- `get_asset_origin_filtered`
- `get_applied_transfers_filtered`
- `get_canceled_transfers_filtered`
- `get_unapplied_transfers_filtered`
- `get_recalled_transfers_filtered`
- `get_pending_transfers_filtered`
- `get_reverted_transfers_filtered`
- `get_accounts_filtered`
- `get_account_auth_descriptors_filtered`
- `get_main_auth_descriptors_filtered`
- `get_auth_descriptor_signers_filtered`
- `get_rl_states_filtered`
- `get_account_creation_transfers_filtered`
- `get_account_links_filtered`
All are mounted on 'ft4' directly.

- 
Added two extendable functions:

- `before_crosschain_balance_change`
- `after_crosschain_balance_change`
The extensions are being called in the `Unsafe.update_balances_if_needed` function which is a part of crosschain transfers.

### Fixed 🔧​

- `authenticate()` and `authenticate_and_return_context()` will no longer delete the auth descriptor used to authenticate the operation, even if it expired during the authentication process. 
## [1.0.0r] - 2024-07-04​

### Added ✅​

- Introduced a filtering option in `get_pending_transfer_strategies` to sort results based on transfer expiration. 
### Fixed 🔧​

- Resolved an issue in the `get_auth_message_template` query where it failed if a GTV-encoded null was provided as `args`. 
## [0.8.0r] - 2024-05-29​

### Breaking changes 💔​

- Removed the key constraint from the `symbol` attribute of the `asset` entity. 
- Replaced `get_asset_by_symbol` with `get_assets_by_symbol`. 
- Updated `register_crosschain_asset` operation to include `asset_id`, `asset_type`, and `uniqueness_resolver` arguments. 
- Renamed `get_paginated_asset_balances_by_name` to `get_paginated_assets_by_name`. 
- Removed the `_ft_auth` function and `ft4.get_account_by_auth_descriptor` query. 
### Changed 🪙​

- Made the `icon_url` attribute of the `asset` entity mutable. 
### Added ✅​

- Added a `uniqueness_resolver` attribute to the `asset` entity. 
- Introduced `get_assets_by_symbol` query to retrieve all registered assets with the same symbol. 
- Added `get_transfer_rules` query to fetch transfer strategy rules configuration. 
### Fixed 🔧​

- Ensured that the number of required signatures in multisig auth descriptors is greater than 0. 
- Verified that new main auth descriptors include all mandatory flags when using `update_main_auth_descriptor`. 
## [0.7.0r] - 2024-04-23​

### Breaking changes 💔​

- Deadline for transfers: Added `deadline` field to `init_transfer`. Transfers applied after this timestamp will fail and can only be reverted. 
- Moved `latest_time`: The `latest_time` field has been relocated from the `accounts` module to the `utils` module. 
- Function renaming: 
- Renamed `_register_account()` to `register_account()`. 
- Renamed `add_auth_descriptor_to_account` function to `add_auth_descriptor`. 
- Renamed `_add_signer` to `add_signers`. 
- Renamed `ft4.get_auth_descriptor_nonce` to `ft4.get_auth_descriptor_counter`. 
- Version requirement: Requires Rell "0.13.10". 
- Entity changes: 
- Updated entity `ft4.crosschain.applied_transfers`. 
- Updated entity `ft4.account_creation_transfer`. 
- Added `transaction` field to the `applied_transfers` entity. 
- Operation changes: 
- Removed `ft4.register_account_evm_signatures`, now use `ft4.evm_signatures` instead. 
- Removed `ft4.delete_all_auth_descriptors_exclude`, replaced with `ft4.delete_all_auth_descriptors_except_main`. 
- Function argument changes: 
- Removed `is_strict` argument from `authenticate()` function. `auth_handlers` will now resolve to the most specific handler if no operation auth handler is found. 
- Account creation updates: 
- Removed signature verification from `create_account_with_auth`. Signatures must now be verified separately using `verify_signers`. 
- `create_account_with_auth` will now create an account with an ID based on the hash of signers if the `account_id` argument is not provided. 
- Configuration changes: 
- All module arguments for FT4 submodules in YML config now include an additional `core` component in the module path. For example, `accounts` module arguments should be defined under `lib.ft4.core.accounts` instead of `lib.ft4.accounts`. 
- Module removal: Removed `ft4_basic` and `ft4_basic_dev` modules. 
### Changed 🪙​

- Revised internal structure of FT4 submodules: External modules are now imported alongside core modules. For instance, importing `lib.ft4.accounts` will also import the associated operations and queries. If needed, you can still import `accounts` entities and functions without the operations and queries by using `lib.ft4.core.accounts`. 
### Added ✅​

- Reversion of incomplete cross-chain transfers: Added support to revert incomplete cross-chain transfers after the deadline has passed. 
- Recall unclaimed register account transfers: Added functionality to recall unclaimed register account transfers after the timeout period has expired. 
- New operation: Introduced `ft4.delete_all_auth_descriptors_except_main`, which deletes all auth descriptors except the main one. 
- Auth flags configuration: Added `auth_flags` config to define mandatory and default authentication flags. 
- Updated `ft4.get_register_account_message`: Now includes register account operation parameters in the auth message. 
- New query: Added `ft4.get_enabled_registration_strategies` to retrieve enabled registration strategies. 
- New utility functions: Added new functions in `ft4.test.utils`. 
- Lock accounts support: Added support for "lock" accounts. 
## [0.6.0r] - 2024-03-22​

### Breaking changes 💔​

- Renamed `account` to `sender` in the `pending_transfer` entity. 
### Added ✅​

- 
New operation: `delete_auth_descriptors_for_signer` to remove all auth descriptors for a specific signer from an account (corresponding to the query `get_account_auth_descriptors_by_signer`).

- 
New attributes and queries:

- Added `type` attribute to the `asset` entity. 
- Introduced `get_assets_by_type` query. 
- Added `register_asset_with_type` admin operation. 
- 
Enhanced query responses:

- Included `is_crosschain` flag in responses from `get_transfer_history`, `get_transfer_history_from_height`, and `get_transfer_history_entry`. 
- Added `blockchain_rid` to responses from `get_transfer_details` and `get_transfer_details_by_asset`. 
- 
Pending transfers: Pending transfers are now completed when the account is registered with direct strategies.

- 
Configuration updates: Added auth descriptor configuration options (`max_rules` and `max_number_per_account`) to the `get_config` query.

- 
Default login configuration: Set a 1-day expiration for default login configuration.

- 
New queries:

- `get_last_pending_transfer_for_account`: Returns the most recent pending cross-chain transfer matching the provided parameters (sender, target chain, recipient, asset, amount). 
- `has_pending_create_account_transfer_for_strategy`: Checks if a "create on transfer" account registration is initiated for the specified strategy. 
## [0.5.0r] - 2024-02-29​

### Breaking changes 💔​

- Changed the mount name of the `lib.ft4.accounts` module from `ft` to `ft4`, affecting the following entities: 
- `account`
- `account_auth_descriptor`
- `auth_descriptor_signer`
- `rl_state`
### Changed 🪙​

- Updated the `_register_account()` function to return the created account. 
### Added ✅​

- 
New extension functions for cross-chain transfers:

- `before_init_transfer`
- `after_init_transfer`
- `before_apply_transfer`
- `after_apply_transfer`
- 
New `register_crosschain_asset` function.

- 
New account creation strategies:

- Create on transfer: Subscription-based 
- Configurable fee account 
## [0.4.0r] - 2024-02-15​

### Breaking changes 💔​

- Changed `asset_data` to `asset` in the following queries: 
- `get_transfer_history`
- `get_transfer_history_from_height`
- `get_transfer_history_entry`
### Changed 🪙​

- The `create_account_with_auth` function now requires the `auth_descriptor` to include the `A` flag. Accounts cannot be created with an `auth_descriptor` missing the `A` flag by default. 
- The `get_auth_descriptor_nonce` function now returns `null` if the auth descriptor is not found, rather than rejecting the request. 
- The `page_size` parameter in paginated queries is now optional. The default value can be configured using the `query_max_page_size` field under the `lib.ft4` section. 
- Updated the `transfer` function to support account creation during transfers. 
- Revised the calculation method for account IDs. 
### Added ✅​

- Introduced a framework for account creation. 
- New account creation strategies: 
- Create on transfer: 
- Open 
- Fee-based 
- Transfer open strategy 
- Added `get_first_allowed_auth_descriptor_by_signers` query. 
- Added support for rules in the login configuration. 
### Bugfixes 🐛​

- Cross-chain transfer operations now validate that transactions are anchored on SAC before proceeding. 
## [0.3.1r] - 2024-01-19​

### Changed 🪙​

- Enhanced validation for `register_crosschain_asset` parameters. 
- Added `op_index` to the `get_transfer_history` response for better tracking. 
## [0.3.0r] - 2024-01-17​

### Breaking changes 💔​

- Removed auth descriptor types `ES` and `EM`. 
### Changed 🪙​

- Introduced `get_all_auth_handlers` query to retrieve all authentication handlers specified by the dapp. 
- Added `get_first_allowed_auth_descriptor` query to enable blockchain selection of auth descriptors for operation authentication. 
- Enabled creation of overridable auth handlers. 
- Added `resolver` field to `AuthHandler` for custom authentication logic evaluation. 
- Updated transaction prioritization to support Postchain 3.14.17 and later. 
- Fixed `init_transfer` auth message issue to support `init_transfer` operation with EVM auth. 
## [0.2.0r] - 2023-12-22​
warning
This version is incompatible with older versions. To upgrade, you will need to perform a database migration. For more information, refer to this page. 
### Breaking changes 💔​

- Paginated queries: Queries `get_accounts_by_participant_id` and `get_account_auth_descriptors_by_participant_id` are now paginated. Update your code to handle these changes. 
- Paginated queries: Removed non-paginated queries `_get_accounts_by_auth_descriptor_id`, `_get_asset_balances`, and `_get_all_assets`. Use their paginated counterparts without the leading `_`. Replaced `get_asset_by_name` with the paginated `get_assets_by_name`. 
- Rules structure: Revised the internal structure of rules. Existing auth descriptors using rules will no longer function and will cause runtime errors. 
- Address functions removed: Deprecated `evm_address_from_pubkey` and `evm_address_from_privkey`. Use `crypto.eth_pubkey_to_address` and `crypto.eth_privkey_to_address` from the Rell standard library. 
- Function return types: `create_account_with_auth` now returns `account` instead of `byte_array`. `add_auth_descriptor_to_account` now returns `account_auth_descriptor` instead of `byte_array`. 
- Rate limit config: Updated format for rate limit configuration. 
- `brid` to `blockchain_rid`: Renamed all instances of `brid` to `blockchain_rid` to clarify the acronym. This affects: 
- `asset.issuing_brid` -> `asset.issuing_blockchain_rid`
- Queries and message templates that previously used `{brid}` now use `{blockchain_rid}`. 
- `asset_origin.origin_brid` -> `asset_origin.origin_blockchain_rid`
- Renamed terms: Updated terminology related to auth descriptors: 
- Queries: `get_account_auth_descriptors_by_participant_id` -> `get_account_auth_descriptors_by_signer`, `get_accounts_by_participant_id` -> `get_accounts_by_signer`. 
- Accounts module: Renamed `single_sig_args.pubkey` to `single_sig_args.signer`, `multi_sig_args.pubkeys` to `multi_sig_args.signers`, `get_participants` to `get_signers`, `auth_descriptor_participant` to `auth_descriptor_signer`, and functions like `get_paginated_auth_descriptors_by_participant_id` to `get_paginated_auth_descriptors_by_signer`. 
- Internals: Renamed `_add_auth_participant` to `_add_signer`, and `_add_eth_auth_participant` to `_add_eth_signer`. 
- Transfer history: Removed `transfer_args` and `entry_index` from `get_transfer_history_entry` and `get_transfer_history` queries. 
### Added ✅​

- Default values for `lib.ft4.accounts` module_args to simplify `chromia.yml` configuration. 
- Configuration parameter `max_auth_descriptor_rules` for `lib.ft4.accounts` module_args, with a default value of 8. 
- Option to customize the rate limiter for some accounts. 
- Queries `get_transfer_details`, `get_transfer_details_by_asset`, and `get_transfer_history_from_height` moved to `lib.ft4.assets.external`. 
- Support for transaction priority. 
### Changed 🪙​

- Updated signature for `evm_auth_operation_for` to accept a `rell.test.op`. 
- `before_authenticate` function is now extendable to add custom pre-authentication logic. 
- `after_authenticate` function is now extendable to execute logic post-authentication. 
- Account creation with `create_account_with_auth` can now be done without `op_context`. 
- Validation added for auth descriptors to prevent the creation of expired descriptors. 
## [0.1.7r] - 2023-10-25​

### Added ✅​

- Added the `get_admin_pubkey()` function to the admin module, enabling retrieval of the admin public key. 
## [0.1.6r] - 2023-10-20​

### Changed 🪙​

- Removed cross-chain submodule imports from `ft4_basic_dev`. 
## [0.1.5r] - 2023-10-19​

### Added ✅​

- Implemented cross-chain functions for asset transfer across chains. 
- Introduced a new admin module, `admin.crosschain`, for registering cross-chain assets. 
- Added the option to specify tests for Rell using the `--tests` or `-t` option in `scripts/relltest.sh`. 
## [0.1.4r] - 2023-09-29​

### Changed 🪙​

- Importing a module now automatically imports the corresponding external module as well. 
## [0.1.3r] - 2023-09-19​

### Added ✅​

- Added additional test utility functions. 
- Added `evm_address_from_pubkey` function. 
## [0.1.2r] - 2023-09-18​

### Changed 🪙​

- The version module is now included regardless of which modules are used from the library. 
### Added ✅​

- Added more test utility functions. 
## [0.1.1r] - 2023-09-12​

### Changed 🪙​

- Assets are now always returned with all properties from Postchain. The separate properties `asset`, `asset_id`, and `decimals` will be removed in favor of the more comprehensive `asset_data`. 
- The version information is now included in every non-external module, ensuring that every dapp using ft4 has access to version details. 
### Added ✅​

- Added a `test` module with utility functions for testing. 
## [0.1.0r] - 2023-07-12​

Initial release
- [1.1.0r] - 2025-02-25
- Changed 🪙
- Added ✅
- Fixed 🔧
- [1.0.0r] - 2024-07-04
- Added ✅
- Fixed 🔧
- [0.8.0r] - 2024-05-29
- Breaking changes 💔
- Changed 🪙
- Added ✅
- Fixed 🔧
- [0.7.0r] - 2024-04-23
- Breaking changes 💔
- Changed 🪙
- Added ✅
- [0.6.0r] - 2024-03-22
- Breaking changes 💔
- Added ✅
- [0.5.0r] - 2024-02-29
- Breaking changes 💔
- Changed 🪙
- Added ✅
- [0.4.0r] - 2024-02-15
- Breaking changes 💔
- Changed 🪙
- Added ✅
- Bugfixes 🐛
- [0.3.1r] - 2024-01-19
- Changed 🪙
- [0.3.0r] - 2024-01-17
- Breaking changes 💔
- Changed 🪙
- [0.2.0r] - 2023-12-22
- Breaking changes 💔
- Added ✅
- Changed 🪙
- [0.1.7r] - 2023-10-25
- Added ✅
- [0.1.6r] - 2023-10-20
- Changed 🪙
- [0.1.5r] - 2023-10-19
- Added ✅
- [0.1.4r] - 2023-09-29
- Changed 🪙
- [0.1.3r] - 2023-09-19
- Added ✅
- [0.1.2r] - 2023-09-18
- Changed 🪙
- Added ✅
- [0.1.1r] - 2023-09-12
- Changed 🪙
- Added ✅
- [0.1.0r] - 2023-07-12