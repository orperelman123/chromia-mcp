# Querying the blockchain with postchain-client

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 6 - Build the client](/courses/book-review/build-client/)
- Querying the blockchain with postchain-clientOn this page
# Querying the blockchain with postchain-client

In this section, we will explore how to query the Chromia blockchain to retrieve information about transactions and the state of our dapp table.

Querying with postchain-client is straightforward. Let's look at an example that fetches all books and breaks them down.

First, we need to define a type for Book to hold our book information:

```typescript
type Book = {  isbn: string;  title: string;  author: string;};
```

Next, we will create a function to fetch and display all books:

```typescript
const getAllBooks = async () => {  const bookList = await client.query("get_all_books", {});  console.log("Book list:", bookList);};
```

In this function, we use the client and the query command to execute our query. We specify the return type as a generic type, in this case Book[].

### Function arguments​

#### First argument: queryFunctionName​

This is the name of the query function defined on the blockchain. It corresponds to a function in a Rell script that retrieves all book records.

#### Second argument: queryArguments​

An empty object ({}) indicates that this query does not require any specific arguments to execute. However, this object could contain parameters that the blockchain function uses to filter or process the results if necessary.

## Handling the response​

The client.query method returns a promise that, when resolved, yields an array of books (Book[]). These retrieved books can then be displayed, processed, or utilized according to your application's needs. In this example, we output the results to the console.

We can call getAllBooks(); from our main function, as shown below. After adding this, you can re-run the client to see your added book listed. To restart and test the example code, you can use the command chr node start --wipe to start a new node without any existing data.

```typescript
async function main() {  client = await createClient({    nodeUrlPool: "http://localhost:7740",    blockchainRid: blockchainRID,  });  console.log("Creating a new book transaction");  await client.signAndSendUniqueTransaction(    { name: "create_book", args: ["ISBN1", "Chromia 101", "John Doe"] },    bookKeeperSignatureProvider  );  getAllBooks();}
```
