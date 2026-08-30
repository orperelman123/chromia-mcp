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
