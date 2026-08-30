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
