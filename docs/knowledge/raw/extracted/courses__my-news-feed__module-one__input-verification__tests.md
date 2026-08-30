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
