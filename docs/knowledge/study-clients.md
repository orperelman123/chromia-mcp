# Chromia official clients, cookbook, and integrations — study notes

Source: official docs crawled 2026-08-26 from `https://docs.chromia.com`. Index paths `/build/clients/` and `/build/cookbook/` return HTTP 404; leaf pages and `/overview` exist. Nothing here is invented: if a page is a stub or only links to a demo repo, that is recorded.

---

## 1. How a client talks to a chain (common model)

Every official client is a REST/GTX wrapper over a Postchain node.

1. Identify the chain with **Blockchain RID** (32-byte hex) or, on some clients, **blockchain IID** (Directory Chain is always IID 0).
2. Point the client at either:
   - `nodeUrlPool` — known node REST URLs (local `http://localhost:7740` or same-cluster nodes), or
   - `directoryNodeUrlPool` — system-cluster nodes running the Directory Chain. The client queries Directory Chain and discovers every node currently running the target dapp.
3. **Queries** are read-only HTTP calls: query name + GTV args → GTV result.
4. **Transactions** are GTX: `[ [ blockchainRID, [ [opName, [args...]], ... ], [signerPubKeys...] ], [signatures...] ]`. Signatures are ECDSA secp256k1 over a Merkle digest of the GTX body. A `nop` (no-operation) with random data makes the tx unique so the same operation can be sent more than once.
5. Submit via REST `POST /tx` with hex-encoded serialized GTX. Status is polled until `Waiting | Rejected | Confirmed | Unknown`.
6. GTV values are converted to ASN.1 DER on the wire.

JS/TS architecture diagram (from the JS/TS reference):

```
User
  chromiaClient.sendTransaction()
    <Buffer with serialized message>
      POST http://localhost:7741/tx {tx: 'hex-encoded message'}
        RestApi → Postchain → backend.funN(conn, tx_iid, ...)
```

Local Hello World query via raw REST (cookbook CLI):

```
curl -X GET 'localhost:7740/query/<BlockchainRID>?type=hello_world'
```

Production connection (same idea in every language): pass Directory system-node URLs + target BRID; the client resolves dapp node URLs itself.

Testnet Directory pool (from deploy/connect-client):

```
https://node0.testnet.chromia.com:7740
https://node1.testnet.chromia.com:7740
https://node2.testnet.chromia.com:7740
https://node3.testnet.chromia.com:7740
```

Mainnet Directory pool examples (from deploy/connect-client; explorer system cluster is the live list):

```
https://system.chromaway.com
https://chromia.validatrium.club
https://chromia-mainnet-systemnode-1.stakin-nodes.com
https://chroma.node.monster:7741
https://dapps0.chromaway.com
https://chromia-mainnet.w3coins.io:7740
https://mainnet-dapp1.sunube.net:7740
```

FT4 sits on top of this: `createClient` (postchain-client) → `createConnection` / `createKeyStoreInteractor` / `Session` (`@chromia/ft4`). FT4 adds accounts, auth descriptors, asset transfers, memos, and cross-chain orchestration.


## 2. Official clients

Canonical overview: https://docs.chromia.com/build/clients/overview


### Clients overview extract

Canonical: https://docs.chromia.com/build/clients/overview

The Clients section highlights tools and libraries that enable integration of applications with the Chromia
blockchain.

Available Postchain clients​

Postchain clients
Description

JavaScript(JS)/TypeScript(TS)
The JavaScript/TypeScript client library provides functionality for interacting with a blockchain using JavaScript or TypeScript. It allows you to send transactions and retrieve information from a blockchain node running Rell.

Kotlin
The Kotlin client provides the capability to interact with a blockchain from a client app written in Kotlin or Java. With this library, you can easily send transactions and retrieve queries from a Rell blockchain node.

C#
The C# client provides functionality for interacting with a blockchain using C#. It allows you to send transactions and retrieve information from a blockchain node running Rell. It can be integrated into your C# projects through the NuGet package or directly referencing the DLL files.

Rust
The Rust client is used for interacting with the Chromia blockchain deployed to a Postchain single node (manual mode) or multi-nodes managed by Directory Chain (managed mode). This library provides functionality for executing queries, creating and signing transactions, and managing blockchain operations.

Python
A Python client library for interacting with Postchain nodes on Chromia blockchain networks. This library provides an interface for creating, signing, and sending transactions, as well as querying the blockchain, with full async support.

Go
The Go client offers a range of built-in functions and utilities designed to simplify interaction with decentralized applications (dApps) created using the Postchain blockchain framework, commonly referred to as Chromia.

Other tools​

Tool
Description

Chromia React Kit
The Chromia React Kit is a library tailored for React developers, simplifying the connection of frontend applications with the Chromia blockchain. It offers hooks to simplify session management, blockchain queries, operations and interactions with the Postchain client, FT4 library, FileHub, Directory Chain and Economy Chain.

Bridge client
The Chromia bridge client is a TypeScript library with utilities for interacting with the Chromia token bridge.

FT4 client
The FT4 client is written in TypeScript. It is made for easier access to the backend operations and queries are ready for use on the frontend (or a JS backend).

Filehub client
The Filehub client is written in TypeScript for persisting and reading files on the Chromia blockchain.

Work in progress​

We are actively working to expand the capabilities of Chromia clients by developing additional libraries, tools, and
documentation. Upcoming updates aim to:

Introduce support for more frameworks and frontend tools.

Streamline user onboarding with enhanced developer resources.

Expand tutorials and examples to help you get started quickly.

Explore more​

If you're starting, dive into the Chromia React Kit documentation for detailed guides and examples. Keep
an eye on this section as we continue to expand and improve our tools for developers.

Additional resources​

FT4 documentation

Chromia developer portal

The /build/clients/ index 404s. Overview lists JS/TS, Kotlin, C#, Rust, Python, Go plus React Kit, Bridge client, FT4 client, Filehub client. MCP Server and REST API are sibling pages.

### 2.1 JavaScript / TypeScript — postchain-client

Canonical: https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/

The JavaScript/TypeScript client library provides a comprehensive set of functions and utilities for interacting with
Chromia blockchain. It allows you to send transactions, retrieve information from blockchain nodes running Rell, and
build decentralized applications with ease.

Getting started​

If you're new to the JavaScript/TypeScript client, start with our quickstart guide to connect your first client to a
Rell blockchain:

Hello World Quickstart - A step-by-step guide to send your first transaction and query

Installation​

The Postchain client is compatible with both JavaScript and TypeScript. The library is distributed on npm (Node Package
Manager) and can be easily installed in your project.

npm install postchain-client

You can also visit the npm package page for more installation options.

Key features​

Transaction Management - Create, sign, and send transactions to the blockchain

Query Execution - Retrieve data from the blockchain using queries

Multiple Node Support - Connect to multiple nodes for redundancy and failover

TypeScript Support - Full type definitions for enhanced development experience

Automatic Discovery - Discover nodes automatically via Directory Chain

Flexible Authentication - Support for various key management strategies

What you'll find here​

This section contains:

Hello World Quickstart - Get started quickly with a practical example

Client Reference - Detailed API reference and advanced features

Useful resources​

Chromia React Kit - For easier React integration

Client libraries overview - Explore clients for other languages

Cookbook examples - Practical examples and recipes

Related courses​

Learn more about building with JavaScript/TypeScript and Rell:

React/Rell course - Detailed
examples of the JavaScript client in action

#### Hello World

Canonical: https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart

This quickstart guide shows you how to send transactions to a blockchain node running
Rell and retrieve information from it using the postchain-client library. The
postchain-client allows you to connect the frontend of your dapp to the backend.

Get the Rell Backend

You can get the complete Rell backend from the
Hello World repository, or follow this guide to add the client
integration. For the complete backend setup, see
Build and run the Hello World dapp.

This tutorial assumes you have a Rell backend running. If you don't have one yet, see
Build and run the Hello World dapp with Chromia CLI to create the
backend first. You can connect the frontend at a later stage.

Prerequisites​

Node.js 18+

Your Hello World backend is running and you have the Blockchain RID

Create the client project​

Create a separate TypeScript project and initialize it.

mkdir my-rell-client && cd my-rell-client

npm init -y

npm install postchain-client --save

npm install -D typescript @types/node tsx

npx tsc --init

mkdir src

Client setup​

The code establishes the client connections, creates, signs, and posts a transaction to the blockchain node, and
retrieves information through a query. For detailed explanation of the code setup, see
JavaScript/TypeScript client reference.

Create my-rell-client/src/index.ts and paste in the code below, replacing <BlockchainRID> with your
Blockchain RID obtained from the node output. For information on finding
Blockchain RID, see
Build and run the Hello World dapp with Chromia CLI.

import { createClient } from "postchain-client";

async function simpleClient() {

  // Key pair (DEV-ONLY, replace for anything beyond localhost)

  const adminPubkey = Buffer.from("031b84c5567b126440995d3ed5aaba0565d71e1834604819ff9c17f5e9d5dd078f", "hex");

  const adminPrivkey = Buffer.from("0101010101010101010101010101010101010101010101010101010101010101", "hex");

  // Connection setup

  const nodeUrl = "http://localhost:7740/"; // Using default postchain node REST API port

  const blockchainRid = "<BlockchainRID>"; // Dapp Blockchain RID

  const client = await createClient({

    nodeUrlPool: [nodeUrl],

    blockchainRid,

  });

  // Transaction

  /*const { status, statusCode, transactionRid } = */ await client.signAndSendUniqueTransaction(

    {

      operations: [

        {

          name: "set_name",

          args: ["Developer"],

        },

      ],

      signers: [adminPubkey],

    },

    { privKey: adminPrivkey, pubKey: adminPubkey }

  );

  // Query

  const result = await client.query("hello_world");

  console.log(result); // Hello Developer!

}

simpleClient().catch((e) => {

  console.error(e);

});

Browser compatibility

This example uses Node's built-in Buffer. In a Node.js environment, no extra configuration is needed. If you run the
same code in a browser/bundler (for example Vite or webpack 5), you will hit:

ReferenceError: Buffer is not defined

To fix this, install the buffer package and add at the top of your code:

import { Buffer } from "buffer";

(globalThis as any).Buffer = Buffer;

This makes Buffer available globally, just like in Node.js.

Understanding the Rell backend​

The client code works with the operation and query specified in the Rell file main.rell. If you created your backend
using the Hello World guide, the code in the main.rell file is as
follows:

module;

object my_name {

  mutable name = "World";

}

operation set_name(name) {

  my_name.name = name;

}

query hello_world() = "Hello %s!".format(my_name.name);

The project structure for the client setup should be as follows:

my-rell-client

    |--node_modules

    |--src

    |   |--index.ts

    |--package-lock.json

    |--package.json

    |--tsconfig.json

Run client code​

From the project root:

npx tsx src/index.ts

After running the code, you should see:

Hello Developer!

Congratulations! With the help of the postchain-client library, you have successfully sent transactions and retrieved
information from a blockchain node running Rell.

Next steps​

Explore the JavaScript/TypeScript client reference for advanced features

Learn about queries and transactions in the cookbook

Check out the React Kit for easier React integration

#### JS/TS reference

Canonical: https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference

note

We are currently updating this documentation. While it offers a general overview, some details may be outdated. Please
check back soon for the latest version.

JavaScript/TypeScript client contains a set of predefined functions and utilities offering a convenient and simplified
interface for interacting with a decentralized application (dapp) built using the Postchain blockchain framework, also
known as Chromia.

tip

You can find detailed examples of the JavaScript client in the
React/Rell course.

Installation​

The Postchain client is compatible with both JavaScript and TypeScript. The library is distributed on npm (Node Package
Manager) and can be easily installed in your project. You have two options to install it:

Visit the npm package page for postchain-client and follow the installation instructions provided there, see
npm-postchain-client.

In your project's terminal or command prompt, execute the following command: npm install postchain-client. This
command will download and install the library in your project.

Initializing the client​

Firstly, import the required libraries.

import crypto from "crypto-browserify";

import secp256k1 from "secp256k1";

import { encryption, createClient, newSignatureProvider } from "postchain-client";

Then, create some dummy keys.

const signerPrivKeyA = Buffer.alloc(32, "a");

const signerPubKeyA = secp256k1.publicKeyCreate(signerPrivKeyA);

const signerPrivKeyB = Buffer.alloc(32, "b");

const signerPubKeyB = secp256k1.publicKeyCreate(signerPrivKeyB);

Each blockchain has a Blockchain RID (blockchainRID) that identifies the specific blockchain we wish to interact with.
This blockchainRID should match the Blockchain RID encoded into the first block of the blockchain. How the blockchainRID
is structured depending on the creator of the blockchain. In this example, we use the Linux command: echo
"A blockchain example"| sha256sum.

const blockchainRid = "7d565d92fd15bd1cdac2dc276cbcbc5581349d05a9e94ba919e1155ef4daf8f9";

Create a Chromia client instance and configure it according to your needs.

Parameters​

settings (Object): A set of network settings to customize the behaviour of the Chromia client.

nodeUrlPool (Optional): An array of URLs representing the nodes the client will send requests to. Use this if you
know the specific nodes handling the client requests. These nodes can be local or belong to the same cluster as the
targeted blockchain.

directoryNodeUrlPool (Optional): An array of URLs representing nodes in the system cluster, where the directory
chain is located. The client will automatically discover every node running the targeted application by querying the
Directory Chain. This can be useful when the client needs to automatically adapt to updates to the nodes within the
cluster where the blockchain is located."

blockchainRid (Optional): Resource Identifier (Rid) of the targeted blockchain. This is a unique identifier for
the specific blockchain.

blockchainIid (Optional): Instance Identifier (Iid) of the targeted blockchain. The Directory Chain always has
Iid 0.

statusPollInterval (Optional): Interval (in milliseconds) at which the client will poll the status after posting a
transaction.

statusPollCount (Optional): Number of consecutive successful status polls before it should stop asking for the
status. Defaults to 1.

failOverConfig (Optional): Configuration for failover behaviour in case of node failures.

strategy (Optional): Failover strategy to use. Defaults to a strategy called Abort On Error.

attemptsPerEndpoint (Optional): Number of consecutive failed attempts allowed for each endpoint before
considering it unreachable. Defaults to 3.

attemptInterval (Optional): Interval (in milliseconds) between consecutive retry attempts during failover.
Defaults to 5000 ms.

unreachableDuration (Optional): Duration (in milliseconds) that an endpoint should remain unreachable before
reattempting. Defaults to 30000 ms.

useStickyNode(Optional): A boolean that will ensure that on succefull requests to a node, the client will continue
using this node unless it starts failing.

Returns​

A promise that resolves to the configured Chromia client instance.

Example:

Client configured with a known node URL:

const chromiaClient = await createClient({

  nodeUrlPool: "http://localhost:7740",

  blockchainRid,

});

The client is configured for node discovery with an array of URLs representing nodes in the system cluster.

const chromiaClient = await createClient({

  directoryNodeUrlPool: ["url1", "url2", "url3", "etc."],

  blockchainRid,

});

Use sticky node​

What is a "sticky node"?

A sticky node is a node that will continue to be used for requests as long as the requests to it are successful, this
means that if the client is requesting to get the block height of a dapp, and it is successful, then following requests
to get other data e.g.: dapp transactions will be using the same node, meaning that is "sticks" with the user once
selected.

How does it work?

The client will need to be initialized with a directoryNodeUrlPool and have the property useStickyNode set to true
in the settings parameter for it to be enabled. Example:

const client = createClient({

  useStickyNode: true,

  directoryNodeUrlPool: ["http://localhost:7740"],

  ...restSettings,

});

This will then create internally create a nodeManager that handles and keeps track of which nodes are available and
which one is currently set to be the "sticky node".

As a user, when you first request with this feature enabled, you will not have any "sticky node" set. Instead, whenever
you request, the client will choose a random node out of the ones available. Should the request to the node be
successful, then that node will be set as the "sticky node". This will happen regardless of what failoverStrategy has
been set.

Any subsequent requests after the first successful one will continue to use the "sticky node" if it continues to give
successful requests. Should the node fail, however, then it will be set at unavailable for the duration configured in
the client settings (or the default time of 30000ms), and following requests will once again try to use a random
available node and, if successfully set that one as the new "sticky node".

Setting how long a node should be unavailable

The duration that a node is configured as unavailable can be se in the failoverConfig of the client settings:

const client = createClient({

useStickyNode: true,

directoryNodeUrlPool: ["http://localhost:7740"],

failOverConfig: {

startegy: "abortOnErrror",

attemptsPerEndpoint: 4,

attemptInterval: 3000,

unreachableDuration: 50000,

}

...restSettings

})

Failover strategies​

When initializing a client, you can configure the failover strategy for the client. Additionally, you can modify
specific parameters within the failover configuration, such as the number of attempts per endpoint and the interval
between attempts.

The Postchain client offers three failover strategies:

Abort on error

The request strategy will abort on client error and retry on server error. The request strategy will not retry the query
if a client error occurs, such as an invalid query parameter. However, the request strategy will retry the query on
another node if a server error occurs, such as a timeout or internal server error.

Try next on error

The Try Next On Error request strategy is similar to Abort On Error but will also retry on client error. This means that
if a client error occurs, the request strategy will retry the query on another node, as well as retry on the server
error.

Single endpoint

The single endpoint request strategy will not retry on another node.

Query majority

The query majority request strategy will query all nodes in parallel and wait until an EBFT majority of the nodes return
the same response. This can ensure the system's integrity by requiring a consensus among nodes before accepting a
result.

Queries​

Query option 1​

Use the query function to send a query to a dapp written in Rell. The function takes the query's name and an object of
query arguments.

chromiaClient.query("get_foobar", {

  foo: 1,

  bar: 2,

});

Query option 2​

Alternatively, the query function can take an object with a name property and an args property.

chromiaClient.query({

  name: "get_foobar",

  args: {

    foo: 1,

    bar: 2,

  },

});

Typed query 1​

You can specify argument and return types for a given query in TypeScript.

type ArgumentsType = {

  foo: number;

  bar: number;

};

type ReturnType = {

  foobar: string;

};

const result = await chromiaClient.query<ReturnType, ArgumentsType>("get_foobar", {

  foo: 1,

  bar: 2,

});

Typed query 2​

Alternatively, you can specify the types in a QueryObject to achieve type safety

type ReturnType = {

  foobar: string;

};

const myQuery: QueryObject<ReturnType> = {

  name: "get_fobar",

  args: { foo: "bar" },

};

const result = await chromiaClient.query(myQuery); // result has type ReturnType

Transactions​

To send transactions, begin by creating a simple signature provider. The signature provider is used to sign
transactions. More details on usage are provided further below.

const signatureProviderA = newSignatureProvider({ privKey: signerPrivKeyA });

Simple transaction​

The signAndSendUniqueTransaction function streamlines the transaction-sending process in three steps. It adds a "nop"
(no operation) with a random number that ensures the transaction is unique, signs it with a signature provider or
private key, and sends it. The function generates a receipt with a status code, status, and tansactionRid. The status
code indicates whether the server successfully processed the transaction. The status represents the current stage of the
transaction on the blockchain, which can be one of the following: Waiting, Rejected, Confirmed, or Unknown.

const { status, statusCode, transactionRid } = await chromiaClient.signAndSendUniqueTransaction(

  {

    operations: [

      {

        name: "my_operation",

        args: ["arg1", "arg2"],

      },

    ],

    signers: [signatureProviderA.pubKey],

  },

  signatureProviderA

);

It is also possible to pass a single operation.

const { status, statusCode, transactionRID } = await chromiaClient.signAndSendUniqueTransaction(

  {

    name: "my_operation",

    args: ["arg1", "arg2"],

  },

  signatureProviderA

);

Signing a transaction​

Signs a transaction using the provided signing method. This can be a SignatureProvider or a key pair. A signature
provider must contain a public key and a sign function that returns the signature of a digest transaction.

const signedTx = await chromiaClient.signTransaction(

  {

    operations: [

      {

        name: "my_operation",

        args: ["arg1"],

      },

    ],

    signers: [signatureProviderA.pubKey],

  },

  signatureProviderA

);

Sending an unsigned transaction​

const receipt = await chromiaClient.sendTransaction({

  name: "my_operation",

  args: ["arg1", "arg2"],

});

Sending a signed transaction​

chromiaClient.sendTransaction(signedTx);

Sending a signed transaction (with status polling enabled)​

chromiaClient.sendTransaction(signedTx, true);

Advanced transaction​

Create a transaction object.

const tx = {

  operations: [

    {

      name: "my_operation_1",

      args: ["arg1", "arg2"],

    },

    {

      name: "my_operation_2",

      args: ["arg1", "arg2"],

    },

  ],

  signers: ["signer1", "signer2"],

};

You can modify the object to add operations or signers.

tx.operations.push({

  name: "my_operation_3",

  args: ["arg1", "arg2"],

});

tx.signers.push("signer3");

A nop can be added to make the transaction unique. It can be added manually to the transaction object or by using the
addNop function.

const uniqueTx = chromiaClient.addNop(tx);

Sign and send the transaction.

const signedTx = await chromiaClient.signTransaction(uniqueTx, signatureProviderA);

const receipt = await chromiaClient.sendTransaction(signedTx);

PromiEvent​

When using functions that involve sending a transaction, you can either wait for a promise or act on an event. The
return value, in this case, is a "PromiEvent," which combines the functionalities of both a "Promise" and an "Event."
This combination allows you to handle asynchronous operations. You can treat it as a Promise by utilizing the .then()
and .catch() methods to handle the result of any potential errors. Moreover, it emits an event when a transaction is
sent, allowing you to listen to the event and execute custom logic based on your specific needs.

