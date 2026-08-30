On this page
# Manage FT4 accounts

FT4 provides powerful tools for managing accounts in a decentralized environment. Each account is represented by an ID, which serves as a unique identifier within the system. The architecture supports the management of accounts by multiple users, each of whom can have different levels of access and control.

## Account structure​

Each account is defined as an entity:

```
entity account {
  key id: byte_array;
  index type: text;
}

```

## Multi-user account management​

Accounts can be controlled by more than one user through a system of auth descriptors (`account_auth_descriptor`). These descriptors specify who is allowed to perform specific actions on the account and what rules or conditions govern that access. This allows for the creation of shared accounts, where different parties have different privileges.

```
entity account_auth_descriptor {
   id: byte_array;
   key account, id;
   index id;
   auth_type;
   args: byte_array;
   rules: byte_array;
   mutable ctr: integer;
   created: timestamp;
}

```

## Account registration​

Account registration ensures that users can interact securely with decentralized applications (dapps), perform transactions, and manage their assets. Each account serves as a unique identity on the blockchain, enabling secure ownership, transfer of assets, and participation in decentralized processes.

FT4 offers multiple ways to register accounts, including a general registration framework and an admin-level operation for more specific use cases. Custom operations can also be defined for more advanced needs. The method you choose depends on the type of control, permissions, and registration strategy required for your dapp.

### Account registration framework​

For most dapps, the account registration framework provided by the FT4 library is the recommended approach. It offers a flexible, secure, and scalable system to create new accounts, supporting a variety of strategies that cater to different business models and security needs. The framework simplifies the process for developers, providing pre-built solutions for common registration scenarios.

#### Registration strategies​

The account registration framework supports the following strategies:

- Open: Anyone can call the `register_account()` operation to create an account without any restrictions. 
- Transfer strategy: Users must perform a transfer to the account address before they can create an account. The transfer strategy has three sub-strategies: 
- Transfer open: The user can claim the entire deposit to their newly created account. 
- Transfer fee: Part of the transferred assets is collected as a fee to the chain's fee account, and the user can only claim the remaining assets to their account. 
- Transfer subscription: Similar to the transfer fee strategy, but the user needs to periodically renew their subscription by paying the subscription fee to keep using their account. 
To enable a specific strategy, import the corresponding module into your Rell file and configure it in the `chromia.yml` file. The modules are named as follows:

- `lib.ft4.core.accounts.strategies.open` (open strategy - not a transfer strategy) 
- `lib.ft4.core.accounts.strategies.transfer.open` (transfer open strategy) 
- `lib.ft4.core.accounts.strategies.transfer.fee` (transfer fee strategy) 
- `lib.ft4.core.accounts.strategies.transfer.subscription` (transfer subscription strategy) 
## Rate limiter​

The account module has a feature to enable rate-limiting in its operations for spam prevention.

The client accumulates one "operation point" every `rate_limit_recovery_time` milliseconds, up to `rate_limit_max_points`. You spend one operation point for each authenticated operation.

You can configure this with module args for the `lib.ft4.core.accounts` module like this:

```
blockchains:
  my_rell_dapp:
    module: main
    moduleArgs:
      lib.ft4.core.accounts:
        rate_limit:
          max_points: 10
          recovery_time: 5000
          points_at_account_creation: 1

```

| Argument | Description | Default value |
| max_points | is the maximum number of operation points that can accumulate (and, therefore, the maximum number of transactions you can do at once). | 10 |
| recovery_time | (Milliseconds) is the cooling period before an account can receive one operation point. | 5000 |
| points_at_account_creation | The points that an account has at the moment of its creation (0 is min). | 1 |

- Account structure
- Multi-user account management
- Account registration
- Account registration framework
- Rate limiter