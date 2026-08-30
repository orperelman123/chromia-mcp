On this page
# Disposable keys and login management

To improve the user experience in web applications, FT4 provides a mechanism for generating disposable keys and adding them to a user's account. This allows non-interactive signing of operations using the directly accessible new key, eliminating the need for the user to sign each operation with MetaMask.

However, it's crucial to exercise caution when adding auth flags to disposable keys, as compromised keys with sensitive flags could lead to asset compromisation or other security risks.

FT4 offers a `login` function that simplifies the process of generating and managing disposable keys.

### Logging in​

The following example demonstrates adding a disposable key to an account using the `login` function and calling the `foo` and `bar` operations. Only one message signing is required to add a new auth descriptor, while the `foo` and `bar` operations are called without signing.

More information about messages and authentication can be found here.

```
createWeb3ProviderEvmKeyStore(window.ethereum).then(async (store: EvmKeyStore) => {
  const { getAccounts, login } = createKeyStoreInteractor(client, store);

  const accounts = await getAccounts();

  if (!accounts.length) return;

  const { session, logout } = await login({
    accountId: accounts[0].id,
  });

  await session.call(op("foo"), op("bar", "some other text", 123456));

  // more calls here ...

  await logout();
});

```

However, the disposable auth descriptor will still require a signature if a transfer operation is called because it doesn't have the "T" (transfer) flag.

To sign the transfer transaction using the disposable auth descriptor, modify the `login` function call and add the "T" flag to the auth descriptor:

```
const { session, logout } = await login({
  accountId: accounts[0].id,
  config: {
    flags: ["T"],
  },
});

```

The disposable auth descriptor will be used to sign the transfer transaction instead of the master auth descriptor.

If we want, we can also specify a keystore when logging in which then will hold the signing key that will be used with the disposable auth descriptor. It is a good idea to always specify a keystore as it gives increased control over the lifecycle of the disposable key. If no keystore is provided, the disposable key will only be stored in memory and will thus be cleared when the browser window is reloaded. Forcing the user to login on each page reload can lead to bad UX.

Furthermore, using an in memory key store while developing could be problematic since each page reload will cause a new disposable auth descriptor to be added. In a development scenario, where each code change might trigger a hot reload of the page, one will quickly hit the upper limit of how many auth descriptors can be added, resulting in the inability of logging in to the account until the login time to live is reached.

To use a more durable keystore, we can specify it when logging in:

```
const { session } = await login({
  accountId: accounts[0].id,
  config: {
    flags: ["T"],
  },
  loginKeyStore: createSessionStorageLoginKeyStore(),
});

```

This will instead store the disposable key in the browsers session storage and consequently have the same lifecycle. Out of the box, there is also a variant that will store the key in the browsers local storage. The `login` function accepts any instance of the `LoginKeystore` interface which means that it is possible to implement a custom login keystore, e.g., for cases when the dapp does not run in a browser context.

One can also add rules to be applied to the new auth descriptor, such as for how long it should be valid:

```
const { session } = await login({
  accountId: accounts[0].id,
  config: {
    flags: ["T"],
    rules: ttlLoginRule(minutes(30)),
  },
  loginKeyStore: createSessionStorageLoginKeyStore(),
});

```

If no login config is provided. The default config will be used. The default timeout is then one day and the flags will be set to `[]`.
caution
Disposable keys aren't securely stored. Therefore, never add auth flags that could lead to asset compromise if the disposable key pair is compromised. 
### Logging out​

Logging out is a necessary step after login which removes the disposable key's access to the account, removing the risk of misuse in case that key is leaked. To do so, you simply have to call the `logout` function:

```
const { session, logout } = await login({
  accountId: accounts[0].id,
});

// use the disposable key...

await logout();

```

There are two main reasons to logout:

- the disposable key might be leaked and used to access the account by third parties, in a way that is comparable to authentication token stealing in web2; 
- every account has a limit to how many auth descriptor can be attached to it, and failing to remove old unused ones brings the account closer to this limit. Reaching the maximum number of auth descriptors added to the account will prevent the user from logging in again until some auth descriptors expire. 
For these reasons, always remember to call the `logout` function to destroy disposable keys when they are no longer needed.

If you want the better UX given by being able to login without user interaction, you can use a more durable login keystore when logging in (like `createSessionStorageLoginKeyStore()`). This can also help reduce the risk of being locked out from the account during development.

A more durable keystore will prevent the buildup of unusable auth descriptors on the account.

For more information on the maximum auth descriptor limit, check out the Use auth descriptors for accounts topic.
- Logging in
- Logging out