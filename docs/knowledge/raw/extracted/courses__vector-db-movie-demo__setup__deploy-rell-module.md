# Deploy your Rell module

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Set up your project](/courses/vector-db-movie-demo/setup/)
- Lesson 3 – Deploy your Rell moduleOn this page
# Deploy your Rell module

With your chromia.yml configured, your container leased, and a deployment key created, you are now prepared to deploy the Rell module to the Chromia testnet.

## Install and build​

Navigate to the rell/ folder and run the following commands:

```bash
chr install      # Installs the vector_db library declared in chromia.ymlchr build        # Compiles your Rell module and validates the syntax
```

These commands install all necessary dependencies and compile your module before deployment.

## Deploy the module​

Execute the following command to deploy your module:

```bash
chr deployment create --settings chromia.yml --network testnet --blockchain  --key-id="vector_db_demo_key"
```

- Ensure that <your-chain-name> matches what you specified under blockchains: in your chromia.yml.

- The --key-id must correspond with the key you created earlier (vector_db_demo_key).

## Update chromia.yml​

After deploying, the CLI will output a chains: section. Copy this section directly into your chromia.yml. It will look something like this:

```yaml
deployments:  testnet:    brid: x""    url: https://node0.testnet.chromia.com:7740    container:     # After deployment, insert the generated `chains:` section here    chains:      : x""
```

Paste this chains: block under the testnet deployment entry without making any changes or using placeholders.

infoYou will need the blockchain RID again soon for your Python .env file. Feel free to copy it now or retrieve it later from chromia.yml.

## Verify the setup​

Run the following command to confirm that your configuration is valid:

```bash
chr deployment update --settings chromia.yml --network testnet --blockchain  --key-id="vector_db_demo_key"
```

If everything is set up correctly, the command should succeed even if there are no changes. This verifies that your deployment and configuration are ready for future updates.

## What’s next?​

You have successfully deployed your Rell module and configured your chromia.yml correctly. Now, you are ready to finalize your Python environment.
