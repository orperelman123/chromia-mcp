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
