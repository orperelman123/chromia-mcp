# COURSE marketplace-course — 15 pages


===== FILE: courses__marketplace-course__introduction.md =====

# Build a decentralized marketplace using FT4

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Build a decentralized marketplace using FT4

This course will guide you through building a decentralized marketplace where users can buy and sell game trading cards.

What we will cover:

- Registering a payment token to be used in the marketplace

- Registering and setting up user accounts

- Defining and minting NFTs using a model built in rell

- Building a marketplace where you can list NFTs for sale

- Handling transactions and transfering NFTs and Payment tokens between users

For many of these operations, we will use the
[Chromia FT4 library](https://docs.chromia.com/ft4/intro), which manages accounts and assets and
handles transfers between accounts.

## Repository link​

The complete code repository for this course is available here:
[Marketplace course repository](https://bitbucket.org/chromawallet/marketplace-course).


===== FILE: courses__marketplace-course__module-assets.md =====

# Module 3 - Build a marketplace

URL: https://learn.chromia.com

- [Home](/)
- Module 3 - Build a marketplace
# Module 3 - Build a marketplace

In this module, we will build our complete marketplace, supporting buying mystery cards, listing your cards on the marketplace, and operating to purchase cards from other users.

## Lessons
[Lesson 1 - Add a fee for buying a mystery card](/courses/marketplace-course/module-assets/buy-mystery-card)[Lesson 2 - List a card for sale on the marketplace](/courses/marketplace-course/module-assets/list-card)[Lesson 3 - Purchase a card from the marketplace](/courses/marketplace-course/module-assets/buy-listed-card)[Lesson 4 - Test our marketplace](/courses/marketplace-course/module-assets/test-marketplace)[Lesson 5 - Test using Chromia CLI](/courses/marketplace-course/module-assets/test-cli)[Start module »](/courses/marketplace-course/module-assets/buy-mystery-card)


===== FILE: courses__marketplace-course__module-assets__buy-listed-card.md =====

# Purchase a card from the marketplace

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Build a marketplace](/courses/marketplace-course/module-assets/)
- Lesson 3 - Purchase a card from the marketplace
# Purchase a card from the marketplace

The marketplace is taking shape, and we now have functions to list NFTs that are up for sale. So, the next step is to create an operation to complete a purchase.

src/rell_marketplace/marketplace.rell
```rell
operation buy_nft(id: integer) {    val account = auth.authenticate();    val (nft, owner, price) = listed_nft @ {        .nft.rowid == rowid(id)    } ( .nft, .listed_by, .price );    Unsafe.transfer(account, owner, dapp_meta.asset, price);    delete listed_nft @ { .nft.rowid == rowid(id) };    nft.owner = account;}
```

When a user calls buy_nft, we verify that the user is authenticated. Then, we fetch the NFT and confirm it's listed by querying listed_nft. Next, we transfer the amount specified by the price from the account buying the NFT to the owner who is selling it. Lastly, we delete the NFT from being listed on the marketplace and set the new owner of the NFT.

Our marketplace now supports the following:

- A user can buy an NFT, which is then minted in our dapp.

- NFTs can be listed on the marketplace at a set price.

- Users can buy a listed NFT at the asked price.


===== FILE: courses__marketplace-course__module-assets__buy-mystery-card.md =====

# Add a fee for buying a mystery card

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Build a marketplace](/courses/marketplace-course/module-assets/)
- Lesson 1 - Add a fee for buying a mystery card
# Add a fee for buying a mystery card

In the minting lesson, we added the operation buy_mystery_card, which a user calls to mint a new random card. But for it to actually be a purchase operation, we need to set a price and withdraw this amount from the user when the operation is called.

The card cost will be withdrawn from the user and added to a particular dapp account, which holds all revenues from card sales.

This dapp economy account doesn't exist yet, so let's use FT4 again to create this account. We need to add a new entity, dapp_account, to store our economy account, which gives us easy access to this account from our Rell code.

We define a structure in our Rell code to reference the account.

src/rell_marketplace/module.rell
```rell
struct module_args {    dapp_account_signer: pubkey;}
```

Then we create the account in the dapp_meta object we declared earlier.

src/rell_marketplace/module.rell
```rell
object dapp_meta {    asset = Unsafe.register_asset("Collector Card", "CRD", 6, chain_context.blockchain_rid, "https://url-to-asset-icon");    account = create_account_with_auth(single_sig_auth_descriptor(chain_context.args.dapp_account_signer, set(["A", "T"])));}
```

Let's break it down: we use the function create_account_with_auth to create a new account, and we pass a single signature auth descriptor with these values:

- Dapp account pubkey from moduleArgs settings

- Flags are "A" for managing accounts and "T" to allow token transfers using this account.

Then, we create a new keypair which we will use as "account owner" for our dapp:

```shell
chr keygen --file .chromia/dapp-account
```

Then we open chromia.yaml add this as a module argument, and add the pubkey to the economy account as value.

chromia.yml
```yaml
blockchains:  rell_marketplace:    module: main    moduleArgs:      lib.ft4.core.accounts:        rate_limit:          max_points: 10          recovery_time: 5000          points_at_account_creation: 2        lib.ft4.core.admin:          admin_pubkey: x"" # Replace with previously generated public key here.        rell_marketplace:          dapp_account_signer: x"" # Replace with previously generated public key here.compile:  rellVersion: 0.14.9database:  schema: schema_rell_marketplacetest:  modules:    - rell_marketplace_test  moduleArgs:    rell_marketplace:      dapp_account_signer: x"" # Replace with previously generated public key here.    lib.ft4.core.admin:        admin_pubkey: x"" # Replace with previously generated public key here.libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

Now, we have an account to which we can transfer charges. For example, when someone buys a card, we transfer the amount from the user to this account.

Let's put this to use by adding a transfer when a user buys a mystery card. We add a transfer with the following parameters:

- From account

- To account

- Asset

- Amount

Our operation buy_mystery_card will now look like this

src/rell_marketplace/marketplace.rell
```rell
operation buy_mystery_card() {    val account = auth.authenticate();    Unsafe.transfer(account, dapp_meta.account, dapp_meta.asset, 100);    mint_card(account);}
```

In this operation we fixed the price of purchasing a card to 100 CRD, which will transfer 100 CRD from the authenticated account to our dapp_account.


===== FILE: courses__marketplace-course__module-assets__list-card.md =====

# List a card for sale on the marketplace

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Build a marketplace](/courses/marketplace-course/module-assets/)
- Lesson 2 - List a card for sale on the marketplace
# List a card for sale on the marketplace

Now, we have the capability to buy mystery cards, in which you will receive a randomly minted collector card.

The next step is to extend the marketplace with functions for listing cards on the market and for users to buy and sell cards.

We start by adding the listed_nft entity.

src/rell_marketplace/nft.rell
```rell
entity listed_nft {    key nft: nft;    mutable price: big_integer;    index listed_by: account;    listed_date: timestamp = op_context.last_block_time;}
```

This entity represents an NFT that's listed on the marketplace. It has a set price for the NFT for sale and a date when it was listed. Next, we will add an operation where users can list an NFT card they own.

src/rell_marketplace/marketplace.rell
```rell
operation list_nft(id: integer, price: big_integer) {    val account = auth.authenticate();    val (nft, owner) = nft @ { .rowid == rowid(id) } ( $, .owner );    require(owner == account, "User must be owner of NFT");    create listed_nft ( nft, price, owner );}
```

What happens here is that the user sends a transaction to list_nft with the card NFT ID and the price.

- First, this operation requires the transaction to be signed by an FT4 account, which we verify using the authenticate method.

- Then, we query for the owner of the NFT, and we fetch the NFT entity.

- A check is done to make sure that the user listing the NFT is the owner of the NFT.

- If everything is okay, a new instance of listed_nft is created with NFT, price, and owner stored.

That's it. This way, users can list their NFTs on the marketplace. We can also add a query to ensure that we can fetch the listed NFTs.

src/rell_marketplace/queries.rell
```rell
enum card_sorting { NONE, PRICE_HIGH, PRICE_LOW }query get_cards(amount: integer, card_sorting)    = (listed_nft, nft_card) @* {    listed_nft.nft == nft_card.nft} (    @omit @sort_desc when (card_sorting) {          PRICE_HIGH -> .price;          PRICE_LOW -> -.price;          else -> nft_card.rowid.to_integer()        },    listed_nft_card_dto (        price = listed_nft.price,        id = nft_card.nft.rowid.to_integer(),        card = nft_card.to_struct()    )) limit amount;struct listed_nft_card_dto {    id: integer;    price: big_integer;    card: struct;}
```

Let's break this down a bit.

This query fetches NFTs listed on our marketplace. We can sort the result by price. The result is then structured using listed_nft_card_dto, where we specify which attributes to return from the result set.


===== FILE: courses__marketplace-course__module-assets__test-cli.md =====

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


===== FILE: courses__marketplace-course__module-assets__test-marketplace.md =====

# Test our marketplace

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Build a marketplace](/courses/marketplace-course/module-assets/)
- Lesson 4 - Test our marketplace
# Test our marketplace

Let's create a test to ensure that everything works as expected. We will build our test step by step using the Rell test framework. Start by opening src/rell_marketplace_test/blockchain_rell_marketplace_test.rell and adding the following imports.

src/rell_marketplace_test/blockchain_rell_marketplace_test.rell
```rell
@test module;import rell_marketplace.*;import lib.ft4.test.core.{ ft_auth_operation_for };import lib.ft4.assets.{ get_asset_balance };import lib.ft4.external.accounts.{ get_accounts_by_signer };
```

We also need to add a query to fetch last account and our NFTs to complete the test; this query will also be used later when we do testing using Chromia CLI.

src/rell_marketplace/queries.rell
```rell
query get_last_account() = account @ {} ( $, @omit @sort_desc .rowid ) limit 1;query get_all_nfts() {    return nft_card @* {} (        nft_card_dto(            strength = .strength,            health = .health,            owner_id = .nft.owner.id,            nft_id = .nft.rowid        )    );}struct nft_card_dto {    strength: integer;    health: integer;    owner_id: byte_array;    nft_id: rowid;}
```

Then we set up a test function named test_list_and_buy_nft, and we set up two test accounts and our economy_account.

src/rell_marketplace_test/blockchain_rell_marketplace_test.rell
```rell
function test_list_and_buy_nft() {    val alice = rell.test.keypairs.alice;    val trudy = rell.test.keypairs.trudy;    val economy_account = rell.test.keypairs.frank;    rell.test.tx()        .op(create_user(alice.pub)).sign(alice)        .run();    val alice_account = get_last_account();    rell.test.tx()        .op(create_user(trudy.pub)).sign(trudy)        .run();    val trudy_account = get_last_account();}
```

After Alice has bought a mystery card, we will add a test function to list it on the marketplace. We start by fetching the first (and only) card minted by our previous operation. Then, we call the list_nft operation and get Alice to sign it.

src/rell_marketplace_test/blockchain_rell_marketplace_test.rell
```rell
rell.test.tx()        .op(ft_auth_operation_for(alice.pub))        .op(buy_mystery_card()).sign(alice)        .run();    val nft = get_all_nfts()[0];    rell.test.tx()        .op(ft_auth_operation_for(alice.pub))        .op(list_nft(nft.nft_id.to_integer(), 20))        .sign(alice)        .run();
```

After the card has been successfully listed, we can let Trudy buy it from the marketplace at a set price.

src/rell_marketplace_test/blockchain_rell_marketplace_test.rell
```rell
rell.test.tx()        .op(ft_auth_operation_for(trudy.pub))        .op(buy_nft(nft.nft_id.to_integer()))        .sign(trudy)        .run();
```

That's it. We can now verify that the result is as expected by checking the account balances and the current owner of the minted NFT.

src/rell_marketplace_test/blockchain_rell_marketplace_test.rell
```rell
val nft_after_trade = get_all_nfts()[0];    assert_equals(get_asset_balance(alice_account, dapp_meta.asset), 920);    assert_equals(get_asset_balance(trudy_account, dapp_meta.asset), 980);    assert_equals(nft_after_trade.owner_id, trudy_account.id);
```

That completes our test, which we can run using the chr test command from the terminal. When the test runs successfully, we can be sure that our marketplace works as expected.

We will do a similar test in the next lesson using Chromia CLI.


===== FILE: courses__marketplace-course__module-ft4.md =====

# Module 1 - Register accounts and assets

URL: https://learn.chromia.com

- [Home](/)
- Module 1 - Register accounts and assets
# Module 1 - Register accounts and assets

For our marketplace, we need a payment token, and a couple of accounts. In this module we will guide you through the process using the FT4 libray.

## Lessons
[Lesson 1 - Register payment token](/courses/marketplace-course/module-ft4/register-token)[Lesson 2 - Create user accounts](/courses/marketplace-course/module-ft4/register-account)[Start module »](/courses/marketplace-course/module-ft4/register-token)


===== FILE: courses__marketplace-course__module-ft4__register-account.md =====

# Create user accounts

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Register accounts and assets](/courses/marketplace-course/module-ft4/)
- Lesson 2 - Create user accountsOn this page
# Create user accounts

For this course, we want to focus on building a marketplace, and token transfers are the main focus, so we will simplify
account creation a bit.

If you are interested in learning more about how to register accounts using EVM wallets, the process is described in
detail in our News feed dapp course, which you can find [here](/courses/my-news-feed/module-one/register-evm-accounts/).

## Configure FT4 account module​

To configure FT4 for accounts, we add the following configuration to the lib.ft4.core.accounts module.

chromia.yml
```yaml
blockchains:  rell_marketplace:    module: main    moduleArgs:      lib.ft4.core.accounts:        rate_limit:          max_points: 10          recovery_time: 5000          points_at_account_creation: 2compile:  rellVersion: 0.14.9database:  schema: schema_rell_marketplacetest:  modules:    - rell_marketplace_testlibs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

