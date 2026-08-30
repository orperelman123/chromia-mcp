# Chromia dapp overview

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Chromia dapp overviewOn this page
# Chromia dapp overview

This diagram shows the different layers of a dapp on the Chromia platform. Similar to Web2, it has a frontend, backend, and database. The backend is written in the programming language Rell and gets deployed to the Chromia platform, where it's run on nodes hosted by a decentralized network of system providers. Dapp state gets stored in a database that's automatically managed by the Chromia core software and duplicated across multiple nodes. Transactions that mutate data in the database get stored in a blockchain, which is also duplicated across multiple nodes.

## Frontend​

Just as in Web2, the frontend remains the user's first point of interaction. Developers continue to use familiar technologies such as HTML, CSS, and JavaScript to craft user interfaces.
This ensures a continued focus on user experience, responsiveness, and aesthetics.

To create or update data in our dapp, we send transactions to the Chromia blockchain network.
We can use tools like the Chromia TypeScript client library, postchain-client, to make this as easy as sending data to a traditional API. We'll illustrate this with real code examples in the next section.

For a transaction to be valid it needs to be signed with the user's cryptographic keys.
A transaction signature acts like a digital fingerprint, unique to both the transaction and the user who created it.
It verifies that the sender is who they claim to be and that the transaction has not been tampered with after being sent.

Cryptographic keys are typically stored in a wallet app on the user's device. When a user wants to send a signed transaction to interact with a dapp on the Chromia network, the dapp frontend uses the wallet app's API to request and generate a signature. This signature is then attached to the transaction, making it valid and secure.

That's it; the wallet generates a digital signature, the signature is attached to the transaction, and the transaction is sent, with the signature, to a Chromia node running the dapp backend.
This shows that the transition to Web3 using Chromia is not that big of a step from Web2.
We will dive deeper into this in the next lesson, but first, let's look at the backend.

## Backend​

Dapp backends on the Chromia platform are written in Rell, Chromia's relational programming language.
Rell is designed to be easy to learn and convenient to use for any developer, especially if you already know SQL.

Rell code is deployed to a so-called container on the Chromia platform, and each dapp has its own blockchain managed by the Chromia network. As soon as a dapp is deployed, you can send transactions and query data via your dapp frontend.
Deploying Rell code will automatically create the necessary tables and relations to hold a dapp's state in a PostgreSQL database, automatically managed and duplicated across multiple Chromia nodes.

Each interaction with the backend is similar to sending a request to a traditional Web2 API.
Sending a transaction to a Chromia node is like making a POST request to a web service. Besides signed transactions, you can also send read-only queries to a node, which is similar to making a traditional GET request.
This is all you have to think about as a developer, so the jump from Web2 is not that big, but let's look at what happens behind the scenes when you send a transaction, for the sake of curiosity.

A Chromia node receives your transaction and begins a round of communication among the other Chromia nodes running your dapp (also known as validators). Together, they ensure that a "supermajority" agrees on the transaction's validity before it is executed. This is similar to multiple servers performing cross-checks before updating a database in a distributed Web2 system.

When two-thirds of validator nodes agree that the transaction is valid, a consensus has been reached, and the transaction is executed.
Executing a transaction means that the operations contained in it are executed. (We'll look more closely at what operations are in the next section.)
This results in state changes that are applied to the dapp's database, similar to performing database actions in a Web2 backend.
The transaction is also recorded on the blockchain, ensuring that it is immutable and reflected across all nodes.

This sums up the Chromia Web3 stack. Again, we can see it is not an entirely different world. Web3 and Web2 have many similar concepts and terminology.
