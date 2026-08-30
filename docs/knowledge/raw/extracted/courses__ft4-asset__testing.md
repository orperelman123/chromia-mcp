# Testing

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - TestingOn this page
# Testing

In this lesson, we will cover important security considerations and how to test our FT4 asset implementation.

### 1. End-to-end tests​

The test cases can be found in the [repo](https://bitbucket.org/chromawallet/ft4-course/src/main/), under src/test/assets_test.rell. Copy this file into your own src/test/ folder.

To run the tests, use the following command:

```bash
chr build & chr test
```

### 2. Manual testing on local node​

#### Start the node locally​

```bash
chr node start
```

#### Generate keypairs of the test users​

Generate key pairs for the test users:

Alice:

```bash
chr keygen --file .chromia/alice_keypair
```

Trudy:

```bash
chr keygen --file .chromia/trudy_keypair
```

#### Initialize the dapp​

Initialize the dapp using the admin key pair to create the pool account (initializing once is enough, but not strictly mandatory):

```bash
chr tx initialize_dapp --secret .chromia/admin_keypair
```

#### Mint initial assets​

Mint initial assets to the pool account:

```bash
chr tx mint --secret .chromia/admin_keypair
```

Expected output:

```bash
transaction with rid D6A1E290E9E00E25453D182742CB92D6707BD38B89712C8AE6A53EF9452BF55A was posted and confirmed
```

Note: The transaction RID will be different for each transaction execution.

#### Check pool account balance​

To verify the pool account's balance, use:

```bash
chr query get_pool_account_balance
```

Output:

```bash
1000000000L
```

#### Create user accounts​

User accounts are required for authentication in calling operations.

Alice:

```bash
chr tx ft4.ras_open '[0, [["A","T"],x"ALICE_PUBKEY"], null]' 'null' --ft-register-account --secret .chromia/alice_keypair --await
```

The authorization flags for the auth descriptor are described
[there](https://docs.chromia.com/ft4/account-management/auth-descriptors#built-in-authorization-flags).

Trudy:

```bash
chr tx ft4.ras_open '[0, [["A","T"],x"TRUDY_PUBKEY"], null]' 'null' --ft-register-account --secret .chromia/trudy_keypair --await
```

#### Faucet assets​

To get test assets from the faucet:

Alice:

```bash
chr tx faucet --ft-auth --secret .chromia/alice_keypair
```

Trudy:

```bash
chr tx faucet --ft-auth --secret .chromia/trudy_keypair
```

#### Transfer assets​

To transfer assets to another account, use:

```bash
chr tx transfer 'x"TRUDY_PUBKEY"' 100L --ft-auth --secret .chromia/alice_keypair
```

#### Lock/Unlock assets​

To lock assets for a specific account, run:

```bash
chr tx lock_asset "FT4_LOCK" 'x"ALICE_PUBKEY"' 10L --secret .chromia/admin_keypair
```

FT4_LOCK is a default
[FT4 lock type](https://docs.chromia.com/ft4/backend/assets/locking-assets#lock-account-overview).

To unlock assets, use:

```bash
chr tx unlock_asset "FT4_LOCK" 'x"ALICE_PUBKEY"' 10L --secret .chromia/admin_keypair
```

#### Check user balances​

To check a user's asset balance, run:

```bash
chr query get_user_account_balance pubkey='x"ALICE_PUBKEY"'
```

Output:

```bash
99900L
```

Note: The actual balance amount may vary depending on the current state of the user's account and any previous transactions.

To check a user's locked asset balance, use:

```bash
chr query get_user_lock_account_balance pubkey='x"ALICE_PUBKEY"'
```

Output:

```bash
[]
```

Example with locked balances:

```bash
[  [    "data": [      "amount": 20L,      "type": "FT4_LOCK"    ],    "rowid": 31  ]]
```

Note:

- An empty array [] indicates no locked balances in the account

- When locked balances exist, each entry contains:

- amount: The locked amount in the smallest unit (with L suffix)

- type: The type of lock (e.g., "FT4_LOCK")

- rowid: A unique identifier for the lock record
