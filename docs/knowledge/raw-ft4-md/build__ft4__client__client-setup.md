On this page
# FT4 client setup with TypeScript

This section discusses how to install and initialize the FT4 client.

#### Install the client​

Use npm to install the FT4 client:

```
npm install @chromia/ft4

```

#### Initialize the client​

In your JavaScript code, initialize the FT4 client:

```
const { createClient } = require("postchain-client");
const { createConnection } = require("@chromia/ft4");

const url = "http://localhost:7740";
const client = await createClient({
  nodeUrlPool: url,
  blockchainIid: 0,
});

const connection = createConnection(client);

```

In the code snippet above, we import the necessary modules from the `postchain-client` and `@chromia/ft4` packages. Next, we define the `url` of the Postchain network we want to connect to (in this case, "http://localhost:7740"). We then create an instance of the `IClient` using the specified URL and the internal ID of the chain, which by default is `0` on a local network.

With the client initialized, you can now utilize its capabilities to interact with the Postchain network and perform various operations, such as sending transactions and querying the blockchain.

We then pass the client to the `createConnection` method from FT4, allowing us to easily use all of the FT4 features.

#### Test the connection​

To test the connection, add a function to call a method on the connection object:

```
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

```

Run the above example with `node index.js`. This will print all the assets registered on the blockchain. If no assets are registered, the response will look like this:

```
{ data: [], nextCursor: null }

```
