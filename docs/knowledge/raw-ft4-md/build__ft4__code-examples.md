On this page
# Code examples

This section showcases practical examples demonstrating how to use the client library. These scripts provide valuable insights into the library's functionality, even though the complete documentation is still under development.

Import statements are omitted for brevity, as IDEs will automatically identify the necessary ones.

While the scripts are mostly complete, they may not be ready to run. `await` can only be used in functions and modules, and you'd never run these scripts as standalone applications. Defining variables like key pairs and IDs will be necessary when implementing these examples in your projects.

## Create a connection​

This code demonstrates how to connect to the blockchain using the client library. This connection allows you to retrieve data through queries but can't perform operations that modify the blockchain state. This is similar to having no wallet access in the context of Ethereum Virtual Machines (EVMs). You can call `view` and `pure` functions (queries), but not others (operations).

```
const url = "http://localhost:7740";
const client = await createClient({
  nodeURLPool: url,
  blockchainIID: 0,
});

// This can be used for most queries
// Code completion will allow you to check what's available
const connection = createConnection(client);

/Example:
const account = await connection.getAccountById("5E2488889F72939DD4D0A034FB91893ACBF14C7EDBCEF2A9F5C621A07169EAD2");

// This account will unlock new queries for us.
// You can't call operations with it! You haven't given access to
// any keypair here. Use code completion to check what's available!

/Example:
const balances = await account.getBalances();

```

## Dealing with paginated entries​

When you retrieve a large number of entities, they are often delivered in batches called `PaginatedEntity`. This helps manage the amount of data transferred at once. To retrieve the next batch, you must query again, specifying the starting point.

```
const url = "http://localhost:7740";
const client = await createClient({
  nodeUrlPool: url,
  blockchainIid: 0,
});
const connection = createConnection(client);

// This is a paginated query:
// It will contain 100 entries and a cursor pointing to the next page
const assetsPage1 = await connection.getAllAssets();

console.log(assetsPage1.data);
// [asset1, asset2, ..., asset100]

// If I only need 3 assets, I can limit the number of entries per page
const assetsPage1Short = await connection.getAllAssets(3);

// I want the next three now:
const assetsPage2Short = await connection.getAllAssets(3, assetsPage1Short.nextCursor);

```

## Authenticating an account​

This section explains how users can access their accounts on the blockchain using a Web3 provider like MetaMask. This method lets users securely interact with their accounts through familiar interfaces without exposing private keys. When an account has been registered with the Web3 provider (through the auth server or other means), it'll have an auth descriptor that allows that EVM address to operate over it. This can then be used to create an instance of an authenticated account:

```
const url = "http://localhost:7740";
const client = await createClient({
  nodeUrlPool: url,
  blockchainIid: 0,
});

/Interact with Metamask or similar...
const { getSession } = createKeyStoreInteractor(
  client,
  await createWeb3ProviderEvmKeyStore(window.ethereum)//If the user has the MetaMask browser plugin installed, it will decorate the `window` object with the `ethereum` property
);

/... to retrieve the session...
const session = await getSession(accountId);

/... which lets us use the AuthenticatedAccount object...
await session.account.burn(assetId, amount);

/... and do low level calls with our account
await session.call(op("my_op", arg1, arg2));

```

## Automatic signatures​

In specific scenarios, you should streamline the user experience by minimizing the need for manual transaction signing. This is especially relevant for routine, low-risk transactions that don't pose significant security threats.

In this case, the wallet can authorize a session that signs them in the background. You should limit the authorization of this background session so that it only performs zero-threat operations. Suppose you defined a new flag for zero-threat transactions and called it `0`, `flags = ["0"]` would allow you to do so. This is how you get a session that asks you to connect to MetaMask and makes you approve the session for automatic signature of operations with flag `0`.

```
const url = "http://localhost:7740";
const client = await createClient({
  nodeUrlPool: url,
  blockchainIiD: 0,
});

/Interact with Metamask or similar...
const keyStoreInteractor = createKeyStoreInteractor(client, await createWeb3ProviderEvmKeyStore(window.ethereum));

// Since we do not specify an expiry time, this session will, by default, expire after one day.
const { session } = await keyStoreInteractor.login({
  accountId: id,
  flags: ["0"], /allow any `0`-flag operation without asking for a signature
});

/Now you can call any operation:

/this has "T" flag. It will require a Metamask signature
session.account.transfer(/*parameters*/);

/this has "A" flag. It will require a Metamask signature
session.account.addAuthDescriptor(/*parameters*/);

/suppose this has "0" flag. It will NOT require a Metamask signature
session.call(op("my_0_flag_operation" /*parameters*/));

/suppose this has "0" AND "T" flag. It WILL require a Metamask signature
session.call(op("my_0_and_T_flag_operation" /*parameters*/));

```

