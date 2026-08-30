# Transfer strategies

URL: https://learn.chromia.com

- [Home](/)
- Lesson 5 - Transfer strategiesOn this page
# Transfer strategies

FT4 provides three transfer strategies for account registration that require users to transfer tokens: transfer open, transfer fee, and transfer subscription. These strategies offer different monetization approaches while requiring token transfers for account activation.

## Transfer open strategy​

The transfer open strategy requires users to transfer a specific amount of tokens to a non-existent account, which they must then claim to activate it. The non-existent account represents an empty account that needs to be funded with tokens. Once activated, users can utilize the tokens sent to the account.

This strategy provides a middle ground between completely free account creation and paid strategies, requiring token transfer but no fees.

warningImportant considerations for production use:

The transfer open strategy can be used in production, but it comes with spam risks since there are no transfer fees (neither local nor cross-chain). Without proper safeguards, users could potentially create thousands of accounts by transferring tokens back and forth.

To use safely in production:

- Consider limiting account creation to same-address transfers (sender ID = recipient ID)

- Implement additional rate limiting or validation mechanisms

- Monitor for potential spam patterns

### Code example for transfer open strategy​

To develop your dapp from the template, refer to the [code examples](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/) and corresponding tests.

The following configuration defines the account registration process and should be added to the chromia.yml file:

```rell
lib.ft4.core.accounts.strategies.transfer:  rules:    - sender_blockchain: x"0000000000000000000000000000000000000000000000000000000000000000"    sender: "*"    recipient: "*"    asset:        - name: "MyTestAsset"        min_amount: 100L    timeout_days: 60    strategy:    - "open"
```

noteFor the complete chromia.yml configuration including blockchain setup, FT4 library configuration, and other required settings, refer to the [actual configuration file](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/rell/chromia.yml) in the repository.

## Transfer fee strategy​

The transfer fee strategy simplifies the process for users by requiring a one-time purchase of a specific amount of tokens to access your dapp's features. This system is ideal for users who prefer a clear and easy-to-follow process.

When implementing the transfer fee strategy for account registration, users need to transfer a specific amount of tokens to a non-existent account. Once the non-existent account receives the tokens, the fee is deducted from the transferred amount. The non-existent account represents an empty account that needs to be topped up with tokens.

### Code example for transfer fee strategy​

To build your dapp from the template, refer to the [code examples](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/) and corresponding tests.

You must add the following example configuration to your chromia.yml file:

```rell
lib.ft4.core.accounts.strategies.transfer:  rules:    - sender_blockchain: x"0000000000000000000000000000000000000000000000000000000000000000"    sender: "*"    recipient: "*"    asset:        - name: "MyTestAsset"        min_amount: 100L    timeout_days: 60    strategy:    - "fee"lib.ft4.core.accounts.strategies.transfer.fee:  asset:    - name: "MyTestAsset" # issued by current blockchain    amount: 40L  fee_account: x"YOUR_FEE_ACCOUNT_ADDRESS" # All fees will be collected into this account
```

noteFor the complete chromia.yml configuration including blockchain setup, FT4 library configuration, and other required settings, refer to the [actual configuration file](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/rell/chromia.yml) in the repository.

## Transfer subscription strategy​

When implementing a transfer subscription strategy for your dapp, users will need to pay regularly to access its functionality. This approach helps generate a steady income to sustain and grow your dapp.

To set up the transfer subscription strategy, you'll need to go through several technical steps. First, configure the chromia.yml file to enable subscriptions. Then, users must call the transfer function, pay a predefined fee, and transfer a specific amount of tokens to a non-existent account. This non-existent account represents an empty account that needs to be filled with tokens and is a crucial part of the process.

Afterward, users must invoke the claim function to claim the non-existent account. You can set the desired subscription period by configuring the subscription_period_days in the chromia.yml file. When the subscription period ends, the account becomes inactive and cannot be used further. To reactivate the account, users must renew the subscription by sending a specific amount of tokens to the account.

### Code example for transfer subscription strategy​

For practical guidance on building your dapp from the template, it's crucial that you refer to the [code examples](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/) and corresponding tests. These resources will provide you with a clear understanding of the implementation process.

To make the transfer subscription available, extend your dapp configuration by copying and pasting the following configuration into your chromia.yml file:

```yml
lib.ft4.core.accounts.strategies.transfer:  rules:    - sender_blockchain: x"0000000000000000000000000000000000000000000000000000000000000000"      sender: "*"      recipient: "*"      asset:        - name: "MyTestAsset"        min_amount: 100L      timeout_days: 60      strategy:      - "subscription"lib.ft4.core.accounts.strategies.transfer.subscription:  asset:    - name: "MyTestAsset" # issued by current blockchain # OR id: x"C633343E4AA3213EA92158648F11BA8DFF606C6CAC80614CFA5F45E57367F823"    amount: 10L  subscription_period_days: 30  free_operations:    - some_free_operation
```

noteFor the complete chromia.yml configuration including blockchain setup, FT4 library configuration, and other required settings, refer to the [actual configuration file](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/rell/chromia.yml) in the repository.

## Strategy comparison​

| 
| Aspect| transfer open| transfer fee| transfer subscription
| Fee required| No (but tokens must be transferred)| Yes (one-time)| Yes (recurring)
| Production use| Safe with proper safeguards| Recommended| Recommended
| Revenue model| No direct revenue| One-time payments| Recurring payments
| User experience| Requires token transfer| Simple one-time payment| Regular payments required
| Complexity| Low| Medium| High

## Configuration properties​

| 
| Property| Description
| sender_blockchain| The property sender_blockchain is set to receive tokens from the blockchain with the BRID: x"0000000000000000000000000000000000000000000000000000000000000000". To receive tokens from all chains in the Chromia network, use the asterisk sign "*" as the value.
| sender| The sender property defines who can send tokens to the dapp. The value "*" allows everyone to send tokens.
| recipient| The recipient property defines who can receive tokens in the dapp. The value "*" indicates that everyone can receive tokens.
| name| The name property represents the name of the asset required for account registration.
| min_amount| The min_amount property sets a minimum threshold for the transfer to a non-existent account for registration purposes.
| timeout_days| The timeout_days property specifies the period in days during which a user can claim a non-existing account with a balance.
| strategy| The strategy configures the strategy type used for the registration process.
notePlease refer to the [tests](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/rell/src/test/) for testing the transfer open strategy, [tests](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/rell/src/test/) for testing the transfer fee strategy, and [tests](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/rell/src/test/) for testing the transfer subscription strategy.