chromiaClient

  .sendTransaction({

    name: "my_operation",

    args: ["arg1", "arg2"],

  })

  .on("sent", (receipt: TransactionReceipt) => {

    console.log("The transaction is sent");

  });

External signing example​

This example demonstrates that you can use external signing mechanisms. It could involve a complex function requiring
you to sign from your phone, another device, or a different method

function askUserBToSign(rawGtxBody) {

  const digest = getDigestToSignFromRawGtxBody(rawGtxBody);

  return Buffer.from(secp256k1.ecdsaSign(digest, signerPrivKeyB).signature);

}

This complex signature process can be implemented in a SignatureProvider. Once you have a callback like the one above,
creating a signature provider is straightforward:

const signatureProviderB = {

  pubKey: signerPubKeyB,

  sign: askUserBToSign,

};

ICCF​

Creates a proof transaction for ICCF (Inter-Chain Communication Framework). This
function generates a proof that a specific transaction has occurred on the source blockchain. The function returns a
transaction object with an operation called iccf_proof, and the operation that the proof should accompany should be
added to this transaction object. The transaction can then be signed and posted to the target blockchain.

const managementBlockchainRid = "7d565d92fd15bd1cdac2dc276cbcbc5581349d05a9e94ba919e1155ef4daf8f9";

const chromiaClient = await createClient({

  nodeUrlPool: "<url-node-running-managementchain>",

  managementBlockchainRid,

});

const txToProveRid: Buffer = <txRid>;

const txToProveHash: Buffer = <txHash>;

const txToProveSigners: Pubkey[] = [<signer1>, <signer2>];

const sourceBlockchainRid: string = "<sourceBlockchainRid>";

const targetBlockchainRid: string = "<targetBlockchainRid>";

const { iccfTx, verifiedTx } = createIccfProofTx(chromiaClient, txToProveRID,txToProveHash,txToProveSigners, sourceBlockchainRid, targetBlockchainRid);

iccfTx is a transaction object with an operation called iccf_proof and an argument containing the composed proof. To
this transaction object, you can now add the operation needing proof. Finally, the transaction object is ready to be
signed and sent.

If necessary, it is possible to solely verify whether a specific transaction has been included in the anchoring
blockchain:

isBlockAnchored(sourceClient, anchoringClient, txRid);

To create an anchoring client there is an utility function:

const anchoringClient = getAnchoringClient();

Architecture​

In the Postchain client, Generic Transactions (GTX) simplify Postchain user implementations. Users do not need to invent
a binary format for their transactions. The client will serialize the function calls, sign them, and send them to
Postchain. Read GTX in the docs.

User

 |

 | chromiaClient.sendTransaction()

 |

 v

 |

 | <Buffer with serialized message>

 |

 v

 |

 | POST http://localhost:7741/tx {tx: 'hex-encoded message'}

 |

 v

RestApi

 |

 | <Buffer with serialized message>

 |

 v

Postchain

 |

 | backend.fun1(conn, tx_iid, 0, [pubKeyA], 'arg1', 'arg2');

 | backend.fun2(conn, tx_iid, 1, [pubKeyA], 'arg1');

 |

 v

Backend

Contributing to the project​

Run tests​

Unit tests:

npm run test:unit

Integration tests:

Make sure a Postgres database is running. Read more here.

Start blockchain

cd resources/testDapp

chr node start --wipe

Run tests

npm run test:integration

### 2.2 Kotlin / Java — postchain-client + chromia-client + ft4-client

https://docs.chromia.com/build/clients/postchain-clients/kotlin-client

Not on Maven Central. GitLab package registries:
- chromia-parent https://gitlab.com/api/v4/projects/50818999/packages/maven
- postchain https://gitlab.com/api/v4/projects/32294340/packages/maven
- postchain-client https://gitlab.com/api/v4/projects/46288950/packages/maven

Artifacts: net.postchain.client:postchain-client, chromia-client, ft4-client.

Postchain client: EndpointPool.singleUrl("http://127.0.0.1:7740") + PostchainClientProviderImpl().createClient(PostchainClientConfig(bcRid, endpointPool, listOf(keyPair))). Query: psClient.query("hello_world", GtvFactory.gtv(mapOf())). Tx: transactionBuilder().addOperation(...).addNop().sign(sigMaker).post() => WAITING | REJECTED | CONFIRMED | UNKNOWN.

Chromia client talks to Chromia networks; on init it looks up system nodes: StandardChromiaClient(url).awaitAnchoredTx(brid, txRid).

FT4 Kotlin: addFtAuthenticationOp(psClient, txBuilder, operationName, pubKey, accountId) then add the real operation, sign, post. EVM: implement EvmSigner.sign(address, message) and addEvmAuthenticationOp. For an EVM signer not yet on an auth descriptor, addEvmSignaturesOp. Helpers: getAccountAuthDescriptorsBySigner, getAssetBalance. Auth types S (single) and M (multi).

### 2.3 Python — postchain-client-py

https://docs.chromia.com/build/clients/postchain-clients/python-client

pip install postchain-client-py. Python 3.7+. Async via aiohttp.

BlockchainClient.create(NetworkSettings(node_url_pool=["http://localhost:7740"], blockchain_rid=...)). query(name) or query(name, {args}). Transaction(operations=[Operation(op_name, args)], signers, signatures=None, blockchain_rid). sign_transaction then send_transaction(..., do_status_polling=True). receipt.status == CONFIRMED. Close rest_client. Test env: POSTCHAIN_TEST_NODE, BLOCKCHAIN_TEST_RID, PRIV_KEY. Keys via coincurve.PrivateKey.

### 2.4 Go — gitlab.com/chromaway/ft4-go-client

https://docs.chromia.com/build/clients/postchain-clients/go-client

go get gitlab.com/chromaway/ft4-go-client

Talks REST. Layers: GTV (ASN.1 DER) -> Merkle hash (v1 has a hash-collision bug; use v2) -> GTX -> postchain.NewClient([]*url.URL{nodeURL}).

Helpers: GetBlockchainRID(iid), GetFeatures, DetectMerkleHashVersion. gtx.NewTransaction(brid, ops, signers, merkleHashVersion); tx.Sign; PostTransaction; AwaitConfirmation(brid, txRID, 20, 500ms). Query(brid, name, gtv.DictValue). Also GetTransaction, GetTransactionInfo, GetConfirmationProof, BlockAtHeight, BlockByRID.

GTX body: [ [ blockchainRID, [ [op, [args...]], ... ], [signerPubKeys...] ], [signatures...] ]

### 2.5 Rust — postchain-client crate

https://docs.chromia.com/build/clients/postchain-clients/rust-client

Manual mode (single node) or managed mode (Directory Chain). Crate postchain-client = "0.0.3" as printed.

RestClient { node_url: vec!["http://localhost:7740", ...], request_time_out: 30, poll_attemps: 5, poll_attemp_interval_time: 5 }. query(brid, ...), send_transaction, get_transaction_status. Operation::from_dict (named) or from_list (positional). Transaction::new then sign / multi_sign_from_raw_priv_keys. RestResponse / RestError; query bytes decode with gtv::decode. Params: Null, Boolean, Integer, BigInteger, Decimal, Text, Array, Dict, ByteArray. GTV is ASN.1 DER on the wire.

### 2.6 C# — ChromiaClient (NuGet / DLL; Unity / Unity WebGL)

https://docs.chromia.com/build/clients/postchain-clients/c-sharp-client

ChromiaClient.Create(url, blockchainRID) or Create(url, iid) or Create(urlList, iid). Directory discovery: CreateFromDirectory(systemUrl, rid|iid). Requests are evenly distributed; failed nodes are retried.

Queries: client.Query<string>("get_city", ("zip", 22222)) or IGtvSerializable with [JsonProperty].

Transactions: SignatureProvider.Create() or from private key; Transaction.Build().AddOperation.AddSignatureProvider.Sign; SendTransaction / SendUniqueTransaction (auto-nop). Multi-location multi-sig: one signer produces a Signature, the other imports it. Unity WebGL: ChromiaClient.SetTransport(new UnityTransport()) before creating the client. Errors: ChromiaException / TransportException.



### 2.7 Postchain REST API

Canonical: https://docs.chromia.com/build/clients/postchain-rest-api

note

We are currently updating this documentation. While it offers a general overview, some details may be outdated. Please
check back soon for the latest version.

The Postchain Rest API, enables users to perform HTTP-based interactions
with the blockchain to access vital information. This includes querying transaction records, inspecting block data,
reviewing the blockchain’s current state, fetching configuration settings, and other related details.

Postchain API usage​

The Postchain Rest API doesn't require installation, as it can be used
directly in a web browser to send requests and receive responses.

The API offers the following groups requests, each containing a set of underlying methods available for use:

Block
Blockchain
Configuration
Error
Experimental
Node
Query
Transaction

Fetch blocks info
Fetch height of next block to be built/synced
Fetch blockchain configuration
Fetch blockchain errors
Fetch current state of blockchain node
Query node EBFT status
Make query
Submit transaction

Fetch block info by block RID
Get API version for the subnode
Validate blockchain configuration

Query all nodes EBFT statuses
Make web query
Fetch list of waiting transaction RIDs

Fetch block info by block height

Fetch the next blockchain configuration height

Get blockchain RID
Make a query with GTV encoded response
Fetch waiting transaction

Confirm block by block RID

Fetch the blockchain configuration features

Get API version
Make a GTV encoded query
Fetch list of rejected transactions

Get infrastructure version

Fetch transaction

Get infrastructure version for the subnode

Fetch transaction info

Check anchoring status for a blockchain

Fetch transactions info

Fetch number of successful transactions

Fetch transaction confirmation proof

Fetch transaction status

No install. Browser or HTTP client. Official page lists groups: Block, Blockchain, Configuration, Error, Experimental, Node, Query, Transaction. Not a full path spec.

Tx status from cookbook: Unknown, Waiting, Confirmed, Rejected (reason + timestamp).

Other official REST usages:
- GET localhost:7740/query/<BlockchainRID>?type=hello_world (cookbook create-rell-dapp)
- POST /tx with hex-encoded GTX (JS/TS architecture)
- GET http://localhost:7754/_debug (exchange replica)
- GET https://system.chromaway.com/config/<BRID>?height=0 (genesis XML)
- Default port 7740; TLS often 443. OpenAPI page /pages/postchain-restapi.html was a 532-byte stub when fetched.

### 2.8 Chromia React Kit

Canonical: https://docs.chromia.com/build/clients/react-kit

### 2.8 React Kit

Canonical: https://docs.chromia.com/build/clients/react-kit

note

We are currently updating this documentation. While it offers a general overview, some details may be outdated. Please
check back soon for the latest version.

Welcome to the Chromia React Kit documentation!

The Chromia React Kit streamlines the integration of React-based front-end applications with the Chromia blockchain. It
offers a suite of tools and react custom hooks that enable developers to build decentralized applications effortlessly
(dapps), simplifying and managing blockchain interactions.

Why use the Chromia React Kit?​

Easily connect your React app to the Chromia blockchain.

A comprehensive reusable custom React Hooks for interacting with Chromia blockchain assets via FT4 libraries and
Filehub.

Custom, reusable SWR hook for effortlessly constructing queries and operations to interact with the Dappmetadata
Provider, Postchain Client, Directory Chain and Economy Chain.

Fully TypeScript-ready.

Leverage SWR features such as caching and revalidation. Learn more at SWR

Getting started​

Install dependencies​

Begin by installing the Chromia React library:

npm install @chromia/react

Core features​

FT4 provider​

The FT4 provider manages the FT4-related state for your application. Wrap your application in this provider to ensure
that FT4-related queries and operations function correctly. We will use the provider in conjunction with Wagmi for the
example code below:

import { FtProvider } from "@chromia/react";

import { useAccount, useConnectors } from "wagmi";

import type { createWeb3ProviderEvmKeyStore } from "@chromia/ft4";

const useEthereumProvider = () => {

  const { connector } = useAccount();

  const allConnectors = useConnectors();

  const matchingConnector = allConnectors.find((c) => c.id === connector?.id);

  return matchingConnector?.getProvider();

};

function App() {

  const ethProvider = useEthereumProvider();

  return (

    <FtProvider

      useEthereumProvider={getEthereumProvider as () => Promise<Parameters<typeof createWeb3ProviderEvmKeyStore>[0]>}

      useAccount={useAccount}

      defaultClientConfig={config}

    >

      <Component />

    </FtProvider>

  );

}

Custom hooks​

The React Kit offers custom hooks to streamline interactions with the Chromia blockchain. For example:

createChromiaHooks​

Simplify interactions with the Postchain client. The createChromiaHooks function returns reusable SWR async functions
useChromiaQuery, useChromiaImmutableQuery and useChromiaInfiniteQuery, enabling you to perform various queries and
operations tailored to your specific scenarios. The example below demonstrates how to use the createChromiaHooks:

import { createChromiaHooks } from "@chromia/react";

import type { ClientConfig } from "@chromia/react";

import { FailoverStrategy } from "postchain-client";

type QueryOrOperationType = {

  [QUERY_OR_OPERATION_NAME]: {

    params: [QUERY_OR_OPERATION_PARAM_TYPE];

    return: [QUERY_OR_OPERATION_RESPONSE_TYPE];

  };

};

const clientConfig: ClientConfig = {

  directoryNodeUrlPool: [BLOCKCHAIN_URL],

  failOverConfig: {

    strategy: FailoverStrategy.AbortOnError,

  },

};

export const { useChromiaInfiniteQuery, useChromiaImmutableQuery, useChromiaQuery } =

  createChromiaHooks<QueryOrOperationType>({

    clientConfig,

  });

useFtQuery​

The useFtQuery hooks return reusable SWR async functions useSWR, enabling you to perform various queries and
operations to interact with the ft4 library. The example below demonstrates how to set the useFtQuery hook in action:

import { useFtQuery } from "@chromia/react";

const { data, isLoading, error } = useFtQuery({

  clientConfig: [CLIENT_CONFIG],

  queryName: [QUERY_NAME],

  queryParams: [QUERY_PARAM],

  accountId: [ACCOUNT_ID],

  options: [SWR_CONFIGURATION],

});

useFileHubImage​

The useFileHubImage hook simplifies interaction with Filehub, returning image data directly from it:

import { useFileHubImage } from "@chromia/react";

const image = useFileHubImage([FILEHUB_IMAGE_URL]);

Additional resources​

FT4 documentation

Learn how to create a simple app using Rell and React


### 2.9 MCP Server

Canonical: https://docs.chromia.com/build/clients/mcp-server

A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps
through the Chromia Explorer.

Overview​

The MCP Server enables AI assistants to query and analyze Chromia blockchain data, including:

Network statistics and analytics

Blockchain information and metadata

Transaction data and analysis

Asset information and distribution

Account activity and analytics

Node performance monitoring

dApp development and deployment information

Documentation retrieval and search

Documentation Tools​

The server includes RAG-powered (Retrieval-Augmented Generation) semantic documentation search that uses vector
embeddings to find relevant documentation based on meaning, not just keywords. The AI assistant will automatically use
semantic search to find and return the most relevant documentation sections.

Setup​

Production/Remote Server​

Use the following JSON configuration to connect to the production MCP server:

{

  "mcpServers": {

    "chromia-mcp": {

      "url": "https://mcp.chromia.dev"

    }

  }

}

Cursor/Windsurf IDEs:

Open Cursor settings and navigate to MCP & Integration → MCP TOOLS

Add the JSON configuration above

Claude Desktop:

Edit your Claude Desktop configuration file:

macOS: ~/Library/Application Support/Claude/claude_desktop_config.json

Windows: %APPDATA%/Claude/claude_desktop_config.json

Add the JSON configuration above

JetBrains AI Assistant:

Go to Settings → Tools → AI Assistant → MCP

Click on + to open the add dialog

In the dropdown switch from "Command" to "As JSON" and paste the JSON configuration above

ChatGPT:

Enable developer mode: Go to Settings → Apps → Advanced → Developer mode

Import Chromia MCP:

Open Workspace settings → Apps → Create

Enter the following:

MCP Server URL: https://mcp.chromia.dev

Authentication: No authentication

Click Create

In app details, new tool lists and descriptions from the MCP server will be shown

Local Server​

To be able to use the MCP server locally, you need to build and run it from the source code.

Clone the chromia-mcp repository:

Clone the chromia-mcp repository:

git clone https://gitlab.com/chromaway/core-tools/chromia-mcp.git

cd chromia-mcp

Run the application:

Run the application using gradle in SSE mode:

./gradlew :app:runSse

This will start the MCP server in SSE mode on 127.0.0.1:3001 by default.

Configure your AI assistant:

Use the following JSON configuration to connect to the local MCP server:

{

  "mcpServers": {

    "chromia-mcp": {

      "url": "http://127.0.0.1:3001"

    }

  }

}

Cursor/Windsurf IDEs:

Open Cursor settings and navigate to MCP & Integration → MCP TOOLS

Add the JSON configuration above

Claude Desktop:

Edit your Claude Desktop configuration file:

macOS: ~/Library/Application Support/Claude/claude_desktop_config.json

Windows: %APPDATA%/Claude/claude_desktop_config.json

Add the JSON configuration above

JetBrains AI Assistant:

Go to Settings → Tools → AI Assistant → MCP

Click on + to open the add dialog

In the dropdown switch from "Command" to "As JSON" and paste the JSON configuration above

Claude Rell LSP Integration​

For enhanced Rell code understanding and analysis, you can also install the Chromia LSP MCP server alongside this
blockchain MCP server. The LSP MCP server provides Claude with direct access to Rell language server capabilities for
better code comprehension and analysis.

When using the MCP through an IDE (cursor, windsurf, etc) this information is already available for your Agent.

Installation​

npm install @chromia/chromia-lsp-mcp -g

Configuration​

Add the LSP MCP server to your MCP configuration

{

  "mcpServers": {

    "lsp-mcp": {

      "command": "npx",

      "args": ["chromia-lsp-mcp", "0.8.8"]

    }

  }

}

note

The second argument "0.8.8" is optional and specifies the Rell LSP version to use. If omitted, the latest version will
be used.

This combination provides Claude with both blockchain data access and deep Rell code understanding capabilities.

Usage Examples​

Basic Network Statistics​

Example Query: What is the BRID of My Neighbor Alice, and in which cluster and container is it deployed?

Click to expand answer

Based on the search results, here's the information about My Neighbor Alice blockchains on Chromia mainnet:
My Neighbor Alice Blockchain Information
I found 4 My Neighbor Alice blockchains on mainnet:
Active/Running Blockchains:

Main My Neighbor Alice Blockchain

BRID (Blockchain RID): F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2

Name: my_neighbor_alice

Cluster: pink

Container: 611062cdf1047b265b652d229858e5a18dbf487afbb62f0f6d30ec4fd81366f9

State: RUNNING ✅

My Neighbor Alice BS2

BRID: 8BC5E4DAA94410BEC640DAEE87CE5C546465C2E7E8D654668BA6267200C97560

Name: my_neighbor_alice_bs2

Cluster: pink

Container: 9f49e091ff1929bc6438a9ee9c5387ec7991eb5eda4154cd7509dd878aa9f70e

State: RUNNING ✅

Removed/Legacy Blockchains:

Legacy Instance 1

BRID: 9F0CCB09BBB441E840035086F5326294F51FFA92344D39FFF54B223C7547D81D

State: REMOVED

Legacy Instance 2

BRID: AEBD8E8CD9FCC268D7B5AC3DAC8855F1F9A817B8DD1A98C0F13E60D01DECB90C

State: REMOVED

Summary
The currently active My Neighbor Alice deployment is:

Primary BRID: F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2

Deployed in: pink cluster

Container ID: 611062cdf1047b265b652d229858e5a18dbf487afbb62f0f6d30ec4fd81366f9

Both active blockchains are running in the pink cluster, which suggests this is a multi-blockchain deployment for
the My Neighbor Alice game ecosystem.

Other query examples:

Get network statistics and analytics for the Chromia Mainnet environment

What is the brid of my neighbor alice, and in which cluster and container it is deployed on

Get the latest 10 transactions on Mainnet with operation type "transfer"

Show activity for account ID: DEF456... across all blockchains

Which account holds most of CHR asset on mainnet

How many active users does my neighbor Alice's dApp have on mainnet?

How to register an account using FT4 open strategy in Rell, can you provide TypeScript client code as well

what is the difference between query and operation in Rell

Could you create a Rell application for managing books that includes FT4 account registration functionality

how to use Chromia CLI to deploy my newly created dapp on testnet

What are the steps to deploy a dApp on Chromia testnet using CLI?

How do I configure network settings for a Chromia blockchain?

Advanced Blockchain Querying​

You can query specific dApps deployed on Chromia networks and execute their custom queries:

Can you run get all libraries query against library chain dApp on testnet?

Expected workflow:

The AI will search for blockchains with "library chain" on testnet

Retrieves the blockchain RID

Fetches the dApp structure to discover available queries and operations using Postchain Client

Executes the get_all_libraries query inside Cursor/Junie...

Returns the results from the library dApp

Other examples of blockchain-specific queries:

Get the deployed dApp structure for My Neighbor Alice on mainnet

Run the get_user_balance query on MNA blockchain for account ABC123

