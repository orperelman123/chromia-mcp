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
