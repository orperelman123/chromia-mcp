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
