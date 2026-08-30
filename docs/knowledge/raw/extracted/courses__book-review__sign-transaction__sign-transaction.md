# Sign a transaction

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 4 - Sign a transaction and filter queries](/courses/book-review/sign-transaction/)
- Sign a transactionOn this page
# Sign a transaction

In this section, we will learn how to sign transactions, which involves verifying the authenticity of the user sending the transaction to the blockchain.

We will create an administration function for a bookkeeper responsible for managing and adding books available for review. Only a specific administrator will be authorized to sign the transactions required for this task.

## Setting up module arguments​

We will use module arguments to ensure that only the administrator can add books. These arguments configure a module on the Chromia blockchain and enable access to variables from a context called chain_context. Here’s how to set it up:

### Create a keypair​

First, generate a new keypair for the administrator, which will be used to sign transactions:

```bash
chr keygen --key-id="book_admin"
```

This generates a mnemonic phrase and a keypair, which are stored in your home directory under ~/.chromia/.

- Public key: ~/.chromia/book_admin.pubkey

- Private key: ~/.chromia/book_admin

- Mnemonic phrase: ~/.chromia/book_admin_mnemonic

You can then immediately copy the public key from the terminal.
Or, if you need to retrieve it later and your CLI version is 0.27.1 or higher, you can use:

```bash
chr keygen --get-pubkey book_admin
```

Check your CLI version with:

```bash
chr --version
```

If you're on an older version, either [update your CLI](https://docs.chromia.com/intro/getting-started/installation/cli-installation#updating-chromia-cli)
or retrieve the key directly from the file:

```bash
cat ~/.chromia/book_admin.pubkey; echo  # add a newline after the key
```

### Define a struct for module arguments​

Next, we need to define a struct to hold our module arguments. This struct should be added to the module where these arguments will be accessed. For this example, add it to src/main/entities.rell:

src/main/entities.rell
```rell
struct module_args {  admin_pubkey: byte_array;}
```

### Configure module arguments in chromia.yml​

Now, update your chromia.yml configuration to include the administrator's public key:

```yaml
blockchains:  book_review:    module: main    moduleArgs:      main:        admin_pubkey: 
```

Replace the placeholder with the actual key printed by the cat command above.

Explanation of the example configuration:

- book-review: This is the name of your blockchain configuration. It should correspond to the blockchain configuration you are working with.

- module: main: Specifies that the main module is used for this blockchain configuration.

- moduleArgs: Under this section, you define arguments specific to the module.

- main: This should match the module name defined in your source code.

- admin_pubkey: This is the argument name that the module will use to verify the administrator's public key.

- <your book_admin public key>: Replace this placeholder with the actual public key extracted from the .chromia/book_admin.pubkey file. This key is used to authorize administrative actions.

For more details on the chromia.yml configuration file, refer to the [Chromia project settings documentation](https://docs.chromia.com/intro/configuration/project-config).

## Require signed transactions​

Now, we will update our create_book operation to require that the administrator signs the transaction. Modify src/main/operations.rell with the following code:

src/main/operations.rell
```rell
operation create_book(isbn: text, title: text, author: text) {  val adminPubkey = chain_context.args.admin_pubkey;  require(op_context.is_signer(adminPubkey), "Only admin can create books");  create book ( .isbn = isbn, .title = title, .author = author );}
```

Here’s what happens in this operation:

- Retrieve the administrator's public key: val adminPubkey = chain_context.args.admin_pubkey;

- This fetches the public key from the module arguments defined in chain_context.

- Check if the transaction is signed by the administrator: require(op_context.is_signer(adminPubkey), "Only admin can create books");

- This checks if the transaction is signed with the administrator's public key. If not, it throws an error message: "Only admin can create books".

This ensures that only transactions signed by the administrator can create books.

## Setup and test signing​

To verify transaction signing, we will update the existing test functions to ensure correct behavior.

### Step 1: Update test_add_book​

First, add a local keypair for testing in your test file src/test/book_review_test.rell:

src/test/book_review_test.rell
```rell
val book_keeper = rell.test.keypair(    priv = x"DEE3B1414196653BF7FA621B2EEFC3146093B1932BA2ABFAEED830906D81972A",    pub = x"0359A8F2CE1BEF95F583169B7DF053AA227A93B2652B0A9C22975FEED638032610");
```

This snippet creates a test keypair for book_keeper with the provided private and public keys.

Next, update your chromia.yml to include this new keypair:

chromia.yml
```yaml
blockchains:  book_review:    module: main    moduleArgs:      main:        admin_pubkey: compile:  rellVersion: 0.14.9database:  schema: schema_book_reviewtest:  modules:    - test  moduleArgs:    main:      admin_pubkey: "0359A8F2CE1BEF95F583169B7DF053AA227A93B2652B0A9C22975FEED638032610"
```

In the test section, set the admin_pubkey to match the public key used for book_keeper.

Now, update the existing test_add_book function to utilize the book_keeper keypair:

src/test/book_review_test.rell
```rell
function test_add_book() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book("124", "Book2", "Author2"))        .sign(book_keeper)        .run();    val all_books = book @* { };    assert_equals(all_books.size(), 2);    assert_equals(all_books[0].title, "Book1");    assert_equals(all_books[0].author, "Author1");}
```

### Step 2: Update test_add_book_review​

Next, modify the existing test_add_book_review function to use the book_keeper keypair:

src/test/book_review_test.rell
```rell
function test_add_book_review() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book_review("123", "Reviewer1", "ReviewText1", 5))        .op(create_book_review("123", "Reviewer2", "ReviewText2", 3))        .sign(book_keeper)        .run();    val reviews = book_review @* { };    val book = book @ { .isbn == "123" };    assert_equals(reviews.size(), 2);    assert_equals(book, reviews[0].book);    assert_equals(reviews[0].reviewer_name, "Reviewer1");    assert_equals(reviews[0].review, "ReviewText1");    assert_equals(reviews[0].rating, 5);}
```

### Step 3: Update test_get_books​

Then, update the test_get_books function to verify the creation of books:

src/test/book_review_test.rell
```rell
function test_get_books() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .op(create_book("124", "Book2", "Author2"))        .sign(book_keeper)        .run();    val books = get_all_books();    assert_equals(books.size(), 2);}
```

### Step 4: Test non-administrator access​

To further check security, create a test where a keypair not configured as an administrator attempts to sign a transaction.

The available users/keypairs are:

```text
bob, alice, trudy, charlie, dave, eve, frank, grace, heidi
```

Add a definition for the bob keypair in your test file:

src/test/book_review_test.rell
```rell
val bob = rell.test.keypairs.bob;
```

Use this keypair in a test to sign the create_book transaction:

src/test/book_review_test.rell
```rell
function test_add_book_as_non_admin() {    rell.test.tx()        .op(create_book("123", "Book1", "Author1"))        .sign(bob)        .run_must_fail();}
```

noteThis test is expected to fail, indicating that it is correctly enforcing the test condition. Bob's public key is not listed as an administrator in the module arguments, so the transaction should fail.

## Run tests​

After updating your tests, run them to ensure everything is working as expected:

```bash
chr test
```

This command will execute all the test functions and confirm that the transaction signing and authorization are functioning correctly.
