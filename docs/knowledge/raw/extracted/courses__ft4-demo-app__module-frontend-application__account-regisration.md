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
