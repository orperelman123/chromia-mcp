# Let&#x27;s look at an example

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 5 - Understand blockchain state and transactions](/courses/book-review/blockchain-transactions/)
- Let's look at an example
# Let's look at an example

Now that we're all set up, let's add a couple of transactions and then query both the state and the transactions to see how they interact.

First, we'll clear all data from our node to start with a fresh slate. Before running the command, if the Chromia Node is running, stop it with Ctrl+C. Then, you can clear the data by running the following command:

```shell
chr node start --wipe
```

This command will remove all transactions from the blockchain and reset the current state. You can verify this by running:

```shell
chr query get_all_books
```

This should return an empty array, indicating that there are no books in the current state.

Next, let's add a new transaction to create a book, intentionally misspelling "Chromia" so that we can later correct this with an update:

```shell
chr tx create_book "ISBN001" "Crhomia 101" "Alice" --key-id "book_admin" --await
```

The output should look like this:

```shell
transaction with rid DF94A07EACBA673CC4ECA4A43D70CDA4FD30E4335E124B7153FB12A78FDD8CE9 was posted CONFIRMED
```

tipInstead of specifying --key-id with every transaction, you can define the default key in a .chromia/config file:

```ini
key.id = book_admin
```
You can place this file in one of two locations:

- Global config: ~/.chromia/config — applies to all projects unless overridden

- Project config: .chromia/config in your project folder — overrides the global file
Once set, you no longer need to include --key-id in CLI transactions:

```bash
chr tx create_book "ISBN001" "Crhomia 101" "Alice" --await
```
The CLI will automatically use the configured key.

For full details on config file precedence, see:
[Key pair reading flow](https://docs.chromia.com/cli/key-pair-management#key-pair-reading-flow)

After creating this transaction, you can query all books using the get_all_books command:

```shell
chr query get_all_books
```

The result should look like this:

```json
[  {    "author": "Alice",    "isbn": "ISBN001",    "title": "Crhomia 101"  }]
```

This confirms that the state/table has been correctly updated with the new book information. Now, let's query the blockchain transactions:

```shell
chr query get_transactions
```

The result will be:

```json
[  [    "body": [      "blockchain_rid": x"B52F0A078993374201853649F807FDEDD6516A424557F311E2AF769761F89D40",      "operations": [        [          "args": [            "ISBN001",            "Crhomia 101",            "Alice"          ],          "name": "create_book"        ]      ],      "signers": [        x"035A25AB32B85E6A5B58A31A71CC709CED159096194984243159492CBA0FADF82F"      ]    ],    "signatures": [      x"91EFC02C2958698E71C41D2B020F9E4EF81F6C427FAA98667D6FF4B87857809E1ECE4C9C923B1E5659177B8D0635711E47A4EBB502217A437D3FFC89741E74DD"    ]  ]]
```

This indicates that the transaction has been successfully added to the blockchain and the state has been updated accordingly.

The following diagram illustrates the process when we run the query get_transactions command:

Next, we'll add a new operation to update a book's title, allowing us to correct the misspelled title. To do this, we need to make the title mutable in our book entity:

src/main/entities.rell
```rell
entity book {  key isbn: text;  mutable title: text;  author: text;}
```

Next, we can add the update operation:

src/main/operations.rell
```rell
operation update_book(isbn: text, title: text){  val adminPubkey = chain_context.args.admin_pubkey;  require(op_context.is_signer(adminPubkey), "Only admin can update books");  update book @ { .isbn == isbn }( .title = title );}
```

Before testing this new operation, update the Chromia Node to ensure it includes the new operation by running the following command and waiting for the next block:

```shell
chr node update
```

After updating, we can test the new operation with a transaction to update a book's title:

```shell
chr tx update_book "ISBN001" "Chromia 101" --key-id "book_admin" --await
```

After this transaction, we can query both the blockchain transactions and the state again:

```shell
chr query get_all_books
```

The result will now show the updated title:

```json
[  {    "author": "Alice",    "isbn": "ISBN001",    "title": "Chromia 101"  }]
```

Similarly, when querying the blockchain transactions:

```shell
chr query get_transactions
```

The result will be:

```json
[  [    "body": [      "blockchain_rid": x"B52F0A078993374201853649F807FDEDD6516A424557F311E2AF769761F89D40",      "operations": [        [          "args": [            "ISBN001",            "Crhomia 101",            "Alice"          ],          "name": "create_book"        ]      ],      "signers": [        x"035A25AB32B85E6A5B58A31A71CC709CED159096194984243159492CBA0FADF82F"      ]    ],    "signatures": [      x"91EFC02C2958698E71C41D2B020F9E4EF81F6C427FAA98667D6FF4B87857809E1ECE4C9C923B1E5659177B8D0635711E47A4EBB502217A437D3FFC89741E74DD"    ]  ],  [    "body": [      "blockchain_rid": x"B52F0A078993374201853649F807FDEDD6516A424557F311E2AF769761F89D40",      "operations": [        [          "args": [            "ISBN001",            "Chromia 101"          ],          "name": "update_book"        ]      ],      "signers": [        x"035A25AB32B85E6A5B58A31A71CC709CED159096194984243159492CBA0FADF82F"      ]    ],    "signatures": [      x"FD9402B45ED52A238B0C756AFA1ABA5BD2F89C9AE44343E58FE76E207F4672CF0AF5C37F7209E5D9A474491671000FAFA9753A4BF2B5D16AE7C5C48CFA18A8E1"    ]  ]]
```

You’ll notice that there are two transactions on the blockchain: one for creating the book and another for updating its title.

When we examine the state, it now reflects the aggregation of these two transactions. This means that the state encompasses all the transactions related to this book, allowing us to backtrack through history and see exactly how the current state was established.

Understanding the interaction between Rell, Chromia, the blockchain, and the state is essential for effectively building and managing decentralized applications.
