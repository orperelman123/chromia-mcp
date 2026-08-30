# Testing the dapp

URL: https://learn.chromia.com

- [Home](/)
- Lesson 4 - Test the dapp
# Testing the dapp

We test the dapp manually by starting the corresponding blockchains, creating a subscription, and using it to access the
warehouse.

First, ensure that the blockchains are defined in the following order to align with the internal IDs used in this
course:

chromia.yml
```yaml
blockchains:  subscription_chain: # Chain id 1  ...  digital-warehouse-chain: # Chain id 2  ...
```

To start the blockchains, you need to run the node from the project folder in a separate terminal window:

```shell
chr node start --directory-chain-mock
```

Expected output of logs:

```shell
...Starting blockchain directory-chain with brid B39406781EE6CA71391B519AA1D3B009DA77A0A63B12DCB2BD4DD59C679E2703 on id 0Starting blockchain subscription_chain with brid EB0A24835FC5B49CFCB6A9C3DC688FBC7D457455E69B9B2956E7689BC830CA49 on id 1Starting blockchain digital_warehouse_chain with brid 42C95A7CCDAE9B78524A32428E7B313F60AEB31BA66FF41A0ECE2FCB576D5FF4 on id 2...
```

Please note that values of brids can be different on your side.

Let`s save brids into environment variables for convenient reuse in the following steps:

```bash
SUBSCRIPTION_CHAIN_BRID=$(curl -s localhost:7740/brid/iid_1) && echo $SUBSCRIPTION_CHAIN_BRID
```

```bash
DIGITAL_WAREHOUSE_BRID=$(curl -s localhost:7740/brid/iid_2) && echo $DIGITAL_WAREHOUSE_BRID
```

Proceed with the setup by creating a new keypair and storing it in the .chromia/config directory, the default search
path for Chromia CLI:

```shell
chr keygen --file .chromia/config
```

Note down the pubkey printed to the console. You will use this as the account ID input for our operations.

Next, attempt to register a new product category to observe that making a transaction towards the digital warehouse will
fail:

```shell
chr tx -brid $DIGITAL_WAREHOUSE_BRID register_product_category 555 0 --await -cfg .chromia/config
```

Result
```shell
... Operation 'digital_warehouse_chain:register_product_category' failed: No account found
```

Create a new subscription on the Subscription chain before attempting again.

Now, you can create a new subscription. Replace <pubkey> with the previously generated one:

```shell
chr tx -brid $SUBSCRIPTION_CHAIN_BRID subscribe 'x"" '[x"", 0]' --await -cfg .chromia/config
```

Example for using an environment variable substitution:

```bash
chr tx -brid $SUBSCRIPTION_CHAIN_BRID subscribe  'x"'"$DIGITAL_WAREHOUSE_BRID"'"' '[x"021E6EE49D997C6DF7721F87027BB56D7436A9CC78F344AE6FF2E8A08789D37F5A", 0]' --await -cfg .chromia/config
```

Output should be like this:

```bash
transaction with rid F300CBD62F22917145F8594DB63F1780803CD123CA7F89DBE952431409EE2629 was posted CONFIRMED
```

Note the transaction rid that was output from the command. You can now use the following command to authenticate the
digital warehouse. Replace <tx-rid> with the previously obtained value.

This command specifies that we intend to make a transaction towards chain ID 2 and include an iccf-transaction that
needs verification against chain 1:

```shell
chr tx -brid $DIGITAL_WAREHOUSE_BRID --iccf-source $SUBSCRIPTION_CHAIN_BRID --iccf-tx  authorize  --await -cfg .chromia/config
```

Now, we should be able to access the system. Let's attempt to register a product category and update the inventory
again:

```shell
chr tx -brid $DIGITAL_WAREHOUSE_BRID register_product_category 555 0 --await -cfg .chromia/config
```

```shell
chr tx -brid $DIGITAL_WAREHOUSE_BRID update_inventory '[555, 1500, "Got some stuff"]' --await -cfg .chromia/config
```

Let's wrap up by generating a warehouse report and checking the receipts in the subscription chain:

```shell
chr query -brid $DIGITAL_WAREHOUSE_BRID create_report 'from=0' 'to=null' -cfg .chromia/config
```

Result
```shell
[    "history": [        [555, [            ["amount": 1500, "comment": "Got some stuff", "product_category": 555, "transaction": 5]]            ]        ],    "inventory": [        ["UNIT": "LITRE", "product_category": 555, "stock": 1500]        ],    "warehouse_id": 1]
```

```shell
chr query -brid $SUBSCRIPTION_CHAIN_BRID get_receipts 'account_id=null' 'blockchain_rid=null' 'from=null'
```

Result
```shell
[    [        "account_id": x"02EEB43C7400CA3CEBDECF1C8AC049EAC05EB361FE4C003EF6D298DEA792F58526",        "blockchain_rid": x"237B0EE3C60CBB1884E63F883F8230FED5C804A9C6933B28FD9214D65FD033B5",        "payment_amount": 30,        "period": "WEEK",        "receipt_id": x"6006EFC423E9032EA4C9584CDE791623A015749FD8BA4831457CCE0769BCA20A",        "timestamp": 1705504892289    ]]
```

Congratulations! You have now learned how to develop a dapp that verifies transactions on another chain to authenticate
users.
