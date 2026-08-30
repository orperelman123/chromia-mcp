# my-news-feed

===== FILE: courses__my-news-feed__introduction.md =====


# A simple app on Chromia is created using Rell, React, and FT4

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# A simple app on Chromia is created using Rell, React, and FT4

This comprehensive course is designed to guide you through the development of a decentralized news feed app similar to Twitter using Chromia.

By the end of this course, Chromia's features will be understood, and the skills needed to build decentralized applications will be gained.

## What will I learn?​

Various fundamental topics are covered in this course, providing a solid foundation for creating decentralized applications. The key areas to be explored are:

- Database model and simple queries: A database model for your news feed app was designed, and basic queries were performed to retrieve and display data.

- Input verification: The validation and securing of user inputs were demonstrated to prevent potential vulnerabilities.

- Accounts: User accounts within your decentralized app were managed, including user registration and authentication.

- Project structure: Insights were provided on organizing your project effectively, structuring your codebase, and managing dependencies.

- Frontend integration (React app): The backend was connected with a React frontend to create a complete user interface for your news feed app.

## What will be built?​

Throughout this course, work will be carried out on both the frontend and backend components of a real-time decentralized news feed app. Through this practical experience, the skills needed to create decentralized applications and explore the world of blockchain technology will be provided.

The decentralized news feed app was built using Chromia.

## Repository link​

