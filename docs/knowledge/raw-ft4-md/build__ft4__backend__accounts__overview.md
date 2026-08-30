On this page
# Register FT4 accounts

In the FT4 library, accounts function as digital identities, enabling users to transfer assets, interact with dapps, and sign transactions. Account registration allows the system to securely identify and authenticate users, supporting access control mechanisms that let users or administrators define permissions for actions an account can perform. This approach is crucial for maintaining security, preventing unauthorized access, and ensuring that resources such as assets or data remain accessible only to the rightful owner.

## Account registration framework​

For most dapps, the account registration framework provided by the FT4 library is the recommended approach. This framework offers a flexible, secure, and scalable system for creating new accounts, supporting various strategies tailored to different business models and security requirements. The framework simplifies the developer experience, offering pre-built solutions for common registration scenarios.

### Registration strategies​

The account registration framework supports several strategies:

- Open: Anyone can call the `register_account()` operation to create an account without restrictions. 
- Transfer strategy: Users must perform a transfer to the account address before creating an account. The transfer strategy includes three sub-strategies: 
- Transfer open: Users can claim the entire deposit to their newly created account. 
- Transfer fee: Part of the transferred assets is collected as a fee to the chain's fee account, allowing users to claim only the remaining assets. 
- Transfer subscription: Similar to the transfer fee strategy, but users need to periodically renew their subscription by paying the subscription fee to maintain account access. 
To enable a specific strategy, import the corresponding module into the Rell file and configure it in the `chromia.yml` file. The modules are named as follows:

- `lib.ft4.core.accounts.strategies.open` (open strategy - not a transfer strategy) 
- `lib.ft4.core.accounts.strategies.transfer.open` (transfer open strategy) 
- `lib.ft4.core.accounts.strategies.transfer.fee` (transfer fee strategy) 
- `lib.ft4.core.accounts.strategies.transfer.subscription` (transfer subscription strategy) 
### Strategy comparison​

| Aspect | Open | Transfer open | Transfer fee | Transfer subscription |
| Fee required | No | No (but tokens must be transferred) | Yes (one-time) | Yes (recurring) |
| Production use | Not recommended (dev/test only) | Safe with proper safeguards | Recommended | Recommended |
| Revenue model | None | No direct revenue | One-time payments | Recurring payments |
| User experience | Direct account creation (no transfer) | Requires token transfer | Simple one-time payment | Regular payments required |
| Complexity | Very low | Low | Medium | High |

#### Transfer strategy​

To use any of the transfer strategies, certain `moduleArgs` must first be configured in the `chromia.yml` file under the key:

```
blockchains:
  my_dapp_chain:
    module: my_dapp_module
    moduleArgs:
      lib.ft4.core.accounts.strategies.transfer:

```

Under this key, settings must be specified, including which chains are permitted to make transfers to the chain and which assets are accepted. Configuring these parameters helps prevent potential DOS attacks and spam from untrusted chains. Here is an example configuration:

```
blockchains:
  my_dapp_chain:
    module: my_dapp_module
    moduleArgs:
      lib.ft4.core.accounts.strategies.transfer:
        rules:
          - sender_blockchain: # List of blockchain rids from which we will accept transfers
              - x"08B02E0E14B634031FDF2ED3FD78E7410A5849CD28"
            sender: * # Anyone on the specified blockchain can send us assets
            recipient: * # They can send assets to anyone on this chain
            asset: # List of assets that can be sent from this chain
              - name: CHR # Name of the asset (id can also be used instead)
                min_amount: 5L # If transfer is of less than this value, then the transfer will be rejected
            timeout_days: 30 # After this many days, the sender is allowed to recall the transfer if it hasn't been claimed
            strategy: # List of transfer strategies to enable
              - "fee"
              - "open"

```

In this example, every user on a specified Chromia blockchain is allowed to send a minimum of 0.000005 CHR to any user on the receiving blockchain. If the transfer recipient does not have an existing account, they can create one using either the `open` or `fee` strategy. This configuration enables granular control over which blockchains can send assets and create accounts.

### Transfer strategy configuration values​

The transfer strategy uses special values to define flexible rules:

- `$` (Current Chain): Represents the current blockchain's BRID. Can be used in `sender_blockchain`, `issuing_blockchain_rid` for assets. 
- `*` (Any Value): Represents any allowed value. Can be used in `sender_blockchain`, `sender`, `recipient`, and asset fields. 
- `X` (Same Value): Requires the sender and recipient to be the same account. Can only be used in `sender` and `recipient` fields together. 
#### Example configurations​

