On this page
# Register assets in FT4

Asset registration is a crucial process within the FT4 library. It involves defining and initializing new assets (such as tokens) that will be used within the system, ensuring that each asset has a unique identity, associated metadata, and is properly tracked across different accounts.

There are two primary ways to register assets in FT4: using the built-in admin operation or by writing a custom operation. Both methods achieve the same goal but allow for different levels of customization and control over the registration process.

Asset registration ensures that each token is recognized and managed according to a set of predefined rules. It serves several purposes:

- 
Asset initialization: Registration initializes an asset on the blockchain by defining its properties (name, symbol, decimals, etc.). Without this, the blockchain has no way of identifying or interacting with the asset.

- 
Standardization: Registration ensures that all assets follow a standardized format, making it easier for wallets, dapps, and other services to recognize and interact with them. The asset’s properties (such as its decimal precision and symbol) are necessary for accurate tracking and transactions.

- 
Security and control: Registering an asset provides security by ensuring that the asset’s identity is unique and tied to the correct issuing entity. This prevents conflicts, duplicate assets, or fraud within the system.

- 
Supply tracking: Registration initializes the asset’s supply (even if the initial supply is zero) and allows the system to track future issuance or transfers of the asset.

- 
Integration with accounts: Once registered, the asset can be held in accounts, transferred between users, or used in decentralized applications (dapps). Without registration, the system cannot recognize the asset or manage account balances.

## Register assets with FT4 admin operation​

FT4 provides an admin operation to easily register assets on the Chromia blockchain. This is a streamlined process that allows administrators to define and add new assets without needing to write custom code. However, the `Admin module` should not be used in production since it's a security liability.

### Steps for asset registration:​

- 
Import the admin module: Ensure the admin module is imported into your project. If you followed the setup guide, this step should already be completed.

- 
Execute the registration command: Register an asset using the following command:

```
chr tx ft4.admin.register_asset TestAsset TST 6 https://url-to-asset-icon --secret .chromia/ft4-admin.keypair --await

```

In this example:

- `TestAsset` is the name of the asset. 
- `TST` is the asset's ticker symbol. 
- `6` specifies the number of decimal places for the asset. 
- `https://url-to-asset-icon` is a URL linking to the asset's icon. 
- `.chromia/ft4-admin.keypair` is the key pair used to sign the registration. 
- 
Verify asset registration: After registration, use this command to verify if the asset was successfully registered:

```
chr query ft4.get_all_assets page_size=10 page_cursor=null

```

- 
View the output: If the registration is successful, the asset details will be displayed:

```
[
"data": [
 [
   "blockchain_rid": x"CCEF62BF92CED034DD41D8230D30D2818FE6C3C96BD12E55B0BEC4D02B25E5A6",
   "decimals": 6,
   "icon_url": "https://url-to-asset-icon",
   "id": x"AB423B9C0A207B9E712FB245A738B1E29C49C5C4CB77F9E0CBA4E003F95C6CAF",
   "name": "TestAsset",
   "supply": 0L,
   "symbol": "TST",
   "type": "ft4"
 ]
],
"next_cursor": null
]

```

:::

note The `blockchain_rid` and `id` values will differ in your case.

:::
warning
Admin operations are enabled by importing the admin module and should not be used in production. 
## Register an asset with a custom operation​

For more complex scenarios, such as registering multiple assets at once or performing additional logic during registration, a custom operation can be written. This method gives developers more flexibility to handle asset creation and other initialization tasks in a single transaction.

Here is an example of a custom operation without importing the admin module:

### Custom asset registration code:​

- 
Add custom code: Insert the following code into the `main.rell` file. This code checks if the asset already exists before registering it.

```
var is_registered = false;

...

operation init() {
    is_registered=true;
    register_asset_if_needed("TestAsset2", "TST2", 10, "https://url-to-asset-2-icon");
    register_asset_if_needed("TestAsset3", "TST3", 18, "https://url-to-asset-3-icon");
}

function register_asset_if_needed(asset_name: name, symbol: text, decimals: integer, icon_url: text) {
   // Derive id of the asset
   val asset_id = (asset_name, chain_context.blockchain_rid).hash();

   // Check if the asset already exists
   val asset = assets.asset @ ? { .id == asset_id };
   if (not empty(asset)) return;

   assets.Unsafe.register_asset(asset_name, symbol, decimals, chain_context.blockchain_rid, icon_url);
}

```

- 
Deploy the code: Use the following command to deploy the updated code:

```
chr node update

```

- 
Activate the configuration: The output will mention the block height when the new configuration becomes active. For example:

```
Configuration added at height 50

```

- 
Execute the custom operation: Once the configuration is active, execute the `init` operation to register the assets:

```
chr tx init --secret .chromia/ft4-admin.keypair --await

```

- 
Verify the registration: Check if the assets were successfully registered:

```
chr query ft4.get_all_assets page_size=10 page_cursor=null

```

The output will include the newly registered assets:

```
[
"data": [
 [
   "blockchain_rid": x"CCEF62BF92CED034DD41D8230D30D2818FE6C3C96BD12E55B0BEC4D02B25E5A6",
   "decimals": 6,
   "icon_url": "https://url-to-asset-icon",
   "id": x"AB423B9C0A207B9E712FB245A738B1E29C49C5C4CB77F9E0CBA4E003F95C6CAF",
   "name": "TestAsset",
   "supply": 0L,
   "symbol": "TST",
   "type": "ft4"
 ],
 [
   "blockchain_rid": x"CCEF62BF92CED034DD41D8230D30D2818FE6C3C96BD12E55B0BEC4D02B25E5A6",
   "decimals": 10,
   "icon_url": "https://url-to-asset-2-icon",
   "id": x"5D99EDF63EF87ADD04966558E575712C9984F0DDDFB7039073BDD83227C4584D",
   "name": "TestAsset2",
   "supply": 0L,
   "symbol": "TST2",
   "type": "ft4"
 ],
 [
   "blockchain_rid": x"CCEF62BF92CED034DD41D8230D30D2818FE6C3C96BD12E55B0BEC4D02B25E5A6",
   "decimals": 18,
   "icon_url": "https://url-to-asset-3-icon",
   "id": x"03AEE687F19E8BE6899E3898D407C65838B85FAB1AF479E330A41EFC98404948",
   "name": "TestAsset3",
   "supply": 0L,
   "symbol": "TST3",
   "type": "ft4"
 ]
],
"next_cursor": null
]

```

After successfully registering assets, the next step is to handle [account registration](../Accounts/Account Registration/register-accounts). This process allows users to create new accounts and link them to their assets, enabling them to interact with the assets within the Chromia network. Admins can perform these registrations through special operations, and custom operations can be created for more complex use cases.
- Register assets with FT4 admin operation
- Steps for asset registration:
- Register an asset with a custom operation
- Custom asset registration code: