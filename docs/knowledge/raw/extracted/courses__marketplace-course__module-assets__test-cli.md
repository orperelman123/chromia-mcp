# Test using Chromia CLI

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Build a marketplace](/courses/marketplace-course/module-assets/)
- Lesson 5 - Test using Chromia CLIOn this page
# Test using Chromia CLI

We now have all the building blocks for our marketplace, so let's test them out using Chromia CLI. Chromia CLI is a command-line tool that allows interaction with the Chromia blockchain utilizing a set of commands. You can read more [about it here](https://docs.chromia.com/cli/introduction).

## Queries for verification​

To make data verification easier, we will add two helper queries to our app. They will help us fetch and verify accounts and account balances.

src/rell_marketplace/module.rell
```rell
import lib.ft4.assets.{ asset, Unsafe, balance };
```

src/rell_marketplace/queries.rell
```rell
query get_all_accounts(){    return account @* {} ($.to_struct());}query get_all_balances(){    return balance @* {} ($.account.to_struct(), $.to_struct());}
```

## Begin testing​

Now we can begin our testing, and we start our blockchain node by running:

### Create accounts​

```shell
chr node start
```

Open a new terminal, where we will start issuing commands using the CLI. First, we can make sure that the dapp account was created by querying the accounts.

```shell
chr query get_all_accounts
```

Now, we can create two users, Alice and Bob.

```shell
chr keygen --file .chromia/alice.keypairchr keygen --file .chromia/bob.keypairchr tx --await create_user "" --secret .chromia/alice.keypairchr tx --await create_user "" --secret .chromia/bob.keypair
```

chr tx will send a transaction to call an operation on the blockchain.

We can now run a query to fetch the balances for all accounts. We want to confirm that our minting operation works correctly, so both Alice's and Bob's accounts should have 1000 CRDs.

```shell
chr query get_all_balances
```

This will generate a result similar to this, which contains our two created accounts and their balance (1000 CRD) that we minted in create_user

```shell
[    [[x"3A0153952DCB30A904B1E3461321892C916B10E51897F32B3352DB65DDDA4562"], [7, 1, 1000L]],     [[x"8EF62426BA321FB23D6163DCFBFC53049E9A078500264F012BC4E4AB5048A859"], [13, 1, 1000L]]]
```

### Buy a mystery card​

Now that we have verified that the balances are okay, we can let Alice buy a mystery card.

```shell
chr tx --await buy_mystery_card --ft-auth --ft-account-id= --secret .chromia/alice.keypair
```

We can now have a look at the currently listed nft_cards and also check the balances to make sure the transaction has been completed.

```shell
chr query get_all_nfts
```

It should result in something similar to this, showing a card NFT with its properties.

```shell
[["health": 10, "nft_id": 22, "owner_id": x"", "strength": 8]]
```

Checking the balance by using the following query

```shell
chr query get_all_balances
```

will result in the following balances for Alice, Bob, and the Economy Account.

| 
| Participant| Action| Amount| Balance
| Alice| Bought a mystery card| -100 CRD| 900 CRD
| Bob| No action| | 1000 CRD
| Economy Account| Received from Alice| +100 CRD| 100 CRD

```shell
[    [[x"3A0153952DCB30A904B1E3461321892C916B10E51897F32B3352DB65DDDA4562"], [7, 1, 900L]],  // Alice    [[x"8EF62426BA321FB23D6163DCFBFC53049E9A078500264F012BC4E4AB5048A859"], [13, 1, 1000L]],  // Bob    [[x"522546AF72636ABA2AFE34E1BF2D7BF2B9C0A483C20F87E145BE32992339962C"], [2, 1, 100L]] // Economy account]
```

### List cards on the marketplace​

We let Alice list her card on the marketplace for 25 CRD. We can get the nft_id from our previous query get_all_nfts and the FT4 account id for Alice from get_all_balances

```shell
chr tx --await list_nft "" "25L" --secret .chromia/alice.keypair --ft-auth --ft-account-id=
```

and then we query using get_cards and pass arguments for filtering and sorting

```shell
chr query get_cards "amount=10" "card_sorting=PRICE_HIGH"
```

This should generate a single result with the card Alice listed on the marketplace.

### Buy a card from the marketplace​

Now, we will let Bob buy Alice's card by calling the buy_card operation

```shell
chr tx --await buy_nft "" --secret .chromia/bob.keypair --ft-auth --ft-account-id=
```

followed by query.

```shell
chr query get_all_balances
```

This will generate our final balances, which should look like this

| 
| Participant| Action| Balance
| Alice| Bought a mystery card for 10 CRD
Received 25 CRD from Bob| 925 CRD
| Bob| Bought Alice's card for 25 CRD| 975 CRD
| Economy Account| Received fee of 100 CRD from Alice| 100 CRD
and from our query.

```shell
[    [[x"3A0153952DCB30A904B1E3461321892C916B10E51897F32B3352DB65DDDA4562"], [7, 1, 925L]],  // Alice    [[x"8EF62426BA321FB23D6163DCFBFC53049E9A078500264F012BC4E4AB5048A859"], [13, 1, 975L]],  // Bob    [[x"522546AF72636ABA2AFE34E1BF2D7BF2B9C0A483C20F87E145BE32992339962C"], [2, 1, 100L]]     // Economy account]
```

This concludes our testing using Chromia CLI and also ends this course. We hope that you enjoyed it, and happy coding!
