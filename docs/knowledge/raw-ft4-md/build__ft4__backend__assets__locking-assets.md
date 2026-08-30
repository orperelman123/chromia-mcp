On this page
# Lock FT4 assets

In the FT4 library, a "lock account" functions as a secure storage for assets that users should not be able to access temporarily. This is useful in situations like staking or auction bids, where assets are still owned by users but are restricted from active use.

## Lock account overview​

A lock account is identified by a specific account type:

```
val ACCOUNT_TYPE_LOCK = "FT4_LOCK";

```

- Purpose: Lock accounts restrict access to assets without transferring ownership. 
- Usage scenarios: Staking, auction participation, or other scenarios where users need to commit assets but are not allowed to spend them. 
An `account_link` entity is created to maintain the relationship between the main account and the lock account. This entity stores:

- account: The original account holding the asset. 
- secondary: The lock account. 
- type: The purpose of the lock (e.g., "staking" or "auction"). 
### Creating a lock account​

To create a lock account associated with a given account and lock type, use the `ensure_lock_account` function:

```
function ensure_lock_account(type: text, accounts.account): accounts.account {
  // logic to ensure a lock account exists or create one
}

```

- Parameters: 
- `type`: The lock type (e.g., "staking"). 
- `account`: The user’s main account. 
- Returns: The lock account for the specified user account and type. 
### Retrieving lock accounts​

To retrieve all lock accounts associated with a particular account, use `get_lock_accounts`:

```
function get_lock_accounts(accounts.account) {
  // logic to retrieve lock accounts
}

```

This function returns all lock accounts linked to the specified user account.

For retrieving only lock accounts with a non-zero balance, use:

```
function get_lock_accounts_with_non_zero_balances(accounts.account) {
  // logic to retrieve lock accounts with assets
}

```

## Locking and unlocking assets​

The following functions allow assets to be locked (restricted from use) or unlocked (restored for use).

### Locking assets​

To lock assets, transferring them to the lock account, use `lock_asset`:

```
function lock_asset(type: text, accounts.account, assets.asset, amount: big_integer) {
  // logic to transfer assets to lock account
}

```

- Parameters: 
- `type`: The type of lock (e.g., "staking"). 
- `account`: The main account from which assets are being locked. 
- `asset`: The asset type to lock. 
- `amount`: The quantity of the asset to lock. 
This function restricts access to the specified amount of assets by transferring them to the lock account.

### Unlocking assets​

To unlock assets, returning them to the main account, use `unlock_asset`:

```
function unlock_asset(type: text, accounts.account, assets.asset, amount: big_integer) {
  // logic to transfer assets back to main account
}

```

- Parameters: 
- `type`: The type of lock (e.g., "staking"). 
- `account`: The main account where assets will be returned. 
- `asset`: The asset type to unlock. 
- `amount`: The quantity of the asset to unlock. 
This function restores access to assets by transferring them from the lock account back to the main account.

## Viewing locked balances​

FT4 provides functions to check locked asset balances:

### Retrieve locked balances by type​

Use `get_locked_asset_balance` to get balances for a specific asset type in various lock accounts:

```
function get_locked_asset_balance(
  accounts.account,
  assets.asset,
  types: list<text>? = null,
  page_size: integer? = null,
  page_cursor: text? = null
) {
  // logic to retrieve paginated asset balances by lock type
}

```

### Retrieve aggregated locked balance of an asset​

For the total balance of a locked asset across all lock types, use `get_locked_asset_aggregated_balance`:

```
function get_locked_asset_aggregated_balance(
  accounts.account,
  assets.asset,
  types: list<text>? = null
) {
  // logic to retrieve aggregated locked balance for a specific asset
}

```

### Retrieve all locked balances of all assets​

Use `get_locked_asset_balances` to retrieve locked balances for all assets:

```
function get_locked_asset_balances(
  accounts.account,
  types: list<text>? = null,
  page_size: integer? = null,
  page_cursor: text? = null
) {
  // logic to retrieve all locked balances with pagination
}

```

### Retrieve aggregated locked balances of all assets​

To get the total locked amounts for each asset across all lock types, use:

```
function get_locked_asset_aggregated_balances(
  accounts.account,
  types: list<text>? = null,
  page_size: integer? = null,
  page_cursor: text? = null
) {
  // logic to retrieve aggregated balances for all assets with pagination
}

```

These functions allow querying of both specific and aggregated locked balances, offering flexibility in tracking and managing locked assets.
- Lock account overview
- Creating a lock account
- Retrieving lock accounts
- Locking and unlocking assets
- Locking assets
- Unlocking assets
- Viewing locked balances
- Retrieve locked balances by type
- Retrieve aggregated locked balance of an asset
- Retrieve all locked balances of all assets
- Retrieve aggregated locked balances of all assets