Allow transfers from any chain:

```
sender_blockchain:
  - "*"

```

Allow transfers only from current chain:

```
sender_blockchain:
  - "$"

```

Allow transfers from specific chains:

```
sender_blockchain:
  - x"08B02E0E14B634031FDF2ED3FD78E7410A5849CD28"
  - x"6403ccac0c67f7cb6af78e5e15b3aaebb2b42370f0d12e099ed01fa5a068f9fb"

```

Allow self-transfers only:

```
sender: "X"
recipient: "X"

```

If only the `open` strategy is enabled, additional configurations are not required. However, the `open` strategy may not provide optimal spam protection, as it could allow the creation of numerous accounts with minimal assets. For enhanced protection, using an alternative strategy, such as the `fee` strategy, is recommended.

To configure the `fee` strategy, additional steps are required beyond the initial setup. First, import the strategy in the main `module.rell` file

```
import lib.ft4.accounts.strategies.transfer.fee;

```

Then, configure the strategy by specifying the valid asset(s) for payments, the fee amount, and the destination for the fee payment. This is done by adding a configuration under the `moduleArgs` key in the `chromia.yml` file:

```
blockchains:
  my_dapp_chain:
    module: my_dapp_module
    moduleArgs:
      lib.ft4.core.accounts.strategies.transfer.fee:
        asset:
          - id: x"b31ba66a11a28930d948c8f959cc306184096d1ee858542e765a139b3c79b1aa" # We can specify an asset by id
            amount: 2L # How much of this asset to pay
          - name: test1 # we can specify an asset by name, this will refer to an asset issued by this chain
            amount: 1L
          - issuing_blockchain_rid: x"6403ccac0c67f7cb6af78e5e15b3aaebb2b42370f0d12e099ed01fa5a068f9fb" # We can also specify assets issued by a different chain, even if the names are the same
            name: test1
            amount: 3L
        fee_account: x"023c72addb4fdf09af94f0c94d7fe92a386a7e70cf8a1d85916386bb2535c7b1b1" # All fees will be collected into this account

```

In the configuration above, users can pay the fee using one of three assets, with varying amounts depending on the asset. This flexibility can accommodate value differences between assets or encourage payment with a preferred asset. It is worth mentioning that the asset id cannot be accompanied by the name or issuing chain rid. Additionally, the account for fee collection is specified, which should typically be controlled by the dapp owner.

### Configuring foreign assets (cross-chain assets)​

For most mainnet dapps, you'll want to use foreign assets like CHR for account registration. Here are the different ways to specify assets:

#### Local asset (issued by current chain)​

```
asset:
  - name: "MyLocalAsset" # Asset issued by this chain
    amount: 1L

```

#### Foreign asset by ID​

```
asset:
  - id: x"C633343E4AA3213EA92158648F11BA8DFF606C6CAC80614CFA5F45E57367F823" # Asset ID
    amount: 1L # 10 CHR (6 decimals)

```

#### Foreign asset by name and issuing chain (recommended for CHR)​

```
asset:
  - name: "CHR" # Asset name
    issuing_blockchain_rid: x"15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA" # Economy chain RID
    amount: 1L # 10 CHR (6 decimals)

```
tip
Most common use case - for mainnet dapps using CHR for account registration, use the third option with the CHR token chain RID. caution
The `amount` set for an asset must be lower than the `min_amount` specified for that asset. If the `amount` is set too high, users may accidentally send an insufficient amount, preventing account creation until the transfer timeout is reached. They would then need to recall and repeat the transfer with the correct amount.
Applies to the Rell library version v1.0.0 and above. :::
For account registration using a transfer strategy, a transfer must first be made to the account. If the user already holds an account with enough assets, they may initiate the transfer themselves, or a friend might perform it on their behalf, functioning as an invitation system.
## Rate limiting​

FT4 provides built-in rate limiting to prevent spam and abuse. Rate limiting can be configured globally or per-account to control how frequently users can perform operations.
### Global rate limiting configuration​

Configure rate limiting in your `chromia.yml`:
```
blockchains:
  <my_blockchain_name>:
    module: my_dapp_module
    moduleArgs:
      lib.ft4.core.accounts:
        rate_limit:
          active: true
          max_points: 20
          recovery_time: 5000 # milliseconds
          points_at_account_creation: 1

```

