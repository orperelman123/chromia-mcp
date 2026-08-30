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
