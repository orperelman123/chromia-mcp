
# Accounts

FT4 accounts can be registered with transfer open, transfer fee, or transfer subscription strategies, each offering flexible access options. auth descriptors control account access, while account_link manages relationships between accounts for scenarios like staking and asset locking.
Register FT4 accounts
Register FT4 accounts using strategies like transfer open, transfer fee, or transfer subscription. Admin operations allow authorized users to create accounts directly, while custom operations enable tailored registration with enhanced security measures. Transfer open strategy

The transfer open strategy requires users to transfer tokens to a non-existent account, which they must then claim to activate it. This provides a middle ground between completely free account creation and paid strategies, requiring token transfer but no fees.
Transfer fee strategy

The transfer fee strategy requires users to transfer a specific amount of tokens to a non-existent account, with part of the transferred assets collected as a fee. This approach provides a clear and easy-to-follow process for users who prefer straightforward access.
Transfer subscription strategy

The transfer subscription strategy requires users to pay regularly to access dapp functionality, helping generate a steady income to sustain and grow the dapp. Users must periodically renew their subscription by paying the subscription fee.
Use auth descriptors for accounts

Accounts can have multiple auth descriptors, with one main descriptor controlling access. Descriptors can be replaced and linked to the account with specific rules, while the main descriptor cannot be deleted.
Link FT4 accounts
The account_link entity manages relationships between accounts, supporting scenarios such as staking, asset locking, and auctions. It enables flexible workflows with secure asset handling for both user-controlled and system-managed accounts.