### Rate limiting parameters​

- `active`: Enables or disables rate limiting globally 
- `max_points`: Maximum points allowed before rate limiting is triggered 
- `recovery_time`: Time in milliseconds for points to recover 
- `points_at_account_creation`: Points consumed when creating an account 
### Per-account rate limiting​

You can implement custom rate limiting logic by extending the `get_rate_limit_config_for_account` function:
```
@extend(get_rate_limit_config_for_account) function get_rate_limit_config_for_account(account: account): rate_limit_config? {
    // Return custom config for specific accounts
    // Return null to use global default
}

```

For more details on rate limiting implementation, see the Rell Security documentation.info
To see an example of account registration using the `transfer open` strategy, explore the Transfer open strategy account registration demo. You can also find examples of all strategies in this repository. 
## Register with FT4 admin operation​

For scenarios requiring greater control or direct intervention, Chromia offers the FT4 admin operation. This admin-level operation allows authorized administrators to create accounts directly, bypassing the user-driven registration process.

### Why use the admin operation?​

Admin operations are intended for cases requiring stricter control over account creation, such as when developers or dapp owners need to create accounts for system users, team members, or to initiate specific business processes. Unlike user-led registrations, admin operations can bypass certain checks and constraints, enabling direct management of the blockchain environment.

However, admin operations are not recommended for production environments, as they may introduce security risks if not carefully managed. If an admin operation is necessary, consider building a custom admin module tailored to the specific requirements of the dapp.
warning
Admin operations are enabled by importing the admin module and should not be used in production. 
### Admin vs. non-admin operations​

- Admin operations: These provide broader permissions and access, allowing administrators to perform tasks like account registration, asset management, or system updates. They are powerful but carry higher risks, as improper use can affect the security and stability of the entire system. 
- Non-admin operations: These are designed for end-users or automated processes with more restricted access. They follow strict rules, ensuring that users can only perform actions they are explicitly authorized to carry out (e.g., transferring assets or modifying their own account settings). 
### Registering an account with FT4 admin​

The FT4 admin operation, `ft4.admin.register_account`, requires an auth descriptor as a parameter. An auth descriptor specifies who can access an account, what actions they can perform, and the lifetime of the auth descriptor. The access to an account is determined by a public key when FT authentication (native Chromia signatures) is used or an EVM account address when an EVM wallet is used for authentication.

The actions allowed by the auth descriptor are specified with authorization keys, and expiration rules determine the activation and validity duration of the auth descriptor. For more details, see the auth descriptor topic.

To create an account using the FT4 admin operation, follow these steps:

- 
Generate a new key pair for the account and retrieve the public key:

```
chr keygen --file .chromia/user.keypair | grep pubkey

```

- 
Register the account using the FT4 admin operation:

```
chr tx ft4.admin.register_account \
    '[0, [["A","T"], x"0351D4F299E3D33EC745C9F3C2F74934960F58411BE8BAE52A1E6EC8D0BA26AEDB"], null]' \
    --await --secret .chromia/ft4-admin.keypair

```

In this command:

- 
`0` represents a single signature auth descriptor.

- 
`A` (account) and `T` (transfer) are auth flags defined in the FT library.
note
It's important to note that when calling FT account operations, such as `ft4.add_auth_descriptor` or `ft4.delete_auth_descriptor`, the authentication will fail unless the auth descriptor used has the `A` flag. Similarly, when calling the transfer operation, the transfer will be rejected if the auth descriptor doesn't have the `T` flag. 
- 
Replace `0351D4F299E3D33EC745C9F3C2F74934960F58411BE8BAE52A1E6EC8D0BA26AEDB` with the generated public key for the user.

- 
A query can be added that returns all accounts to verify if the account exists. Add the following query to your code:

```
query get_all_accounts() = accounts.account @* {} (.id);

```

- 
Update and wait for the blockchain to reflect the changes:

```
chr node update

```

- 
You can now execute the query to retrieve all accounts:

```
chr query get_all_accounts

```

The output will display the account ID(s), such as:

```
[x"5E2488889F72939DD4D0A034FB91893ACBF14C7EDBCEF2A9F5C621A07169EAD2"]

```

## Register with a custom operation​

