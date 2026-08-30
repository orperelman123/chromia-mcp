# COURSE book-review — 26 pages


===== FILE: courses__book-review__blockchain-transactions.md =====

# Lesson 5 - Understand blockchain state and transactions

URL: https://learn.chromia.com

- [Home](/)
- Lesson 5 - Understand blockchain state and transactions
# Lesson 5 - Understand blockchain state and transactions

In this lesson, you will learn how Chromia manages transactions and states. You will explore how user-signed transactions are validated, applied to the dapp table state, and committed to the blockchain.

Additionally, you will learn how to query the current state of your dapp and how to retrieve all transactions from the blockchain to understand the relationship between blockchain transactions and the dapp table state.

Finally, you will see an example of updating a book's title and how these changes impact both the blockchain and the state.

## Sections
[Understanding blockchain state and transactions](/courses/book-review/blockchain-transactions/query-transaction)[Let's look at an example](/courses/book-review/blockchain-transactions/example)[Start lesson »](/courses/book-review/blockchain-transactions/query-transaction)


===== FILE: courses__book-review__blockchain-transactions__example.md =====

# Let&#x27;s look at an example

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 5 - Understand blockchain state and transactions](/courses/book-review/blockchain-transactions/)
- Let's look at an example
# Let's look at an example

Now that we're all set up, let's add a couple of transactions and then query both the state and the transactions to see how they interact.

First, we'll clear all data from our node to start with a fresh slate. Before running the command, if the Chromia Node is running, stop it with Ctrl+C. Then, you can clear the data by running the following command:

```shell
chr node start --wipe
```

This command will remove all transactions from the blockchain and reset the current state. You can verify this by running:

```shell
chr query get_all_books
```

This should return an empty array, indicating that there are no books in the current state.

Next, let's add a new transaction to create a book, intentionally misspelling "Chromia" so that we can later correct this with an update:

```shell
chr tx create_book "ISBN001" "Crhomia 101" "Alice" --key-id "book_admin" --await
```

The output should look like this:

```shell
transaction with rid DF94A07EACBA673CC4ECA4A43D70CDA4FD30E4335E124B7153FB12A78FDD8CE9 was posted CONFIRMED
```

tipInstead of specifying --key-id with every transaction, you can define the default key in a .chromia/config file:

```ini
key.id = book_admin
```
You can place this file in one of two locations:

- Global config: ~/.chromia/config — applies to all projects unless overridden

- Project config: .chromia/config in your project folder — overrides the global file
Once set, you no longer need to include --key-id in CLI transactions:

```bash
chr tx create_book "ISBN001" "Crhomia 101" "Alice" --await
```
The CLI will automatically use the configured key.

For full details on config file precedence, see:
[Key pair reading flow](https://docs.chromia.com/cli/key-pair-management#key-pair-reading-flow)

After creating this transaction, you can query all books using the get_all_books command:

```shell
chr query get_all_books
```

The result should look like this:

```json
[  {    "author": "Alice",    "isbn": "ISBN001",    "title": "Crhomia 101"  }]
```

This confirms that the state/table has been correctly updated with the new book information. Now, let's query the blockchain transactions:

```shell
chr query get_transactions
```

The result will be:

```json
[  [    "body": [      "blockchain_rid": x"B52F0A078993374201853649F807FDEDD6516A424557F311E2AF769761F89D40",      "operations": [        [          "args": [            "ISBN001",            "Crhomia 101",            "Alice"          ],          "name": "create_book"        ]      ],      "signers": [        x"035A25AB32B85E6A5B58A31A71CC709CED159096194984243159492CBA0FADF82F"      ]    ],    "signatures": [      x"91EFC02C2958698E71C41D2B020F9E4EF81F6C427FAA98667D6FF4B87857809E1ECE4C9C923B1E5659177B8D0635711E47A4EBB502217A437D3FFC89741E74DD"    ]  ]]
```

This indicates that the transaction has been successfully added to the blockchain and the state has been updated accordingly.

The following diagram illustrates the process when we run the query get_transactions command:

Next, we'll add a new operation to update a book's title, allowing us to correct the misspelled title. To do this, we need to make the title mutable in our book entity:

src/main/entities.rell
```rell
entity book {  key isbn: text;  mutable title: text;  author: text;}
```

Next, we can add the update operation:

src/main/operations.rell
```rell
operation update_book(isbn: text, title: text){  val adminPubkey = chain_context.args.admin_pubkey;  require(op_context.is_signer(adminPubkey), "Only admin can update books");  update book @ { .isbn == isbn }( .title = title );}
```

Before testing this new operation, update the Chromia Node to ensure it includes the new operation by running the following command and waiting for the next block:

```shell
chr node update
```

After updating, we can test the new operation with a transaction to update a book's title:

```shell
chr tx update_book "ISBN001" "Chromia 101" --key-id "book_admin" --await
```

After this transaction, we can query both the blockchain transactions and the state again:

```shell
chr query get_all_books
```

The result will now show the updated title:

```json
[  {    "author": "Alice",    "isbn": "ISBN001",    "title": "Chromia 101"  }]
```

Similarly, when querying the blockchain transactions:

```shell
chr query get_transactions
```

The result will be:

```json
[  [    "body": [      "blockchain_rid": x"B52F0A078993374201853649F807FDEDD6516A424557F311E2AF769761F89D40",      "operations": [        [          "args": [            "ISBN001",            "Crhomia 101",            "Alice"          ],          "name": "create_book"        ]      ],      "signers": [        x"035A25AB32B85E6A5B58A31A71CC709CED159096194984243159492CBA0FADF82F"      ]    ],    "signatures": [      x"91EFC02C2958698E71C41D2B020F9E4EF81F6C427FAA98667D6FF4B87857809E1ECE4C9C923B1E5659177B8D0635711E47A4EBB502217A437D3FFC89741E74DD"    ]  ],  [    "body": [      "blockchain_rid": x"B52F0A078993374201853649F807FDEDD6516A424557F311E2AF769761F89D40",      "operations": [        [          "args": [            "ISBN001",            "Chromia 101"          ],          "name": "update_book"        ]      ],      "signers": [        x"035A25AB32B85E6A5B58A31A71CC709CED159096194984243159492CBA0FADF82F"      ]    ],    "signatures": [      x"FD9402B45ED52A238B0C756AFA1ABA5BD2F89C9AE44343E58FE76E207F4672CF0AF5C37F7209E5D9A474491671000FAFA9753A4BF2B5D16AE7C5C48CFA18A8E1"    ]  ]]
```

You’ll notice that there are two transactions on the blockchain: one for creating the book and another for updating its title.

When we examine the state, it now reflects the aggregation of these two transactions. This means that the state encompasses all the transactions related to this book, allowing us to backtrack through history and see exactly how the current state was established.

Understanding the interaction between Rell, Chromia, the blockchain, and the state is essential for effectively building and managing decentralized applications.


===== FILE: courses__book-review__blockchain-transactions__query-transaction.md =====

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


===== FILE: courses__book-review__book-entity.md =====

# Lesson 1 - Create your first entity

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - Create your first entity
# Lesson 1 - Create your first entity

In this introductory lesson, you'll begin by defining a book entity using the Rell language.

You will also create an operation to store book entries on the blockchain, learn about test modules to ensure your decentralized application (dapp) functions as intended, and write a query to retrieve all book entries.

## Sections
[Create your first entity](/courses/book-review/book-entity/tables)[Add your first operation](/courses/book-review/book-entity/basic-operations)[Write a query to retrieve all books](/courses/book-review/book-entity/write-queries)[Start lesson »](/courses/book-review/book-entity/tables)


===== FILE: courses__book-review__book-entity__basic-operations.md =====

# Add your first operation

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 1 - Create your first entity](/courses/book-review/book-entity/)
- Add your first operationOn this page
# Add your first operation

