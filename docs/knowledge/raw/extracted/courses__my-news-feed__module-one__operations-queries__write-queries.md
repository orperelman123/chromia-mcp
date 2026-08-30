# Basic queries

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 3 - Explore operations and queries](/courses/my-news-feed/module-one/operations-queries/)
- Basic queriesOn this page
# Basic queries

This section introduces the fundamentals of working with queries in Chromia blockchain development. Queries are essential for retrieving data from the blockchain, and they will be created and tested step by step.

In the dapp, queries are needed to:

- Display the username of a user.

- Count how many followers a user has.

- Count how many users a user is following.

- Check if one user follows another.

- Retrieve a list of posts.

- Show all users.

The following describes how to create and use queries in Chromia:

## User and follower queries​

Queries are defined as functions using the query keyword. Let’s start by creating a query to get a uniquely identifiable username:

rell/src/news_feed/queries.rell
```rell
query get_user_name(user_id: byte_array): text {  return user @ { user_id } ("%s#%s".format(.name, .id.to_hex().sub(0, 5)));}
```

This query retrieves a user and formats a text string by concatenating the name and the first five characters of the hex representation of the ID. Next, two queries will be created to count followers and the users a given user follows.

rell/src/news_feed/queries.rell
```rell
query get_followers_count(user_id: byte_array): integer {  return follower @ { .user == user @ { user_id } } (@sum 1);}query get_following_count(user_id: byte_array): integer {  return follower @ { .follower == user @ { user_id } } (@sum 1);}
```

These queries are similar but differ in how they filter results from the follower database query. The return type integer can be omitted because Rell can deduce it from the return statement, allowing the queries to be simplified as follows:

```rell
query get_followers_count(user_id: byte_array) =  follower @ { .user == user @ { user_id } } (@sum 1);
```

A query can also be created to check if a follower entity exists as follows:

rell/src/news_feed/queries.rell
```rell
query is_following(my_id: byte_array, your_id: byte_array) =  exists(follower @? { .user.id == your_id, .follower.id == my_id });
```

## Query posts with pagination​

Next, a query will be created to retrieve posts created by users that a specific user follows. Since this may return a large number of posts, pagination is necessary to manage the data effectively. The results should be ordered from the latest to the oldest posts.

To manage post data efficiently, a [struct](https://docs.chromia.com/rell/language-features/modules/struct) called post_dto is defined:

src/news_feed/dto.rell
```rell
struct post_dto {  timestamp;  user: struct;  content: text;}
```

This structure resembles a post entity, but the user field has a slightly different format.
The type [struct](https://docs.chromia.com/rell/language-features/modules/struct#structmutable-t) is an in-memory representation of an entity,
meaning that all fields are loaded into memory for efficient use.

### Retrieve posts​

To retrieve the desired posts, tables are joined, data is filtered, and pagination is added:

rell/src/news_feed/queries.rell
```rell
query get_posts(  user_id: byte_array,  pointer: integer,  n_posts: integer): (pointer: integer, posts: list) {  val posts = (user, follower, post) @* {    user.id == user_id,    follower.follower == user,    post.user == follower.user  } (    @sort_desc @omit post.rowid,    post_dto(        post.timestamp,        post.content,        user = post.user.to_struct()    )  ) offset pointer limit n_posts;  return (    pointer = pointer + posts.size(),    posts = posts  );}
```

Here’s what happens in this query:

- The user whose followers' posts are to be retrieved is specified using user_id.

- The user, follower, and post tables are joined to obtain the necessary data.

- The posts are sorted in descending order based on their creation timestamp to retrieve the latest posts first.

- A post_dto data structure is created for each post, including the user's structured representation.

- An offset is included to skip posts and a limit to control how many posts to retrieve.

In the database query, posts are sorted in descending order to fetch the latest ones first, although this detail is omitted from the resulting data structure. While pagination could be based on timestamps, this method simplifies the process.

### Return results​

Finally, the results are returned as a [named tuple](https://docs.chromia.com/rell/language-features/types/complex-types#tuple) with two components:

- pointer: An index indicating where to start the next query.

- posts: A list of post_dto objects containing the retrieved posts.

With this query, posts created by a user's followers can be fetched efficiently with pagination, simplifying the management and display of data in the dapp.

## Query user list​

To retrieve all users in the dapp, we need a query that combines elements from the get_user_name query and pagination from get_posts. The following query accomplishes this:

rell/src/news_feed/queries.rell
```rell
query get_users(pointer: integer, n_users: integer) {  val users = user @* {} (name = "%s#%s".format(.name, .id.to_hex().sub(0, 5)), id = .id) offset pointer limit n_users;  return (    pointer = pointer + users.size(),    users = users  );}
```

## Test the queries​

We can now test these queries to ensure they function as expected. We begin with a simple test case for the get_user_name query:

src/test/news_feed_test.rell
```rell
function test_user_name() {    rell.test.tx()        .op(create_user("Alice", alice))        .run();    assert_equals(get_user_name(alice), "Alice#02466");    val users_result = get_users(0, 20);    assert_equals(users_result.pointer, 1);    assert_equals(users_result.users.size(), 1);    assert_true(users_result.users @* {} (.name).contains("Alice#02466"));}
```

In this example, the @-operator successfully operates on lists.

Next, we will assess the follower count through another test case:

src/test/news_feed_test.rell
```rell
val charlie = rell.test.pubkeys.charlie;function test_follower_calculation() {    rell.test.tx()        .op(create_user("Alice", alice))        .op(create_user("Bob", bob))        .op(create_user("Charlie", charlie))        .run();    rell.test.tx()        .op(follow_user(alice, bob))        .op(follow_user(alice, charlie))        .run();    assert_true(is_following(alice, bob));    assert_true(is_following(alice, charlie));    assert_equals(get_following_count(alice), 2);    assert_equals(get_following_count(bob), 0);    assert_equals(get_followers_count(alice), 0);    assert_equals(get_followers_count(bob), 1);}
```

In this test case:

- We create three users: Alice, Bob, and Charlie.

- Alice follows both Bob and Charlie.

- We use assert_equals to verify that the queries return the correct follower and following counts.

### Test pagination for posts​

Next, we will test the pagination feature for retrieving posts. This test case will involve creating users, having them follow each other, and then creating posts.

src/test/news_feed_test.rell
```rell
function test_pagination_of_posts() {    rell.test.tx()        .op(create_user("Alice", alice))        .op(create_user("Bob", bob)).run();    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(follow_user(bob))        .sign(alice_kp)        .run();    for (i in range(5)) {        rell.test.tx()            .op(ft_auth_operation_for(bob))            .op(make_post("Content %d".format(i)))            .sign(bob_kp)            .run();    }    val initial_posts = get_posts(alice, 0, 4);    assert_equals(initial_posts.pointer, 4);    assert_equals(initial_posts.posts.size(), 4);    val last_posts = get_posts(alice, initial_posts.pointer, 4);    assert_equals(last_posts.pointer, 5);    assert_equals(last_posts.posts.size(), 1);}
```

In this test case:

- We create users Alice and Bob.

- Alice
