# Account registration

URL: https://learn.chromia.com

- [Home](/)
- Account registration
# Account registration

When users interact with a dapp, they typically need to create a new account. The account registration process relies on the [Chromia FT4 library](https://docs.chromia.com/ft4/intro) (Flexible Token 4), which sets a standard for managing accounts and tokens on Chromia.

There are three types of account registration: Custom, Open, and Transfer.

- Custom: In the custom strategy, developers need to create their own registration logic using the create_account_with_auth function.

- Open: The open strategy is often used in development environments as it allows swift account registration without fees, but it should be used cautiously in production environments to avoid potential exploitation for spamming the network. The register_account function is used for this strategy.

- Transfer: Transfer strategies include transfer_subscription, transfer_fee, and transfer_open, which provide templates for developers to configure their dapp. These strategies use the transfer function to transfer tokens to a new account, paying a fee and transferring a designated amount of tokens to initiate the account registration process. If the non-activated account remains unclaimed for a specific period, the sender can retrieve the tokens. It's important to note that the transfer_open strategy does not require paying a fee for account registration.
