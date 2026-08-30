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
