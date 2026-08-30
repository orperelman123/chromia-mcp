# COURSE icmf-course — 7 pages


===== FILE: courses__icmf-course__defining-messages.md =====

# Define messages

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - Define messages
# Define messages

Consider the following steps involved when ordering a product through our system:

- The user creates an order and sends the details to the Order Chain (OC).

- OC forwards the production requirements to the Factory Chain (FC).

- OC also communicates the delivery details to the Delivery Chain (DC).

- The factory manufactures the ordered products.

- Delivery prepares to deliver the products once ready.

- FC notifies DC that the order is prepared for shipping.

- Delivery completes the delivery of the order to the user (off-chain).

- The user confirms receipt of the delivery.

Let's model this in Rell by defining three topics, one for each message we want to send between the chains and the
content for each message. Create a new file called messages.rell in the src directory and add the following code to
it:

src/messages.rell
```rell
module;namespace topic {    val PRODUCTION_ORDER = "L_production";    val NEW_DELIVERY = "L_delivery";    val SHIPMENT_READY = "L_shipment_ready";}namespace msg {    struct order_details {        customer_id: integer;        address: text;        products: list;    }    struct product {        id: integer;        quantity: integer;    }    // Order -> Factory    struct production_details {        order_id: integer;        products: list;    }    // Order -> Delivery    struct delivery_details {        order_id: integer;        shipping_address: text;        customer_id: integer;    }    // Factory -> Delivery    struct shipment_ready {        order_id: integer;    }}
```

The prefix L_ is essential for ICMF, restricting message access to the sender's cluster. System blockchainsalone have the privilege to send global messages, denoted by the G_ prefix. :::


===== FILE: courses__icmf-course__delivery-chain.md =====

# Delivery chain (receive message)

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - Delivery chainOn this page
# Delivery chain (receive message)

For the delivery chain, we only need to think about two things: we want to keep track of which orders we should deliver
but must also take into account if, for some reason, the message is delayed such that the shipment becomes ready before
it was created. This would never happen in a real-life scenario, but we'll consider it in this example.

Let’s create a new module file module.rell in the src/delivery_chain directory to hold the following definition:

src/delivery_chain/module.rell
```rell
module;
```

Next, we need to define an enum in the src/delivery_chain/entities.rell file, that will contain three states associated
with an order: created, dispatched, and delivered.

src/delivery_chain/entities.rell
```rell
enum shipping_state {    CREATED,    DISPATCHED,    DELIVERED}entity delivery {    key order_id: integer;    index customer_id: integer;    shipping_address: text;    mutable shipping_state;}// orders that can be dispatched but have not yet been createdentity pending_delivery {    key order_id: integer;}
```

Add a new operation to the src/delivery_chain/operations.rell file to change the status of the shipment to be delivered:

src/delivery_chain/operations.rell
```rell
operation accept_delivery(order_id: integer) {    require(delivery @ { order_id }.shipping_state == shipping_state.DISPATCHED, "Order must be dispatched before it can be completed");    update delivery @ { order_id } ( shipping_state = shipping_state.DELIVERED);}
```

To get the current delivery status, you have to add the corresponding query to the src/selivery_chain?queries.rell file:

src/delivery_chain/queries.rell
```rell
query get_delivery_details(order_id: integer) = delivery @* { order_id } ($.to_struct());query list_deliveries() = delivery @* {} ($.to_struct());
```

## Receiving a message​

To be able to receive a message, we must first configure our new blockchain and add a reference to the receiver that
we defined in the Introduction section. Add the following delivery_chain details to the blockchains property in the
chromia.yml file:

chromia.yml
```yaml
blockchains:  # ↓↓↓ Add this code snippet ↓↓↓  delivery_chain:    module: delivery_chain    config:      
In this configuration, we make the dapp subscribe to the topics L_delivery and L_shipment_ready, as defined by our
dapp code. The brid should point to the producer of the message topic, but since we have not deployed our dapp to the
network yet, we leave it as null.

infoNote that null only works when testing locally. When deploying to a real network, the producer chain must be
deployed first to obtain the brid to be specified in this field.

To handle two message types, we add the following imports to the src/delivery_chain/module.rell file:

src/delivery_chain/module.rell
```rell
import messages. { topic.*, msg };import lib.icmf.receiver.{ receive_icmf_message };
```

We also define a new extension called receive_icmf_message in the functions.rell file:

src/delivery_chain/functions.rell
```rell
@extend(receive_icmf_message)function (sender: byte_array, topic: text, body: gtv) {    when (topic) {        NEW_DELIVERY -> handle_new_delivery(msg.delivery_details.from_gtv(body));        SHIPMENT_READY -> handle_shipment_ready(msg.shipment_ready.from_gtv(body));        else -> log("Message type %s not handled".format(topic));    }}function handle_new_delivery(msg: msg.delivery_details) {    val state = pending_delivery @? { msg.order_id } (shipping_state.DISPATCHED) ?: shipping_state.CREATED;    create delivery(        order_id = msg.order_id,        customer_id = msg.customer_id,        shipping_address = msg.shipping_address,        shipping_state = state    );    delete pending_delivery @? { msg.order_id };}function handle_shipment_ready(msg: msg.shipment_ready) {    if (not exists(delivery @? { .order_id == msg.order_id })) {        create pending_delivery ( msg.order_id );    } else {        update delivery @ { .order_id == msg.order_id } ( shipping_state = shipping_state.DISPATCHED );    }}
```

The extension checks the topic and switches behavior accordingly. If the message topic is not handled, we fail the
operation, halting the block building process due to incorrect configuration. We handle two message types by creating or
updating entities to mark the delivery with the correct state.

## Testing​

To test the delivery chain, we need to add new configuration details to the chromia.yaml file. Insert the test
property under the delivery_chain section, along with the necessary details.

chromia.yml
```yaml
blockchains:  delivery_chain:    # ↓↓↓ Add this code snippet ↓↓↓    test:      modules:        - test.delivery_chain_test    # ↑↑↑ Add this code snippet ↑↑↑
```

We then create a test that emits events using ICMF test utilities and ensures that the correct shipment orders and
states are created. Create a file called delivery_chain_test.rell in the src/test directory and insert the following
code:

src/test/delivery_chain_test.rell
```rell
@test module;import delivery_chain.{ delivery, accept_delivery, shipping_state };import messages.{ msg, topic };import lib.icmf.test.{ test_icmf_message };function test_make_delivery() {    rell.test.tx().op(            test_icmf_message(                x"",                topic.NEW_DELIVERY,                msg.delivery_details(                    order_id = 1,                    customer_id = 10,                    shipping_address = "MyStreet 101"                )                    .to_gtv()            )        )        .run();    assert_equals(delivery @ { .order_id == 1 }.shipping_state, shipping_state.CREATED);    rell.test.tx().op(accept_delivery(1)).run_must_fail("must be dispatched before");    rell.test.tx().op(            test_icmf_message(                x"",                topic.SHIPMENT_READY,                msg.shipment_ready(order_id = 1)                    .to_gtv()            )        )        .run();    assert_equals(delivery @ { .order_id == 1 }.shipping_state, shipping_state.DISPATCHED);    rell.test.tx().op(accept_delivery(1)).run();}
```

Then run the chr test command to verify the test results.

```bash
Running tests for chain: delivery_chainTEST: order_chain_test:test_make_deliveryOK: order_chain_test:test_make_order (0,837s)------------------------------------------------------------TEST RESULTS:OK delivery_chain_test:delivery_make_orderSUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL (0,837s)
```

Below are the project structure and the contents of the chromia.yml configuration file after the changes have been made.
It can be handy for comparison at this stage of the course:

Project structure

```text
order-system-example/├── build/├── src/│   ├── delivery_chain/│   │   ├── entities.rell│   │   ├── functions.rell│   │   ├── module.rell│   │   ├── operations.rell│   │   └── queries.rell│   ├── lib/│   ├── order_chain/│   │   ├── entities.rell│   │   ├── functions.rell│   │   ├── module.rell│   │   ├── operations.rell│   │   └── queries.rell│   ├── test/│   │   ├── delivery_chain_test.rell│   │   └── order_chain_test.rell│   └── messages.rell ├── .gitignore└── chromia.yml
```

Final version of the chromia.yml

chromia.yml
```yaml
definitions:  - &sender # Configuration for a chain that sends messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"  - &receiver # Base configuration for a chain that receives messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"  - &sender_receiver # Base configuration for a chain that will both send and receive messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"blockchains:  order_chain:    module: order_chain    config:


