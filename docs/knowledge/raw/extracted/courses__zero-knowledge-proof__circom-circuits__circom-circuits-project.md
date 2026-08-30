# Circom files overview

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Circom circuits](/courses/zero-knowledge-proof/circom-circuits/)
- Circom circuits: projectOn this page
# Circom files overview

This section provides a comprehensive analysis of all the Circom files in the zkp-demo/circom_circuit folder.
This is a zero-knowledge proof (ZKP) based privacy-preserving token system for Chromia that enables private token transfers while maintaining on-chain verifiability.

## Overview of the system​

The system implements a shielded balance architecture with three main privacy operations:

- Shield: Convert public tokens to private tokens

- Unshield: Convert private tokens back to public tokens

- Private transfer: Transfer tokens privately between users

## Detailed analysis of each Circom file​

### shield_operation.circom​

circom_circuit/shield_operation.circom
```circom
pragma circom 2.0.0;include "./node_modules/circomlib/circuits/poseidon.circom";include "./node_modules/circomlib/circuits/comparators.circom";// Template for shielding public tokens to privatetemplate ShieldOperation() {    // Private inputs (known only to the prover)    signal input privateSpendKey;    signal input amount;    signal input blindingFactor;    signal input userPubKey; // The public key of the user shielding tokens    signal input shieldAmount; // Amount being shielded    // Public inputs (visible on-chain)    signal output commitment;            // 1. Derive private address from private key    component hashPrivateKey = Poseidon(1);    hashPrivateKey.inputs[0] 
Purpose: Converts public tokens to private tokens

Key features:

- Takes a user's public tokens and creates a private commitment

- Uses Poseidon hash to derive private address from spending key

- Creates a commitment using amount, private address, and blinding factor

- Ensures the shielded amount matches the private amount

### unshield_operation.circom​

circom_circuit/unshield_operation.circom
```circom
pragma circom 2.0.0;include "./node_modules/circomlib/circuits/poseidon.circom";include "./node_modules/circomlib/circuits/comparators.circom";// Template for unshielding private tokens back to publictemplate UnshieldOperation() {    // Private inputs (known only to the prover)    signal input privateSpendKey;    signal input amount;    signal input blindingFactor;        // Public inputs (visible on-chain)    signal input commitment;    signal input nullifier;    signal input userPubKey; // The public key of the user unshielding tokens    signal input unshieldAmount; // Amount being unshielded        // 1. Derive private address from private key    component hashPrivateKey = Poseidon(1);    hashPrivateKey.inputs[0] 
Purpose: Converts private tokens back to public tokens

Key features:

- Proves ownership of private tokens without revealing the private key

- Generates a nullifier to prevent double-spending

- Verifies the commitment matches the claimed amount

- Publishes the unshielded amount to make it publicly visible

### private_transfer.circom​

circom_circuit/private_transfer.circom
```circom
pragma circom 2.0.0;include "./node_modules/circomlib/circuits/poseidon.circom";include "./node_modules/circomlib/circuits/comparators.circom";// Template for private-to-private token transferstemplate PrivateTransfer() {    // Private inputs (known only to the prover)    signal input privateSpendKey;           // Sender's spending key    signal input amount_input;              // Amount of the input note    signal input privateAddress_input;      // Sender's private address    signal input blindingFactor_input;      // Blinding factor of input note    signal input transfer_amount;           // Amount being transferred    signal input blindingFactor_output_sender;     // Blinding factor for sender's change    signal input blindingFactor_output_recipient;  // Blinding factor for recipient's note        // Public inputs (visible on-chain)    signal input commitment_input;          // Input note commitment    signal input commitment_output_sender;  // Sender's change note commitment    signal input commitment_output_recipient; // Recipient's note commitment    signal input recipient_privateAddress;  // Recipient's private address    signal input nullifier;                 // Nullifier for input note        // 1. Verify that privateAddress_input is correctly derived from privateSpendKey    component hashPrivateKey = Poseidon(1);    hashPrivateKey.inputs[0] = 0 (sufficient funds)    // Since Circom works with field elements, we need to ensure no underflow    // With 6 decimal places, amounts can be large (1 token = 1,000,000 base units)    // Use 252-bit comparison to handle large token amounts safely    component gtEqZero = GreaterEqThan(252); // 252-bit comparison for large amounts with decimals    gtEqZero.in[0] = transfer_amount}// The main component with public signals specifiedcomponent main {     public [        commitment_input,         commitment_output_sender,         commitment_output_recipient,         recipient_privateAddress,         nullifier    ] } = PrivateTransfer();
```

Purpose: Enables private token transfers between users

Key features:

- Most complex circuit with ~3,000 constraints

- Implements a UTXO-like model: spends one input note, creates two output notes (change + recipient)

- Validates sender has sufficient funds using range checks

- Ensures conservation of value (input = sender_change + transfer_amount)

- Prevents double-spending with nullifiers

- Keeps transfer amounts completely private

## Key cryptographic concepts​

### Poseidon hash function​

- Used throughout all circuits as the primary hash function

- Optimized for zero-knowledge proofs (more efficient than SHA-256 in ZK contexts)

- Used for deriving private addresses and creating commitments

### Commitments​

- Hide token amounts and ownership using the formula: Poseidon(amount, privateAddress, blindingFactor)

- The blinding factor adds randomness to prevent brute-force attacks

- Commitments are published on-chain but reveal no information about the underlying values

### Nullifiers​

- Prevent double-spending of private notes

- Calculated as: Poseidon(privateSpendKey, commitment)

- Each note can only be spent once because its nullifier becomes public

### Range checks​

- Ensure users have sufficient funds for transfers

- Use 252-bit comparisons to handle large token amounts with decimal precision

- Prevent underflow attacks in the arithmetic circuits

## Security features​

- Privacy: Token amounts and sender/recipient identities remain hidden

- Double-spending prevention: Nullifiers ensure each note can only be spent once

- Value conservation: Circuits enforce that inputs equal outputs in transfers

- Access control: Only holders of private spending keys can spend notes

- Range validation: Prevents negative balances and overflow attacks
