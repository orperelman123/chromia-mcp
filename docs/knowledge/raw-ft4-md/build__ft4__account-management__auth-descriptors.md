On this page
# Auth descriptors

One of the core features of FT4 is the ability to use multiple key pairs or accounts to control a single account.

Auth descriptors are a mechanism that defines the authorized key pairs and their associated permissions for interacting with accounts on the blockchain. They enable features like multi-signature security and granular access control, allowing for secure and flexible management of account access.

### Types of auth descriptors​

- Single-signature auth descriptor: This involves a single signer who is identified by either a native Chromia public key or an Ethereum Virtual Machine (EVM) account address for cross-chain signatures. 
- Multi-signature auth descriptor: This allows multiple signers to manage an account. Each signer is identified by either a public key (for Chromia) or an EVM account address (for Ethereum integration). 
### Flexible account management with multiple auth descriptors​

Each `auth_descriptor` defines who can access the account and what they are allowed to do. FT4 supports multiple types of auth descriptors, including single-signature and multi-signature setups. This makes FT4 suitable for a range of use cases, from personal wallets to shared or multi-party accounts.

To manage performance and prevent excessive data on the blockchain, there is a limit on the number of authorization descriptors an account can have. By default, this is set to 10, but it can be configured by developers to meet the needs of their dapp:

```
blockchains:
  my_rell_dapp:
    module: main
    moduleArgs:
      lib.ft4.core.accounts:
        auth_descriptor:
          max_number_per_account: 4

```

## Authorization flags and operation control​

Authorization flags (auth flags) are a critical component of FT4's security model. These flags define which actions a user can perform on an account. If an auth descriptor lacks the necessary flag, any operation attempting to use that descriptor will be rejected.

### Built-in authorization flags​

- Transfer (T) flag: Required for token transfer operations. 
- Account (A) flag: Required for FT4 account operations, such as adding or deleting auth descriptors. 
In addition to the built-in flags, dapps can create custom auth flags tailored to their specific needs. While the flags can technically be any string, it’s recommended to use shorter flags for better performance.

## Expiration rules for auth descriptors​

FT4 allows for fine-grained control over access by supporting expiration rules for auth descriptors. These rules govern the lifespan or usability of the descriptor, offering extra security by limiting the duration or usage of certain access privileges.

### Supported expiration rules​

- Block height: The descriptor becomes invalid after a specific block height is reached. 
- Block time: The descriptor expires after a certain block time. 
- Number of authorizations: The descriptor expires after it has been used a predefined number of times. 
Note that descriptors created when an account is first set up cannot have expiration rules to ensure that there is always a way to manage the account. The maximum number of expiration rules that can be set per auth descriptor is configurable (default is 8).

```
blockchains:
  my_rell_dapp:
    module: main
    moduleArgs:
      lib.ft4.core.accounts:
        max_auth_descriptor_rules: 4

```

# Authentication with FT4 accounts

Authentication in dapps is essential for verifying if a user has permission to perform specific actions. While the process is straightforward with regular Rell applications, it becomes more complex with FT4 due to support for native and EVM signatures and multiple keys for an FT4 account. However, the complexity is abstracted, and authentication can be easily implemented using the `authenticate` operation.

## Operation signing​

FT4 supports both EVM and non-EVM signatures (Chromia only). Every operation has a signature message being signed in case EVM is used. In case of non-EVM signatures, the whole transaction is signed.
- Types of auth descriptors
- Flexible account management with multiple auth descriptors
- Authorization flags and operation control
- Built-in authorization flags
- Expiration rules for auth descriptors
- Supported expiration rules
- Operation signing