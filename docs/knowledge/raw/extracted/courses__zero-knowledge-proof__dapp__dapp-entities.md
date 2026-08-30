# Dapp entities

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: entitiesOn this page
# Dapp entities

The project contains several key entities that work together to enable private token transactions.
Let's explore each entity and understand their role in the privacy-preserving token system.

## Core privacy entities​

### spent_nullifier​

rell/src/zkp_demo/entities.rell
```rell
entity spent_nullifier {    key nullifier_hash: big_integer;}
```

Purpose: This entity tracks spent nullifiers to prevent double-spending attacks in the privacy system.

How it works:

- When a private token is spent, a unique nullifier is generated from the commitment

- This nullifier is stored in spent_nullifier to mark it as "used"

- Before allowing any transaction, the system checks if the nullifier has already been spent

- This prevents the same private token from being spent multiple times

### unspent_commitment​

rell/src/zkp_demo/entities.rell
```rell
entity unspent_commitment {    key commitment_hash: big_integer;}
```

Purpose: Stores all unspent commitments that represent available private tokens.

How it works:

- Each private token is represented by a commitment (a cryptographic hash)

- When tokens are shielded (made private) or received in a private transfer, new commitments are added

- When tokens are spent or unshielded, commitments are removed from this entity

- This acts as the "UTXO set" for private tokens

### private_transfer_event​

rell/src/zkp_demo/entities.rell
```rell
@logentity private_transfer_event {    commitment_input: big_integer;    commitment_output_sender: big_integer;    commitment_output_recipient: big_integer;    recipient_private_address: big_integer;    nullifier: big_integer;    encrypted_sender_note: byte_array;    encrypted_recipient_note: byte_array;}
```

Purpose: Logs all private transfers with encrypted transaction details.

How it works:

- Records each private transfer transaction on-chain

- Contains the input commitment being spent and two output commitments (sender change + recipient)

- Includes encrypted notes that only the sender and recipient can decrypt

- The @log annotation makes this queryable for wallet scanning

- Wallets can scan these events to detect incoming private transactions

## Token shielding/unshielding entities​

### shield_log​

rell/src/zkp_demo/private_token.rell
```rell
@logentity shield_log {    account_id: byte_array;    commitment: big_integer;    amount: big_integer;    encrypted_note: byte_array;}
```

Purpose: Logs when public tokens are converted to private tokens (shielded).

How it works:

- Records when a user converts their public FT4 tokens into private commitments

- Links the public account to the private commitment created

- Contains encrypted note with transaction details

- Enables users to track their shielding history

### unshield_log​

rell/src/zkp_demo/private_token.rell
```rell
@logentity unshield_log {    account_id: byte_array;    nullifier: big_integer;    commitment: big_integer;    amount: big_integer;}
```

Purpose: Logs when private tokens are converted back to public tokens (unshielded).

How it works:

- Records when a user converts their private commitments back to public FT4 tokens

- Links the nullifier and commitment to the public account receiving tokens

- Enables users to track their unshielding history

- Provides audit trail for regulatory compliance

## User management entity​

### private_address_registry​

rell/src/zkp_demo/private_address_registry.rell
```rell
entity private_address_registry {    key account_id: byte_array;    mutable private_address: big_integer;    mutable public_encryption_key: text;    mutable registered_at: timestamp;}
```

Purpose: Maps public FT4 accounts to their private addresses and encryption keys.

How it works:

- Each user registers their private address (derived from their private key) with their public FT4 account

- Stores the user's public encryption key for secure note encryption

- Enables other users to send private tokens by looking up the recipient's private address

- Acts as a "phone book" for private transactions

- Users can update their keys as needed

## Entity relationships​

## Privacy model summary​

This entity structure implements a UTXO-based privacy model where:

- Public tokens are converted to private commitments via shielding

- Private commitments can be transferred privately using zero-knowledge proofs

- Nullifiers prevent double-spending without revealing which commitment was spent

- Encrypted notes allow transaction details to be shared securely

- Address registry enables users to receive private payments

The system provides transaction privacy (amounts and recipients are hidden) while maintaining auditability through the logged events and regulatory compliance through the shield/unshield logs that link to public identities.
