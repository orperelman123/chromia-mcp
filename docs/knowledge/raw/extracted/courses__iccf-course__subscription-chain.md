# Subscription chain

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Subscription chainOn this page
# Subscription chain

Let's implement the Subscription Chain. For this, we will need two entities; account to store the account balance and
receipt to store digital receipts for logging purposes. This is done by configuring a new blockchain in chromia.yml.

```yaml
blockchains:  # ↓↓↓ Delete this code snippet ↓↓↓  digital_warehouse_example:    module: main  # ↑↑↑ Delete this code snippet ↑↑↑  # ↓↓↓ Add this code snippet ↓↓↓  subscription_chain:    module: subscription_chain# ↑↑↑ Add this code snippet ↑↑↑
```

Add this code to the entities.rell file in the src/subscription_chain directory:

src/subscription_chain/entities.rell
```rell
entity account {    key id: pubkey;    mutable balance: integer = 10000;}@logentity receipt {    // We don't reference the account entity here since this entity may be removed    account_id: pubkey;    amount: integer;    period;    blockchain_rid: byte_array;    index account_id, blockchain_rid;}
```

As seen here, the account is modeled by an entity that contains an ID and a mutable balance. For simplicity, we give
users 10,000 tokens upon account creation.

noteExercise caution when managing funds on the blockchain, and it is recommended that you utilize the FT token library.

We also have an entity called receipt, annotated with @log. This annotation ensures that all entries in this table
are permanently immutable, which is crucial for transparent monetary transaction monitoring. To achieve this, we include
account_id as a reference to the account. Instead of directly referencing the account entity, we use account_id to
maintain flexibility in account deletion, if necessary, for legal reasons.

In the receipt, we include the payment amount, the purchased period, and the blockchain RID (Resource IDentifier) of
the Digital Warehouse Chain where the payment was directed. This ensures that the Warehouse Chain can verify the payment
and prevent double-spending across chains. Additionally, we have added an index on the account_id and blockchain_rid
to enhance query performance.

## Account creation​

For this simple example, accounts are created dynamically when a user wishes to subscribe. Let's implement a
straightforward function to manage this process. Add the following code block to the functions.rell file stored in the
src/subscription_chain folder.

src/subscription_chain/functions.rell
```rell
function get_or_create_account(id: pubkey) {    require(op_context.is_signer(id));    return account @? { id } ?: create account(id);}
```

## Subscription​

To create a new subscription, we define a new operation called subscribe, which requires the blockchain RID of the
chain where we want to confirm this transaction. We also include the subscription metadata.

src/subscription_chain/operations.rell
```rell
operation subscribe(blockchain_rid: byte_array, subscription) {    val account = get_or_create_account(subscription.account_id);    val subscription_fee = period_price(subscription.period);    require(account.balance >= subscription_fee, "Insufficient funds");    account.balance-=subscription_fee;    create receipt (        account_id = subscription.account_id,        subscription_fee,        subscription.period,        blockchain_rid    );}
```

This operation retrieves an existing account or creates a new one, deducts the account balance, and generates a receipt.

## Queries​

The ability to query receipts for a specific user or transaction can be helpful for monitoring purposes. Let's complete
this dapp implementation by adding a set of queries to the queries.rell file stored in the src/subscription_chain
directory.

src/subscription_chain/queries.rell
```rell
query get_receipts(account_id: pubkey?, blockchain_rid: byte_array?, from: timestamp?)    = (receipt, account) @* {    if (account_id??) account.id == account_id else true,    account.id == receipt.account_id,    if (blockchain_rid??) .blockchain_rid == blockchain_rid else true,    if (from??) .transaction.block.timestamp >= from else true} (    receipt_id = .transaction.tx_rid,    account_id = account.id,    period = .period,    payment_amount = .amount,    blockchain_rid = .blockchain_rid,    timestamp = .transaction.block.timestamp);
```

This query lets us filter receipts based on account_id, blockchain_rid, and timestamp.

## Unit test​

Writing unit tests for this blockchain is straightforward. To do this, create a new file named
subscription_chain_test.rell in the src/test directory and add the following code to it:

src/test/subscription_chain_test.rell
```rell
@test module;import subscription_chain.{ subscribe, subscription, period, account, period_price, period, get_receipts };val TEST_WAREHOUSE_CHAIN = x"ABAB";function test_create_subscription() {    rell.test.tx()        .op(subscribe(TEST_WAREHOUSE_CHAIN, subscription(rell.test.pubkeys.alice, period.WEEK)))        .sign(rell.test.keypairs.alice)        .run();    val test_account = account @? { rell.test.pubkeys.alice };    assert_not_null(test_account);    assert_equals(test_account.balance, 10000 - period_price(period.WEEK));    val receipts = get_receipts(test_account.id, null, null);    assert_equals(receipts.size(), 1);    assert_equals(receipts[0].payment_amount, 30);    assert_equals(receipts[0].blockchain_rid, TEST_WAREHOUSE_CHAIN);}
```

Also, you have to add new configuration details to the chromia.yml file:

chromia.yml
```yaml
blockchains:  subscription_chain:    module: subscription_chain    # ↓↓↓ Add this code snippet ↓↓↓    test:      modules:        - test.subscription_chain_test    # ↑↑↑ Add this code snippet ↑↑↑# ↓↓↓ Delete this code snippet ↓↓↓test:  modules:    - test# ↑↑↑ Delete this code snippet ↑↑↑
```

The test creates a new subscription for the test user, Alice, and checks that a receipt is generated with the correct
amounts and blockchain target.

To execute the test, run:

```text
chr test
```

Below are the project structure and the contents of the chromia.yml configuration file after the changes have been made.
It can be handy for comparison at this stage of the course:

Project structure

```text
digital_warehouse_example/├── build/├── src/│   ├── lib/│   ├── subscription_chain/│   │   ├── entities.rell│   │   ├── functions.rell│   │   ├── module.rell│   │   ├── operations.rell│   │   └── queries.rell│   ├── test/│   │   └── subscription_chain_test.rell│   └── main.rell├── .gitignore└── chromia.yml
```

Final version of the chromia.yml file

```text
yaml title="chromia.yml"blockchains:  subscription_chain:    module: subscription_chain    test:      modules:        - test.subscription_chain_test  digital_warehouse_chain:    module: digital_warehouse_chain    config:      gtx:        modules:          - net.postchain.d1.iccf.IccfGTXModule    moduleArgs:      digital_warehouse_chain:        warehouse_id: 1    test:      modules:        - test.digital_warehouse_chain_testcompile:  rellVersion: 0.14.9database:  schema: schema_digital_warehouse_examplelibs:  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"  iccf_test:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/test/iccf    tagOrBranch: 1.87.0    rid: x"873D71557531E35E453587FA71C2D3CCB674F0EB49D18B30FA838C52C1155EB3"
```