## Signatures​

This section covers advanced topics related to signatures and signature providers. It's intended for developers familiar with these concepts. You can safely skip this section for standard postchain + EVM usage.

This example demonstrates creating a simple signature provider from a generated key pair; all the mentioned functions are exported from `postchain-client` package:

```
/this is a simple key pair
/it has a pubKey and a privKey property
const kp = encryption.makeKeyPair();

/this is a signature provider
/it hides away the privKey by only exposing
/pubKey and sign()
const sp = newSignatureProvider(kp);

/you can also create it without ever touching the keypair
const sp2 = newSignatureProvider();

/in both cases, the privateKey is on the machine. If you need
/to sign with a different solution (e.g. an already generated
/keypair from a different app or a hardware wallet) you need
/to create your own signatureProvider. It's quite easy:
/*
const sp3 = {
    pubKey: Buffer.from("032846C2EDB843E37D63A128C033788C924D30C5BA51FE8E7AD81A2D748839F2B0", "hex")
    sign: async (digest: Buffer) => {
        /custom code to sign
        return signature
    }
}
*/
/the sign function is async to allow the code to wait for user interaction.

```

## Using admin functions​

Admin functions are powerful tools that grant complete control over the blockchain. These functions should never be used in production client applications, as that would leak the admin private keys, giving everyone full access to the Postchain. However, these functions can be useful for setup code, testing, and other specific applications.

```
const url = "http://localhost:7740";
const client = await createClient({
  nodeURLPool: url,
  blockchainIID: 0,
});

const adminSigProv = newSignatureProvider(
  encryption.makeKeyPair("2AC313A8384F319058C578F0E46A9871EACE285EA9144166D80FACE635713D39")
);

const accountSignatureProvider = newSignatureProvider();

const authDesc = createSingleSigAuthDescriptorRegistration(["A", "T"], accountSignatureProvider.pubKey);

await registerAccountAdmin(client, adminSigProv, authDesc);

```

## Complex transactions​

Sometimes, you need to be able to make a more complex transaction. For example, when trying to submit a transaction that needs to be signed by multiple users, they may also use different signature types. This can be achieved by using the transaction builder, which can be acquired from a `session`:

```
const tb = session.transactionBuilder();

```

See previous section for information about creating a `session`.

Let's say that we've an account that is created with a multisig auth descriptor that uses two EVM signatures to sign, and we want to add a new single auth descriptor to this account that uses FT signatures to sign. This scenario is complex as we first need to sign the transaction with the EVM keys and then with the FT key. However, we still need the FT signer to be listed as the signer of the transaction. From the perspective of one of the EVM signers, such a transaction can be built and signed like this:

```
// Create a session using an authenticator that represents one of the EVM signers
const tx = await session
  // Get a transaction builder
  .transactionBuilder()
  // List the ft signers that will sign this operation. No need to list the other EVM signer,
  // as it is part of the main auth descriptor so it will happen automatically
  .add(addAuthDescriptor(adToAdd), {
    signers: [ftSigner(ftPubkey)],
  })
  // When we build the transaction, it will be signed using the key that is wrapped in `authenticator`
  .build();

```

When we've done the above, we will have a transaction that's signed by the first EVM key. The signers we listed when we added the operation will also be listed as signers on the operation, but since we don't have their keys, we cannot add their signatures. Instead, we need to take this newly built transaction and send it to them. Once they receive the transaction, they can sign it like:

```
const tx = /* Aquire the tx */
const signedTx = await session.sign(tx)

```

In the above code, the caller passes the partially signed transaction produced in the previous step. The code looks the same regardless of whether the user tries to sign with EVM or FT signing. The function will automatically determine the correct signature scheme based on the data stored in the `session` object.
info
Note that while the orders in which the signers sign the transaction don't matter, all the EVM signers need to sign the transaction before any of the FT signers do. Trying to sign with FT signers before all EVM signatures have been added will result in an error.
- Create a connection
- Dealing with paginated entries
- Authenticating an account
- Automatic signatures
- Signatures
- Using admin functions
- Complex transactions