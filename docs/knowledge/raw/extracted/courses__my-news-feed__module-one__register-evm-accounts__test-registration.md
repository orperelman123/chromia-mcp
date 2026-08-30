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