Setting rate-limit points_at_account_creation means that a user can at most perform 2 operations per time unit. The
recovery time for these points can be set using recovery_time property which defaults to 5000ms. We can also configure
the max_points an account can have or disable rate limiting altogether by setting active to false.

Start the node to verify if the configuration file is set up correctly by running the command chr node start and
querying the config using chr query ft4.get_config.

## Create a user account​

Now that our configurations are set, we can continue registering a user account.

We will do this by creating an operation which creates and account with 1000 tokens. Let's add some imports to
src/rell_marketplace/module.rell

src/rell_marketplace/module.rell
```rell
import lib.ft4.core.accounts.{ account, create_account_with_auth, single_sig_auth_descriptor };
```

and create a new file src/rell_marketplace/accounts.rell and add the following:

src/rell_marketplace/accounts.rell
```rell
operation create_user(pubkey) {    val account = create_account_with_auth(single_sig_auth_descriptor(pubkey, set(["A", "T"])));    Unsafe.mint(account, dapp_meta.asset, 1000);}
```

The operation creates an account for a specific pubkey and adds an auth descriptor with flags A and T. This means
the registered key can be used to perform administrator commands such as adding and removing other auth descriptors and
performing transfers. Then, the operation mints 1000 tokens on the account. Note this this is just for testing and
learning purpose, so we dont have to take any other security measures when it comes to creating tokens and accounts.

