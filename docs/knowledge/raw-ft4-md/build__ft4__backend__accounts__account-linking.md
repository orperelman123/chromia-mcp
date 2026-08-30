On this page
# Link FT4 accounts

The `account_link` entity is a versatile structure designed to represent a relationship between two accounts. This entity can be customized for various purposes by developers, enabling the management of complex account interactions such as staking, locking, or auction-related transactions.

## Overview​

The `account_link` can be leveraged in scenarios where assets are held temporarily in a non-user-controlled account, or in cases where accounts must be linked for internal management or governance reasons. This flexibility allows developers to implement custom workflows and control mechanisms that support both user-controlled and system-controlled accounts.

## Structure​

### Entity definition: `account_link`​

The entity's structure allows it to store essential details of the relationship between accounts. Here’s an outline of the fields and their purposes:

- Primary account (`accounts.account`): Represents the main or initiating account in the relationship. Typically, this is a user account. 
- Secondary account (`secondary: accounts.account`): The target or secondary account in the relationship, often a non-user-controlled account used for specific purposes. 
- Relationship type (`type: text`): A description of the relationship, specified as text. Common examples include terms like `"stake"` or `"bid"`. 
#### Indices​

- Primary key: `(account, secondary)` — Uniquely identifies the link between a specific primary and secondary account. 
- Secondary index: `(secondary, type)` — Facilitates efficient querying based on the secondary account and relationship type. 
---

## Usage scenarios​

This entity is intended for scenarios where temporary control of assets or relationships between accounts is needed, especially when assets should be beyond the immediate control of the primary user account. Here are some typical use cases:

- Lock accounts: 
- Lock accounts allow assets to be held temporarily, with users having limited or no access. 
- Example: Staking tokens in a contract that prevents immediate withdrawal, using a custom lock account type defined as `ACCOUNT_TYPE_LOCK`. 
- Bidding and auction: 
- Bids in auctions often require assets to be held by the system to prevent immediate access by the bidder. 
- Example: An auction system where bid amounts are locked until the auction concludes, utilizing a system-controlled secondary account. 
- System-controlled accounts: 
- Many types of operations require system-level accounts to hold assets on behalf of users, without allowing users to reclaim them freely. 
- Example: A service or governance system managing assets with specific rules for accessibility or control. 
- Custom non-user account types: 
- Developers can define accounts that users may only control under certain conditions or with specific permissions. 
- Example: An escrow account controlled by a smart contract, where a user may release funds only after predefined conditions are met. 
## Implementation notes​

- Customizability: The `account_link` entity is deliberately flexible. It allows any relationship type to be defined based on the application’s needs. 
- Security and access control: Since non-user accounts can hold assets beyond a user's immediate reach, implementing secure access and control mechanisms is recommended for any application that leverages this entity. 
- On-chain transparency: By storing the relationship on-chain, applications using this entity can audit account links, ensuring transparency and accountability in asset management and user interactions. 
## Related modules​

### Locking and account types​

- `core.assets.locking`: Provides examples of lock accounts, including details on how assets can be secured in non-user accounts. 
- `core.accounts.account.type`: Offers more information on custom account types and their potential use in creating accounts with limited user control. 
## Example definition​

```
entity account_link {
    key account, secondary;
    index secondary, type;

    // Primary account in the relationship, typically a user account.
    accounts.account;

    // Secondary account, often a system or non-user account.
    secondary: accounts.account;

    // Describes the type of relationship, e.g., "stake", "bid".
    type: text;
}

```

- Overview
- Structure
- Entity definition: `account_link`
- Usage scenarios
- Implementation notes
- Related modules
- Locking and account types
- Example definition