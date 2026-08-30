On this page
# Manage cross-chain assets

In the FT4 library, cross-chain assets are assets that originate on one blockchain but can be registered, tracked, and transferred across other chains in the ecosystem.

## Cross-chain asset registration​

Cross-chain assets must be registered on a receiving chain to be used there. The `register_crosschain_asset` function handles this registration, ensuring that assets from other blockchains are properly linked to their origin and available on the new chain. This function is marked as `Unsafe` because it could potentially register numerous assets and thus impact storage requirements.

### Registration requirements and parameters​

When registering a cross-chain asset, certain criteria must be met to ensure the asset's uniqueness and proper linkage to its origin chain. Below are the key parameters required for registration:

- `asset_id`: A unique identifier for the asset. 
- `name`: The name of the asset, which should be concise and unique within the network to prevent conflicts. 
- `symbol`: A short identifier, like a ticker, to represent the asset. 
- `decimals`: Specifies the precision (number of decimal places) of the asset, within an acceptable range (0–78). 
- `issuing_blockchain_rid`: The blockchain where the asset was initially created. 
- `origin_blockchain_rid`: The origin chain providing the asset to the current chain, often the same as the issuing chain but can differ in complex scenarios. 
- `icon_url`: Optional URL for the asset's icon. 
- `type`: Specifies the asset type, typically set to `core.asset.ASSET_TYPE_FT4`. 
- `uniqueness_resolver`: An optional byte array used to avoid conflicts in cases where uniqueness cannot be guaranteed by other parameters alone. 
Note: The `register_crosschain_asset` function includes several validations, such as checking ID length, type conformity, and icon URL validity.

### Validation constraints​

This function has built-in checks to prevent registration errors or misuse:

- Origin and Issuing Chain Constraints: 
- The `origin_blockchain_rid` must not be the current chain, ensuring the asset is from an external source. 
- Similarly, the `issuing_blockchain_rid` cannot be the current chain, as an asset issued locally does not qualify as a cross-chain asset. 
- Asset ID Validation: 
- Validates asset ID length (32 bytes) and ensures the name, symbol, and uniqueness resolver fall within specified length limits. 
## Avoiding name conflicts​

To avoid conflicts in asset names and symbols across multiple chains, Chromia enforces several rules:

- Uniqueness of `name` and `symbol`: These identifiers must be unique within the blockchain network. This uniqueness prevents ambiguity when the same asset name or symbol exists across multiple chains. 
- `uniqueness_resolver`: In situations where name conflicts are still possible (for example, in multi-chain scenarios with similar assets), the `uniqueness_resolver` can be used to enforce distinctness. This byte array acts as an extra layer to ensure that even assets with the same name or symbol have a unique identifier. 
## Structure of cross-chain asset management​

Chromia's cross-chain asset management relies on two core structures: the `asset_origin` entity and blockchain accounts for tracking cross-chain balances.

### `asset_origin` entity​

The `asset_origin` entity links each cross-chain asset to its originating blockchain, enabling proper routing and tracking when assets are transferred across chains.

- `assets.asset`: The cross-chain asset being registered. 
- `origin_blockchain_rid`: The identifier for the originating blockchain, establishing the origin chain that supplies this asset to the current chain. 
### Blockchain accounts​

Blockchain accounts in Chromia serve as balance trackers for cross-chain transfers. Each downstream chain maintains a blockchain account to track incoming and outgoing balances for tokens from upstream chains.

#### Key characteristics of blockchain accounts​

- Balance Management: When tokens are transferred to or from downstream chains, the blockchain account's balance is adjusted accordingly. 
- Account Type: Each blockchain account is set to a specific account type, `ACCOUNT_TYPE_BLOCKCHAIN`, which designates it as a blockchain account rather than a user or business account. 
The function `ensure_blockchain_account` ensures that a blockchain account exists for each downstream chain, creating it as necessary and assigning it the correct type.

## Example of cross-chain asset registration​

When registering an asset from an external chain, the registration function call might look like the following:

```
register_crosschain_asset(
    asset_id = "unique-asset-id",
    name = "CrossChainToken",
    symbol = "CCT",
    decimals = 18,
    issuing_blockchain_rid = "upstream-chain-rid",
    icon_url = "https://example.com/icon.png",
    type = "ft4",
    uniqueness_resolver = "resolver-bytes",
    origin_blockchain_rid = "parent-chain-rid"
)

```

In this example:

- The function verifies the uniqueness and validity of the asset parameters. 
- Checks are made to ensure the asset comes from an external source (`origin_blockchain_rid`). 
- A new `asset_origin` entity is created, linking `CrossChainToken` to its origin chain, `parent-chain-rid`. 
- Cross-chain asset registration
- Registration requirements and parameters
- Validation constraints
- Avoiding name conflicts
- Structure of cross-chain asset management
- `asset_origin` entity
- Blockchain accounts
- Example of cross-chain asset registration