If you want to learn more how to handle auth descriptors and accounts, you can take the course
[Create a simple app on Chromia using Rell and React](/courses/my-news-feed/introduction).

Next, we will look at an NFT model, the asset we want to trade in our marketplace.


===== FILE: courses__marketplace-course__module-ft4__register-token.md =====

# Register payment token

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Register accounts and assets](/courses/marketplace-course/module-ft4/)
- Lesson 1 - Register payment token
# Register payment token

For our marketplace, we need to register and mint a payment token, which will be used to buy and sell cards.

We first register an asset we will use as a payment token using the FT4 library, which has built-in operations for registering assets.

Let's create an asset with the following parameters:

- Name: Collector Card

- Symbol: CRD

- Decimals: 6

- URL to icon: [https://url-to-asset-icon](https://url-to-asset-icon)

This registration should occur immediately upon deploying our blockchain. To achieve this, we utilize an object that functions as a global singleton within our dapp. This object is initialized at the launch of the blockchain, ensuring the asset's registration is executed at startup.

src/rell_marketplace/module.rell
```rell
module;import lib.ft4.assets.{ asset, Unsafe };object dapp_meta {    asset = Unsafe.register_asset("Collector Card", "CRD", 6, chain_context.blockchain_rid, "https://url-to-asset-icon");}
```

Now we can use dapp_meta.asset from within the dapp to access the asset when doing transfers.


===== FILE: courses__marketplace-course__module-nft.md =====

# Module 2 - Build NFT model in Rell

URL: https://learn.chromia.com

- [Home](/)
- Module 2 - Build NFT model in Rell
# Module 2 - Build NFT model in Rell

In this module, we will build our NFT model using Rell. It will have capability to handle ownership and attributes that define the NFT and give it properties.

Also, we will look at how we can mint new NFTs and provide a quick look at how to generate randomness on the Chromia blockchain.

## Lessons
[Lesson 1 - Define the NFT model](/courses/marketplace-course/module-nft/nft)[Lesson 2 - Mint NFTs](/courses/marketplace-course/module-nft/mint-nfts)[Lesson 3 - Add randomness to the card](/courses/marketplace-course/module-nft/randomness)[Start module »](/courses/marketplace-course/module-nft/nft)


===== FILE: courses__marketplace-course__module-nft__mint-nfts.md =====

# Mint NFTs

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Build NFT model in Rell](/courses/marketplace-course/module-nft/)
- Lesson 2 - Mint NFTs
# Mint NFTs

