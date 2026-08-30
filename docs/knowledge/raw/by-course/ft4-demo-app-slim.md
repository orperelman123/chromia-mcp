# ft4-demo-app

===== FILE: courses__ft4-demo-app__introduction.md =====


# Build an asset management system with FT4

URL: https://learn.chromia.com

- [Home](/)
- Course OverviewOn this page
# Build an asset management system with FT4

In this course, we will see how to build a digital asset management system using Chromia's FT4 token standard. You'll learn how to create, manage, and transfer digital assets while implementing account management functionality.

What we will cover:

- Setting up an FT4-based project

- Creating and managing user accounts

- Registering and minting digital assets

- Implementing asset transfers between accounts

- Fatching user balance

- Conduct comprehensive testing of the asset management system.

The FT4 library provides a robust foundation for handling accounts and assets on Chromia. We'll explore its key features while building a practical asset management application.

The complete code repository for this course is available here: [FT4 Demo App Repository](https://bitbucket.org/chromawallet/dapp-templates/src/main/asset_management/).

### Related materials​

This course relies on the following documentation, which can help you understand the underlying concepts and approaches:

| 
| Section| Type| Documentation
| FT4| Introduction| [FT4](https://docs.chromia.com/ft4/intro)
| FT4| Accounts| [Account management](https://docs.chromia.com/ft4/account-management/)
| FT4| Assets| [Asset management](https://docs.chromia.com/ft4/asset-management/)
| Overview| Dapps| [Building your dapps on Chromia](https://docs.chromia.com/intro/dapp)


===== FILE: courses__ft4-demo-app__module-blockchain.md =====


# Module 2 - Blockchain dapp

URL: https://learn.chromia.com

- [Home](/)
- Module 2 - Blockchain dapp
# Module 2 - Blockchain dapp

This course will guide you to explore the blockchain application of the asset management system.

In this course, you will learn how to:

- Basic interaction or rell dapp.

- Creating operations and queries.

- Working with accounts and assets.

- Implementing tests.

## Lessons
[Lesson 1 - Configure the Blockchain dapp](/courses/ft4-demo-app/module-blockchain/setup)[Lesson 2 - Account Management](/courses/ft4-demo-app/module-blockchain/account-management)[Lesson 3 - Asset Registration](/courses/ft4-demo-app/module-blockchain/asset-registration)[Lesson 4 - Test](/courses/ft4-demo-app/module-blockchain/test)[Start module »](/courses/ft4-demo-app/module-blockchain/setup)


===== FILE: courses__ft4-demo-app__module-blockchain__account-management.md =====


# Account management

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Blockchain dapp](/courses/ft4-demo-app/module-blockchain/)
- Lesson 2 - Account ManagementOn this page
# Account management

In this lesson, we'll explore how to implement account management functionality using FT4. We'll see how to create operations for registering new accounts and managing account permissions.

## Account authentication​

Authentication handler in main module defines what permissions are required for different operations.

The Open Strategy is used for account registration. The Open Strategy allows anyone to call the register_account() operation to create an account without any restrictions. To enable this, we need to import the required module in the main file import lib.ft4.accounts.strategies.open, this ensures the Open Strategy is properly integrated into the account registration process.

src/main.rell
```rell
module;import lib.ft4.auth;import lib.ft4.accounts.strategies.open;import lib.ft4.core.accounts.{ account, Account, create_account_with_auth, single_sig_auth_descriptor };@extend(auth.auth_handler)function () = auth.add_auth_handler(    flags = ["T"]);
```

Auth descriptors specify who can access an account and what they are allowed to do. The T flag indicates that accounts will have transfer permissions. This is essential for our asset management system. You can learn more about this here: [FT4 accounts overview](https://docs.chromia.com/ft4/intro). You can learn more about all the available strategies and their implementation methods at the following link: [Account Registration Framework - Chromia Docs](https://docs.chromia.com/ft4/backend/accounts/overview#account-registration-framework).


===== FILE: courses__ft4-demo-app__module-blockchain__asset-registration.md =====


# Asset Registration and Minting

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Blockchain dapp](/courses/ft4-demo-app/module-blockchain/)
- Lesson 3 - Asset RegistrationOn this page
# Asset Registration and Minting

In this lesson, we'll explore asset registration and minting functionality. Our system allows each account to register one digital asset and mint an initial supply.

## Asset Registration Operation​

Here's our operation for registering and minting new assets:

src/main.rell
```rell
import lib.ft4.external.assets.{ get_asset_balances };import lib.ft4.assets.{ asset, Unsafe, balance };operation register_and_mint_asset(    asset_name: text,    symbol: text,    decimals: integer,    amount: big_integer,    icon_url: text) {    val owner_account = auth.authenticate();    require(get_asset_balances(owner_account.id, 10, null).data.empty(), "One asset allowed");    functions.register_and_mint_asset(owner_account, asset_name, symbol, decimals, amount, icon_url);}
```

The key features of this operation:

- Authenticates the account owner

- Ensures one asset per account limit

- Registers the asset and mints initial supply

The actual registration and minting logic is handled in the functions [namespace](https://docs.chromia.com/rell/language-features/modules/namespace):

src/main.rell
```rell
namespace functions {    function register_and_mint_asset(        owner_account: account,        asset_name: name,        symbol: text,        decimals: integer,        amount: big_integer,        icon_url: text    ) {        // Derive asset ID        val asset_id = (asset_name, chain_context.blockchain_rid).hash();        // Check if asset exists        val asset = asset @ ? { .id == asset_id };        if (not empty(asset)) return;        // Register and mint        val asset_created = Unsafe.register_asset(asset_name, symbol, decimals, chain_context.blockchain_rid, icon_url);        Unsafe.mint(owner_account, asset_created, amount);    }}
```

This implementation:

- Generates a unique asset ID

- Checks for existing assets

- Registers the new asset

- Mints the initial supply to the owner's account


===== FILE: courses__ft4-demo-app__module-blockchain__setup.md =====


# Lesson 1 - Configure the Blockchain dapp

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Blockchain dapp](/courses/ft4-demo-app/module-blockchain/)
- Lesson 1 - Configure the Blockchain dapp
# Lesson 1 - Configure the Blockchain dapp

- You can spot chromia.yml file to have a FT4 library as a dependency. Update this file with the following configuration:

chromia.yml
```yaml
blockchains:  asset_management:    module: main    moduleArgs:      main:        basic: 5      lib.ft4.core.admin:        admin_pubkey: x"023BEE5A479CE5AF31F6F64EDE7BEAD394E92E4D973E1727782DB577A55E878563"compile:  rellVersion: 0.14.9  source: rell/srcdatabase:  schema: schema_asset_managementtest:  modules:    - test  moduleArgs:    lib.ft4.core.admin:      admin_pubkey: x"023BEE5A479CE5AF31F6F64EDE7BEAD394E92E4D973E1727782DB577A55E878563"libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

- Run chr install to download the FT4 library.

This configuration sets up your project with the FT4 library and necessary dependencies for building the asset management system.


===== FILE: courses__ft4-demo-app__module-blockchain__test.md =====


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


===== FILE: courses__ft4-demo-app__module-frontend-application.md =====


# Module 3 - Frontend application

URL: https://learn.chromia.com

- [Home](/)
- Module 3 - Frontend application
# Module 3 - Frontend application

This course will guide you to explore the frontend application of the asset management system.

In this course, you will learn how to:

- The essential tools and concepts required to interact with the FT4 token standard.

- Basic interaction with the FT4 token standard.

- Creating an account.

- Register and mint digital assets.

- Implement asset transfers between accounts.

## Lessons
[Lesson 1 - Set up the Frontend Application](/courses/ft4-demo-app/module-frontend-application/setup)[Lesson 2 - Chromia tools](/courses/ft4-demo-app/module-frontend-application/tools)[Lesson 3 - Account Registration](/courses/ft4-demo-app/module-frontend-application/account-regisration)[Lesson 4 - Register and Mint](/courses/ft4-demo-app/module-frontend-application/register-and-mint)[Lesson 5 - Burn Tokens](/courses/ft4-demo-app/module-frontend-application/burn)[Lesson 6 - Transfer asset](/courses/ft4-demo-app/module-frontend-application/transfer)[Lesson 7 - Deploy onchain](/courses/ft4-demo-app/module-frontend-application/deploy-onchain)[Start module »](/courses/ft4-demo-app/module-frontend-application/setup)


===== FILE: courses__ft4-demo-app__module-frontend-application__account-regisration.md =====


# Lesson 3 - Account Registration

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Frontend application](/courses/ft4-demo-app/module-frontend-application/)
- Lesson 3 - Account Registration
# Lesson 3 - Account Registration

In order to manage assets, you need an account. In this lesson, we will overview how the account creation process handled in the asset management template.

FT4 offers registerAccount operation to create an account.

asset_management/src/hooks/chromia-hooks.ts
```tsx
import { useCallback, useState } from "react";import {  AuthFlag,  createSingleSigAuthDescriptorRegistration,  registerAccount,  registrationStrategy,} from "@chromia/ft4";import {  useEvmKeyStore,  useFtAccounts,  usePostchainClient,} from "@chromia/react";import { IClient } from "postchain-client";import { useAccount } from "wagmi";import { publicClientConfig as clientConfig } from "@/utils/generate-client-config";export const useChromiaAccount = ({  onAccountCreated,}: {  onAccountCreated?: () => void;} = {}) => {  const [isLoading, setIsLoading] = useState(false);  const [tried, setTried] = useState(false);  const { address: ethAddress } = useAccount();  const { data: client } = usePostchainClient({ config: clientConfig });  const { data: keyStore } = useEvmKeyStore();  const { mutate, data: ftAccounts } = useFtAccounts({ clientConfig });  const createAccount = useCallback(async () => {    try {      setIsLoading(true);      if (!ethAddress || !keyStore || !client) return;      const ad = createSingleSigAuthDescriptorRegistration(        [AuthFlag.Account, AuthFlag.Transfer],        keyStore.id,      );      await registerAccount(        client as IClient,        keyStore,        registrationStrategy.open(ad),      );      await mutate();      onAccountCreated?.();    } catch (e) {      console.error(e);    } finally {      setIsLoading(false);      setTried(true);    }  }, [client, ethAddress, keyStore, mutate, onAccountCreated]);  return {    createAccount,    isLoading,    tried,    account: ftAccounts?.[0],    hasAccount: !!ftAccounts?.length,  };};
```


===== FILE: courses__ft4-demo-app__module-frontend-application__burn.md =====


# Lesson 5 - Burn Tokens

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Frontend application](/courses/ft4-demo-app/module-frontend-application/)
- Lesson 5 - Burn Tokens
# Lesson 5 - Burn Tokens

The FT4 session object provides a method to burn tokens. The burn method is used to burn tokens from the account.

asset_management/src/hooks/token-hooks.ts
```tsx
import { useFtSession } from "@chromia/react";import { publicClientConfig as clientConfig } from "@/utils/generate-client-config";interface BurnTokenParams {  ticker: string;  name: string;  amount: number;}function useBurnTokens() {  const { data: ftAccounts } = useFtAccounts({ clientConfig });  const { data: session } = useFtSession(    ftAccounts?.length ? { clientConfig, account: ftAccounts[0] } : null,  );  const burnTokens = useCallback(    async (token: BurnTokenParams) => {      if (!session) return;      try {        await session.account.burn(token.ticker, createAmount(token.amount));        onSuccess?.(token);      } catch (error) {        console.error(error);        onError?.(token);      }    },    [session],  );  return burnTokens;}
```

In the code snippet above, the burn method is called on the session object's account property to burn the specified amount of tokens.


===== FILE: courses__ft4-demo-app__module-frontend-application__deploy-onchain.md =====


# Lesson 7 - Deploy onchain

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Frontend application](/courses/ft4-demo-app/module-frontend-application/)
- Lesson 7 - Deploy onchain
# Lesson 7 - Deploy onchain

The Frontend Application can be deployed in 2 ways:

- The traditional way hosted on any server. Pleas follow the package.json scripts in the project.

- On chain of the dapp node. The [instructions](https://docs.chromia.com/intro/deployment/frontend-application/deploy-on-chain).


===== FILE: courses__ft4-demo-app__module-frontend-application__register-and-mint.md =====


# Lesson 4 - Register and Mint

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Frontend application](/courses/ft4-demo-app/module-frontend-application/)
- Lesson 4 - Register and Mint
# Lesson 4 - Register and Mint

In this lesson, we will walk you through the process of minting an asset using the FT4 token standard.

We will use the register_and_mint_asset operation defined in the asset_management/rell/src/main.rell file  to create a new asset and mint it to an account.

To call an operation, you will need an FT4 session. You can get the current session using the useFtSession hook from the @chromia/react package.

asset_management/src/hooks/token-hooks.ts
```tsx
import { useFtSession } from "@chromia/react";import { publicClientConfig as clientConfig } from "@/utils/generate-client-config";interface MintTokenParams {  ticker: string;  name: string;  amount: number;}function useMintToken() {  const { data: ftAccounts } = useFtAccounts({ clientConfig });  const { data: session } = useFtSession(    ftAccounts?.length ? { clientConfig, account: ftAccounts[0] } : null,  );  const registerAsset = useCallback(    async (token: MintTokenParams) => {      if (!session) return;      try {        await session          .transactionBuilder()          .add(            op(              "register_and_mint_asset",              token.name,              token.ticker,              8,              BigInt(token.amount),              "https://cdn-icons-png.flaticon.com/512/4863/4863873.png",            ),          )          .buildAndSend();        onSuccess?.(token);      } catch (error) {        console.error(error);        onError?.(token);      }    },    [session],  );  return mintToken;}
```

In the code snippet above, we have created a custom hook called useMintToken that uses the useFtSession hook to get the current session.

The useMintToken hook takes a MintTokenParams object as an argument, which contains the following properties:

- ticker: The ticker symbol of the asset.

- name: The name of the asset.

- amount: The amount of the asset to mint.

Transactions can be built using the transactionBuilder method of the session object. We add the register_and_mint_asset operation to the transaction using the add method.

Calling the buildAndSend method will initiate a signature request for the transaction by using the configured keystore.


===== FILE: courses__ft4-demo-app__module-frontend-application__setup.md =====


# Lesson 1 - Set up the Frontend Application

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Frontend application](/courses/ft4-demo-app/module-frontend-application/)
- Lesson 1 - Set up the Frontend ApplicationOn this page
# Lesson 1 - Set up the Frontend Application

Stay in the root folder of the frontend application where package.json is located.

### Install dependencies​

Use pnpm to install all required packages:

```bash
pnpm install
```

### Configure the environment​

Create a .env file based on .env.example and fill it with the appropriate values:

```bash
NEXT_PUBLIC_NODE_URL=http://localhost:7740NEXT_PUBLIC_BRID=
```

- 
NEXT_PUBLIC_NODE_URL – URL of the Chromia blockchain node.

- 
NEXT_PUBLIC_BRID – BRID of your locally or remotely running dapp.

#### Running the application​

After installing dependencies and configuring the .env file, run the application locally:

```bash
pnpm dev
```

The app will be available at [http://localhost:3000](http://localhost:3000).


===== FILE: courses__ft4-demo-app__module-frontend-application__tools.md =====


# Lesson 2 - Chromia tools

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Frontend application](/courses/ft4-demo-app/module-frontend-application/)
- Lesson 2 - Chromia tools
# Lesson 2 - Chromia tools

- 
postchain-client:
Used to interact with the Chromia blockchain.

- 
@chromia/ft4:
Chromia's FT4 token standard library. Provides the tools to interact with the FT4 token standard.

- 
@chromia/react:
The Chromia React library provides a set of hooks for accessing postchain client and ft4 token standard.

- 
connectkit:
ConnectKit provides a pre-built UI for connecting to EVM based wallets like Metamask.

- 
wagmi:
Used to interact with the EVM based wallets.

FT4 supports both Ethereum and FT keystores. See the [FT4 documentation client keystore](https://docs.chromia.com/ft4/client/client-key-store#keystore-interface) for more information.


===== FILE: courses__ft4-demo-app__module-frontend-application__transfer.md =====


# Lesson 6 - Transfer asset

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Frontend application](/courses/ft4-demo-app/module-frontend-application/)
- Lesson 6 - Transfer asset
# Lesson 6 - Transfer asset

In this lesson, we will explore how to transfer assets between accounts using the FT4 token standard.

We can check the balances of the accounts using the useGetBalances hook from the @chromia/react package. After fetching the balances, we can check if the account has enough tokens to transfer.

asset_management/src/hooks/token-hooks.ts
```tsx
import { useCallback } from "react";import { createAmount, op } from "@chromia/ft4";import {  useFtAccounts,  useFtSession,  useGetAllAssets,  useGetBalances,  usePostchainClient,} from "@chromia/react";import { useAccount } from "wagmi";import { ensureBuffer } from "@/utils/ensure-buffer";import { publicClientConfig as clientConfig } from "@/utils/generate-client-config";export function useTransferTokens({  onSuccess,  onError,}: {  onSuccess?: (token: MintTokenParams) => void;  onError?: (token: MintTokenParams) => void;}) {  const { data: ftAccounts } = useFtAccounts({ clientConfig });  const { data: session } = useFtSession(    ftAccounts?.length ? { clientConfig, account: ftAccounts[0] } : null,  );  const { flatData: balances } = useGetBalances(    ftAccounts?.length      ? {          clientConfig,          account: ftAccounts[0],          params: [10],          swrInfiniteConfiguration: {            refreshInterval: 20_000,          },        }      : null,  );  const transferTokens = useCallback(    async (recipient: string, amount: number) => {      if (!balances?.length) {        return;      }      const asset = balances[0].asset;      try {        await session?.account.transfer(          ensureBuffer(recipient),          ensureBuffer(asset.id),          createAmount(amount),        );        onSuccess?.({ ticker: asset.symbol, name: asset.name, amount });      } catch (error) {        console.error(error);        onError?.({ ticker: asset.symbol, name: asset.name, amount });      }    },    [balances, onError, onSuccess, session?.account],  );  return transferTokens;}
```

In the code snippet above, we have created a custom hook called useTransferTokens that uses the useFtSession hook to get the current session and the useGetBalances hook to get the account balances and check if the account has enough tokens to transfer. If the account has enough tokens, the transfer method is called on the session object's account property to transfer the specified amount of tokens to the recipient account.


===== FILE: courses__ft4-demo-app__module-init.md =====


# Module 1 - Init Fullstack application

URL: https://learn.chromia.com

- [Home](/)
- Module 1 - Init Fullstack application
# Module 1 - Init Fullstack application

This course will guide you to setup blockchain environment and create a rell dapp template.

In this course, you will learn how to:

- Setup environment.

- Create a project based on the asset-managent template.

## Lessons
[Set up the Fullstack application](/courses/ft4-demo-app/module-init/setup-application)[Start module »](/courses/ft4-demo-app/module-init/setup-application)


===== FILE: courses__ft4-demo-app__module-init__setup-application.md =====


# Set up the Fullstack application

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Init Fullstack application](/courses/ft4-demo-app/module-init/)
- Set up the Fullstack applicationOn this page
# Set up the Fullstack application

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

Let's kick things off by setting up your blockchain app project using the Chromia CLI.

Create a new directory for your project and navigate to it:

```shell
chr create-rell-dapp asset_management --template=asset-management
```

```shell
cd asset_management
```
