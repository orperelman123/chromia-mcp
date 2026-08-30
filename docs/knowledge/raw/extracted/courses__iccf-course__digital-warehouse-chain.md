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