Can you list all the queries available for My Neighbor Alice on mainnet ?

Rell Query Translation to SQL and vice versa​

Simple SELECT translation:

"Can you translate this SQL query to Rell?

SELECT name, genre FROM plays WHERE duration_minutes > 120;"

JOIN queries:

How would I write this SQL join in Rell?

SELECT p.name, t.name, b.timestamp

FROM bookings b

JOIN performances p ON b.performance_id = p.id

JOIN theater_halls t ON p.theater_id = t.id

WHERE b.status = 'CONFIRMED';"

Aggregation queries:

Convert this SQL aggregation to Rell syntax:

SELECT play_name, COUNT(*) as total_bookings, SUM(price) as revenue

FROM bookings b

JOIN performances p ON b.performance_id = p.id

GROUP BY play_name

HAVING COUNT(*) > 5;"

Subqueries:

How do I write this SQL subquery in Rell?

SELECT name FROM plays

WHERE id IN (

    SELECT play_id FROM performances

    WHERE timestamp > NOW()

);"

Complex conditions:

Translate this SQL query with multiple conditions to Rell:

SELECT DISTINCT p.name, t.name

FROM plays p

JOIN performances pf ON p.id = pf.play_id

JOIN theater_halls t ON pf.theater_id = t.id

WHERE p.genre = 'DRAMA'

AND pf.timestamp BETWEEN ? AND ?

AND EXISTS (

    SELECT 1 FROM bookings b

    WHERE b.performance_id = pf.id

);"

From Rell to SQL

Can you translate this Rell query to SQL?

(b: bookings, p: performances) @* {

    b.performance_id == p.id

} (

    @group play_name = p.play_name,

    total_bookings = @sum 1,

    revenue = @sum b.price

) @* {

    .total_bookings > 5

}

When asking it's helpful to:

Provide the complete SQL/Rell query with proper formatting

Specify any relevant entity structures when working with an external project

Mention any special requirements (e.g., sorting, limiting, null handling)

Include context about the data model if it's not obvious

Example complete prompt:

I have these entities in my Rell code:

entity play {

    name: text;

    genre: text;

    duration: integer;

}

entity performance {

    play: play;

    date: timestamp;

    status: text;

}

Can you help me translate this SQL query to Rell?

SELECT p.name, COUNT(pf.id) as performance_count

FROM plays p

LEFT JOIN performances pf ON p.id = pf.play_id

WHERE p.genre = 'DRAMA'

GROUP BY p.name

HAVING COUNT(pf.id) > 5

ORDER BY performance_count DESC;"

This format provides all the necessary context for accurate translation. The AI can understand:

The exact data structure

The relationships between entities

The desired query logic

Any special requirements for the output

Networks​

The server supports multiple Chromia networks:

Mainnet - Production network

Testnet - Testing network

Specify the network parameter in your queries to target the appropriate environment.


### 2.10 Bridge client stub

Canonical: https://docs.chromia.com/build/clients/bridge-client


### 2.10a Bridge work-with-client

Canonical: https://docs.chromia.com/ecosystem/bridge/bridge-client/work-with-client

To interact with the Chromia blockchain using the bridge client, you'll need to manage an active Session that handles
all Chromia-related queries and operations. However, the bridge client can be initialized without a session at the
start. Here's a step-by-step guide to set up your application using @chromia/ft4.

// 1: Setup a connection to postchain through postchain-client

const pcl = await createClient({

  nodeUrlPool: "YOU_NODE_URL_POOL",

  blockchainRid: "YOUR_BLOCKCHAIN_RID",

});

// 2: Create an account

const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);

const ad = createSingleSigAuthDescriptorRegistration([AuthFlag.Account, AuthFlag.Transfer], evmKeyStore.id);

const response = await registerAccount(pcl, evmKeyStore, registrationStrategy.open(ad));

// 3: Log in your user

const evmKeyStoreInteractor = createKeyStoreInteractor(pcl, evmKeyStore);

const accounts = await evmKeyStoreInteractor.getAccounts();

const session = await evmKeyStoreInteractor.getSession(accounts[0].id);

note

Ensure that you save your evmKeyStore in your application state, as some methods in @chromia/bridge-client will
require it.

With your Session object ready, you can initialize the bridge client. You will also need to provide an EVM Provider,
either a BrowserProvider or a JsonRpcProvider:

const provider = new BrowserProvider(window.ethereum);

const bcl = await bridgeClient(

  { bridgeAddress: "YOUR_BRIDGE_ADDRESS", tokenAddress: "YOUR_TOKEN_ADDRESS" },

  provider,

  session

);

If your application setup prevents having an active Session when instantiating the bridge client, you can still set up
the client without it:

const provider = new BrowserProvider(window.ethereum);

const bcl = await bridgeClient({ bridgeAddress: "YOUR_BRIDGE_ADDRESS", tokenAddress: "YOUR_TOKEN_ADDRESS" }, provider);

Later, when you have access to the Session, you can set it on the client with setSession(session: Session):

bcl.setSession(session);

### 2.10b Bridge example

Canonical: https://docs.chromia.com/ecosystem/bridge/bridge-client/example

Bridge from EVM to Chromia​

Approve token spending​

Ensure the user approves token spending by the token bridge:

const approvalResponse = await bcl.approveDepositAmount(BigInt(10));

Bridge from EVM to Chromia​

To bridge tokens from EVM to Chromia, use the depositToEvmBridgeContract method, specifying the number of tokens to
bridge:

const contractTransactionResponse = await bcl.depositToEvmBridgeContract(BigInt(100));

If the user has created an FT4 account and linked it to their EVM address, funds for foreign tokens will be minted
directly to that account. For native tokens, funds will be transferred from the bridge account to the user's account.

If the user hasn't created an account, or has created one but hasn't linked it to their EVM address, funds for foreign
tokens will be minted to the pool account instead. For native tokens, funds will be transferred from the bridge
account to the pool account. In this case, the user must create and link an FT4 account. Once this is done, the funds
will be transferred from the pool account to the user's account.

Link EVM account​

After a deposit, link the EVM account with the corresponding FT4 account created during the deposit process. Provide
the evmKeyStore you created earlier:

const accountLinkingResponse = await bcl.linkEvmEoaAccount(evmKeyStore);

note

If the EVM account is linked, it will be returned to the accountLinkingResponse. An EVM account can have multiple
FT4 accounts linked to it.

Bridge from Chromia to EVM​

Bridge from Chromia​

Call the bridgeFromChromia method with the amount and the asset ID:

// Network ID does not need to be provided as it will be fetched from the provider

const transactionResponse = await bcl.bridgeFromChromia(BigInt(10), Buffer.from("YOUR_ASSET_ID", "hex"));

Request withdrawal from EVM bridge​

Create a pending withdrawal request​

This needs to be accepted by the user to start the withdrawal process using the requestEvmWithdraw method:

const erc20WithdrawalInfo = await bcl.getErc20WithdrawalByTransactionRid(

  transactionResponse.receipt.transactionRid,

  opIndex

);

// Get event proof for withdrawal

const eventProof = await bcl.getWithdrawRequestEventProof(erc20WithdrawalInfo.event_hash);

// Request withdrawal

const requestedWithdraw = await bcl.requestEvmWithdraw(eventProof);

Check withdrawal status​

Depending on the bridge contract configuration, the user must wait a certain number of blocks on EVM to complete their
withdrawal. This can be done using the getPendingWithdrawFromProof method:

const { block_number } = await getPendingWithdrawFromProof(eventProof);

Once the block_number has been reached on the target EVM chain, the user can withdraw their tokens:

const withdrawal = await bcl.evmWithdraw(eventProof.leaf as Buffer);

Additional methods​

getErc20Deposits(filter?: DepositFilter, pageSize?: number, pageCursor?: string): Returns all deposits specified by
the filter.

getErc20Withdrawals(filter?: WithdrawFilter, pageSize?: number, pageCursor?: string): Returns all withdrawals from
the EVM bridge specified by the filter.

setBlockchainRid(blockchainRid: Buffer): Sets the blockchain RID.

### 2.10c Bridge client setup

Canonical: https://docs.chromia.com/ecosystem/bridge/bridge-client/client

The Chromia bridge client is a TypeScript library with utilities
for interacting with the Chromia Token Bridge. This topic will walk you through setting up the Chromia bridge client to
facilitate token transfers between Chromia and Ethereum Virtual Machine (EVM) networks.

Prerequisites​

Before you begin, ensure you have the following:

TokenBridge contract: The address of a deployed TokenBridge contract on an EVM network.

ERC20 token contract: The address of an ERC20 token on the same EVM network.

Blockchain RID: The RID of a Chromia blockchain using the hbridge Rell library.

Configuration checklist​

Approve ERC20 token​

The ERC20 token must be approved by the TokenBridge contract owner using the allowToken function. You can
accomplish this with the @chromia/bridge-client using the allowToken method:

const contractTransactionResponse = await bcl.allowToken(token);

Check spending allowance​

Verify the spending allowance with the checkAllowance method:

const allowance: bigint = await bcl.checkAllowance();

Set blockchain RID​

Use the setBlockchainRid function in the TokenBridge contract to set the blockchain RID. You can use the built-in
method setBlockchainRid with @chromia/bridge-client:

const contractTransactionResponse = await bcl.setBlockchainRid(rid);

Validator configuration​

Convert the public keys of the validating nodes in the Chromia network to EVM addresses and configure them in the
Validator contract, either during deployment or by calling the updateValidators function.

### 2.11 Filehub client stub

Canonical: https://docs.chromia.com/build/clients/filehub-client


### 2.11a Filehub work

Canonical: https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-work

Filehub allows users to store files on-chain and retrieve them using the
Filehub NPM package. A Gateway is also available for easy file access,
supporting various use cases such as images, videos, and static websites.

Filehub client​

Fetching a file​

To retrieve a file stored in Filehub, use the following code:

const { Filehub } = require("filehub");

const filehub = new Filehub({

  directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL,

  blockchainRid: FILEHUB_BLOCKCHAIN_RID,

});

const file = await filehub.getFile(fileHash);

Storing a file​

To store a file in Filehub, utilize the following example:

const { Filehub, FsFile } = require("filehub");

const filehub = new Filehub({

  directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL,

  blockchainRid: FILEHUB_BLOCKCHAIN_RID,

});

const file = FsFile.fromData(buffer, { contentType: "image/jpeg" });

await filehub.storeFile(ft4Session, file);

Payment model​

Filehub operates on a one-time payment model, charging users $0.10 per megabyte for permanent file storage. Unlike
traditional storage solutions that may rely on random hard drives with uncertain longevity, Filehub ensures that files
are fully stored on-chain, providing a guarantee of long-term accessibility.

note

Currently, Filehub operates on a general-purpose cluster, which is not specifically optimized for storage needs. As a
result, the available storage capacity is somewhat limited. The initial release of Filehub aims to support projects on
Chromia while providing early adopters an opportunity to explore its capabilities and potential applications. Looking
ahead, we anticipate the introduction of dedicated storage-optimized clusters on the , which would enable us to
significantly reduce storage costs for users.

Filehub APIs​

Filehub offers a robust API for managing Filechains, facilitating file storage, and configuring payments. Key operations
include:

Registering a Filechain: Assign a new Filechain with administrative privileges.

Enabling/Disabling Filechains: Control the availability of Filechains based on storage requirements.

Managing Payments: Enable or disable payments for file storage and configure the assets used for transactions.

Example​

Here’s how to register a new Filechain using the Filehub Administrator API:

const filehub = new Filehub({

  directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL,

  blockchainRid: FILEHUB_BLOCKCHAIN_RID,

});

const admin = newSignatureProvider({ privKey: FILEHUB_ADMIN_PRIVKEY });

const filehubAdministrator = new FilehubAdministrator(filehub, admin);

// Register a Filechain

await filehubAdministrator.registerFilechain(FILECHAIN_BLOCKCHAIN_RID, FILECHAIN_ADMIN_EVM_ADDRESS);

### 2.11b Filehub overview

Canonical: https://docs.chromia.com/ecosystem/filehub/overview

Filehub is a scalable, reliable, and secure storage platform that Chromia's relational blockchain powers. It provides
developers with a decentralized solution for storing and accessing files, ensuring data integrity and availability even
when faced with external disruptions.

Cost​

Filehub operates on a fixed payment model for storage. The current price is $0.10 per MB for perpetual storage. Your
files will remain accessible for a minimum of 30 years. Access continues indefinitely with no renewal fees, as long as
the network remains operational.

File access​

Everyone can access all files stored in Filehub free of charge. If you want to store private files, you must encrypt
them before upload.

Getting started with Filehub​

To use Filehub, you need to provide it with CHR from the Economy Chain. A minimum deposit of 1 CHR is required to create
an account.

If your CHR is currently on an EVM chain, you can create an Economy Chain account and bridge it over using the
Vault Transfer feature.

Once your CHR is on the Economy Chain, you can create an account and transfer your CHR to the Filehub Chain using the
Vault UI for Filehub.

Finally, navigate to the Filehub UI and start uploading your files.

You can also access the Filehub UI on Testnet, which behaves the same as the
Mainnet version but uses Testnet CHR. You can request Testnet CHR from the
Chromia Faucet. Files uploaded to Testnet are not guaranteed to be stored
permanently.

Components​

Filehub consists of two fundamental components:

Filehub blockchain: This acts as the central index, tracking files and managing references to data chunks
distributed across multiple Filechains.

Filechain blockchain(s): These function as the actual blob storage, responsible for persisting file data

Filehub​

The Filehub blockchain serves as the central indexing system, responsible for:

Managing the administration and availability of Filechains.

Selecting the appropriate Filechains for distributing and storing each file chunk.

Maintaining a comprehensive index of file chunks and their corresponding Filechain locations.

Handling the billing system for file storage, ensuring proper payment for services.

Filehub’s architecture is modular and scalable. It starts with a single Filehub and two Filechains, with the flexibility
to expand incrementally by adding more Filechains as storage requirements grow. This design supports unlimited
horizontal scaling, increasing both storage capacity and file distribution efficiency.

Filechain​

The Filechain Blockchain(s) function as blob storage, handling the following tasks:

Storing file chunks in a secure and decentralized manner.

Validating payments made to Filehub using
ICCF (Interchain Confirmation Facility)-proofs.

Hashing incoming data and verifying it against expected hashes to ensure data integrity.

Accepting transactions and persisting file chunks on the Filechain blockchain.

Filechain serves strictly as a blob storage solution. It operates without knowledge of file metadata or the
relationships between chunks. Its sole responsibility is to store data chunks and validate their integrity.

ICCF and anchoring chain​

The ICCF (Interchain Confirmation Facility) facilitates communication between
Filehub and Filechains. This framework ensures that file chunks are only stored on Filechains after payment has been
confirmed.

Here's how the process works:

Payment and allocation: When a user uploads a file to Filehub, the platform allocates storage on a suitable
Filechain and processes the payment.

ICCF proof generation: Filehub generates an ICCF proof, which is a cryptographic signature verifying that the
file has been allocated and paid for.

Proof submission: The ICCF proof is submitted to the target Filechain.

Proof verification: The Filechain independently verifies the ICCF proof to confirm that the payment for the
corresponding file chunk has been completed.

This indirect communication mechanism, facilitated by the ICCF and the anchoring chain, ensures the security and
integrity of the file storage process. By verifying payments before storing file chunks, Filehub prevents unauthorized
access and ensures that users only pay for the storage they use.

### 3. FT4 client index

Canonical: https://docs.chromia.com/build/ft4/client/

The FT4 Client provides the interface through which applications interact with the blockchain. It facilitates operations
such as querying assets, signing transactions, transferring assets, and performing cross-chain transfers.

To set up the FT4 client with TypeScript, install the necessary packages using npm install @chromia/ft4. Initialize the client by creating a connection to the Postchain network, and use it to interact with the blockchain (e.g., querying assets).

FT4 supports Single-Signature (SingleSig) and Multi-Signature (MultiSig) auth descriptors, allowing customizable
permissions and rules for secure access control in decentralized applications.

Register accounts using Chromia-native or MetaMask (EVM-compatible) keys with examples for key pair management and
the open registration strategy.

FT4 enables non-interactive signing with disposable keys. These keys can be managed via the login function, with
care needed when assigning sensitive auth flags. Keys can be stored with different keystore options and cleared on
logout for security.

FT4's KeyStore interface manages cryptographic keys for signing transactions. It includes implementations for
Ethereum-compatible keys (EvmKeyStore) and FT4-specific keys (FtKeyStore), supporting in-memory, session, and local
storage options.

To transfer assets with FT4, use the Chromia CLI with the ft4.transfer command or the Postchain client library to
initiate a transfer using a session. Ensure proper setup of assets, accounts, and key pairs before transferring.

Use the orchestrator for cross-chain asset transfers, ensuring multichain configuration and ICCF module inclusion. Transfer assets between source and target chains with proper error handling and asset registration.

### 3.1 FT4 client setup

Canonical: https://docs.chromia.com/build/ft4/client/client-setup

This section discusses how to install and initialize the FT4 client.

Install the client​

Use npm to install the
FT4 client:

npm install @chromia/ft4

Initialize the client​

In your JavaScript code, initialize the FT4 client:

const { createClient } = require("postchain-client");

const { createConnection } = require("@chromia/ft4");

const url = "http://localhost:7740";

const client = await createClient({

  nodeUrlPool: url,

  blockchainIid: 0,

});

const connection = createConnection(client);

In the code snippet above, we import the necessary modules from the postchain-client and @chromia/ft4 packages.
Next, we define the url of the Postchain network we want to connect to (in this case,
"http://localhost:7740"). We then create an instance of the IClient using the specified URL
and the internal ID of the chain, which by default is 0 on a local network.

With the client initialized, you can now utilize its capabilities to interact with the Postchain network and perform
various operations, such as sending transactions and querying the blockchain.

We then pass the client to the createConnection method from FT4, allowing us to easily use all of the FT4 features.

Test the connection​

To test the connection, add a function to call a method on the connection object:

const { createClient } = require("postchain-client");

const { createConnection } = require("@chromia/ft4");

async function main() {

  const url = "http://localhost:7740";

  const client = await createClient({

    nodeUrlPool: url,

    blockchainIid: 0,

  });

  const connection = createConnection(client);

  console.log(await connection.getAllAssets()); // This line is new

}

main();

Run the above example with node index.js. This will print all the assets registered on the blockchain. If no assets
are registered, the response will look like this:

{ data: [], nextCursor: null }

### 3.2 FT4 account registration

Canonical: https://docs.chromia.com/build/ft4/client/client-account-registration

This section demonstrates how to register a new account on the blockchain using either Chromia-native keys or
MetaMask (EVM-compatible keys). By now, you should have a client connection established and be familiar with auth
descriptors.

note

The main Rell module should include the following import to enable account registration:

import lib.ft4.external.accounts.strategies;

The examples use the open registration strategy, which requires:

import lib.ft4.core.accounts.strategies.open;

For reference, consult the FT4 Rell documentation for other strategies and
additional use cases.

Chromia-native account registration​

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

An available key pair can be passed directly to registerChromiaNativeAccount, or one can be generated as follows:

const keyPair = encryption.makeKeyPair(); // Generates a key pair with pubKey and privKey

console.log("Save your private key securely:", keyPair.privKey.toString("hex"));

Call registerChromiaNativeAccount, passing the connection and the key pair:

await registerChromiaNativeAccount(connection, keyPair);

MetaMask account registration​

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

To register an account with MetaMask, make sure the browser plugin is available, and call the function with a valid
connection:

await registerMetaMaskAccount(connection);

In this repository, you can find additional code examples
for account registration.

### 3.3 FT4 auth descriptors

Canonical: https://docs.chromia.com/build/ft4/client/client-auth-descriptors

The FT4 client library provides flexible tools for creating Single-Signature (SingleSig) and Multi-Signature
(MultiSig) auth descriptors, along with optional rules to customize the permissions.

This document explains the implementation details and demonstrates how to create and instantiate auth descriptors for
various use cases.

Auth descriptor structure​

An auth descriptor is represented by the following TypeScript type:

AuthDescriptor<T>​

export type AuthDescriptor<T extends SingleSig | MultiSig> = {

  id: Buffer; // Unique identifier

  accountId: Buffer; // Account to which this auth descriptor belongs

  authType: AuthType; // "S" for SingleSig, "M" for MultiSig

  rules: AuthDescriptorRules | null; // Optional rules for descriptor

  created: Date; // Creation timestamp

  args: T; // Signature details (SingleSig or MultiSig)

};

Types of signatures​

SingleSig: Used for single-key authentication.

export type SingleSig = {

  flags: string[]; // Permissions (e.g., "A", "T")

  signer: Buffer; // Public key for the single signer

};

MultiSig: Used for multi-key authentication.

export type MultiSig = {

  flags: string[]; // Permissions

  signaturesRequired: number; // Number of signatures required for operation

  signers: Buffer[]; // List of public keys

};

Creating auth descriptors​

The FT4 library provides two primary functions for creating auth descriptors:

createSingleSigAuthDescriptorRegistration

createMultiSigAuthDescriptorRegistration

Function: createSingleSigAuthDescriptorRegistration​

Creates a SingleSig auth descriptor with the specified permissions, public key, and optional rules.

Parameters:

flags: string[] - List of permissions (e.g., ["A"] for account edits, ["T"] for transfers).

pubKey: Buffer - Public key of the signer.