In this section, we’ll create the create_book operation. This operation allows us to add new book entries to the blockchain by updating the dApp’s state.

## Define the create_book operation​

In Rell, an operation is a signed public function that modifies the state of the blockchain, specifically updating the dapp state. Operations serve as the primary means to interact with and alter the state of your dapp.

Open the file src/main/operations.rell and add the following Rell code:

src/main/operations.rell
```rell
operation create_book(isbn: text, title: text, author: text) {  create book(isbn = isbn, title = title, author = author);}
```

### Breakdown of the code​

- operation keyword: This defines a new operation. It functions like a public function that can be called to perform actions that change the state of your dapp.

- Parameters: The create_book operation takes three parameters: isbn, title, and author. These parameters correspond to the attributes of the book entity.

- create command: Inside the operation, the create command inserts a new book entry into the dApp’s state using the provided parameters.

## Understanding the process​

Here’s an overview of what happens when you execute the create_book operation:

A detailed view of the transaction process is illustrated below:

- User signs a transaction: The user initiates a transaction to execute the create_book operation.

- Transaction validation: The Chromia node validates the transaction and forwards it to the blockchain.

- State update: The create_book operation updates the dApp’s state by creating a new book entry.

- Blockchain confirmation: If the transaction is confirmed, it is added to the blockchain, and the dApp’s state is updated accordingly.

## Add unit tests​

Testing ensures that your operations work as expected. We’ll add a test for the create_book operation.

Insert the following test code into src/test/book_review_test.rell, so that the file looks like this:

src/test/book_review_test.rell
```rell
@test module;import main.{ book, create_book };function test_add_book() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book("124", "Book2", "Author2"))        .run();    val all_books = book @* { };    assert_equals(all_books.size(), 2);    assert_equals(all_books[0].title, "Book1");    assert_equals(all_books[0].author, "Author1");}
```

### Breakdown of the test​

- Transaction creation: rell.test.tx() creates a test transaction.

- Adding operations: Two create_book operations are added to the transaction.

- Execution: run() executes the transaction.

- Assertions: The test queries all books and verifies that there are two entries with the correct attributes.

## Running the test​

To run the test, ensure you're in your project folder and enter the following command in your terminal:

```shell
chr test
```

## Testing on a local node​

To test the create_book operation on a local Chromia node, follow these steps:

- 
Start a local Chromia node: Open your terminal and run the following command:

```shell
chr node start
```

- 
Add a book: In a new terminal window or tab, run the following command to add a book:

```shell
chr tx --await create_book "ISBN1234" "'1984'" "George Orwell"
```

The result should be:

```shell
transaction with rid TxRid(rid=) was posted CONFIRMED
```

This message confirms that the transaction was added to the blockchain, and the book table now contains a new row with the book data.


===== FILE: courses__book-review__book-entity__tables.md =====

# Create your first entity

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 1 - Create your first entity](/courses/book-review/book-entity/)
- Create your first entityOn this page
# Create your first entity

In this section, we'll define the Book entity using Rell. This will allow us to manage book data and perform queries
on it.

## Define the Book entity​

In Rell, entities are similar to tables in relational databases. We'll create a Book entity to manage the book
information.

### Entity diagram​

### Add the entity definition​

Open the src/main/entities.rell file and insert the following Rell code:

src/main/entities.rell
```rell
entity book {  key isbn: text;  title: text;  author: text;}
```

### Code explanation​

- entity keyword: This keyword defines a new entity, similar to creating a table in a database.

- Attributes:

- key isbn: text: This specifies isbn as the unique identifier for each book. The key keyword ensures that the value is unique and creates an index on this field, which improves query performance. The text type indicates that the value will be stored as a string.

- title: text and author: text: These attributes define the book's title and author, both stored as text.

With the Book entity defined, you can now utilize it in your decentralized application.


===== FILE: courses__book-review__book-entity__write-queries.md =====

# Write a query to retrieve all books

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 1 - Create your first entity](/courses/book-review/book-entity/)
- Write a query to retrieve all booksOn this page
# Write a query to retrieve all books

This section covers the basics of working with queries in Chromia blockchain development. Queries are used to fetch data
from the blockchain. We'll guide you through the process, showing you how to create and test them.

## Define the query​

Open the file src/main/queries.rell and add the following Rell code:

src/main/queries.rell
```rell
query get_all_books() {  return book @* { } (    .isbn,    .title,    .author  );}
```

### Breakdown of the query​

- query keyword: This defines a new query, similar to a SELECT statement in SQL, allowing you to retrieve data from the blockchain.

- @* operand: In book @*, we expect zero or more objects of the book type from the query.

- Filter criteria: The curly braces {} enable you to specify conditions for the query. In this case, we keep it simple by fetching all entries.

- Attributes: After the curly braces, we specify which attributes to retrieve: .isbn, .title, and .author.

This query retrieves all books in a collection, with each item including the attributes isbn, title, and author.

## Update the unit tests​

To ensure our query works correctly, we need to update the tests.

### Step 1: Update imports​

Open the file src/test/book_review_test.rell and modify the import statement as follows:

src/test/book_review_test.rell
```rell
import main.{ book, create_book, get_all_books };
```

### Step 2: Add the new test function​

Next, add the following test function to the same file:

src/test/book_review_test.rell
```rell
function test_get_books() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book("124", "Book2", "Author2"))        .run();    val books = get_all_books();    assert_equals(books.size(), 2);}
```

### Breakdown of the test​

- Transaction creation: rell.test.tx() creates a test transaction.

- Adding operations: Two create_book operations are added to the transaction.

- Execution: run() executes the transaction.

- Query and assertions: The get_all_books() function retrieves the books. We then assert that the number of
retrieved books is 2.

To run the tests, ensure you are in your project folder and enter the following command in your terminal:

```shell
chr test
```

## Test on a local Chromia node​

To validate our query on a local Chromia node, follow these steps:

- 
Update or start the local Chromia node:

- 
If the node is already running, use the following command to update it:

```shell
chr node update
```

- 
If the node is not already running, start it with:

```shell
chr node start
```

noteAfter running chr node update, it may take a moment for the update to be reflected. You may need to wait for the next block to be processed before the query becomes available.

- 
Run the query: In a new terminal window or tab, execute the following command:

```shell
chr query get_all_books
```

You should see the results, including any books you previously inserted, such as those added with the create_book operation. For instance, if you followed the earlier steps, you should observe output similar to the following:

```json
[  {    "author": "George Orwell",    "isbn": "ISBN1234",    "title": "1984"  }]
```

This confirms that the query correctly retrieves the stored book from the blockchain.

Congratulations! You have successfully stored entities on your test node's blockchain and created a query to retrieve a collection of all books.


===== FILE: courses__book-review__book-review-entity.md =====

# Lesson 2 - Create a related entity

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Create a related entity
# Lesson 2 - Create a related entity

In this lesson, you will define the book_review entity, which will allow you to record and connect book reviews to the book entity.

In the next section, you will add an operation to create new book reviews. This operation will use the ISBN to link each review to a specific book.

Finally, you will learn how to write a query to retrieve all reviews associated with a particular book, using the ISBN as a reference.