With our NFT model in place, we can examine how to mint new NFTs.

# Minting

We will set up operations where users can buy random cards in the marketplace. This will result in the minting of new cards with attributes defined by our game rules in combination with a random element. So, let's add a function to mint a new card.

We start by creating a nft_card and setting fixed properties. We will change these to be calculated later, but we want to focus on creating the NFT for now. Let's add the following function:

src/rell_marketplace/nft.rell
```rell
function mint_card(account) {    val nft = create nft(account);    val health = 10;    val strength = 8;    create nft_card(.nft = nft, .strength = strength, .health = health);}
```

What happens here is that we first create an instance of nft registered to an account.

We then declare our card attributes and create the specific gaming card nft_card, where we pass our fixed attributes.

Now that we've a minting process, we can start with the marketplace. To make the authentication work in our dapp, we need to register an auth_handler. An auth handler is a function which describes the rules for which any call to auth.authenticate function should follow. Lets add the following handler:

src/rell_marketplace/module.rell
```rell
import lib.ft4.auth;@extend(auth.auth_handler)function () = auth.add_auth_handler(  flags = ["T"]);
```

This handler says that all operations within the marketplace module should require the T flag, which is what we used when we registered the account. One can also set a scope on the handler, if the handler should only work for a specific operation, but we will make this a global handler. All operations that uses ft4 authentication will use this handler as a fallback.

We first need a way to buy a new card, so let's add an operation for this in a new file src/rell_marketplace/marketplace.rell:

src/rell_marketplace/marketplace.rell
```rell
operation buy_mystery_card() {    val account = auth.authenticate();    mint_card(account);}
```

We will use the FT4 function auth.authenticate() to verify if the user has permission to perform the operation. If authentication is successful, we retrieve the account and use it to mint a new card.

Next up, we will add some randomness to our generated card to add some flavor to them.


===== FILE: courses__marketplace-course__module-nft__nft.md =====

# Define the NFT model

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Build NFT model in Rell](/courses/marketplace-course/module-nft/)
- Lesson 1 - Define the NFT model
# Define the NFT model

We will fully define our own NFT model on-chain and build it in Rell for this project.

We'll start by defining the base entity nft, whose purpose is to represent an NFT with a unique ID and a mutable attribute for the owner of the NFT. Lets create a new file src/rell_marketplace/nft.rell and add the following:

src/rell_marketplace/nft.rell
```rell
entity nft {    key id: byte_array = op_context.transaction.tx_rid;    mutable owner: account;    index owner;}
```

This definition has an id property set to the Transaction RID of a minted NFT. There is also a mutable property owner typed as an account. This means that we can change the account owner of the NFT and, in practice, transfer the NFT to a new account.

The entity account is included in the FT4 lib, representing an account registered in FT4. You can read more about FT4 accounts [here](https://docs.chromia.com/ft4/intro).

We also have an index on the owner to optimize performance when looking up owners of NFTs. Note that this will make the operation of changing owner heavier. With this base entity in place, we can create our Gaming Trade Card NFT, adding attributes specific to our NFT.

src/rell_marketplace/nft.rell
```rell
entity nft_card {    key nft;    strength: integer;    health: integer;}
```

Let's break it down

- 
The property key nft creates a relation to our NFT entity with a unique constraint.

- 
The strength and health properties give our cards unique attributes.

That's it. We have created a simple NFT model in just a few steps, and this is a diagram showing our entities.


===== FILE: courses__marketplace-course__module-nft__randomness.md =====

# Add randomness to the card

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Build NFT model in Rell](/courses/marketplace-course/module-nft/)
- Lesson 3 - Add randomness to the cardOn this page
# Add randomness to the card