The complete code repository for this course can be accessed here:
[News feed course repository](https://bitbucket.org/chromawallet/news-course).

### Related materials​

The following documentation is relied upon in this course to support understanding of the underlying concepts and approaches:

| 
| Section| Type| Documentation
| Rell| Modules| [Rell](https://docs.chromia.com/rell/language-features/)
| FT4| Introduction| [FT4](https://docs.chromia.com/ft4/intro)
| Overview| Dapps| [Building your dapps on Chromia](https://docs.chromia.com/intro/getting-started/)


===== FILE: courses__my-news-feed__module-one.md =====


# Module 1 - Create a Rell backend app with FT accounts

URL: https://learn.chromia.com

- [Home](/)
- Module 1 - Create a Rell backend app with FT accounts
# Module 1 - Create a Rell backend app with FT accounts

In this first module, a decentralized application (dapp) based on Rell will be explored.
A data model, operations and queries to manipulate and retrieve data will be examined,
and authentication using Chromia's accounts protocol will be explored.

By the end of this module, a production-ready dapp backend will be ready for deployment to the Chromia network.

## Lessons
[Lesson 1 - Database schema](/courses/my-news-feed/module-one/data-modeling/tables)[Lesson 2 - Create accounts](/courses/my-news-feed/module-one/create-accounts/install-configure-ft4)[Lesson 3 - Explore operations and queries](/courses/my-news-feed/module-one/operations-queries/basic-operations)[Lesson 4 - Input verification and validation](/courses/my-news-feed/module-one/input-verification/input-verification)[Lesson 5 - Project structure of the dapp](/courses/my-news-feed/module-one/project-structure/modules)[Lesson 6 - Register users using EVM wallet](/courses/my-news-feed/module-one/register-evm-accounts/register-evm-accounts)[Start module »](/courses/my-news-feed/module-one/data-modeling/)


===== FILE: courses__my-news-feed__module-one__create-accounts.md =====


# Lesson 2 - Create accounts

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- Lesson 2 - Create accounts
# Lesson 2 - Create accounts

In this lesson, you will actively learn about authentication and security by setting up FT4 accounts.
You will install the FT4 library and create FT accounts for users.

Next, you will check authentication using the auth module. A global authentication handler will centralize the process of verifying user identities.

## Sections
[FT4 accounts configuration](/courses/my-news-feed/module-one/create-accounts/install-configure-ft4)[Authentication with FT4 accounts](/courses/my-news-feed/module-one/create-accounts/authentication)[Start lesson »](/courses/my-news-feed/module-one/create-accounts/install-configure-ft4)


===== FILE: courses__my-news-feed__module-one__create-accounts__authentication.md =====


# Authentication with FT4 accounts

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 2 - Create accounts](/courses/my-news-feed/module-one/create-accounts/)
- Authentication with FT4 accounts
# Authentication with FT4 accounts

In this section, authentication with FT4 accounts will be explored.

Authentication with FT4 accounts is simple and efficient. First, import the auth module:

src/news_feed/module.rell
```rell
import lib.ft4.auth;
```

Next, a call to auth.authenticate is added in the operations:

src/news_feed/operations.rell
```rell
operation make_post(content: text) {  val account = auth.authenticate();  require(content.size() 
The auth.authenticate function handles authentication, streamlining the process and enhancing security. The user’s public key no longer needs to be passed to the operations.

To activate authentication, register an [auth handler](https://docs.chromia.com/ft4/backend/authentication/auth#the-authenticate-function). Add the following code to your Rell module:

src/news_feed/auth.rell
```rell
@extend(auth.auth_handler)function () = auth.add_auth_handler(  flags = ["S"]);
```

This extension function registers a new handler with the "S" flag,
matching the flag used during account creation.
It registers globally, enabling it to work with all operations that utilize the auth.authenticate function
in your app. If you wish to assign a specific handler for a particular operation or module,
you can include a scope argument in the auth.add_auth_handler.


===== FILE: courses__my-news-feed__module-one__create-accounts__install-configure-ft4.md =====


# FT4 accounts configuration

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 2 - Create accounts](/courses/my-news-feed/module-one/create-accounts/)
- FT4 accounts configurationOn this page
# FT4 accounts configuration

This section provides guidance on installing the FT4 library and configuring accounts.

## Install the FT4 library​

Before diving into account configuration, the FT4 library needs to be installed to manage accounts effectively. This is done through the following configuration in the chromia.yml file:

chromia.yml
```yaml
libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

## Configure FT4 accounts​

In this app, FT4 accounts will be added to the model, enhancing the authentication mechanism.

Start by importing the necessary modules:

src/registration/module.rell
```rell
import lib.ft4.core.accounts.strategies.open. { ras_open };import lib.ft4.accounts.strategies. { register_account };
```

Next, define an account within the user entity:

src/news_feed/model.rell
```rell
entity user {  mutable name;  key id: byte_array;  key account;}
```

This setup enforces a one-to-one mapping between an FT4 account and a user. Now, modify the create_user operation to also create an FT4 account:

src/registration/module.rell
```rell
operation register_user(name) {    val account = register_account();    val user = create user ( name, account.id, account );    create follower ( user = user, follower = user );}
```

For detailed information on account permissions and the FT4 framework, refer to the [FT4 Accounts and Tokens documentation](https://docs.chromia.com/ft4/intro).

Next, the test section is moved under the blockchain tag in chromia.yml and the admin public key is added:

chromia.yml
```yaml
blockchains:  newschain:    module: main    moduleArgs:      lib.ft4.core.auth:        evm_signatures_authorized_operations:          - register_user      lib.ft4.core.admin:          admin_pubkey: "0359A8F2CE1BEF95F583169B7DF053AA227A93B2652B0A9C22975FEED638032610"    test:      modules:        - testcompile:  rellVersion: 0.14.9database:  schema: schema_newschain  libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

For further details on configuring your chromia.yml file, refer to the [Chromia Project Configuration documentation](https://docs.chromia.com/intro/configuration/project-structure).


===== FILE: courses__my-news-feed__module-one__data-modeling.md =====


# Lesson 1 - Database schema

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- Lesson 1 - Database schema
# Lesson 1 - Database schema

In this lesson, you will explore a data model for our decentralized app (dapp) that focuses on users, posts, and followers.

You will discover how to implement this model in Chromia's programming language, Rell, by using entity attributes, mutability, efficient indexing, and unique constraints.

By the end of this section, you will have a solid data model for your dapp.

## Sections
[The data model](/courses/my-news-feed/module-one/data-modeling/tables)[Implement the model in Rell](/courses/my-news-feed/module-one/data-modeling/model)[Start lesson »](/courses/my-news-feed/module-one/data-modeling/tables)


===== FILE: courses__my-news-feed__module-one__data-modeling__model.md =====


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


===== FILE: courses__my-news-feed__module-one__data-modeling__tables.md =====


# The data model

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 1 - Database schema](/courses/my-news-feed/module-one/data-modeling/)
- The data model
# The data model

In this section, we will guide you through a straightforward database model for your dapp.

Our dapp's database model features three main tables:

- Users: This table stores user identities.

- Posts: This table manages posts, including their content, the user who created each post, and the timestamps of creation.

- Followers: This table represents the connections between users, specifically their followers.

This model can be visualized as follows:


===== FILE: courses__my-news-feed__module-one__input-verification.md =====


# Lesson 4 - Input verification and validation

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- Lesson 4 - Input verification and validation
# Lesson 4 - Input verification and validation

In this lesson, security will be explored based on input verification in Chromia using the "require" function.
The focus will be on ensuring the user's validity when signing operations, verifying content length,
and confirming the existence of users.

By the end, tests will be added to prevent impersonation and to ensure that input validation functions correctly.

## Sections
[Verify inputs](/courses/my-news-feed/module-one/input-verification/input-verification)[Run unit tests](/courses/my-news-feed/module-one/input-verification/tests)[Start lesson »](/courses/my-news-feed/module-one/input-verification/input-verification)


===== FILE: courses__my-news-feed__module-one__input-verification__input-verification.md =====


# Verify inputs

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 4 - Input verification and validation](/courses/my-news-feed/module-one/input-verification/)
- Verify inputsOn this page
# Verify inputs

In this section, input verification will be explored and operations will be secured by preventing impersonation. The [require function](https://docs.chromia.com/rell/language-features/systemlib/require-function) will be used as part of this process.

## Input validation​

To prevent impersonation, input verification will be added to the make_post and follow_user operations

A length requirement will be enforced on the content provided to make_post:

src/news_feed/operations.rell
```rell
require(content.size() 
Next, it needs to be verified that the user exists in the database. The database query will be made nullable and the result required to exist:

```rell
val user = require(user @? { user_id }, "User with id %s does not exist".format(user_id));
```

To further reduce duplication, a function will be created for this as well:

src/news_feed/module.rell
```rell
function require_user(id: byte_array) = require(user @? { id }, "User with id %b does not exist".format(id));
```

The modified operations are shown below:

src/news_feed/operations.rell
```rell
operation make_post(user_id: byte_array, content: text) {    val account = auth.authenticate();    require(content.size() 
The require_user function will also be added to follower queries:

src/news_feed/queries.rell
```rell
query get_followers_count(user_id: pubkey): integer {  return follower @ { .user == require_user(user_id) } (@sum 1);}query get_following_count(user_id: pubkey): integer {  return follower @ { .follower == require_user(user_id) } (@sum 1);}
```

To use the require_user function in get_user_name and get_users, the following function is defined:

src/news_feed/module.rell
```rell
function format_user(user) = "%s#%s".format(user.name, user.id.to_hex().sub(0, 5));
```

Then, the queries will be adjusted accordingly:

src/news_feed/queries.rell
```rell
query get_user_name(user_id: byte_array): text {  return format_user(require_user(user_id));}query get_users(pointer: integer, n_users: integer) {  val users = user @* {} (name = format_user($), id = .id) offset pointer limit n_users;  return (    pointer = pointer + users.size(),    users = users  );}
```

The final query, get_posts, does not require additional handling because the join statement returns zero values if a user is not found.


===== FILE: courses__my-news-feed__module-one__input-verification__tests.md =====


# Run unit tests

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 4 - Input verification and validation](/courses/my-news-feed/module-one/input-verification/)
- Run unit testsOn this page
# Run unit tests

After making input verification changes to the queries, running the tests will result in failures with the message, "User must sign this operation." This indicates success, as the original test implementation did not account for signing.

To fix this, the test transactions need to be signed using .sign(keypair). For example, in the test_create_entities function, the transaction is signed with Alice's keypair:

src/test/news_feed_test.rell
```rell
val alice_kp = rell.test.keypairs.alice; // 
In the second test case, test_follower_calculation, the transaction also needs to be signed:

src/test/news_feed_test.rell
```rell
function test_follower_calculation() {    ...    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(follow_user(bob))        .op(ft_auth_operation_for(alice))        .op(follow_user(charlie))        .sign(alice_kp)                 // 
For the last test case, the follow_user transaction should be signed by Alice, while the make_post transactions should be signed by Bob. A new constant for Bob's keypair is added and the following adjustments are made:

src/test/news_feed_test.rell
```rell
val bob_kp = rell.test.keypairs.bob;function test_pagination_of_posts() {    rell.test.tx()        .op(create_user("Alice", alice))        .op(create_user("Bob", bob)).run();    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(follow_user(bob))        .sign(alice_kp)                // 
All tests now work as expected.

## Additional testing​

A new test case should be added to confirm that impersonation is not possible and that input validation works correctly:

src/test/news_feed_test.rell
```rell
val charlie_kp = rell.test.keypairs.charlie;function test_input_verification() {    rell.test.tx()        .op(create_user("Alice", alice))        .op(create_user("Bob", bob)).run();    // Bob cannot impersonate alice    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(follow_user(bob))        .sign(bob_kp)        .run_must_fail();    rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(make_post("My malicous post"))        .sign(bob_kp)        .run_must_fail();    // Alice cannot follow non-existing charlie    val f1 = rell.test.tx()        .op(ft_auth_operation_for(alice))        .op(follow_user(charlie))        .sign(alice_kp)        .run_must_fail();    assert_true(f1.message.contains("does not exist"));    // Charlie cannot create post since he does not exist    val f2 = rell.test.tx()        .op(make_post("My secret post"))        .sign(charlie_kp)        .run_must_fail();    assert_true(f2.message.contains("Expected at least two operations"));}
```

Congratulations! Basic input validation on queries and operations has been learned. In the next module, more secure and structured account management using the [ft-library](https://docs.chromia.com/ft4/intro) will be explored.


===== FILE: courses__my-news-feed__module-one__operations-queries.md =====


# Lesson 3 - Explore operations and queries

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- Lesson 3 - Explore operations and queries
# Lesson 3 - Explore operations and queries

In this lesson, data on the Chromia blockchain platform will be manipulated. Writing operations for creating users, posting content, and managing follower relationships will be demonstrated with clear Rell code examples. Testing methodologies to ensure your dapp's integrity will also be explored.

Additionally, the focus will be on retrieving data through queries. The lesson will start with simple queries to gather user information and follower statistics, gradually progressing to more complex queries that involve pagination and sorting. Finally, manual testing of queries using the Chromia CLI will be covered.

## Sections
[Basic operations](/courses/my-news-feed/module-one/operations-queries/basic-operations)[Basic queries](/courses/my-news-feed/module-one/operations-queries/write-queries)[Start lesson »](/courses/my-news-feed/module-one/operations-queries/basic-operations)


===== FILE: courses__my-news-feed__module-one__operations-queries__basic-operations.md =====


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


===== FILE: courses__my-news-feed__module-one__operations-queries__write-queries.md =====


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


===== FILE: courses__my-news-feed__module-one__project-structure.md =====


# Lesson 5 - Project structure of the dapp

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- Lesson 5 - Project structure of the dapp
# Lesson 5 - Project structure of the dapp

In this lesson, you will focus on structuring your dapp project in Chromia. This is a vital step to ensure your code is organized and maintainable.

You will learn how to use Rell modules to effectively manage your code structure. By the end of this lesson, you will have the skills to break your code into smaller, more manageable modules and easily import them into your project. This modular approach will help you create a cleaner and more efficient dapp.

Let's begin the journey to an organized project!

## Sections
[Work with Rell modules](/courses/my-news-feed/module-one/project-structure/modules)[Incorporate modules in the dapp](/courses/my-news-feed/module-one/project-structure/incorporate-modules)[Start lesson »](/courses/my-news-feed/module-one/project-structure/modules)


===== FILE: courses__my-news-feed__module-one__project-structure__incorporate-modules.md =====


# Incorporate modules in the dapp

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 5 - Project structure of the dapp](/courses/my-news-feed/module-one/project-structure/)
- Incorporate modules in the dapp
# Incorporate modules in the dapp

Let’s put this into practice by organizing code into smaller modules. First, a folder named news_feed is created and a module.rell file is added inside it:

src/news_feed/module.rell
```rell
module;
```

Next, everything from this module is imported into main.rell by adding the following line:

src/main.rell
```rell
import news_feed.*;
```

Then, a file named model.rell is created in the src/news_feed folder and the entity definitions from the main file are moved into this new file:

src/news_feed/model.rell
```rell
entity user {  mutable name;  key id: byte_array;  key account;}entity follower {  index user;  index follower: user;  key user, follower;}entity post {  timestamp = op_context.last_block_time;  index user;  content: text;}
```

Now, the import statement for the account entity from the ft-library is shifted to the module.rell file. This arrangement allows all files within the module to access it:

src/news_feed/module.rell
```rell
module;import lib.ft4.accounts.{ account };import lib.ft4.auth;
```

The auth_handler is moved to src/news_feed/auth.rell and post_dto to src/news_feed/dto.rell. If preferred, the require_user and format_user utility functions can also be added directly to module.rell. The queries are placed in src/news_feed/queries.rell, and the operations are relocated to src/news_feed/operations.rell.

After these changes, the module.rell file in the news_feed module should look like this:

src/news_feed/module.rell
```rell
module;import lib.ft4.accounts.{ account };import lib.ft4.auth;function require_user(id: byte_array) = require(user @? { id }, "User with id %s does not exist".format(id));function format_user(user) = "%s#%s".format(user.name, user.id.to_hex().sub(0, 5));
```

With the proper imports in place, the main.rell file should now only include the create_user operation, which is beneficial for testing purposes.

Next, a new module called test_operations is created in the test folder by creating a file there and declaring it as a module:

src/test/test_operations.rell
```rell
module;import lib.ft4.core.accounts. { create_account_with_auth };import lib.ft4.accounts.{ single_sig_auth_descriptor };import ^^.news_feed.{ user };operation create_user(name, pubkey) {    val account = create_account_with_auth(single_sig_auth_descriptor(pubkey, set(["A", "T", "S"])));    create user ( name, pubkey, account );}
```

In the test file, this module is explicitly imported, possibly using relative imports:

src/test/news_feed_test.rell
```rell
import ^.test_operations.{ create_user };
```

It has now been shown how to effectively structure the project using Rell modules. The file structure should resemble the following:

```text
rell/src├─ main.rell├─ news_feed│  ├─ auth.rell│  ├─ dto.rell│  ├─ model.rell│  ├─ module.rell│  ├─ operations.rell│  └─ queries.rell├─ registration│  └─ module.rell└─ test   ├─ news_feed_test.rell   ├─ registration_test.rell   └─ test_operations.rell
```


===== FILE: courses__my-news-feed__module-one__project-structure__modules.md =====


# Work with Rell modules

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 5 - Project structure of the dapp](/courses/my-news-feed/module-one/project-structure/)
- Work with Rell modules
# Work with Rell modules

In this section, project structuring and modularization of the dapp will be explored to enhance the organization and maintainability of the code.

In Chromia, code can be organized into modules to improve structure and ease of maintenance. To define a module, the module; keyword is simply added at the top of the file. By default, the module name is derived from the file name, unless the file is named module.rell. In that case, the entire folder becomes a module with the name of the folder itself. Sub-modules can be created by adding folders within modules or by declaring another module; in a file within the same folder.

For example:

src/main.rell
```rell
module;
```

This defines a module called main.

src/main/module.rell
```rell
module;
```

This also defines a module called main.

If a second file, src/main/foo.rell, is added without a module declaration, it automatically becomes part of the main module. However, if it is declared as a module:

src/main/bar.rell
```rell
module;
```

A sub-module called main.bar will be created.

By following this pattern, a modular structure for the code can be built, promoting better organization and maintainability.


===== FILE: courses__my-news-feed__module-one__register-evm-accounts.md =====


# Lesson 6 - Register users using EVM wallet

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- Lesson 6 - Register users using EVM wallet
# Lesson 6 - Register users using EVM wallet

In this lesson, a module will be configured to enable seamless account registration using EVM wallets, including popular options like MetaMask. A dedicated registration module will be created and an operation will be designed to verify EVM signatures and link them to user accounts.

Finally, tests will be written to validate the implementation.

## Sections
[Register accounts using EVM Wallets](/courses/my-news-feed/module-one/register-evm-accounts/register-evm-accounts)[Test the registration](/courses/my-news-feed/module-one/register-evm-accounts/test-registration)[Start lesson »](/courses/my-news-feed/module-one/register-evm-accounts/register-evm-accounts)


===== FILE: courses__my-news-feed__module-one__register-evm-accounts__register-evm-accounts.md =====


# Register accounts using EVM Wallets

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 6 - Register users using EVM wallet](/courses/my-news-feed/module-one/register-evm-accounts/)
- Register accounts using EVM WalletsOn this page
# Register accounts using EVM Wallets

In this section, we will create a module that enables user account registration using EVM (Ethereum Virtual Machine) wallets. This functionality allows users to sign in with EVM-compatible wallets like MetaMask and can be extended to include features such as payments.

## Conceptual design​

Users will register an account by signing a message with MetaMask. We achieve this by utilizing the [open account registration strategy](https://docs.chromia.com/ft4/backend/accounts/overview#account-registration-framework) from the ft library. The following sequence diagram illustrates the registration flow:

## Implementation in Rell​

We will implement the registration flow by creating a new module called registration. This separation allows easy inclusion or removal of account registration features and supports multiple registration methods. Follow these steps for implementation:

- 
Create a folder named registration, and within it, add a file called module.rell.

- 
Add the following definitions to registration/module.rell:

src/registration/module.rell
```rell
module;import ^.news_feed.{ user, follower };import lib.ft4.core.accounts.strategies.open. { ras_open };import lib.ft4.accounts.strategies. { register_account };operation register_user(name) {    val account = register_account();    val user = create user ( name, account.id, account );    create follower ( user = user, follower = user );}
```

- The open strategy allows users to register themselves.

- The register_user operation registers an account, creates a user, and establishes follower information.

- The register_account function ensures that the transaction is signed by a unique EVM account.

- In your chromia.yml file, add an exclusion for this operation to notify the ft library that the register_user operation is safe to use:

chromia.yml
```yaml
blockchains:  newschain:    module: main    moduleArgs:      lib.ft4.core.auth:        evm_signatures_authorized_operations:          - register_user      lib.ft4.core.admin:          admin_pubkey: "0359A8F2CE1BEF95F583169B7DF053AA227A93B2652B0A9C22975FEED638032610"
```

For more details on configuring your chromia.yml file, refer to the [Chromia Project Configuration documentation](https://docs.chromia.com/intro/configuration/project-structure).

- In your main.rell file, include an import statement for the new registration module:

src/main.rell
```rell
module;import news_feed.*;import registration.*;
```

cautionWhile the register_user operation requires a valid EVM signature, it can expose a potential DDOS attack vector since each account corresponds to a row in the database. To mitigate this risk, consider the following strategies:

- Implement rate limiting for the operation.

- Enforce payments during registration.

- Establish an [authentication](https://docs.chromia.com/ft4/backend/authentication/) layer as an intermediary.


===== FILE: courses__my-news-feed__module-one__register-evm-accounts__test-registration.md =====


# Test the registration

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 6 - Register users using EVM wallet](/courses/my-news-feed/module-one/register-evm-accounts/)
- Test the registrationOn this page
# Test the registration

To test our new module, follow these steps:

- 
Create a test file named src/test/registration_test.rell.

- 
In your chromia.yml file, include the test module by adding it to the list of modules:

src/chromia.yml
```yaml
blockchains:  newschain:    test:      modules:        - test
```

- In registration_test.rell, import the necessary modules and define the EVM address and private key for testing:

src/test/registration_test.rell
```rell
@test module;import registration. { register_user };import news_feed. { make_post, user, post };import lib.ft4.accounts. { single_sig_auth_descriptor };import lib.ft4.auth. { evm_signatures };import lib.ft4.core.accounts.strategies.open. { ras_open };import lib.ft4.external.accounts.{ add_auth_descriptor };import lib.ft4.external.accounts.strategies. { get_register_account_message };import lib.ft4.test.core.{ ft_auth_operation_for, evm_auth_operation_for, evm_sign };val evm_address = x"1337c28e95ce85175af66353fecccd676e3d273a";val evm_privkey = x"18e2d37cd5b51555c52d454c22608dee5e7151384f2d7b7bc21616e2eadc3e6f";
```

- Create a test case in registration_test.rell that registers an account using EVM signatures:

src/test/registration_test.rell
```rell
function test_evm_registration() {    val alice_auth_desc = single_sig_auth_descriptor(evm_address, set(["A", "T"]));    val alice_session_1 = rell.test.keypairs.alice;    val session1_auth_desc = single_sig_auth_descriptor(alice_session_1.pub, set(["S"]));    val strategy_op = ras_open(alice_auth_desc, session1_auth_desc);    val register_op = register_user("Alice");    val message = get_register_account_message(        strategy_op.to_gtx_operation(),        register_op.to_gtx_operation()    );    val signature = evm_sign(message, evm_privkey);    // Register account using evm address     rell.test.tx()        .op(evm_signatures([evm_address], [signature]))        .op(strategy_op)        .op(register_op)        .sign(alice_session_1)        .run();    assert_equals(user @ { } ( @sum 1 ), 1);    // Make post using session key    rell.test.tx()        .op(ft_auth_operation_for(alice_session_1.pub))        .op(make_post("My first post"))        .sign(alice_session_1)        .run();    assert_equals(post @ { } ( @sum 1 ), 1);        // Start a new session    val alice_session_2 = rell.test.keypairs.bob;    val session_auth_desc = single_sig_auth_descriptor(alice_session_2.pub, set(["S"]));    val add_auth_descriptor_operation = add_auth_descriptor(session_auth_desc);    // Add session token    rell.test.tx()        .op(evm_auth_operation_for(evm_privkey, add_auth_descriptor_operation))        .op(add_auth_descriptor_operation)        .sign(alice_session_2)        .run();    // Make another post using session key    rell.test.tx()        .op(ft_auth_operation_for(alice_session_2.pub))        .op(make_post("My second post"))        .sign(alice_session_2)        .run();    assert_equals(post @ { } ( @sum 1 ), 2);}
```

This test case simulates a user registering an account using an EVM address and signature.

In this test case, we:

- Define an authentication descriptor for the EVM key with the "A" and "T" flags, for administrative and transfer access
within the ft framework.

- Create a session keypair and authentication descriptor for Alice (alice_session_1).

- Generate an EVM signature for registration using evm_sign including the operations we will call.

- Create a transaction that includes the register_account operations and sign it with Alice's session keypair.

- Assert that the registration was successful.

### Posting with session keypair​

As a supplementary verification, we demonstrate the usage of the session keypair for making a post:

```rell
// Make a post using the session keypair    rell.test.tx()        .op(ft_auth_operation_for(alice_session_1.pub))        .op(make_post("My first post"))        .sign(alice_session_1)        .run();    assert_equals(post @ { } ( @sum 1 ), 1);
```

In this case, we use the session keypair for signing, and we employ ft_auth_operation_for from the ft test module
to inject the authentication properties.

In this test, we:

- Create a new transaction with ft_auth_operation_for to inject authentication properties.

- Sign the transaction with Alice's session keypair.

- Assert that the post was successfully created.

### Logging in with a new session keypair​

To further validate the behavior, we simulate a user logging in for the second time. They create a new session keypair
and register it with the dapp using the ft4.add_auth_descriptor operation:

```rell
// Start a new session    val alice_session_2 = rell.test.keypairs.bob;    val session_auth_desc = single_sig_auth_descriptor(alice_session_2.pub, set(["MySession"]));    val add_auth_descriptor_operation = add_auth_descriptor(session_auth_desc);    // Add a session token    rell.test.tx()        .op(evm_auth_operation_for(evm_privkey, add_auth_descriptor_operation))        .op(add_auth_descriptor_operation)        .sign(alice_session_2)        .run();    // Make another post using the session key    rell.test.tx()        .op(ft_auth_operation_for(alice_session_2.pub))        .op(make_post("My second post"))        .sign(alice_session_2)        .run();    assert_equals(post @ { } ( @sum 1 ), 2);
```

In this test, we:

- Create a new session keypair and define a single signature auth descriptor with the "MySession" flag and make a
transaction.

- Make a transaction to add the new session key.

- Sign the transaction using the EVM private key.

- Create another post and assert that it was successfully created.

warningIt's important to note that when creating the EVM auth operation, we must use the fully qualified name with the ft4.
prefix.

For a deeper understanding of how the FT4 framework operates, including the concepts of authentication descriptors and session management, you can refer to the [FT4 Accounts and Tokens documentation](https://docs.chromia.com/ft4/intro).

That's it! You have created and tested a module for registering user accounts using the EVM wallet and verified its
functionality.


===== FILE: courses__my-news-feed__module-two.md =====


# Module 2 - React project

URL: https://learn.chromia.com

- [Home](/)
- Module 2 - React project
# Module 2 - React project

In this module, the [Next.js framework](https://nextjs.org/) will be leveraged for front-end development. A new React project will be created and a scaffold will be added. Next, the page layout will be designed and built.

Finally, the connection between the React app and your decentralized application (dapp) will be established. This integration will enable followers to be added or removed, posts to be created, and changes to be observed in real-time on the feed page.

## Lessons
[Set up the project](/courses/my-news-feed/module-two/setup)[Project scaffold](/courses/my-news-feed/module-two/scaffold)[Connect the client](/courses/my-news-feed/module-two/connecting-the-client)[Summary and manual testing](/courses/my-news-feed/module-two/summary-and-tests)[Start module »](/courses/my-news-feed/module-two/setup)


===== FILE: courses__my-news-feed__module-two__connecting-the-client.md =====


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


===== FILE: courses__my-news-feed__module-two__scaffold.md =====


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


===== FILE: courses__my-news-feed__module-two__setup.md =====


# Set up the project

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - React project](/courses/my-news-feed/module-two/)
- Set up the project
# Set up the project

In this course, we will actively leverage the [Next.js framework](https://nextjs.org) for our frontend development. Our journey starts with creating a new project using [create-next-app](https://nextjs.org/docs/getting-started/installation) while harnessing the power of Tailwind CSS.

Before we proceed, ensure you have a wallet installed, such as [MetaMask](https://metamask.io), since our dapp requires one to interact with the blockchain.

You can find the project [here](https://bitbucket.org/chromawallet/news-course/src/main/frontend/).

Navigate to the frontend folder and install the dependencies:

```shell
npm install
```

With this, we kick off our project journey with Next.js, Tailwind CSS, and FT4, all while prioritizing style and efficiency.


===== FILE: courses__my-news-feed__module-two__summary-and-tests.md =====


# Summary and manual testing

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - React project](/courses/my-news-feed/module-two/)
- Summary and manual testingOn this page
# Summary and manual testing

Let's summarize what we've accomplished so far:

- A decentralized app was created using Rell and Chromia.

- Unit tests were written and a test node was started.

- The React app was connected to the dapp using the FT client wrapper.

## Start the frontend app​

To view the changes in our frontend app, start it with this command:

```shell
npm run dev
```

The dapp can now be interacted with. Followers can be added or removed, posts can be created, and content updates can be observed on the feed page.

Congratulations! A first dapp on Chromia has been built and tested successfully.


===== FILE: courses__my-news-feed__setup.md =====


# The project is set up

URL: https://learn.chromia.com

- [Home](/)
- The project is set upOn this page
# The project is set up

Before starting, the following prerequisites should be in place:

PostgreSQL database setup
# Set up PostgreSQL database

Rell requires PostgreSQL 16.3. The IDE can work without it but can't run a node. A console or a remote postchain app can
run without a database.

The default database configuration for Rell is:

- database: postchain

- user: postchain

- password: postchain

... [standard PG+CLI setup omitted] ...

===== FILE: courses__my-news-feed__what-next.md =====


# What is next?

URL: https://learn.chromia.com

- [Home](/)
- What is next?On this page
# What is next?

Congratulations! The course has been successfully completed.

With a solid foundation in Chromia established, the following next steps can be considered:

- Advanced Chromia features and concepts can be explored.

- More complex dapps that tackle real-world use cases can be built.

- The Chromia developer community can be joined for support and collaboration.

Congratulations on the completion of this tutorial! Readiness to start building blockchain-powered decentralized applications has now been achieved. Happy coding!

## Join Discord for support​

Our Discord channel for those new to Chromia can be joined. Questions or feedback about the course can be posted—an active community is eager to help.

The [invite link](https://discord.com/invite/chromia?ref=learn.chromia.com) is provided to join our Discord.

## Spread the word​

If the course was enjoyed, the word may be spread!
