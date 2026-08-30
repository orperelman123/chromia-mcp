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
