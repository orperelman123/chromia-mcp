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
