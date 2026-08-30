On this page
# Register an account

This section demonstrates how to register a new account on the blockchain using either Chromia-native keys or MetaMask (EVM-compatible keys). By now, you should have a client connection established and be familiar with auth descriptors.
note
The main Rell module should include the following import to enable account registration:
```
import lib.ft4.external.accounts.strategies;

```

The examples use the open registration strategy, which requires:
```
import lib.ft4.core.accounts.strategies.open;

```

For reference, consult the FT4 Rell documentation for other strategies and additional use cases. 
---

## Chromia-native account registration​

```
import { registerAccount, registrationStrategy } from "@chromia/ft4";
import { createSingleSigAuthDescriptorRegistration } from "@chromia/ft4";
import { createInMemoryFtKeyStore } from "@chromia/ft4";

/**
 * Registers an account on the blockchain.
 *
 * @param {object} connection - A valid FT4 connection instance.
 * @param {object} keyPair - The key pair to register the account with (format: { pubKey, privKey }).
 */
async function registerChromiaNativeAccount(connection, keyPair) {
  const keyStore = createInMemoryFtKeyStore(keyPair); // Create a keystore using the provided key pair
  const authDescriptor = createSingleSigAuthDescriptorRegistration(["A", "T"], keyStore.id); // Create the auth descriptor

  const { session } = await registerAccount(
    connection.client,
    keyStore,
    registrationStrategy.open(authDescriptor)//Use the open strategy for registration
  );

  console.log(`Account registered successfully! Account ID: ${session.account.id.toString("hex")}`);
}

```

An available key pair can be passed directly to `registerChromiaNativeAccount`, or one can be generated as follows:

```
const keyPair = encryption.makeKeyPair(); // Generates a key pair with pubKey and privKey
console.log("Save your private key securely:", keyPair.privKey.toString("hex"));

```

Call `registerChromiaNativeAccount`, passing the connection and the key pair:

```
await registerChromiaNativeAccount(connection, keyPair);

```

---

## MetaMask account registration​

```
import { createWeb3ProviderEvmKeyStore } from "@chromia/ft4";
import { registerAccount, registrationStrategy } from "@chromia/ft4";
import { createSingleSigAuthDescriptorRegistration } from "@chromia/ft4";

/**
 * Registers an account on the blockchain using MetaMask.
 *
 * @param {object} connection - A valid FT4 connection instance.
 */
async function registerMetaMaskAccount(connection) {
  if (!window.ethereum) {
    console.error("MetaMask not found. Please install MetaMask.");
    return;
  }

  console.log("Connecting to MetaMask...");
  const keyStore = await createWeb3ProviderEvmKeyStore(window.ethereum); // Use MetaMask to create a keystore

  const authDescriptor = createSingleSigAuthDescriptorRegistration(
    ["A", "T"],//Permissions for the account
    keyStore.id
  );

  const { session } = await registerAccount(connection.client, keyStore, registrationStrategy.open(authDescriptor));

  console.log(`Account registered successfully! Account ID: ${session.account.id.toString("hex")}`);
}

```

To register an account with MetaMask, make sure the browser plugin is available, and call the function with a valid connection:

```
await registerMetaMaskAccount(connection);

```

In this repository, you can find additional code examples for account registration.
- Chromia-native account registration
- MetaMask account registration