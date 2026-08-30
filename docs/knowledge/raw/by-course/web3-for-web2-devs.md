# COURSE web3-for-web2-devs — 11 pages


===== FILE: courses__web3-for-web2-devs__chromia-web3-stack.md =====

# Chromia dapp overview

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Chromia dapp overviewOn this page
# Chromia dapp overview

This diagram shows the different layers of a dapp on the Chromia platform. Similar to Web2, it has a frontend, backend, and database. The backend is written in the programming language Rell and gets deployed to the Chromia platform, where it's run on nodes hosted by a decentralized network of system providers. Dapp state gets stored in a database that's automatically managed by the Chromia core software and duplicated across multiple nodes. Transactions that mutate data in the database get stored in a blockchain, which is also duplicated across multiple nodes.

## Frontend​

Just as in Web2, the frontend remains the user's first point of interaction. Developers continue to use familiar technologies such as HTML, CSS, and JavaScript to craft user interfaces.
This ensures a continued focus on user experience, responsiveness, and aesthetics.

To create or update data in our dapp, we send transactions to the Chromia blockchain network.
We can use tools like the Chromia TypeScript client library, postchain-client, to make this as easy as sending data to a traditional API. We'll illustrate this with real code examples in the next section.

For a transaction to be valid it needs to be signed with the user's cryptographic keys.
A transaction signature acts like a digital fingerprint, unique to both the transaction and the user who created it.
It verifies that the sender is who they claim to be and that the transaction has not been tampered with after being sent.

Cryptographic keys are typically stored in a wallet app on the user's device. When a user wants to send a signed transaction to interact with a dapp on the Chromia network, the dapp frontend uses the wallet app's API to request and generate a signature. This signature is then attached to the transaction, making it valid and secure.

That's it; the wallet generates a digital signature, the signature is attached to the transaction, and the transaction is sent, with the signature, to a Chromia node running the dapp backend.
This shows that the transition to Web3 using Chromia is not that big of a step from Web2.
We will dive deeper into this in the next lesson, but first, let's look at the backend.

## Backend​

Dapp backends on the Chromia platform are written in Rell, Chromia's relational programming language.
Rell is designed to be easy to learn and convenient to use for any developer, especially if you already know SQL.

Rell code is deployed to a so-called container on the Chromia platform, and each dapp has its own blockchain managed by the Chromia network. As soon as a dapp is deployed, you can send transactions and query data via your dapp frontend.
Deploying Rell code will automatically create the necessary tables and relations to hold a dapp's state in a PostgreSQL database, automatically managed and duplicated across multiple Chromia nodes.

Each interaction with the backend is similar to sending a request to a traditional Web2 API.
Sending a transaction to a Chromia node is like making a POST request to a web service. Besides signed transactions, you can also send read-only queries to a node, which is similar to making a traditional GET request.
This is all you have to think about as a developer, so the jump from Web2 is not that big, but let's look at what happens behind the scenes when you send a transaction, for the sake of curiosity.

A Chromia node receives your transaction and begins a round of communication among the other Chromia nodes running your dapp (also known as validators). Together, they ensure that a "supermajority" agrees on the transaction's validity before it is executed. This is similar to multiple servers performing cross-checks before updating a database in a distributed Web2 system.

