# Confirm Events Across Blockchains

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Confirm Events Across Blockchains

In this course, you’ll learn how to build a simple digital warehouse app that lets users prove they made a payment on
one blockchain, and then use that proof on another blockchain. This is done using Chromia’s Inter-Chain Confirmation
Facility (ICCF).

What is ICCF?

ICCF (Inter-Chain Confirmation Facility) is a tool that lets you prove that a transaction happened on one blockchain
and have another blockchain recognize that proof. It’s ideal for situations where you need to confirm an event.

ICCF makes it easy to show that something happened on one blockchain and have another blockchain recognize it. For
example, you can prove that a payment was made on Chain A, and then use that proof to unlock something on Chain B. The
process is user-driven: after making a transaction, the user collects a proof and presents it to the other chain
whenever they want.

This method is best for situations where you need to prove an event happened. If you want to send messages or data
between chains, Chromia’s [Inter-Chain Messaging Facility (ICMF)](https://docs.chromia.com/intro/about/protocols/icmf) might be a better fit.

What is a Cluster Anchoring Chain?

A special blockchain in the Chromia network that collects and anchors data from all blockchains in a dapp cluster. It
helps ensure data integrity and security by making it easy to detect tampering or unauthorized changes across the
cluster.

## Why use ICCF?​

- User-driven and asynchronous: Users can collect and present proofs at any time after their transaction is
confirmed, making the process flexible and efficient.

- Security: The event is anchored and the proof is verified by a trusted chain.

- Separation of concerns: One chain can handle payments, while another manages business logic or access control.

## What will you build?​

By the end of this course, you’ll have built a digital warehouse app where:

- Users make a fictional payment on one blockchain.

- They receive a proof of payment.

- They use that proof to gain access or perform actions on another blockchain.

## Example use cases​

- Cross-chain subscriptions: Prove that a user paid for a subscription on one chain to unlock services on another.

- Multi-chain access control: Grant access to digital goods or services on one blockchain based on actions (like
payments) on another.

- Decentralized identity verification: Prove identity or credentials issued on one chain to another chain’s
application.

- Gaming: Unlock in-game items or features on one blockchain based on achievements or purchases on another.

- Event ticketing: Prove ticket purchase on one chain to gain entry or benefits on another chain’s system.

## How does it work?​

Confirming an event (transaction) can be described by the following sequence:

- The user sends a transaction to the source chain.

- The source chain confirms the transaction and includes it in a block.

- The block is sent to the Cluster Anchoring Chain as a transaction.

- The Cluster Anchoring Chain verifies the block (anchors) and includes it in its own block.

- The user constructs a proof by obtaining a confirmation proof for the transaction.

- The user presents the proof to the target chain as another transaction.

- The target chain validates the proof by verifying its anchoring in the Cluster Anchoring Chain.

- The target chain completes its verification by including the proof in a block.

As seen in this flow, the user drives this operation. The user constructs and presents the proof, making this step
asynchronous and possible anytime after the blocks have been confirmed. This approach is particularly suitable for
applications focusing on proving occurrences rather than transferring data. While it can be used for sending messages,
if the user is not concerned with verifying when the message arrives, using the messaging facility (ICMF) might be more
appropriate.

In this course, we will explore a scenario where one blockchain handles monetary transactions and subscriptions while
another chain manages the business logic. This setup allows us to prove that a monetary transaction occurred without the
money leaving the first chain.

## Related materials​

This course relies on the following documentation, which can help you understand the underlying concepts and approaches:

| 
| Section| Type| Documentation
| Overview| Cross-chain| [ICCF](https://docs.chromia.com/intro/cross-chain/icmf)
| FT4| Introduction| [FT4](https://docs.chromia.com/ft4/intro)
| Overview| Dapps| [Building your dapps on Chromia](https://docs.chromia.com/intro/getting-started/create-dapp/)

## Repository link​

The complete code repository for this course is available here:
[ICCF course repository](https://bitbucket.org/chromawallet/iccf-course).
