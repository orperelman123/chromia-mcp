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
