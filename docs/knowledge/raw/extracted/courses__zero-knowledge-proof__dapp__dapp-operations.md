# Dapp operations overview

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: operationsOn this page
# Dapp operations overview

This guide provides a comprehensive breakdown of the operations implemented in the zkp-demo workspace, which demonstrates a private token transfer system on Chromia using zero-knowledge proofs.

## Overview​

The ZKP demo implements a private token transfer system that allows users to:

- Convert public tokens to private tokens (shielding)

- Transfer private tokens anonymously

- Convert private tokens back to public tokens (unshielding)

- Maintain a registry of private addresses

## Core operations​

### Private transfer operation​

rell/src/zkp_demo/operations.rell
```rell
operation transfer_private_tokens(    encrypted_sender_note: byte_array,    encrypted_recipient_note: byte_array) {    val public_signals = zkp.extract_signals_from_preceeding_proof_op("private_transfer");    zkp.check_plonk_proof("private_transfer", public_signals);    // Extract public signals    require(public_signals.size() == 5, "Invalid public signals size");    val commitment_input = public_signals[0];    val commitment_output_sender = public_signals[1];    val commitment_output_recipient = public_signals[2];    val recipient_private_address = public_signals[3];    val nullifier = public_signals[4];        // Check input commitment exists    val sender_unspent_commitment = require(        unspent_commitment @? { .commitment_hash == commitment_input },        "Input commitment not found in unspent list"    );        // Check nullifier has not been spent    require(        spent_nullifier @? { .nullifier_hash == nullifier } == null,        "Nullifier already spent"    );        // Update the blockchain state    create spent_nullifier ( nullifier_hash = nullifier );    delete sender_unspent_commitment;    create unspent_commitment ( commitment_hash = commitment_output_sender );    create unspent_commitment ( commitment_hash = commitment_output_recipient );        // Log the transfer event with encrypted notes    create private_transfer_event (        commitment_input,        commitment_output_sender,        commitment_output_recipient,        recipient_private_address,        nullifier,        encrypted_sender_note,        encrypted_recipient_note    );}
```

Purpose: Core private transfer functionality using zero-knowledge proofs

How it works:

- 
ZKP verification: Extracts and validates public signals from a "private_transfer" proof

- 
Input validation: Checks that exactly 5 public signals are provided:

- commitment_input: Input commitment being spent

- commitment_output_sender: New output commitment for sender (change)

- commitment_output_recipient: Output commitment for recipient

- recipient_private_address: Recipient's private address

- nullifier: Prevents double-spending

- 
Security checks:

- Verifies the input commitment exists in unspent list

- Ensures the nullifier hasn't been used before (prevents double-spending)

- 
State updates:

- Marks nullifier as spent

- Removes input commitment from unspent list

- Adds two new output commitments (sender change + recipient)

- 
Event logging: Records the transfer with encrypted notes for wallet synchronization

### Shield operation​

rell/src/zkp_demo/private_token.rell
```rell
operation shield_tokens(    encrypted_note: byte_array) {    val account = auth.authenticate();    val public_signals = zkp.extract_signals_from_preceeding_proof_op("shield_operation");    // Extract public signals    val commitment = public_signals[0];    val amount = public_signals[2];        // Verify the ZKP    zkp.check_plonk_proof("shield_operation", public_signals);        // Subtract amount from user's public balance    assets.deduct_balance(account, zkp_demo.init.test_asset, amount);        // Add commitment to unspent commitments    create zkp_demo.unspent_commitment(commitment);        // Log the shield operation    create shield_log(        account_id = account.id,        commitment = commitment,        amount = amount,        encrypted_note = encrypted_note    );}
```

Purpose: Converts public FT4 tokens to private tokens

Process:

- Authenticates the user account

- Extracts public signals from "shield_operation" ZKP:

- commitment: New private commitment

- amount: Amount being shielded

- Verifies the zero-knowledge proof

- Deducts tokens from user's public FT4 balance

- Creates a new unspent commitment in the private system

- Logs the shielding operation with encrypted note

### Unshield operation​

rell/src/zkp_demo/operations.rell
```rell
operation unshield_tokens() {    val account = auth.authenticate();    val public_signals = zkp.extract_signals_from_preceeding_proof_op("unshield_operation");    // Extract public signals    val commitment = public_signals[0];    val nullifier = public_signals[1];    val amount = public_signals[3];        // Verify the ZKP    zkp.check_plonk_proof("unshield_operation", public_signals);        // Check if commitment exists    val unspent_commitment = require(zkp_demo.unspent_commitment @? { commitment }, "Commitment does not exist");    // Check if nullifier has been spent    require(zkp_demo.spent_nullifier @? { nullifier } == null, "Nullifier already spent");        // Mark nullifier as spent    create zkp_demo.spent_nullifier(nullifier);        // Remove commitment from unspent list    delete unspent_commitment;        // Add amount to user's public balance    assets.increase_balance(account, zkp_demo.init.test_asset, amount);        // Log the unshield operation    create unshield_log(        account_id = account.id,        nullifier = nullifier,        commitment = commitment,        amount = amount    );}
```

Purpose: Converts private tokens back to public FT4 tokens

Process:

- Authenticates the user account

- Extracts public signals from "unshield_operation" ZKP:

- commitment: Commitment being unshielded

- nullifier: Prevents double-spending

- amount: Amount being unshielded

- Verifies the zero-knowledge proof

- Validates the commitment exists and nullifier hasn't been spent

- Marks nullifier as spent and removes commitment

- Adds tokens to user's public FT4 balance

- Logs the unshielding operation

### Private address registration​

rell/src/zkp_demo/private_address_registry.rell
```rell
operation register_private_address(private_address: big_integer, public_encryption_key: text) {    val account = auth.authenticate();        // Check if already registered    val existing = private_address_registry @? { account.id };        if (existing != null) {        // Update existing registration        update existing (             private_address = private_address,             public_encryption_key = public_encryption_key,            registered_at = op_context.last_block_time         );    } else {        // Create new registration        create private_address_registry(            account_id = account.id,            private_address = private_address,            public_encryption_key = public_encryption_key,            registered_at = op_context.last_block_time        );    }}
```

Purpose: Associates FT4 accounts with private addresses and encryption keys

Features:

- Maps FT4 account IDs to private addresses

- Stores public encryption keys for secure note encryption

- Supports updating existing registrations

- Enables private communication between users
