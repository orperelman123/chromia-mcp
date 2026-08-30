# Comparing frontends

URL: https://learn.chromia.com

- [Home](/)
- Lesson 4 - Comparing frontendsOn this page
# Comparing frontends

Next, we'll look at how to call our Web2 REST API from a frontend client and compare that with sending transactions and queries to our Chromia Web3 dapp.

## Web2 frontend with Axios​

Now that we have a REST API for our Web2 app, we need a frontend client with an interface that users can interact with. Let's look at how one might use JavaScript to call our API. We could use the native JavaScript Fetch API, but let's use the [Axios](https://axios-http.com/) library to make our code slightly more clean:

```javascript
// frontend.jsimport axios from "axios";// Base URL for the APIconst API_BASE_URL = "http://localhost:3000";// Create a userfunction createUser(name, id) {  return axios    .post(`${API_BASE_URL}/users`, { name, id })    .then((response) => response.data);}// Create a postfunction createPost(userId, content) {  return axios    .post(`${API_BASE_URL}/posts`, { userId, content })    .then((response) => response.data);}// Fetch users with paginationfunction fetchUsers(offset = 0, limit = 10) {  return axios    .get(`${API_BASE_URL}/users`, {      params: { offset, limit },    })    .then((response) => response.data);}// Example Usage:// createUser('John Doe', 'unique_id_as_byte_array');// createPost('USER_OBJECT_ID_HERE', 'This is a post content.');// fetchUsers();
```

This should look familiar to most web developers. Next, let's see the equivalent of this on the Chromia platform.

## Web3 Frontend with Chromia's postchain-client​

To do the same thing on Chromia, we must mutate data by sending transactions to the Chromia blockchain network. To make this easy, we can use the Chromia TypeScript client library, [postchain-client](https://bitbucket.org/chromawallet/postchain-client/).

What is Postchain?Postchain is the name of the underlying blockchain platform technology that Chromia runs on. It's developed and maintained by the Chromia team and is entirely [open source](https://gitlab.com/chromaway/).

Here's how we'd use postchain-client to send transactions equivalent to the REST API calls from earlier:

```typescript
import { createClient } from "postchain-client";// Our dapp's unique ID when deployed to the Chromia network. We get this when// we deploy our dapp.const blockchainRid = "...";// URLs to system nodes in the Chromia network. These are automatically queried by the// client library to find the URLs of the specific nodes running our particular dapp.const directoryNodeUrlPool = ["url1", "url2", "url3", "etc."];const chromiaClient = await createClient({  directoryNodeUrlPool,  blockchainRid,});// Create a userfunction createUser(name, id) {  return chromiaClient.sendTransaction(    {      name: "create_user",      args: [name, id],    }  );}// Create a postfunction createPost(userId, content) {  return chromiaClient.sendTransaction(    {      name: "make_post",      args: [userId, content],    }  );}// Example Usage:// createUser('John Doe', 'unique_id_string');// createPost('USER_OBJECT_ID_HERE', 'This is a post content.');
```

We don't need to send a transaction to fetch users via the get_users query we defined in our dapp backend. Transactions (almost always signed with cryptographic keys) are only necessary when mutating data. To call our read-only query, we do this:

```typescript
const users = await chromiaClient.query("get_users", {  pointer: 0,  n_users: 10,});
```

Next, we'll look at how to add authentication and authorization to our dapp.
