# Asset functions, operations &amp; queries

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Asset functions, operations & queriesOn this page
# Asset functions, operations & queries

In this lesson, we will implement the core operations for the pool account and manage FT4 assets, including minting,
burning, transferring, and locking assets.

## Functions​

Let’s define a few functions we’ll use to identify the asset and the pool account. These will be used throughout the module.

Add the following to src/main/functions.rell:

src/main/functions.rell
```rell
import lib.ft4.accounts;import lib.ft4.auth;function create_pool_account() =    accounts.create_account_without_auth(get_pool_account_id(), account_type);function get_pool_account_id() =    (account_type + chain_context.blockchain_rid).hash();function get_pool_account() =    accounts.account_by_id(get_pool_account_id());function get_asset_id() =    (asset_name, chain_context.blockchain_rid).hash();@extend(auth.auth_handler)function () = auth.add_auth_handler(flags = ["T"]);
```

We require the "T" flag so only users who can transfer assets can use operations like the faucet — ensuring they can use the assets they receive.

## Operations​

Now we can define the operation to initialize the dapp by creating the pool account.

Add the following to src/main/operations.rell:

src/main/operations.rell
```rell
import lib.ft4.core.admin;operation initialize_dapp() {    admin.require_admin();    create_pool_account();}
```

Key points:

- Creates a pool account with admin privileges

- Generates a unique account ID, derived from blockchain RID and account type

## Minting assets​

Minting creates new assets and adds them to the pool account.

Add the following import the top of the file:

src/main/operations.rell
```rell
import lib.ft4.assets.{ Unsafe }; // For minting and burning
```

Now define the minting operation:

src/main/operations.rell
```rell
operation mint() {    admin.require_admin();    Unsafe.mint(get_pool_account(), dapp_meta.asset, asset_amount_to_mint);}
```

Key points:

- Only admins can mint new assets

- Assets are minted to the pool account

- The amount is specified in the smallest unit (considering decimals)

## Burning assets​

Burning permanently removes assets from circulation.

Add the following import the top of the file:

src/main/operations.rell
```rell
import lib.ft4.core.assets.{ Asset };
```

Now define the burning operation:

src/main/operations.rell
```rell
operation burn() {    admin.require_admin();    val pool_account = get_pool_account();    Unsafe.burn(pool_account, Asset(get_asset_id()), asset_amount_to_burn);}
```

Key points:

- Only admins can burn assets

- Assets must be present in the pool account

- Burning reduces the total supply of the asset

## Faucet implementation​

Creating a faucet for testing purposes:

src/main/operations.rell
```rell
operation faucet() {    val receiver = auth.authenticate(); // extended to require the "T" flag, ensuring assets can be used    val pool_account = get_pool_account();    Unsafe.transfer(pool_account, receiver, Asset(get_asset_id()), asset_amount_to_faucet);}
```

Key points:

- Users must authenticate

- There is a fixed amount per request

- Transfers are conducted from the pool account

## Transferring assets​

Implementing asset transfers between accounts:

src/main/operations.rell
```rell
operation transfer(receiver: byte_array, amount: big_integer) {    val sender = auth.authenticate();    Unsafe.transfer(sender, accounts.Account(receiver.hash()), Asset(get_asset_id()), amount);}
```

Key points:

- The sender must be authenticated

- The amount must be positive

## Asset locking​

Implementing asset locking for temporary restrictions.

Add the following import the top of the file:

src/main/operations.rell
```rell
import lib.ft4.core.assets.locking;
```

Now define the locking operations:

src/main/operations.rell
```rell
operation lock_asset(type: text, account_id: byte_array, amount: big_integer) {    admin.require_admin();    locking.lock_asset(type, accounts.Account(account_id.hash()), Asset(get_asset_id()), amount);}operation unlock_asset(type: text, account_id: byte_array, amount: big_integer) {    admin.require_admin();    locking.unlock_asset(type, accounts.Account(account_id.hash()), Asset(get_asset_id()), amount);}
```

Key points:

- Only admins can lock or unlock assets

- Assets remain in the account but cannot be transferred

- The lock type can be used to categorize different kinds of restrictions (e.g. vesting, escrow)

## Queries​

Implementing queries to check balances and account statuses:

src/queries.rell
```rell
import lib.ft4.core.assets.{ get_asset_balance };query get_pool_account_balance() =  get_asset_balance(get_pool_account(), Asset(get_asset_id()));query get_user_account_balance(    pubkey) =  get_asset_balance(accounts.account_by_id(pubkey.hash()), Asset(get_asset_id()));query get_user_lock_account_balance(    pubkey) = locking.get_locked_asset_balance(        accounts.account_by_id(pubkey.hash()),        Asset(get_asset_id()),        ["FT4_LOCK"],        5,        null    );
```