Generating true randomness on the blockchain is challenging because blockchains are natural deterministic systems designed to ensure that operations can be replicated across all nodes. This means that every transaction or function call must produce the same result on every node, which contradicts the unpredictable nature of true randomness.

However, there are ways to simulate randomness using information from block creation. This isn't random but can be enough for simple use cases. This lesson is a bit of a sidetrack. If you want to continue focusing on the marketplace, skip this part and work with fixed attributes for our cards.

### Add random functions​

Let's add random strength and health to our buy_mystery_card operation by first creating a function which generates a pseudo-random number:

src/rell_marketplace/nft.rell
```rell
// Generate a random integer within a specified rangefunction random(high: integer, seed: integer): integer {    // Ensure high is not zero to avoid division by zero    if (high == 0) return 0;    // Calculate the random value using the provided seed    return (op_context.last_block_time - op_context.block_height - op_context.op_index + seed) % high;}
```

We take the Transaction RID to generate a seed and then substring the seed for each random number we need. These seeds are then used to generate random health and strength.

src/rell_marketplace/nft.rell
```rell
function extract_seeds() {    // Extract seeds for randomness from the transaction RID    val seeds = op_context.transaction.tx_rid.to_hex();    val health_seed: integer = integer.from_hex(seeds.sub(0, 4));    val strength_seed: integer = integer.from_hex(seeds.sub(4, 8));    return (health_seed, strength_seed);}
```

With our seed and random functions in place, we add two functions to generate random health and strength. The generate_health and generate_strength functions utilize the extracted seeds to produce random values for health and strength, respectively.

src/rell_marketplace/nft.rell
```rell
// Generate random health within a specified rangefunction generate_health(seed: integer): integer {    val rand = random(30, seed) + 1; // Generate a random value between 1 and 30    val baseline = 30; // Consistent baseline for all cards    return baseline + rand;}// Generate random strength within a specified rangefunction generate_strength(seed: integer): integer {    val rand = random(30, seed) + 1; // Generate a random value between 1 and 30    val baseline = 50; // Consistent baseline for all cards    return baseline + rand * 2; // Adjust range to 50-110}
```

The mint_card function creates a new NFT card with random attributes (strength and health) using the generated values.

src/rell_marketplace/nft.rell
```rell
// Mint a new card with random attributesfunction mint_card(account) {    // Create a new NFT for the specified account    val nft = create nft(account);    // Extract seeds for randomness    val (health_seed, strength_seed) = extract_seeds();    // Generate random health and strength based on the provided seeds    val health = generate_health(health_seed);    val strength = generate_strength(strength_seed);    // Create the NFT card with the generated attributes    create nft_card(        .nft = nft,        .strength = strength,        .health = health    );}
```


===== FILE: courses__marketplace-course__setup.md =====

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

## Install​

- Mac
- Linux
- Docker
- Windows

