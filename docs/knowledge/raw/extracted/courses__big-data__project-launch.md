# Prepare the project

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Prepare your projectOn this page
# Prepare the project

## Prepare the blockchain component​

### Navigate to the blockchain directory​

```shell
cd rell
```

### Start the blockchain component​

```shell
chr node start
```

## Prepare the PySpark component​

### Navigate to the PySpark directory​

```shell
cd pyspark
```

### Populate the .env file with the following values​

```env
POSTCHAIN_TEST_NODE=http://localhost:7740BLOCKCHAIN_TEST_RID=brid # This can be found in the terminal of the running chr nodePRIV_KEY=your_private_key # Enter the private key of the signer here
```
