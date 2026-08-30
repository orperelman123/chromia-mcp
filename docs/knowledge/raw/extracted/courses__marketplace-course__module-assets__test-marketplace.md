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
