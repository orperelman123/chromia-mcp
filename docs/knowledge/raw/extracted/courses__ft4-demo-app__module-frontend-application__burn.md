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
