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
