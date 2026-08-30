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
