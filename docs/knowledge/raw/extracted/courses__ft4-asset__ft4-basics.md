# Asset basics

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - Asset basicsOn this page
# Asset basics

In this lesson, we will explore the fundamental concepts of FT4 assets and their functionality within the Chromia
ecosystem.

## What are FT4 assets?​

FT4 assets are digital tokens that can represent any item of value on the Chromia blockchain. They are created using the
FT4 library, which provides a standardized approach for creating and managing assets.

## Key components of FT4 assets​

Account management

- Pool accounts for asset management

- User accounts for holding assets

- Different account types and permissions

Asset registration

- Every asset must be registered on the blockchain

- Assets have unique identifiers

- Assets are characterized by properties such as name, symbol, and decimal places

Asset operations

- Minting: Creating new assets

- Burning: Destroying assets

- Transferring: Moving assets between accounts

- Locking: Temporarily restricting asset movement

## Account types of the dapp​

In our project, we utilize two primary types of accounts:

Pool account

- Used for initial asset distribution

- Managed by the dapp

User accounts

- Created by users

- Used for holding and transferring assets

FT4 accounts can have varying permissions based on their intended use. This project follows a simplified structure with
user and admin roles.

## Asset registration​

Now, let's implement a straightforward asset configuration and registration.

Add the following to src/main/module.rell:

src/main/module.rell
```rell
import lib.ft4.assets.{ asset, Unsafe };import lib.ft4.core.accounts.strategies.open;// Asset propertiesval account_type = "PoolAccount";val asset_name = "TestAsset";val asset_symbol = "TAT";val asset_decimals = 6;val asset_icon_url = "https://url-to-asset-icon";val asset_amount_to_mint = 1000000000L;val asset_amount_to_faucet = 100000L;val asset_amount_to_burn = 100L;object dapp_meta {    asset = Unsafe.register_asset(        asset_name,        asset_symbol,        asset_decimals,        chain_context.blockchain_rid,        asset_icon_url    );}
```

This defines the configuration of the asset and registers it during blockchain initialization.

In the next lesson, we’ll implement the operations to mint, burn, and transfer this asset.
