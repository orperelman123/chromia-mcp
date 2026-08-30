On this page
# Manage assets in FT4

FT4 offers extensive asset management capabilities, supporting multiple types of assets within the same ecosystem. Each asset is tracked within the `asset` table, which maintains all necessary metadata about the asset, such as its name, symbol, and total supply.

Asset registration is a crucial process within the FT4 framework on the Chromia blockchain. It involves defining and initializing new assets (such as tokens) that will be used within the system, ensuring that each asset has a unique identity, associated metadata, and is properly tracked across different accounts.

The process of asset registration is essential because it formalizes the creation of tokens or other digital assets on the blockchain, allowing them to be used in transactions, held in accounts, and transferred securely. It also provides critical information about the asset, such as its symbol, total supply, and decimal precision, which are necessary for users and dapps to interact with the asset properly.

There are two primary ways to register assets in FT4: using the built-in admin operation or by writing a custom operation. Both methods achieve the same goal but allow for different levels of customization and control over the registration process.
warning
Admin operations are enabled by importing the admin module and should not be used in production. 
## Asset definition​

Assets are defined using the following structure:

```
entity asset {
   key id: byte_array;
   name;
   key symbol: text;
   decimals: integer;
   issuing_blockchain_rid: byte_array;
   icon_url: text;
   type: text = ASSET_TYPE_FT4;
   mutable total_supply: big_integer;
}

```

This table tracks key asset properties such as:

- Name: The asset's name. 
- Symbol: The asset's ticker or symbol. 
- Decimals: The number of decimal places the asset supports. 
- Total supply: The total amount of the asset in circulation, which can be mutable. 
## Balances​

The `balance` table tracks how much of each asset is held by each account. This allows for efficient querying of account balances and ensures that the system keeps track of asset ownership.

```
entity balance {
  key accounts.account, asset;
  mutable amount: big_integer;
}

```

- Asset definition
- Balances