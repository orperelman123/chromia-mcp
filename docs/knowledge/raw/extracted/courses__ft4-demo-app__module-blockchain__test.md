# Testing the Asset Management System

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Blockchain dapp](/courses/ft4-demo-app/module-blockchain/)
- Lesson 4 - TestOn this page
# Testing the Asset Management System

In this final lesson, we'll look into comprehensive testing of our asset management system. We'll look at different test scenarios and how to verify system behavior.

## Test Setup​

Our test module imports all necessary components:

src/test/module.rell
```rell
@test module;import lib.ft4.core.accounts.strategies.open.{ ras_open };import lib.ft4.external.accounts. { get_accounts_by_signer };import lib.ft4.external.accounts.strategies.{ register_account };import lib.ft4.external.assets. { get_asset_balance, get_assets_by_name, transfer };import lib.ft4.test.core. { ft_auth_operation_for, create_auth_descriptor };import lib.ft4.utils. { paged_result };import ^.main. { register_and_mint_asset };
```

## Structuring​

To retrieve account information, we've defined helper functions in our utils:

src/test/utils.rell
```rell
struct account_dto {    id: byte_array;    type: text;}struct asset_dto {    id: byte_array;    name: text;    symbol: text;    decimals: integer;    blockchain_rid: byte_array;    icon_url: text;    type: text;    supply: big_integer;}function account_from_gtv(paged_result) = account_dto.from_gtv_pretty(paged_result.data[0]);function asset_from_gtv(paged_result) = asset_dto.from_gtv_pretty(paged_result.data[0]);
```

These utilities help us work with account data in a structured way throughout our testing process.

## Test Cases​

### 1. Asset Registration Test​

This test verifies the functionality of creating an account and registering a new asset. It ensures that an asset can be successfully created and that the system accurately reflects this in the asset registry.

src/test/asset_management_test.rell
```rell
function test_create_account_and_register_and_mint_asset() {    val alice = rell.test.keypairs.alice;    val trudy = rell.test.keypairs.trudy;    val required_asset = (        name = "TestAsset1",        id = x"A85755C27F76B25C4139C929861E81C001C8C449F0260A9132F8ECFEA9075C39", //(asset_name, chain_context.blockchain_rid).hash();        symbol = "TST1",        decimals = 10,        blockchain_rid = x"0000000000000000000000000000000000000000000000000000000000000000",        icon_url = "https://url-to-asset-1-icon",        type = "ft4",        supply = 100L,    );    val auth_descriptor_alice = create_auth_descriptor(alice.pub, ["A", "T"], null.to_gtv());    val auth_descriptor_trudy = create_auth_descriptor(trudy.pub, ["A", "T"], null.to_gtv());    rell.test.tx()        .op(ras_open(auth_descriptor_alice))        .op(register_account())                .sign(alice)        .run();    rell.test.tx()        .op(ras_open(auth_descriptor_trudy))        .op(register_account()).sign(trudy)        .run();    rell.test.tx()        .op(ft_auth_operation_for(alice.pub))        .op(            register_and_mint_asset(                required_asset.name,                required_asset.symbol,                required_asset.decimals,                required_asset.supply,                required_asset.icon_url            )        )        .sign(alice)        .run();    //query    val asset_gtv = get_assets_by_name(required_asset.name, 10, null);    assert_equals(asset_gtv.data[0], required_asset.to_gtv_pretty());    val asset_data = asset_from_gtv(asset_gtv);    val account_alice = account_from_gtv(get_accounts_by_signer(alice.pub, 10, null));    val account_trudy = account_from_gtv(get_accounts_by_signer(trudy.pub, 10, null));    val balance_alice = get_asset_balance(account_alice.id, asset_data.id);    assert_equals(balance_alice?.amount, required_asset.supply);}
```

### 2. Multiple Asset Registration Test​

This test ensures that an account cannot register more than one asset, validating the one-asset-per-account restriction. The first asset registration should succeed, while the second attempt to register a different asset should fail.

