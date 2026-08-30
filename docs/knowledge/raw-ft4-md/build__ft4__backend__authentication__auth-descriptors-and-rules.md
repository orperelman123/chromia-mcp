On this page
# Set up auth descriptors

Auth descriptors are a mechanism that defines the authorized key pairs and their associated permissions for interacting with accounts on the blockchain. They enable features like multi-signature security and granular access control, allowing for secure and flexible management of account access.

### Entities involved​

- Account: An on-chain entity representing a user or an app on the blockchain, capable of interacting with other entities like tokens and smart contracts. 
- User: A person seeking access to on-chain services through an account. 
- Key pair: A cryptographic tool used for identification and authorization, consisting of a public key (shared publicly) and a private key (kept secret). 
A more detailed description of these terms can be found in the glossary

### Key concepts​

- 
#### User and key pair relationships​

- A single user can have multiple key pairs for added security or convenience. 
- A single key pair should never be shared among multiple users. 
- 
#### Account and key pair/user relationships​

- A single account can be accessed by multiple key pairs and users, enabling shared accounts for groups or applications. 
- A single key pair can be used to access multiple accounts, providing flexibility for the user. warning
Never share your private key. The FT4 library offers secure methods for managing multiple user access to accounts without compromising private keys. 
Role of auth descriptors:

Auth descriptors define the key pairs authorized to interact with the blockchain on behalf of an account. This allows for:

- Multi-signature security: By requiring multiple keypairs to co-sign transactions, critical operations can be secured with the combined approval of authorized parties. 
- Granular access control: Permissions specified in the descriptor can restrict the actions and resources specific keypairs can access within an account. 
## Auth descriptor components​

An auth descriptor consists of arguments and rules.

- The arguments include who can access it and what they can do. 
- The rules explain under what conditions the auth descriptor is usable. 
### Auth descriptor arguments​

Components:

- Public keys: This component specifies the public keys that can be used to identify authorized key pairs for accessing the account. You can include multiple public keys to establish multi-signature access control. 
- Permissions (flags): This component defines the specific actions or resources the authorized key pairs can access or interact with within the account. For example, certain key pairs may be granted permission to transfer tokens, while others may be restricted to basic interactions with tokens. 
- Required signatures: This optional component specifies the number of authorized key pairs whose signatures are required for a transaction to be valid. This configuration enables multi-signature authentication, where multiple key pair approvals are needed for critical actions, enhancing security. This value can be less than the number of provided public keys if not all signers need to sign a transaction. 
Variations:

- Single-signature (single-sig): If only one key pair can be granted access, the "Required Signatures" component can be omitted, simplifying the auth descriptor for straightforward single-user scenarios. 
- Multi-signature (multi-sig): The default configuration includes the "Required Signatures" component, enabling scenarios where multiple key pairs need to co-sign transactions, providing higher security for critical operations. 
### Auth descriptor rules​

Auth descriptor rules use a combination of operators, variables, and values to define specific conditions that must be met for an auth descriptor to be considered valid.

- Operators 
- `>`: Greater than 
- `>=`: Greater than or equal to 
- `=`: Equal to 
- `<`: Less than 
- `<=`: Less than or equal to 
- Variables 
- `operation_count`: Represents the number of times the account has been used. 
- `block_time`: Represents the timestamp of the latest block on the blockchain. 
- `block_height`: Represents the block height of the latest block on the blockchain. 
- `relative_block_height`: Same as `block_height`, but the value is relative to the block in which the auth descriptor was registered instead of the genesis block. 
- `relative_block_time`: Same as `block_time` but relative to the block in which the auth descriptor was registered instead of the timestamp of the genesis block. 
In the case of the two relative rules, an expression like `relative_block_time < 120000` would mean that the the auth descriptor would be valid from its creation and 120000ms (aka 2 minutes) into the future.
note
The `operation_count` variable is only compatible with the `<=` and `<` operators. 
A rule can either be simple or complex. Complex rules have multiple conditions, and they must all be true for the auth descriptor to be usable.

When considering whether an auth descriptor is usable, the rules will be checked, and the auth descriptor will be assigned a status. Based on the evaluation of its rules, an auth descriptor can be assigned one of three statuses:

- Active: The auth descriptor meets all its rule requirements and can be used for transactions. 
- Inactive: The auth descriptor currently violates one or more rule conditions, but it may become usable if the conditions change. 
- Expired: The auth descriptor permanently violates its rules and can no longer be used for transactions. 
Expired auth descriptors will be automatically cleaned up when the user interacts with an account, and you do not need to remove them manually.

The provided functions allow you to define a rule easily. These rules will enable you to define specific conditions that must be met before the associated auth descriptor becomes valid or invalid.
- Entities involved
- Key concepts
- Auth descriptor components
- Auth descriptor arguments
- Auth descriptor rules