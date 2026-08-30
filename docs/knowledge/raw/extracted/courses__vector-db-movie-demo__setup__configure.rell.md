# Configure your Rell module

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Set up your project](/courses/vector-db-movie-demo/setup/)
- Lesson 2 – Configure your Rell moduleOn this page
# Configure your Rell module

In this lesson, you will configure your chromia.yml file with the correct dimensionality for the Vector Database Extension and prepare it for deployment to the Chromia testnet.

## Open the rell/ folder​

If you’re still inside the python/ folder from the previous step, navigate back to the project root:

```bash
cd ..
```

Next, move into the Rell backend folder:

```bash
cd rell
```

## Set the correct embedding dimensions​

Open the chromia.yml file and find the following section:

rell/chromia.yml
```yaml
blockchains:  vector_demo:  # Update this to a unique name before deployment    module: vector_interface    config:      gtx:        modules:          - "net.postchain.gtx.extensions.vectordb.VectorDbGTXModule"      vector_db_extension:        dimensions: 768  # Match your selected embedding model
```

Update the dimensions: value to match the embedding model you selected earlier:

| 
| Model| Dimensions
| MiniLM| 384
| mpnet-base| 768
| mpnet-large / e5 / bge| 1024
noteEnsure this number matches the vector size produced by your selected model exactly.

A mismatch will lead to failed vector uploads or queries.

## Set a unique chain name​

Chain name must be globally unique on testnetUpdate the following line in chromia.yml to a unique name. This name will serve as the public identifier of your chain on the testnet:

```yaml
blockchains:  vector_demo:  # Update this to a unique name before deployment
```
For example:

```yaml
blockchains:  vector_demo_42:
```
If another user has already deployed a chain with the same name, your deployment will not succeed. So, be sure to choose a name that is unique across the testnet.

## Generate a deployment key​

To sign the deployment transaction, you’ll need a deployment key. If you haven’t generated one yet, run the following command:

```bash
chr keygen --key-id="vector_db_demo_key"
```

This will create a new key pair saved in your local .chromia directory.

After generating the key, you will find your private key saved at:

```bash
~/.chromia/vector_db_demo_key
```

Open this file, copy the full hex-encoded private key, and paste it into the PRIV_KEY field in your Python .env file, located in the python/ folder:

```env
PRIV_KEY=
```

This key is essential for signing transactions when you upload or query vectors on-chain.

## Lease a container on testnet​

Before you deploy, lease a container that supports the Vector Database Extension.

Follow this guide to lease a container and obtain your container ID:

👉 [Get a container on Chromia testnet](https://docs.chromia.com/intro/getting-started/testnet/get-container)

Using your public keyDuring the leasing process, you will need to provide a public key. Use the public key generated earlier with chr keygen.

You will see this public key printed in your terminal upon key creation, and it’s also saved in the following file:

```bash
~/.chromia/.pubkey
```

Once you’ve leased your container, paste the container ID into the chromia.yml file as shown:

```yaml
deployments:  testnet:    brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"    url: https://node0.testnet.chromia.com:7740    container:     # After deployment, insert the generated `chains:` section here
```

Leave the brid and url as they are; they point to the testnet Directory Chain and node0.

## What’s next?​

After configuring your chromia.yml file with the correct container ID, embedding dimensions, and a unique chain name, along with generating your deployment key, you’re ready to deploy the Rell module.

We will cover that in the next lesson.
