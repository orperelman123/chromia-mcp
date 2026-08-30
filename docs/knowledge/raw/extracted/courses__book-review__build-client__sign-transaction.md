# Connecting to the Chromia blockchain

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 6 - Build the client](/courses/book-review/build-client/)
- Connecting to the Chromia blockchainOn this page
# Connecting to the Chromia blockchain

In this section, we will guide you through connecting to the Chromia blockchain using a client. Together, we will build an example client, starting with the necessary imports at the top of our file, book_review.ts.

```typescript
import { encryption, createClient, newSignatureProvider, IClient, MERKLE_HASH_VERSIONS } from "postchain-client";import * as readline from "readline";
```

Next, we create a main function and add a helper function to receive input:

```typescript
async function main() {}
```

## Using the createClient method​

The createClient method returns a client instance that enables us to query the blockchain and send transactions. Let's add this method and break down how it works:

```typescript
let client: IClient;const blockchainRID = "";async function main() {  client = await createClient({    nodeUrlPool: "http://localhost:7740",    blockchainRid: blockchainRID,  });}
```

### Parameters:​

- 
nodeUrlPool: This is the URL of the node you want to connect to. In most development scenarios, Chromia nodes run locally, typically using http://localhost:7740. However, in production or testnet scenarios, you would use the address of a remote node.

- 
blockchainRid: Every Chromia blockchain has a unique Referential Identifier (RID), which is a hexadecimal string that distinguishes different blockchains.

In the code snippet above, we connect to a local Chromia node and specify the blockchain of interest using its RID.

Why is this important?

- 
Specificity: A single Chromia node might be associated with multiple blockchains, and the RID ensures that you interact with the correct one.

- 
Flexibility: By parameterizing the node URL and blockchain RID, you can easily switch between different environments (e.g., development, staging, production) or blockchains without changing your application's core logic.

How to Get the Blockchain RID: When you have a node running, you can always query the node for the Blockchain RID using the following command:

```text
curl http://localhost:7740/brid/iid_0
```

## Signing a transaction with postchain-client​

The postchain-client library simplifies transaction signing for Chromia. Let’s break down the process step by step.

### Generate a keypair​

Before signing any transaction, you need to generate a cryptographic keypair consisting of a public and private key. Add the following to your book_review.ts file. Use the private key that corresponds to the admin bookkeeper defined in chromia.yml:

```typescript
const privKey = Buffer.from("", "hex");const bookKeeperKeyPair = encryption.makeKeyPair(privKey);
```

The makeKeyPair function from the encryption module generates a keypair using the provided private key (privKey). The resulting bookKeeperKeyPair contains both the private key (used for signing) and the associated public key (used for verification).

If you're using the book_admin key id as instructed earlier, you can retrieve the private key from the .chromia folder like this:

```sh
cat ~/.chromia/book_admin
```

### Secure storage and retrieval of keys​

When working with cryptographic keys, it’s crucial to store them securely to prevent unauthorized access. To learn about best practices for securely generating, storing, and managing your keys, please refer to the [Chromia documentation on key generation](https://docs.chromia.com/cli/commands/keygen). This documentation provides guidance on file placement and other important security practices.

Warning: Security riskIn this code example, privKey is assigned directly for the sake of simplicity. It is critically important to manage private keys securely in a production environment. Never store private keys in plain text or expose them in client-side code, as this poses a significant security risk.

### Set up the signature provider​

Once you have your keypair, you need a mechanism to use it for signing transactions. Let’s create the SignatureProvider and add it to book_review.ts.

```typescript
const bookKeeperSignatureProvider = newSignatureProvider(MERKLE_HASH_VERSIONS.TWO, bookKeeperKeyPair);
```

The newSignatureProvider function creates a signature provider using the provided keypair (bookKeeperKeyPair). This signature provider is responsible for signing any transaction before it is sent to the blockchain.

### Using the signAndSendUniqueTransaction method​

Now we can add our first transaction to the main function in book_review.ts:

```typescript
console.log("Creating a new book transaction");  await client.signAndSendUniqueTransaction(    { name: "create_book", args: ["ISBN1", "Chromia 101", "John Doe"] },    bookKeeperSignatureProvider  );
```

The signAndSendUniqueTransaction method from the postchain-client library combines two primary tasks:

- 
Signing: Before a transaction is dispatched to the blockchain, it is signed using the provided signature provider. When verified by the blockchain network, this signature proves that the transaction has not been tampered with after being signed and confirms the sender's identity.

- 
Sending: Once the transaction is signed, it is dispatched to the Chromia blockchain for processing.

### Parameters explained:​

This method takes two arguments:

- 
Transaction object:

- name: This represents the operation you intend to execute on the blockchain. For example, "create_book" corresponds to a Rell function that creates a new book entity on the blockchain.

- args: This is an array that contains the arguments required for the operation.

- 
Signature provider: This component is responsible for signing the transaction using the previously discussed key pair. In the provided code snippet, the bookKeeperSignatureProvider is being utilized.

### How it fits into the overall flow:​

When you invoke signAndSendUniqueTransaction, you are effectively directing the client to:

- Create a transaction to call the create_book function on the blockchain with the specified arguments.

- Sign this transaction using the bookKeeperSignatureProvider.

- Send the signed transaction to the Chromia blockchain.

### Testing our code​

Finally, at the end of our file, we add a function call to run the main function as follows:

```typescript
main();
```

Now, we can test running our first example:

```shell
npx tscnode book_review.js
```

Next, we will explore how to add a query for fetching books and reviews.