## Sections
[Defining the book review entity](/courses/book-review/book-review-entity/tables)[Adding an operation to create a book review](/courses/book-review/book-review-entity/basic-operations)[Write a query to retrieve all reviews of a book](/courses/book-review/book-review-entity/write-queries)[Start lesson »](/courses/book-review/book-review-entity/tables)


===== FILE: courses__book-review__book-review-entity__basic-operations.md =====

# Adding an operation to create a book review

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 2 - Create a related entity](/courses/book-review/book-review-entity/)
- Adding an operation to create a book reviewOn this page
# Adding an operation to create a book review

Now that we have two entities in our model, we need to add a mechanism for creating a new entry for a book review. To achieve this, we will define an operation to create a new book review.

Open the file src/main/operations.rell and add the following Rell code:

src/main/operations.rell
```rell
operation create_book_review(isbn: text, reviewer_name: text, review: text, rating: integer) {  val book = book @ { .isbn == isbn };  create book_review (    book,    reviewer_name,    review,    rating  );}
```

This operation is similar to the create_book operation, but it establishes a connection between the review and the book. In our example, the key for a book is the isbn, which we will use to associate the review with a specific book.

### Inside the operation​

We send the operation to create a book review to a Chromia node, which will generate a new transaction and persist the data in the dapp table state, as illustrated below.

- A user signs the transaction create_book_review("ISBN1234", "Alice", "It was a great book!", 5).

- The transaction is sent to the Chromia Node, where it undergoes validation before being forwarded to the blockchain.

- Inside the operation:

```rell
val book = book @ { .isbn == isbn };  create book_review (    book,    reviewer_name,    review,    rating  );
```

The book is queried from the dapp table state, and a new row is created for the book_review with a relationship to the book.

- If the transaction is confirmed, it is added to a block on the blockchain, and the dapp table state is updated accordingly.

## Adding unit tests​

Let's incorporate a test to ensure our book review operations function as expected. In Rell, we can use test modules to create our tests, which is relatively straightforward.

Open the src/test/book_review_test.rell file and update the imports to include book_review and create_book_review:

src/test/book_review_test.rell
```rell
import main.{ book, create_book, get_all_books, book_review, create_book_review };
```

After updating the imports, add the following test code to the same file:

src/test/book_review_test.rell
```rell
function test_add_book_review() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book_review("123", "Reviewer1", "ReviewText1", 5))        .op(create_book_review("123", "Reviewer2", "ReviewText2", 3))        .run();    val reviews = book_review @* { };    val book = book @ { .isbn == "123" };    assert_equals(reviews.size(), 2);    assert_equals(book, reviews[0].book);    assert_equals(reviews[0].reviewer_name, "Reviewer1");    assert_equals(reviews[0].review, "ReviewText1");    assert_equals(reviews[0].rating, 5);}
```

This code executes a transaction with three operations. First, we create a book; then, we create two reviews and connect them to the book we created.

- assert_equals(reviews.size(), 2); checks that there are two entries.

- assert_equals(book, reviews[0].book); ensures that the correct book is connected to the review.

- assert_equals(reviews[0].reviewer_name, "Reviewer1") confirms the reviewer's name.

- assert_equals(reviews[0].review, "ReviewText1") checks the review text.

- assert_equals(reviews[0].rating, 5) verifies the rating.

After setting up the tests, we can run them with the following command:

```shell
chr test
```

After running the test, you should see the results indicating that all tests have passed:

```shell
TEST RESULTS:OK test.book_review_test:test_add_bookOK test.book_review_test:test_get_booksOK test.book_review_test:test_add_book_reviewSUMMARY: 0 FAILED / 3 PASSED / 3 TOTAL***** OK *****
```

## Testing with a local Chromia node​

To validate the newly created operation on a local Chromia node, follow these steps:

- 
Update or start the local Chromia node:

- 
If the node is already running, use the following command to update it:

```shell
chr node update
```

- 
If the node is not running, start it with:

```shell
chr node start
```

Note: After running chr node update, it may take a moment for the update to be reflected. You might need to wait for the next block to be processed before the query becomes available.

- 
Create a transaction to add a book review:

Execute the following command to create a transaction that adds a book review:

```shell
chr tx --await create_book_review "ISBN1234" "Alice" "It was a great book" 5
```

The expected result should be:

```shell
transaction with rid TxRid(rid=) was posted CONFIRMED
```

This message confirms that the transaction has been added to the blockchain, and the state now contains a row with the book review data.


===== FILE: courses__book-review__book-review-entity__tables.md =====

# Defining the book review entity

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 2 - Create a related entity](/courses/book-review/book-review-entity/)
- Defining the book review entityOn this page
# Defining the book review entity

Now that we have created the book entity, the next step is to introduce the book_review entity. This entity will allow us to record book reviews. The model is defined as follows:

## Adding the book review entity​

To define the book_review entity, open the file src/main/entities.rell and add the following code:

src/main/entities.rell
```rell
entity book_review {  index book: book;  reviewer_name: text;  review: text;  rating: integer;}
```

infoLinking & implicit joins

This entity includes a reference to book. Under the hood, Rell stores the referenced row’s rowid, creating a link between the review and its corresponding book.

- When a query or operation starts from book_review and accesses fields of .book

(such as .book.title, .book.to_struct(), or @sort .book.author), Rell performs an implicit join between the book_review and book entities to retrieve those values.

- If you only access the fields of book_review, no join occurs.

- If the expression begins from the book entity, you are already positioned in that entity — no extra join is necessary.

tipEach reference you dereference adds its own implicit join. If an entity has many references and you retrieve them all (e.g., via .to_struct()), the expression will perform multiple joins, which can affect performance on large datasets.

### Breakdown of the code​

- entity keyword: Defines a new entity, similar to creating a table in a SQL database.

- index book: book;: The index keyword is used to optimize queries that involve the book relationship. This attribute refers to a book, linking the book to its reviews.

- reviewer_name: text;: Allows users to enter their name or alias as a reviewer.

- review: text;: Stores the content of the review as text.

- rating: integer;: Captures the numerical rating assigned to the book review.

With these attributes, you can now begin recording book reviews in your Chromia-based app.


===== FILE: courses__book-review__book-review-entity__write-queries.md =====

# Write a query to retrieve all reviews of a book

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 2 - Create a related entity](/courses/book-review/book-review-entity/)
- Write a query to retrieve all reviews of a bookOn this page
# Write a query to retrieve all reviews of a book

Now that we have a mechanism to add reviews connected to books, we need a way to retrieve them. We'll define a query for
that.

## Define the query​

Open the file src/main/queries.rell and add the following Rell code:

src/main/queries.rell
```rell
query get_all_reviews_for_book(isbn: text) {  val reviews = book_review @* { .book.isbn == isbn } (    .reviewer_name,    .review,    .rating  );  return reviews;}
```

### Breakdown of the query​

- Input parameter: The query takes an isbn parameter to specify which book's reviews you want to retrieve.

- Join operation: val reviews = book_review @* { .book.isbn == isbn }; retrieves all book_review entities where
the book.isbn matches the given isbn.

- Attributes: We specify which attributes to include in the result by adding:

```rell
(  .reviewer_name,  .review,  .rating);
```

This query retrieves all reviews for a specified book, including the reviewer's name, the review text, and the rating.

## Test it on a local Chromia node​

Testing on a local Chromia node is done using the Chromia CLI. Here are the steps:

- 
Update or start the local Chromia node:

- 
If the node is already running, use the following command to update it:

```shell
chr node update
```

