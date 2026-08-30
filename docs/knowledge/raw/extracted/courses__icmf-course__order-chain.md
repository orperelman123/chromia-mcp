# Order chain (send message)

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Order chainOn this page
# Order chain (send message)

The order chain will contain the logic for buying an item, similar to e-commerce. The chain will then automatically send
a message to the factory and delivery chains that a new order has been completed.

## Configuration​

We start by configuring the order chain to use the &sender part that we defined earlier. Open the chromia.yml and
add the following to the blockchains property:

chromia.yml
```yaml
blockchains:  order_chain:    module: order_chain    config:      
This should be enough to enable the infrastructure needed for the dapp to emit messages.

## Database Model​

Let’s create a new directory named order_chain. Inside this directory, create a file called module.rell. This file
represents the module definition and must have the following content:

src/order_chain/module.rell
```rell
module;
```

We want to track which orders have been processed and their contents. Create a new file called entities.rell in the
src/order_chain directory, and add the following code:

- Rell
- Entity Relationsrc/order_chain/entities.rell
```rell
entity order {    key tx: byte_array = op_context.transaction.tx_rid;    index customer_id: integer;    address: text;}entity product {    id: integer;}entity product_order {    key order, product;    quantity: integer;}
```

In the code, we model a many-to-many relationship between an order and a product by the product_order entity that
contains the quantity ordered of that product. Let's also add a few queries to the queries.rell file to be able to
read the data from a client:

src/order_chain/queries.rell
```rell
query list_orders() {    val ordered_products = product_order @* {} (.order.rowid.to_integer(), (product_id = .product.id, quantity = .quantity));    return group_products_by_id(ordered_products);}query get_order_id(tx: byte_array) = order @? { tx }.rowid;query get_order_details(id: integer)    = product_order @* { .order.rowid == rowid(id) } ( order_id = .order.rowid, product_id = .product.id, quantity = .quantity );query get_order_details_by_tx(tx: byte_array) = get_order_details(require(get_order_id(tx)?.to_integer()));
```

Create a new file named functions.rell in the src/order_chain directory. It will store the helper functions
presented below:

src/order_chain/functions.rell
```rell
function group_products_by_id(value: list) {    val result = map>();    for ((k, v) in value) {        if (k not in result) result[k] = list();        result[k].add(v);    }    return result @* {} (order_id = $[0], details = $[1]);}
```

We also need a simple way to add products to our catalog. Create a new file called operations.rell in the
src/order_chain directory and insert the following code:

src/order_chain/operations.rell
```rell
operation register_product(id: integer) {    create product ( id );}
```

## Sending a message​

Now, we are ready to create a customer order and notify other chains that this event has happened. We do this by
importing the message types and messaging utilities to the src/order_chain/module.rell file:

src/order_chain/module.rell
```rell
module;import messages.{ msg, topic };import lib.icmf.{ send_message };
```

Next, we have to define an operation that the customer can call to make an order. Add the following code to the
src/order_chain/operations.rell file:

src/order_chain/operations.rell
```rell
operation make_customer_order(details: msg.order_details) {    val order = create order (        customer_id = details.customer_id,        address = details.address    );    val product_to_ids = product @* { .id in details.products @* { }.id }( $, .id );    val ordered_products = list>();    for ((p, id) in product_to_ids) {        ordered_products.add(        struct(            order = order,            product = p,            quantity = details.products @ { .id == id }.quantity        )        );    }    create product_order ( ordered_products );    val order_id = order.rowid.to_integer();    send_production_order(order_id, details.products);    send_new_delivery(order_id, details.customer_id, details.address);}
```

Also, add the following functions to the src/order_chain/functions.rell file. These functions allow sending an order
to production and also initiate the delivery.

src/order_chain/functions.rell
```rell
function send_production_order(order_id: integer, products: list) {    send_message(        topic.PRODUCTION_ORDER,        msg.production_details(order_id, products).to_gtv()    );}function send_new_delivery(order_id: integer, customer_id: integer, shipping_address: text) {    send_message(        topic.NEW_DELIVERY,        msg.delivery_details(order_id, customer_id, shipping_address).to_gtv()    );}
```

The operation takes a struct containing order details as input. It then creates the order and product_order entities
before sending the production order and new delivery message. We wrap the calls to
send_message(topic: text, body: gtv) to make the code more readable.

## Testing​

Let's configure a new test module for the order chain by adding the following to the blockchains:order_chain part in
the chromia.yml.

chromia.yml
```yaml
blockchains:  order_chain:    # ↓↓↓ Add this code snippet ↓↓↓    test:      modules:        - test.order_chain_test    # ↑↑↑ Add this code snippet ↑↑↑
```

Ceate a new file order_chain_test.rell in the src/test directory and add the following code:

src/test/order_chain_test.rell
```rell
@test module;import order_chain.{ register_product, make_customer_order, order };import messages.{ msg, topic };function test_make_order() {    val test_order = msg.order_details(0, "MyStreet 12", [msg.product(id = 12, quantity = 110)]);    rell.test.tx().op(register_product(12)).run();    rell.test.tx().op(make_customer_order(test_order)).run();    val order_id = order @ { } ( .rowid.to_integer() );    assert_events(        (        "icmf_message",        (topic = topic.PRODUCTION_ORDER,        body = msg.production_details(order_id, test_order.products).to_gtv())            .to_gtv_pretty()),        ("icmf_message",        (topic = topic.NEW_DELIVERY,        body = msg.delivery_details(order_id, customer_id = test_order.customer_id, test_order.address).to_gtv())            .to_gtv_pretty())    );}
```

In the test, we register a product, create a new customer order, and assert that the events have been created correctly.
It should now be possible to run the tests and get the following output:

```bash
chr test
```

Result
```bash
Running tests for chain: order_chainTEST: order_chain_test:test_make_orderOK: order_chain_test:test_make_order (0,628s)------------------------------------------------------------TEST RESULTS:OK order_chain_test:test_make_orderSUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL (0,628s)
```

Below are the project structure and the contents of the chromia.yml configuration file after the changes have been
made. It can be handy for comparison at this stage of the course:

Project structure

```text
order-system-example/ ├── build/ ├── src/ │   ├── lib/ │   ├── order_chain/ │   │   ├── entities.rell │   │   ├── functions.rell │   │   ├── module.rell │   │   ├── operations.rell │   │   └── queries.rell │   ├── test/ │   │   └── order_chain_test.rell │   └── messages.rell ├── .gitignore └── chromia.yml
```

Final version of the chromia.yml file

chromia.yml
```yaml
definitions:  - &sender # Configuration for a chain that sends messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"  - &receiver # Base configuration for a chain that receives messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"  - &sender_receiver # Base configuration for a chain that will both send and receive messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"blockchains:  order_chain:    module: order_chain    config:
