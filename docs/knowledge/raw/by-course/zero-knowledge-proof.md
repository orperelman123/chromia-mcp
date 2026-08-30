# COURSE zero-knowledge-proof — 19 pages


===== FILE: courses__zero-knowledge-proof__architecture-overview.md =====

# Architecture overview

URL: https://learn.chromia.com

- [Home](/)
- Architecture overviewOn this page
# Architecture overview

## What is the demo dapp?​

The ZKP demo dapp is a production-ready privacy-preserving token system built on Chromia that demonstrates how to implement confidential transactions using zero-knowledge proofs. It creates a complete dual-token ecosystem where users can seamlessly move between public and private token representations while maintaining full transaction privacy.

### Core concept​

The dapp implements a UTXO-based privacy model, where:

- Public tokens (FT4) exist transparently on the blockchain

- Private tokens exist as cryptographic commitments with hidden amounts

- Shield operations convert public tokens to private commitments

- Private transfers move private tokens between users with complete anonymity

- Unshield operations convert private tokens back to public tokens

This creates a confidential transaction layer on top of Chromia's blockchain,
giving users the choice between public and private transactions.

## Technical architecture​

### Multi-layer architecture​

The dapp consists of three tightly integrated layers:

### 1. Circom zero-knowledge circuits​

Technology stack: Circom, PLONK proof system, Groth16 (backup)

Key features:

- PLONK proofs for efficient verification

- Poseidon hashing for circuit-friendly cryptography

- Nullifier generation to prevent double-spending

- Commitment schemes for amount hiding

### 2. Rell dapp (blockchain layer)​

Technology stack: [Rell](https://docs.chromia.com/rell/rell-intro),
[Chromia Postchain](https://docs.chromia.com/intro/about/architecture/node#overview-of-postchain),
[FT4 Framework](https://docs.chromia.com/ft4/intro),
ZKPGTXModule

#### Security features​

- ZKP verification using ZKPGTXModule for all private operations

- Double-spending prevention through nullifier tracking

- Input validation ensuring only valid commitments can be spent

- State consistency maintaining accurate UTXO sets

### 3. Next.js client (frontend layer)​

Technology stack: Next.js, React, TypeScript, MetaMask integration, Chromia FT4Client

Key features:

- SecureNoteManager for client-side key management and note encryption

- FT4Client integration for seamless blockchain interaction

- MetaMask wallet connection for user authentication and signing

- ZKP proof generation in the browser using WebAssembly circuits

- Private key derivation from wallet signatures for note decryption

- Intuitive UI for shield, unshield, and private transfer operations


===== FILE: courses__zero-knowledge-proof__circom-circuits.md =====

# Module 1 – Circom circuits

URL: https://learn.chromia.com

- [Home](/)
- Module 1 – Circom circuits
# Module 1 – Circom circuits

Circom circuits form the cryptographic foundation of the zero-knowledge proof system,
allowing us to define mathematical constraints that prove knowledge of private information without revealing the data itself.

In this module, you'll learn to write, compile, and understand the three core circuits that power our private token transfer demo:
shield operations, unshield operations, and private transfers.

By mastering these circuits, you'll gain hands-on experience with the domain-specific language
that transforms our privacy requirements into verifiable cryptographic proofs.

## Lessons
[Circom circuits: introduction](/courses/zero-knowledge-proof/circom-circuits/circom-circuits-introduction)[Circom circuits: project](/courses/zero-knowledge-proof/circom-circuits/circom-circuits-project)[Circom circuits: compile](/courses/zero-knowledge-proof/circom-circuits/circom-circuits-compile)[Start module »](/courses/zero-knowledge-proof/circom-circuits/circom-circuits-introduction)


===== FILE: courses__zero-knowledge-proof__circom-circuits__circom-circuits-compile.md =====

# Circom circuits: compile

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Circom circuits](/courses/zero-knowledge-proof/circom-circuits/)
- Circom circuits: compileOn this page
# Circom circuits: compile

## Setup Circom circuits​

The first component to set up is the Circom circuits. These need to be compiled to generate proving and verification keys.

```bash
cd circom_circuit
```

Install dependencies

```bash
npm install
```

## Compile Circom circuits​

Compile all circuits and generate proving/verification keys

```bash
chmod +x compile_complete.sh
```

Compile circuits

```bash
./compile_complete.sh
```

The following files will be generated:

- shield_operation.zkey & shield_operation_verification_key.json

- unshield_operation.zkey & unshield_operation_verification_key.json

- private_transfer.zkey & verification_key.json

This compilation process does a few things:

- It converts the human-readable .circom files into a set of constraints (.r1cs).

- It performs a trusted setup to generate a proving key (.zkey) and a verification key (verification_key.json). The proving key is used by the client to generate proofs, and the verification key is used by the blockchain to verify them.

- It also generates WebAssembly (.wasm) versions of the circuits for efficient execution in the browser.


===== FILE: courses__zero-knowledge-proof__circom-circuits__circom-circuits-introduction.md =====

# Circom circuits: introduction

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Circom circuits](/courses/zero-knowledge-proof/circom-circuits/)
- Circom circuits: introductionOn this page
# Circom circuits: introduction
A Circom circuit is a program written in the Circom language that defines a set of constraints for a zero-knowledge proof. Circom is a domain-specific language (DSL) for creating arithmetic circuits, which are fundamental for building zk-SNARKs (Zero-Knowledge Succinct Non-Interactive Arguments of Knowledge).

In essence, you use Circom to describe a computation in a way that can be proven without revealing all the inputs. The circuit is composed of:

- Signals: These are the variables of the circuit, which can be inputs, outputs, or intermediate values. Inputs can be public or private.

- Constraints: These are equations that define the relationships between the signals. All constraints in Circom must be reduced to a quadratic form: A * B + C = 0, where A, B, and C are linear combinations of the signals.

### Example of a Circom circuit​

Here is a simple example of a Circom circuit that proves knowledge of two numbers that multiply to a given output.

```circom
pragma circom 2.0.0;template Multiplier2() {   // Private inputs   signal input a;   signal input b;   // Public output   signal output c;   // Constraint   c 
In this example:

- a and b are private input signals. The prover knows their values, but they are not revealed to the verifier.

- c is a public output signal. Its value is known to both the prover and the verifier.

- c <== a * b; is a constraint that forces c to be the product of a and b. The <== operator both assigns the value and creates a constraint.

### How to use a Circom circuit​

Using a Circom circuit involves a multi-step process to create and verify a zero-knowledge proof.

- 
Writing the circuit: First, you write your circuit in a .circom file, like the Multiplier2 example above.

- 
Compilation: You compile the .circom file using the Circom compiler. This checks for syntax errors and generates two files:

- A file containing the circuit's constraints in a format called R1CS (Rank-1 Constraint System).

- A program (often in WebAssembly) to calculate the witness.

```bash
circom multiplier.circom --r1cs --wasm --sym
```

- 
Witness calculation: The witness is a special file that contains all the signal values for a specific execution of the circuit (a valid assignment of values to all signals). To generate the witness, you need to provide an input file with the values for the private and public inputs.

- 
Proof generation: Once you have the R1CS file and the witness, you can use a library like snarkjs to generate the actual zero-knowledge proof. This step also requires a "proving key" which is generated from a trusted setup ceremony.

- 
Proof verification: Finally, another party can use the proof, the public inputs/outputs, a "verification key" (also from the trusted setup), and the original circuit's constraints (R1CS) to verify that the proof is valid. If it is, they can be sure that the prover knew a set of private inputs that satisfy the circuit's constraints, without learning what those inputs were.


===== FILE: courses__zero-knowledge-proof__circom-circuits__circom-circuits-project.md =====

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


===== FILE: courses__zero-knowledge-proof__dapp.md =====

# Module 2 – Dapp

URL: https://learn.chromia.com

- [Home](/)
- Module 2 – Dapp
# Module 2 – Dapp

The Rell dapp implements a privacy-preserving token system that enables confidential transactions
using zero-knowledge proofs, with three core operations:
shield (converting public tokens to private commitments),
unshield (converting private notes back to public tokens),
and private transfers (transferring tokens between users without revealing amounts or identities).

The dapp manages unspent commitments and spent nullifiers to prevent double-spending
while maintaining complete transaction privacy, storing encrypted notes on-chain for wallet synchronization.

At its core, the dapp leverages Chromia's ZKPGTXModule to verify PLONK proofs for each operation,
ensuring cryptographic integrity while keeping sensitive transaction details hidden from the public ledger.

## Lessons
[Dapp: setup and run](/courses/zero-knowledge-proof/dapp/dapp-setup-run)[Dapp: overview](/courses/zero-knowledge-proof/dapp/dapp-overview)[Dapp: entities](/courses/zero-knowledge-proof/dapp/dapp-entities)[Dapp: PLONK verification](/courses/zero-knowledge-proof/dapp/dapp-verification)[Dapp: operations](/courses/zero-knowledge-proof/dapp/dapp-operations)[Dapp: queries](/courses/zero-knowledge-proof/dapp/dapp-queries)[Start module »](/courses/zero-knowledge-proof/dapp/dapp-setup-run)


===== FILE: courses__zero-knowledge-proof__dapp__dapp-entities.md =====

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


===== FILE: courses__zero-knowledge-proof__dapp__dapp-operations.md =====

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


===== FILE: courses__zero-knowledge-proof__dapp__dapp-overview.md =====

# Dapp overview

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: overviewOn this page
# Dapp overview

The zero-knowledge proof (ZKP) dapp implements a privacy-preserving token system on Chromia that enables confidential transactions using the blockchain's built-in ZKP verification capabilities.

## Dapp architecture​

The dapp is built using the Rell programming language and leverages Chromia's native ZKP infrastructure:

- ZKPGTXModule: Built-in Chromia module for PLONK proof verification

- FT4 integration: Seamless integration with Chromia's token standard

- State management: On-chain tracking of commitments and nullifiers

- Event logging: Encrypted note storage for wallet synchronization

## ZKP integration on Chromia​

### Transaction flow​

- Client submits transaction with zkp_plonk_verify operation

- ZKPGTXModule verifies the proof against stored verification keys

- Public signals extracted and passed to subsequent operations

- Dapp operations validate constraints and update state

## Privacy model​

### UTXO-based privacy​

- Commitments: Private notes represented as Pedersen commitments

- Nullifiers: Prevent double-spending without revealing note details

The dapp demonstrates how Chromia's built-in ZKP capabilities enable sophisticated privacy features
directly at the blockchain protocol level,
providing enterprise-grade confidential transactions with minimal complexity.


===== FILE: courses__zero-knowledge-proof__dapp__dapp-queries.md =====

# Dapp queries overview

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: queriesOn this page
# Dapp queries overview

This guide provides a comprehensive breakdown of the queries implemented in the zkp-demo workspace, which demonstrates a private token transfer system on Chromia using zero-knowledge proofs.

## Queries​

The system provides comprehensive queries for:

### Transaction queries​

rell/src/zkp_demo/operations.rell
```rell
// Check nullifier statusquery is_nullifier_spent(nullifier_hash: big_integer) {    return spent_nullifier @? { .nullifier_hash == nullifier_hash } != null;}// Get unspent commitmentsquery get_all_unspent_commitments() {    return unspent_commitment @* {} (.commitment_hash);}// Get transfer eventsquery get_private_transfer_events() {    return private_transfer_event @* {} ($.to_struct());}// Get test assetquery get_test_asset() {    return init.test_asset.to_struct();}
```

### Shield/unshield queries​

rell/src/zkp_demo/operations.rell
```rell
// Get shield logsquery get_shield_logs(): list> {    return shield_log @* {} ($.to_struct());}// Get unspent shield logs by accountquery get_all_unspent_shield_logs_by_account_id(account_id: byte_array): list> {    return (uc: zkp_demo.unspent_commitment, sl: shield_log) @* {         uc.commitment_hash == sl.commitment,         sl.account_id == account_id     } (sl.to_struct());}// Get unshield logsquery get_unshield_logs(): list> {    return unshield_log @* {} ($.to_struct());}
```

### Address registry queries​

rell/src/zkp_demo/operations.rell
```rell
// Get private address by account IDquery get_private_address_by_account_id(account_id: byte_array): big_integer? {    return private_address_registry @? { account_id } ( .private_address );}// Get encryption key by account IDquery get_public_encryption_key_by_account_id(account_id: byte_array): text? {    return private_address_registry @? { account_id } ( .public_encryption_key );}// Get both private address and public encryption key by account IDquery get_user_keys_by_account_id(account_id: byte_array): (private_address: big_integer?, public_encryption_key: text?) {    val user = private_address_registry @? { account_id };    return (        private_address = user?.private_address,        public_encryption_key = user?.public_encryption_key    );}// Get all registered usersquery get_all_registered_users() {    return private_address_registry @* {} ( $.to_struct() );}// Get EVM wallet address from account IDquery get_user_wallet_address(account_id: byte_array): byte_array? {    val account = ft4.accounts.account @? { .id == account_id };    if (account == null) return null;        var evm_wallet_address: byte_array? = null;    val auth_descriptor_signers = (        mad: ft4.accounts.main_auth_descriptor,        ads: ft4.accounts.auth_descriptor_signer    ) @* {        account == mad.account,        ads.account_auth_descriptor == mad.auth_descriptor    } ( ads );    for (auth_descriptor_signer in auth_descriptor_signers) {        if (auth_descriptor_signer.id.size() == 20) {            evm_wallet_address = auth_descriptor_signer.id;            break;        }    }    return evm_wallet_address;}
```

## Integration with FT4​

The ZKP demo seamlessly integrates with Chromia's FT4 framework:

- Account authentication: Uses FT4's authentication system

- Asset management: Leverages FT4's asset handling for public tokens

- Balance operations: Integrates with FT4's balance management


===== FILE: courses__zero-knowledge-proof__dapp__dapp-setup-run.md =====

# Dapp: setup and run

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: setup and runOn this page
# Dapp: setup and run

## Setup and run the dapp​

With the verification keys generated, you need to configure your local Chromia node to use them.

The generated verification keys must be configured in rell/chromia.yml.
The contents of the .json files generated in the previous step must be added into this chromia.yml file manually.

rell/chromia.yml
```yaml
gtx:  modules:    - "net.postchain.zkp.ZKPGTXModule"zkp:  plonk:    verification_keys:      shield_operation:        # Copy contents from shield_operation_verification_key.json      unshield_operation:        # Copy contents from unshield_operation_verification_key.json        private_transfer:        # Copy contents from verification_key.json
```

The already generated keys can be used from the example repository.

Next, run a local directory node using Docker:

```bash
docker run --rm -it -p 7740:7740/tcp registry.gitlab.com/chromaway/example-projects/directory1-example/managed-single:0.7.1
```

```bash
cd rell
```

Generate a provider keypair:

```bash
chr keygen -f provider-keypair
```

And deploy the dapp to your local network:

```bash
chr deployment create -y -d local --secret provider-keypair
```

Expected output:

```bash
deployments:  local:    chains:      zkp_demo: x"3E339D3D2AB109....EC3CD97681134"
```

DAPP_BRID = E339D3D2AB109....EC3CD97681134


===== FILE: courses__zero-knowledge-proof__dapp__dapp-verification.md =====

# PLONK verification

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: PLONK verificationOn this page
# PLONK verification

This guide explains how PLONK (Permutations over Lagrange-bases for Oecumenical Noninteractive arguments of Knowledge) verification is implemented in the zkp-demo dapp to ensure the integrity of private token operations.

## What is PLONK verification?​

PLONK verification is the process of cryptographically validating that a zero-knowledge proof was generated correctly without revealing the underlying private information. In our course, it ensures that operations like private transfers, shielding, and unshielding are mathematically valid while maintaining privacy.

## Core verification function: zkp.check_plonk_proof​

### Function overview​

The zkp.check_plonk_proof method is Chromia's built-in function for verifying PLONK proofs within blockchain operations:

Chromia ZKP Library
```rell
/** * Checks whether or not the current transaction contains a valid PLONK proof. * * @param verification_key_id ID of the verification key that the proof must have been validated with * @param public_signals The public signals that the proof must have been validated with */function check_plonk_proof(    verification_key_id: text,    public_signals: list)
```

### Verification requirements​

For PLONK verification to work in the dapp, the transaction must include a zkp_plonk_verify GTX operation containing:

- Verification key ID: Must match a key configured in chromia.yml

- PLONK proof: The cryptographic proof data generated by the circuit

- Public signals: Public values that the proof validates

### Dapp verification keys​

The course uses three different verification keys for different operations:

zkp-demo/rell/chromia.yml
```yaml
# chromia.yml configurationzkp:  plonk:    verification_keys:      private_transfer:    # For anonymous transfers        curve: bn128        nPublic: 5      shield_operation:    # For converting public to private tokens          curve: bn128        nPublic: 4      unshield_operation:  # For converting private to public tokens        curve: bn128        nPublic: 4
```

## Verification security features​

### Double-spending prevention​

The verification process includes nullifier checks to prevent the same proof from being used multiple times:

zkp-demo/rell/src/zkp_demo/operations.rell
```rell
// Check if nullifier has been used beforerequire_not_exists(nullifier_registry @* { .nullifier == nullifier });// Register the nullifier after successful verificationcreate nullifier_registry(nullifier = nullifier, block_height = op_context.block_height);
```

### Signal integrity​

check_plonk_proof ensures that:

- Public signals match exactly what was submitted in the proof

- The proof was generated with the correct circuit constraints

- No tampering occurred during transmission

### Circuit constraint validation​

The PLONK proof mathematically guarantees that:

- The prover knows the private key for the commitment

- Arithmetic relationships (like balances) are correctly maintained

- All circuit logic was properly executed

This PLONK verification system ensures that all private operations in the course maintain both privacy and mathematical correctness, forming the foundation of the secure private token transfer system.


===== FILE: courses__zero-knowledge-proof__frontend.md =====

# Module 3 – Frontend

URL: https://learn.chromia.com

- [Home](/)
- Module 3 – Frontend
# Module 3 – Frontend

This module explores the Next.js client application that provides the user interface for our zero-knowledge proof token system.

You'll learn how the frontend integrates WebAssembly-compiled Circom circuits for client-side proof generation,
implements secure key management with password-based encryption, and handles real-time shield, transfer,
and unshield operations.

The lessons cover both setting up the development environment and understanding the sophisticated cryptographic architecture
that makes privacy-preserving transactions accessible through a modern web interface.

## Lessons
[Frontend: setup and run](/courses/zero-knowledge-proof/frontend/frontend-setup-run)[Frontend: architecture](/courses/zero-knowledge-proof/frontend/frontend-explore)[Frontend: test](/courses/zero-knowledge-proof/frontend/frontend-test)[Start module »](/courses/zero-knowledge-proof/frontend/frontend-setup-run)


===== FILE: courses__zero-knowledge-proof__frontend__frontend-explore.md =====

# Frontend architecture

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 – Frontend](/courses/zero-knowledge-proof/frontend/)
- Frontend: architectureOn this page
# Frontend architecture

The frontend is built with Next.js, providing a complete user interface for interacting with zero-knowledge proof operations on Chromia.
Let's explore how it is implemented.

## Key dependencies​

The frontend relies on several important libraries for ZKP operations:

client/package.json
```json
{  "dependencies": {    "@chromia/ft4": "^2.0.0",          // FT4 token standard    "circomlibjs": "^0.1.7",           // Circom library for ZK circuits    "crypto-js": "^4.1.1",             // Cryptographic operations    "ethers": "^5.7.2",                // Ethereum wallet integration    "postchain-client": "^2.0.0",      // Chromia blockchain client    "snarkjs": "^0.7.0"                // zk-SNARK proof generation  }}
```

## Architecture overview​

The frontend is organized into several key components:

- SecureNoteManager: Core ZKP functionality for managing private notes

- ZKP utilities: Proof generation and verification

- Crypto utilities: Cryptographic operations and hashing

- FT4Client: Blockchain interaction layer

- React components: User interface for ZKP operations

## SecureNoteManager - core ZKP engine​

The SecureNoteManager class is the heart of the ZKP system, managing encrypted notes and cryptographic keys:

client/src/services/secureNoteManager.js
```javascript
class SecureNoteManager {    constructor() {        this.poseidon = null;           // Poseidon hash function        this.masterSeed = null;         // Master seed for key derivation        this.spendingKey = null;        // ZK spending key        this.viewingKey = null;         // ZK viewing key        this.encryptionPrivateKey = null; // Asymmetric encryption key        this.encryptionPublicKey = null;  // Asymmetric public key        this.notes = new Map();         // commitment -> note data        this.nullifiers = new Set();    // Used nullifiers        this.isUnlocked = false;        // Security state    }    // Generate secure deterministic master seed from EVM signature + password    async generateSecureMasterSeed(password, accountId, evmWallet) {        const message = `ZKP_WALLET_MASTER_SEED_${password}_${accountId}`;                // Get signature from EVM wallet (MetaMask)        const signature = await evmWallet.signMessage(message);                // Derive deterministic master seed from signature        const signatureSeed = ethers.utils.keccak256(signature);        const masterSeed = ethers.utils.keccak256(            ethers.utils.concat([                ethers.utils.arrayify(signatureSeed),                ethers.utils.toUtf8Bytes(password),                ethers.utils.toUtf8Bytes(accountId)            ])        );                return ethers.utils.arrayify(masterSeed);    }    // Derive ZK keys for zero-knowledge proofs    async deriveZKKeys() {        const masterKey = ethers.utils.keccak256(this.masterSeed);        // Derive spending key (for generating nullifiers and proving ownership)        this.spendingKey = this.poseidon.F.toString(            this.poseidon([BigInt(masterKey)])        );        // Derive viewing key (for ZK proofs)        this.viewingKey = this.poseidon.F.toString(            this.poseidon([BigInt(this.spendingKey), BigInt(1)])        );    }}
```

## ZKP utilities - proof generation​

The ZKP utilities handle the generation of zero-knowledge proofs for private transfers:

client/src/services/ft4Client.js
```javascript
// Generate a proof for a private transferexport const generateProof = async ({  privateSpendKey,  amount_input,  privateAddress_input,  blindingFactor_input,  transfer_amount,  blindingFactor_output_sender,  blindingFactor_output_recipient,  recipient_privateAddress}) => {  // Initialize crypto if needed  await initCrypto();    // Calculate input commitment (for verification)  const commitment_input = hashValues([    amount_input,    privateAddress_input,    blindingFactor_input  ]);    // Calculate sender's change amount  const sender_change = BigInt(amount_input) - BigInt(transfer_amount);    // Calculate output commitments  const commitment_output_sender = hashValues([    sender_change,    privateAddress_input,    blindingFactor_output_sender  ]);    const commitment_output_recipient = hashValues([    transfer_amount,    recipient_privateAddress,    blindingFactor_output_recipient  ]);    // Calculate nullifier (prevents double-spending)  const nullifier = hashValues([privateSpendKey, commitment_input]);    // Prepare inputs for the circuit  const inputs = {    // Private inputs (hidden from verifier)    privateSpendKey: privateSpendKey.toString(),    amount_input: amount_input.toString(),    privateAddress_input: privateAddress_input.toString(),    blindingFactor_input: blindingFactor_input.toString(),    transfer_amount: transfer_amount.toString(),    blindingFactor_output_sender: blindingFactor_output_sender.toString(),    blindingFactor_output_recipient: blindingFactor_output_recipient.toString(),        // Public inputs (visible to verifier)    commitment_input: commitment_input.toString(),    commitment_output_sender: commitment_output_sender.toString(),    commitment_output_recipient: commitment_output_recipient.toString(),    recipient_privateAddress: recipient_privateAddress.toString(),    nullifier: nullifier.toString()  };    try {    // Generate the ZK proof using snarkjs    const { proof, publicSignals } = await snarkjs.plonk.fullProve(      inputs,      '/private_transfer.wasm',  // Circuit WASM file      '/circuit.zkey'           // Proving key    );        // Format the proof for Postchain    const proofData = Object.values(proof).flat().map(value => value.toString());        return {      proof: proofData,      publicSignals: publicSignals.map(signal => signal.toString()),      commitment_input,      commitment_output_sender,       commitment_output_recipient,      recipient_privateAddress,      nullifier,      sender_change    };  } catch (error) {    console.error('Error generating proof:', error);    throw error;  }};
```

## Crypto utilities - cryptographic operations​

The crypto utilities provide essential cryptographic functions:

client/src/utils/crypto.js
```javascript
import CryptoJS from 'crypto-js';// Initialize circomlib for Poseidon hashinglet poseidon;export const initCrypto = async () => {  if (!poseidon) {    const circomlibjs = await import('circomlibjs');    poseidon = await circomlibjs.buildPoseidon();  }  return { poseidon };};// Generate a random blinding factor for commitmentsexport const generateBlindingFactor = () => {  const buf = new Uint8Array(32);  window.crypto.getRandomValues(buf);  return BigInt('0x' + Array.from(buf).map(b => b.toString(16).padStart(2, '0')).join(''));};// Derive private address from private spend keyexport const derivePrivateAddress = async (privateSpendKey) => {  await initCrypto();  return hashValues([privateSpendKey]);};// Hash values using Poseidon (simplified version)export function hashValues(values) {  const combined = values.map(v => v.toString()).join(',');  return BigInt('0x' + CryptoJS.SHA256(combined).toString().slice(0, 16));}// Encrypt note for recipientexport function encryptNote(note, privateAddress) {  // Convert private address to encryption key  const keyHex = BigInt(privateAddress).toString(16).padStart(64, '0');  const key = keyHex.slice(0, 64);    // Encrypt using AES  const noteJson = JSON.stringify(note);  const encrypted = CryptoJS.AES.encrypt(noteJson, key).toString();    return Buffer.from(encrypted, 'utf8');}// Decrypt note if intended for this addressexport function decryptNote(encryptedNote, privateAddress) {  try {    const keyHex = BigInt(privateAddress).toString(16).padStart(64, '0');    const key = keyHex.slice(0, 64);        const encryptedString = Buffer.isBuffer(encryptedNote)       ? encryptedNote.toString('utf8')       : encryptedNote;        const decrypted = CryptoJS.AES.decrypt(encryptedString, key);    const decryptedString = decrypted.toString(CryptoJS.enc.Utf8);        return decryptedString ? JSON.parse(decryptedString) : null;  } catch (error) {    return null; // Note not intended for this address  }}
```

## Frontend user interface​

The main React component provides the user interface for ZKP operations:

client/src/pages/index.js
```javascript
export default function Home() {  // State management for ZKP operations  const [ft4Client, setFt4Client] = useState(null);  const [isWalletUnlocked, setIsWalletUnlocked] = useState(false);  const [publicBalance, setPublicBalance] = useState('0');  const [privateBalance, setPrivateBalance] = useState('0');  const [showPasswordModal, setShowPasswordModal] = useState(false);    // Initialize FT4 Client with ZKP support  useEffect(() => {    const initializeClient = async () => {      const client = new FT4Client();      await client.initialize();      setFt4Client(client);            // Check for existing sessions      const existingSession = await client.checkExistingSession();      if (existingSession) {        // Restore session and sync private notes        if (client.isWalletUnlocked()) {          await client.syncPrivateNotes();        }      }    };        initializeClient();  }, []);  // Handle shielding tokens (public -> private)  const handleShield = async () => {    const performShield = async () => {      const loadingToast = toast.loading('Shielding tokens... Generating ZK proof...');            try {        // Convert to blockchain format        const shieldAmountFormatted = formatAmount(shieldAmount);                // Generate ZK proof and shield tokens        await ft4Client.shieldTokens(BigInt(shieldAmountFormatted));                // Refresh balances        await refreshBalances();                toast.success(`Successfully shielded ${shieldAmount} tokens!`, { id: loadingToast });      } catch (error) {        toast.error('Failed to shield tokens', { id: loadingToast });      }    };    // Prompt for password if wallet is locked    if (!isWalletUnlocked) {      promptForPassword('shield', performShield);    } else {      await performShield();    }  };  // Handle private transfers  const handleTransfer = async () => {    const performTransfer = async () => {      const loadingToast = toast.loading('Processing private transfer... Generating ZK proof...');            try {        const transferAmountFormatted = formatAmount(transferAmount);                // Generate ZK proof and perform private transfer        await ft4Client.privateTransfer(          BigInt(transferAmountFormatted),           transferRecipient        );                await refreshBalances();                toast.success(`Successfully transferred ${transferAmount} tokens privately!`, { id: loadingToast });      } catch (error) {        toast.error('Failed to complete private transfer', { id: loadingToast });      }    };    if (!isWalletUnlocked) {      promptForPassword('transfer', performTransfer);    } else {      await performTransfer();    }  };  // Handle unshielding tokens (private -> public)  const handleUnshield = async () => {    const performUnshield = async () => {      const loadingToast = toast.loading('Unshielding tokens... Generating ZK proof...');            try {        const unshieldAmountFormatted = formatAmount(unshieldAmount);                // Generate ZK proof and unshield tokens        await ft4Client.unshieldTokens(BigInt(unshieldAmountFormatted));                await refreshBalances();                toast.success(`Successfully unshielded ${unshieldAmount} tokens!`, { id: loadingToast });      } catch (error) {        toast.error('Failed to unshield tokens', { id: loadingToast });      }    };    if (!isWalletUnlocked) {      promptForPassword('unshield', performUnshield);    } else {      await performUnshield();    }  };}
```

## Note management and privacy​

The frontend manages private notes with strong encryption:

client/src/services/secureNoteManager.js
```javascript
// Create a new private noteasync createNote(amount, recipientPrivateAddress = null) {    this.requireUnlocked();    const privateAddress = recipientPrivateAddress || this.getPrivateAddress();    const blindingFactor = this.generateBlindingFactor();        // Create commitment: hash(amount, privateAddress, blindingFactor)    const commitment = this.createCommitment(amount, privateAddress, blindingFactor);    const noteData = {        amount: amount.toString(),        privateAddress: privateAddress.toString(),        blindingFactor,        commitment,        isOwned: privateAddress.toString() === this.getPrivateAddress().toString(),        createdAt: Date.now()    };    // Store note locally if we own it    if (noteData.isOwned) {        this.notes.set(commitment, noteData);        await this.saveNotesToStorage();    }    return noteData;}// Save notes with asymmetric encryptionasync saveNotesToStorage() {    const notesData = {        notes: Array.from(this.notes.entries()),        nullifiers: Array.from(this.nullifiers),        version: '2.0',        keypairId: this.keypairSeed.slice(0, 8),        publicEncryptionKey: this.encryptionPublicKey    };    const publicKeyBase64 = this.getPublicEncryptionKey();    const encrypted = await this.encryptDataAsymmetric(        this.safeStringify(notesData),         publicKeyBase64    );        const encryptedBase64 = btoa(String.fromCharCode(...encrypted));    localStorage.setItem(this.getStorageKey('encrypted_notes'), encryptedBase64);}
```

## ZKP operation flow​

The frontend implements the complete ZKP operation flow:

### 1. Shield operation (public → private)​

- User enters amount to shield

- Frontend generates ZK proof proving ownership of public tokens

- Proof is submitted to blockchain

- Public balance decreases, private note is created

### 2. Private transfer​

- User specifies recipient and amount

- Frontend finds suitable private notes to spend

- ZK proof is generated proving:

- Ownership of input notes

- Correct amount calculation

- Valid nullifiers (prevents double-spending)

- New private notes are created for recipient and change

### 3. Unshield operation (private → public)​

- User enters amount to unshield

- Frontend generates ZK proof proving ownership of private notes

- Private notes are nullified

- Public balance increases

## Security features​

The frontend implements several security measures:

- Password-Protected Wallet: Master seed derived from EVM signature + password

- Automatic Lock: Wallet locks after 30 minutes of inactivity

- Asymmetric Encryption: Notes encrypted with derived keypairs

- Session Management: Secure session handling with MetaMask integration

- Note Validation: Prevents double-spending through nullifier tracking

This architecture provides a complete, secure, and user-friendly interface for zero-knowledge proof operations on Chromia,
demonstrating how complex cryptographic operations can be made accessible through modern web technologies.


===== FILE: courses__zero-knowledge-proof__frontend__frontend-setup-run.md =====

# Frontend: setup and run

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 – Frontend](/courses/zero-knowledge-proof/frontend/)
- Frontend: setup and runOn this page
# Frontend: setup and run

## Start the client application​

Finally, let's set up and start the front-end application.

```bash
cd client
```

```bash
npm install
```

Copy .env_example file to .env.

```bash
cp .env.example .env
```

Update the configuration.

.env
```javascript
NEXT_PUBLIC_BLOCKCHAIN_RID= // the brid from the deploy the dapp stepNEXT_PUBLIC_NODE_API_URL=http://localhost:7740
```

DAPP_BRID - the brid from the deploy the dapp step

Start the development server:

```bash
npm run dev
```

## Access the application​

Once the server is running, you can access the application in your browser:

- Open [http://localhost:3000](http://localhost:3000).

- Connect your MetaMask wallet.

- Login or register a new account.

- Create a wallet password for private key encryption.

- You are now ready to use private transfers!

In the next sections, we will explore the different parts of the application in more detail.


===== FILE: courses__zero-knowledge-proof__frontend__frontend-test.md =====

# Frontend: test

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 – Frontend](/courses/zero-knowledge-proof/frontend/)
- Frontend: testOn this page
# Frontend: test

### Initialization steps​

- Connect MetaMask

- Login/register

Create FT4 account on Rell dapp side.

- Unlock wallet

The unlock wallet part is a step that is created to securely generate an encryption keypair
that would be used for decrypting/encrypting private operation data from logs.
This is used to derive and calculate the total private balance of users,
as well as for encrypting private data.

- Register private address

This is for users to be able to receive private transfers. The private address is not hidden;
it is being used in the registry to target a certain user more easily with a fixed value.
This is a preference and not a technical requirement in order to achieve this demo.

## Demo scenarios​

### Basic flow demo​

- Open app (keys auto-generated)

- Get 1000 tokens from faucet

- Shield 500 tokens (public → private)

- Transfer 200 tokens to random recipient

- Unshield remaining 300 tokens (private → public)

### Privacy demo​

- Show public balance is visible on blockchain

- Shield tokens to make them private

- Demonstrate private transfers don't reveal amounts

- Show how recipient addresses are hidden

- Unshield to convert back to public when needed

### Multi-user demo​

- Generate multiple random recipients

- Show how private transfers work between users

- Demonstrate that only the recipient can decrypt their notes

- Show how nullifiers prevent double-spending


===== FILE: courses__zero-knowledge-proof__introduction.md =====

# Introduction to the course

URL: https://learn.chromia.com

- [Home](/)
- Introduction to the courseOn this page
# Introduction to the course

Welcome to the Zero-Knowledge Proof (ZKP) course on Chromia.
This course will guide you through a production-ready demonstration of private token transfers using zero-knowledge proofs on the Chromia blockchain.
This demo showcases confidential transactions where transfer amounts
remain hidden from the public ledger while maintaining full transactional integrity.

## Course overview​

This course is based on a demo application that implements a complete private token lifecycle with four core operations:

- Balance creation (faucet): Initialize public token balances for demonstration purposes.

- Shield operation: Convert public tokens to private notes using ZKP.

- Private transfer: Transfer private tokens between users with full privacy and smart consolidation.

- Unshield operation: Convert private notes back to public tokens using ZKP.

By the end of this course, you will understand how these components work together to create a secure and private token system on Chromia.

## Key features​

The system you'll be exploring has a number of powerful features:

- Complete privacy: Transfer amounts and balances are hidden using ZKP.

- Bank-grade security: Hierarchical key derivation with password-based encryption.

- Smart consolidation: Automatic note consolidation for seamless transfers.

- Cross-user encryption: Asymmetric encryption for secure note sharing.

- Production ready: Session management, auto-lock, and enterprise security.

- MetaMask integration: Seamless wallet connection and account management.

- Modern UI: Intuitive interface with real-time balance updates.

Let's get started on our journey into zero-knowledge proofs on Chromia!


===== FILE: courses__zero-knowledge-proof__setup.md =====

# Project setup

URL: https://learn.chromia.com

- [Home](/)
- Project setupOn this page
# Project setup

This section will guide you through setting up the project and understanding how it works.
Following these steps will allow you to run the application and interact with it directly.

## Prerequisites​

Before you begin, make sure you have the following installed:

- Node.js: Version 16 or higher.

- MetaMask: The browser extension should be installed and set up in your browser.

- Docker: To run a Chromia Postchain node locally.

- Rell: rell version 0.14.10. [Install or upgrade.](https://docs.chromia.com/intro/getting-started/installation/cli-installation)

- Circom and snarkjs: Circom is used to write and compile zero-knowledge circuits that define the cryptographic constraints for our proofs.
snarkjs handles the generation and verification of zero-knowledge proofs based on these compiled circuits.
[These tools](https://docs.circom.io/getting-started/installation/) need to be installed globally.

## Clone the demo project​

Please clone the source code for the ZKP demo.

```bash
git clone https://bitbucket.org/chromawallet/zkp-demo.git
```

```bash
cd zkp-demo
```


===== FILE: courses__zero-knowledge-proof__zero-knowledge-proof.md =====

# Zero-knowledge proof

URL: https://learn.chromia.com

- [Home](/)
- Zero-knowledge proofOn this page
# Zero-knowledge proof

## What are zero-knowledge proofs?​

Zero-knowledge proofs are a cryptographic method by which one party (the prover) can prove to another party (the verifier)
that they know a value x, without conveying any information apart from the fact that they know the value x.
This concept is revolutionary for privacy in blockchain applications.

## Use cases for zero-knowledge proofs​

Zero-knowledge proofs have powerful applications specifically in blockchain and decentralized systems:

### Private DeFi and finance​

- Confidential transactions: Hide transaction amounts while maintaining blockchain verifiability

- Private DEX trading: Trade tokens without revealing trading strategies or portfolio sizes

- Anonymous lending: Prove creditworthiness for DeFi loans without exposing financial history

- Private yield farming: Participate in liquidity pools without revealing position sizes

### Decentralized identity and access​

- Proof of membership: Verify membership in DAOs or communities without revealing identity

- Age verification: Prove eligibility for age-restricted dApps without revealing exact age

- Credential verification: Validate qualifications for DeFi protocols without exposing personal data

- Sybil resistance: Prove uniqueness without revealing identifying information

### Blockchain gaming and NFTs​

- Provably fair games: Demonstrate game randomness without revealing random seeds

- Hidden information games: Enable card games and strategy games with concealed information

- Private asset ownership: Prove NFT ownership without revealing collection details

- Achievement systems: Verify game progress without exposing gameplay patterns

### DAO governance and voting​

- Anonymous governance: Vote on proposals while maintaining privacy of vote choices

- Stake-weighted voting: Prove voting power without revealing exact token holdings

- Quadratic voting: Implement fair voting systems with privacy protection

- Delegation privacy: Prove delegation rights without exposing delegation relationships

### Cross-chain and scaling​

- Private cross-chain transfers: Move assets between blockchains without revealing amounts

- Layer 2 privacy: Maintain privacy in rollups and sidechains

- Bridge compliance: Prove regulatory compliance for cross-chain transfers

- Batched transactions: Combine multiple private transactions for efficiency
