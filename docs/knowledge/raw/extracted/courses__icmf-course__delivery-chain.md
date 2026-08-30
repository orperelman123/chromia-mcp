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
