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
