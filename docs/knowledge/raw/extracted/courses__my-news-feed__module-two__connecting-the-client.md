# Connect the client

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - React project](/courses/my-news-feed/module-two/)
- Connect the clientOn this page
# Connect the client

In this section, a connection between the frontend and the blockchain is established using the [postchain-client](https://www.npmjs.com/package/postchain-client) library along with the [FT4-wrapper](https://www.npmjs.com/package/@chromia/ft4). A [ContextProvider](https://react.dev/learn/passing-data-deeply-with-context) is created and custom [React hooks](https://react.dev/learn/reusing-logic-with-custom-hooks#extracting-your-own-custom-hook-from-a-component) are defined to seamlessly integrate blockchain capabilities into the app.

## Create a context​

To manage blockchain-related data and state efficiently, a context needs to be created within the app. This context will serve as a central hub for handling blockchain interactions. A file named ContextProvider.tsx is created within the src/components directory. In this file, the contexts are declared and defined:

src/components/ContextProvider.tsx
```typescript
"use client";import {  Session,  createKeyStoreInteractor,  createSingleSigAuthDescriptorRegistration,  createWeb3ProviderEvmKeyStore,  hours,  registerAccount,  registrationStrategy,  ttlLoginRule,} from "@chromia/ft4";import { createClient } from "postchain-client";import { ReactNode, createContext, useContext, useEffect, useState } from "react";import { getRandomUserName } from "../app/user";// Create context for Chromia sessionconst ChromiaContext = createContext(undefined);export function ContextProvider({ children }: { children: ReactNode }) {  // Initialize session and EVM address states  const [session, setSession] = useState(undefined);  // Additional state initialization will be defined here  return {children};}
```

In the ContextProvider component, a context is defined:

- ChromiaContext: This context holds a Session object, serving as a wrapper for the FT4 client. It manages the state of the current session, including key pairs for signing transactions.

The respective states are initialized within this component and the app components are wrapped with these contexts. To utilize this context, the NavBar and {children} tags are wrapped within src/app/layout.tsx:

src/app/layout.tsx
```typescript
import { ContextProvider } from '@/components/ContextProvider'    {children}
```

## Initialize the session​

The process of initializing a new session can be visualized using a simple flow diagram:

Here’s a step-by-step explanation of the session initialization process:

- Start the app: The app is initiated by connecting with MetaMask, a popular Ethereum wallet provider.

- Check user existence (within the dapp): It is determined whether the user's Ethereum wallet is already associated with an FT account in the decentralized app (dapp).

- Create a session (user exists): If the user's Ethereum wallet links to an account in the dapp, a new session is created. This session allows seamless interaction with the FT protocol.

- Create an account (user doesn't exist): If the user's Ethereum wallet isn't connected to an account in the dapp, a new account is created. This involves a detailed process explained in [Module 1](/courses/my-news-feed/module-one/register-evm-accounts/register-evm-accounts).

- Create a session: After successfully creating a new account, a session is established that enables interaction with the blockchain using the FT4-wrapper.

Now, the initialization flow can be implemented:

src/components/ContextProvider.tsx
```typescript
// 2.declare global {  interface Window {    ethereum: any;  }}export function ContextProvider({ children }: { children: ReactNode }) {  const [session, setSession] = useState(undefined);  useEffect(() => {    const initSession = async () => {      console.log("Initializing Session");      // 1. Initialize Client      const client = await createClient({        nodeUrlPool: "http://localhost:7740",         blockchainRid: "26A69CAACE069D03404D58E17CF9E38B4417274D5E4BEB663E6F329FD56F6D90", // Add your blockchainRid      });      // 2. Connect with MetaMask      const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);      // 3. Get all accounts associated with evm address      const evmKeyStoreInteractor = createKeyStoreInteractor(client, evmKeyStore);      const accounts = await evmKeyStoreInteractor.getAccounts();      if (accounts.length > 0) {        // 4. Start a new session        const { session } = await evmKeyStoreInteractor.login({          accountId: accounts[0].id,          config: {            rules: ttlLoginRule(hours(2)),            flags: ["MySession"],          },        });        setSession(session);      } else {        // 5. Create a new account by signing a message using metamask        const authDescriptor = createSingleSigAuthDescriptorRegistration(["A", "T"], evmKeyStore.id);        const { session } = await registerAccount(          client,          evmKeyStore,          registrationStrategy.open(authDescriptor, {            config: {              rules: ttlLoginRule(hours(2)),              flags: ["MySession"],            },          }),          {            name: "register_user",            args: [getRandomUserName()],          }        );        setSession(session);      }      console.log("Session initialized");    };    initSession().catch(console.error);  }, []);  return {children};}
```

Let's break this down:

- 
Initialize the client: We initialize the client and connect to the local blockchain node using blockchainIid 0. When you start a test node with chr node start, you’ll see logs that indicate the port for the REST API connection and both the internal (chain id) and external (blockchain Rid) identifiers for the blockchain.

During testing, using the internal Id (blockchainIid) is more convenient because it accurately reflects the state of the chain. However, when connecting the client to a blockchain deployed on a network, it’s crucial to use the blockchain's referential Id (blockchainRid) to ensure you identify and interact with the specific blockchain on the network correctly.

If you want to change from blockchainRid to blockchainIid for local development, you can do it like the below code:

```typescript
const client = await createClient({  nodeUrlPool: "http://localhost:7740",  // blockchainRid: "..."  // Replace this  blockchainIid: 0          // With this for local dev});
```

- 
Connect with MetaMask: We connect with MetaMask, ensuring that the ethereum object is declared globally on the Window interface to eliminate any compilation warnings.

- 
Retrieve accounts: We retrieve all accounts associated with the Ethereum wallet address from our dapp. We anticipate having either 0 or 1 account per wallet.

- 
Start a new session: If an account exists, we start a new session with the "MySession" flag defined in our dapp's authentication handlers. We set the expiration time for this session to two hours, after which the user will need to sign in again.

- 
Create a new account: If no accounts exist, we create a new account by signing a message in MetaMask and sending a transaction to our dapp. This process is described in detail on the [Conceptual design](/courses/my-news-feed/module-one/register-evm-accounts/register-evm-accounts#conceptual-design) page. For the EVM key, we add the flags "A" and "T" to allow signing administrative and transfer operations for this key, along with the "MySession" flag for the session key utilized by the Session. We mark register_user as the registration operation, enabling us to pass the user name as an argument.

We also use the getRandomUserName function defined in src/app/user.tsx to generate a random user name for account creation.

src/app/user.tsx
```typescript
const funnyAnimalNames = [  "SneakyLlama",  "CheekyMonkey",  "LaughingPenguin",  "CrazyKangaroo",  "GigglingHedgehog",  "WackyWalrus",  "DancingDolphin",  "BumblingBee",  "HoppingHare",  "SingingSeagull",];export function getRandomUserName() {  const randomIndex = Math.floor(Math.random() * funnyAnimalNames.length);  return funnyAnimalNames[randomIndex];}
```

## Access context with custom hooks​

We streamline the interaction with our backend by adding custom hooks that allow us to call queries and operations easily from our frontend:

src/components/ContextProvider.tsx
```typescript
// Define hooks for accessing contextexport function useSessionContext() {  return useContext(ChromiaContext);}
```

Next, we create a new file called src/app/hooks.tsx, where we introduce a new hook named useQuery:

src/app/hooks.tsx
```typescript
// Create a custom hook for queriesimport { useSessionContext } from "@/components/ContextProvider";import { useEffect, useState, useCallback } from "react";import { RawGtv, DictPair } from "postchain-client";// Custom hook for queries and operationsexport function useQuery
(  name: string,  args?: TArgs) {  const session = useSessionContext();  const [serializedArgs, setSerializedArgs] = useState(JSON.stringify(args));  const [data, setData] = useState
();  // Function to send the query  const sendQuery = useCallback(async () => {    if (!session || !args) return;    const data = await session.query
({ name: name, args: args });    setSerializedArgs(JSON.stringify(args));    setData(data!!);  }, [session, name, args]);  // Trigger the query when session, query name, or arguments change  useEffect(() => {    sendQuery().catch(console.error);  }, [session, name, serializedArgs]);  // Return query result and reload function  return {    result: data,    reload: sendQuery,  };}
```

This custom hook retrieves the necessary context and initiates a query whenever the session, query name, or arguments change. We serialize the arguments to ensure stability and avoid unintended queries. Additionally, we graciously handle cases where the arguments may still need initialization, postponing the query until they are defined.

## Implement the news feed​

We will implement the news feed in the NewsFeed.tsx component. First, we define the Data Transfer Object (DTO) structures that correspond to the return type of the get_posts query. These structures help us organize the data effectively:

src/components/NewsFeed.tsx
```typescript
import { useEffect } from "react";import { useSessionContext } from "@/components/ContextProvider";import { useQuery } from "@/app/hooks";export type User = {  name: string;  id: number;  account: number;};export type PostDto = {  timestamp: number;  user: User;  content: string;};export type GetPostsReturnType = {  pointer: number;  posts: PostDto[];};
```

In the NewsFeed component, we utilize the custom hook useQuery to fetch data. We retrieve information about the user, followers, those they follow, and the actual news feed posts:

src/components/NewsFeed.tsx
```typescript
export default function NewsFeed() {    const session = useSessionContext()    const accountId = session?.account.id;    const { result: userName } = useQuery("get_user_name", accountId ? { user_id: accountId } : undefined);    const { result: followersCount } = useQuery("get_followers_count", accountId ? { user_id: accountId } : undefined);    const { result: followingCount } = useQuery("get_following_count", accountId ? { user_id: accountId } : undefined);    const { result: newsFeed, reload: reloadPosts } = useQuery("get_posts", accountId ? { user_id: accountId, n_posts: 10, pointer: 0 } : undefined);    // Refresh posts every 10 seconds    useEffect(() => {        const refreshPosts = setInterval(() => {            reloadPosts();        }, 10000);        return () => {            clearInterval(refreshPosts);        }    });
```

The queries are directed to fetch specific data: get_user_name, get_followers_count, get_following_count, and
get_posts. We refresh the posts every 10 seconds. To ensure smooth loading, we pass undefined if evmContext is not
initialized. Here's how we incorporate these values into our style:

src/components/NewsFeed.tsx
```typescript
return (            
# {userName}
              {/* Followers Box */}                  
### Followers
          {followersCount}

                {/* Following Box */}                  
### Following
          {followingCount}

                      {/* News Feed */}                  {newsFeed ? (          newsFeed.posts.map((post, index) => (            
-                               {post.user.name}                {new Date(post.timestamp).toLocaleString()}                            {post.content}              {/* Add a horizontal line between posts */}                                    ))        ) : (          Loading...

        )}            );
```

### The UserList component​

In the UserList component, we implement the following code to fetch 100 users for the app:

src/components/UserList.tsx
```typescript
import { useQuery } from "@/app/hooks";export default function UsersList() {    const { result: users } = useQuery("get_users", { n_users: 100, pointer: 0 });
```

This code enables the app to retrieve the first 100 users. While we could enhance the get_users and get_posts queries to fetch users dynamically while scrolling, we'll keep it simple for this course.

### The UserItem component​

To make the follow/unfollow button in the UserItem component functional, we need to check if we are currently following the user and trigger the appropriate action: either follow_user or unfollow_user based on the follow state. We can build upon the code structure we prepared in [Lesson 2](/courses/my-news-feed/module-two/scaffold#user-list-page) by adding a hook to the session and calling the necessary operation.

src/components/UserItem.tsx
```typescript
export default function UserItem({ user }: { user: UsersDto }) {    // Step 1: Initialize state variables    const session = useSessionContext();    const accountId = session?.account.id;    const { result: isFollowing, reload: updateIsFollowing } = useQuery("is_following", accountId ? { my_id: accountId, your_id: user.id } : undefined);  ...    // Step 2: Handle follow/unfollow click  const handleFollowClick = async (userId: Buffer, follow: boolean) => {    if (!session) return    try {      setIsLoading(true);      // Step 3: Handle follow/unfollow logic      await session.call({        name: follow ? "follow_user" : "unfollow_user",        args: [userId]      });      updateIsFollowing();
```

First, we replace the isFollowing state with an actual query that retrieves the current follow status. In the handleFollowClick function, we then call either the follow_user or unfollow_user operation to change the follow status. Finally, we update the isFollowing state by reloading the query, ensuring that our component reflects the latest following state.

### The NewPost component​

To enable users to create new posts, let's implement a pattern similar to the UserItem. Building on the scaffold from [Lesson 2](/courses/my-news-feed/module-two/scaffold#new-post-page), we will add the following logic:

src/components/NewPost.tsx
```typescript
export default function NewPost() {  // Step 1: Initialize state variables  const session = useSessionContext();  ...    // Step 3: Handle form submission  const onSubmit = async (data: string) => {    if (!session) return    try {      if (data.trim() !== '') {        setIsLoading(true);        // Step 4: Content submission        await session.call({          name: "make_post",          args: [data],        })        router.push('/');
```

With these changes, your dapp now allows users to create and send posts seamlessly.
