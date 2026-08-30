On this page
# Set up your FT4 project

In this section, you'll learn how to set up a project to use FT4. If you want to see an example of a complete dapp using the FT4 protocol, you can explore the FT4 demo app.

- 
Install Chromia CLI and set up PostgreSQL database.

- 
Create a new directory for your project and navigate into it:

```
mkdir ft4-demo && cd ft4-demo

```

- 
Use the Chromia CLI to create a Rell project:

```
chr create-rell-dapp && cd my-rell-dapp

```

- 
Generate an admin key pair that will be used for registering accounts and assets:

```
chr keygen --file .chromia/ft4-admin.keypair

```
info
Ensure the keypair file is saved inside the `.chromia` folder, which is ignored by `.gitignore`. If saved elsewhere, add `*.keypair` to the appropriate `.gitignore` file. 
- 
Configure the FT4 module arguments by adding the `moduleArgs` and `libs` sections to your `chromia.yml` file.

```
blockchains:
  hello:
    module: main
    moduleArgs:
      lib.ft4.core.admin:
        admin_pubkey: 03028A31DBA82E46DE26A608249147A6A1A88C62A1A65B640C9B4369D4CAD928BE
libs:
  ft4:
    registry: https://gitlab.com/chromaway/ft4-lib.git
    path: rell/src/lib/ft4
    tagOrBranch: v1.1.0r
    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"
    insecure: false
  iccf:
    registry: https://gitlab.com/chromaway/core/directory-chain
    path: src/lib/iccf
    tagOrBranch: 1.87.0
    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"
    insecure: false

compile:
  rellVersion: 0.14.9 # Or your rell version if your is newer

test:
  modules:
    - test.arithmetic_test
    - test.data_test

```
note
The ICCF dependency is mandatory to use cross-chain transfers in your dapp, but we suggest installing it even if you're not planning to use it. It won't be compiled in the production code, and your IDE might complain if you don't have it.
The `admin_pubkey` definition is only required if you're using the admin module.
For more information, see documentation for imports. 
- 
Replace the `admin_pubkey` value with the generated admin public key from the `ft-admin.keypair` file. You can also configure the rate limiter settings as needed. Refer to the rate limiter topic for more details on the configuration.

- 
Install the FT4 Rell library to your project:

```
chr install

```

:::note VS Code users

If you're using Visual Studio Code and see import errors for FT4 libraries after running `chr install`, close and reopen your `main.rell` file to refresh the editor. Alternatively, use the Command Palette (View -> Command Palette) and run Rell: Invalidate index caches.

:::

- 
Import some of the FT4 functionality into the main app module by adding these import statements to the `main.rell` file:

```
import lib.ft4.accounts;
import lib.ft4.assets;
import lib.ft4.admin;

```
note
This method of importing is the easiest to set up. For more advanced import options, refer to the documentation. 
- 
Start the node to verify the configuration:

```
chr node start

```

Run a query to check if the FT4 module is imported correctly:

```
chr query ft4.get_config

```

The output should look similar to:

```
{
  rate_limit={
    active=1,
    max_points=10,
    points_at_account_creation=1,
    recovery_time=5000
  }
}

```

## FT4 client setup with TypeScript​

This section discusses how to install and initialize the FT4 client.

#### 1. Install the Client​

Use npm to install the FT4 client:

```
npm install @chromia/ft4

```

#### 2. Initialize the client​

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

#### 3. Test the connection​

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

### Registering assets​

Once you complete your project setup and initialize your clients, proceed to register assets and perform various blockchain operations using the FT4 protocol.

For detailed instructions on registering assets and other operations, refer to the Register assets topic.
- FT4 client setup with TypeScript
- Registering assets