rules?: AuthDescriptorRules - Optional rules for the descriptor.

Example:

const authDesc = createSingleSigAuthDescriptorRegistration(["A", "T"], pubKey);

Function: createMultiSigAuthDescriptorRegistration​

Creates a MultiSig auth descriptor with the specified permissions, multiple public keys, the number of required
signatures, and optional rules.

Parameters:

flags: string[] - List of permissions.

pubKeys: Buffer[] - List of public keys.

signaturesRequired: number - Number of required signatures.

rules?: AuthDescriptorRules - Optional rules for the descriptor.

Example:

const authDesc = createMultiSigAuthDescriptorRegistration(["T"], [pubKey1, pubKey2], 2);

Using rules in auth descriptors​

Rules further refine the conditions under which auth descriptors can be used. These can be simple or complex.

Simple rule​

A simple rule specifies a condition for a single variable.

Structure:

export type SimpleRule<T extends string> = {

  variable: T; // Variable being constrained (e.g., opCount)

  operator: RuleOperator; // Comparison operator (e.g., lessThan)

  value: number; // Value to compare against

};

Example: "Expire after 2 uses"

const rule = { variable: "opCount", operator: "lessThan", value: 2 };

Complex rule​

A complex rule combines multiple simple rules using logical operators (e.g., "and").

Structure:

export type ComplexRule<T extends string> = {

  operator: "and";

  rules: SimpleRule<T>[];

};

Example: "Expire after 2 uses and only allow transfers"

const complexRule = {

  operator: "and",

  rules: [

    { variable: "opCount", operator: "lessThan", value: 2 },

    { variable: "permissions", operator: "equals", value: "T" },

  ],

};

Examples of creating auth descriptors​

Single-sig example​

"Whenever the public key signs, they can edit the account or transfer funds."

const authDesc = createSingleSigAuthDescriptorRegistration(["A", "T"], pubKey);

Multi-sig example 1​

"Whenever both pubKey1 and pubKey2 sign, they can transfer funds."

const authDesc = createMultiSigAuthDescriptorRegistration(["T"], [pubKey1, pubKey2], 2);

Multi-sig example 2​

"The first time 3 out of 5 keys sign, they can edit the account."

const authDesc = createMultiSigAuthDescriptorRegistration(["A"], [pubKey1, pubKey2, pubKey3, pubKey4, pubKey5], 3, {

  variable: "opCount",

  operator: "lessThan",

  value: 2,

});

Instantiate rules​

Here's a few example of possible rules which can be created:

// Example rules

const lessThanThreeTimes = lessThan(opCount(3));

const untilBlock100 = lessOrEqual(blockHeight(100));

const during2024 = and(

  greaterOrEqual(blockTime(1704067200000)), /Jan 1st, 2024 (millisecond timestamp)

  lessThan(blockTime(1735689600000)) /Jan 1st, 2025

);

const onceDuring2024 = and(

  greaterOrEqual(blockTime(1704067200000)), /Jan 1st, 2024

  lessThan(blockTime(1735689600000)), /Jan 1st, 2025

  lessThan(opCount(2))

);

Instantiation steps​

Generate key pairs Create or load the public/private key pairs:

const pubKey1 = Buffer.from("public_key_1_hex", "hex");

const pubKey2 = Buffer.from("public_key_2_hex", "hex");

Define permissions Use flags like ["A"] for account editing or ["T"] for transfers.

Add rules (optional) Specify simple or complex rules for additional constraints.

Create the auth descriptor Use the appropriate function for SingleSig or MultiSig auth descriptors.

Key points to remember​

Flags: Control what the auth descriptor allows (e.g., "A" for account editing, "T" for transfers).

Rules: Add constraints like expiration or operational limits.

Signature type: Choose SingleSig for single-key auth or MultiSig for multi-key requirements.

### 3.4 FT4 key store

Canonical: https://docs.chromia.com/build/ft4/client/client-key-store

In the FT4 client library, the KeyStore interface represents a general abstraction for managing cryptographic keys.
This abstraction is crucial for implementing security and transaction signing mechanisms. Two key implementations extend
this interface:

EvmKeyStore: Designed for Ethereum-compatible keys.

FtKeyStore: Designed for FT4-specific keys.

Both implementations provide methods to sign messages or transactions and to integrate with authentication handlers.

For an example implementation of these keystores, refer to the
Chromia Keystore Demo repository.

KeyStore interface​

The KeyStore interface defines the foundational structure and behavior for managing cryptographic keys:

Properties:

id: Buffer: A unique identifier for the key.

isInteractive: boolean: Indicates if signing requires user interaction (e.g., MetaMask).

EvmKeyStore implementation​

Description​

The EvmKeyStore extends KeyStore and implements the EvmSigner interface. It is designed for managing
Ethereum-compatible (EVM) keys and includes functionality to sign Ethereum messages and handle EVM-specific operations.

Properties and methods​

Properties:

address: Buffer: The Ethereum address associated with the key (without the leading 0x).

Methods:

signMessage(message: string): Promise<Signature>: Signs a given message using the EVM key.

Instantiation options​

In-memory EvmKeyStore

Created with ephemeral keys stored in memory.

Method: createInMemoryEvmKeyStore(keyPair: KeyPair): EvmKeyStore

Example:

const keyPair = { privKey: Buffer.from("private_key_hex", "hex") };

const evmKeyStore = createInMemoryEvmKeyStore(keyPair);

Generic EvmKeyStore

Allows customization with a provided signing function and configuration.

Method:
createGenericEvmKeyStore(config: { address: string, signMessage: Function, isInteractive?: boolean }): Promise<EvmKeyStore>

Example:

const evmKeyStore = await createGenericEvmKeyStore({

  address: "YourEthereumAddress",

  signMessage: async (message) => "signature_string",

  isInteractive: false,

});

Web3 provider EvmKeyStore

Integrates with external Web3 providers (e.g., MetaMask).

Method: createWeb3ProviderEvmKeyStore(externalProvider: Eip1193Provider): Promise<EvmKeyStore>

Example:

const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);

FtKeyStore implementation​

The FtKeyStore extends KeyStore and implements the FtSigner interface. It manages FT4-specific keys and supports
signing FT4 transactions.

Properties and methods​

Properties:

pubKey: Buffer: The public key associated with the FT4 key.

Methods:

sign(transaction: GTX | RawGtx): Promise<Buffer>: Signs an FT4 transaction.

Instantiation options​

In-memory FtKeyStore

Created with ephemeral keys stored in memory.

Method: createInMemoryFtKeyStore(keyHolder: KeyPair | SignatureProvider): FtKeyStore

Example:

const keyPair = { privKey: Buffer.from("private_key_hex", "hex") };

const ftKeyStore = createInMemoryFtKeyStore(keyPair);

Storage options​

The FT4 client supports storing keys in:

Memory: Keys are stored temporarily and cleared upon application restart.

Session storage: Keys persist only for the duration of the browser session using
createSessionStorageLoginKeyStore.

Local storage: Keys persist across browser sessions and application restarts using
createLocalStorageLoginKeyStore.

Using in-memory storage​

In-memory storage is the default for ephemeral keys, especially for testing.

note

When login keys (private keys) are stored in an in-memory KeyStore, they are held in volatile memory (RAM) and are
cleared upon page refresh. This also removes the disposable
authentication descriptor associated with the session. While the FT4 session
remains valid at the blockchain level—since the blockchain does not invalidate sessions upon a client refresh—you will
lose the ability to perform authenticated actions.
This is because actions requiring authentication depend on both
the disposable authentication descriptor and login keys, the latter being no longer accessible. Although the disposable
authentication descriptor remains technically accessible on the blockchain, it cannot be used without the associated
login keys.
To restore functionality, you need to
reinitialize or reauthenticate the session by providing a new
disposable authentication descriptor.

Custom storage implementation​

For session or local storage, wrap the KeyStore creation logic to persist the keys using browser APIs or custom
storage strategies.

Key differences​

Feature
EvmKeyStore
FtKeyStore

Purpose
Ethereum-compatible signing
FT4-specific signing

Identifier
address
pubKey

Signing Method
signMessage
sign

Storage Options
Memory, Session, Browser
Memory

Example usage​

Case 1: Integrating with Ethereum using Web3ProviderEvmKeyStore​

const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);

const signature = await evmKeyStore.signMessage("Hello, Ethereum!");

Case 2: Managing keys in memory using InMemoryFtKeyStore​

const keyPair = { privKey: Buffer.from("private_key_hex", "hex") };

const ftKeyStore = createInMemoryFtKeyStore(keyPair);

const signedTx = await ftKeyStore.sign(someTransaction);

For a complete reference, please consult the
FT4 Authentication module documentation.

### 3.5 FT4 login

Canonical: https://docs.chromia.com/build/ft4/client/client-login

To improve the user experience in web applications, FT4 provides a mechanism for generating disposable keys and adding
them to a user's account. This allows non-interactive signing of operations using the directly accessible new key,
eliminating the need for the user to sign each operation with MetaMask.

However, it's crucial to exercise caution when adding auth flags to disposable keys, as compromised keys with sensitive
flags could lead to asset compromisation or other security risks.

FT4 offers a login function that simplifies the process of generating and managing disposable keys.

Logging in​

The following example demonstrates adding a disposable key to an account using the login function and calling the
foo and bar operations. Only one message signing is required to add a new auth descriptor, while the foo and bar
operations are called without signing.

More information about messages and authentication can be found
here.

createWeb3ProviderEvmKeyStore(window.ethereum).then(async (store: EvmKeyStore) => {

  const { getAccounts, login } = createKeyStoreInteractor(client, store);

  const accounts = await getAccounts();

  if (!accounts.length) return;

  const { session, logout } = await login({

    accountId: accounts[0].id,

  });

  await session.call(op("foo"), op("bar", "some other text", 123456));

  // more calls here ...

  await logout();

});

However, the disposable auth descriptor will still require a signature if a transfer operation is called because it doesn't
have the "T" (transfer) flag.

To sign the transfer transaction using the disposable auth descriptor, modify the login function call and add the "T" flag
to the auth descriptor:

const { session, logout } = await login({

  accountId: accounts[0].id,

  config: {

    flags: ["T"],

  },

});

The disposable auth descriptor will be used to sign the transfer transaction instead of the master auth descriptor.

If we want, we can also specify a keystore when logging in which then will hold the signing key that will be used with
the disposable auth descriptor. It is a good idea to always specify a keystore as it gives increased control over the
lifecycle of the disposable key. If no keystore is provided, the disposable key will only be stored in memory and will
thus be cleared when the browser window is reloaded. Forcing the user to login on each page reload can lead to bad UX.

Furthermore, using an in memory key store while developing could be problematic since each page reload will cause a new
disposable auth descriptor to be added. In a development scenario, where each code change might trigger a hot reload of
the page, one will quickly hit the upper limit of how many auth descriptors can be added, resulting in the inability of
logging in to the account until the login time to live is reached.

To use a more durable keystore, we can specify it when logging in:

const { session } = await login({

  accountId: accounts[0].id,

  config: {

    flags: ["T"],

  },

  loginKeyStore: createSessionStorageLoginKeyStore(),

});

This will instead store the disposable key in the browsers session storage and consequently have the same lifecycle. Out
of the box, there is also a variant that will store the key in the browsers local storage. The login function accepts
any instance of the
LoginKeystore
interface which means that it is possible to implement a custom login keystore, e.g., for cases when the dapp does not
run in a browser context.

One can also add rules to be applied to the new auth descriptor, such as for how long it should be valid:

const { session } = await login({

  accountId: accounts[0].id,

  config: {

    flags: ["T"],

    rules: ttlLoginRule(minutes(30)),

  },

  loginKeyStore: createSessionStorageLoginKeyStore(),

});

If no login config is provided. The default config will be used. The default timeout is then one day and the flags
will be set to [].

caution

Disposable keys aren't securely stored. Therefore, never add auth flags that could lead to asset compromise if the
disposable key pair is compromised.

Logging out​

Logging out is a necessary step after login which removes the disposable key's access to the account, removing the risk
of misuse in case that key is leaked. To do so, you simply have to call the logout function:

const { session, logout } = await login({

  accountId: accounts[0].id,

});

// use the disposable key...

await logout();

There are two main reasons to logout:

the disposable key might be leaked and used to access the account by third parties, in a way that is comparable to
authentication token stealing in web2;

every account has a limit to how many auth descriptor can be attached to it, and failing to remove old unused ones
brings the account closer to this limit. Reaching the maximum number of auth descriptors added to the account will
prevent the user from logging in again until some auth descriptors expire.

For these reasons, always remember to call the logout function to destroy disposable keys when they are no longer
needed.

If you want the better UX given by being able to login without user interaction, you can use a more durable login
keystore when logging in (like createSessionStorageLoginKeyStore()). This can also help reduce the risk of being
locked out from the account during development.

A more durable keystore will prevent the buildup of unusable auth descriptors on the account.

For more information on the maximum auth descriptor limit, check out the
Use auth descriptors for accounts topic.

### 3.6 FT4 transfer assets

Canonical: https://docs.chromia.com/build/ft4/client/client-transfer-assets

Transferring assets is a fundamental operation in decentralized applications built on the FT4 framework. FT4 provides a
secure and efficient way to transfer assets between accounts, ensuring proper authentication and authorization. This
section will guide you through the process of transferring assets using both the Chromia CLI and the Postchain client
library.

Prerequisites​

Before you can transfer assets, you need to have the following:

Registered assets: You must have registered one or more assets on the blockchain. If you haven't done so already,
follow the instructions in the Register assets section.

Registered accounts: You need at least two registered accounts on the blockchain: a sender account and a
recipient account. Refer to the Register FT4 accounts section for instructions on
account registration.

Minted tokens: Ensure that the sender account has a balance of the asset you wish to transfer. You can mint
tokens to an account using the ft4.admin.mint operation.

tip

To mint tokens to the sender account:

chr tx ft4.admin.mint \

  SENDER_ACCOUNT_ID \

  ASSET_ID \

  AMOUNT_WITH_DECIMALS \

  --await --secret .chromia/ft4-admin.keypair

Replace SENDER_ACCOUNT_ID with the ID of the account you want to mint tokens to, ASSET_ID with the ID of the
asset you're minting, and AMOUNT_WITH_DECIMALS with the amount of tokens to mint (including decimal places).

Transfer assets using the Chromia CLI​

The Chromia CLI provides a convenient way to perform asset transfers from the command line. Here's an example of how to
transfer tokens using the CLI:

Verify the account balance:

chr query ft4.get_asset_balance \

  'account_id=SENDER_ACCOUNT_ID' \

  'asset_id=ASSET_ID'

Transfer tokens to the recipient account:

chr tx ft4.transfer \

  RECIPIENT_ACCOUNT_ID \

  ASSET_ID \

  AMOUNT_TO_TRANSFER \

  --ft-auth --await --secret .chromia/SENDER_ACCOUNT_KEYPAIR

Replace RECIPIENT_ACCOUNT_ID with the ID of the account you want to transfer tokens to, ASSET_ID with the ID of
the asset you're transferring, and AMOUNT_TO_TRANSFER with the amount of tokens to transfer (including decimal
places). Additionally, replace .chromia/SENDER_ACCOUNT_KEYPAIR with the path to the sender account's keypair file.

The --ft-auth flag enables FT4 authentication, allowing you to sign the transaction with the sender account's
keypair.

note

Currently, the CLI only supports --ft4-auth. Support for the equivalent of --evm-auth might be added in the future,
but for now, the only way to achieve EVM authentication is by using the client lib.

Transfer assets using the Postchain client library​

Alternatively, you can use the Postchain client library to perform asset transfers programmatically. Here's an example
of how to transfer assets using the client library:

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

Replace YOUR_SENDER_PRIVATE_KEY, SENDER_ACCOUNT_ID, RECIPIENT_ACCOUNT_ID, and ASSET_ID with the appropriate
values for your use case.

This example demonstrates the following steps: register-assets

Connect to the blockchain node using the Postchain client library.

Define the sender's keypair and retrieve the sender's account ID, recipient's account ID, and asset ID.

Create a session for the sender account using the createKeyStoreInteractor and createInMemoryFtKeyStore
functions.

Transfer the assets using the session.account.transfer method, specifying the recipient account ID, asset ID, and
the amount to transfer.

Check the sender's balance after the transfer using the session.account.getBalanceByAssetId method.

By following these steps, you can securely transfer assets between accounts using either the
Chromia CLI or the
Postchain client library, depending on your specific
requirements.

### 3.7 FT4 orchestrator

Canonical: https://docs.chromia.com/build/ft4/client/client-orchestrator

The orchestrator is a utility object that facilitates cross-chain transfers. It can be instantiated, listen to events,
and execute asset transfers between blockchains.

note

Prerequisite environment setup: Before you proceed with cross-chain transfers, ensure you have set up the necessary
environment. You need to clone the FT4 library using the following commands:

git clone https://gitlab.com/chromaway/ft4-lib.git

cd ft4-lib

Run the examples and scripts from the root directory of the cloned repository.

Initial multichain environment setup​

Before proceeding with the examples, initialize your multichain environment. Run the npm run multichain:demo script
from within the root directory of the cloned ft4-lib repository. Upon execution, you'll see an output similar to the
following:

--- SUMMARY ---

Copy the following into your code as needed:

Multichain00 BRID: ...

Multichain02 BRID: ...

User Account ID:   ...

User Private Key:  ...

Test Asset ID: ...

-----------------

Press Ctrl+C to exit.

Make sure to copy the values provided, as they will be required to set up the orchestrator.

warning

Important Configuration Requirement: To ensure the successful validation of ICCF proofs during cross-chain
transfers, it's crucial to include the ICCF module in your chain configuration. Typically specified in chromia.yml,
the configuration should resemble the following:

blockchains:

  <my_blockchain_name>:

    module: <module_name>

    config:

      gtx:

        modules:

          - "net.postchain.d1.iccf.IccfGTXModule"

Omitting this module from the configuration may lead to the non-validation of ICCF proofs, impeding the proper
functioning of cross-chain transfers. Check Import the cross-chain module
for more information.

Performing the transfer​

The code snippet below performs the transfer. The values for targetChainId, recipientId, assetId, and keyPair
should be copied from the npm run multichain:demo script output.

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

Error handling​

caution

Ensure you handle errors appropriately during the cross-chain transfer.

The orchestrator emits errors that are descendants of OrchestratorError from @chromia/ft4.

Registering cross-chain assets​

Cross-chain assets can be manually registered using the registerCrosschainAsset() function.

// example to register an asset

const { registerCrosschainAsset, IClient, SignatureProvider, Asset, BufferId } = require('@chromia/ft4');

const childClient: IClient = session0.client;

const adminSignatureProvider: SignatureProvider = /* ... */;

const asset: Asset = /* ... */;

const parentBrid: BufferId = /* Paste parent blockchain RID */;

await registerCrosschainAsset(childClient, adminSignatureProvider, asset, parentBrid);

To build asset hierarchies, specify the parentBrid and a corresponding childClient during registration. The
childClient can be connected to different blockchains like multichain00 or multichain01. See the
Transfer assets across chains topic for more information.

You can also register cross-chain assets using the CLI:

chr tx --blockchain-rid <child blockchain RID> \

    ft4.admin.register_crosschain_asset TestAsset TST 6 $TEST_ASSET_BRID https://url-to-asset-icon \

    <parent blockchain RID> \

    --await \

    --secret .chromia/ft4-admin.keypair

info

If you want to see an example of a cross-chain transfer between two chains, you can explore the
Simple FT4 cross-chain demo.

### Cookbook overview

Canonical: https://docs.chromia.com/build/cookbook/overview

This Cookbook provides concise, real-world examples and step-by-step recipes for common tasks, from managing accounts
and tokens to deploying dapps and querying data. Whether you're just starting or need quick reference code, you'll find
useful snippets and patterns here. Each section focuses on a specific problem and gives you the recipe to solve it. No
theory, just hands-on examples.

The Cookbook contains ready-made code that you can copy and paste directly into your project with minimal adjustments
(like replacing chain BRIDs). Perfect for when you need working solutions quickly without spending time figuring out the
implementation details.

### Cookbook account-creation

Canonical: https://docs.chromia.com/build/cookbook/account-creation

This section contains practical, hands-on recipes for common Chromia CLI development workflows. Each topic provides
step-by-step guidance, automation scripts, and real-world examples to help you build and deploy dapps efficiently.

Create an account with an open strategy (not a transfer strategy).

Create an account with a transfer fee strategy where part of the transferred amount goes to the chain as a fee.

Create an account with a transfer open strategy where users can claim the full deposit amount.

Create an account with a transfer subscription strategy that requires periodic subscription renewals.

### Cookbook account-creation/open-strategy

Canonical: https://docs.chromia.com/build/cookbook/account-creation/open-strategy

The open strategy allows developers to create a registration process that does not require users to pay any fees. In
this method, users must transfer a specific amount of tokens to a non-existent account, which they must then claim to
activate it using the register_account() operation.

warning

Please note that it is advisable to use the open strategy only during the development phase. Using it outside of this
phase may result in users registering accounts without paying any fees, potentially causing network spam.

Rell side​

Rell open strategy demo

Client side​

JavaScript open strategy demo

To learn more:​

FT4 registration strategies

FT4 account management

Client account registration

### Cookbook account-creation/transfer-fee-strategy

Canonical: https://docs.chromia.com/build/cookbook/account-creation/transfer-fee-strategy

