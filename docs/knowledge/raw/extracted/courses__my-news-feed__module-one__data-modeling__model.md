# Implement the model in Rell

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 1 - Database schema](/courses/my-news-feed/module-one/data-modeling/)
- Implement the model in RellOn this page
# Implement the model in Rell

In Chromia, the blockchain model is defined using a language called Rell.
The Rell code that represents the model is broken down below:

src/news_feed/model.rell
```rell
entity user {  mutable name;  key id: byte_array;}entity follower {  index user;  index follower: user;  key user, follower;}entity post {  timestamp = op_context.last_block_time;  index user;  content: text;}
```

### User entity​

The user entity acts as a database table that contains two fields: name and id.
The name is made mutable, allowing it to be changed,
and id is marked as a key to signify that it serves as the primary key,
which must be unique. Here are some additional details:

- name corresponds to the text datatype.

### Follower entity​

The follower entity links one user to another as a follower. Let's break it down:

- index user;: This field links to the user entity and creates a one-to-many relationship since a user can have multiple followers. By indexing this field, you enhance the speed of SQL queries.

- index follower: user;: The second field, named "follower," represents the user who follows the specified user in the first field. You also index this field for efficient querying.

- key user, follower;: This combined key ensures that each user can follow another user only once, maintaining the uniqueness of the follower relationship.

### Post entity​

The post entity represents an actual post on your social media platform. It includes:

- timestamp = op_context.last_block_time;: This field captures the timestamp with an implicit integer data type, defaulting to the time of the last known block's creation. This setup removes the need for the timestamp to be set explicitly in code.

- index user;: This field references the user who creates the post and is indexed for efficient retrieval.

- content: text;: The content field stores the text content of the post.

Great! With Rell, a basic database model for a social media platform on the Chromia blockchain has been established. This model effectively stores user information, posts, and follower relationships. Building the decentralized app on Chromia can now proceed.