When two-thirds of validator nodes agree that the transaction is valid, a consensus has been reached, and the transaction is executed.
Executing a transaction means that the operations contained in it are executed. (We'll look more closely at what operations are in the next section.)
This results in state changes that are applied to the dapp's database, similar to performing database actions in a Web2 backend.
The transaction is also recorded on the blockchain, ensuring that it is immutable and reflected across all nodes.

This sums up the Chromia Web3 stack. Again, we can see it is not an entirely different world. Web3 and Web2 have many similar concepts and terminology.


===== FILE: courses__web3-for-web2-devs__classic-web2-stack.md =====

# Traditional web app overview

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - Traditional web app overviewOn this page
# Traditional web app overview

In Web2, the architecture of a web app can be broadly categorized into three layers:

## Frontend​

- This is the user interface, the part of the app users interact with directly.

- It's primarily crafted using languages like HTML, CSS, and JavaScript.

- Frameworks such as React, Angular, and Vue.js have become popular for developing more dynamic and responsive apps.

## Backend​

- Acts as the brains of the app, processing user requests, performing operations, and returning the desired data.

- Commonly used languages and frameworks include Node.js, Python (Django, Flask, etc.), Ruby on Rails, and Java.

- The backend also manages authentication, ensuring data security and integrity.

## Database​

- This is where the app's data is stored, retrieved, and manipulated.

- Solutions range from relational databases like MySQL, PostgreSQL, and Oracle to NoSQL databases like MongoDB, CouchDB, and Cassandra.

These layers interact with each other, ensuring a smooth and efficient user experience.
Next, we examine how the frontend, backend, and database evolve when we move to Web3 and the Chromia stack.


===== FILE: courses__web3-for-web2-devs__compare-authentication.md =====

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


===== FILE: courses__web3-for-web2-devs__compare-backend.md =====

# Comparing backends

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - Comparing backendsOn this page
# Comparing backends

Now, it's time to get practical. Let's illustrate the differences and similarities between a traditional Web2 app and a Chromia dapp using real code examples. We'll learn some Chromia development concepts along the way. Still, for an actual deep dive into dapp development, you may later want to check out our [intro to dapp development course](https://learn.chromia.com/courses/book-review/introduction) or our [advanced dapp development course](https://learn.chromia.com/courses/my-news-feed/introduction).

For our code examples, we'll use a highly simplified version of the dapp you built in the advanced dapp development course; [a social media news feed dapp](https://learn.chromia.com/courses/my-news-feed/introduction/). To keep our examples short, all we'll do is create users, allow users to create posts, and fetch a list of posts.

Let's examine the practical differences between building this app for Web2 and Web3 on Chromia. We'll start with the backend, first reminding ourselves of what a simple Web2 backend looks like and then compare that to developing on Chromia.

## Web2 backend with Node.js, Express, and PostgreSQL​

There are countless tech stacks you can use to build a Web2 app. In this example, we'll use Node.js, Express, and PostgreSQL, but the same principles would apply if we used other frameworks or other languages.

To create our backend, we'd first set up a PostgreSQL database and host it somewhere, for example, on AWS RDS or Google Cloud SQL.

Once we have a database, we can create a Node.js service that connects to it using the Node.js PostgreSQL driver or an ORM (Object Relational Mapper) such as [Sequelize](https://sequelize.org/). With Sequelize, it would look something like this:

```javascript
// db.jsimport { Sequelize, DataTypes, Model } from "sequelize";const sequelize = new Sequelize(  "postgres://username:password@localhost:5432/mydatabase",  {    dialect: "postgres",    logging: false,  });// User Modelclass User extends Model {}User.init(  {    name: { type: DataTypes.STRING, allowNull: false },    id: { type: DataTypes.INTEGER, autoIncrement: true, primaryKey: true },  },  { sequelize, modelName: "User", timestamps: false });// Post Modelclass Post extends Model {}Post.init(  {    timestamp: { type: DataTypes.DATE, defaultValue: DataTypes.NOW },    userId: {      type: DataTypes.INTEGER,      references: { model: User, key: "id" },    },    content: { type: DataTypes.TEXT, allowNull: false },  },  {    sequelize,    modelName: "Post",    timestamps: false,    indexes: [{ fields: ["userId"] }],  });// RelationsUser.hasMany(Post, { foreignKey: "userId" });Post.belongsTo(User, { foreignKey: "userId" });export default { sequelize, User, Post };
```

Here, we've a User model and a Post model. This is a naïve example, of course. An actual app would typically have more models, but as mentioned, we'll keep things simple to compare Web2 and Web3 on Chromia.

Next, we need to expose an API that allows for creating users and posts and fetching a list of users. We'll use the popular JavaScript web framework [Express](https://expressjs.com/) for this:

```javascript
// server.jsimport express from "express";import bodyParser from "body-parser";import { User, Post } from "./db.js";const app = express();const PORT = 3000;app.use(bodyParser.json());// Create a userapp.post("/users", async (req, res) => {  try {    const user = await User.create(req.body);    res.status(201).send(user);  } catch (error) {    res.status(400).send(error.message);  }});// Create a postapp.post("/posts", async (req, res) => {  try {    const post = await Post.create(req.body);    res.status(201).send(post);  } catch (error) {    res.status(400).send(error.message);  }});// Fetch a list of users with simple paginationapp.get("/users", async (req, res) => {  try {    const limit = req.query.limit ? parseInt(req.query.limit, 10) : 10;    const offset = req.query.offset ? parseInt(req.query.offset, 10) : 0;    const { count, rows } = await User.findAndCountAll({      limit: limit,      offset: offset,    });    res.status(200).send({ total: count, data: rows });  } catch (error) {    res.status(500).send(error.message);  }});app.listen(PORT, () => {  console.log(`Server is running on port ${PORT}`);});
```

We'll skip authentication and authorization for now, and return to that later in this course.

Now, we can deploy our Node.js service to a cloud hosting provider such as AWS, Google Cloud, or Heroku.

## Web3 backend with Chromia entities and operations​

To do the same thing for Web3 on Chromia, we need to write a dapp backend in the Rell programming language. We don't need to worry about hosting a database. That's taken care of behind the scenes by the Chromia platform.

Let's define our data model in Rell:

```rell
entity user {  mutable name;  key id: byte_array;}entity post {  timestamp = op_context.last_block_time;  index user;  content: text;}
```

Deploying this code will automatically create the necessary tables in Chromia's distributed relational database. Chromia uses PostgreSQL under the hood, but you don't interact with it directly. Instead, servers (nodes) that run Chromia core software interpret Rell code and translate it into SQL queries. That way, the database is kept secure and individual dapps can only update their own data.

Now, let's expose an API that allows us to create users and posts, as well as fetch a list of users:

```rell
operation create_user(name, pubkey) {  create user(name, pubkey);}operation make_post(user_id: byte_array, content: text) {  create post(    user @ { user_id },    content  );}query get_users(pointer: integer, n_users: integer) {  val users = user @* {} ( .name ) offset pointer limit n_users;  return (    pointer = pointer + users.size(),    users = users  );}
```

In Rell, an operation mutates data in the database. A query is read-only and used to fetch data.

That's it! Operations and queries in Rell are automatically exposed. They can be called by sending transactions or queries to our news feed dapp on the Chromia network.

Learn more about RellIf you want a more in-depth explanation of this Rell code, there's a [whole course](/courses/my-news-feed/introduction) on building a Chromia Dapp that expands on this example.

Dapp code is deployed to the Chromia Network using the Chromia CLI. To read about the Chromia CLI and how to deploy to a network, check out the [Chromia Docs](https://docs.chromia.com/).


===== FILE: courses__web3-for-web2-devs__compare-frontend.md =====

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


===== FILE: courses__web3-for-web2-devs__introduction.md =====

# Web3 for Web2 developers

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Web3 for Web2 developers

Welcome to our course on making the jump from Web2 development to building decentralized applications (dapps) in the new world of Web3.
The course is designed to fill knowledge gaps and equip you with practical skills to understand the future of the web as a developer.

We will compare the architecture of traditional Web2 applications with that of Web3 dapps on the Chromia platform.
Additionally, we will explore code examples to understand how familiar concepts from Web2 app development correspond to dapp development on Chromia.

At the end of the course, we will look at concepts beyond just programming, like scalability, security, and operational costs on Web3 platforms.

This course relies on the following documentation, which can help you understand the underlying concepts:

| 
| Section| Type| Documentation
| Overview| Architecture| [Chromia Architecture](https://docs.chromia.com/intro/architecture/platform-architecture)
| FT4| Overview| [FT4 Introduction](https://docs.chromia.com/ft4/intro)

## What is Web3, and why does it matter?​

The internet, as we know it, has undergone two significant evolutions in the past. We are now entering the third.

Web1, often called the Static Web, was the internet in its early form.
It consisted mainly of static pages, which meant they didn't change unless someone manually updated them.
Think of it as reading a newspaper online.
You could visit a website to check out an article, view a schedule, or download a document, but your ability to interact with content or contribute your own was limited.

## Web1: The static web​
Static, read-only pages primarily designed for information consumption. A stage of the internet where users primarily consumed content with limited interaction or contribution.

- Architecture: Centralized servers hosting websites with static HTML pages

- Key Technologies: HTML, CGI

- Business Model: Banner ads, online directories, subscriptions

- Examples: Early versions of sites like Yahoo!, AltaVista, GeoCities

Then emerged Web2, enabling users to create, share, and modify content through dynamic platforms rather than just consume.
On platforms like Facebook and YouTube, anyone could share their story or showcase a talent, transforming users from passive consumers into active participants.
These platforms weren't just websites. They were communities where your input mattered.
The web became a space for collaboration and sharing on a scale never seen before, but the platforms are still controlled by central authorities, with users having little to no say in how they are run.

## Web2: The social web​
Dynamic platforms with user-generated content and interactivity. A shift from passive consumption to active collaboration.

- Architecture: Centralized platforms, web applications

- Key Technologies: AJAX, RSS, Blogs, APIs

- Business Model: Targeted advertising, affiliate marketing, SaaS

- Examples: Facebook, YouTube, Twitter, WordPress

Web3 is the current shift.
It's fundamentally about giving the power back to users through blockchain and decentralized technologies.
In Web3, data isn't stored in a central repository but is spread across a network of nodes, ensuring that no single entity has full authority over it.
Users maintain control and ownership of their data through cryptographic keys.

## Web3: The decentralized web​
A decentralized and interconnected web, using technologies like blockchain, where users not only generate content, but truly own content and take part in governing the platforms where that content is shared. With the introduction of cryptocurrencies, economies can be built and managed entirely in code without the need to trust central authorities.

- Architecture: Decentralized platforms and applications, often built on blockchain technology

- Key Technologies: Blockchain, smart contracts, decentralized applications (dapps), tokens, cryptocurrencies

- Shared Governance: Consensus-driven approach to decision-making, particularly in decentralized applications (dapps)

- Protected Data Ownership: Data is publicly stored, yet ownership is privately controlled. Cryptographic keys ensure that while anyone can see data on the network, only owners with the keys can access or modify it, ensuring user control over personal data.

- Examples: Chromia, Ethereum, Polkadot, Polygon

This gives us a brief overview of the history of the web and some initial insights into what Web3 is about. In the next lesson, we'll get more practical, comparing the tech stack of a traditional web app to that of a dapp on the Chromia platform.


===== FILE: courses__web3-for-web2-devs__revenues-and-op-costs.md =====

# Revenue models and operational costs

URL: https://learn.chromia.com

- [Home](/)
- Lesson 9 - Revenue models and operational costsOn this page
# Revenue models and operational costs

To run an app on the web, whether a centralized web app or a dapp, someone must pay for hosting. In Web2, this is relatively simple. A company runs servers and offers those servers to other companies or developers for a fee. The fee can be a fixed subscription or calculated based on usage. Developers pay the hosting company using traditional fiat currency.

In Web3, things work differently. Since platforms are decentralized, there's no single company in control of all servers. Servers, or nodes, are run by many companies or individuals who contribute resources to the network and, in return, get paid in cryptocurrency. That way, all payments can be managed by the system in code, and there's no need for a central legal entity that collects and distributes money.

## The problem with gas fees​

In early Web3 platforms, such as Ethereum, hosting is paid by users when sending individual transactions. This fee, on Ethereum referred to as gas fee, is essentially a bid put up by the user to get their transaction included in the next block. If the fee is competitive enough, the transaction will get included in a block. The gas fee for a given transaction is calculated based on the computational resources required to run that particular transaction and the demand on the network at that time.

This means that every time users want to perform an action that affects dapp state on the blockchain, they have to pay a small fee. Imagine using a social media app where you must pay every time you post or a work collaboration tool with a fee to update a task. Not the best user experience.

## Chromia's flexible fee model​

Chromia solves this problem by putting the fee structure back in the hands of developers. As a developer, you host your dapp in a container on the network for a predictable fee. You know the cost and resources of your dapp, just like on a traditional cloud hosting platform. Hosting fees are paid by sending cryptocurrency to a dedicated address on the system and are automatically distributed among providers running nodes on the network.

How to collect currency from users is then up to you. You can, of course, have the equivalent of gas fees, charging a small amount every time a user performs an action, but you can also implement a subscription model, a freemium model, or even subsidize your dapp by collecting money off-chain if you'd like. You have the power to put user experience first and build whatever model makes the most sense for your specific dapp, just like in Web2.

## Web2 app economy​
Revenue models

- Often ad-driven, where user data is monetized.

- Subscription models and one-time purchases are also prevalent.

- Direct sales and affiliate marketing are common in e-commerce platforms.
Operational costs

- Ongoing server maintenance, data storage, and infrastructure costs.

- Licensing fees for certain software or platforms.

## Web3 dapp economy​
Revenue models

- Token monetization allows for unique revenue streams, such as Token Sales. Capital can be raised through Initial Coin Offerings (ICOs).

- Users can earn by participating in network validation or staking.

- Decentralized finance (DeFi) offers interest earnings, lending, and other financial instruments.

- Using Chromia, there is always the possibility of using a subscription model, freemium model, pay-by feature, or even collecting money off-chain, just like in Web2.
Operational costs

- Early Web3 platforms like Ethereum use gas fees that are unpredictable and make the end-user pay for every action.

- Chromia features a predictable hosting fee where you pay a recurring subscription for a set unit of resources, much like on modern Cloud services today.

## Simple example of a revenue model on Chromia​

Chromia's flexible fee structure means you can implement dapp revenue models that are impossible on earlier Web3 platforms. For example, you could implement a freemium model, with some users paying a subscription to gain access to premium features while keeping the dapp free for the rest. Premium users pay their subscription in cryptocurrency. Part of the collected fees can automatically be used to pay for hosting on Chromia, and part can be sent to an account controlled by you as payment for developing and maintaining the dapp.

To summarize:

- User 1 and User 2 pay a subscription fee and get access to premium features.

- User 3 has a free account and has access to free features.

- User 1 and User 2's subscription fees cover operation costs; part of it can be earnings for the dapp developer.


===== FILE: courses__web3-for-web2-devs__scalability.md =====

# Scalability

URL: https://learn.chromia.com

- [Home](/)
- Lesson 7 - ScalabilityOn this page
# Scalability

Scaling in Web3 looks different than scaling in Web2, and the decentralized nature of dapps introduces new challenges. In this section, we'll briefly outline some of these challenges and look at how they're more easily alleviated on the Chromia platform than on other Web3 platforms.

## Potential scalability issues in Web3​

On Web3 platforms like Ethereum, the network gets congested during peaks in activity. Limited block space means that high demand can lead to a backlog of unconfirmed transactions and a fee surge. This can happen due to ICOs (Initial Coin Offerings) unrelated to your dapp, new token launches, or other events that lead to a sudden increase in transactions being broadcast to the network.

## How Chromia solves network congestion​

Every dapp on Chromia exists on its own sidechain, capable of handling hundreds of transactions per second. Each sidechain runs in a so-called container, part of a cluster. If a dapp grows, it can deploy additional sidechains to its container or even run multiple containers on multiple clusters.
This means your dapp can scale and keep throughput high as it becomes more popular.

The sidechain architecture also means that your dapp is protected from surges in activity from other dapps. Since your dapp is isolated, with its own blockchain and resources, you don't need to worry about congestion due to activity on the rest of the network.

## Scaling in Web2​

- Applications can often scale by adding more servers or resources to centralized infrastructure.

- Mature infrastructure makes handling high amounts of traffic and data easier.

- A bottleneck at a single point can lead to system-wide slowdowns.

## Scaling in Web3​

- Network congestion occurs on some platforms when there's high demand.

- Potential scalability issues due to consensus mechanisms and design.

- Chromia's sidechain architecture solves the network congestion problem, allowing dapps to scale.

## Scalability example​

Let's take the example of a social media app like Reddit. As the number of users, views, and posts increases, the load on the system increases. In Web2, we can add resources on the backend to handle the load, but in Web3, it can be challenging. On platforms like Ethereum, you can work to optimize your smart contract, but that can only go so far. We're limited to the network's resources and the throughput it can handle.

On Chromia, on the other hand, we've the option to scale horizontally. We can deploy additional sidechains as the number of users and posts increases. These are logical replicas of our dapp, but they each have their own blockchain.

To use this strategy, we need to build our dapp architecture to consider this. In our Reddit-like example, we can put different subreddits (forum topics) on different sidechains, each with its own resources. The client can then manage and direct requests for specific subreddits to their respective sidechains. This way, we've load-balanced our dapp and have much better prospects of scaling than on other Web3 platforms.

Next, we'll look at security and see how Web3 is inherently more secure than Web2.


===== FILE: courses__web3-for-web2-devs__security.md =====

# Security

URL: https://learn.chromia.com

- [Home](/)
- Lesson 8 - SecurityOn this page
# Security

Web3 platforms are architected to be decentralized and trustless from the ground up. Security is built into the very core of their design.

What does this mean in practice? For one thing, the decentralized nature of dapps implies that data and logic is replicated across multiple servers controlled by multiple entities. This inherently makes the system less vulnerable to attacks because a hacker would need to compromise multiple systems at once.

Your app is also more resistant to single points of failure. If one node is down, there are always other nodes in the system capable of running your dapp.

Blockchain platforms use cryptography to maintain data integrity. Once a transaction is added to the blockchain, it can't be altered. Furthermore, transactions are cryptographically signed by users to verify authenticity and establish ownership that can't be disputed.

These features are built into platforms, giving Web3 an edge in terms of security.

## Web2 security​

- Well-established authentication and security solutions that are integrated and widely available in most Web2 stacks.

- Centralized systems present a single point of failure. If compromised, the entire system can be at risk.

## Web3 security​

- Decentralized nature reduces single points of vulnerability.

- Use of cryptography as an inherent part of the system ensures data integrity and user authentication.

## Chromia makes authentication easy for developers​

While traditional Web2 authentication systems are familiar and mature, they do come with their own challenges.
As seen in our authentication comparison, Chromia introduces a straightforward and secure way to authenticate users with the Chromia FT4 library.
Chromia FT4 offers a developer-friendly way to authenticate and authorize users when signing and sending transactions, ensuring that authentication is no more difficult than in Web2 but with the added security of Web3.

Next, we'll look at how to collect revenue in dapps and how to pay to run them.


===== FILE: courses__web3-for-web2-devs__summary.md =====

# Summary

URL: https://learn.chromia.com

- [Home](/)
- Summary
# Summary

This course has offered an exploration of the transition from Web2 to Web3, placing particular emphasis on Chromia's unique approach.

We've delved into the web's history to understand why Web3 represents the next evolutionary step.
The course has compared and analyzed the development stacks of traditional Web2 and the emerging Web3, providing hands-on code examples to illustrate the differences and similarities.
We've also extended our focus beyond mere development, considering critical aspects like scalability, security, and economy.

We hope the course has made the jump to Web3 seem a little less daunting. To get more hands-on with Rell and dapp development on Chromia, we encourage you to check out our [intro to dapp development course](/courses/book-review/introduction) and our [advanced dapp development course](/courses/my-news-feed/introduction).

Happy coding!


===== FILE: courses__web3-for-web2-devs__web3-benefits.md =====

# Benefits and challenges of Web3

URL: https://learn.chromia.com

- [Home](/)
- Lesson 6 - Benefits and challenges of Web3On this page
# Benefits and challenges of Web3

So far, we've looked at how to move from Web2 to Web3 on Chromia with the help of code examples, focusing on what you need to think about during this transition. The examples show that using Rell and other Chromia tools isn't as big a step as it might first seem.

The way Chromia works fits well with what most Web2 developers already know. But it's important to remember that there's more to the switch than just working on data models, business logic, and client interfaces. As a developer or someone invested in the project, you should also consider scalability, security, how you'll make money, and the costs of running everything.

Let's zoom out and consider some of the overall benefits and challenges of Web3.

## Benefits of Web3​

- Decentralized infrastructure: Information and control are distributed across nodes, ensuring no single point of authority.

- Token monetization: Ability to integrate native tokens or cryptocurrencies for new revenue models.

- Immutable data: Once data is written to a blockchain, it cannot be changed, ensuring data integrity.

- Governance: Users are more involved in the governance of the app. Which means they can vote on changes and upgrades.

## Challenges of Web3​

- New concepts: Requires understanding of new concepts like consensus algorithms, dapps, operational costs, and fees.

- Scalability concerns: Current blockchain infrastructures can have throughput limitations and are hard to scale depending on load.
However, we will show in this module that this is different for Chromia.

- End user complexity: Convincing users to set up and manage digital wallets can be a barrier to entry for many new applications.

These benefits and challenges can be broken down into three areas:

- Scalability

- Security

- Revenues and operational costs

Next, we'll examine these areas more closely and discuss what you, as a developer, can do to mitigate the challenges and take advantage of the benefits.