- 
If the node is not already running, start it with:

```shell
chr node start
```

noteAfter running chr node update, the query might take a moment to become available. You may need to wait for the
following block to be processed before executing the query. If you get a "400 Bad Request" error, please wait a few
moments and try again.

- 
Execute the query: In a new terminal window or tab, run the following command to fetch all reviews for a book:

```shell
chr query get_all_reviews_for_book "isbn=ISBN1234"
```

The expected result should be:

```json
[  {    "rating": 5,    "review": "It was a great book",    "reviewer_name": "Alice"  }]
```

In upcoming lessons, we'll explore Rell's powerful capabilities further, including handling validation, structuring
results from queries and executing transactions with signatures.


===== FILE: courses__book-review__build-client.md =====

# Lesson 6 - Build the client

URL: https://learn.chromia.com

- [Home](/)
- Lesson 6 - Build the client
# Lesson 6 - Build the client

In this lesson, you will set up your development environment and connect to the Chromia blockchain. You will import the necessary modules from the postchain-client and learn how to implement various functions in your front-end application.

Next, you will query the blockchain and transmit transactions using the client instance. To sign transactions, we will utilize the postchain-client library, where you will generate a cryptographic keypair for secure transaction signing.

Finally, you will see a practical example in which we create and query entities.

## Sections
[Prerequisites](/courses/book-review/build-client/prerequisites)[Connecting to the Chromia blockchain](/courses/book-review/build-client/sign-transaction)[Querying the blockchain with postchain-client](/courses/book-review/build-client/query-blockchain)[Complete the example](/courses/book-review/build-client/complete-example)[Start lesson »](/courses/book-review/build-client/prerequisites)


===== FILE: courses__book-review__build-client__complete-example.md =====

# Complete the example

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 6 - Build the client](/courses/book-review/build-client/)
- Complete the example
# Complete the example

Let's add the remaining parts of our client to test different functions.

We start by defining the remaining type we’ll need for querying book reviews:

```typescript
type BookReview = {  book: Book;  reviewer_name: string;};
```

Next, we define functions to query for entities and some helper functions to read input from the user:

```typescript
const rl = readline.createInterface({  input: process.stdin,  output: process.stdout,});function getInput(query: string): Promise {  return new Promise((resolve) => {    rl.question(query, (answer) => {      resolve(answer);    });  });}const getReviewsForBook = async (isbn: string) => {  const bookList = await client.query("get_all_reviews_for_book", { isbn: isbn });  console.log("Book review list\n", bookList);};const getAllTransactions = async () => {  const transactions = await client.query("get_transactions", {});  console.dir(transactions, {    depth: null,    customInspect: true, // This forces Buffer to use hex string output  });};
```

And finally, our complete main function where we add calls to create books, reviews, and fetch data:

```typescript
async function main() {  client = await createClient({    nodeUrlPool: "http://localhost:7740",    blockchainRid: blockchainRID,  });  console.log("Creating a new book transaction");  await client.signAndSendUniqueTransaction(    { name: "create_book", args: ["ISBN1", "Chromia 101", "John Doe"] },    bookKeeperSignatureProvider  );  await getInput("Transaction committed!\nPress any key to continue...");  console.log("Let's fetch and view all books currently in the node");  await getAllBooks();  await getInput("Press any key to continue...");  console.log("We can now add a second book");  await client.signAndSendUniqueTransaction(    { name: "create_book", args: ["ISBN2", "Rell 101", "Jane Doe"] },    bookKeeperSignatureProvider  );  await getInput("Transaction committed, press any key to continue...");  console.log("Let's fetch and view all books currently in the node");  await getAllBooks();  await getInput("Press any key to continue...");  console.log("We can now add two reviews for the book with ISBN = ISBN2");  await client.signAndSendUniqueTransaction(    {      name: "create_book_review",      args: ["ISBN2", "Bob Doe", "This is a great book!", 5],    },    bookKeeperSignatureProvider  );  await client.signAndSendUniqueTransaction(    {      name: "create_book_review",      args: ["ISBN2", "Charlie Doe", "It was ok!", 3],    },    bookKeeperSignatureProvider  );  await getInput("Transaction committed, press any key to continue...");  console.log("Let's fetch and view all reviews for ISBN2");  await getReviewsForBook("ISBN2");  await getInput("Press any key to continue...");  console.log("Now let's look at all transactions that have been committed to the blockchain");  await getAllTransactions();  await getInput("Press any key to continue...");  rl.close();}
```

Now, we have a fully functional example to showcase how straightforward it is to implement a front-end client using Chromia and postchain-client.


===== FILE: courses__book-review__build-client__prerequisites.md =====

# Prerequisites

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 6 - Build the client](/courses/book-review/build-client/)
- PrerequisitesOn this page
# Prerequisites

This tutorial will guide you through each step, from setting up your development environment to understanding the code.

Before we begin coding, let's ensure your development environment is properly configured.

## Step 1: Install Node.js and set up the project​

- 
Install Node.js: If you don't have Node.js installed, download and install it from the [official website](https://nodejs.org/). Node.js allows you to run JavaScript on the server side.

- 
Create a project directory: Open your terminal, navigate to the directory where you want to create your project, and run the following commands:

```shell
mkdir chromia-book-review-democd chromia-book-review-demo
```

- 
Initialize a Node.js project: Run the following command to create a package.json file:

```shell
npm init -y
```

## Step 2: Install required packages​

Now that your project is set up, you'll need to install some essential packages:

```shell
npm install typescript postchain-client readline @types/node
```

- typescript: This package provides TypeScript support.

- postchain-client: This package contains tools for interacting with the Chromia blockchain.

- readline: This core Node.js module helps read user input from the command line.

## Step 3: Configure TypeScript​

To use TypeScript in your project, you need to create a configuration file named tsconfig.json. You can generate one with the following command:

```shell
npx tsc --init
```

This command creates a default tsconfig.json file, which you can customize according to your project's requirements.

## Step 4: Integrate the code​

Now that you have installed the necessary packages, it's time to integrate the code into your project. Follow these steps:

- 
Create a TypeScript file named book_review.ts in your project directory. For now, this file will be empty, but we will add code to it in the upcoming sections.

- 
To run your TypeScript code, it must first be transpiled to JavaScript. Execute the following command:

```shell
npx tsc
```

This command generates a book_review.js file containing the compiled JavaScript code.

- 
Finally, run your code using Node.js:

```shell
node book_review.js
```

This will execute your TypeScript code and interact with the Chromia blockchain as specified.

Congratulations! You have successfully set up your development environment, installed the necessary packages, and integrated TypeScript into your project. In the next section, we will explore an example of our client.


===== FILE: courses__book-review__build-client__query-blockchain.md =====

# Querying the blockchain with postchain-client

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 6 - Build the client](/courses/book-review/build-client/)
- Querying the blockchain with postchain-clientOn this page
# Querying the blockchain with postchain-client

In this section, we will explore how to query the Chromia blockchain to retrieve information about transactions and the state of our dapp table.

Querying with postchain-client is straightforward. Let's look at an example that fetches all books and breaks them down.

First, we need to define a type for Book to hold our book information:

```typescript
type Book = {  isbn: string;  title: string;  author: string;};
```

Next, we will create a function to fetch and display all books:

```typescript
const getAllBooks = async () => {  const bookList = await client.query("get_all_books", {});  console.log("Book list:", bookList);};
```

In this function, we use the client and the query command to execute our query. We specify the return type as a generic type, in this case Book[].

### Function arguments​

