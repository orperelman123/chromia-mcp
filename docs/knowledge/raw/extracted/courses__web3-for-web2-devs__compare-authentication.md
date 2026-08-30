# Authentication

URL: https://learn.chromia.com

- [Home](/)
- Lesson 5 - AuthenticationOn this page
# Authentication

Now that we have our basic backends and frontends set up, we need to add user accounts and authentication to ensure that users can only perform the actions they're supposed to.

In Web2, authentication typically relies on usernames and passwords or third-party identity providers such as Google or Facebook. Users create accounts, and their credentials or authentication tokens are stored on centralized servers.

In Web3, on the other hand, authentication relies on cryptographic keypairs. A user is identified on the network using their public key and proves their identity by signing transactions with their private key. Blockchain wallet applications like Metamask or Trust Wallet often manage keys. The user is in complete control of their identity on the network.

What does this look like when developing Web2 applications and Chromia dapps? Let's compare.

## Web2 auth with JSON web tokens​

We won't go into too much detail about authentication in Web2 to keep things simple, but let's remind ourselves of the core concepts. One common way of handling authentication in Web2 is using JSON Web Tokens (JWTs). There are third-party authentication services that can help make things easier, such as Auth0 or Firebase. Typically, users log in with a username and password, which get sent to the auth service. The auth service responds with a JWT stored in the user's browser.

Once the user is authenticated and has a valid JWT, the frontend can include the token in API calls to the backend. Adding this to our createPost function from earlier would look something like this, assuming we're using Auth0:

```javascript
// Create a postfunction createPost(content) {  return auth0Client    .getIdTokenClaims()    .then((tokenClaims) => {      return axios.post(        `${API_BASE_URL}/posts`,        { content },        {          headers: {            Authorization: `Bearer ${tokenClaims.__raw}`,          },        }      );    })    .then((response) => response.data);}
```

Here, we add our auth token as an Authorization header in our API call. We can then check the validity of that token on the backend:

```javascript
// server.jsimport express from "express";import bodyParser from "body-parser";import jwt from "express-jwt";import jwksRsa from "jwks-rsa";import { User, Post } from "./db.js";const app = express();const PORT = 3000;app.use(bodyParser.json());// Auth0 middleware configurationconst checkJwt = jwt({  secret: jwksRsa.expressJwtSecret({    jwksUri: "YOUR_AUTH0_DOMAIN/.well-known/jwks.json",  }),  audience: "YOUR_AUTH0_AUDIENCE",  issuer: "YOUR_AUTH0_DOMAIN",  algorithms: ["RS256"],});app.use(checkJwt);// Express routes here, as in our earlier example...app.listen(PORT, () => {  console.log(`Server is running on port ${PORT}`);});
```

We can also update our endpoint logic to take the user information in the JWT and ensure the user is authorized to perform any given action. For example, instead of submitting userId via the API when creating a post, we can check that the user is authenticated and then get their userId with the help of the JWT so that users can only create posts linked to their own account.

Now, let's see how this process is different in Web3 and on Chromia.

## Web3 Auth with Chromia's FT4 Protocol​

In Web3, users hold cryptographic keypairs (often managed in a wallet app on a device controlled by the user) that sign transactions before they're sent to the blockchain platform. The blockchain platform can then verify the signature to check if the user who sent the transaction is who they say they are.

Chromia provides a protocol called FT4 that makes this easy. The FT4 library integrates with popular crypto wallets and helps you create operations that allow users to register accounts.

Creating user accountsTo read more about registering and creating user accounts, check out the [advanced dapp course](/courses/my-news-feed/introduction).

In this example—just like in the Web2 example—we'll assume that the user's account has already been created in our dapp. With that in mind, let's add authentication and authorization to our operations in Rell. All we need to do is add an auth handler and a call to function auth.authenticate() in our operations, like so:

```rell
import lib.ft4.auth;@extend(auth.auth_handler)function () = auth.add_auth_handler(  flags = ["MySession"]);operation make_post(content: text) {  val account = auth.authenticate();  require(content.size() 
Now that we have authentication and authorization on the backend, let's look at the frontend. In our example, we expect the user to have a cryptographic keypair managed in the Metamask wallet app. We'll use the Chromia FT4 typescript library to integrate with Metamask and allow users to authenticate themselves.

We need some JavaScript to allow users to connect their Metamask wallet to our dapp frontend, and log in if the Metamask key is known.

```typescript
async function setupAuthAndLoginUser() {  // 1. Connect with MetaMask  const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);  // 2. Get all accounts associated with evm address  const evmKeyStoreInteractor = createKeyStoreInteractor(client, evmKeyStore);  const accounts = await evmKeyStoreInteractor.getAccounts();  if (accounts.length > 0) {    // 3. Start a new session    session = await evmKeyStoreInteractor.getLoginManager().login({      accountId: accounts[0].id,      config: {        flags: ["MySession"],      },    });  }}
```

Our function, setupAuthAndLoginUser, fetches a list of accounts associated with the user's Metamask wallet address.
If there's an account associated with the address, we create a new session for the user with the FT4 library's login function.

Now the user is authenticated, and we can sign transactions to send to our dapp backend. Instead of using the postchain-client library directly to call operations, we'll now use our FT4 session instance to call operations. The FT4 library will automatically bundle the operation into a transaction and sign it for us:

```typescript
function createPost(content) {  return await session.call({    name: "make_post",    args: [content],  });}
```

That's it! These are the cornerstones of building a production-ready Web3 dapp on Chromia. Of course, this was just a brief overview. To dive deeper into these concepts, including the Rell language and the various Chromia libraries, check out our [intro to dapp development course](/courses/book-review/introduction) or our [advanced dapp development course](/courses/my-news-feed/introduction).

Next, we'll look at higher-level concepts beyond just programming, namely scalability, security, and economics of Web3 dapps.
