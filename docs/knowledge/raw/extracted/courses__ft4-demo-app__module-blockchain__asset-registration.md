# Asset Registration and Minting

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Blockchain dapp](/courses/ft4-demo-app/module-blockchain/)
- Lesson 3 - Asset RegistrationOn this page
# Asset Registration and Minting

In this lesson, we'll explore asset registration and minting functionality. Our system allows each account to register one digital asset and mint an initial supply.

## Asset Registration Operation​

Here's our operation for registering and minting new assets:

src/main.rell
```rell
import lib.ft4.external.assets.{ get_asset_balances };import lib.ft4.assets.{ asset, Unsafe, balance };operation register_and_mint_asset(    asset_name: text,    symbol: text,    decimals: integer,    amount: big_integer,    icon_url: text) {    val owner_account = auth.authenticate();    require(get_asset_balances(owner_account.id, 10, null).data.empty(), "One asset allowed");    functions.register_and_mint_asset(owner_account, asset_name, symbol, decimals, amount, icon_url);}
```

The key features of this operation:

- Authenticates the account owner

- Ensures one asset per account limit

- Registers the asset and mints initial supply

The actual registration and minting logic is handled in the functions [namespace](https://docs.chromia.com/rell/language-features/modules/namespace):

src/main.rell
```rell
namespace functions {    function register_and_mint_asset(        owner_account: account,        asset_name: name,        symbol: text,        decimals: integer,        amount: big_integer,        icon_url: text    ) {        // Derive asset ID        val asset_id = (asset_name, chain_context.blockchain_rid).hash();        // Check if asset exists        val asset = asset @ ? { .id == asset_id };        if (not empty(asset)) return;        // Register and mint        val asset_created = Unsafe.register_asset(asset_name, symbol, decimals, chain_context.blockchain_rid, icon_url);        Unsafe.mint(owner_account, asset_created, amount);    }}
```

This implementation:

- Generates a unique asset ID

- Checks for existing assets

- Registers the new asset

- Mints the initial supply to the owner's account