#### First argument: queryFunctionName​

This is the name of the query function defined on the blockchain. It corresponds to a function in a Rell script that retrieves all book records.

#### Second argument: queryArguments​

An empty object ({}) indicates that this query does not require any specific arguments to execute. However, this object could contain parameters that the blockchain function uses to filter or process the results if necessary.

## Handling the response​

The client.query method returns a promise that, when resolved, yields an array of books (Book[]). These retrieved books can then be displayed, processed, or utilized according to your application's needs. In this example, we output the results to the console.

We can call getAllBooks(); from our main function, as shown below. After adding this, you can re-run the client to see your added book listed. To restart and test the example code, you can use the command chr node start --wipe to start a new node without any existing data.

```typescript
async function main() {  client = await createClient({    nodeUrlPool: "http://localhost:7740",    blockchainRid: blockchainRID,  });  console.log("Creating a new book transaction");  await client.signAndSendUniqueTransaction(    { name: "create_book", args: ["ISBN1", "Chromia 101", "John Doe"] },    bookKeeperSignatureProvider  );  getAllBooks();}
```


===== FILE: courses__book-review__build-client__sign-transaction.md =====

# Connecting to the Chromia blockchain

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 6 - Build the client](/courses/book-review/build-client/)
- Connecting to the Chromia blockchainOn this page
# Connecting to the Chromia blockchain

In this section, we will guide you through connecting to the Chromia blockchain using a client. Together, we will build an example client, starting with the necessary imports at the top of our file, book_review.ts.

```typescript
import { encryption, createClient, newSignatureProvider, IClient, MERKLE_HASH_VERSIONS } from "postchain-client";import * as readline from "readline";
```

Next, we create a main function and add a helper function to receive input:

```typescript
async function main() {}
```

## Using the createClient method​

The createClient method returns a client instance that enables us to query the blockchain and send transactions. Let's add this method and break down how it works:

```typescript
let client: IClient;const blockchainRID = "";async function main() {  client = await createClient({    nodeUrlPool: "http://localhost:7740",    blockchainRid: blockchainRID,  });}
```

### Parameters:​

- 
nodeUrlPool: This is the URL of the node you want to connect to. In most development scenarios, Chromia nodes run locally, typically using http://localhost:7740. However, in production or testnet scenarios, you would use the address of a remote node.

- 
blockchainRid: Every Chromia blockchain has a unique Referential Identifier (RID), which is a hexadecimal string that distinguishes different blockchains.

In the code snippet above, we connect to a local Chromia node and specify the blockchain of interest using its RID.

Why is this important?

- 
Specificity: A single Chromia node might be associated with multiple blockchains, and the RID ensures that you interact with the correct one.

- 
Flexibility: By parameterizing the node URL and blockchain RID, you can easily switch between different environments (e.g., development, staging, production) or blockchains without changing your application's core logic.

How to Get the Blockchain RID: When you have a node running, you can always query the node for the Blockchain RID using the following command:

```text
curl http://localhost:7740/brid/iid_0
```

## Signing a transaction with postchain-client​

The postchain-client library simplifies transaction signing for Chromia. Let’s break down the process step by step.

### Generate a keypair​

Before signing any transaction, you need to generate a cryptographic keypair consisting of a public and private key. Add the following to your book_review.ts file. Use the private key that corresponds to the admin bookkeeper defined in chromia.yml:

```typescript
const privKey = Buffer.from("", "hex");const bookKeeperKeyPair = encryption.makeKeyPair(privKey);
```

The makeKeyPair function from the encryption module generates a keypair using the provided private key (privKey). The resulting bookKeeperKeyPair contains both the private key (used for signing) and the associated public key (used for verification).

If you're using the book_admin key id as instructed earlier, you can retrieve the private key from the .chromia folder like this:

```sh
cat ~/.chromia/book_admin
```

### Secure storage and retrieval of keys​

When working with cryptographic keys, it’s crucial to store them securely to prevent unauthorized access. To learn about best practices for securely generating, storing, and managing your keys, please refer to the [Chromia documentation on key generation](https://docs.chromia.com/cli/commands/keygen). This documentation provides guidance on file placement and other important security practices.

Warning: Security riskIn this code example, privKey is assigned directly for the sake of simplicity. It is critically important to manage private keys securely in a production environment. Never store private keys in plain text or expose them in client-side code, as this poses a significant security risk.

### Set up the signature provider​

Once you have your keypair, you need a mechanism to use it for signing transactions. Let’s create the SignatureProvider and add it to book_review.ts.

```typescript
const bookKeeperSignatureProvider = newSignatureProvider(MERKLE_HASH_VERSIONS.TWO, bookKeeperKeyPair);
```

The newSignatureProvider function creates a signature provider using the provided keypair (bookKeeperKeyPair). This signature provider is responsible for signing any transaction before it is sent to the blockchain.

### Using the signAndSendUniqueTransaction method​

Now we can add our first transaction to the main function in book_review.ts:

```typescript
console.log("Creating a new book transaction");  await client.signAndSendUniqueTransaction(    { name: "create_book", args: ["ISBN1", "Chromia 101", "John Doe"] },    bookKeeperSignatureProvider  );
```

The signAndSendUniqueTransaction method from the postchain-client library combines two primary tasks:

- 
Signing: Before a transaction is dispatched to the blockchain, it is signed using the provided signature provider. When verified by the blockchain network, this signature proves that the transaction has not been tampered with after being signed and confirms the sender's identity.

- 
Sending: Once the transaction is signed, it is dispatched to the Chromia blockchain for processing.

### Parameters explained:​

This method takes two arguments:

- 
Transaction object:

- name: This represents the operation you intend to execute on the blockchain. For example, "create_book" corresponds to a Rell function that creates a new book entity on the blockchain.

- args: This is an array that contains the arguments required for the operation.

- 
Signature provider: This component is responsible for signing the transaction using the previously discussed key pair. In the provided code snippet, the bookKeeperSignatureProvider is being utilized.

### How it fits into the overall flow:​

When you invoke signAndSendUniqueTransaction, you are effectively directing the client to:

- Create a transaction to call the create_book function on the blockchain with the specified arguments.

- Sign this transaction using the bookKeeperSignatureProvider.

- Send the signed transaction to the Chromia blockchain.

### Testing our code​

Finally, at the end of our file, we add a function call to run the main function as follows:

```typescript
main();
```

Now, we can test running our first example:

```shell
npx tscnode book_review.js
```

Next, we will explore how to add a query for fetching books and reviews.


===== FILE: courses__book-review__input-verification.md =====

# Lesson 3 - Verify and validate inputs

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - Verify and validate inputs
# Lesson 3 - Verify and validate inputs

In this lesson, you'll learn how to implement input validation in Rell to improve app security. We will use the require function within the create_book_review operation to ensure that users enter valid data. Additionally, you will write tests for both existing and non-existing books, which will provide robust input validation.

We will also explore how to structure query results using the book_review_dto struct. This struct combines both book and review data into a single, organized object.

## Sections
[Verify inputs](/courses/book-review/input-verification/input-verification)[Adding structured results from queries](/courses/book-review/input-verification/structure)[Start lesson »](/courses/book-review/input-verification/input-verification)


===== FILE: courses__book-review__input-verification__input-verification.md =====

# Verify inputs

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 3 - Verify and validate inputs](/courses/book-review/input-verification/)
- Verify inputsOn this page
# Verify inputs

