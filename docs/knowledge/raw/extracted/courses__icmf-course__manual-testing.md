# Test the dapp

URL: https://learn.chromia.com

- [Home](/)
- Lesson 5 - Test the dapp
# Test the dapp

We will test the dapp manually by starting three blockchains locally, placing a customer order, and then tracing its
path through the blockchains.

First, ensure that your chains are configured in the following order in your chromia.yml file.

chromia.yml
```yaml
blockchains:  order_chain: # internal id 1    ...  delivery_chain: # internal id 2    ...  factory-chain: # internal id 3    ...
```

The Chromia CLI will initiate the chains sequentially based on their internal IDs as defined. Knowing these IDs is
advantageous for testing purposes, as they are more convenient to handle compared to the referential ID (brid).

Let's start testing by starting the node in a separate terminal window.

```bash
chr node start --directory-chain-mock
```

We then proceed with the setup by registering two products, with ID 101 and 102:

```shell
chr tx --cid 1 register_product 101 --await
```

```shell
chr tx --cid 1 register_product 102 --await
```

Now we are ready to make our first order. The input argument to make_customer_order is a struct of type
order_details. A struct should be encoded as an array when passed through the CLI, so making an order for 1 product of
type 101 and 10 products of type 102 sent to a customer with id 1104 on address Homestreet 2 would be encoded as
[1104, "Homestreet 2", [[101, 1], [102, 10]]]. Let's make an order using these details to chain 0:

```shell
chr tx --cid 1 make_customer_order '[1104, "Homestreet 2", [[101, 1], [102, 10]]]' --await
```

Note the additional quotes around the order details to ensure our terminal parses it as a single program argument.

To verify that our order was created, we can query the order chain for all orders:

```bash
chr query --cid 1 list_orders
```

Result
```bash
[{details=[{product_id=102, quantity=10}, {product_id=101, quantity=1}], order_id=3}]
```

Let's also verify that the factory chain with ID 2 has manufactured the products with the correct amount. We will
perform the following query:

```bash
chr query --cid 3 get_total_manufactured_products
```

Result
```bash
[{id=101, quantity=1}, {id=102, quantity=10}]
```

Finally, verifying that the delivery chain, with ID 1, has created a delivery for us and has a status:

```bash
chr query --cid 2 list_deliveries
```

Result
```bash
[{customer_id=1104, order_id=3, shipping_address="Homestreet 2", shipping_state="DISPATCHED"}]
```

The shipping_state will be either CREATED or DISPATCHED depending on whether the chain has processed the second
message. If it has not, wait a few seconds. For completeness, we can mark the delivery as DELIVERED by calling the
following command:

```bash
chr tx --cid 2 accept_delivery 3
```

Congratulations! You have now built your first multi-chain app!
