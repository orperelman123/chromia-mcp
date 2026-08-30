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
