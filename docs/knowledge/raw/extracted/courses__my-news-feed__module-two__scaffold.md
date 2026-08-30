# Project scaffold

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - React project](/courses/my-news-feed/module-two/)
- Project scaffoldOn this page
# Project scaffold

In this section, we will explore the project scaffold. We'll check how to interact with the Rell dapp while focusing on the layout and design of the pages.

## Overview​

Our dapp will comprise three main pages:

- Home page - This page will display the news feed.

- New post page - Users can create a new post for their feed here.

- Users page - This page will list all users in the system and enable follow/unfollow capabilities.

## Basic layout​

Let's establish a foundational layout structure. In src/app/layout.tsx, the {children} element will sit within a main div, serving as the base for all our pages.

src/app/layout.tsx
```typescript
export default function RootLayout({  children,}: {  children: React.ReactNode;}) {  return (                            {children}                    );}
```

This layout ensures a clean and responsive design while maintaining consistency across all pages.

## Navigation​

A navigation component enables easy movement between our pages.

src/components/NavBar.tsx
```typescript
import Link from "next/link";export default function NavBar() {  return (                  
-           
-             News feed dapp                                    
-             
-               New Post                                
-             
-               Users                                
-             
-               Feed                                          );}
```

You can verify its functionality by running the app; clicking these links will update the URL accordingly.

```shell
$ npm run dev
```

## News feed (home) page​

Let’s explore the component in src/components/NewsFeed.tsx:

src/components/NewsFeed.tsx
```typescript
export default function NewsFeed() {  return (                  
# User name
                  {/* Followers Box */}                      
### Followers
            0

                    {/* Following Box */}                      
### Following
            0

                              {/* News Feed */}                        
-                           User1                              {new Date().toLocaleString()}                                      Some content            {/* Add a horizontal line between posts */}            {}                              );}
```

## New post page​

The new post page allows users to create a post for the news feed. It includes a free text field and a button to handle requests.

src/components/NewPost.tsx
```typescript
import { useRouter } from "next/navigation";import { useState } from "react";export default function NewPost() {  // Step 1: Initialize state variables  const router = useRouter();  const [isLoading, setIsLoading] = useState(false);  const [content, setContent] = useState("");  // Step 2: Handle text area content change  const handleContentChange = (e: React.ChangeEvent) => {    setContent(e.target.value);  };  // Step 3: Handle form submission  const onSubmit = async (data: string) => {    try {      if (data.trim() !== "") {        setIsLoading(true);        // Step 4: Content submission (will be replaced later)        router.push("/");      }    } catch (error) {      console.error(error);    } finally {      // Step 5: Reset state and loading indicator      setContent("");      setIsLoading(false);    }  };  // Render the component  return (                 onSubmit(content)}        disabled={isLoading}      >        {isLoading ? "Posting..." : "Post"}            );}
```

Here’s a breakdown of each step:

Step 1: Initialize state variables

- Start by initializing essential state variables:

- router: Utilize Next.js's router to manage in-app navigation.

- isLoading: Track whether the component is currently loading to ensure a smooth user experience.

- content: Hold the user's input for the text content of the post.

Step 2: Handle text area content changes

- Implement the handleContentChange function to respond to user input in the text area. This function updates the content state with the text entered by the user.

Step 3: Handle form submission

- Create an asynchronous onSubmit function to manage content submission. While the current example simulates content submission using router.push('/'), you should replace this placeholder with your content submission mechanism, such as API requests.

Step 4: Submit content

- In this step, we temporarily use router.push('/') to simulate content submission. Once your app connects to a backend server, replace this with your actual content submission logic.

Step 5: Reset state and loading indicator

- After submitting content—whether successful or not—the onSubmit function resets the component’s state, clearing the content field and setting isLoading back to false.

### Rendering the NewPost component​

- The NewPost component is well-organized, featuring:

- A text area for users to input their post content.