src/test/asset_management_test.rell
```rell
function test_create_account_and_register_and_mint_2_assets_must_fail() {    val alice = rell.test.keypairs.alice;    val trudy = rell.test.keypairs.trudy;    val required_asset_1 = (        name = "TestAsset1",        symbol = "TST1",        decimals = 10,        icon_url = "https://url-to-asset-1-icon",        type = "ft4",        supply = 100L,    );    val required_asset_2 = (        name = "TestAsset2",        symbol = "TST2",        decimals = 10,        icon_url = "https://url-to-asset-1-icon",        type = "ft4",        supply = 100L,    );    val auth_descriptor_alice = create_auth_descriptor(alice.pub, ["A", "T"], null.to_gtv());    val auth_descriptor_trudy = create_auth_descriptor(trudy.pub, ["A", "T"], null.to_gtv());    rell.test.tx()        .op(ras_open(auth_descriptor_alice))        .op(register_account()).sign(alice)        .run();    rell.test.tx()        .op(ras_open(auth_descriptor_trudy))        .op(register_account()).sign(trudy)        .run();    rell.test.tx()        .op(ft_auth_operation_for(alice.pub))        .op(            register_and_mint_asset(                required_asset_1.name,                required_asset_1.symbol,                required_asset_1.decimals,                required_asset_1.supply,                required_asset_1.icon_url            )        ).sign(alice)        .run();    rell.test.tx()        .op(ft_auth_operation_for(alice.pub))        .op(            register_and_mint_asset(                required_asset_2.name,                required_asset_2.symbol,                required_asset_2.decimals,                required_asset_2.supply,                required_asset_2.icon_url            )        ).sign(alice)        .run_must_fail();}
```

### Transfer Test​

The FT4 library provides a transfer operation that we can use directly. Here's how we test asset transfers:

src/test/asset_management_test.rell
```rell
function test_create_account_and_register_and_mint_and_transfer_asset() {    val alice = rell.test.keypairs.alice;    val trudy = rell.test.keypairs.trudy;    val asset_name = "TestAsset1";    val asset_amount = 100;    val asset_amount_to_transfer = 20;    val required_asset = (        name = "TestAsset1",        symbol = "TST1",        decimals = 10,        icon_url = "https://url-to-asset-1-icon",        type = "ft4",        supply = 100L,    );    val auth_descriptor = create_auth_descriptor(alice.pub, ["A", "T"], null.to_gtv());    val auth_descriptor_trudy = create_auth_descriptor(trudy.pub, ["A", "T"], null.to_gtv());    rell.test.tx()        .op(ras_open(auth_descriptor))        .op(register_account()).sign(alice)        .run();    rell.test.tx()        .op(ras_open(auth_descriptor_trudy))        .op(register_account()).sign(trudy)        .run();    rell.test.tx()        .op(ft_auth_operation_for(alice.pub))        .op(            register_and_mint_asset(                required_asset.name,                required_asset.symbol,                required_asset.decimals,                required_asset.supply,                required_asset.icon_url            )        ).sign(alice)        .run();    //query    val asset_data = asset_from_gtv(get_assets_by_name(required_asset.name, 10, null));    val account_alice = account_from_gtv(get_accounts_by_signer(alice.pub, 10, null));    val account_trudy = account_from_gtv(get_accounts_by_signer(trudy.pub, 10, null));    rell.test.tx()        .op(ft_auth_operation_for(alice.pub))        .op(transfer(account_trudy.id, asset_data.id, asset_amount_to_transfer)).sign(alice)        .run();    val alice_balance = get_asset_balance(account_alice.id, asset_data.id);    assert_equals(alice_balance?.amount, asset_amount - asset_amount_to_transfer);    val trudy_balance = get_asset_balance(account_trudy.id, asset_data.id);    assert_equals(trudy_balance?.amount, asset_amount_to_transfer);}
```

The transfer process involves several key steps:

- Account Setup: Both sender and receiver must have valid accounts

- Asset Registration: The asset must be registered and minted

- Transfer Execution: Using FT4's transfer operation

- Balance Verification: Checking that balances are updated correctly

## Running Tests​

To run all tests, use the Chromia CLI:

```bash
chr test
```
