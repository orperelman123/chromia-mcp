On this page
# Perform cross-chain transfers

The orchestrator is a utility object that facilitates cross-chain transfers. It can be instantiated, listen to events, and execute asset transfers between blockchains.
note
Prerequisite environment setup: Before you proceed with cross-chain transfers, ensure you have set up the necessary environment. You need to clone the FT4 library using the following commands:
```
git clone https://gitlab.com/chromaway/ft4-lib.git
cd ft4-lib

```

Run the examples and scripts from the root directory of the cloned repository. 
## Initial multichain environment setup​

Before proceeding with the examples, initialize your multichain environment. Run the `npm run multichain:demo` script from within the root directory of the cloned `ft4-lib` repository. Upon execution, you'll see an output similar to the following:

```
--- SUMMARY ---
Copy the following into your code as needed:
Multichain00 BRID: ...
Multichain02 BRID: ...
User Account ID:   ...
User Private Key:  ...
Test Asset ID: ...
-----------------
Press Ctrl+C to exit.

```

Make sure to copy the values provided, as they will be required to set up the orchestrator.
warning
Important Configuration Requirement: To ensure the successful validation of ICCF proofs during cross-chain transfers, it's crucial to include the ICCF module in your chain configuration. Typically specified in `chromia.yml`, the configuration should resemble the following:
```
blockchains:
  <my_blockchain_name>:
    module: <module_name>
    config:
      gtx:
        modules:
          - "net.postchain.d1.iccf.IccfGTXModule"

```

Omitting this module from the configuration may lead to the non-validation of ICCF proofs, impeding the proper functioning of cross-chain transfers. Check Import the cross-chain module for more information. 
## Performing the transfer​

The code snippet below performs the transfer. The values for `targetChainId`, `recipientId`, `assetId`, and `keyPair` should be copied from the `npm run multichain:demo` script output.

```
const { createClient, encryption, formatter} = require("postchain-client");
const { createAmount, createInMemoryFtKeyStore, createKeyStoreInteractor } = require('@chromia/ft4');

// Prepare the required context
const sourceChainId = /* Paste the BRID of multichain00 */;
const targetChainId = /* Paste the BRID of multichain02 */;
const accountId = /* Paste user account ID here */;
const keyPair = encryption.makeKeyPair(/* Paste user private key here */);
const assetId = /* Paste the asset ID */;
const amountToSend = createAmount(10, 0);

  // Initialize Chromia client on multichain00
  const client0 = await createClient({
    directoryNodeUrlPool: "http://localhost:7740",
    blockchainRid: sourceChainId,
  });

  // Initialize session and keystore
  const { getSession } = createKeyStoreInteractor(
    client0,
    createInMemoryFtKeyStore(keyPair)
  );

  const session0 = await getSession(accountId);

  // make transfer
  try {
    await session0.account.crosschainTransfer(
      targetChainId,
      accountId,
      assetId,
      amountToSend,
    )
      .on("signed", (tx) => console.log("Transaction signed"))
      .on("init", (receipt) => console.log("Transfer initialized"))
      .on("hop", (bcRid) => console.log(`On chain ${formatter.toString(bcRid)}`));
    console.log("Transfer completed");
  } catch(e) {
    console.log(`Transfer failed due to ${e}`);
  }

```

### Error handling​
caution
Ensure you handle errors appropriately during the cross-chain transfer. 
The orchestrator emits errors that are descendants of `OrchestratorError` from `@chromia/ft4`.

### Registering cross-chain assets​

Cross-chain assets can be manually registered using the `registerCrosschainAsset()` function.

```
// example to register an asset
const { registerCrosschainAsset, IClient, SignatureProvider, Asset, BufferId } = require('@chromia/ft4');

const childClient: IClient = session0.client;
const adminSignatureProvider: SignatureProvider = /* ... */;
const asset: Asset = /* ... */;
const parentBrid: BufferId = /* Paste parent blockchain RID */;

await registerCrosschainAsset(childClient, adminSignatureProvider, asset, parentBrid);

```

To build asset hierarchies, specify the `parentBrid` and a corresponding `childClient` during registration. The `childClient` can be connected to different blockchains like `multichain00` or `multichain01`. See the Transfer assets across chains topic for more information.

You can also register cross-chain assets using the CLI:

```
chr tx --blockchain-rid <child blockchain RID> \
    ft4.admin.register_crosschain_asset TestAsset TST 6 $TEST_ASSET_BRID https://url-to-asset-icon \
    <parent blockchain RID> \
    --await \
    --secret .chromia/ft4-admin.keypair

```
info
If you want to see an example of a cross-chain transfer between two chains, you can explore the Simple FT4 cross-chain demo.
- Initial multichain environment setup
- Performing the transfer
- Error handling
- Registering cross-chain assets