===== FILE: courses__icmf-course__factory-chain.md =====

# Factory chain (send and receive)

URL: https://learn.chromia.com

- [Home](/)
- Lesson 4 - Factory chainOn this page
# Factory chain (send and receive)

The final chain in our example is the factory chain. The chain should react to production orders emitted from the order
chain and should notify when an order is ready for shipment. These would likely be separate steps in a real-world case,
but we will simplify this in our example.

We can say a product is 'manufactured' by increasing its value and reemitting the message directly. The database model
consists of only a single entity. To encapsulate the logic for this new blockchain, we create a new module
factory_chain in the src/factory_chain directory with the following content:

src/factory_chain/module.rell
```rell
module;import lib.icmf.{ send_message };import lib.icmf.receiver.{ receive_icmf_message };import messages.{ topic.*, msg };
```

Define the corresponding manufactured product entity in the src/factory_chain/entities.rell file:

src/factory_chain/entities.rell
```rell
entity manufactured_product {    key id: integer;    mutable quantity: integer;}
```

Additionally, you need to add two queries to fetch the quantity of a manufactured product and the total number of
manufactured products to the src/factory_chain/queries.rell file.

src/factory_chain/queries.rell
```rell
query get_total_manufactured(id: integer) = manufactured_product @? { id }.quantity;query get_total_manufactured_products() = manufactured_product @* {} ( $.to_struct() );
```

## Sending and receiving a message​

To send and receive messages in a single chain, we will apply what we have learned from previous sections in our
configuration. Open the chromia.yml file and insert the factory_chain property, along with its underlying
configuration details, into the blockchains section to create a new blockchain.

chromia.yml
```yaml
blockchains:  # ↓↓↓ Add this code snippet ↓↓↓  factory_chain:    module: factory_chain    config:      
Insert the following code to the functions.rell file to handle our messages.

src/factory_chain/functions.rell
```rell
@extend(receive_icmf_message)function (sender: byte_array, topic: text, body: gtv) {    when (topic) {        PRODUCTION_ORDER -> {            val order = msg.production_details.from_gtv(body);            for (product in order.products) manufacture_product(product);            send_message(SHIPMENT_READY, msg.shipment_ready(order.order_id).to_gtv());        }        else -> log("Message type %s not handled".format(topic));    }}function manufacture_product(product: msg.product) {    if (not exists(manufactured_product @? { .id == product.id})) {        create manufactured_product( id = product.id, quantity = product.quantity);    } else {        update manufactured_product @ { .id == product.id } (quantity += product.quantity);    }}
```

## Testing​

To test the factory chain, we need to add new configuration details to the chromia.yaml file. Insert the test
property under the factory_chain section, along with other details.

chromia.yml
```yaml
blockchains:  factory-chain:    # ↓↓↓ Add this code snippet ↓↓↓    test:      modules:        - test.factory_chain_test    # ↑↑↑ Add this code snippet ↑↑↑
```

The test emits a message and the dapp responds to it by emitting an event. Create a new file factory_chain_test.rell
in the src/test directory and insert the following code:

src/test/factory_chain_test.rell
```rell
@test module;import factory_chain.{ manufactured_product };import messages.{ msg, topic.* };import lib.icmf.test.{ test_icmf_message };function test_manufacture_order() {    rell.test.tx().op(            test_icmf_message(                x"",                PRODUCTION_ORDER,                msg.production_details(                    order_id = 1,                    products = [msg.product(id = 2, quantity = 10)]                )                    .to_gtv()            )        )        .run();    assert_events(("icmf_message", (topic = SHIPMENT_READY, body = msg.shipment_ready(1).to_gtv()).to_gtv_pretty()));    assert_equals(manufactured_product @ { .id == 2 }.quantity, 10);}
```

Then run the chr test command to verify the test results.

```bash
Running tests for chain: factory-chainTEST: factory_chain_test:test_manufacture_orderOK: factory_chain_test:test_manufacture_order (0.688s)------------------------------------------------------------TEST RESULTS:OK factory_chain_test:test_manufacture_orderSUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL (0.688s)***** OK *****
```

Now, you have successfully written a dapp that uses three separate blockchains. In the final section, we will wrap
things up by running the blockchains locally and testing the flow.

Below you can find the project structure and the contents of the chromia.yml configuration file after the changes have
been made. It can be handy for comparison at this stage of the course:

Project structure

```text
order-system-example/ ├── build/ ├── src/ │   ├── delivery_chain/ │   │   ├── entities.rell │   │   ├── functions.rell │   │   ├── module.rell │   │   ├── operations.rell │   │   └── queries.rell │   ├── factory_chain/ │   │   ├── entities.rell │   │   ├── functions.rell │   │   ├── module.rell │   │   └── queries.rell │   ├── lib/ │   ├── order_chain/ │   │   ├── entities.rell │   │   ├── functions.rell │   │   ├── module.rell │   │   ├── operations.rell │   │   └── queries.rell │   ├── test/ │   │   ├── delivery_chain_test.rell │   │   ├── factory_chain_test.rell │   │   └── order_chain_test.rell │   └── messages.rell ├── .gitignore └── chromia.yml
```

Final version of the chromia.yml file

chromia.yml
```yaml
definitions:  - &sender # Configuration for a chain that sends messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"  - &receiver # Base configuration for a chain that receives messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"  - &sender_receiver # Base configuration for a chain that will both send and receive messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"blockchains:  order_chain:    module: order_chain    config:


