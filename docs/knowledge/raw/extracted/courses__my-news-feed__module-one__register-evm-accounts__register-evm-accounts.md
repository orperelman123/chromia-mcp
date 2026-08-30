# Register accounts using EVM Wallets

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 6 - Register users using EVM wallet](/courses/my-news-feed/module-one/register-evm-accounts/)
- Register accounts using EVM WalletsOn this page
# Register accounts using EVM Wallets

In this section, we will create a module that enables user account registration using EVM (Ethereum Virtual Machine) wallets. This functionality allows users to sign in with EVM-compatible wallets like MetaMask and can be extended to include features such as payments.

## Conceptual design​

Users will register an account by signing a message with MetaMask. We achieve this by utilizing the [open account registration strategy](https://docs.chromia.com/ft4/backend/accounts/overview#account-registration-framework) from the ft library. The following sequence diagram illustrates the registration flow:

## Implementation in Rell​

We will implement the registration flow by creating a new module called registration. This separation allows easy inclusion or removal of account registration features and supports multiple registration methods. Follow these steps for implementation:

- 
Create a folder named registration, and within it, add a file called module.rell.

- 
Add the following definitions to registration/module.rell:

src/registration/module.rell
```rell
module;import ^.news_feed.{ user, follower };import lib.ft4.core.accounts.strategies.open. { ras_open };import lib.ft4.accounts.strategies. { register_account };operation register_user(name) {    val account = register_account();    val user = create user ( name, account.id, account );    create follower ( user = user, follower = user );}
```

- The open strategy allows users to register themselves.

- The register_user operation registers an account, creates a user, and establishes follower information.

- The register_account function ensures that the transaction is signed by a unique EVM account.

- In your chromia.yml file, add an exclusion for this operation to notify the ft library that the register_user operation is safe to use:

chromia.yml
```yaml
blockchains:  newschain:    module: main    moduleArgs:      lib.ft4.core.auth:        evm_signatures_authorized_operations:          - register_user      lib.ft4.core.admin:          admin_pubkey: "0359A8F2CE1BEF95F583169B7DF053AA227A93B2652B0A9C22975FEED638032610"
```

For more details on configuring your chromia.yml file, refer to the [Chromia Project Configuration documentation](https://docs.chromia.com/intro/configuration/project-structure).

- In your main.rell file, include an import statement for the new registration module:

src/main.rell
```rell
module;import news_feed.*;import registration.*;
```

cautionWhile the register_user operation requires a valid EVM signature, it can expose a potential DDOS attack vector since each account corresponds to a row in the database. To mitigate this risk, consider the following strategies:

- Implement rate limiting for the operation.

- Enforce payments during registration.

- Establish an [authentication](https://docs.chromia.com/ft4/backend/authentication/) layer as an intermediary.
