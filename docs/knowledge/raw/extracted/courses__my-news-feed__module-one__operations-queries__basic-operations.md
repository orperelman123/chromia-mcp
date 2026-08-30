# Basic operations

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 3 - Explore operations and queries](/courses/my-news-feed/module-one/operations-queries/)
- Basic operationsOn this page
# Basic operations

In this section, basic operations on the Chromia blockchain will be explored, with a focus on adding data through transactions. By the end of this tutorial, the creation of users, making of posts, and management of followers within the dapp will be understood.

## Add data through transactions​

In Chromia, data is added to the blockchain by sending transactions, which can contain one or more database operations known as "operations." The operations for creating users, making posts, and following users are defined as follows.

### Register a user​

src/registration/module.rell
```rell
operation register_user(name) {    val account = register_account();    val user = create user ( name, account.id, account );    create follower ( user = user, follower = user );}
```

The register_user operation creates a new user. It takes the parameter name (the user's name) and creates a user in the database. A public key, used as id, gets passed directly to the constructor, thanks to Rell’s ability to identify types.

- Use a byte_array with a length of 32 or 64 for the public key.

An alternative notation would look like this:

```rell
create user( name = name, id = pubkey );
```

### Make a post​

rell/src/news_feed/operations.rell
```rell
operation make_post(content: text) {    val account = auth.authenticate();    require(content.size() 
The make_post operation is used to create posts. It requires user_id (the ID of the posting user) and content (the text of the post). Note that the timestamp does not need to be specified, as it has a default value in the entity definition.

### Follow a user​

rell/src/news_feed/operations.rell
```rell
operation follow_user(follow_pubkey: pubkey) {    val account = auth.authenticate();    val user = user @ { account };    val follow = require_user(follow_pubkey);    create follower ( user = follow, follower = user );}
```

The follow_user operation is used to allow one user to follow another. It requires user_id (the ID of the user who wants to follow) and follow_id (the ID of the user to be followed). The operation fetches both users from the database and creates a follower entity to establish the relationship.

### Unfollow a user​

To unfollow a user, the follower entity needs to be deleted.

rell/src/news_feed/operations.rell
```rell
operation unfollow_user(unfollow_id: byte_array) {    val account = auth.authenticate();    val user = user @ { account };    val follow = require_user(unfollow_id);    delete follower @? { .user == follow, .follower == user };}
```

## Test the operations​

To ensure that the dapp code works as intended, unit tests are introduced.

### Test module​

- Navigate to the test folder in your project's directory.

- There is a file named news_feed_test.rell inside the test folder. This file contains your test cases.

src/test/news_feed_test.rell
```rell
@test module;import ^.test_operations. { create_user };import ^^.news_feed.*;import lib.ft4.test.core. { ft_auth_operation_for };
```

- The @test module declaration indicates that this module contains test code.

- The import^^.news_feed.*; line imports everything from the news_feed module, making your dapp code accessible for testing.

- To get your tests up and running with FT4 authentication, auth.ft_auth or auth.evm_auth  is called

in the same transaction directly before the operation.
Use the ft_auth_operation_for function from the ft library's test module

### Test dapp​

Let’s examine some test cases to ensure that your dapp functions correctly. We will test the creation of users, following relationships, and posts.

src/test/news_feed_test.rell
```rell
val alice = rell.test.pubkeys.alice;val bob = rell.test.pubkeys.bob;function test_create_entities() {    rell.test.tx()        .op(create_user("Alice", alice))        .op(create_user("Bob", bob))        .run();    assert_equals(user @ { } ( @sum 1 ), 2);    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(follow_user(bob))        .op(ft_auth_operation_for(alice))        .op(make_post("My post"))        .sign(alice_kp)        .run();    assert_true(is_following(alice, bob));    assert_equals(follower @ { } ( @sum 1 ), 1);    assert_equals(post @ { } ( @sum 1 ), 1);    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(unfollow_user(bob))        .sign(alice_kp)        .run();    assert_equals(follower @ { } ( @sum 1 ), 0);}
```

In this test case:

- Two public keys, alice and bob are used from the Rell test framework as user IDs.

- The function test_create_entities, prefixed with test_ to indicate that it is a test case, runs a series of transactions to create users and manage their interactions.

- After each transaction, we use the assert_equals function to check if the expected number of entities is present in the corresponding tables. We employ the @sum function to aggregate values from the table, setting it to 1 because we are only interested in counting the number of entities.

Updating test_input_verification showcases the power of this approach, making it difficult to impersonate others. In the case where Charlie tries to make a post, he cannot proceed because he cannot create the auth operation. The modified test now looks like this:

src/test/news_feed_test.rell
```rell
function test_input_verification() {    rell.test.tx()        .op(create_user("Alice", alice))        .op(create_user("Bob", bob)).run();    // Bob cannot impersonate alice    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(follow_user(bob))        .sign(bob_kp)        .run_must_fail();    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(make_post("My malicous post"))        .sign(bob_kp)        .run_must_fail();    // Alice cannot follow non-existing charlie    val f1 = rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(follow_user(charlie))        .sign(alice_kp)        .run_must_fail();    assert_true(f1.message.contains("does not exist"));    // Charlie cannot create post since he does not exist    val f2 = rell.test.tx()        .op(make_post("My secret post"))        .sign(charlie_kp)        .run_must_fail();    assert_true(f2.message.contains("Expected at least two operations"));}
```

### Run the tests​

Now the tests can be run to verify your dapp's functionality.

- 
Open a terminal and navigate to your project's root directory.

- 
Execute the following command to run the tests:

```shell
chr test
```

This command runs the tests located in the test: modules defined in chromia.yml. If all tests pass, you will see a confirmation that your dapp's functionality works as expected.

Congratulations! You have successfully added unit tests to your dapp, ensuring its reliability.
