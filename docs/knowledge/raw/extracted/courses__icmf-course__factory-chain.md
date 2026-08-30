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
