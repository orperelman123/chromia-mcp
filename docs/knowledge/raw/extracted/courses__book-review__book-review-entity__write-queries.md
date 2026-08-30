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