If the provided account registration framework or admin operation does not meet specific requirements, a custom operation can be created to handle account registration. Writing a custom operation provides full control over the registration process, enabling customization according to unique requirements and security needs.
tip
When implementing a custom operation, it is advisable to follow best practices for secure coding, including code reviews and thorough testing, to maintain the integrity and reliability of the account registration process. 
When registering an account with a custom operation, protecting the operation against potential spam attacks on the blockchain is essential. In this example, users are required to provide a voucher during registration. This approach involves creating a register and an admin operation to allow vouchers to be added.

- 
Define the voucher entity and the `add_voucher` operation in your code:

```
entity voucher {
    hash: byte_array;
    mutable is_used: boolean = false;
}

operation add_voucher(hash: byte_array) {
    admin.require_admin();
    create voucher(hash);
}

```

- 
Define the `register_account` operation, which includes voucher validation and account creation:

```
operation register_account(accounts.auth_descriptor, voucher_code: text) {
    // extract pubkey from auth descriptor
    val pubkey = byte_array.from_gtv(auth_descriptor.args[1]);

    // check if provided key is signer
    require(op_context.is_signer(pubkey), "Transaction needs to be signed by %s".format(pubkey));

    val hash = voucher_code.hash();
    val voucher = require(
        voucher @? { hash },
        "Provided voucher with code <%s> does not exist".format(voucher_code)
    );
    require(
        not voucher.is_used,
        "Provided voucher with code <%s> is already used".format(voucher_code)
    );
    voucher.is_used = true;
    accounts.create_account_with_auth(auth_descriptor);
}

```

- 
After adding the code snippets to the appropriate sections in your `main.rell` file, update the blockchain to apply the changes.

- 
Generate a voucher hash using the `chr repl` command as follows:

```
chr repl -c '"voucher_1".hash()'

```

The output will be a hash like:

```
x"E1E72D0C6C975815BD3259D81E67253D98CF90D888B4C7CB393C8CFB9043BAF3"

```

- 
Add the voucher hash to the blockchain using the admin operation:

```
chr tx add_voucher \
  'x"E1E72D0C6C975815BD3259D81E67253D98CF90D888B4C7CB393C8CFB9043BAF3"' \
  --await --secret .chromia/ft4-admin.keypair

```

- 
Generate a new keypair for the user who'll register the account, and retrieve the public key:

```
chr keygen --file .chromia/user-2.keypair | grep pubkey

```

- 
Register the account using the custom operation `register_account`:

```
chr tx register_account \
  '[0, [["A", "T"], x"03772E03AE22835384164AA90E28C84F78C97D29A2635861DC3F7E32F0CC8FDF51"], null]' \
  voucher_1 \
  --await --secret .chromia/user-2.keypair

```

Replace `03772E03AE22835384164AA90E28C84F78C97D29A2635861DC3F7E32F0CC8FDF51` with the generated public key for the user.

- 
To verify the successful account registration, execute the `get_all_accounts` query again. You should now see two account IDs or one if you didn't follow the first half of the guide:

```
chr query get_all_accounts

```

The output would include the account IDs:

```
[x"5E2488889F72939DD4D0A034FB91893ACBF14C7EDBCEF2A9F5C621A07169EAD2", x"79C71AF3C9C951BED380F8ADAB2E407C15CC4A9EB942AA222D870136C45801CE"]

```

## Dapp-controlled accounts​

Chromia provides support for accounts that are entirely managed by dapp logic. These accounts do not have any associated auth descriptors, meaning they cannot be accessed or modified by external parties.

Dapp-controlled accounts are beneficial for:

- Creating liquidity pools or treasuries 
- Holding locked funds or protocol fees 
- Implementing automated reward systems 
You can find a code example demonstrating how to create a dapp-controlled account in this demo repository.

## Transfer open strategy example​

This FT4 example demonstrates account registration with the `transfer open` strategy, which allows registration only when sender and recipient IDs match during cross-chain transfers. The `Test` asset is registered dynamically on the first chain and via the `init` operation on the second.

The sender account uses a custom `custom_register_account` operation to register and mint 100 `Test` assets. The recipient is registered using the standard `register_account` operation with the same strategy.

Refer to the Github repository for more details and code examples.
- Account registration framework
- Registration strategies
- Strategy comparison
- Transfer strategy configuration values
- Configuring foreign assets (cross-chain assets)
- Rate limiting
- Global rate limiting configuration
- Rate limiting parameters
- Per-account rate limiting
- Register with FT4 admin operation
- Why use the admin operation?
- Admin vs. non-admin operations
- Registering an account with FT4 admin
- Register with a custom operation
- Dapp-controlled accounts
- Transfer open strategy example