The transfer fee strategy allows users to create an account by transferring assets to a non-existent account. Part
of the transferred assets is collected as a fee to the chain's fee account, and the user can only claim the remaining
assets to their account.

Rell side​

Rell transfer fee strategy demo

Client side​

JavaScript transfer fee strategy demo

To learn more:​

FT4 registration strategies

FT4 account management

FT4 transfer fee strategy

Client account registration

### Cookbook account-creation/transfer-open-strategy

Canonical: https://docs.chromia.com/build/cookbook/account-creation/transfer-open-strategy

The transfer open strategy allows users to create an account by transferring assets to a non-existent account. The
user can claim the entire deposit to their newly created account without any fees deducted.

Rell side​

Rell transfer open strategy demo

Client side​

JavaScript transfer open strategy demo

To learn more:​

FT4 registration strategies

FT4 account management

FT4 transfer open strategy

Client account registration

### Cookbook account-creation/transfer-subscription-strategy

Canonical: https://docs.chromia.com/build/cookbook/account-creation/transfer-subscription-strategy

The transfer subscription strategy allows users to create an account by transferring assets to a non-existent
account. Similar to the fee strategy, part of the transferred assets is collected as a fee, but the user needs to
periodically renew their subscription by paying the subscription fee to keep using their account.

Rell side​

Rell transfer subscription strategy demo

Client side​

JavaScript transfer subscription strategy demo

To learn more:​

FT4 registration strategies

FT4 account management

FT4 transfer subscription strategy

Client account registration

### Cookbook cli

Canonical: https://docs.chromia.com/build/cookbook/cli

This section contains practical, hands-on recipes for common Chromia CLI development workflows. Each topic provides
step-by-step guidance, automation scripts, and real-world examples to help you build and deploy dapps efficiently.

Bootstrap a new project, start the node, and test your first Hello World dapp.

Test data retrieval with practical parameter patterns and automation scripts.

Execute transactions that modify blockchain state with proper signing and parameter handling.

Create effective tests, organize test suites, and automate testing workflows.

### Cookbook cli/create-rell-dapp

Canonical: https://docs.chromia.com/build/cookbook/cli/create-rell-dapp

Creating a new Rell dapp is the first step in your Chromia development journey. This guide shows you how to bootstrap a
project, understand the generated structure, and customize it for your needs.

Prerequisites​

Chromia CLI installed

PostgreSQL database installed and configured

Basic understanding of command line operations

Alternative: You can also create projects using
VS Code with the Chromia extension, which provides a graphical
interface for project creation.

Basic project creation​

Step 1: Create your dapp​

chr create-rell-dapp

This creates a new project called my-rell-dapp with the necessary files:

my-rell-dapp

   |--chromia.yml

   |--src

      |--main.rell

      |--test

         |--arithmetic_test.rell

         |--data_test.rell

Step 2: Explore the project structure​

cd my-rell-dapp

ls -la

You'll see this structure:

my-rell-dapp/

├── chromia.yml          # Project configuration

├── src/

│   ├── main.rell        # Main Rell source code

│   └── test/            # Test directory

│       ├── arithmetic_test.rell

│       └── data_test.rell

Running your dapp​

Step 1: Start the node​

Navigate to your project directory and start a local node:

cd my-rell-dapp

chr node start

This starts a node with your application running on it. All blockchains listed in the blockchains key in chromia.yml
get started.

Step 2: Get the Blockchain RID​

Locate the Blockchain RID in the terminal output. You need it to interact with your dapp. The Blockchain RID is the
hex value following the "Blockchain RID:" tag in the node output.

Step 3: Test your dapp​

You can query your dapp in two ways:

Option 1: Using CURL

curl -X GET 'localhost:7740/query/<BlockchainRID>?type=hello_world'

Option 2: Using Chromia CLI

chr query --blockchain-rid <BlockchainRID> hello_world

Both commands should return:

"Hello World!"

Note: Replace <BlockchainRID> with your actual Blockchain RID from the node output.

Tip: For local development, you can use the simpler --local flag instead of specifying the blockchain RID:

chr query --local hello_world

Project customization patterns​

Advanced project creation​

The chr create-rell-dapp command creates a default project named my-rell-dapp. For custom project names or setups:

# Create default project (named my-rell-dapp)

chr create-rell-dapp

# For custom names, rename after creation

chr create-rell-dapp

mv my-rell-dapp my-custom-dapp

# Or create in a custom directory structure

mkdir my-custom-project

cd my-custom-project

chr create-rell-dapp

# This creates my-rell-dapp inside my-custom-project/

Directory structure customization​

Organize your project for larger applications by extending the generated structure:

my-rell-dapp/

├── chromia.yml

├── src/

│   ├── main.rell           # Main entry point (generated)

│   ├── entities/           # Data models (custom)

│   │   ├── user.rell

│   │   └── message.rell

│   ├── operations/         # Business logic (custom)

│   │   ├── user_ops.rell

│   │   └── message_ops.rell

│   ├── queries/           # Data queries (custom)

│   │   ├── user_queries.rell

│   │   └── message_queries.rell

│   └── test/              # Test directory (generated)

│       ├── arithmetic_test.rell  # Generated test

│       ├── data_test.rell        # Generated test

│       ├── user_test.rell        # Custom test

│       └── message_test.rell     # Custom test

Configuration customization​

Customize your chromia.yml for development:

blockchains:

  hello:

    module: main

# Development settings (optional)

database:

  schema_version: 1

# Test configuration (optional)

test:

  timeout: 30000

  parallel: true

# Build settings (optional)

build:

  output_dir: build

  optimize: true

Next steps​

After creating your dapp:

Test your Hello World dapp - Follow the "Running your dapp" section above

Run queries - Advanced query patterns and testing

Run operations - Test state modifications with chr tx

Run tests - Set up comprehensive testing workflows

### Cookbook cli/run-operations

Canonical: https://docs.chromia.com/build/cookbook/cli/run-operations

Operations are transactions that modify blockchain state by creating, updating, or deleting data. This guide shows you
how to use operations during development and testing.

Prerequisites​

Chromia CLI installed

Rell dapp project created with local node running OR access to a deployed dapp

Key pair generated for signing transactions

Basic operation workflow​

Step 1: Verify your node is running​

# Check if local node is responding

curl http://localhost:7740

