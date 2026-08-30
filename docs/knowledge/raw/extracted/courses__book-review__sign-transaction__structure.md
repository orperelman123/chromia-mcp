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