===== FILE: courses__icmf-course__introduction.md =====

# Build an event-driven multi-blockchain dapp

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Build an event-driven multi-blockchain dapp

In this course, you’ll learn how to build a decentralized application (dapp) that consists of three blockchains: Order,
Factory, and Delivery. These blockchains will interact with each other using Chromia’s Inter-Chain Messaging Facility
(ICMF).

What is ICMF?

The Inter-Chain Messaging Facility (ICMF) is a system in the Chromia network that allows different blockchains to send
and receive messages or events between each other automatically, without user intervention.

- Event-driven communication: Chains can broadcast messages (events) on specific topics, and other chains can
subscribe to these topics to react when something happens.

- Separation of concerns: Each chain can focus on its own responsibilities and only respond to relevant events
from other chains.

- Scalable and flexible: New chains can be added by simply subscribing to the topics they care about, without
changing the logic of existing chains.

ICMF is ideal for building multi-blockchain dapps where different parts of the system need to coordinate actions, such
as in supply chain, gaming, or modular business applications.

[Learn more about ICMF](https://docs.chromia.com/intro/cross-chain/icmf)

The dapp you’ll build is an ordering system with three blockchains:

- Ordering chain: Users can order products using this chain.

- Factory chain: Produces products and keeps statistics of how many products have been created.

- Delivery chain: Handles delivery of orders to customers.

The main idea is to separate concerns for different actors. Products are made available on the order chain and can be
ordered by a user. Factory workers only need to interact with the factory chain. Delivery companies only need to
interact with the delivery chain. This separation makes the system modular and scalable.

There are other ways to split a dapp into several blockchains, for example, by sharding (having several factories for
different product types) to parallelize the workload or to achieve different time scales. But this example is a great
way to learn how to use ICMF.

The system can be modeled like this:

ICMF works like a message queue: a chain can emit events on a specific topic, and other chains can subscribe to those
topics. This is a core part of Chromia’s architecture, enabling blockchains to communicate without user interaction.

## Example use cases​

- Supply chain management: Each stage (ordering, manufacturing, delivery) is managed by a separate chain. When an
order is placed, the order chain notifies the factory chain to start production, and the factory chain notifies the
delivery chain when the product is ready.

- Gaming: Different chains can handle player actions, in-game assets, and tournaments. When a player achieves
something in one chain, a message can trigger rewards or events in another.

- Decentralized finance (DeFi): Separate chains for lending, trading, and collateral management can communicate
events like loan approval or liquidation, triggering actions across the ecosystem.

- Modular business applications: HR, payroll, and project management can each run on their own chain, sending
messages to coordinate actions like onboarding a new employee or starting a new project.

- IoT and automation: Device chains can emit events (like sensor readings or alerts) that trigger actions on control
or analytics chains.

## How does ICMF work?​

Sending a message using ICMF is easy in Rell, but here’s what happens under the hood:

- The sender dapp calls the function send_message from the ICMF Rell library. This could be triggered by a user or by
the dapp itself.

- This emits an event on the node, which is sent to the cluster's anchoring chain.

- The receiver node polls for messages on the subscribed topics before each block is built.

- When a message is found, the node calls the __icmf_message special operation on the dapp.

- The ICMF library calls the function receive_icmf_message, which triggers any logic defined by the dapp.

This approach is perfect for dapps where you want chains to broadcast a message when an action is completed, and let
other chains subscribe and react to those actions. You can add new chains with new responsibilities by simply
subscribing to the topics they care about—no changes are needed in the sender chain.

For example, in this course:

- Factory workers don’t care who created an order or when; they just need to know that an order was created so they can
start manufacturing.

- The delivery company only cares about what products to deliver and where, not what’s inside the box.

## Related materials​

This course relies on the following documentation, which can help you understand the underlying concepts and approaches:

| 
| Section| Type| Documentation
| Overview| Cross-chain| [ICMF](https://docs.chromia.com/intro/cross-chain/iccf)
| FT4| Introduction| [FT4](https://docs.chromia.com/ft4/intro)
| Overview| Dapps| [Building your dapps on Chromia](https://docs.chromia.com/intro/getting-started/)

## Repository link​

The complete code repository for this course is available here:
[ICMF course repository](https://bitbucket.org/chromawallet/icmf-course).


===== FILE: courses__icmf-course__manual-testing.md =====

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


===== FILE: courses__icmf-course__order-chain.md =====

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


===== FILE: courses__icmf-course__setup.md =====

# Set up your project

URL: https://learn.chromia.com

- [Home](/)
- Set up your projectOn this page
# Set up your project

Before we start, please make sure you have the following prerequisites in place:

Set up PostgreSQL database

# Set up PostgreSQL database

Rell requires PostgreSQL 16.3. The IDE can work without it but can't run a node. A console or a remote postchain app can
run without a database.

The default database configuration for Rell is:

- database: postchain

- user: postchain

- password: postchain

## Install​

- Mac
- Linux
- Docker
- Windows

- 
Install Homebrew: [Homebrew installation guide](https://brew.sh/)

- 
Install PostgreSQL:

```shell
brew install postgresql@16brew services start postgresql@16createuser -s postgres
```

- 
Prepare the PostgreSQL database:

```plsql
psql -U postgres -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8';" -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

noteIf you get an error saying peer authentication failed, you must change the authentication method from peer to
md5. You can change it in the pg_hba.conf file of your psql database.

- 
Install PostgreSQL:

```bash
sudo apt install postgresql-16
```

- 
Prepare the PostgreSQL database:

```bash
sudo -u postgres psql -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8';" -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

- 
Install Docker: [Docker installation guide](https://docs.docker.com/engine/install/)

- 
Prepare the PostgreSQL database:

```dockerfile
docker run --name postgres -e POSTGRES_USER=postchain -e POSTGRES_PASSWORD=postchain -p 5432:5432 -d postgres:16.3-alpine3.20
```

noteWe use the Alpine version of PostgreSQL because it provides the correct collation settings by default. This can be
explicitly set using the environment variable:

```bash
POSTGRES_INITDB_ARGS="--lc-collate=C.UTF-8 --lc-ctype=C.UTF-8 --encoding=UTF-8"
```

- 
Download the PostgreSQL installer from the [official website](https://www.postgresql.org/download/windows/).

- 
Install the executable and add the PostgreSQL folder containing the binaries to your environment variables. Open the
Command Prompt (CMD) and run the following command, ensuring that you set the path to your binaries correctly and
replace the <version> placeholder with the actual version number:

```shell
setx POSTGRESQL "C:\Program Files\PostgreSQL\\bin"
```

- 
Reopen the Command Prompt (CMD) to update the environment variables. Prepare the PostgreSQL database by running the
following two commands sequentially:

```plsql
psql -U postgres -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' ENCODING 'UTF-8';"psql -U postgres -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

noteDepending on your Windows version, you may encounter various errors related to permissions or incorrect PostgreSQL
installations. If this happens, we recommend installing Docker and deploying the PostgreSQL container.

Install Chromia CLI

# Install Chromia CLI

This topic contains instructions to install and update the
[Chromia CLI](https://gitlab.com/chromaway/core-tools/chromia-cli).

## Prerequisite​

Before proceeding, make sure the following prerequisites are met:

- PostgreSQL database: See [Set up PostgreSQL database](/docs/install/database-setup).

- RELL_JAVA environment: Chromia CLI do requries a java runtime (version 21 or later) to execute. Through the
different package managers this has been abstracted away for you so you don't need to set this up. If you want to
control which java runtime you use to execute Chromia CLI with it is recomended to set RELL_JAVA variable in your
environment to point to a valid Java installation

## Installation​

You can install Chromia CLI using a package manager or by downloading it directly from
[Chromia CLI Packages](https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages).

- macOS
- Linux/WSL
- WindowsTo install Chromia CLI (chr) on macOS, follow these steps:

- 
If Homebrew is not installed, install it by running:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

- 
Add the Chromia repository to Homebrew by running the following command:

```bash
brew tap chromia/core https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
```

- 
Install Chromia CLI with:

```bash
brew install chromia/core/chr
```

infoTo install a specific version of Chromia CLI, use the following commands:

```shell
brew install chromia/core/chr@brew unlink chrbrew link chr@
```
You can list available versions using: brew search chr.

- 
To verify the installation, check the version by running:

```bash
chr --version
```

To install Chromia CLI (chr) on Linux or WSL (Windows Subsystem for Linux), follow these steps:

- 
Download and add Chromia's apt-repo public key to your system’s trusted keyrings:

```bash
curl -fsSL https://apt.chromia.com/chromia.gpg | sudo tee /usr/share/keyrings/chromia.gpg
```

- 
Add the Chromia repository to your list of package sources:

```bash
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/chromia.gpg] https://apt.chromia.com stable main" | sudo tee /etc/apt/sources.list.d/chromia.list
```

- 
Run the following command to update your package sources:

```bash
sudo apt-get update
```

infoIf you added apt.chromia.com before Chromia CLI version 0.16.0, run the following command to allow the repository
update:

```bash
sudo apt-get --allow-releaseinfo-change update
```

- 
Once the repository is updated, install Chromia CLI by running:

```bash
sudo apt-get install chr
```

- 
To verify that Chromia CLI is installed successfully, check the version:

```bash
chr --version
```

To install Chromia CLI (chr) on Windows using [Scoop](https://scoop.sh/), follow these steps:

- 
If Scoop is not installed, install it by running the following command in PowerShell (run as Administrator):

```powershell
iwr -useb get.scoop.sh | iex
```

- 
Add the Chromia repository (bucket) to Scoop by running:

```powershell
scoop bucket add chromia https://gitlab.com/chromaway/core-tools/scoop-chromia/
```

- 
Add the Java bucket to Scoop by running:

```powershell
scoop bucket add java
```

This will enable scoop to download the openjdk21 which chromia-cli depends on when installing

- 
Install Chromia CLI by running:

```powershell
scoop install chr
```

- 
To verify that Chromia CLI is installed successfully, check the version:

```powershell
chr --version
```

## Updating Chromia CLI​

You can download and install the latest Chromia CLI from
[here](https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages), or if you have installed the Chromia CLI via a
package manager, you can update it with the following:

- macOS
- Linux
- Windows
```shell
brew updatebrew upgrade chr
```

```shell
sudo apt-get updatesudo apt-get install chr
```

```shell
scoop updatescoop update chr
```

## Docker​

Docker can run a standalone Linux container with the Chromia CLI pre-installed. Make sure that you have set up the
[PostgreSQL database](/docs/install/database-setup).

To use the published Docker images, you must first have Docker installed and configured on your host machine. Please
refer to the Docker documentation on how to [install Docker](https://docs.docker.com/get-docker/) on Windows, Mac, and
Linux.

### Start the Docker container with Chromia CLI pre-installed​

To run the latest version of the Chromia CLI, use the docker run command and specify the CLI Docker image name and
chr.

```shell
docker run --rm -v $(pwd):/usr/app registry.gitlab.com/chromaway/core-tools/chromia-cli/chr: chr
```

Windows Command Prompt UsersIf you're using Windows Command Prompt (cmd), the $(pwd) syntax will not work. Use one of these alternatives:

- PowerShell: Use $(Get-Location) or $(pwd)

- Command Prompt: Use %CD%
Example for Command Prompt:

```cmd
docker run --rm -v %CD%:/usr/app registry.gitlab.com/chromaway/core-tools/chromia-cli/chr: chr
```

noteMake sure to configure your chromia.yml file correctly:

- Mac: Use host.docker.internal for database:host.

- Windows: Set database:host to 172.17.0.1.

- Linux: Use the --network=host argument in Docker commands.
These configurations are crucial to ensure connectivity between Chromia CLI and the PostgreSQL instance.

See the [Docker command line reference](https://docs.docker.com/engine/reference/commandline/docker/) for more
information on updating or uninstalling the Docker image.

```bash
#!/bin/bash# Allocate a pseudo-TTY one when run in interactive modeif [ -t 0 ] && [ -t 1 ] ; then TTY="--tty"; else TTY=""; fidocker run \  # Sets the network to host to not need to change the database hostname (linux only)  --network=host \  # Set timezone based on system settings (linux only)  -e TZ=$(cat /etc/timezone) \  # Sets process ownership to current user  --user $(id -u):$(id -g) \  --mount type=bind,source="/etc/passwd",target=/etc/passwd,readonly \  --mount type=bind,source="/etc/group",target=/etc/group,readonly \  # Configures ssh-agent (only needed if chr install is called on non-public repositores)  -e SSH_AUTH_SOCK=$SSH_AUTH_SOCK \  --volume "$SSH_AUTH_SOCK:$SSH_AUTH_SOCK" \  --mount type=bind,source="${HOME}/.ssh",target=${HOME}/.ssh,readonly \  --mount type=bind,source="${HOME}/.config/jgit",target=${HOME}/.config/jgit \  # Mounts current folder into the container (Use `Get-Location` on PowerShell)  --mount type=bind,source="$(pwd)",target=/usr/app \  --interactive ${TTY} \  --rm \  registry.gitlab.com/chromaway/core-tools/chromia-cli/chr:${CHR_VERSION:-latest} chr "$@"
```

Windows Command Prompt UsersThis bash script uses $(pwd) which won't work in Windows Command Prompt. For Windows users:

- PowerShell: The script should work as-is

- Command Prompt: Replace $(pwd) with %CD% in the mount command
Modified mount line for Command Prompt:

```cmd
--mount type=bind,source="%CD%",target=/usr/app \
```

Let's kick things off by setting up your blockchain app project. Follow these steps:

- Create a new project from the template using the Chromia CLI:

```shell
chr create-rell-dapp order-system-example --template=plain
```

```shell
cd order-system-example
```

- Configure the ICMF library by adding the following to the newly generated chromia.yml file:

chromia.yml
```yaml
libs:  icmf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/icmf    tagOrBranch: 1.90.1    rid: x"B56524EE85B679371EAB1A069B03BE6A3E94F36993DEA900747BA5BAE8B99122"
```

- Run the following command to download the library. This will allow sending and receiving messages between different
blockchains:

```shell
chr install
```

- Add the following definitions to the very beginning of the chromia.yml file that will make it easier for us to
configure the chains for messaging:

chromia.yml
```yaml
definitions:  - &sender # Configuration for a chain that sends messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"  - &receiver # Base configuration for a chain that receives messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"  - &sender_receiver # Base configuration for a chain that will both send and receive messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"
```

Here we have defined an array containing three elements, each associated with an anchor: &sender, &receiver, and
&sender_receiver. Each of these anchors represents a specific configuration that a chain will adopt based on whether
it is intended to send messages, receive messages, or both.

Below you can find the project structure and also the contents of the chromia.yml configuration file after the
performed changes. It can be very useful for comparison:

Project structure

```text
order-system-example/ ├── build/ ├── src/ │   ├── lib/ │   ├── test/ │   └── main.rell ├── .gitignore └── chromia.yml
```

Final version of the chromia.yml file

chromia.yml
```yaml
definitions:  - &sender # Configuration for a chain that sends messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"  - &receiver # Base configuration for a chain that receives messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"  - &sender_receiver # Base configuration for a chain that will both send and receive messages    gtx:      modules:        - "net.postchain.d1.icmf.IcmfSenderGTXModule"        - "net.postchain.d1.icmf.IcmfReceiverGTXModule"    sync_ext:      - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"blockchains:  order-system-example:    module: maincompile:  rellVersion: 0.14.9database:  schema: schema_order_system_exampletest:  modules:    - testlibs:  icmf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/messaging/icmf    tagOrBranch: 1.29.0    rid: x"19D6BC28D527E6D2239843608486A84F44EDCD244E253616F13D1C65893F35F6"
```