Step 2: Generate a key pair (if you haven't already)​

# Generate a new key pair

chr keygen

# This creates .chromia/chromia_key and .chromia/chromia_key.pubkey

# The default key ID is 'chromia_key'

Step 3: Run a simple operation​

# Test a basic operation (--local is the default)

chr tx --secret .chromia/chromia_key create_book "ISBN001" "Chromia 101" "Alice Johnson"

Expected output:

Transaction submitted successfully

Transaction hash: 0x1234567890abcdef...

Note: You can also use the Blockchain RID explicitly:

chr tx --blockchain-rid <BlockchainRID> --secret .chromia/chromia_key create_book "ISBN001" "Chromia 101" "Alice Johnson"

Replace <BlockchainRID> with the actual Blockchain RID from your node output. You can find this in the node startup
output as: Blockchain RID: FC17B67D66F6F35A5D8B75ED3F83AE222FB8C8FCA241624F06285150F10C6BAC

Operation methods overview​

Local operations (development)​

# Default local operation

chr tx --secret .chromia/chromia_key create_house "123 Main St" 4 200

# Explicit blockchain RID (equivalent to --local for development)

chr tx --blockchain-rid <BlockchainRID> --secret .chromia/chromia_key create_house "123 Main St" 4 200

# Wait for transaction confirmation

chr tx --secret .chromia/chromia_key --await create_house "123 Main St" 4 200

Remote operations (deployed dapps)​

# Operation on deployed dapp using network and blockchain name

chr tx --network testnet --blockchain my_rell_dapp --secret .chromia/chromia_key create_company "TechCorp" "456 Business Ave"

Operation parameters​

When you create operations that accept parameters, use this syntax based on the
official CLI documentation:

Basic parameter types​

# String parameters (quoting optional for simple strings)

chr tx --secret .chromia/chromia_key create_book "ISBN001" "Chromia 101" "Alice Johnson"

# Strings with spaces (require quotes)

chr tx --secret .chromia/chromia_key create_company "TechCorp Inc"

# Integer parameters

chr tx --secret .chromia/chromia_key create_house "123 Main St" 4 200

# Multiple parameters example

chr tx --secret .chromia/chromia_key create_library "Central Library" "Downtown" 1000

Advanced parameter types​

# Byte array parameters (public keys, hashes, etc.)

chr tx --secret .chromia/chromia_key transfer_tokens 'x"0373599a61cc6b3bc02a78c34313e1737ae9cfd56b9bb24360b437d469efdf3b15"' 'x"0373599a61cc6b3bc02a78c34313e1737ae9cfd56b9bb24360b437d469efdf3b15"' 100

# Dictionary/map parameters

chr tx --secret .chromia/chromia_key update_user_metadata '["key1": "value1", "key2": "value2"]'

# Array parameters

chr tx --secret .chromia/chromia_key batch_create_users '["Alice", "Bob", "Charlie"]'

Transaction options​

Awaiting confirmation​

# Wait for transaction to be confirmed (recommended for important operations)

chr tx --secret .chromia/chromia_key --await create_book "ISBN001" "Chromia 101" "Alice Johnson"

Making transactions unique​

# Add a NOP to make transaction hash unique (useful for repeated operations)

chr tx --secret .chromia/chromia_key --nop create_book "ISBN001" "Chromia 101" "Alice Johnson"

Using different key files​

# Use a specific key file

chr tx --secret /path/to/my_key create_book "ISBN001" "Chromia 101" "Alice Johnson"

# Use key by ID (if configured in .chromia/config)

chr tx --secret my_custom_key create_book "ISBN001" "Chromia 101" "Alice Johnson"

Testing against deployed dapps​

Testnet operations​

# Basic operation on deployed dapp

chr tx --network testnet --blockchain my_rell_dapp --secret .chromia/chromia_key create_company "TechCorp" "456 Business Ave"

# Operation with parameters on deployed dapp

chr tx --network testnet --blockchain my_rell_dapp --secret .chromia/chromia_key create_house "123 Main St" 4 200

Common operation patterns​

Book management​

# Create a new book

chr tx --secret .chromia/chromia_key create_book "ISBN001" "Chromia 101" "Alice Johnson"

# Update book information

chr tx --secret .chromia/chromia_key update_book "ISBN001" "Chromia 101: Updated Edition"

# Delete book record

chr tx --secret .chromia/chromia_key delete_book "ISBN001"

House management​

# Create a new house

chr tx --secret .chromia/chromia_key create_house "123 Main St" 4 200

# Update house details

chr tx --secret .chromia/chromia_key update_house "123 Main St" 5

# Register house ownership

chr tx --secret .chromia/chromia_key register_house_owner "123 Main St" "Bob Smith"

Company management​

# Create a new company

chr tx --secret .chromia/chromia_key create_company "TechCorp" "456 Business Ave"

# Update company information

chr tx --secret .chromia/chromia_key update_company "TechCorp" "789 Innovation Blvd"

# Add employee to company

chr tx --secret .chromia/chromia_key add_employee "TechCorp" "Carol Davis"

Library management​

# Create a new library

chr tx --secret .chromia/chromia_key create_library "Central Library" "Downtown"

# Add book to library

chr tx --secret .chromia/chromia_key add_book_to_library "Central Library" "ISBN001" 3

# Check out book

chr tx --secret .chromia/chromia_key checkout_book "ISBN001" "David Wilson"

Error handling​

Common error scenarios​

# Operation fails due to insufficient permissions

chr tx --secret .chromia/chromia_key admin_only_operation

# Error: Operation requires admin privileges

# Operation fails due to invalid parameters

chr tx --secret .chromia/chromia_key create_book "" "Chromia 101" "Alice Johnson"

# Error: ISBN cannot be empty

# Operation fails due to duplicate key

chr tx --secret .chromia/chromia_key create_book "ISBN001" "Chromia 101" "Alice Johnson"

chr tx --secret .chromia/chromia_key create_book "ISBN001" "Another Book" "Bob Smith"

# Error: Book with ISBN 'ISBN001' already exists

Debugging operations​

# Use --await to see detailed error messages

chr tx --secret .chromia/chromia_key --await problematic_operation

# Check transaction status

chr query get_transaction_status "string:tx_hash->0x1234567890abcdef..."

Next steps​

After mastering operations:

Run queries to verify your operations worked correctly

Run tests to automate operation testing

Deploy your dapp to make operations available publicly

Use multi-signature transactions for advanced security requirements

### Cookbook cli/run-queries

Canonical: https://docs.chromia.com/build/cookbook/cli/run-queries

Queries are read-only operations that retrieve data from your blockchain without modifying state. This guide shows you
how to use queries during development and testing.

Prerequisites​

Chromia CLI installed

Rell dapp project created with local node running OR access to a deployed dapp

Basic query workflow​

Step 1: Verify your node is running​

# Check if local node is responding

curl http://localhost:7740

Step 2: Run a simple query​

# Test the default hello_world query (--local is the default)

chr query hello_world

Expected output:

"Hello World!"

Note: You can also use the Blockchain RID explicitly:

chr query --blockchain-rid <BlockchainRID> hello_world

Replace <BlockchainRID> with the actual Blockchain RID from your node output. You can find this in the node startup
output as: Blockchain RID: FC17B67D66F6F35A5D8B75ED3F83AE222FB8C8FCA241624F06285150F10C6BAC

Query methods overview​

Local queries (development)​

# Default local query

chr query hello_world

# Explicit blockchain RID (equivalent to --local for development)

chr query --blockchain-rid <BlockchainRID> hello_world

Remote queries (deployed dapps)​

# Query deployed dapp using network and blockchain name

chr query --network testnet --blockchain my_rell_dapp hello_world

Query parameters​

When you create queries that accept parameters, use this syntax based on the
official CLI documentation:

Basic parameter types​

# String parameters (quoting optional for simple strings)

chr query my_query arg1=Alice arg2=hello

# Strings with spaces (require quotes)

chr query my_query 'arg1="My Neighbor"' arg2=hello

# Integer parameters

chr query my_query arg1=123 arg2=456

# Multiple parameters example from official docs

chr query hello_world foo=17 bar=hello 'baz="hello world"'

Advanced parameter types​

# Byte array parameters

chr query my_query 'arg=x"AB12"'

# Using -- to avoid additional quotes

chr query my_query -- arg1=foo arg2=x"AB12"

# Dictionary/map parameters

chr query my_query 'arg=["key": 12]'

# Array parameters

chr query my_query 'arg=[foo, 123]'

Testing against deployed dapps​

Testnet queries​

# Basic query on deployed dapp

chr query --network testnet --blockchain my_rell_dapp hello_world

# Query with parameters on deployed dapp

chr query --network testnet --blockchain my_rell_dapp my_query foo=17 bar=hello

Next steps​

After mastering queries:

Run operations to modify blockchain state

Run tests to verify your queries work correctly

Deploy your dapp to make queries available publicly

### Cookbook cli/run-tests

Canonical: https://docs.chromia.com/build/cookbook/cli/run-tests

Testing is crucial for ensuring your Rell dapp works correctly before deployment. This guide shows you how to write and
run tests using the official Rell testing framework.

Prerequisites​

Chromia CLI installed

Rell dapp project created

Basic testing workflow​

Step 1: Verify your test files exist​

Test files are automatically discovered in the src/test/ directory. Any .rell file in this directory will be treated
as a test module:

# Check test directory structure

ls -la src/test/

# Should show:

# arithmetic_test.rell

# data_test.rell

The test runner automatically discovers and executes all .rell files in the src/test/ directory. Functions starting
with test_ within these files are treated as test cases.

Step 2: Run all tests​

chr test

Expected output:

TEST RESULTS:

arithmetic_test:test_addition OK (0.002s)

arithmetic_test:test_subtraction OK (0.001s)

data_test:test_hello_world OK (0.003s)

SUMMARY: 0 FAILED / 3 PASSED / 3 TOTAL (0.006s)

Understanding Rell tests​

Test module basics​

Tests use the @test annotation to define a test module:

@test module;

function test_hello_world() {

    val result = hello_world();

    assert_equals(result, "Hello World!");

}

Functions starting with test_ are automatically executed as tests.

Assertion functions​

Use the official assertion functions provided by Rell:

@test module;

function test_basic_assertions() {

    // Test equality

    assert_equals(2 + 2, 4);

    // Test boolean conditions

    assert_true(5 > 3);

    assert_false(2 > 5);

    // Test comparisons

    assert_lt(3, 5); // 3 less than 5

    assert_gt(5, 3); // 5 greater than 3

    // Test that a function fails as expected

    assert_fails(some_failing_function);

}

Running specific tests​

# Run tests from specific module

chr test --modules arithmetic_test

# Run tests from multiple modules

chr test --modules arithmetic_test data_test

# See detailed test output

chr test --verbose

SQL logging for debugging​

Enable SQL query logging to understand how your Rell code translates to database operations:

# Log all SQL queries (both user and system)

chr test --sql-log

# Log only user queries (your dapp's queries)

chr test --sql-log=user

# Log only system queries (internal Chromia queries)

chr test --sql-log=system

# Combine with other options

chr test --sql-log --modules arithmetic_test --verbose

For more information on analyzing SQL statements, see Rell dapp code optimization.

Generating test data with seeder​

The seeder command helps you generate mock data for a local database. This is an early-stage feature and may be subject
to change.

Initialize seeder configuration​

# Generate default seeder configuration for all blockchains

chr seeder init

# Generate configuration for specific blockchain

chr seeder init --blockchain my_blockchain

This creates a .chromia/seeder directory with YAML configuration files for each entity in your dapp.

Generate the seeder module​

# Generate seeder modules for all blockchains

chr seeder generate

# Generate for specific blockchain

chr seeder generate --blockchain my_blockchain

This creates src/seeder/seed_[blockchain-name].rell files containing the data generation logic.

For detailed information on using the seeder, see Seeder command documentation and
Using the seeder.

Test file organization​

Default test structure:

src/

├── main.rell

└── test/

    ├── arithmetic_test.rell    # Mathematical operation tests

    └── data_test.rell         # Data structure tests

The test runner automatically discovers and executes all .rell files in the src/test/ directory. Functions starting
with test_ within these files are treated as test cases.

Debugging test failures​

Reading test output​

When tests fail, the output shows details:

TEST RESULTS:

my_test_module:test_foo OK (0.005s)

my_test_module:test_bar FAILED (0.001s)

SUMMARY: 1 FAILED / 1 PASSED / 2 TOTAL (0.007s)

***** FAILED *****

Adding debug output​

@test module;

function test_with_debug() {

    print("Debug: Starting test");

    val result = my_function();

    print("Debug: Result is " + result);

    assert_equals(result, expected_value);

}

Next steps​

After mastering basic testing:

Deploy your tested dapp with confidence

Learn more about advanced testing patterns in the Rell testing documentation

Explore the Chromia learning platform for
integration testing

Run operations to test state modifications

Run queries to verify your test data and operations

### Cookbook data-inspection

Canonical: https://docs.chromia.com/build/cookbook/data-inspection

Explore simple, practical recipes for reading on‑chain data (blocks, transactions, events).

Tx RID → block RID → block info using Postchain.

Fetch and decode transaction data using Postchain and FT4.

Retrieve the status of a transaction using its RID.

Retrieve paginated transfer history for an account with detailed transaction info.

Retrieve all accounts associated with a signer's public key or EVM address.

Retrieve account information from the blockchain using its unique ID.

### Cookbook data-inspection/get-account-by-id

Canonical: https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-id

This recipe demonstrates how to retrieve an account from the Chromia blockchain by its ID using the FT4 library.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Get account by ID demo

Learn more​

FT4 client documentation — covers account management and FT4 operations.

Account management — detailed guide on FT4 account operations.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook data-inspection/get-account-by-signer

Canonical: https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-signer

This recipe demonstrates how to retrieve all accounts associated with a specific signer's public key or EVM address
using the FT4 library.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Get accounts by signer demo

Learn more​

FT4 client documentation — covers account management and FT4 operations.

Account management — detailed guide on FT4 account operations.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook data-inspection/get-account-transfer-history

Canonical: https://docs.chromia.com/build/cookbook/data-inspection/get-account-transfer-history

This recipe demonstrates how to retrieve the transfer history for a specific account on the Chromia blockchain using the
FT4 library, including pagination and transfer details.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Get account transfer history demo

Learn more​

FT4 client documentation — covers account management and FT4 operations.

Account management — detailed guide on FT4 account operations.

Asset management — understanding FT4 transfers and assets.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook data-inspection/get-block-data

Canonical: https://docs.chromia.com/build/cookbook/data-inspection/get-block-data

To fetch and decode block data, first extract the block RID from a given transaction RID, then use Postchain
to retrieve the full block information.

JS/TS client

Prerequisites​

Node.js

Postchain client: npm install postchain-client

Demo script​

Fetch and decode block data demo

Learn more​

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

Chromia Explorer — find RIDs for transactions, blocks, chains, etc.

### Cookbook data-inspection/get-transaction-data

Canonical: https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-data

This recipe demonstrates how to get and decode transaction information from the Chromia blockchain.

Client side​

JavaScript get and decode transaction

To learn more:​

Generic Transaction Protocol (GTX)

Transactions in explorer

Transactions in the client

### Cookbook data-inspection/get-transaction-status

Canonical: https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-status

This recipe demonstrates how to retrieve the status of a transaction on the Chromia blockchain using its RID (Resource
Identifier).

Transaction statuses​

When retrieving transaction status, you can receive one of the following statuses:

Unknown - Transaction status is unknown

Waiting - Transaction is waiting to be included in the block

Confirmed - Transaction is included in the block

Rejected - Transaction has been rejected (includes rejection reason and timestamp)

For detailed information about the transaction status API endpoint, see the
REST API documentation.

JS/TS client

Prerequisites​

Node.js

Postchain client: npm install postchain-client

Demo script​

Get transaction status demo

Learn more​

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

Chromia Explorer — find RIDs for transactions, blocks, chains, etc.

Transactions in explorer — understanding transaction data in the
explorer.

### Cookbook query-creation

Canonical: https://docs.chromia.com/build/cookbook/query-creation

Explore simple, practical recipes for querying blockchain data and inspecting account states.

Learn the fundamental pattern for performing parameterized queries against Chromia blockchains.

Query whether an FT4 account requires memo for transfers.

Fetch FT4 account balances for specific assets using EVM key pairs.

Implement cursor-based pagination using FT4 utilities for efficient data retrieval.

Implement cursor-based pagination with custom utilities for full control over pagination logic.

### Cookbook query-creation/check-account-memo-requirement

Canonical: https://docs.chromia.com/build/cookbook/query-creation/check-account-memo-requirement

This recipe demonstrates how to query whether a given FT4 account requires a memo for transfers on the Chromia
blockchain. This is useful before sending transfers to ensure compliance with the recipient's requirements.

JS/TS client

Prerequisites​

Node.js

Postchain client: npm install postchain-client

Demo script​

Check account memo requirement demo

Key Features​

This recipe includes examples for:

Account memo queries - Using the does_account_require_memo query operation

Pre-transfer validation - Checking requirements before initiating transfers

Account compliance - Ensuring transfers meet recipient account settings

Simple query execution - Demonstrating basic blockchain query patterns

Query Operation​

The recipe uses the does_account_require_memo query which:

Input: Account ID (Buffer/hex format)

Output: Boolean indicating whether memo is required

Purpose: Validates transfer requirements before execution

Use Cases​

This query is particularly useful for:

Payment processors - Ensuring all transfers include required memos

Exchange integrations - Validating withdrawal requirements

Wallet applications - Warning users about memo requirements

Automated systems - Pre-validating transfer compliance

Learn more​

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

FT4 client documentation — covers account management and FT4 operations.

Asset management — detailed guide on FT4 transfers and memo requirements.

### Cookbook query-creation/get-account-balance

Canonical: https://docs.chromia.com/build/cookbook/query-creation/get-account-balance

This recipe demonstrates how to fetch an FT4 account balance using an EVM key pair. This is essential for checking asset
balances before performing transfers or for displaying account information in applications.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Get account balance demo

Key Features​

This recipe includes examples for:

FT4 balance queries - Using the getBalanceByAccountId function for specific assets

EVM key integration - Working with EVM key pairs to resolve FT4 accounts

Asset-specific balances - Querying balances for particular assets (e.g., CHR)

Account resolution - Converting EVM keys to FT4 account identifiers

Balance Query Process​

The recipe follows this workflow:

Create keystore - Initialize an EVM keystore with your private/public key pair

Resolve accounts - Use the keystore interactor to find associated FT4 accounts

Create connection - Establish an FT4 connection to the blockchain

Query balance - Call getBalanceByAccountId with account ID and asset ID

Common Use Cases​

This pattern is useful for:

Wallet applications - Displaying current asset balances to users

Payment validation - Checking sufficient funds before transfers

Portfolio tracking - Monitoring asset holdings across accounts

Exchange integrations - Validating account balances for withdrawals

Requirements​

Registered EVM key - Your EVM key must be registered on the target blockchain

FT4 account - An associated FT4 account must exist for the EVM key

Asset ID - The hex-encoded asset ID you want to query (e.g., CHR asset ID)

Learn more​

FT4 client documentation — covers account management and FT4 operations.

Asset management — detailed guide on FT4 assets and balance queries.

Account management — understanding FT4 accounts and key management.

### Cookbook query-creation/make-query

Canonical: https://docs.chromia.com/build/cookbook/query-creation/make-query

This recipe demonstrates how to perform queries with parameters using the postchain-client against a Chromia
blockchain. This is the fundamental pattern for retrieving data from blockchain queries.

JS/TS client

Prerequisites​

Node.js

Postchain client: npm install postchain-client

Demo script​

Make query demo

Key Features​

This recipe includes examples for:

Parameterized queries - Using the client.query() method with custom parameters

Buffer handling - Working with hex-encoded Buffer parameters for blockchain data

Query configuration - Adapting query names and parameters for different blockchains

Result handling - Processing and displaying query results

Common Query Examples​

This pattern works for various blockchain queries:

Account balances - get_balance with account ID parameter

Transaction history - Query transactions for specific accounts

Asset information - Retrieve details about blockchain assets

Custom dapp queries - Any query defined in your dapp's Rell code

Parameter Types​

Queries can accept various parameter types:

Buffer/Hex data - Account IDs, transaction IDs, asset IDs

Strings - Text-based identifiers or search terms

Numbers - Amounts, timestamps, pagination limits

Objects - Complex structured data for advanced queries

Learn more​

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

Rell language — understanding how to create custom queries in your dapp.

### Cookbook query-creation/pagination-with-ft4

Canonical: https://docs.chromia.com/build/cookbook/query-creation/pagination-with-ft4

This recipe demonstrates how to implement cursor-based pagination using FT4 utilities in Rell and consume it from a
client. This approach is ideal for projects already using FT4, as it leverages pagination helpers from lib.ft4.utils
for consistent cursor handling.

JS/TS client

Prerequisites​

Chromia CLI

Docker (for local blockchain)

Node.js

FT4 client

Postchain client

Demo script​

Pagination with FT4 demo

Key Features​

This recipe includes examples for:

Cursor-based pagination - Using FT4's cursor system for efficient data traversal

Rell implementation - Server-side pagination logic using lib.ft4.utils

Client consumption - JavaScript client that handles pagination requests

Consistent pagination - Following FT4 patterns for standardized pagination across your dapp

Architecture Overview​

The recipe includes both server and client components:
Rell Module (Server-side)​

Entity: user with name field for demonstration

Query: get_users_paginated with page_size and page_cursor parameters returning FT4-style result with data
and next_cursor

Operation: add_user for seeding test data

JavaScript Client​

First page request - Fetches initial data with page_cursor set to null

Subsequent pages - Uses next_cursor from previous response

Page size control - Configurable page size per request

FT4 Pagination Pattern​

The Rell implementation uses FT4 utilities for consistent pagination:

Uses ft4_utils.before_rowid to handle cursor positioning

Employs ft4_utils.pagination_result for data formatting

Utilizes ft4_utils.fetch_data_size for proper page sizing

Returns results via ft4_utils.make_page for standardized response format

Client Usage Pattern​

The JavaScript client demonstrates the standard pagination flow:

Initial request sets page_cursor to null for the first page

Subsequent requests use the next_cursor value from previous responses

Page size can be adjusted per request for flexible data loading

Use Cases​

This pagination approach is ideal for:

Large datasets - Efficiently browsing through extensive records

User interfaces - Implementing paginated lists and tables

API endpoints - Providing paginated data to frontend applications

Performance optimization - Reducing memory usage and response times

Learn more​

FT4 client documentation — covers FT4 operations and utilities.

Rell language — understanding how to create queries and operations in Rell.

JavaScript/TypeScript client — covers client setup and
usage.

### Cookbook query-creation/pagination

Canonical: https://docs.chromia.com/build/cookbook/query-creation/pagination

This recipe demonstrates how to implement cursor-based pagination using custom pagination utilities in Rell and consume
it from a client. This approach provides full control over pagination logic and is ideal for projects that need
customized pagination behavior.

JS/TS client

Prerequisites​

Chromia CLI

Docker (for local blockchain)

Node.js

FT4 client

Postchain client

Demo script​

Custom pagination demo

Key Features​

This recipe includes examples for:

Custom pagination utilities - Custom-built pagination logic with full control over cursor behavior

Offset-based pagination - Uses offset and limit for data retrieval with page tracking

Custom cursor encoding - Base64-encoded cursors with page number tracking

Flexible page sizing - Configurable page sizes with maximum limits

Architecture Overview​

The recipe includes both server and client components:
Rell Module (Server-side)​

Entity: user with name field for demonstration

Query: get_users_paginated with page_cursor and data_size parameters returning custom paged_result

Operation: add_user for seeding test data

Custom utilities: paginator.rell module with pagination helper functions

JavaScript Client​

First page request - Fetches initial data with page_cursor set to null

Subsequent pages - Uses next_cursor from previous response

Page size control - Uses data_size parameter for configurable page sizes

Custom Pagination Pattern​

The Rell implementation uses custom pagination utilities:
Pagination Structures​

page_cursor: Tracks current page position with after_rowid and after_page

pagination_result: Contains data and rowid for each result item

paged_result: Standard response format with data and next_cursor

Cursor Management​

encode_cursor: Converts cursor to base64-encoded string

decode_cursor: Parses cursor from client requests

Page tracking: Maintains current page number for offset calculations

Query Processing​

fetch_data_size: Fetches one extra item to determine if more pages exist

query_offset_for_page_size: Calculates offset based on current page

make_page: Constructs response with appropriate next cursor

Client Usage Pattern​

The JavaScript client demonstrates the pagination flow:

Initial request sets page_cursor to null and specifies data_size

Each response includes data array and optional next_cursor

Subsequent requests use the next_cursor to fetch following pages

Pagination continues until next_cursor is null

Customization Benefits​

This custom approach offers several advantages:

Full control - Complete control over pagination logic and cursor format

Flexible implementation - Easy to modify pagination behavior for specific needs

Performance optimization - Can implement custom indexing and query optimizations

Custom metadata - Can include additional pagination metadata in responses

Use Cases​

This custom pagination approach is ideal for:

Custom sorting requirements - Complex sorting that doesn't fit standard patterns

Specialized cursor logic - Need for custom cursor encoding or metadata

Performance optimization - Projects requiring specific query optimization strategies

Legacy integration - Matching existing API pagination formats

Learn more​

Rell language — understanding how to create custom queries and utilities in Rell.

JavaScript/TypeScript client — covers client setup and
usage.

### Cookbook transaction-creation

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation

Explore simple, practical recipes for creating transactions.

Use the signAndSendUniqueTransaction helper for easy transaction creation and sending.

Create and execute asset transfers using both EVM and FT4 key pairs.

Configure memo requirements for your account transfers with online and offline methods.

Create asset transfers that include memo data for enhanced tracking and references.

Build transactions that are only valid within specific time windows for enhanced security.

Transfer assets between different Chromia blockchains using FT4 crosschain functionality.

Register assets from one blockchain to be used on another blockchain using FT4 admin operations.

Create new local assets native to a blockchain using FT4 admin operations.

### Cookbook transaction-creation/call-operation-with-ft4-auth

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/call-operation-with-ft4-auth

This recipe demonstrates how to call dapp operations that require FT4 authentication using a Web3 provider (e.g.,
MetaMask). This is essential for interacting with custom dapp operations that need authenticated user accounts.

JS/TS client

Prerequisites​

Node.js

Web3 provider (e.g., MetaMask browser extension)

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Call operation with FT4 auth demo

Key Features​

This recipe includes examples for:

Web3 integration - Using injected Web3 providers like MetaMask for key management

FT4 authentication - Creating authenticated sessions for operation calls

Custom operations - Calling arbitrary dapp operations that require user authentication

Session management - Handling FT4 session creation and operation execution

Authentication Flow​

The recipe demonstrates this flow:

Connect to Web3 provider - Uses createWeb3ProviderEvmKeyStore to integrate with MetaMask

Get FT4 accounts - Retrieves accounts associated with the Web3 provider

Create session - Establishes an authenticated FT4 session

Call operations - Executes custom dapp operations with proper authentication

Learn more​

FT4 client documentation — covers account management and FT4 operations.

Account management — understanding FT4 accounts and authentication.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook transaction-creation/crosschain-transfer

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/crosschain-transfer

This recipe demonstrates how to perform crosschain transfers between different Chromia blockchains using EVM keys with
FT4. This enables asset transfers across the multichain Chromia ecosystem.

JS/TS client

Prerequisites​

Node.js

FT4 client

Postchain client

Account registration on both source and target chains

Crosschain asset must be registered on the target chain

Demo dapp​

Crosschain transfer demo

Key Features​

This recipe includes examples for:

Multi-chain connectivity - Connecting to multiple Chromia blockchains through directory service

EVM key integration - Using EVM key pairs for crosschain authentication

Asset transfer orchestration - Managing transfer state across multiple blockchains

Event monitoring - Tracking transfer progress through signed, init, and hop events

Architecture Overview​

The recipe demonstrates crosschain transfer using FT4:
Transfer Components​

Source chain - The blockchain where assets originate and the transfer begins

Target chain - The destination blockchain where assets will be received

Directory service - Provides connectivity and routing across multiple chains

FT4 orchestrator - Manages the transfer process and state transitions

JavaScript Client​

Multi-chain session - Creates authenticated sessions across different blockchains

Transfer initiation - Uses crosschainTransfer function to start the process

Progress tracking - Monitors transfer events for completion confirmation

Transfer Process​

The crosschain transfer follows this workflow:

Account setup - Configure EVM key pair and account credentials

Chain configuration - Define source and target blockchain identifiers

Asset specification - Specify the asset to transfer and amount

Transfer initiation - Execute the crosschainTransfer operation

Event monitoring - Track transfer progress through event handlers

Transfer completion - Confirm successful transfer on target chain

Required Information​

To perform a crosschain transfer, you need:

Source chain RID - Blockchain identifier where assets currently exist

Target chain RID - Blockchain identifier for the destination

Account credentials - EVM private/public key pair and account ID

Asset details - Asset ID and amount to transfer

Directory service - URL for multi-chain connectivity

Transfer Configuration​

When configuring a crosschain transfer, specify:

Chain identifiers - Source and target blockchain RIDs

Account information - Your account ID on both chains

Asset parameters - Asset ID and transfer amount

Network settings - Directory node URL for multi-chain access

Event Monitoring​

The transfer process provides real-time feedback through events:

Signed event - Transaction has been signed and submitted

Init event - Transfer has been initialized on the source chain

Hop event - Transfer is progressing through intermediate chains

Completion - Transfer has successfully reached the target chain

Prerequisites for Success​

Before attempting crosschain transfers:

Asset registration - The asset must be registered on the target chain

Account existence - Your account must exist on both source and target chains

Sufficient balance - You must have enough assets to cover the transfer and fees

Network connectivity - Stable connection to directory services

Use Cases​

Crosschain transfers are essential for:

Multi-chain applications - Apps operating across multiple Chromia blockchains

Asset arbitrage - Moving assets between chains for trading opportunities

Ecosystem participation - Accessing services on different Chromia chains

Portfolio management - Distributing assets across multiple chains

Cross-chain DeFi - Participating in DeFi protocols on different chains

Learn more​

FT4 client documentation — covers FT4 operations and crosschain functionality.

Cross-chain transfers — detailed guide on crosschain transfer
mechanics.

Cross-chain assets — understanding crosschain asset management.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook transaction-creation/enable-disable-memo

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/enable-disable-memo

This recipe demonstrates how to enable or disable the memo requirement for transfers on your account. When enabled, all
transfers to your account must include a memo field.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Enable/disable memo demo

Key Features​

This recipe includes examples for:

Online method - Using FT4 client sessions for simple account configuration

Offline method - Creating and signing GTX transactions locally for advanced control

Account security - Managing memo requirements for enhanced transfer tracking

Session management - Using authenticated sessions to modify account settings

Learn more​

FT4 client documentation — covers account management and FT4 operations.

Account management — understanding FT4 accounts and authentication.

Asset management — detailed guide on FT4 transfers and memo requirements.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook transaction-creation/make-transfer

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/make-transfer

This recipe demonstrates how to create and execute asset transfers on the Chromia blockchain using both EVM and FT4 key
pairs.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Make transfer demo

Key Features​

This recipe includes examples for:

FT4 key transfers - Using native Chromia key pairs

EVM key transfers - Using Ethereum-compatible key pairs

Asset management - Transferring CHR and other FT4 assets

Account sessions - Managing account authentication and sessions

Learn more​

FT4 client documentation — covers account management and FT4 operations.

Asset management — detailed guide on FT4 transfers and assets.

Account management — understanding FT4 accounts and authentication.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook transaction-creation/register-asset

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/register-asset

This recipe demonstrates how to register local (non-crosschain) assets using FT4 admin operations. This is essential for
creating new assets that are native to a specific blockchain within the Chromia ecosystem.

JS/TS client

Prerequisites​

Chromia CLI

Docker (for local blockchain)

Node.js

FT4 client

Postchain client

Demo script​

Register asset demo

Key Features​

This recipe includes examples for:

Local asset creation - Registering assets native to a specific blockchain

Asset metadata - Configuring asset name, symbol, decimals, and icon URL

Admin operations - Using FT4 admin functions for asset registration

Asset verification - Confirming successful registration through queries

Architecture Overview​

The recipe includes both server and client components:
Rell Module (Server-side)​

FT4 integration - Uses FT4 library for asset management

Admin operations - Implements admin-level asset registration functions

Test coverage - Unit tests demonstrating registration process and verification

JavaScript Client​

Admin authentication - Uses admin signature provider for authorization

Asset configuration - Defines asset properties (name, symbol, decimals, icon URL)

Registration execution - Calls the registerAsset function

Registration Process​

The local asset registration follows this workflow:

Admin setup - Configure admin private/public key pair for authorization

Asset metadata - Define asset name, symbol, decimal places, and icon URL

Registration call - Execute the registerAsset operation

Verification - Confirm successful registration through asset queries

Asset availability - The asset becomes available for use on the blockchain

Required Information​

To register a local asset, you need:

Admin credentials - Private and public key pair with admin privileges

Asset name - A descriptive name for the asset (e.g., "TestAsset")

Asset symbol - A short symbol identifier (e.g., "TA")

Decimal places - Number of decimal places for the asset (typically 6-18)

Icon URL - Optional URL pointing to the asset's icon image

Administrative Requirements​

This operation requires administrative privileges:

Admin keypair - Must have admin rights on the target blockchain

Proper authorization - Admin signature provider must be correctly configured

Network access - Connection to the appropriate network nodes

Unique asset - Asset symbol must be unique on the blockchain

Asset Properties​

When registering an asset, you configure:

Name - Full descriptive name of the asset

Symbol - Short identifier used in transactions and displays

Decimals - Precision level for fractional amounts

Icon URL - Visual representation for user interfaces

Use Cases​

Local asset registration is essential for:

Native tokens - Creating blockchain-specific utility tokens

Governance tokens - Tokens for decentralized governance systems

Reward systems - Custom tokens for incentive mechanisms

Application tokens - Tokens specific to particular dapps

Stablecoins - Blockchain-native stable value tokens

Learn more​

FT4 client documentation — covers FT4 operations and admin functions.

Asset management — detailed guide on FT4 assets and local asset functionality.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook transaction-creation/register-crosschain-asset

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/register-crosschain-asset

This recipe demonstrates how to register crosschain assets using FT4 admin operations. This is essential for enabling
assets from one blockchain to be used on another blockchain within the Chromia ecosystem.

JS/TS client

Prerequisites​

Chromia CLI

Docker (for local blockchain)

Node.js

FT4 client

Postchain client

Demo script​

Register crosschain asset demo

Key Features​

This recipe includes examples for:

Admin operations - Using FT4 admin functions to register crosschain assets

Crosschain asset management - Managing assets across multiple blockchains

Asset registration - Establishing the connection between origin and target blockchains

Signature provider setup - Configuring admin signature providers for authorization

Architecture Overview​

The recipe includes both server and client components:
Rell Module (Server-side)​

FT4 integration - Uses FT4 library for crosschain asset management

Admin operations - Implements admin-level asset registration functions

Test coverage - Unit tests demonstrating registration process

JavaScript Client​

Admin authentication - Uses admin signature provider for authorization

Asset configuration - Configures asset ID and origin blockchain RID

Registration execution - Calls the registerCrosschainAsset function

Registration Process​

The crosschain asset registration follows this workflow:

Admin setup - Configure admin private/public key pair for authorization

Asset identification - Specify the asset ID to be registered

Origin blockchain - Define the origin blockchain RID where the asset exists

Registration call - Execute the registerCrosschainAsset operation

Verification - Confirm successful registration through queries

Required Information​

To register a crosschain asset, you need:

Admin credentials - Private and public key pair with admin privileges

Asset ID - The unique identifier of the asset to register

Origin blockchain RID - The RID of the blockchain where the asset originates

Target blockchain - The blockchain where you want to register the asset

Administrative Requirements​

This operation requires administrative privileges:

Admin keypair - Must have admin rights on the target blockchain

Proper authorization - Admin signature provider must be correctly configured

Network access - Connection to the appropriate network nodes

Asset validation - The asset must exist on the origin blockchain

Use Cases​

Crosschain asset registration is essential for:

Multi-chain applications - Apps that operate across multiple Chromia blockchains

Asset bridges - Enabling asset transfers between different chains

Ecosystem expansion - Growing the available assets on new blockchains

Interoperability - Creating connections between different blockchain environments

Learn more​

FT4 client documentation — covers FT4 operations and admin functions.

Asset management — detailed guide on FT4 assets and crosschain functionality.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Cookbook transaction-creation/simple-transaction

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction

This recipe demonstrates how to send a transaction using the signAndSendUniqueTransaction helper function, which
simplifies the process of creating, signing, and sending transactions on the Chromia blockchain.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Simple transaction demo

Key Features​

The signAndSendUniqueTransaction helper function does three things:

Automatic uniqueness - Adds a nop operation with random data to ensure the transaction is unique

Simplified signing - Signs with the provided signature provider or private key

Transaction receipt - Sends the transaction and returns a receipt with statusCode, status, and
transactionRid

Transaction Status​

The helper returns one of the following statuses:

Waiting - Transaction is pending inclusion

Rejected - Transaction was rejected by the network

Confirmed - Transaction was successfully included in a block

Unknown - Transaction status could not be determined

Learn more​

FT4 client documentation — covers account management and FT4 operations.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

Generic Transaction Protocol (GTX) — understanding GTX transaction structure.

### Cookbook transaction-creation/time-bound-transactions

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/time-bound-transactions

This recipe demonstrates how to create transactions that are only valid within a specific time window using the timeb
operation. Time-bound transactions enhance security and control by restricting when transactions can be included
on-chain.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Time-bound transactions demo

Key Features​

This recipe includes examples for:

Time window validation - Restricting transaction execution to specific time periods

Offline transaction building - Creating and signing GTX transactions with temporal constraints

Enhanced security - Preventing replay attacks and ensuring timely execution

Transaction composition - Combining transfers with time-bound operations

What timeb does​

The timeb(time bound) operation enforces a temporal condition on transaction validity by requiring that transactions
be included in a block only within a specified time window (in epoch milliseconds). This enhances control, security, and
synchronization of blockchain actions.
This operation requires that the client system's clock is reasonably synchronized with the signer node's clocks. While
minor discrepancies of a few seconds are generally acceptable, significant time differences, such as several hours, may
cause the operation to fail. This issue often arises from misconfigured system time settings, including incorrect time
zones or daylight saving time (DST) adjustments.
All timestamps are expected to be in Coordinated Universal Time (UTC). However, if the system's timezone
configuration is incorrect, it may result in inaccurate UTC values, leading to unexpected validation failures during the
timeb operation.
Learn more​

FT4 client documentation — covers account management and FT4 operations.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

Generic Transaction Protocol (GTX) — understanding GTX transaction structure.

### Cookbook transaction-creation/transfer-with-memo

Canonical: https://docs.chromia.com/build/cookbook/transaction-creation/transfer-with-memo

This recipe demonstrates how to create and execute asset transfers that include memo data on the Chromia blockchain.
Memos are useful for adding transaction references, payment IDs, or other metadata to transfers.

JS/TS client

Prerequisites​

Node.js

FT4 client: npm install @chromia/ft4

Postchain client: npm install postchain-client

Demo script​

Transfer with memo demo

Key Features​

This recipe includes examples for:

Online method - Using FT4 client transaction builder with memo operations

Offline method - Creating and signing GTX transactions with memo data locally

Memo integration - Adding custom memo data to transfers for enhanced tracking

Transaction building - Combining memo and transfer operations in a single transaction

Learn more​

FT4 client documentation — covers account management and FT4 operations.

Asset management — detailed guide on FT4 transfers and memo requirements.

Account management — understanding FT4 accounts and authentication.

JavaScript/TypeScript client — covers setup, connection,
and usage patterns.

### Integrations hub

Canonical: https://docs.chromia.com/build/integrations/

Chromia integrations provide comprehensive guides and tools for seamlessly connecting external systems, exchanges, and
applications with the Chromia blockchain. These integration resources help developers and technical teams efficiently
implement secure, scalable solutions.

Exchange integration overview​

Chromia provides comprehensive tools and protocols for seamless exchange integration, enabling cryptocurrency exchanges
to list CHR tokens and other FT4 assets efficiently and securely.

Key integration features​

Account management

Create and manage Chromia accounts with flexible authentication options

Support for both native Chromia accounts and EVM-compatible wallets

Multi-signature support for enhanced security

Account registration strategies for different business models

Transaction handling

Secure deposit and withdrawal processing using FT4 protocol

Real-time transaction monitoring and status tracking

Support for cross-chain transfers within the Chromia network

Integration with Chromia's Economy Chain for asset management

Advanced capabilities

Memo-enabled transfers for transaction context and identification

Private replica node setup for enhanced control and performance

Comprehensive monitoring and alerting systems

Support for non-existent account transfers with automatic activation

Integration benefits​

Operational efficiency

Streamlined account creation and management processes

Automated transaction processing with real-time monitoring

Reduced manual intervention through smart contract automation

Scalable architecture supporting high transaction volumes

Security and compliance

Multi-signature support for enhanced fund security

Transparent transaction history with full audit trails

Compliance-ready features for regulatory requirements

Secure key management and authentication protocols

User experience

Fast transaction processing with sub-second finality

Support for multiple wallet types and authentication methods

Seamless integration with existing exchange infrastructure

Comprehensive error handling and recovery mechanisms

Integration guides​

Complete step-by-step guide for exchanges to integrate with Chromia, covering account creation, transaction
handling, monitoring, and advanced features like multisig operations.

Learn how to implement memo functionality for FT4 transfers, enabling textual annotations for transactions that
are essential for exchange integrations and shared account management.

Getting started​

To begin your exchange integration with Chromia:

Review the exchange guide - Follow the comprehensive exchange integration guide for
step-by-step integration

Set up development environment - Install Chromia CLI and configure your development environment

Implement core features - Start with account creation and basic transaction handling

Add advanced features - Integrate memo functionality and monitoring systems

Deploy to production - Set up private replica nodes and implement security best practices

For technical implementation details, refer to the FT4 documentation and
client setup guides.

### Integrations exchange-guide

Canonical: https://docs.chromia.com/build/integrations/exchange-guide

This guide is designed for technical teams at exchanges that have decided to list Chromia and are focused on completing
the integration efficiently. The guide provides a step-by-step process to assist exchanges in integrating with Chromia.

Guide highlights​

Clear instructions for securely managing accounts and transactions.

Support for advanced features like memos and multisig operations.

Guidance on monitoring transaction status and ensuring finality.

Setup for hosting a private replica node for direct blockchain access.

Links to essential tools, documentation, and resources to streamline the integration process.

This section provides a high-level introduction to Chromia, its architecture, and key features to help exchanges
understand the platform before starting integration.

Learn how to create and manage Chromia accounts.

Instructions on connecting to Chromia's Economy Chain using public system nodes or by setting up a private replica
node for enhanced control.

Detailed guidance on handling deposits, withdrawals, and monitoring transactions using Chromia's FT4 library.

Explore how to send assets to accounts that have not yet been activated and ensure users can access their funds
securely.

Access extra tools, documentation, and links for testing, development, and troubleshooting to streamline
integration.

### Integrations exchange-guide/additional-resources

Canonical: https://docs.chromia.com/build/integrations/exchange-guide/additional-resources

Official website

Learn Chromia platform

Chromia Explorer

Chromia Vault

Staking

Testnet Explorer: Use the Testnet Explorer to monitor transactions and chain
activity.

Testnet staking: Test the staking functionality using a dedicated interface.

Testnet Vault: Use Chromia Vault to test wallet features and bridging.

Testnet faucet: Use the tCHR Faucet to obtain test tokens.

### Integrations exchange-guide/overview

Canonical: https://docs.chromia.com/build/integrations/exchange-guide/overview

Chromia is a Layer-1 blockchain platform designed to deliver high performance and scalability for decentralized
applications. Its multi-chain architecture ensures that each dapp operates independently, avoiding network congestion
and maintaining optimal performance even during peak usage.

Key features of Chromia​

Gas-Free Transactions: Users can interact with dapps without paying transaction fees, removing cost barriers and
simplifying adoption.

On-Chain Data Storage: Chromia allows large-scale data to be stored directly on the blockchain, enabling advanced
applications such as gaming, digital asset management, and enterprise solutions.

Relational Data Model: By integrating PostgreSQL, Chromia supports complex queries and data manipulation directly
on-chain, optimizing data accessibility and interaction.

Scalability: Each dapp operates on its dedicated chain, and the network scales horizontally by adding more nodes
and clusters as demand grows.

Key components​

Economy Chain: The backbone for Chromia's token ecosystem and the primary chain for integration.
Learn more here.

Postchain: The core component of Chromia combines blockchain functionality with relational databases. It processes
transactions, ensures consensus using the eBFT (Enhanced Byzantine Fault Tolerance) algorithm, and executes dapp code
written in the Rell programming language. Learn more here.

FT4 protocol: A token standard for managing assets, including account creation, transfers, and more.
Learn more here.

Chromia Vault: A wallet for managing accounts and tokens, enabling bridging and staking.

Dapp clusters: Each dapp is hosted in a dedicated cluster with its blockchain. Clusters interact through an
anchoring system that ensures consistency and security across the network.

### Integrations exchange-guide/step-1-account

Canonical: https://docs.chromia.com/build/integrations/exchange-guide/step-1-account

Creating an account on Chromia's Economy Chain is a crucial first step for integrating with the network. An account acts
as a unique identifier that enables interaction with decentralized applications on the Chromia blockchain. It is
required for executing transactions, holding and managing CHR tokens, and participating in staking and other ecosystem
functionalities. For exchange integration, you must create a dedicated account on the Economy Chain. This account will
serve as the foundation for all exchange-related operations.

To help you get started, let's explore three different ways to create an account on the Chromia Economy Chain.

Bridging CHR from an EVM-compatible blockchain​

Ensure you have at least 10 CHR in your EVM wallet (e.g., MetaMask).

Visit the Vault.

Connect your wallet.

Follow the steps to bridge 10 CHR from an EVM-compatible chain to the Chromia economy chain.

Once the bridging process is complete, your Chromia account will be created and activated.

Staking CHR​

Go to the Staking page.

Connect your wallet.

Stake minimum of 10 CHR.

Follow the prompts to complete the staking process.

After staking, your Chromia account will be created and ready to use.

Paying a fee via internal transfer​

Navigate to the Vault.

Connect your wallet and retrieve your account address on the Chromia economy chain.

Use an existing activated account to send at least 10 CHR to this new account address.

The 10 CHR will be used as a one-time account creation fee, activating the account.

If you do not have access to an existing activated account or require assistance with the account creation process,
please contact our technical support team. To ensure a smooth setup, kindly include your public key in the request — it
will be used to generate your Chromia account address.

### Integrations exchange-guide/step-2-access

Canonical: https://docs.chromia.com/build/integrations/exchange-guide/step-2-access

To access Chromia's Economy Chain, you have two primary options:

Public access: Use publicly available system nodes for a quick and simple connection without hosting your own
infrastructure.

Private node: Set up and run your own replica node for greater control, security, and reliability.

These options cater to different levels of control, reliability, and infrastructure needs, allowing you to choose the
best fit for your integration goals.

Use public system nodes​

For a quick and easy connection, you can use any of the publicly available system nodes. These nodes provide reliable
access to the Economy Chain without the need to host your own infrastructure. To connect, you can find the Economy Chain
BRID and a list of public nodes on the Chromia explorer.

If you choose this option, you can skip the remaining steps in this section and proceed directly to Step 3.

Set up your own replica node​

For greater control and security, setting up a private replica node is the recommended approach. Hosting your own node
provides better reliability and flexibility, with a detailed guide available to help you through hardware requirements,
configuration steps, and troubleshooting tips.

Add provider:

To register a node in the network, you must possess an identity of any tier and provide your public key to your
Chromaway contact. Please reach out to us so we can manually add you as a verified provider and link your node to the
system.

pmc keygen --save=".provider_keypair"

Download genesis config:

Create a directory called system_chains in your working directory:

mkdir system_chains

Download the genesis configuration file for the management chain.

curl -s https://system.chromaway.com/config/7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4?height=0 > system_chains/dc.xml

Start PostgreSQL:

Visit this page to set up a PostgreSQL database.

Configure the node:

Create two folders in your working directory: logs and node_config.

mkdir logs && mkdir node_config

These folders will be mounted to the node. Inside the node_config folder, add a node.properties file with the
following structure.

touch node_config/node.properties

Generate messaging.privkey and messaging.pubkey using the command.

pmc keygen -n -s node_identity

Fill out the node.properties file with the following fields:

messaging.privkey

messaging.pubkey

database.password

Also, add forwarding_replica with the value true to the file.

A detailed description of all available node properties can be found on this page or on
gitlab.

# Postchain configuration

messaging.privkey= <node privkey -- different from the provider privkey from step 1>

messaging.pubkey= <node pubkey -- different from the provider pubkey from step 1>

messaging.port=9870

genesis.pubkey=037434C8D4F2B7B7DE44E80486A814676DC3D898FD4488E10E1940B1C4C5837200

genesis.host=system.chromaway.com

genesis.port=9870

api.port=443  # Default HTTPS port - you can also use 7740 as an alternative

api.enable_tls=false

database.driverclass=org.postgresql.Driver

database.username=postchain

database.password=<password from step 3>

database.url=jdbc:postgresql:/172.17.0.1:5432/postchain

database.schema=postchain0

database.blockBuilderWriteConcurrency=100

database.readConcurrency=100

infrastructure=net.postchain.d1.D1InfrastructureFactory

metrics.prometheus.port=9190

configuration.provider.node=managed

container.postgres_max_locks_per_transaction=1024

forwarding_replica=true

Start the node:

Use Docker to start the node.

docker run --name postchain \

    --volume /var/run/docker.sock:/var/run/docker.sock \

    --mount type=bind,source="$(pwd)/node_config",target=/config,readonly \

    --mount type=bind,source="$(pwd)/logs",target=/opt/chromaway/postchain/logs \

    -e POSTCHAIN_DEBUG=true \

    -p 9874:9870/tcp \

    -p 9884:9881/tcp \

    -p 7744:7740/tcp \

    -p 7754:7750/tcp \

    -p 50055:50051/tcp \

    registry.gitlab.com/chromaway/postchain-chromia/chromaway/chromia-server:3.28.2 \

    run-server --node-config /config/node.properties

You can find the latest version of chromia-server
here.

Register node:

Register your node as a replica.

pmc node register-replica --pubkey <value of messaging.pubkey in the properties file>

Make sure that your .chromia/config includes the following line:

api.url = https://system.chromaway.com

Windows: C:\Users\<YourUsername>\.chromia\config

macOS and Linux: /Users/<YourUsername>/.chromia/config or /home/<YourUsername>/.chromia/config

Start the management chain:

Start the management chain to sync with the target network.

docker run --rm \

    --mount type=bind,source="$(pwd)"/system_chains/,target=/opt/chromaway/postchain/system_chains,readonly \

    registry.gitlab.com/chromaway/postchain-chromia/chromaway/chromia-server:3.28.2 \

    admin blockchain initialize -t 172.17.0.1:50055 -cid 0 -bc ./system_chains/dc.xml

Add replication of other chains:

Adding replication for additional chains is a critical step, but it can only be performed after your node has fully
synchronized with the system chains in the previous step. This ensures the node is correctly integrated with the
network.

To confirm synchronization, compare the block heights of the following chains using a block explorer or a local curl
command:

Management chain: 15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA

System Anchoring Chain: B497391373BB74944193205EB37C84B0520D474F491E2EF4743F16F670DB289B

curl http://localhost:7754/_debug

To replicate a specific chain, use the following command:

pmc blockchain replica add -brid <Target chain brid> --pubkey <value of messaging.pubkey in the properties file>

To find the BRID of the required chain, visit the
Chromia explorer.

To stop or start the node gracefully:

docker run --rm \

registry.gitlab.com/chromaway/postchain-chromia/chromaway/chromia-server:3.25.4 \

admin blockchain start -t 172.17.0.1:50055 -cid 0

### Integrations exchange-guide/step-3-transaction

Canonical: https://docs.chromia.com/build/integrations/exchange-guide/step-3-transaction

Handling deposits, withdrawals, and monitoring transactions are essential components of integrating with Chromia's
Economy Chain. These operations are streamlined using the FT4 library, which provides a comprehensive toolkit for
interacting with the Chromia ecosystem.

The FT4 library is designed to help developers build real-world applications within Chromia by offering out-of-the-box
functionality for:

Account creation and access management: Simplifies user account setup and integrates with familiar external
signature solutions.

Asset management: Enables issuance, allocation, transfers, and tracing of asset activities, both within a single
chain and across chains in the Chromia ecosystem.

Using the FT4 library, you can efficiently manage deposits and withdrawals while monitoring transaction activities
within the blockchain network.

Additional resources​

How to set up the FT4 library: Client setup guide

How to set up the Postchain JS/TS client:
Postchain JS/TS client

Detailed documentation: FT4 library overview

Repository: FT4 library GitLab

Memo integration guide: Memo integration guide

Required Dependencies​

Install the following packages using npm:

npm install @chromia/ft4@1.1.1 postchain-client@1.22.0

note

Make sure you have Node.js version 16 or higher installed on your system.

Using the FT4 library example​

Setting up the client​

Start by configuring the client to interact with the Chromia blockchain. Use the postchain-client library to create a
generic client and the @chromia/ft4 library to establish FT4 connections.

import { createClient } from "postchain-client";

import { createConnection } from "@chromia/ft4";

// All IDs are supposed to be 32bytes, or 64-character hex strings - account ID, blockchain RID, asset ID, etc.

// Blockchain connection details

const NODE_URL_POOL = [

  https://chr.bbbnnnbbb.net:443

  https://chroma.node.monster:7741

  https://chromia.01node.com:7740

  https://chromia-api.hashkey.cloud:7740

  https://chromia-mainnet-1.dappradar.com:7740

  https://chromia-mainnet.caliber.build:443

  https://chromia-mainnet.everstake.one:7740

  https://chromia-mainnet-replica.everstake.one:7740

  https://chromia-mainnet-systemnode-1.stakin-nodes.com:7740

  https://chromia-mainnet-system-node.asymm.ventures:7740

  https://chromia.mainnet-system.nodeops.ninja:7740

  https://chromia-mainnet-system-replica.dwellir.com:443

  https://chromia-mainnet.w3coins.io:7740

  https://chromia.nocturnallabs.org:7740

  https://chromia-sp.bwarelabs.com:7740

  https://chromia-system-node.moca-services.xyz:7740

  https://chromia.validatrium.club

  https://chromina-node.stablelab.xyz:7740

  https://dapps0.chromaway.com

  https://mainnet-dapp1.sunube.net:7740

  https://replica0.chromaway.com

  https://sys-main.chromia.coinhall.org:7740

  https://system.chromaway.com

];

// Note: Port 7740 can also be used as an alternative

const BLOCKCHAIN_RID = "15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA"; // Mainnet Economy chain BRID

// Create a generic client for calling queries and operations

const client = await createClient({

  directoryNodeUrlPool: NODE_URL_POOL,

  blockchainRid: BLOCKCHAIN_RID,

});

// Advanced client for FT4 interaction

const connection = createConnection(client);

Accessing an account​

Retrieve an account using its ID to access account-related data. This account remains read-only and can only fetch
information, not perform operations like transferring funds.

import { Account } from "@chromia/ft4";

// Access an account by its ID:

const accountId = "2aafb9bc1b75a0cb12850959ca148175491aee03aff5a53a11af595c579ddec1";

const myAccount: Account | null = await connection.getAccountById(accountId);

if (myAccount == null) throw "Account not found";

tip

The getAccountById function helps you verify whether an account exists before you initiate a transfer. By performing
this check, you identify if the recipient account already exists or if you need to send a minimum of 10 CHR to create
the account.

Retrieving transaction history​

Fetch transaction history in batches to handle large datasets efficiently. Use pagination to retrieve a specified number
of entries for better control over the data.

import { TransferHistoryType } from "@chromia/ft4";

// Retrieve the latest 200 transactions in which the account participated:

const { data: first200Entries, nextCursor } = await myAccount.getTransferHistory(

  200,//Maximum number of entries to fetch (default and cap: 200).

  { transferHistoryType: TransferHistoryType.Received } // Filter for received transactions.

);

// Retrieve 50 more transactions starting from the last fetched entry:

const { data: other50Entries, nextCursor: nextNextCursor } = await myAccount.getTransferHistory(

  50,//Number of entries to fetch.

  { transferHistoryType: TransferHistoryType.Received },//Filter for received transactions.

  nextCursor//Cursor to start from the last position in the previous query.

);

// Fetch 200 additional transactions:

const { data: other200Entries, nextCursor: thirdCursor } = await myAccount.getTransferHistory(

  200,

  { transferHistoryType: TransferHistoryType.Received },

  nextNextCursor//Start from the 250th entry.

);

Retrieving transfer details​

Access detailed information about a specific transfer, transaction, and block.

const aTransfer = first200Entries[0];

// Retrieve details about a specific transfer:

const info = await connection.getTransferDetails(aTransfer.transactionId, aTransfer.opIndex);

// Retrieve transaction details:

const transactionInfo = await client.getTransactionInfo(aTransfer.transactionId);

// Retrieve transaction status:

const transactionStatus = await client.getTransactionStatus(aTransfer.transactionId);

// Retrieve block details:

const blockInfo = await client.getBlockInfo(transactionInfo.blockRid.toString("hex"));

Retrieving account balances​

Check the balance of an account for all assets or focus on a specific asset to obtain precise information.

// Retrieve account balances (supports pagination):

await myAccount.getBalances();

// Retrieve the balance of a specific token:

await myAccount.getBalanceByAssetId("asset-id-as-hex-string"); // Returns null if balance is zero.

Setting up a key store and transferring funds​

Create a key store to handle transaction signing and transfer funds using a read-write account. Utilize utility
functions to manage token amounts effectively.

import {

  createInMemoryFtKeyStore,

  createInMemoryEvmKeyStore,

  createKeyStoreInteractor,

  createAmount,

  createAmountFromBalance,

  Session,

} from "@chromia/ft4";

// Set up a key store for signing transactions:

const keystore = createInMemoryFtKeyStore({

  privKey: Buffer.from("my-privkey-as-hex-string", "hex"),

  pubKey: Buffer.from("my-pubkey-as-hex-string", "hex"),

});

// Set up a EVM key store for signing transactions:

const keystoreEVM = createInMemoryEvmKeyStore({

  privKey: Buffer.from("my-privkey-as-hex-string", "hex"),

  pubKey: Buffer.from("my-pubkey-as-hex-string", "hex"),

});

// Retrieve accounts accessible by this key pair:

const { getAccounts, getSession } = createKeyStoreInteractor(client, keystore);

const accounts = await getAccounts();

// If you only have one account linked to the keypair, you can access it as:

const readonlyAccount = accounts[0]; // This account is still read-only.

// Access a read-write account:

const session: Session = await getSession("my-account-id-as-hex-string");

const account = session.account;

// Transfer funds to another account:

account.transfer(

  "receiver-id-as-hex-string",//Recipient account ID.

  "asset-id-as-hex-string",//Asset being transferred.

  createAmount(5, 6)//Amount: 5 units with 6 decimals (5000000n).

);

// Use `createAmountFromBalance` if the amount is already a bigint:

createAmountFromBalance(5000000n, 6);

Offline transactions​

Prepare a transaction without sending it immediately. This approach is not suitable when the environment lacks
blockchain access. It could be useful, however, whenever there should be a delay between the transaction being built and
the actual sending. For a completely offline transaction process (except for the sending step), refer to section 8.

import { transactionBuilder } from "@chromia/ft4";

// Build a transaction without sending it:

await transactionBuilder(account.authenticator, client)

  .add({

    name: "ft4.transfer",

    args: [

      Buffer.from("receiver-id-as-hex-string", "hex"),

      Buffer.from("asset-id-as-hex-string"),

      createAmount(5, 6).value,//or 5000000n

    ],

  })

  .build();

tip

You can find detailed information about operation names and arguments at:
FT4 Rell Documentation

Sending offline transactions​

Send a pre-built transaction by specifying transaction details and using a signer for authorization.

import { Transaction } from "postchain-client";

// The following information is required:

// - Blockchain RID

// - Account ID

// - Authentication descriptor ID

const blockchainRid = Buffer.from("id-as-hex", "hex");

const accountId = Buffer.from("id-as-hex", "hex");

const authDescriptorId = Buffer.from("id-as-hex", "hex");

const transaction: Transaction = {

  operations: [

    { name: "ft4.ft_auth", args: [accountId, authDescriptorId] },

    {

      name: "ft4.transfer",

      args: [

        /* Values for recipient, asset and amount as above. Buffer, Buffer, bigint. */

      ],

    },

  ],

  signers: [

    Buffer.from("pubkey-as-hex", "hex"),//The signer for the auth descriptor defined above.

  ],

};

// To send the transaction on a connected machine:

client.signAndSendUniqueTransaction(transaction, signatureProvider);

Offline transaction creation and signing​

For more complex scenarios where you need full control over transaction creation and signing, you can use the following
approach. This example demonstrates how to create a transaction with multiple operations, including memo, authentication
and transfer.

import { gtx, gtv, newSignatureProvider, createClient } from "postchain-client";

import { createAmount, deriveAuthDescriptorId, createSingleSigAuthDescriptorRegistration } from "@chromia/ft4";

// Define your keypair

const privateKey = "my-privkey-as-hex-string";

const publicKey = "my-pubkey-as-hex-string";

const keypair = {

  privKey: Buffer.from(privateKey, "hex"),

  pubKey: Buffer.from(publicKey, "hex"),

};

// Initialize the client

const chromiaClient = await createClient({

  nodeUrlPool: "https://system.chromaway.com",

  blockchainRid: "15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA",

});

// Define transaction parameters

const assetId = Buffer.from("asset-id-as-hex-string", "hex");

const toAddress = Buffer.from("receiver-id-as-hex-string", "hex");

const brid = Buffer.from("blockchain-rid-as-hex-string", "hex");

// Create signature provider

const sigProv = newSignatureProvider(keypair);

// Create authentication descriptor and derive IDs

const authDescriptor = createSingleSigAuthDescriptorRegistration(["A", "T"], keypair.pubKey, null);

const accountId = gtv.gtvHash(authDescriptor.args.signer);

const authDescriptorId = deriveAuthDescriptorId(authDescriptor);

// Create and build the transaction

let tx = gtx.emptyGtx(brid);

// Add memo operation

tx = gtx.addTransactionToGtx("memo", ["100346"], tx);

// Add authentication operation

tx = gtx.addTransactionToGtx("ft4.ft_auth", [accountId, authDescriptorId], tx);

// Add transfer operation

tx = gtx.addTransactionToGtx("ft4.transfer", [toAddress, assetId, createAmount(1000, 2).value], tx);

// Add NOP operation

tx = gtx.addTransactionToGtx(

  "nop",

  [7], /random number

  tx

);

// Add signer and sign the transaction

gtx.addSignerToGtx(keypair.pubKey, tx);

tx = await gtx.sign(tx, sigProv);

// Serialize and send the transaction

const encodedSignedTx = gtx.serialize(tx);

const signedTx = await chromiaClient.sendTransaction(encodedSignedTx);

tip

When creating offline transactions, make sure to:

Keep your private keys secure

Verify all transaction parameters before signing

Double-check the blockchain RID and node URLs

Test the transaction on a testnet before using it on

Offline account registration​

You can also register a new account offline using the following approach.

import { gtx, newSignatureProvider } from "postchain-client";

import { createSingleSigAuthDescriptorRegistration, gtv as gtvft4 } from "@chromia/ft4";

// Define your keypair

const privkey = "my-privkey-as-hex-string";

const pubkey = "my-pubkey-as-hex-string";

const keypair = {

  privKey: Buffer.from(privkey, "hex"),

  pubKey: Buffer.from(pubkey, "hex"),

};

// Initialize the client

const chromiaClient = await createClient({

  nodeUrlPool: "https://system.chromaway.com",

  blockchainRid: "15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA",

});

// Define transaction parameters

const assetId = Buffer.from("asset-id-as-hex-string", "hex");

const brid = Buffer.from("blockchain-rid-as-hex-string", "hex");

// Create signature provider

const sigProv = newSignatureProvider(keypair);

// Create authentication descriptor

const authDescriptor = createSingleSigAuthDescriptorRegistration(["A", "T"], keypair.pubKey, null);

const ras_params = gtvft4.authDescriptorRegistrationToGtv(authDescriptor);

// Create and build the transaction

let tx = gtx.emptyGtx(brid);

// Add transfer fee operation

tx = gtx.addTransactionToGtx("ft4.ras_transfer_fee", [assetId, ras_params, null], tx);

// Add account registration operation

tx = gtx.addTransactionToGtx("ft4.register_account", [], tx);

// Add NOP operation

tx = gtx.addTransactionToGtx("nop", [Buffer.from([0])], tx);

// Add signer and sign the transaction

gtx.addSignerToGtx(keypair.pubKey, tx);

tx = await gtx.sign(tx, sigProv);

// Serialize and send the transaction

const encodedSignedTx = gtx.serialize(tx);

const signedTx = await chromiaClient.sendTransaction(encodedSignedTx);

### Integrations exchange-guide/step-4-non-existent-accounts

Canonical: https://docs.chromia.com/build/integrations/exchange-guide/step-4-non-existent-accounts

Exchanges can send assets to users who do not yet have a Chromia account by transferring funds to an unregistered
account. This process allows users to activate their accounts through the Chromia Vault. The exchange should first
verify if the user has an existing Chromia account:

If the account exists: You can send any amount of CHR.

If the account does not exist: Notify the user that they need to send at least 10 CHR, as this is the required fee
for account creation.

Once the transfer is complete, users can visit the Chromia Vault to activate their
accounts. Below is the implementation example:

// Create a single-signature account descriptor for the new account

const authDescriptor = createSingleSigAuthDescriptorRegistration(

  ["A", "T"],//Fixed value for the Economy Chain

  Buffer.from("pubkey or EVM address, without 0x", "hex"),//Public key or EVM-compatible address of the recipient

  null//No additional rules; this is standard for all account registrations

);

// Retrieve the account ID for the single-signature account

const signer = authDescriptor.args.signer;

const singleSigAccId = gtv.gtvHash(signer);

// Transfer assets to the unregistered account

// Ensure the amount is at least 10 CHR to cover the account creation fee

myAccount.transfer(

  singleSigAccId,

  "CHR asset id as hex",//The unique identifier for the CHR asset

  createAmount(19, 6)//Amount: 19 CHR (9 will be available; 10 will be used as the account creation fee)

);

// The user needs to visit the Chromia Vault to activate their account

// Inform the user to go to https://vault.chromia.com for account activation

// Creating the account yourself

// Set up an in-memory key store with the private key for signing

const ks = createInMemoryFtKeyStore(encryption.makeKeyPair("privkey"));

// Register the account using the provided registration strategy

const { session } = await registerAccount(client, ks, registrationStrategy.transferFee(CHRAsset, authDescriptor, null));

// After registration, the session can be used for further operations. The account is now created and can be retrieved as described in the previous steps.

### Integrations memo-guide

Canonical: https://docs.chromia.com/build/integrations/memo-guide

The memo feature adds an optional memo requirement for FT4 transfers, ensuring that certain accounts mandate memos for
all incoming transfers. This feature is useful for attaching textual annotations to blockchain transactions, making it
especially beneficial for applications requiring transaction context and shared account management.

Business value​

The memo system delivers significant business benefits:

Enhanced transaction tracking​

Contextual information attached to transfers for better identification

Optional memo enforcement allows flexible account configuration

Textual annotations provide human-readable transaction context

Standardized protocols ensure consistent memo handling across platforms

Operational efficiency​

Simplified account management for shared accounts and complex workflows

Reduced operational overhead in transaction identification and reconciliation

Flexible account configuration supports different business requirements

Automated processing reduces manual intervention in transaction tracking

Risk mitigation​

Transaction context prevents misallocation of funds in shared accounts

Memo requirements ensure proper identification of transfer purposes

Audit trail enhancement provides better transaction history and compliance

Error reduction through clear transaction labeling and identification

Key components​

Operations​

Enable memo requirement

Operation: enable_transfer_memo()

Description: Activates memo enforcement for an FT4 account, requiring that all incoming transfers include a memo.

Disable memo requirement

Operation: disable_transfer_memo()

Description: Deactivates memo enforcement, allowing incoming transfers without a memo.

Add a memo

Operation: memo(text)

Description: Attaches a textual memo to a transfer.

Constraints:

The memo text must be between 1 and 50 characters.

This operation is only valid when paired with an ft4.transfer operation targeting accounts that require memos.

Queries​

Check memo requirement

Query: does_account_require_memo(account_id: byte_array): boolean

Description: Checks if an FT4 account enforces memo requirements for incoming transfers.

tip

For the full implementation of the memo feature, refer to the source code
here.

Example workflows​

FT4 client-side integration​

Receiver account should allow to add memo to transactions:

/... code for initialization

await session.call(op("enable_transfer_memo"));

Sender account of transfer transactions can add memo if it was enabled by receiver:

// Initialization code

const tb = session.transactionBuilder();

    tb.add({name: "memo", args: [memo]}, { authenticator: noopAuthenticator })

      .add(transfer(<ACCOUNT_ID>, <assetId>, <amount>))

      .buildAndSend();

Find complete instructions for FT4 client-side initialization and asset transfers here.

note

You can find more examples of memo usage in the
Recipes repository.

Rell test scripts​

Enable memo requirement​

rell.test.tx()

    .op(enable_transfer_memo())

    .op(test.ft_auth_operation_for(alice.keypair.pub))

    .sign(alice.keypair)

    .run();

Transfer with memo​

rell.test.tx()

    .op(memo("For invoice #1234"))

    .op(test.ft_auth_operation_for(alice.keypair.pub))

    .op(assets.external.transfer(bob.account_id, asset.id, 100))

    .sign(alice.keypair)

    .run();

Query memo requirement​

val requires_memo = does_account_require_memo(bob.account_id);

print("Bob's account requires memo: %s".format(requires_memo));

Invalid transfer (no memo)​

rell.test.tx()

    .op(test.ft_auth_operation_for(alice.keypair.pub))

    .op(assets.external.transfer(bob.account_id, asset.id, 100))

    .sign(alice.keypair)

    .run_must_fail("Transfers to account ... require a memo");

tip

For the complete set of Rell test examples, check
here.

Use cases​

The memo feature is particularly valuable for:

Exchange integrations requiring transaction context for shared accounts

Shared account management where multiple users access the same account

Payment processing systems needing invoice or order references

Audit and compliance requirements for transaction tracking

Cross-border transfers requiring additional context information

Next steps​

Explore the FT4 client documentation

Follow the deployment guide

Check out other integration guides on Chromia

### Connect deployment/mainnet/connect-client

Canonical: https://docs.chromia.com/build/deployment/mainnet/connect-client

This section covers how to connect a frontend or other client to a dapp backend deployed on a Chromia network. The
examples here use the TypeScript-based postchain-client, but the
principles are the same when using other client libraries.

Available Client Libraries

This guide shows one example using the TypeScript client. For a complete list of available client libraries and their
documentation, see the Client Libraries Overview.

To connect to a deployed dapp, the client library needs a pool of URLs to system nodes in the Chromia network, as well
as the Blockchain RID of the dapp to connect to. The client automatically queries the
Directory Chain on the system nodes to fetch the URLs of all nodes where your dapp is
currently running, thus establishing a connection to the dapp.

Ensure you have a dapp deployed on Mainnet. For deployment instructions, see
Deploy your dapp to Mainnet.

If you are deploying a frontend created using the
Hello World Quickstart, remove the admin
key pair configuration and replace it with a secure link to your stored key pair. This ensures the security of your
key pair. For more details on how to build a production-ready dapp, visit the
Chromia course page.

// Remove any private keys from your production client code

const adminPubkey = Buffer.from("<PubkeyLink>", "hex");

const adminPrivkey = Buffer.from("<PrivkeyLink>", "hex");

Specify URLs of Chromia nodes running the Directory Chain in your client code. For the most recent and complete list,
visit the Chromia Explorer and view the list of nodes in the system cluster.

const directoryNodeUrlPool = [

 "https://system.chromaway.com",

 "https://chromia.validatrium.club",

 "https://chromia-mainnet-systemnode-1.stakin-nodes.com",

 "https://chroma.node.monster:7741",

 "https://dapps0.chromaway.com",

 "https://chromia-mainnet.w3coins.io:7740",

 "https://mainnet-dapp1.sunube.net:7740"

];

Establish a connection to your dapp by providing the dapp's Blockchain RID and the system node URLs:

// Connection input

const blockchainRID = "<BlockchainRID>"; // Target Blockchain RID

const directoryNodeUrlPool = ["<TargetUrl1>", "<TargetUrl2>", "etc."]; // System node URLs

// Connection setup

const chromiaClient = await pcl.creatClient({

  blockchainRID,

  directoryNodeUrlPool,

});

Replace <BlockchainRID> with the actual blockchain RID for your dapp, and the <TargetUrls> with the appropriate
Chromia system node URLs from step 3.

With these steps, you have successfully configured postchain-client to work with your dapp backend running on the
Chromia network.

To learn more about developing production-ready dapps, visit the Chromia course page.

### Connect deployment/testnet/connect-client

Canonical: https://docs.chromia.com/build/deployment/testnet/connect-client

This section covers how to connect a frontend or other client to a dapp backend deployed on a Chromia network. The
examples here use the TypeScript-based postchain-client, but the
principles are the same when using other client libraries.

To connect to a deployed dapp, the client library needs a pool of URLs to system nodes in the Chromia network, as well
as the Blockchain RID of the dapp to connect to. The client automatically queries the
Directory chain on the system nodes to fetch the URLs of all nodes where your dapp is
currently running, thus establishing a connection to the dapp.

info

To check if the dapp is deployed successfully - visit the Chromia Explorer and
set Current network to Testnet.
Deployed dapp will be accessible in about five minutes.

Ensure you have a dapp deployed on a Chromia network. This will most likely be Testnet. For deployment
instructions, see Deploy your dapp to testnet.

If you are deploying a frontend created using the Hello World guide,
remove the admin key pair configuration and replace it with a secure link to your stored key pair. This ensures the
security of your key pair. For more details on how to build a production-ready dapp, visit the
Chromia course page.

// Remove any private keys from your production client code

const adminPubkey = Buffer.from("<PubkeyLink>", "hex");

const adminPrivkey = Buffer.from("<PrivkeyLink>", "hex");

Establish a connection to your dapp by providing the Blockchain RID and a node pool from Testnet:

// Connection input

const blockchainRID = "<BlockchainRID>"; // Target Blockchain RID

const directoryNodeUrlPool = [

  "https://node0.testnet.chromia.com:7740",

  "https://node1.testnet.chromia.com:7740",

  "https://node2.testnet.chromia.com:7740",

  "https://node3.testnet.chromia.com:7740",

]; // Target URLs

// Connection setup

const chromiaClient = await pcl.createClient({

  blockchainRID,

  directoryNodeUrlPool,

});

Replace <BlockchainRID> with the actual blockchain RID for your dapp.

With these steps, you have successfully configured postchain-client to work with your dapp backend running on the
Chromia network.

To learn more about developing production-ready dapps, visit the Chromia course page.


## Cookbook demo repositories (official page links)

JS/TS cookbook pages describe the pattern and link out. Demos are not inlined.

Fee / registration (Rell + JS):
- https://bitbucket.org/chromawallet/fee-samples/src/main/open/rell/
- https://bitbucket.org/chromawallet/fee-samples/src/main/open/js_client/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/rell/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/js_client/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/rell/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/js_client/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/rell/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/js_client/

Recipes:
- https://bitbucket.org/chromawallet/recipes/src/main/get_account_by_id/
- https://bitbucket.org/chromawallet/recipes/src/main/get_account_by_signer/
- https://bitbucket.org/chromawallet/recipes/src/main/get_account_transfer_history/
- https://bitbucket.org/chromawallet/recipes/src/main/get_block/
- https://bitbucket.org/chromawallet/recipes/src/main/get_transaction/
- https://bitbucket.org/chromawallet/recipes/src/main/get_transaction_status/
- https://bitbucket.org/chromawallet/recipes/src/main/check_account_memo_requirement/
- https://bitbucket.org/chromawallet/recipes/src/main/get_account_balance/
- https://bitbucket.org/chromawallet/recipes/src/main/make_query/
- https://bitbucket.org/chromawallet/recipes/src/main/pagination_with_ft4/
- https://bitbucket.org/chromawallet/recipes/src/main/pagination/
- https://bitbucket.org/chromawallet/recipes/src/main/call_operation_with_ft4_auth/
- https://bitbucket.org/chromawallet/recipes/src/main/make_crosschain_transfer/
- https://bitbucket.org/chromawallet/recipes/src/main/enable_disable_memo/
- https://bitbucket.org/chromawallet/recipes/src/main/make_transfer/
- https://bitbucket.org/chromawallet/recipes/src/main/register_asset/
- https://bitbucket.org/chromawallet/recipes/src/main/register_crosschain_asset/
- https://bitbucket.org/chromawallet/recipes/src/main/simple_transaction/
- https://bitbucket.org/chromawallet/recipes/src/main/timebound/
- https://bitbucket.org/chromawallet/recipes/src/main/transfer_with_memo/
- https://bitbucket.org/chromawallet/recipes/src/main/memo

## Coverage

Crawled 2026-08-26 from docs.chromia.com sitemap plus leaf fetches (WebFetch and curl; Cloudflare on some WebFetch 404/timeouts).

Covered:
- Clients overview, FT4 stub, Postchain JS/TS (overview + Hello World + reference), Kotlin, Python, Go, Rust, C#, REST API groups page, React Kit, MCP Server, Bridge stub + ecosystem client/work/example, Filehub stub + overview + work
- All FT4 client usage pages under /build/ft4/client/
- All cookbook sitemap leaves plus overview
- Integrations hub, full exchange guide (overview, steps 1-4, additional-resources), memo guide
- Testnet and mainnet connect-client

Not fetched:
- Bitbucket recipe source files (only official page text + demo URLs)
- OpenAPI /pages/postchain-restapi.html (532-byte stub)
- /build/clients/ and /build/cookbook/ directory indexes (HTTP 404)

Gaps the official pages themselves leave:
- Several client pages say documentation is being updated / may be outdated
- Most cookbook JS recipes are descriptions plus Bitbucket links
- REST API page is a capability table, not a path/query-param spec
- Economy Chain BRID 15C0CA99...7BBA is labeled both Management chain (exchange step 2) and Mainnet Economy chain BRID (step 3); recorded as printed
