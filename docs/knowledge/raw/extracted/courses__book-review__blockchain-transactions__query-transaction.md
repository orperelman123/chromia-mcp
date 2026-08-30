# Understanding blockchain state and transactions

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 5 - Understand blockchain state and transactions](/courses/book-review/blockchain-transactions/)
- Understanding blockchain state and transactionsOn this page
# Understanding blockchain state and transactions

Let's explore how transactions are stored and how the state is managed on Chromia. To illustrate this, let’s refer back to the diagram from Lesson 1 that showed the flow of a create_book transaction:

- The user signs the transaction create_book("ISBN1234", "1984", "George Orwell").

- The transaction is sent to the Chromia Node, where it is validated and then sent to the blockchain.

- Inside the operation, the call create_book(isbn = isbn, title = title, author = author); is applied to the DApp table state using Rell.

- If the transaction is confirmed, it is added to a block on the blockchain, and the DApp table state is updated.

This diagram illustrates how a transaction is stored on the blockchain and how the state is updated based on the operation within the transaction. Let’s delve into the details of this process to better understand the differences between blockchain and state.

We start by making a query to fetch the state using the query get_all_books.

## Querying state for all books​

When we execute a query like get_all_books, we interact only with the DApp table state. This makes sense because we want to fetch the current state of the books. Here’s the flow for this query:

In this sequence:

- The user queries the Chromia Node for all books.

- The Chromia Node forwards the query to Rell.

- Rell performs a query on the state to fetch all books.

- The state executes the query and returns the result to Rell.

- Rell provides the user with the results containing all books from the state.

Now, let’s move on to querying the blockchain for transactions.

## Querying the blockchain for transactions​

The next step is to show how we can query the blockchain and fetch all transactions that have led up to the current DApp table state.

Each transaction on the blockchain is stored in the system transaction entity, which includes a field called tx_data. This field contains a GTV-encoded binary representation of the full transaction — including its operations, arguments, signers, and signatures.

To decode this and work with it in Rell, you can use the built-in gtx_transaction type.

### Adding a Query​

Add the following query to your src/main/queries.rell file:

src/main/queries.rell
```rell
query get_transactions() {  return transaction @* { } ( gtx_transaction.from_bytes(.tx_data) );}
```

This will return a list of structured transactions, including:

- The blockchain RID the transaction belongs to

- All operations and their arguments

- Signers of the transaction

- Associated signatures

You can inspect the decoded transaction format in detail in the [system entities documentation](https://docs.chromia.com/rell/language-features/systemlib/system-entities).

This concludes our definitions for querying transactions. In the next lesson, we will look at how to use these queries in practice.