In this section, we will add validations to ensure that we only allow valid data. To achieve this, we will utilize the [require function](https://docs.chromia.com/rell/language-features/systemlib/require-function).

## Input validation​

We need to update the create_book_review operation to incorporate input validation using the require function. Make the following changes to the src/main/operations.rell file:

src/main/operations.rell
```rell
operation create_book_review(isbn: text, reviewer_name: text, review: text, rating: integer) {  val book = require(book @? { .isbn == isbn }, "Book with ISBN %s not found".format(isbn));  create book_review (    book,    reviewer_name,    review,    rating  );}
```

In this updated function:

- book @? { .isbn == isbn } attempts to fetch a book with a matching isbn. The @? operator returns null if no matching book is found.

- The require() function checks for a null value and throws an error if the book does not exist (i.e., is null).

This simple yet powerful validation mechanism is seamlessly integrated into Rell, allowing you to easily maintain the security and stability of your application.

We will explore validation in greater depth in upcoming courses and tutorials, particularly when examining transaction signing and user account management.

## Test validations​

Now, let's ensure that our new validation for create_book_review is thoroughly tested. We already have a test that successfully adds a book review since it requires an existing book:

src/test/book_review_test.rell
```rell
rell.test.tx()    .op(create_book("123", "Book1", "Author1"))    .op(create_book_review("123", "Reviewer1", "ReviewText1", 5))    .run();
```

However, we also need to add a new test for a non-existing book, which should fail the create_book_review operation. Add the following test function to the src/test/book_review_test.rell file:

src/test/book_review_test.rell
```rell
function test_add_book_review_for_missing_book() {    rell.test.tx()        .op(create_book_review("N/A", "Reviewer2", "ReviewText2", 3))        .run_must_fail("Book with ISBN N/A not found");}
```

In this test, we use isbn = "N/A", which will trigger a failed requirement, and thus, the test will pass. This comprehensive testing ensures that our input validation is robust and functions as intended.

## Run the tests​

To verify that everything is functioning as expected after updating your code, execute the following command:

```shell
chr test
```


===== FILE: courses__book-review__input-verification__structure.md =====

# Adding structured results from queries

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 3 - Verify and validate inputs](/courses/book-review/input-verification/)
- Adding structured results from queriesOn this page
# Adding structured results from queries

In this section, we will learn how to structure the results from queries in Chromia. This approach is beneficial when you need to fetch complex data, such as combining book information with associated reviews.

To achieve this, we will create a book_review_dto struct to encapsulate both the book details and the review information. We will then update our query to return this structured data.

### Define the struct​

We will define a new struct, book_review_dto, which will contain the book information along with the review details. This struct should be added to the src/main/entities.rell file, which is the appropriate location for defining such data structures.

Open the src/main/entities.rell file and add the following code:

src/main/entities.rell
```rell
struct book_review_dto {  book: struct;  reviewer_name: text;  review: text;  rating: integer;}
```

The struct<book> type includes all fields from the referenced book entity. When used in an expression, this triggers an implicit join to fetch those fields.

### Update the query​

Next, we will modify the get_all_reviews_for_book query to utilize the book_review_dto struct. This allows us to include both book and review information in a single query result.

Open the src/main/queries.rell file and update the query as follows:

src/main/queries.rell
```rell
query get_all_reviews_for_book(isbn: text) {  require(book @? { .isbn == isbn }, "Book with isbn %s not found".format(isbn));  val reviews = book_review @* { .book.isbn == isbn } (    book_review_dto(      book = .book.to_struct(),      .reviewer_name,      .review,      .rating    )  );  return reviews;}
```

### Explanation​

- 
Struct definition: The book_review_dto struct combines the book and review details into a single unit, making it easier to manage related information together.

- 
Updated query:

- The require function checks that the book with the specified ISBN exists.

- The query book_review @* { .book.isbn == isbn } fetches all reviews for the specified book.

- The book attribute of the struct is populated using book_review.book.to_struct(), which converts the reference into a structured object. This triggers the implicit join to retrieve all fields from the referenced book entity.

### Testing the update​

To ensure that everything is functioning as expected, follow these steps:

- 
Update the node:

If you have a Chromia node running, update it with the latest version of your dapp:

```shell
chr node update
```

If the node is not running, start it with:

```shell
chr node start
```

noteIf you update the node while it is running, it might take a moment for the changes to propagate. You may need to wait for the next block for the update to be fully effective.

- 
Run the query:

Execute the query using the Chromia CLI:

```shell
chr query get_all_reviews_for_book "isbn=ISBN1234"
```

The expected output should look something like this:

```json
[  {    "book": {      "author": "George Orwell",      "isbn": "ISBN1234",      "title": "1984"    },    "rating": 5,    "review": "It was a great book",    "reviewer_name": "Alice"  }]
```


===== FILE: courses__book-review__introduction.md =====

# Build your first app with Rell on Chromia

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Build your first app with Rell on Chromia

This course is your starting point for building applications on Chromia using Rell, the programming language behind Chromia decentralized applications (dapps). You will create BookView, a simple book review app, while learning how to model data, write queries, and sign transactions.

## What is Chromia?​

Chromia is a relational blockchain that merges the features of a relational database with blockchain capabilities. With Chromia, dapps can be developed in a way that feels familiar to developers from various backgrounds, whether they are working on enterprise applications, games, or smaller projects.

A unique feature of Chromia is Rell, a specialized language designed for both blockchain and database use. Rell offers static typing, increased expressiveness, enhanced database security, and requires up to 10 times fewer lines of code compared to other blockchains.