- 
Install Homebrew: [Homebrew installation guide](https://brew.sh/)

- 
Install PostgreSQL:

```shell
brew install postgresql@16brew services start postgresql@16createuser -s postgres
```

- 
Prepare the PostgreSQL database:

```plsql
psql -U postgres -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8';" -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

noteIf you get an error saying peer authentication failed, you must change the authentication method from peer to
md5. You can change it in the pg_hba.conf file of your psql database.

- 
Install PostgreSQL:

```bash
sudo apt install postgresql-16
```

- 
Prepare the PostgreSQL database:

```bash
sudo -u postgres psql -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8';" -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

- 
Install Docker: [Docker installation guide](https://docs.docker.com/engine/install/)

- 
Prepare the PostgreSQL database:

```dockerfile
docker run --name postgres -e POSTGRES_USER=postchain -e POSTGRES_PASSWORD=postchain -p 5432:5432 -d postgres:16.3-alpine3.20
```

noteWe use the Alpine version of PostgreSQL because it provides the correct collation settings by default. This can be
explicitly set using the environment variable:

```bash
POSTGRES_INITDB_ARGS="--lc-collate=C.UTF-8 --lc-ctype=C.UTF-8 --encoding=UTF-8"
```

- 
Download the PostgreSQL installer from the [official website](https://www.postgresql.org/download/windows/).

- 
Install the executable and add the PostgreSQL folder containing the binaries to your environment variables. Open the
Command Prompt (CMD) and run the following command, ensuring that you set the path to your binaries correctly and
replace the <version> placeholder with the actual version number:

```shell
setx POSTGRESQL "C:\Program Files\PostgreSQL\\bin"
```

- 
Reopen the Command Prompt (CMD) to update the environment variables. Prepare the PostgreSQL database by running the
following two commands sequentially:

```plsql
psql -U postgres -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' ENCODING 'UTF-8';"psql -U postgres -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

noteDepending on your Windows version, you may encounter various errors related to permissions or incorrect PostgreSQL
installations. If this happens, we recommend installing Docker and deploying the PostgreSQL container.

Install Chromia CLI
# Install Chromia CLI

This topic contains instructions to install and update the
[Chromia CLI](https://gitlab.com/chromaway/core-tools/chromia-cli).

## Prerequisite​

Before proceeding, make sure the following prerequisites are met:

- PostgreSQL database: See [Set up PostgreSQL database](/docs/install/database-setup).

- RELL_JAVA environment: Chromia CLI do requries a java runtime (version 21 or later) to execute. Through the
different package managers this has been abstracted away for you so you don't need to set this up. If you want to
control which java runtime you use to execute Chromia CLI with it is recomended to set RELL_JAVA variable in your
environment to point to a valid Java installation

## Installation​

You can install Chromia CLI using a package manager or by downloading it directly from
[Chromia CLI Packages](https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages).

- macOS
- Linux/WSL
- WindowsTo install Chromia CLI (chr) on macOS, follow these steps:

- 
If Homebrew is not installed, install it by running:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

- 
Add the Chromia repository to Homebrew by running the following command:

```bash
brew tap chromia/core https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
```

- 
Install Chromia CLI with:

```bash
brew install chromia/core/chr
```

infoTo install a specific version of Chromia CLI, use the following commands:

```shell
brew install chromia/core/chr@brew unlink chrbrew link chr@
```
You can list available versions using: brew search chr.

- 
To verify the installation, check the version by running:

```bash
chr --version
```

To install Chromia CLI (chr) on Linux or WSL (Windows Subsystem for Linux), follow these steps:

- 
Download and add Chromia's apt-repo public key to your system’s trusted keyrings:

```bash
curl -fsSL https://apt.chromia.com/chromia.gpg | sudo tee /usr/share/keyrings/chromia.gpg
```

- 
Add the Chromia repository to your list of package sources:

```bash
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/chromia.gpg] https://apt.chromia.com stable main" | sudo tee /etc/apt/sources.list.d/chromia.list
```

- 
Run the following command to update your package sources:

```bash
sudo apt-get update
```

infoIf you added apt.chromia.com before Chromia CLI version 0.16.0, run the following command to allow the repository
update:

```bash
sudo apt-get --allow-releaseinfo-change update
```

- 
Once the repository is updated, install Chromia CLI by running:

```bash
sudo apt-get install chr
```

- 
To verify that Chromia CLI is installed successfully, check the version:

```bash
chr --version
```

To install Chromia CLI (chr) on Windows using [Scoop](https://scoop.sh/), follow these steps:

- 
If Scoop is not installed, install it by running the following command in PowerShell (run as Administrator):

```powershell
iwr -useb get.scoop.sh | iex
```

- 
Add the Chromia repository (bucket) to Scoop by running:

```powershell
scoop bucket add chromia https://gitlab.com/chromaway/core-tools/scoop-chromia/
```

- 
Add the Java bucket to Scoop by running:

```powershell
scoop bucket add java
```

This will enable scoop to download the openjdk21 which chromia-cli depends on when installing

- 
Install Chromia CLI by running:

```powershell
scoop install chr
```

- 
To verify that Chromia CLI is installed successfully, check the version:

```powershell
chr --version
```

## Updating Chromia CLI​

You can download and install the latest Chromia CLI from
[here](https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages), or if you have installed the Chromia CLI via a
package manager, you can update it with the following:

- macOS
- Linux
- Windows
```shell
brew updatebrew upgrade chr
```

```shell
sudo apt-get updatesudo apt-get install chr
```

```shell
scoop updatescoop update chr
```

## Docker​

Docker can run a standalone Linux container with the Chromia CLI pre-installed. Make sure that you have set up the
[PostgreSQL database](/docs/install/database-setup).

To use the published Docker images, you must first have Docker installed and configured on your host machine. Please
refer to the Docker documentation on how to [install Docker](https://docs.docker.com/get-docker/) on Windows, Mac, and
Linux.

### Start the Docker container with Chromia CLI pre-installed​

To run the latest version of the Chromia CLI, use the docker run command and specify the CLI Docker image name and
chr.

```shell
docker run --rm -v $(pwd):/usr/app registry.gitlab.com/chromaway/core-tools/chromia-cli/chr: chr
```

Windows Command Prompt UsersIf you're using Windows Command Prompt (cmd), the $(pwd) syntax will not work. Use one of these alternatives:

- PowerShell: Use $(Get-Location) or $(pwd)

- Command Prompt: Use %CD%
Example for Command Prompt:

```cmd
docker run --rm -v %CD%:/usr/app registry.gitlab.com/chromaway/core-tools/chromia-cli/chr: chr
```

noteMake sure to configure your chromia.yml file correctly:

- Mac: Use host.docker.internal for database:host.

- Windows: Set database:host to 172.17.0.1.

- Linux: Use the --network=host argument in Docker commands.
These configurations are crucial to ensure connectivity between Chromia CLI and the PostgreSQL instance.

See the [Docker command line reference](https://docs.docker.com/engine/reference/commandline/docker/) for more
information on updating or uninstalling the Docker image.

```bash
#!/bin/bash# Allocate a pseudo-TTY one when run in interactive modeif [ -t 0 ] && [ -t 1 ] ; then TTY="--tty"; else TTY=""; fidocker run \  # Sets the network to host to not need to change the database hostname (linux only)  --network=host \  # Set timezone based on system settings (linux only)  -e TZ=$(cat /etc/timezone) \  # Sets process ownership to current user  --user $(id -u):$(id -g) \  --mount type=bind,source="/etc/passwd",target=/etc/passwd,readonly \  --mount type=bind,source="/etc/group",target=/etc/group,readonly \  # Configures ssh-agent (only needed if chr install is called on non-public repositores)  -e SSH_AUTH_SOCK=$SSH_AUTH_SOCK \  --volume "$SSH_AUTH_SOCK:$SSH_AUTH_SOCK" \  --mount type=bind,source="${HOME}/.ssh",target=${HOME}/.ssh,readonly \  --mount type=bind,source="${HOME}/.config/jgit",target=${HOME}/.config/jgit \  # Mounts current folder into the container (Use `Get-Location` on PowerShell)  --mount type=bind,source="$(pwd)",target=/usr/app \  --interactive ${TTY} \  --rm \  registry.gitlab.com/chromaway/core-tools/chromia-cli/chr:${CHR_VERSION:-latest} chr "$@"
```

Windows Command Prompt UsersThis bash script uses $(pwd) which won't work in Windows Command Prompt. For Windows users:

- PowerShell: The script should work as-is

- Command Prompt: Replace $(pwd) with %CD% in the mount command
Modified mount line for Command Prompt:

```cmd
--mount type=bind,source="%CD%",target=/usr/app \
```

We will start by setting up a new project using Chromia CLI:

```shell
chr create-rell-dapp rell_marketplace --template=plain-multi
```

```shell
cd rell_marketplace
```

To finalize the setup, we add the FT4 library to our project. Open your chromia.yml file and add ft4 to a new libs
section and configure the public key as the admin key for our dapp:

chromia.yml
```yaml
blockchains:  rell_marketplace:    module: maincompile:  rellVersion: 0.14.9database:  schema: schema_rell_marketplacetest:  modules:    - rell_marketplace_testlibs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

Delete the src/rell_marketplace_test directory.

To install FT4, we run:

```shell
chr install
```

That's the basic setup for using FT4. In the upcoming lessons, we will use this to create assets, transfer tokens, and
register user accounts.
