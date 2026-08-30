# iccf-course

===== FILE: courses__iccf-course__digital-warehouse-chain.md =====


# Digital warehouse chain

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - Digital warehouse chainOn this page
# Digital warehouse chain

The digital warehouse chain tracks inventory for a specific warehouse. Users are authenticated by presenting proof of
payment on the Subscription Chain, granting them unrestricted system access until their subscription expires.

Next, create a directory named digital_warehouse_chain. Within this directory, create a file named module.rell and
insert the following definition:

src/digital_warehouse_chain/module.rell
```rell
module;
```

Add the user account entity to the entities.rell that includes an expiration date for the subscription:

src/digital_warehouse_chain/entities.rell
```rell
entity warehouse_account {    key id: pubkey;    mutable valid_until: timestamp = op_context.last_block_time;}
```

## Inventory model​

The inventory is modeled using a single entity that monitors the stock level of a specific product ID. We also add a
logged entity, inventory_log, to track any changes in the inventory. Add the following code to the entities.rell
file:

src/digital_warehouse_chain/entities.rell
```rell
entity inventory {    key product_category: integer;    UNIT;    mutable stock: integer = 0;}enum UNIT {    LITRE, PIECE, KILOGRAM}@log entity inventory_log {    // We don't reference the inventory entity here since this entity may be removed    product_category: integer;    amount: integer;    comment: text;}struct inventory_dto {    product_category: integer;    amount: integer;    comment: text;}
```

Next, we must enable users to register a new product category and update inventory.

The update_inventory operation is responsible for updating the current inventory stock and logging the details of how
and why the change occurred. inventory_dto represents a structure used to gather information about the inventory
change.

Add the following code to the operations.rell file stored in the src/digital_warehouse_chain:

src/digital_warehouse_chain/operations.rell
```rell
operation register_product_category(category: integer, UNIT) {    require_authenticated_signer();    create inventory(product_category = category, UNIT);}operation update_inventory(inventory_dto) {    require_authenticated_signer();    update inventory @ { .product_category == inventory_dto.product_category } (stock += inventory_dto.amount);    create inventory_log(        product_category = inventory_dto.product_category,        amount = inventory_dto.amount,        comment = inventory_dto.comment        );}
```

Additionally, we need to add a function to the functions.rell file stored in the src/digital_warehouse_chain
directory.

The function require_authenticated ensures that a user exists, has signed the transaction, and possesses a valid
subscription:

src/digital_warehouse_chain/functions.rell
```rell
function require_authenticated_signer() {    require(op_context.get_signers().size() == 1, "Require exactly one signature");    val valid_until = warehouse_account @? { op_context.get_signers()[0]}.valid_until;    require(exists(valid_until), "No account found");    require(op_context.last_block_time 

## Authentication​

Authentication of a user is handled through ICCF. To enable this, we configure our Digital Warehouse Chain to utilize
ICCF by adding the following configuration to the chromia.yml file:

chromia.yml
```yaml
blockchains:  # ↓↓↓ Add this code snippet ↓↓↓  digital_warehouse_chain:    module: digital_warehouse_chain    config:      gtx:        modules:          - net.postchain.d1.iccf.IccfGTXModule# ↑↑↑ Add this code snippet ↑↑↑
```

Then you have to import the ICCF module by adding the following to the module.rell file:

src/digital_warehouse_chain/module.rell
```rell
import lib.iccf;import subscription_chain.{ subscription, period, period_to_millis };
```

Define the authorize operation that takes a gtx_transaction containing transaction information to confirm with ICCF.
It ensures uniqueness by storing a transaction hash before verifying and extracting the operation arguments. The first
argument of the transaction should include the blockchain RID where the payment was made, and the second argument should
contain the subscription metadata. We verify the correctness of the blockchain RID and decode the subscription metadata.

The operation must be defined in the operations.rell file, which is stored in the src/digital_warehouse_chain
directory:

src/digital_warehouse_chain/operations.rell
```rell
operation authorize(tx: gtx_transaction) {    iccf.make_transaction_unique(tx);    val args = iccf.extract_operation_args(tx, "subscribe", verify_signers = true);    require(byte_array.from_gtv(args[0]) == chain_context.blockchain_rid, "Wrong blockchain proof, found %s".format(chain_context.blockchain_rid));    val subscription = subscription.from_gtv(args[1]);    val warehouse_account = get_or_create_account(subscription.account_id);    val new_expiration_date = max(op_context.last_block_time, warehouse_account.valid_until) + period_to_millis(subscription.period);    warehouse_account.valid_until = new_expiration_date;}
```

Finally, we retrieve or create a new account and extend its expiration date by adding the subscription period. This
approach ensures that regardless of when a subscription is purchased, the period will be added to the existing
expiration date.

Add the following block of code to the functions.rell file stored in the src/digital_warehouse_chain directory:

src/digital_warehouse_chain/functions.rell
```rell
function get_or_create_account(id: pubkey) {    require(op_context.is_signer(id));    return warehouse_account @? { id } ?: create warehouse_account(id);}
```

## Creating a report​

Finally, we define a query to create a report and history log of all inventory updates that have occurred. Since this
smart contract represents only a single warehouse, we include a module argument containing information about the
warehouse to be included in the report.

Add the following struct to the entities.rell file stored in the src/digital_warehouse_chain directory:

src/digital_warehouse_chain/entities.rell
```rell
struct module_args {    warehouse_id: integer;}
```

Next, place the create_report query within the queries.rell file located in the src/digital_warehouse_chain
directory;

src/digital_warehouse_chain/queries.rell
```rell
query create_report(from: timestamp?, to: timestamp?) {    val current_inventory = inventory @* {} ($.to_struct());    val history = inventory_log @* {        if (from??).transaction.block.timestamp >= from else true,        if (to??).transaction.block.timestamp 
Additionally, include the group_logs_by_product function in the functions.rell file located in the
src/digital_warehouse_chain folder.

The query collects the current inventory and historical updates, combining them into a tuple along with the warehouse
ID:

src/digital_warehouse_chain/functions.rell
```rell
function group_logs_by_product(value: list)>) {    val result = map>>();    for (v in value) {        if (v.product_category not in result) result[v.product_category] = [];        result[v.product_category].add(v);    }    return result;}
```

Configure the warehouse ID by adding new configuration details (moduleArgs) in the chromia.yml file:

chromia.yml
```yaml
blockchains:  digital_warehouse_chain:    # ↓↓↓ Add this code snippet ↓↓↓    moduleArgs:      digital_warehouse_chain:        warehouse_id: 1    # ↑↑↑ Add this code snippet ↑↑↑
```

Creating multiple warehouses is straightforward; simply define several blockchains in the chromia.yml file,each with its own set of module arguments.

chromia.yml
```yaml
blockchains:  digital_warehouse_chain_1:    module: digital_warehouse_chain    ...    moduleArgs:      digital_warehouse_chain:        warehouse_id: 1  digital_warehouse_chain_2:    module: digital_warehouse_chain    ...    moduleArgs:      digital_warehouse_chain:        warehouse_id: 2
```

## Unit tests​

To initiate testing for this app, we need to create a new file named src/digital_warehouse_chain_test.rell, where the
code for the test will be stored:

src/test/digital_warehouse_chain_test.rell
```rell
@test module;import digital_warehouse_chain.{ subscription, period, authorize, warehouse_account, period_to_millis, register_product_category, update_inventory, inventory_dto, create_report, UNIT };import lib.iccf_test.iccf_test_lib.{ iccf_proof_for };function test_grant_access() {    val certificate = subscription(        account_id = rell.test.pubkeys.alice,        period = period.WEEK    );    val tx = rell.test.tx();    val gtx = gtx_transaction(        body = gtx_transaction_body(            blockchain_rid = x"AB",            operations = [gtx_operation(name = "subscribe", args = [chain_context.blockchain_rid.to_gtv(), certificate.to_gtv()])],            signers = [rell.test.pubkeys.alice.to_gtv()]        ),        signatures = []    );    rell.test.set_next_block_time(1);    rell.test.block().run();    rell.test.tx()        .op(iccf_proof_for(x"AB", gtx))        .op(authorize(gtx))        .sign(rell.test.keypairs.alice)        .run();    val warehouse_account = warehouse_account @? { rell.test.pubkeys.alice };    assert_not_null(warehouse_account);    assert_equals(warehouse_account.valid_until, 1 + period_to_millis(period.WEEK));    rell.test.tx()        .op(register_product_category(101, UNIT.LITRE))        .op(update_inventory(inventory_dto(product_category = 101, amount = 5000, "Received milk shipment from barn")))        .sign(rell.test.keypairs.alice)        .run();    val report = create_report(null, null);    assert_equals(report.warehouse_id, 1);    assert_equals(report.inventory[0].stock, 5000);    assert_equals(report.history[101].size(), 1);    assert_equals(report.history[101][0].comment, "Received milk shipment from barn");}
```

Next, add the test definition along with its corresponding details to the chromia.yml file:

chromia.yml
```yaml
blockchains:  digital_warehouse_chain:    # ↓↓↓ Add this code snippet ↓↓↓    test:      modules:        - test.digital_warehouse_chain_test    # ↑↑↑ Add this code snippet ↑↑↑
```

This test creates a dummy gtx_transaction for the alice pubkey. It then forcefully sets the next block time to 1 and
executes an empty block before invoking the authorize operation. This step ensures that we obtain accurate block time
values when verifying the results.

In the final transaction executed, we include a gtx_operation that invokes iccf_proof with placeholder values,
except for gtx.hash().to_gtv(). This approach accommodates the test framework's limitation of not directly verifying
proofs; therefore, constructing the transaction and passing the correct transaction hash. We verify the successful
creation of the account, log an inventory update, and validate the generated report.

The tests are executed using the chr test command.

Below you can find the project structure and the contents of the chromia.yml configuration file after the changes have
been made. It can be very useful for comparison at this stage of the course:

Project structure

```text
digital_warehouse_example/├── build/├── src/│   ├── digital_warehouse_chain/│   │   ├── entities.rell│   │   ├── functions.rell│   │   ├── module.rell│   │   ├── operations.rell│   │   └── queries.rell│   ├── lib/│   ├── subscription_chain/│   │   ├── entities.rell│   │   ├── functions.rell│   │   ├── module.rell│   │   ├── operations.rell│   │   └── queries.rell│   ├── test/│   │   ├── subscription_chain_test.rell│   │   └── digital_warehouse_chain_test.rell│   └── main.rell├── .gitignore└── chromia.yml
```

Final version of the chromia.yml

```text
yaml title="chromia.yml"blockchains:  subscription_chain:    module: subscription_chain    test:      modules:        - test.subscription_chain_test  digital_warehouse_chain:    module: digital_warehouse_chain    config:      gtx:        modules:          - net.postchain.d1.iccf.IccfGTXModule    moduleArgs:      digital_warehouse_chain:        warehouse_id: 1    test:      modules:        - test.digital_warehouse_chain_testcompile:  rellVersion: 0.14.9database:  schema: schema_digital_warehouse_examplelibs:  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"  iccf_test:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/test/iccf    tagOrBranch: 1.87.0    rid: x"873D71557531E35E453587FA71C2D3CCB674F0EB49D18B30FA838C52C1155EB3"
```


===== FILE: courses__iccf-course__introduction.md =====


# Confirm Events Across Blockchains

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Confirm Events Across Blockchains

In this course, you’ll learn how to build a simple digital warehouse app that lets users prove they made a payment on
one blockchain, and then use that proof on another blockchain. This is done using Chromia’s Inter-Chain Confirmation
Facility (ICCF).

What is ICCF?

ICCF (Inter-Chain Confirmation Facility) is a tool that lets you prove that a transaction happened on one blockchain
and have another blockchain recognize that proof. It’s ideal for situations where you need to confirm an event.

ICCF makes it easy to show that something happened on one blockchain and have another blockchain recognize it. For
example, you can prove that a payment was made on Chain A, and then use that proof to unlock something on Chain B. The
process is user-driven: after making a transaction, the user collects a proof and presents it to the other chain
whenever they want.

This method is best for situations where you need to prove an event happened. If you want to send messages or data
between chains, Chromia’s [Inter-Chain Messaging Facility (ICMF)](https://docs.chromia.com/intro/about/protocols/icmf) might be a better fit.

What is a Cluster Anchoring Chain?

A special blockchain in the Chromia network that collects and anchors data from all blockchains in a dapp cluster. It
helps ensure data integrity and security by making it easy to detect tampering or unauthorized changes across the
cluster.

## Why use ICCF?​

- User-driven and asynchronous: Users can collect and present proofs at any time after their transaction is
confirmed, making the process flexible and efficient.

- Security: The event is anchored and the proof is verified by a trusted chain.

- Separation of concerns: One chain can handle payments, while another manages business logic or access control.

## What will you build?​

By the end of this course, you’ll have built a digital warehouse app where:

- Users make a fictional payment on one blockchain.

- They receive a proof of payment.

- They use that proof to gain access or perform actions on another blockchain.

## Example use cases​

- Cross-chain subscriptions: Prove that a user paid for a subscription on one chain to unlock services on another.

- Multi-chain access control: Grant access to digital goods or services on one blockchain based on actions (like
payments) on another.

- Decentralized identity verification: Prove identity or credentials issued on one chain to another chain’s
application.

- Gaming: Unlock in-game items or features on one blockchain based on achievements or purchases on another.

- Event ticketing: Prove ticket purchase on one chain to gain entry or benefits on another chain’s system.

## How does it work?​

Confirming an event (transaction) can be described by the following sequence:

- The user sends a transaction to the source chain.

- The source chain confirms the transaction and includes it in a block.

- The block is sent to the Cluster Anchoring Chain as a transaction.

- The Cluster Anchoring Chain verifies the block (anchors) and includes it in its own block.

- The user constructs a proof by obtaining a confirmation proof for the transaction.

- The user presents the proof to the target chain as another transaction.

- The target chain validates the proof by verifying its anchoring in the Cluster Anchoring Chain.

- The target chain completes its verification by including the proof in a block.

As seen in this flow, the user drives this operation. The user constructs and presents the proof, making this step
asynchronous and possible anytime after the blocks have been confirmed. This approach is particularly suitable for
applications focusing on proving occurrences rather than transferring data. While it can be used for sending messages,
if the user is not concerned with verifying when the message arrives, using the messaging facility (ICMF) might be more
appropriate.

In this course, we will explore a scenario where one blockchain handles monetary transactions and subscriptions while
another chain manages the business logic. This setup allows us to prove that a monetary transaction occurred without the
money leaving the first chain.

## Related materials​

This course relies on the following documentation, which can help you understand the underlying concepts and approaches:

| 
| Section| Type| Documentation
| Overview| Cross-chain| [ICCF](https://docs.chromia.com/intro/cross-chain/icmf)
| FT4| Introduction| [FT4](https://docs.chromia.com/ft4/intro)
| Overview| Dapps| [Building your dapps on Chromia](https://docs.chromia.com/intro/getting-started/create-dapp/)

## Repository link​

The complete code repository for this course is available here:
[ICCF course repository](https://bitbucket.org/chromawallet/iccf-course).


===== FILE: courses__iccf-course__manual-testing.md =====


# Testing the dapp

URL: https://learn.chromia.com

- [Home](/)
- Lesson 4 - Test the dapp
# Testing the dapp

We test the dapp manually by starting the corresponding blockchains, creating a subscription, and using it to access the
warehouse.

First, ensure that the blockchains are defined in the following order to align with the internal IDs used in this
course:

chromia.yml
```yaml
blockchains:  subscription_chain: # Chain id 1  ...  digital-warehouse-chain: # Chain id 2  ...
```

To start the blockchains, you need to run the node from the project folder in a separate terminal window:

```shell
chr node start --directory-chain-mock
```

Expected output of logs:

```shell
...Starting blockchain directory-chain with brid B39406781EE6CA71391B519AA1D3B009DA77A0A63B12DCB2BD4DD59C679E2703 on id 0Starting blockchain subscription_chain with brid EB0A24835FC5B49CFCB6A9C3DC688FBC7D457455E69B9B2956E7689BC830CA49 on id 1Starting blockchain digital_warehouse_chain with brid 42C95A7CCDAE9B78524A32428E7B313F60AEB31BA66FF41A0ECE2FCB576D5FF4 on id 2...
```

Please note that values of brids can be different on your side.

Let`s save brids into environment variables for convenient reuse in the following steps:

```bash
SUBSCRIPTION_CHAIN_BRID=$(curl -s localhost:7740/brid/iid_1) && echo $SUBSCRIPTION_CHAIN_BRID
```

```bash
DIGITAL_WAREHOUSE_BRID=$(curl -s localhost:7740/brid/iid_2) && echo $DIGITAL_WAREHOUSE_BRID
```

Proceed with the setup by creating a new keypair and storing it in the .chromia/config directory, the default search
path for Chromia CLI:

```shell
chr keygen --file .chromia/config
```

Note down the pubkey printed to the console. You will use this as the account ID input for our operations.

Next, attempt to register a new product category to observe that making a transaction towards the digital warehouse will
fail:

```shell
chr tx -brid $DIGITAL_WAREHOUSE_BRID register_product_category 555 0 --await -cfg .chromia/config
```

Result
```shell
... Operation 'digital_warehouse_chain:register_product_category' failed: No account found
```

Create a new subscription on the Subscription chain before attempting again.

Now, you can create a new subscription. Replace <pubkey> with the previously generated one:

```shell
chr tx -brid $SUBSCRIPTION_CHAIN_BRID subscribe 'x"" '[x"", 0]' --await -cfg .chromia/config
```

Example for using an environment variable substitution:

```bash
chr tx -brid $SUBSCRIPTION_CHAIN_BRID subscribe  'x"'"$DIGITAL_WAREHOUSE_BRID"'"' '[x"021E6EE49D997C6DF7721F87027BB56D7436A9CC78F344AE6FF2E8A08789D37F5A", 0]' --await -cfg .chromia/config
```

Output should be like this:

```bash
transaction with rid F300CBD62F22917145F8594DB63F1780803CD123CA7F89DBE952431409EE2629 was posted CONFIRMED
```

Note the transaction rid that was output from the command. You can now use the following command to authenticate the
digital warehouse. Replace <tx-rid> with the previously obtained value.

This command specifies that we intend to make a transaction towards chain ID 2 and include an iccf-transaction that
needs verification against chain 1:

```shell
chr tx -brid $DIGITAL_WAREHOUSE_BRID --iccf-source $SUBSCRIPTION_CHAIN_BRID --iccf-tx  authorize  --await -cfg .chromia/config
```

Now, we should be able to access the system. Let's attempt to register a product category and update the inventory
again:

```shell
chr tx -brid $DIGITAL_WAREHOUSE_BRID register_product_category 555 0 --await -cfg .chromia/config
```

```shell
chr tx -brid $DIGITAL_WAREHOUSE_BRID update_inventory '[555, 1500, "Got some stuff"]' --await -cfg .chromia/config
```

Let's wrap up by generating a warehouse report and checking the receipts in the subscription chain:

```shell
chr query -brid $DIGITAL_WAREHOUSE_BRID create_report 'from=0' 'to=null' -cfg .chromia/config
```

Result
```shell
[    "history": [        [555, [            ["amount": 1500, "comment": "Got some stuff", "product_category": 555, "transaction": 5]]            ]        ],    "inventory": [        ["UNIT": "LITRE", "product_category": 555, "stock": 1500]        ],    "warehouse_id": 1]
```

```shell
chr query -brid $SUBSCRIPTION_CHAIN_BRID get_receipts 'account_id=null' 'blockchain_rid=null' 'from=null'
```

Result
```shell
[    [        "account_id": x"02EEB43C7400CA3CEBDECF1C8AC049EAC05EB361FE4C003EF6D298DEA792F58526",        "blockchain_rid": x"237B0EE3C60CBB1884E63F883F8230FED5C804A9C6933B28FD9214D65FD033B5",        "payment_amount": 30,        "period": "WEEK",        "receipt_id": x"6006EFC423E9032EA4C9584CDE791623A015749FD8BA4831457CCE0769BCA20A",        "timestamp": 1705504892289    ]]
```

Congratulations! You have now learned how to develop a dapp that verifies transactions on another chain to authenticate
users.


===== FILE: courses__iccf-course__setup.md =====


# Set up your project

URL: https://learn.chromia.com

- [Home](/)
- Set up your projectOn this page
# Set up your project

Before we start, please make sure you have the following prerequisites in place:

Set up PostgreSQL database

# Set up PostgreSQL database

Rell requires PostgreSQL 16.3. The IDE can work without it but can't run a node. A console or a remote postchain app can
run without a database.

The default database configuration for Rell is:

- database: postchain

- user: postchain

- password: postchain
... [standard PG+CLI setup omitted] ...

===== FILE: courses__iccf-course__subscription-chain.md =====


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


===== FILE: courses__iccf-course__system-overview.md =====


# System overview

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - System overviewOn this page
# System overview

The decentralized application (dapp) we are developing comprises two blockchains: the Subscription Chain and the Digital
Warehouse Chain.

The user initiates a payment on the Subscription Chain to establish a 'subscription'. This involves deducting a
specified value from the user's account. This process can be further customized and expanded using the
[FT-library](https://docs.chromia.com/ft4/intro) at a later stage. Subsequently, the user presents a payment receipt to
the Digital Warehouse Chain, enabling access to the system. This access facilitates the user in managing inventory
updates within a fictional warehouse.

This architecture facilitates the management of multiple warehouses through the deployment of multiple warehouse chains.
By delegating payment processing to another chain, this component could potentially integrate into a broader, more
versatile system.

## Subscription model​

To model our subscription, we will create a structure that can be passed to the Subscription Chain and later verified on
the Digital Warehouse chain. This structure will serve as metadata to be extracted by the warehouse during payment
verification. Let’s create a new directory called subscription_chain and add a file named module.rell to it.

src/subscription_chain/module.rell
```rell
module;
```

Create a file named entities.rell in the src/subscription_chain directory and insert the following code:

src/subscription_chain/entities.rell
```rell
struct subscription {    account_id: pubkey;    period;}enum period {    WEEK,    MONTH,}val MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
```

Create another file called functions.rell in the src/subscription_chain and insert the following code:

src/subscription_chain/functions.rell
```rell
function period_price(period): integer {    return when (period) {        MONTH -> 90;        else -> 30;    };}function period_to_millis(period): integer {    return MILLIS_IN_A_DAY * when (period) {        MONTH -> 30;        else -> 7;    } ;}
```

When a user requests a new subscription, we set the account ID and period. Additionally, utility functions are provided
to convert the period to an associated price and duration.
