On this page
# Transfer assets with FT4 client

Transferring assets is a fundamental operation in decentralized applications built on the FT4 framework. FT4 provides a secure and efficient way to transfer assets between accounts, ensuring proper authentication and authorization. This section will guide you through the process of transferring assets using both the Chromia CLI and the Postchain client library.

## Prerequisites​

Before you can transfer assets, you need to have the following:

- 
Registered assets: You must have registered one or more assets on the blockchain. If you haven't done so already, follow the instructions in the Register assets section.

- 
Registered accounts: You need at least two registered accounts on the blockchain: a sender account and a recipient account. Refer to the Register FT4 accounts section for instructions on account registration.

- 
Minted tokens: Ensure that the sender account has a balance of the asset you wish to transfer. You can mint tokens to an account using the `ft4.admin.mint` operation.
tip
To mint tokens to the sender account:
```
chr tx ft4.admin.mint \
  SENDER_ACCOUNT_ID \
  ASSET_ID \
  AMOUNT_WITH_DECIMALS \
  --await --secret .chromia/ft4-admin.keypair

```

Replace `SENDER_ACCOUNT_ID` with the ID of the account you want to mint tokens to, `ASSET_ID` with the ID of the asset you're minting, and `AMOUNT_WITH_DECIMALS` with the amount of tokens to mint (including decimal places). 
## Transfer assets using the Chromia CLI​

The Chromia CLI provides a convenient way to perform asset transfers from the command line. Here's an example of how to transfer tokens using the CLI:

- 
Verify the account balance:

```
chr query ft4.get_asset_balance \
  'account_id=SENDER_ACCOUNT_ID' \
  'asset_id=ASSET_ID'

```

- 
Transfer tokens to the recipient account:

```
chr tx ft4.transfer \
  RECIPIENT_ACCOUNT_ID \
  ASSET_ID \
  AMOUNT_TO_TRANSFER \
  --ft-auth --await --secret .chromia/SENDER_ACCOUNT_KEYPAIR

```

Replace `RECIPIENT_ACCOUNT_ID` with the ID of the account you want to transfer tokens to, `ASSET_ID` with the ID of the asset you're transferring, and `AMOUNT_TO_TRANSFER` with the amount of tokens to transfer (including decimal places). Additionally, replace `.chromia/SENDER_ACCOUNT_KEYPAIR` with the path to the sender account's keypair file.

The `--ft-auth` flag enables FT4 authentication, allowing you to sign the transaction with the sender account's keypair.
note
Currently, the CLI only supports `--ft4-auth`. Support for the equivalent of `--evm-auth` might be added in the future, but for now, the only way to achieve EVM authentication is by using the client lib. 
## Transfer assets using the Postchain client library​

Alternatively, you can use the Postchain client library to perform asset transfers programmatically. Here's an example of how to transfer assets using the client library:

```
const {
  createAmount,
  createInMemoryFtKeyStore,
  createKeyStoreInteractor,
  createConnection,
  gtv,
} = require("@chromia/ft4");
const { createClient, encryption } = require("postchain-client");

// Connect to the blockchain node
const url = "http://localhost:7740";
const client = await createClient({
  nodeUrlPool: url,
  blockchainIid: 0,
});

// Define the sender's keypair (replace with your own keypair)
const senderKeyPair = encryption.makeKeyPair("YOUR_SENDER_PRIVATE_KEY");

// Get the sender's account ID
const senderId = "SENDER_ACCOUNT_ID";

// Get the recipient's account ID
const recipientId = "RECIPIENT_ACCOUNT_ID";

// Get the asset ID
const assetId = "ASSET_ID";

// Define the amount to transfer (including decimal places)
const amountToSend = createAmount(10, 6); // Transfers 10 tokens with 6 decimal places

// Create a session for the sender account
const { getSession } = createKeyStoreInteractor(client, createInMemoryFtKeyStore(senderKeyPair));
const session = await getSession(senderId);

// Transfer the assets
await session.account.transfer(recipientId, assetId, amountToSend);

// Check the sender's balance after the transfer
console.log(await session.account.getBalanceByAssetId(assetId));

```

Replace `YOUR_SENDER_PRIVATE_KEY`, `SENDER_ACCOUNT_ID`, `RECIPIENT_ACCOUNT_ID`, and `ASSET_ID` with the appropriate values for your use case.

This example demonstrates the following steps: register-assets

- Connect to the blockchain node using the Postchain client library. 
- Define the sender's keypair and retrieve the sender's account ID, recipient's account ID, and asset ID. 
- Create a session for the sender account using the `createKeyStoreInteractor` and `createInMemoryFtKeyStore` functions. 
- Transfer the assets using the `session.account.transfer` method, specifying the recipient account ID, asset ID, and the amount to transfer. 
- Check the sender's balance after the transfer using the `session.account.getBalanceByAssetId` method. 
By following these steps, you can securely transfer assets between accounts using either the Chromia CLI or the Postchain client library, depending on your specific requirements.
- Prerequisites
- Transfer assets using the Chromia CLI
- Transfer assets using the Postchain client library