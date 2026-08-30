# Python components

URL: https://learn.chromia.com

- [Home](/)
- Lesson 5 - Explore Python componentsOn this page
# Python components

## Navigate to the PySpark folder​

```shell
cd pyspark
```

## Client-side interaction with the blockchain​

The BlockchainClient class utilizes the NetworkSettings class to establish a connection to the blockchain node. This includes parameters such as node URLs, retry intervals, and failover strategies.

./seed_products.py
```python
settings = NetworkSettings(            node_url_pool=[os.getenv("POSTCHAIN_TEST_NODE", "http://localhost:7740")],            status_poll_interval=int(os.getenv("STATUS_POLL_INTERVAL", "500")), # Opitional parameter            status_poll_count=int(os.getenv("STATUS_POLL_COUNT", "5")), # Opitional parameter            verbose=True, # Opitional parameter            attempt_interval=int(os.getenv("ATTEMPT_INTERVAL", "5000")), # Opitional parameter            attempts_per_endpoint=int(os.getenv("ATTEMPTS_PER_ENDPOINT", "3")), # Opitional parameter            failover_strategy=FailoverStrategy.ABORT_ON_ERROR, # Opitional parameter            unreachable_duration=int(os.getenv("UNREACHABLE_DURATION", "30000")), # Opitional parameter            use_sticky_node=False, # Opitional parameter            blockchain_iid=int(os.getenv("BLOCKCHAIN_IID", "0")) # Opitional parameter        )        # Create blockchain client        client = await BlockchainClient.create(settings)}
```

seed_products function builds and sends a blockchain transaction to create a product in the database of the node .

./seed_products.py
```python
async def seed_products(client: BlockchainClient):    """Example of how to create and send a transaction"""    try:        # Create private/public key pair        private_bytes = bytes.fromhex(os.getenv("PRIV_KEY"))        private_key = PrivateKey(private_bytes, raw=True)        public_key = private_key.pubkey.serialize()                # Create operation        operation = Operation(            op_name="seed_data",            args=[]        )                # Create transaction        transaction = Transaction(            operations=[operation],            signers=[public_key],            signatures=None,            blockchain_rid=client.config.blockchain_rid        )                # Sign the transaction        signed_transaction = await client.sign_transaction(            transaction,            private_bytes        )                # Send transaction and wait for confirmation        receipt = await client.send_transaction(            signed_transaction,            do_status_polling=True        )                print(f"\nTransaction status: {receipt.status}")        if receipt.status == ResponseStatus.CONFIRMED:            print("Transaction confirmed!")        else:            print(f"Transaction failed: {receipt.message}")                except Exception as e:        print(f"Transaction failed: {str(e)}")
```

get_products_paginated function retrieves products from the database paginataed.

./get_products_paginated.py
```python
async def get_products_paginated(client: BlockchainClient, cursor: None):    """Example of how to query the blockchain"""    try:        return await client.query("get_products_paginated",             {"page_size": 1000, "page_cursor": cursor})    except Exception as e:        print(f"Query failed: {str(e)}")
```

This codesnippet demomstates how to use the query with pagination.
The main idea is to call the query in the loop until cursor equals to None.

./get_products_paginated.py
```python
products_all = []        products = await get_products_paginated(client, None)        products_all = products_all + products['data']        while products['next_cursor'] != None:            products = await get_products_paginated(client, products['next_cursor'])            products_all = products_all + products['data']
```
