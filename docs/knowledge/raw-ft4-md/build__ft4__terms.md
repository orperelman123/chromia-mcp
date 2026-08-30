On this page
# Glossary

This section provides a high-level explanation of the terms most commonly used in the documentation for the FT4 library.

## Account​

An account is an on-chain entity. It can hold auth descriptors to allow one or more users to access it. It can hold assets.

While the most similar entity on an EVM chain would be an Externally Owned Account (EOA), an account has some key differences:

- It only needs to interact with the blockchain if coded to do so. If a dapp doesn't use FT4, they might not even have accounts. 
- It's not tied to a key pair. While on EVMs, only one key pair can access an EOA, here on Chromia, you can define auth descriptors to allow multiple key pairs. This doesn't mean the original owner can lose access to the account, though: look at the auth descriptor section to learn more. 
This last point also means an account can't "sign" a transaction, as it has no associated key pair. Users can sign a transaction on its behalf through auth descriptors.

### Different account types​

There are three different classes of ft accounts, which fundamentally behaves in the same way, but have some key differences.

#### User accounts​

These are the most common type of accounts and the only type which the user has direct control over. To most dapp users, user accounts will be equivalent to just accounts and most users will not be aware of any other types of accounts. These are the only accounts which are controlled by an auth descriptor by default.

#### Lock accounts​

These accounts are associated with a user account but assets in the lock account are not directly accessible by the user. They are, as suggested by the name, used to lock assets, such as when performing staking operations, in which case the user can stake assets by transfering them to a specific lock account. Under certain circumstances, dapps can allow users to retrieve assets in lock accounts, however, rules for this operation has to be implemented by the dapp and the operation to move assets from the lock account must also be performed by dapp code and not directly by a user. There can be any number of lock accounts associated with a user account, in order to serve various purposes.

#### System accounts​

Similar to lock accounts, system accounts is not accessible to users, but they're not even linked to a user account. What makes system accounts different from lock accounts is that system accounts are not associated with any user account and they are used purely for administrative purposes by the dapp. One example of a system account would be the account that a blockchain uses to keep track of cross chain assets it has received from or sent to other chains.

## Asset​

An asset is an on-chain entity representing many things, such as tokens, shares of ownership, and tickets to an event.

FT4 only supports fungible assets: the most similar entity on an EVM chain would be an ERC20.

There are some significant differences to keep in mind between ERC20 and FT4 assets:

- Burning assets: users can always burn tokens. On an EVM, they can always send them to an empty address, which won't affect the token supply. On FT4, there's no such thing as empty addresses, and all users can call the burn operation, reducing the token supply. 
- Minting assets: Minting is only allowed natively as part of the admin module operations and should never be included on a production dapp. It's advisable to add your minting capability if you need it. 
- Supply limits: there's no such thing as native supply limits (like maximum supply) on FT4 assets. If you need that capability, you should build your own. info
FT4 assets are not perfectly equivalent to ERC20. 
## Auth descriptor​

An auth descriptor is permission to operate as an account. It can also be thought of as one or multiple key pairs decorated with a set of access rules. It must be bound to the account it is allowing access to, but one auth descriptor can also be bound to multiple accounts. It can be limited in different ways.

It has a few components:

- Signers: the users that this descriptor represents. 
- Number of signers: If there is more than one signer, this parameter defines how many signers need to sign before the transaction is accepted. 
- Flags: they define what the signer can do with the account. There are two basic flag types, and dapp developers can define more:  - Account (specified as `A` when used in code): grants the rights to edit the account details, like adding or removing auth descriptors.  - Transfer (specified as `T` when used in code): grants the rights to transfer assets from the account, including burning them. 
- Rules: they define when the auth descriptor can be used and when it'll expire. They're discussed more deeply in the expiration rules section. 
Not to be confused with the auth handlers.

## Auth handler​

An auth handler specifies what permissions are needed to call an operation.

Most operations in FT4 require an account to be called: the auth handler for that operation specifies whether specific auth descriptors have sufficient authorization to call that operation on the account's behalf successfully.

Not to be confused with the auth descriptors.

## Balance​

The amount of a certain asset that is held by an account. It is an on-chain entity, separate from the account and the asset. This differs from EVM chains, where the balance is a map stored in the asset contract.

## Expiration rules​

Expiration rules are a component of auth descriptors. They limit the signers' access to the account. They can limit:

- The number of authorizations: the signers can only approve certain operations for this account. While one transaction can contain multiple operations, they'll be counted as different authorizations. 
- The block number: the signers can only approve transactions before or after a certain block number, relative to the genesis block or to the block where the auth descriptor was created. 
- The block time: the signers can only approve transactions before or after a block where the last block timestamp is specified in the rule. This time can be relative to the genesis block or the block where the auth descriptor was added. This can generally be used to specify a time expiration for the auth descriptor, but remember that the rule applies to the last block timestamp. If an auth descriptor expires after time `X`, and the previous block timestamp is `X - 1s`, the signers can approve transactions in the next block even if it takes longer than one second to produce. 
Using auth handlers, developers can implement their own custom rules via the `resolver` field.

## User​

Whenever the docs refer to a user, it should be understood as a key pair. Ideally, only one person can access any key pair, meaning a user should represent a physical person. When shared control is required, please use multi-signature schemes! Sharing a private key is neither safe nor should be required by dapp developers in the Chromia ecosystem.
- Account
- Different account types
- Asset
- Auth descriptor
- Auth handler
- Balance
- Expiration rules
- User