- A button for submitting the post.

- The button's appearance and behavior change dynamically based on the isLoading state, providing visual feedback to users.

To use the component, include it in your page as follows:

src/app/new-post/page.tsx
```typescript
import NewPost from "@/components/NewPost";export default function NewPostPage() {  return ;}
```

## User list page​

### The UserItem component​

To effectively represent individual users, we created the UserItem component. This component encapsulates the visual representation of each user, including their name and options to follow or unfollow them.

src/components/UserItem.tsx
```typescript
import { useState } from "react";export type UsersDto = {  name: string;  id: Buffer;};export default function UserItem({ user }: { user: UsersDto }) {  // Step 1: Initialize state variables  const [isLoading, setIsLoading] = useState(false);  const [isFollowing, setIsFollowing] = useState(false);  // Step 2: Handle follow/unfollow click  const handleFollowClick = async (userId: Buffer, follow: boolean) => {    try {      setIsLoading(true);      // Step 3: Handle follow/unfollow logic (Will be replaced later)      console.log("Following " + userId.toString("hex") + ": " + follow);      setIsFollowing(follow);    } catch (error) {      console.log(error);    } finally {      // Step 4: Reset the loading indicator      setIsLoading(false);    }  };  // Render the component  return (                  {/* User Avatar or Image */}                  {user.name[0]}                {user.name}             handleFollowClick(user.id, !isFollowing)}      >        {isLoading ? "Loading..." : isFollowing ? "Following" : "Follow"}            );}
```

Let’s examine how it works:

Step 1: Initialize state variables

- Initialize two critical state variables:

- isLoading: Track whether any user-related operation (like following or unfollowing) is in progress.

- isFollowing: Indicate whether the currently logged-in user follows the displayed user, providing immediate feedback about their following status.

Step 2: Handle follow/unfollow interaction

- The handleFollowClick function manages follow/unfollow interactions. Trigger this function when the user clicks the follow/unfollow button. Set isLoading to true to indicate that the operation is underway. Remember to replace this placeholder logic with real interactions with your backend or database.

Step 3: Update follow status

- Within the handleFollowClick function, log the follow/unfollow action temporarily. Replace this logic with actual data interactions.

#### Rendering the UserItem component​

- The UserItem component displays:

- A user avatar or image: A circular element shows the user’s initials for visual identification.

- A user name: Display the user’s name prominently next to the avatar.

- A follow/unfollow button: This button lets users follow or unfollow the displayed user. Its appearance and behavior adapt based on the isLoading and isFollowing state variables, providing immediate feedback.

### The UsersList component​

To aggregate and display multiple user items, we created the UsersList component. This component uses the UserItem component to dynamically render a list of users.

src/components/UserList.tsx
```typescript
import UserItem, { UsersDto } from "./UserItem";export type GetUsersReturnType = {  pointer: number;  users: UsersDto[];};export default function UsersList() {  // Define an example array of users  const users: GetUsersReturnType | undefined = {    pointer: 0,    users: [{ name: "User1", id: Buffer.from("AB", "hex") }],  };  return (                  {users && users.users.length > 0 ? (          users.users.map((user, index) => (            
-               {/* Render the UserItem component for each user */}                            {/* Add a horizontal line between user items */}              {index               )}                      ))        ) : (          <>        )}            );}
```

Here’s how it works:

- We define the structure of user data using the GetUsersReturnType type, which reflects the format of user data retrieved from the backend or database.

- In our example, we provide a placeholder array of users, which you should replace with the actual data obtained from your system.

- The component maps through the user array and renders the UserItem component for each user in the list. We use horizontal lines to separate each user item for better clarity.

Finally, we include the component in the users page:

src/app/users/page.tsx
```typescript
import UsersList from "@/components/UserList";export default function UsersPage() {  return ;}
```

With these components in place, your app is set up to handle user interactions and foster engagement effectively.