To learn more about Chromia and its architecture, please explore the [Chromia overview](https://docs.chromia.com).

## About the dapp​

In this course, you will build a book review app where users can add books and book reviews, as well as provide ratings. The course will also cover various methods for refining and filtering reviews based on specific criteria.

## What will I learn?​

By the end of this course, you will have acquired the following skills:

- Define entities and establish their relationships.

- Retrieve data using queries while effectively applying filters.

- Understand basic validation techniques to secure your app.

- Learn how to sign and commit transactions using cryptographic signatures.

The outcome will be a fully functional dapp where users can add books, write reviews, and retrieve reviews based on personalized filters—all powered by Rell and the Chromia blockchain.

### Related materials​

This course relies on the following documentation to help you understand the underlying concepts and approaches:

| 
| Section| Type| Documentation
| Overview| Architecture| [Nodes](https://docs.chromia.com/intro/about/architecture/node)
| Overview| Architecture| [Dapps](https://docs.chromia.com/intro/about/dapp)
| Rell| Language features| [Language features](https://docs.chromia.com/rell/language-features/)

## Repository link​

The complete code repository for this course is available here: [Book review course repository](https://bitbucket.org/chromawallet/book-course).


===== FILE: courses__book-review__rell-structure.md =====

# Rell project structure

URL: https://learn.chromia.com

- [Home](/)
- Rell project structureOn this page
# Rell project structure

Rell is the programming language used by Chromia, designed specifically for building decentralized applications (dapps). It seamlessly integrates relational data models, providing an efficient and intuitive way to develop robust applications on the blockchain. For more details, visit the [Rell introduction](https://docs.chromia.com/rell/rell-intro).

## Organizing your project​

To structure your project effectively, we will organize the code into dedicated files and folders. This approach ensures clarity, ease of management, and scalability.

### Project setup​

- 
Create the main module folder:

- Since this project will consist of a single module, create a new folder named main within the src directory. This folder will hold all module-specific files.

- 
Set up essential files:

- Inside the main folder, create the following files:

- module.rell

- entities.rell

- functions.rell

- operations.rell

- queries.rell

The entities.rell file defines your data model. In Rell, each entity corresponds to a SQL table, with keywords such as key, index, and mutable influencing how the table is stored and accessed. For more details, see [Entity](https://docs.chromia.com/rell/language-features/modules/entity).

- 
Initialize the module (module.rell):

- In the module.rell file, add the following code:

src/main/module.rell
```rell
module;
```

- The module; declaration is required in Rell. It indicates the presence of a module and instructs Rell to treat the contents of this folder as a single module, thereby enhancing organization and code management.

- 
Delete the existing main.rell file:

- The src folder currently contains a file named main.rell. While it is possible to place all your code in this file, adopting a modular structure with multiple files offers clearer organization. As part of the restructuring process, delete the existing main.rell file.

Your project structure should now look like this:

```text
book-review/├── src/│   ├── main/│   │   ├── entities.rell│   │   ├── functions.rell│   │   ├── module.rell│   │   ├── operations.rell│   │   └── queries.rell│   └── test/│       └── book_review_test.rell├── .gitignore└── chromia.yml
```

Each file serves a specific purpose within the project, promoting clear organization and efficient management as your application evolves. In the following sections, we will explore each component in detail, starting with defining entities in Rell and progressively building out the decentralized application.


===== FILE: courses__book-review__setup.md =====

# Set up your project

URL: https://learn.chromia.com

- [Home](/)
- Set up your projectOn this page
# Set up your project

Before we start, esure you have the following prerequisites in place:

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

Let’s get started by setting up your blockchain app project using the Chromia CLI.

```shell
chr create-rell-dapp book-review --template=plain
```

```shell
cd book-review
```

This command will generate the essential project files and configurations for you.


===== FILE: courses__book-review__sign-transaction.md =====

# Lesson 4 - Sign a transaction and filter queries

URL: https://learn.chromia.com

- [Home](/)
- Lesson 4 - Sign a transaction and filter queries
# Lesson 4 - Sign a transaction and filter queries

In this lesson, you will learn how to sign transactions to ensure user authenticity. You'll also create an administrative function called book_keeper to manage and add books available for review.

Additionally, you will learn how to use queries in Rell for efficient data filtering and sorting. This includes creating queries to filter and sort reviews by their rating.

## Sections
[Sign a transaction](/courses/book-review/sign-transaction/sign-transaction)[Using filters and sorting in queries](/courses/book-review/sign-transaction/structure)[Start lesson »](/courses/book-review/sign-transaction/sign-transaction)


===== FILE: courses__book-review__sign-transaction__sign-transaction.md =====

# Sign a transaction

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 4 - Sign a transaction and filter queries](/courses/book-review/sign-transaction/)
- Sign a transactionOn this page
# Sign a transaction

In this section, we will learn how to sign transactions, which involves verifying the authenticity of the user sending the transaction to the blockchain.

We will create an administration function for a bookkeeper responsible for managing and adding books available for review. Only a specific administrator will be authorized to sign the transactions required for this task.

## Setting up module arguments​

We will use module arguments to ensure that only the administrator can add books. These arguments configure a module on the Chromia blockchain and enable access to variables from a context called chain_context. Here’s how to set it up:

### Create a keypair​

First, generate a new keypair for the administrator, which will be used to sign transactions:

```bash
chr keygen --key-id="book_admin"
```

This generates a mnemonic phrase and a keypair, which are stored in your home directory under ~/.chromia/.

- Public key: ~/.chromia/book_admin.pubkey

- Private key: ~/.chromia/book_admin

- Mnemonic phrase: ~/.chromia/book_admin_mnemonic

You can then immediately copy the public key from the terminal.
Or, if you need to retrieve it later and your CLI version is 0.27.1 or higher, you can use:

```bash
chr keygen --get-pubkey book_admin
```

Check your CLI version with:

```bash
chr --version
```

If you're on an older version, either [update your CLI](https://docs.chromia.com/intro/getting-started/installation/cli-installation#updating-chromia-cli)
or retrieve the key directly from the file:

```bash
cat ~/.chromia/book_admin.pubkey; echo  # add a newline after the key
```

### Define a struct for module arguments​

Next, we need to define a struct to hold our module arguments. This struct should be added to the module where these arguments will be accessed. For this example, add it to src/main/entities.rell:

src/main/entities.rell
```rell
struct module_args {  admin_pubkey: byte_array;}
```

### Configure module arguments in chromia.yml​

Now, update your chromia.yml configuration to include the administrator's public key:

```yaml
blockchains:  book_review:    module: main    moduleArgs:      main:        admin_pubkey: 
```

Replace the placeholder with the actual key printed by the cat command above.

Explanation of the example configuration:

- book-review: This is the name of your blockchain configuration. It should correspond to the blockchain configuration you are working with.

- module: main: Specifies that the main module is used for this blockchain configuration.

- moduleArgs: Under this section, you define arguments specific to the module.

- main: This should match the module name defined in your source code.

- admin_pubkey: This is the argument name that the module will use to verify the administrator's public key.

- <your book_admin public key>: Replace this placeholder with the actual public key extracted from the .chromia/book_admin.pubkey file. This key is used to authorize administrative actions.

For more details on the chromia.yml configuration file, refer to the [Chromia project settings documentation](https://docs.chromia.com/intro/configuration/project-config).

## Require signed transactions​

Now, we will update our create_book operation to require that the administrator signs the transaction. Modify src/main/operations.rell with the following code:

src/main/operations.rell
```rell
operation create_book(isbn: text, title: text, author: text) {  val adminPubkey = chain_context.args.admin_pubkey;  require(op_context.is_signer(adminPubkey), "Only admin can create books");  create book ( .isbn = isbn, .title = title, .author = author );}
```

Here’s what happens in this operation:

- Retrieve the administrator's public key: val adminPubkey = chain_context.args.admin_pubkey;

- This fetches the public key from the module arguments defined in chain_context.

- Check if the transaction is signed by the administrator: require(op_context.is_signer(adminPubkey), "Only admin can create books");

- This checks if the transaction is signed with the administrator's public key. If not, it throws an error message: "Only admin can create books".

This ensures that only transactions signed by the administrator can create books.

## Setup and test signing​

To verify transaction signing, we will update the existing test functions to ensure correct behavior.

### Step 1: Update test_add_book​

First, add a local keypair for testing in your test file src/test/book_review_test.rell:

src/test/book_review_test.rell
```rell
val book_keeper = rell.test.keypair(    priv = x"DEE3B1414196653BF7FA621B2EEFC3146093B1932BA2ABFAEED830906D81972A",    pub = x"0359A8F2CE1BEF95F583169B7DF053AA227A93B2652B0A9C22975FEED638032610");
```

This snippet creates a test keypair for book_keeper with the provided private and public keys.

Next, update your chromia.yml to include this new keypair:

chromia.yml
```yaml
blockchains:  book_review:    module: main    moduleArgs:      main:        admin_pubkey: compile:  rellVersion: 0.14.9database:  schema: schema_book_reviewtest:  modules:    - test  moduleArgs:    main:      admin_pubkey: "0359A8F2CE1BEF95F583169B7DF053AA227A93B2652B0A9C22975FEED638032610"
```

In the test section, set the admin_pubkey to match the public key used for book_keeper.

Now, update the existing test_add_book function to utilize the book_keeper keypair:

src/test/book_review_test.rell
```rell
function test_add_book() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book("124", "Book2", "Author2"))        .sign(book_keeper)        .run();    val all_books = book @* { };    assert_equals(all_books.size(), 2);    assert_equals(all_books[0].title, "Book1");    assert_equals(all_books[0].author, "Author1");}
```

### Step 2: Update test_add_book_review​

Next, modify the existing test_add_book_review function to use the book_keeper keypair:

src/test/book_review_test.rell
```rell
function test_add_book_review() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book_review("123", "Reviewer1", "ReviewText1", 5))        .op(create_book_review("123", "Reviewer2", "ReviewText2", 3))        .sign(book_keeper)        .run();    val reviews = book_review @* { };    val book = book @ { .isbn == "123" };    assert_equals(reviews.size(), 2);    assert_equals(book, reviews[0].book);    assert_equals(reviews[0].reviewer_name, "Reviewer1");    assert_equals(reviews[0].review, "ReviewText1");    assert_equals(reviews[0].rating, 5);}
```

### Step 3: Update test_get_books​

Then, update the test_get_books function to verify the creation of books:

src/test/book_review_test.rell
```rell
function test_get_books() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book("124", "Book2", "Author2"))        .sign(book_keeper)        .run();    val books = get_all_books();    assert_equals(books.size(), 2);}
```

### Step 4: Test non-administrator access​

To further check security, create a test where a keypair not configured as an administrator attempts to sign a transaction.

The available users/keypairs are:

```text
bob, alice, trudy, charlie, dave, eve, frank, grace, heidi
```

Add a definition for the bob keypair in your test file:

src/test/book_review_test.rell
```rell
val bob = rell.test.keypairs.bob;
```

Use this keypair in a test to sign the create_book transaction:

src/test/book_review_test.rell
```rell
function test_add_book_as_non_admin() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .sign(bob)        .run_must_fail();}
```

noteThis test is expected to fail, indicating that it is correctly enforcing the test condition. Bob's public key is not listed as an administrator in the module arguments, so the transaction should fail.

## Run tests​

After updating your tests, run them to ensure everything is working as expected:

```bash
chr test
```

This command will execute all the test functions and confirm that the transaction signing and authorization are functioning correctly.


===== FILE: courses__book-review__sign-transaction__structure.md =====

# Using filters and sorting in queries

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 4 - Sign a transaction and filter queries](/courses/book-review/sign-transaction/)
- Using filters and sorting in queriesOn this page
# Using filters and sorting in queries

In this tutorial section, we'll explore how to create structured queries in Rell to filter and sort data efficiently, thereby enhancing the user experience. Rell's relational expressions follow a consistent pattern, making data retrieval straightforward and coherent.

A structured query in Rell consists of five key elements:

- FROM: Specifies the data source, which may be joined and filtered.

- CARDINALITY: Determines the result cardinality, using operators such as @, @*, @+, or @?.

- WHERE: Filters data based on conditions and joins.

- WHAT: Defines the projection, aggregation, and sorting.

- LIMIT: Restricts the number of returned elements.

Rell processes queries logically in this sequence, maintaining a clear order of execution.

## Filtering reviews by rating​

Let’s begin by creating a new query, get_reviews_by_rating, to fetch reviews based on their ratings. We will then add a filter to this query to retrieve reviews that match a specific rating. Here’s the complete query, followed by an explanation:

src/main/queries.rell
```rell
query get_reviews_by_rating(rating: integer) {  return book_review @* { .rating == rating } (    .book.to_struct(),    .reviewer_name,    .review,    .rating  );}
```

In this query:

- FROM: book_review

- CARDINALITY: @*

- WHERE: { .rating == rating }

- WHAT: ( .book.to_struct(), .reviewer_name, .review, .rating )

(Using .book.to_struct() accesses a referenced entity and triggers an implicit join.)

## Testing Filtered Reviews​

To ensure our new query correctly filters reviews by rating, let's create a test in our test module.

### Updating imports​

First, update the imports in your test file to include the new query:

src/test/book_review_test.rell
```rell
import main.{ book, create_book, get_all_books, book_review, create_book_review, get_reviews_by_rating };
```

### Adding a test function​

Next, add the following test function to verify that our query works as expected:

src/test/book_review_test.rell
```rell
function test_get_reviews_by_rating() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book_review("123", "Reviewer1", "ReviewText1", 4))        .op(create_book_review("123", "Reviewer2", "ReviewText2", 5))        .op(create_book_review("123", "Reviewer3", "ReviewText3", 4))        .op(create_book_review("123", "Reviewer4", "ReviewText4", 1))        .sign(book_keeper)        .run();    val reviews = get_reviews_by_rating(4);    assert_equals(reviews.size(), 2);}
```

In this test, we create a book and add four reviews with different ratings. We then query for reviews with a rating of 4 and assert that there are two such reviews.

## Sorting reviews by rating​

Next, let’s explore how to sort reviews by rating. We will update an existing query, get_all_reviews_for_book, to include sorting by rating. This example sorts reviews by rating in ascending order. To sort in descending order, you can use @sort_desc:

src/main/queries.rell
```rell
query get_all_reviews_for_book(isbn: text) {  require(book @? { .isbn == isbn }, "Book with ISBN %s not found".format(isbn));  val reviews = book_review @* { .book.isbn == isbn } (    @omit @sort_desc .rating, // Sort by rating, but omit this field in the result.    book_review_dto(      book = .book.to_struct(),      .reviewer_name,      .review,      .rating    )  );  return reviews;}
```

### Updating imports​

Update the imports in your test file to include this query:

src/test/book_review_test.rell
```rell
import main.{ book, create_book, get_all_books, book_review, create_book_review, get_reviews_by_rating, get_all_reviews_for_book };
```

### Adding a test function​

Then, add the following test function to verify that the reviews are correctly sorted:

src/test/book_review_test.rell
```rell
function test_get_sorted_reviews_by_rating() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book_review("123", "Reviewer1", "ReviewText1", 5))        .op(create_book_review("123", "Reviewer2", "ReviewText2", 2))        .op(create_book_review("123", "Reviewer3", "ReviewText3", 4))        .sign(book_keeper)        .run();    val reviews = get_all_reviews_for_book("123");    assert_equals(reviews.size(), 3);    assert_equals(reviews[0].rating, 5);    assert_equals(reviews[1].rating, 4);    assert_equals(reviews[2].rating, 2);}
```

In this test, we create a book and add three reviews with different ratings. We then query for reviews sorted by rating in ascending order and assert that the results are correctly sorted.

## Running the tests​

To execute your tests and verify the correctness of your queries, run the following command:

```sh
chr test
```

With these filtering and sorting capabilities, our application now allows an administrator to add books and any user to post reviews for those books. We can effectively query for reviews and books and apply filtering and sorting to the results.


===== FILE: courses__book-review__what-next.md =====

# What’s next?

URL: https://learn.chromia.com

- [Home](/)
- What’s next?On this page
# What’s next?

Congratulations! You have successfully completed the course.

Now that you have a foundational understanding of Chromia, here are some suggested next steps:

- Explore more advanced features and concepts of Chromia.

- Build more complex decentralized applications (dapps) with real-world use cases.

- Join the Chromia developer community for support and collaboration.

Congratulations on finishing this tutorial! You are now equipped to start building your own blockchain-powered decentralized applications. Happy coding!

## Join our Discord for support​

We have a Discord channel specifically for those who are new to Chromia. Feel free to post your questions or feedback about this course. It's a very active community.

You can join our Discord using this [invite link](https://discord.com/invite/chromia?ref=learn.chromia.com).

## Spread the word​

Enjoyed the course? Please share it with others!
